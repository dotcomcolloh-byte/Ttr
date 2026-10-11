package com.telefam.company.app.notifications

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import com.telefam.company.app.data.ChatMessageEntity
import com.telefam.company.app.domain.ChatAttachmentPayloadCodec
import com.telefam.company.app.domain.ChatMessageType
import java.io.File
import java.util.Locale

/**
 * Fast background bitmap & summary builder for Telefam rich expandable notifications
 * (Photos, Multi-Photo Albums, Video Thumbnails with Play overlay, and Location Map Cards).
 */
object TelefamNotificationPreviewRenderer {

    fun buildNotificationSummaryLine(msg: ChatMessageEntity): String {
        if (msg.isViewOnce || msg.isViewOnceConsumed) {
            return when (msg.messageType) {
                ChatMessageType.IMAGE.name -> "View-once photo"
                ChatMessageType.VIDEO.name -> "View-once video"
                ChatMessageType.VOICE_NOTE.name -> "View-once voice note"
                ChatMessageType.LOCATION.name -> "View-once location"
                else -> "New view-once message"
            }
        }
        val cleanCaption = msg.decryptedText.replace("\n", " ").trim()
        return when (msg.messageType) {
            ChatMessageType.IMAGE.name -> if (cleanCaption.isNotBlank()) "Photo · $cleanCaption" else "Sent a photo"
            ChatMessageType.VIDEO.name -> if (cleanCaption.isNotBlank()) "Video · $cleanCaption" else "Sent a video"
            ChatMessageType.MEDIA_ALBUM.name -> {
                val count = ChatAttachmentPayloadCodec.decodeAlbum(msg.mediaUrl)?.items?.size ?: 2
                if (cleanCaption.isNotBlank()) "Album ($count items) · $cleanCaption" else "Sent $count photos/videos"
            }
            ChatMessageType.VOICE_NOTE.name -> {
                val secs = (msg.voiceDurationMs / 1000L).coerceAtLeast(1L)
                String.format(Locale.US, "Voice note (%d:%02d)", secs / 60L, secs % 60L)
            }
            ChatMessageType.LOCATION.name -> {
                val loc = ChatAttachmentPayloadCodec.decodeLocation(msg.mediaUrl)
                val prefix = if (loc?.isLiveLocation == true) "Live location" else "Location"
                val label = loc?.placeTitle?.ifBlank { loc.addressLine }.orEmpty()
                if (label.isNotBlank()) "$prefix · $label" else "Shared a location"
            }
            ChatMessageType.CONTACT.name -> {
                val contact = ChatAttachmentPayloadCodec.decodeContact(msg.mediaUrl)
                "Contact · ${contact?.displayName?.ifBlank { "Shared Contact" } ?: "Shared Contact"}"
            }
            ChatMessageType.DOCUMENT.name -> {
                val doc = ChatAttachmentPayloadCodec.decodeDocument(msg.mediaUrl)
                "Document · ${doc?.fileName ?: msg.mediaTitle.ifBlank { "File" }}"
            }
            ChatMessageType.POLL.name -> {
                val poll = ChatAttachmentPayloadCodec.decodePoll(msg.mediaUrl)
                "Poll · ${poll?.question ?: cleanCaption.ifBlank { "New poll" }}"
            }
            ChatMessageType.EVENT.name -> {
                val ev = ChatAttachmentPayloadCodec.decodeEvent(msg.mediaUrl)
                "Event · ${ev?.title ?: cleanCaption.ifBlank { "New event" }}"
            }
            ChatMessageType.STICKER.name -> "Sent a sticker"
            ChatMessageType.GIF.name -> "Sent a GIF"
            ChatMessageType.CLIP.name -> "Sent a video clip"
            else -> cleanCaption.ifBlank { "New encrypted message" }
        }
    }

    fun buildRichExpandedPreviewBitmap(msg: ChatMessageEntity): Bitmap? {
        return runCatching {
            when (msg.messageType) {
                ChatMessageType.IMAGE.name -> {
                    val path = msg.mediaPreviewUrl?.takeIf { it.startsWith("/") && File(it).exists() }
                        ?: msg.mediaLocalPath?.takeIf { File(it).exists() }
                    if (path != null) decodeFastNotificationBitmap(path, maxDimension = 720) else null
                }
                ChatMessageType.MEDIA_ALBUM.name -> {
                    val album = ChatAttachmentPayloadCodec.decodeAlbum(msg.mediaUrl)
                    val firstItem = album?.items?.firstOrNull()
                    val path = firstItem?.thumbnailPath?.takeIf { File(it).exists() }
                        ?: firstItem?.localPath?.takeIf { File(it).exists() }
                        ?: msg.mediaPreviewUrl?.takeIf { it.startsWith("/") && File(it).exists() }
                    val baseBmp = if (path != null) decodeFastNotificationBitmap(path, maxDimension = 720) else null
                    if (baseBmp != null && (album?.items?.size ?: 0) > 1) {
                        overlayAlbumCountBadgeOnBitmap(baseBmp, album?.items?.size ?: 2)
                    } else {
                        baseBmp
                    }
                }
                ChatMessageType.VIDEO.name, ChatMessageType.CLIP.name -> {
                    val thumbPath = msg.mediaPreviewUrl?.takeIf { it.startsWith("/") && File(it).exists() }
                    var videoFrame = if (thumbPath != null) {
                        decodeFastNotificationBitmap(thumbPath, maxDimension = 720)
                    } else null
                    if (videoFrame == null && !msg.mediaLocalPath.isNullOrBlank() && File(msg.mediaLocalPath).exists()) {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(msg.mediaLocalPath)
                            videoFrame = retriever.getFrameAtTime(150_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                ?: retriever.getFrameAtTime(0L)
                        } catch (_: Throwable) {
                        } finally {
                            runCatching { retriever.release() }
                        }
                    }
                    if (videoFrame != null) {
                        overlayVideoPlayBadgeOnBitmap(videoFrame, msg.voiceDurationMs)
                    } else null
                }
                ChatMessageType.LOCATION.name -> {
                    val loc = ChatAttachmentPayloadCodec.decodeLocation(msg.mediaUrl)
                    if (loc != null) {
                        renderLocationMapPreviewBitmap(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            placeTitle = loc.placeTitle.ifBlank { "Pinned Location" },
                            addressLine = loc.addressLine,
                            isLive = loc.isLiveLocation
                        )
                    } else null
                }
                else -> null
            }
        }.getOrNull()
    }

    fun decodeFastNotificationBitmap(filePath: String, maxDimension: Int): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(filePath, opts)
        }.getOrNull()
    }

    private fun overlayVideoPlayBadgeOnBitmap(source: Bitmap, durationMs: Long): Bitmap {
        val w = source.width.coerceAtLeast(320)
        val h = source.height.coerceAtLeast(180)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), paint)

        paint.color = Color.argb(72, 0, 0, 0)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        val cx = w / 2f
        val cy = h / 2f
        val radius = (minOf(w, h) * 0.16f).coerceIn(24f, 48f)
        paint.color = Color.argb(210, 200, 8, 21)
        canvas.drawCircle(cx, cy, radius, paint)

        val triPath = Path().apply {
            moveTo(cx - radius * 0.28f, cy - radius * 0.42f)
            lineTo(cx + radius * 0.46f, cy)
            lineTo(cx - radius * 0.28f, cy + radius * 0.42f)
            close()
        }
        paint.color = Color.WHITE
        canvas.drawPath(triPath, paint)

        if (durationMs > 0L) {
            val totalSec = (durationMs / 1000L).coerceAtLeast(1L)
            val label = String.format(Locale.US, "VIDEO · %d:%02d", totalSec / 60L, totalSec % 60L)
            paint.textSize = (h * 0.065f).coerceIn(18f, 28f)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val pad = 16f
            val tw = paint.measureText(label)
            val pillRect = RectF(pad, h - pad - 44f, pad + tw + 28f, h - pad)
            paint.color = Color.argb(190, 16, 18, 24)
            canvas.drawRoundRect(pillRect, 14f, 14f, paint)
            paint.color = Color.WHITE
            canvas.drawText(label, pad + 14f, h - pad - 14f, paint)
        }
        return out
    }

    private fun overlayAlbumCountBadgeOnBitmap(source: Bitmap, totalItems: Int): Bitmap {
        val w = source.width.coerceAtLeast(320)
        val h = source.height.coerceAtLeast(180)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), paint)

        val label = "+${(totalItems - 1).coerceAtLeast(1)} more"
        paint.textSize = (h * 0.07f).coerceIn(20f, 30f)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val tw = paint.measureText(label)
        val pad = 18f
        val pillRect = RectF(w - tw - pad - 32f, h - pad - 46f, w - pad, h - pad)
        paint.color = Color.argb(210, 200, 8, 21)
        canvas.drawRoundRect(pillRect, 16f, 16f, paint)
        paint.color = Color.WHITE
        canvas.drawText(label, pillRect.left + 16f, pillRect.bottom - 14f, paint)
        return out
    }

    fun renderLocationMapPreviewBitmap(
        latitude: Double,
        longitude: Double,
        placeTitle: String,
        addressLine: String,
        isLive: Boolean
    ): Bitmap {
        val width = 640
        val height = 320
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(Color.rgb(232, 241, 236), Color.rgb(218, 232, 244)),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        paint.color = Color.argb(55, 120, 145, 160)
        paint.strokeWidth = 2f
        for (x in 0..width step 48) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), paint)
        }
        for (y in 0..height step 48) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
        }

        paint.color = Color.argb(180, 255, 255, 255)
        paint.strokeWidth = 14f
        canvas.drawLine(0f, height * 0.68f, width.toFloat(), height * 0.24f, paint)
        canvas.drawLine(width * 0.22f, 0f, width * 0.66f, height.toFloat(), paint)

        val cx = width * 0.5f
        val cy = height * 0.44f
        paint.style = Paint.Style.FILL
        paint.color = if (isLive) Color.argb(50, 200, 8, 21) else Color.argb(42, 25, 118, 210)
        canvas.drawCircle(cx, cy, 56f, paint)

        paint.color = Color.rgb(200, 8, 21)
        canvas.drawCircle(cx, cy - 10f, 22f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy - 10f, 8f, paint)

        val bannerRect = RectF(16f, height - 96f, width - 16f, height - 14f)
        paint.color = Color.argb(235, 20, 22, 28)
        canvas.drawRoundRect(bannerRect, 18f, 18f, paint)

        paint.color = if (isLive) Color.rgb(255, 102, 112) else Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 24f
        val header = if (isLive) "LIVE LOCATION · ${placeTitle.take(28)}" else placeTitle.take(34)
        canvas.drawText(header, 34f, height - 58f, paint)

        paint.color = Color.rgb(200, 206, 216)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 19f
        val coords = String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
        val sub = if (addressLine.isNotBlank()) "${addressLine.take(36)} ($coords)" else coords
        canvas.drawText(sub, 34f, height - 28f, paint)

        return bmp
    }

    fun buildCircularSenderAvatarBitmap(displayName: String, avatarPath: String?): Bitmap {
        val size = 128
        if (!avatarPath.isNullOrBlank() && avatarPath.startsWith("/") && File(avatarPath).exists()) {
            val decoded = decodeFastNotificationBitmap(avatarPath, maxDimension = size)
            if (decoded != null) {
                val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(out)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(decoded, null, Rect(0, 0, size, size), paint)
                return out
            }
        }

        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(200, 8, 21)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        val initials = displayName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "TF" }

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 48f
        paint.textAlign = Paint.Align.CENTER
        val yPos = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(initials, size / 2f, yPos, paint)
        return out
    }
}
