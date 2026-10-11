package com.telefam.company.app.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.telefam.company.app.domain.ChatAttachmentPayloadCodec
import com.telefam.company.app.domain.ChatContactPayload
import com.telefam.company.app.domain.ChatDocumentCategory
import com.telefam.company.app.domain.ChatDocumentPayload
import com.telefam.company.app.domain.ChatEventPayload
import com.telefam.company.app.domain.ChatLocationPayload
import com.telefam.company.app.domain.ChatMediaAttachmentItem
import com.telefam.company.app.domain.ChatMessageItem
import com.telefam.company.app.domain.ChatMessageType
import com.telefam.company.app.domain.ChatPollPayload
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * View-Once media/location card in chat bubbles:
 * - Flagged as View-Once with the `1` badge
 * - Allows opening once (no thumbnail leak, no download button)
 * - Once opened, permanently marks the message consumed and wipes local media files
 */
@Composable
fun TelefamViewOnceAttachmentBubble(
    message: ChatMessageItem,
    isOutgoing: Boolean,
    textColor: Color,
    subTextColor: Color,
    onOpenViewOnce: () -> Unit
) {
    val typeLabel = when (message.messageType) {
        ChatMessageType.IMAGE -> "Photo"
        ChatMessageType.VIDEO -> "Video"
        ChatMessageType.LOCATION -> "Location"
        else -> "Media"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isOutgoing) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !message.isViewOnceConsumed) { onOpenViewOnce() }
            .testTag("view_once_bubble_${message.messageId}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (message.isViewOnceConsumed) {
                    subTextColor.copy(alpha = 0.18f)
                } else {
                    Color(0xFF10B981).copy(alpha = 0.22f)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.VIEW_ONCE_ONE,
                        tint = if (message.isViewOnceConsumed) subTextColor else Color(0xFF10B981),
                        size = 20.dp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (message.isViewOnceConsumed) {
                        "Opened $typeLabel"
                    } else {
                        "View-Once $typeLabel"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (message.isViewOnceConsumed) subTextColor else textColor
                )
                Text(
                    text = if (message.isViewOnceConsumed) {
                        "Expired · Media wiped from storage"
                    } else {
                        "Tap to view once · Download disabled"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = subTextColor
                )
            }

            if (!message.isViewOnceConsumed) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF10B981)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.EYE_VIEW,
                            tint = Color.White,
                            size = 14.dp
                        )
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders Single Image, Single Video (with thumbnail + play icon + duration), or Multi-Item Grid Album
 * (with "+N" button to open horizontal sideways pager, plus Download / Download All buttons when privacy allows).
 */
@Composable
fun TelefamMediaAttachmentBubble(
    message: ChatMessageItem,
    isOutgoing: Boolean,
    textColor: Color,
    subTextColor: Color,
    allowDownloads: Boolean,
    onOpenUrlInAppBrowser: (String) -> Unit,
    onSelectPhoneNumber: (String) -> Unit,
    onConsumeViewOnce: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val albumPayload = remember(message.mediaUrl, message.mediaLocalPath, message.mediaPreviewUrl) {
        ChatAttachmentPayloadCodec.decodeAlbum(message.mediaUrl) ?: run {
            val singlePath = message.mediaLocalPath ?: message.mediaPreviewUrl.orEmpty()
            if (singlePath.isNotBlank()) {
                com.telefam.company.app.domain.ChatMediaAlbumPayload(
                    items = listOf(
                        ChatMediaAttachmentItem(
                            itemId = message.mediaId ?: message.messageId,
                            localPath = singlePath,
                            thumbnailPath = message.mediaPreviewUrl ?: singlePath,
                            mimeType = message.mediaMimeType,
                            fileName = message.mediaTitle.ifBlank { "Media" },
                            fileSizeBytes = 0L,
                            widthPx = 0,
                            heightPx = 0,
                            durationMs = message.voiceDurationMs,
                            isVideo = message.messageType == ChatMessageType.VIDEO
                        )
                    ),
                    caption = message.decryptedText
                )
            } else null
        }
    }

    var pagerOpenAtIndex by remember { mutableStateOf<Int?>(null) }

    if (message.isViewOnce) {
        TelefamViewOnceAttachmentBubble(
            message = message,
            isOutgoing = isOutgoing,
            textColor = textColor,
            subTextColor = subTextColor,
            onOpenViewOnce = {
                if (!message.isViewOnceConsumed && albumPayload != null && albumPayload.items.isNotEmpty()) {
                    pagerOpenAtIndex = 0
                }
            }
        )
        if (pagerOpenAtIndex != null && albumPayload != null) {
            TelefamFullScreenMediaPagerDialog(
                items = albumPayload.items,
                initialIndex = pagerOpenAtIndex ?: 0,
                isViewOnce = true,
                allowDownloads = false,
                onDismiss = {
                    pagerOpenAtIndex = null
                    onConsumeViewOnce(message.messageId)
                }
            )
        }
        return
    }

    val items = albumPayload?.items.orEmpty()
    val caption = albumPayload?.caption?.ifBlank { message.decryptedText } ?: message.decryptedText

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (items.size == 1) {
            val single = items.first()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable { pagerOpenAtIndex = 0 }
                    .testTag("chat_single_media_preview_${message.messageId}")
            ) {
                AsyncImage(
                    model = File(single.thumbnailPath ?: single.localPath),
                    contentDescription = single.fileName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (single.isVideo) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(54.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.PLAY,
                                tint = Color.White,
                                size = 24.dp
                            )
                        }
                    }

                    if (single.durationMs > 0L) {
                        val sec = (single.durationMs / 1000L).coerceAtLeast(1L)
                        val durStr = String.format(Locale.US, "%d:%02d", sec / 60L, sec % 60L)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.68f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.CLIP_VIDEO,
                                    tint = Color.White,
                                    size = 12.dp
                                )
                                Text(
                                    text = durStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Small Download button in top-end corner when privacy allows
                if (allowDownloads) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                scope.launch {
                                    TelefamAttachmentDeviceHelper.saveMediaItemToDeviceDownloads(context, single)
                                }
                            }
                            .testTag("chat_single_media_download_${message.messageId}"),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.DOWNLOAD,
                                tint = Color.White,
                                size = 16.dp
                            )
                        }
                    }
                }
            }
        } else if (items.size > 1) {
            // 2x2 Grid with "+" overlay to scroll sideways through all items
            val displayItems = items.take(4)
            val remainingCount = (items.size - 4).coerceAtLeast(0)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val rows = displayItems.chunked(2)
                rows.forEachIndexed { rowIdx, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowItems.forEachIndexed { colIdx, item ->
                            val globalIdx = rowIdx * 2 + colIdx
                            val isLastVisibleTile = globalIdx == displayItems.lastIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.25f))
                                    .clickable { pagerOpenAtIndex = globalIdx }
                                    .testTag("chat_album_tile_${message.messageId}_$globalIdx")
                            ) {
                                AsyncImage(
                                    model = File(item.thumbnailPath ?: item.localPath),
                                    contentDescription = item.fileName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (item.isVideo) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(36.dp),
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.62f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            TelefamLucideIcon(
                                                kind = LucideIconKind.PLAY,
                                                tint = Color.White,
                                                size = 16.dp
                                            )
                                        }
                                    }
                                }

                                // Individual small download button on tile when privacy allows
                                if (allowDownloads && !isLastVisibleTile) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(5.dp)
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                scope.launch {
                                                    TelefamAttachmentDeviceHelper.saveMediaItemToDeviceDownloads(context, item)
                                                }
                                            },
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.6f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            TelefamLucideIcon(
                                                kind = LucideIconKind.DOWNLOAD,
                                                tint = Color.White,
                                                size = 13.dp
                                            )
                                        }
                                    }
                                }

                                // "+" button overlay on last grid tile to open sideways scroll view
                                if (isLastVisibleTile) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = if (remainingCount > 0) 0.58f else 0.28f))
                                            .clickable { pagerOpenAtIndex = globalIdx },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = Color.Black.copy(alpha = 0.72f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                TelefamLucideIcon(
                                                    kind = LucideIconKind.PLUS,
                                                    tint = Color.White,
                                                    size = 15.dp
                                                )
                                                Text(
                                                    text = if (remainingCount > 0) "$remainingCount more" else "View All (${items.size})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // "Download All" action bar for multi-item album when privacy allows
            if (allowDownloads) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${items.size} media items · Tap to swipe sideways",
                        style = MaterialTheme.typography.labelSmall,
                        color = subTextColor
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isOutgoing) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                scope.launch {
                                    TelefamAttachmentDeviceHelper.saveAllMediaItemsToDeviceDownloads(context, items)
                                }
                            }
                            .testTag("chat_album_download_all_${message.messageId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.DOWNLOAD,
                                tint = textColor,
                                size = 13.dp
                            )
                            Text(
                                text = "Download All",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        if (caption.isNotBlank()) {
            SmartAutodetectMessageContent(
                messageId = message.messageId,
                textContent = caption,
                isOutgoing = isOutgoing,
                bubbleTextColor = textColor,
                bubbleSubtleColor = subTextColor,
                maxLines = Int.MAX_VALUE,
                onBubbleClick = {},
                onBubbleLongPress = {},
                onOpenSecureBrowser = onOpenUrlInAppBrowser,
                onOpenEmail = { email -> launchEmailClientSafely(context, email) },
                onOpenPhoneActions = { _, dialable -> onSelectPhoneNumber(dialable) },
                onCopyOtp = { otp ->
                    copyTextToClipboard(context, "OTP Code", otp)
                }
            )
        }
    }

    if (pagerOpenAtIndex != null && items.isNotEmpty()) {
        TelefamFullScreenMediaPagerDialog(
            items = items,
            initialIndex = pagerOpenAtIndex ?: 0,
            isViewOnce = false,
            allowDownloads = allowDownloads,
            onDismiss = { pagerOpenAtIndex = null }
        )
    }
}

/**
 * Full-screen sideways scrollable Pager for single or multiple images & videos:
 * - Pinch-to-zoom & double-tap zoom on images
 * - Full video playback with MediaController for videos
 * - "Download This" and "Download All" buttons when `allowDownloads` is true
 */
@Composable
fun TelefamFullScreenMediaPagerDialog(
    items: List<ChatMediaAttachmentItem>,
    initialIndex: Int,
    isViewOnce: Boolean,
    allowDownloads: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
        pageCount = { items.size }
    )
    val currentItem = items.getOrNull(pagerState.currentPage)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF070A0F)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with counter + Download This + Download All
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("fullscreen_media_close_button")
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.CLOSE_X,
                            tint = Color.White,
                            size = 22.dp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (items.size > 1) {
                                "${pagerState.currentPage + 1} of ${items.size}"
                            } else {
                                currentItem?.fileName ?: "Media Preview"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isViewOnce) {
                                "View Once · Closes and deletes on exit"
                            } else if (currentItem?.isVideo == true) {
                                "Video Player"
                            } else {
                                "Pinch or double-tap to zoom"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    if (allowDownloads && !isViewOnce && currentItem != null) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    scope.launch {
                                        TelefamAttachmentDeviceHelper.saveMediaItemToDeviceDownloads(context, currentItem)
                                    }
                                }
                                .testTag("fullscreen_download_this_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.DOWNLOAD,
                                    tint = Color.White,
                                    size = 15.dp
                                )
                                Text(
                                    text = if (items.size > 1) "Download This" else "Download",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (items.size > 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable {
                                        scope.launch {
                                            TelefamAttachmentDeviceHelper.saveAllMediaItemsToDeviceDownloads(context, items)
                                        }
                                    }
                                    .testTag("fullscreen_download_all_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.DOWNLOAD,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        size = 15.dp
                                    )
                                    Text(
                                        text = "Download All (${items.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Sideways Horizontal Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { pageIndex ->
                    val pageItem = items[pageIndex]
                    if (pageItem.isVideo) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        val mediaController = MediaController(ctx)
                                        mediaController.setAnchorView(this)
                                        setMediaController(mediaController)
                                        setVideoURI(Uri.fromFile(File(pageItem.localPath)))
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = false
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                            )
                        }
                    } else {
                        var scale by remember(pageIndex) { mutableFloatStateOf(1f) }
                        var offset by remember(pageIndex) { mutableStateOf(Offset.Zero) }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(pageIndex) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale > 1.1f) {
                                                scale = 1f
                                                offset = Offset.Zero
                                            } else {
                                                scale = 2.5f
                                            }
                                        }
                                    )
                                }
                                .pointerInput(pageIndex) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                                        scale = newScale
                                        offset = if (newScale > 1f) {
                                            Offset(
                                                x = (offset.x + pan.x).coerceIn(-600f, 600f),
                                                y = (offset.y + pan.y).coerceIn(-600f, 600f)
                                            )
                                        } else {
                                            Offset.Zero
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = File(pageItem.localPath),
                                contentDescription = pageItem.fileName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Location & Live Location Bubble in Chats:
 * - Supports View-Once Location (opens once inside the secure In-App Browser and then expires)
 * - Supports Live Location with server-issued session token badge and expiry countdown
 * - Never opens externally; always opens inside `TelefamInAppBrowserDialog`
 */
@Composable
fun TelefamLocationAttachmentBubble(
    message: ChatMessageItem,
    isOutgoing: Boolean,
    textColor: Color,
    subTextColor: Color,
    onOpenUrlInAppBrowser: (String) -> Unit,
    onConsumeViewOnce: (String) -> Unit
) {
    val location = remember(message.mediaUrl) {
        ChatAttachmentPayloadCodec.decodeLocation(message.mediaUrl)
    } ?: ChatLocationPayload(
        latitude = 37.7749,
        longitude = -122.4194,
        accuracyMeters = 12f,
        placeTitle = message.mediaTitle.ifBlank { "Shared Location" },
        addressLine = message.decryptedText,
        isLiveLocation = false,
        isViewOnce = message.isViewOnce
    )

    val now = System.currentTimeMillis()
    val isLiveExpired = location.isLiveLocation && location.expiresAtMs != null && location.expiresAtMs < now

    if (message.isViewOnce || location.isViewOnce) {
        TelefamViewOnceAttachmentBubble(
            message = message,
            isOutgoing = isOutgoing,
            textColor = textColor,
            subTextColor = subTextColor,
            onOpenViewOnce = {
                if (!message.isViewOnceConsumed) {
                    val inAppUrl = TelefamAttachmentDeviceHelper.buildInAppLocationViewerUrl(location)
                    onOpenUrlInAppBrowser(inAppUrl)
                    onConsumeViewOnce(message.messageId)
                }
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_location_bubble_${message.messageId}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TelefamInteractiveMapPreviewCard(
            placeTitle = location.placeTitle,
            addressLine = location.addressLine,
            latitude = location.latitude,
            longitude = location.longitude,
            isLiveLocation = location.isLiveLocation && !isLiveExpired,
            isViewOnce = false,
            modifier = Modifier.clickable(enabled = !isLiveExpired) {
                val url = TelefamAttachmentDeviceHelper.buildInAppLocationViewerUrl(location)
                onOpenUrlInAppBrowser(url)
            }
        )

        if (location.isLiveLocation) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.NAVIGATION_LIVE,
                        tint = if (isLiveExpired) subTextColor else Color(0xFF10B981),
                        size = 14.dp
                    )
                    Text(
                        text = if (isLiveExpired) {
                            "Live location session ended"
                        } else {
                            "Live token: ${location.liveSessionToken.orEmpty().take(14)}..."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLiveExpired) subTextColor else textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (location.caption.isNotBlank() && location.caption != location.placeTitle) {
            Text(
                text = location.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isOutgoing) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !isLiveExpired) {
                    val url = TelefamAttachmentDeviceHelper.buildInAppLocationViewerUrl(location)
                    onOpenUrlInAppBrowser(url)
                }
                .testTag("chat_location_open_in_app_button_${message.messageId}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                TelefamLucideIcon(
                    kind = LucideIconKind.GLOBE_WEB,
                    tint = textColor,
                    size = 15.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLiveExpired) "Live Location Expired" else "Open Map in Secure In-App Browser",
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Contact Attachment Bubble in Chats:
 * - Shows rich contact card preview (Avatar initials, Name, Organization, formatted phone number)
 * - Direct "Call", "Save" (to device contacts), and "View Card" buttons that open the full contact detail modal
 */
@Composable
fun TelefamContactAttachmentBubble(
    message: ChatMessageItem,
    isOutgoing: Boolean,
    textColor: Color,
    subTextColor: Color
) {
    val context = LocalContext.current
    val contact = remember(message.mediaUrl) {
        ChatAttachmentPayloadCodec.decodeContact(message.mediaUrl)
    } ?: ChatContactPayload(
        displayName = message.mediaTitle.ifBlank { "Shared Contact" },
        phoneNumber = message.decryptedText
    )

    var showContactDetailModal by remember { mutableStateOf(false) }
    val formattedPrimaryPhone = remember(contact.phoneNumber) {
        if (contact.phoneNumber.isNotBlank()) {
            TelefamSmartEntityDetector.formatAnyPhoneNumber(contact.phoneNumber)
        } else ""
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isOutgoing) Color.White.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { showContactDetailModal = true }
            .testTag("chat_contact_bubble_${message.messageId}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = Color(0xFF0284C7).copy(alpha = 0.22f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val initials = contact.displayName
                            .split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .take(2)
                            .joinToString("")
                            .ifBlank { "C" }
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOutgoing) Color.White else Color(0xFF0284C7)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (formattedPrimaryPhone.isNotBlank()) {
                        Text(
                            text = formattedPrimaryPhone,
                            style = MaterialTheme.typography.labelMedium,
                            color = subTextColor
                        )
                    }
                    if (contact.organization.isNotBlank()) {
                        Text(
                            text = contact.organization,
                            style = MaterialTheme.typography.labelSmall,
                            color = subTextColor
                        )
                    }
                }

                TelefamLucideIcon(
                    kind = LucideIconKind.EXTERNAL_LINK,
                    tint = subTextColor,
                    size = 16.dp
                )
            }

            HorizontalDivider(color = subTextColor.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (contact.phoneNumber.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isOutgoing) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                launchDialerSafely(context, contact.phoneNumber)
                            }
                            .testTag("chat_contact_call_btn_${message.messageId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.PHONE_CALL,
                                tint = textColor,
                                size = 14.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Call",
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOutgoing) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            savePhoneNumberToDeviceContacts(
                                context = context,
                                formattedPhone = formattedPrimaryPhone,
                                dialablePhone = contact.phoneNumber,
                                suggestedName = contact.displayName
                            )
                        }
                        .testTag("chat_contact_save_btn_${message.messageId}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.USER_PLUS_CONTACT,
                            tint = textColor,
                            size = 14.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Contact",
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showContactDetailModal) {
        Dialog(onDismissRequest = { showContactDetailModal = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = Color(0xFF0284C7).copy(alpha = 0.16f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.USER_CONTACT,
                                    tint = Color(0xFF0284C7),
                                    size = 26.dp
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (contact.organization.isNotBlank()) {
                                Text(
                                    text = contact.organization,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val allPhones = listOf(contact.phoneNumber).filter { it.isNotBlank() } + contact.secondaryPhones
                    allPhones.forEach { phone ->
                        val formatted = TelefamSmartEntityDetector.formatAnyPhoneNumber(phone)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = formatted,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Mobile / Direct",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    launchDialerSafely(context, phone)
                                }) {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.PHONE_CALL,
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 18.dp
                                    )
                                }
                                IconButton(onClick = {
                                    copyTextToClipboard(context, "Phone", formatted)
                                }) {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.COPY,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 18.dp
                                    )
                                }
                            }
                        }
                    }

                    if (contact.email.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    launchEmailClientSafely(context, contact.email)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.MAIL_EMAIL,
                                    tint = MaterialTheme.colorScheme.primary,
                                    size = 18.dp
                                )
                                Text(
                                    text = contact.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showContactDetailModal = false }) {
                            Text("Close")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showContactDetailModal = false
                                savePhoneNumberToDeviceContacts(
                                    context = context,
                                    formattedPhone = formattedPrimaryPhone,
                                    dialablePhone = contact.phoneNumber,
                                    suggestedName = contact.displayName
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.USER_PLUS_CONTACT,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                size = 16.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to Device")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Document, Audio, and App (`.apk`) Bubble in Chats:
 * - For APK Apps: shows "Download", "Install", and "Open" actions
 * - For Audio / PDFs / Docs / Archives: shows "Open" (in supported device apps via FileProvider) and "Save to Device" (when privacy allows)
 */
@Composable
fun TelefamDocumentAttachmentBubble(
    message: ChatMessageItem,
    isOutgoing: Boolean,
    textColor: Color,
    subTextColor: Color,
    allowDownloads: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val doc = remember(message.mediaUrl, message.mediaLocalPath, message.mediaTitle) {
        ChatAttachmentPayloadCodec.decodeDocument(message.mediaUrl) ?: ChatDocumentPayload(
            documentId = message.mediaId ?: message.messageId,
            localPath = message.mediaLocalPath.orEmpty(),
            fileName = message.mediaTitle.ifBlank { "Document" },
            mimeType = message.mediaMimeType.ifBlank { "application/octet-stream" },
            fileSizeBytes = 0L,
            category = ChatDocumentPayload.inferCategory(message.mediaMimeType, message.mediaTitle)
        )
    }

    val isApkApp = doc.category == ChatDocumentCategory.APK_APP
    val isAudio = doc.category == ChatDocumentCategory.AUDIO
    val iconKind = when (doc.category) {
        ChatDocumentCategory.APK_APP -> LucideIconKind.PACKAGE_APP
        ChatDocumentCategory.AUDIO -> LucideIconKind.MUSIC_AUDIO
        else -> LucideIconKind.FILE_DOC
    }
    val accentColor = when (doc.category) {
        ChatDocumentCategory.APK_APP -> Color(0xFF10B981)
        ChatDocumentCategory.AUDIO -> Color(0xFFF59E0B)
        ChatDocumentCategory.PDF -> Color(0xFFE11D48)
        else -> Color(0xFF7C3AED)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isOutgoing) Color.White.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_document_bubble_${message.messageId}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        TelefamLucideIcon(
                            kind = iconKind,
                            tint = if (isOutgoing) Color.White else accentColor,
                            size = 22.dp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doc.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sizeStr = if (doc.fileSizeBytes > 0L) {
                        TelefamAttachmentDeviceHelper.formatBytesReadable(doc.fileSizeBytes)
                    } else "Encrypted File"
                    val categoryLabel = when (doc.category) {
                        ChatDocumentCategory.APK_APP, ChatDocumentCategory.APP_APK -> "ANDROID APP (.APK)"
                        ChatDocumentCategory.AUDIO -> "AUDIO TRACK"
                        ChatDocumentCategory.PDF -> "PDF DOCUMENT"
                        ChatDocumentCategory.SPREADSHEET -> "SPREADSHEET"
                        ChatDocumentCategory.ARCHIVE -> "ARCHIVE"
                        else -> "DOCUMENT"
                    }
                    Text(
                        text = "$categoryLabel · $sizeStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = subTextColor
                    )
                }
            }

            if (doc.caption.isNotBlank() && doc.caption != doc.fileName) {
                Text(
                    text = doc.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Open / Install button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOutgoing) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            TelefamAttachmentDeviceHelper.openOrInstallDocumentFile(context, doc)
                        }
                        .testTag("chat_doc_open_btn_${message.messageId}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelefamLucideIcon(
                            kind = if (isApkApp) LucideIconKind.PACKAGE_APP else LucideIconKind.EXTERNAL_LINK,
                            tint = textColor,
                            size = 14.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isApkApp) "Install / Open" else if (isAudio) "Play / Open" else "Open",
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Download / Save to Device button (respects privacy setting)
                if (allowDownloads) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isOutgoing) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                scope.launch {
                                    TelefamAttachmentDeviceHelper.saveDocumentToDeviceDownloads(context, doc)
                                }
                            }
                            .testTag("chat_doc_save_btn_${message.messageId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.DOWNLOAD,
                                tint = textColor,
                                size = 14.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isApkApp) "Download APK" else "Save to Device",
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Poll Bubble in Chats:
 * - Supports both standard text options AND photo/image options with captions beside them
 * - Displays live vote counts and animated percentage progress bars
 * - Respects single-choice vs multiple-choice voting
 * - Includes "View Poll Sheet" to inspect full poll results
 */
@Composable
fun TelefamPollAttachmentBubble(
    message: ChatMessageItem,
    currentUserId: String,
    isOutgoing: Boolean,
    textColor: Color,
    subTextColor: Color,
    onVotePoll: (List<String>) -> Unit
) {
    val poll = remember(message.mediaUrl) {
        ChatAttachmentPayloadCodec.decodePoll(message.mediaUrl)
    } ?: return

    var showPollDetailsSheet by remember { mutableStateOf(false) }
    var zoomedOptionImagePath by remember { mutableStateOf<String?>(null) }

    val totalVotes = poll.totalVotesCount()
    val mySelectedOptionIds = remember(poll, currentUserId) {
        poll.options.filter { currentUserId in it.voterUserIds }.map { it.optionId }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isOutgoing) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_poll_bubble_${message.messageId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    color = Color(0xFFF59E0B).copy(alpha = 0.22f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.BAR_CHART_POLL,
                            tint = if (isOutgoing) Color.White else Color(0xFFF59E0B),
                            size = 16.dp
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = poll.question,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = buildString {
                            append(if (poll.allowMultipleVotes) "Select one or more" else "Select one")
                            if (poll.isAnonymous) append(" · Anonymous poll")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = subTextColor
                    )
                }
            }

            poll.options.forEach { option ->
                val isSelectedByMe = option.optionId in mySelectedOptionIds
                val optionVotes = option.voterUserIds.size
                val pct = if (totalVotes > 0) {
                    ((optionVotes.toFloat() / totalVotes.toFloat()) * 100f).roundToInt()
                } else 0
                val progressFraction = if (totalVotes > 0) {
                    (optionVotes.toFloat() / totalVotes.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelectedByMe) {
                        if (isOutgoing) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        if (isOutgoing) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val newSelection = if (poll.allowMultipleVotes) {
                                if (isSelectedByMe) mySelectedOptionIds - option.optionId
                                else mySelectedOptionIds + option.optionId
                            } else {
                                if (isSelectedByMe) emptyList() else listOf(option.optionId)
                            }
                            onVotePoll(newSelection)
                        }
                        .testTag("chat_poll_option_${message.messageId}_${option.optionId}")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Selection radio/check circle
                            Surface(
                                modifier = Modifier.size(22.dp),
                                shape = CircleShape,
                                color = if (isSelectedByMe) Color(0xFF10B981) else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isSelectedByMe) Color(0xFF10B981) else subTextColor
                                )
                            ) {
                                if (isSelectedByMe) {
                                    Box(contentAlignment = Alignment.Center) {
                                        TelefamLucideIcon(
                                            kind = LucideIconKind.CHECK_SINGLE,
                                            tint = Color.White,
                                            size = 13.dp
                                        )
                                    }
                                }
                            }

                            // Optional Candidate / Option Photo thumbnail
                            if (!option.imageLocalPath.isNullOrBlank()) {
                                AsyncImage(
                                    model = File(option.imageLocalPath),
                                    contentDescription = option.text,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { zoomedOptionImagePath = option.imageLocalPath },
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Text(
                                text = option.text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelectedByMe) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "$pct% ($optionVotes)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = if (isSelectedByMe) Color(0xFF10B981) else Color(0xFFF59E0B),
                            trackColor = subTextColor.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalVotes ${if (totalVotes == 1) "vote" else "votes"} total",
                    style = MaterialTheme.typography.labelSmall,
                    color = subTextColor
                )
                Text(
                    text = "View Poll Details",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPollDetailsSheet = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }

    if (zoomedOptionImagePath != null) {
        val singleItem = ChatMediaAttachmentItem(
            itemId = "poll_img",
            localPath = zoomedOptionImagePath!!,
            thumbnailPath = zoomedOptionImagePath,
            mimeType = "image/jpeg",
            fileName = "Poll Option Photo",
            fileSizeBytes = 0L,
            widthPx = 0,
            heightPx = 0,
            durationMs = 0L,
            isVideo = false
        )
        TelefamFullScreenMediaPagerDialog(
            items = listOf(singleItem),
            initialIndex = 0,
            isViewOnce = false,
            allowDownloads = true,
            onDismiss = { zoomedOptionImagePath = null }
        )
    }

    if (showPollDetailsSheet) {
        Dialog(onDismissRequest = { showPollDetailsSheet = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = poll.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalVotes total ${if (totalVotes == 1) "vote" else "votes"} · ${if (poll.allowMultipleVotes) "Multiple choices allowed" else "Single vote only"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    poll.options.forEach { opt ->
                        val count = opt.voterUserIds.size
                        val pct = if (totalVotes > 0) ((count.toFloat() / totalVotes) * 100f).roundToInt() else 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!opt.imageLocalPath.isNullOrBlank()) {
                                AsyncImage(
                                    model = File(opt.imageLocalPath),
                                    contentDescription = opt.text,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opt.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$count ${if (count == 1) "vote" else "votes"} ($pct%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showPollDetailsSheet = false }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Event Bubble in Chats:
 * - Displays Calendar badge, Title, Description, Schedule, Location, and live RSVP buttons (Going / Maybe / Declined)
 */
@Composable
fun TelefamEventAttachmentBubble(
    message: ChatMessageItem,
    currentUserId: String,
    isOutgoing: Boolean,
    textColor: Color,
    subTextColor: Color,
    onRsvpEvent: (String) -> Unit
) {
    val event = remember(message.mediaUrl) {
        ChatAttachmentPayloadCodec.decodeEvent(message.mediaUrl)
    } ?: return

    val monthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("dd", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()) }

    val myStatus = when {
        currentUserId in event.goingUserIds -> "GOING"
        currentUserId in event.maybeUserIds -> "MAYBE"
        currentUserId in event.declinedUserIds -> "DECLINED"
        else -> ""
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isOutgoing) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_event_bubble_${message.messageId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF43F5E).copy(alpha = 0.2f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = monthFormat.format(Date(event.startTimeMs)).uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOutgoing) Color.White else Color(0xFFF43F5E),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dayFormat.format(Date(event.startTimeMs)),
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = fullDateFormat.format(Date(event.startTimeMs)),
                        style = MaterialTheme.typography.labelSmall,
                        color = subTextColor
                    )
                    if (event.locationLabel.isNotBlank()) {
                        Text(
                            text = event.locationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = subTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (event.description.isNotBlank()) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }

            Text(
                text = "${event.goingUserIds.size} Going · ${event.maybeUserIds.size} Maybe · ${event.declinedUserIds.size} Can't Go",
                style = MaterialTheme.typography.labelSmall,
                color = subTextColor,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("GOING" to "Going", "MAYBE" to "Maybe", "DECLINED" to "Can't Go").forEach { (code, label) ->
                    val isSelected = myStatus == code
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            Color(0xFF10B981)
                        } else {
                            if (isOutgoing) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onRsvpEvent(code) }
                            .testTag("chat_event_rsvp_${message.messageId}_$code")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else textColor
                            )
                        }
                    }
                }
            }
        }
    }
}
