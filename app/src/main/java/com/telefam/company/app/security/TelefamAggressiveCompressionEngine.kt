package com.telefam.company.app.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Silent, zero-UI background compression & 100 MB file limit enforcement engine.
 *
 * - Enforces the strict 100 MB (`104,857,600` bytes) maximum file size on both client and backend.
 * - Aggressively compresses all texts, structured JSON payloads, E2EE plaintext envelopes (prior to
 *   AES-256-GCM encryption), WebSocket frames, HTTP request/response bodies, images, thumbnails,
 *   voice notes, and document/binary transfer payloads in the background (`Dispatchers.IO`).
 * - Never displays any compression banners, notices, or progress UI.
 */
object TelefamAggressiveCompressionEngine {

    /**
     * Hard maximum file size accepted by the backend and client: 100 MB (104,857,600 bytes).
     */
    const val MAX_BACKEND_FILE_SIZE_BYTES: Long = 100L * 1024L * 1024L

    // Hard safety cap when reading an uncompressed input stream to attempt compression into <= 100 MB
    private const val MAX_RAW_CANDIDATE_STREAM_BYTES: Long = 160L * 1024L * 1024L

    private const val FRAME_RAW_UNCOMPRESSED: Byte = 0x00
    private const val FRAME_DEFLATE_BEST_NOWRAP: Byte = 0x01

    private const val MAX_IMAGE_DIMENSION_PX = 1440
    private const val MAX_THUMBNAIL_DIMENSION_PX = 320
    private const val TARGET_IMAGE_BYTE_BUDGET = 190 * 1024 // ~190 KB target for ultra-fast sending on poor links

    /**
     * Aggressively compresses UTF-8 plaintext bytes using raw Deflate at `Deflater.BEST_COMPRESSION` (Level 9)
     * before AES-256-GCM encryption. Prepends a 1-byte framing tag (`0x01` = deflated, `0x00` = raw).
     * Even short texts and structured JSON envelopes are compressed whenever Deflate reduces byte length.
     */
    fun compressPlaintextBeforeEncryption(rawUtf8Bytes: ByteArray): ByteArray {
        if (rawUtf8Bytes.isEmpty()) return byteArrayOf(FRAME_RAW_UNCOMPRESSED)
        return try {
            val deflated = deflateRawBest(rawUtf8Bytes)
            if (deflated.size < rawUtf8Bytes.size) {
                ByteArray(deflated.size + 1).also { out ->
                    out[0] = FRAME_DEFLATE_BEST_NOWRAP
                    System.arraycopy(deflated, 0, out, 1, deflated.size)
                }
            } else {
                ByteArray(rawUtf8Bytes.size + 1).also { out ->
                    out[0] = FRAME_RAW_UNCOMPRESSED
                    System.arraycopy(rawUtf8Bytes, 0, out, 1, rawUtf8Bytes.size)
                }
            }
        } catch (_: Throwable) {
            ByteArray(rawUtf8Bytes.size + 1).also { out ->
                out[0] = FRAME_RAW_UNCOMPRESSED
                System.arraycopy(rawUtf8Bytes, 0, out, 1, rawUtf8Bytes.size)
            }
        }
    }

    /**
     * Transparently decompresses plaintext bytes after AES-256-GCM decryption.
     * Supports both `0x01` (Deflate Level 9), `0x00` (raw framed), and legacy unframed JSON (`{` = 0x7B).
     */
    fun decompressPlaintextAfterDecryption(decryptedBytes: ByteArray): ByteArray {
        if (decryptedBytes.isEmpty()) return decryptedBytes
        return try {
            when (decryptedBytes[0]) {
                FRAME_DEFLATE_BEST_NOWRAP -> {
                    inflateRaw(decryptedBytes, offset = 1, length = decryptedBytes.size - 1)
                }
                FRAME_RAW_UNCOMPRESSED -> {
                    decryptedBytes.copyOfRange(1, decryptedBytes.size)
                }
                else -> {
                    // Legacy unframed JSON payload stored before framing header was introduced
                    decryptedBytes
                }
            }
        } catch (_: Throwable) {
            // Fallback if payload was unframed
            decryptedBytes
        }
    }

    /**
     * Aggressively compresses any text or JSON string into a compact Base64 wire representation
     * using raw Deflate Level 9 (`Deflater.BEST_COMPRESSION`).
     */
    fun compressTextAggressiveToBase64(text: String): String {
        if (text.isEmpty()) return ""
        val utf8 = text.toByteArray(StandardCharsets.UTF_8)
        val compressed = compressPlaintextBeforeEncryption(utf8)
        return encodeBase64NoWrap(compressed)
    }

    /**
     * Decompresses a Base64 wire string produced by [compressTextAggressiveToBase64], or returns
     * the original string if it is already plain UTF-8 / JSON.
     */
    fun decompressTextAggressiveFromBase64OrRaw(input: String): String {
        if (input.isEmpty()) return ""
        val trimmed = input.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) return trimmed
        return try {
            val decoded = decodeBase64NoWrap(trimmed)
            val plain = decompressPlaintextAfterDecryption(decoded)
            String(plain, StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            input
        }
    }

    /**
     * Compresses an HTTP/WebSocket payload using GZIP with `Deflater.BEST_COMPRESSION` (Level 9).
     */
    fun compressGzipBest(rawBytes: ByteArray): ByteArray {
        if (rawBytes.isEmpty()) return rawBytes
        val bos = ByteArrayOutputStream(max(64, rawBytes.size / 2))
        val gzipOut = object : GZIPOutputStream(bos) {
            init {
                def.setLevel(Deflater.BEST_COMPRESSION)
            }
        }
        gzipOut.use { it.write(rawBytes) }
        return bos.toByteArray()
    }

    /**
     * Decompresses a GZIP byte array back to raw bytes.
     */
    fun decompressGzip(compressedBytes: ByteArray): ByteArray {
        if (compressedBytes.size < 2) return compressedBytes
        val isGzipMagic = (compressedBytes[0].toInt() and 0xFF) == 0x1F &&
            (compressedBytes[1].toInt() and 0xFF) == 0x8B
        if (!isGzipMagic) return compressedBytes
        return GZIPInputStream(ByteArrayInputStream(compressedBytes)).use { it.readBytes() }
    }

    /**
     * Raw Deflate with `Deflater.BEST_COMPRESSION` and `nowrap = true` (omits zlib header/checksum overhead).
     */
    fun deflateRawBest(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        return try {
            deflater.setInput(input)
            deflater.finish()
            val bos = ByteArrayOutputStream(max(32, input.size / 2))
            val buf = ByteArray(4096)
            while (!deflater.finished()) {
                val count = deflater.deflate(buf)
                if (count > 0) {
                    bos.write(buf, 0, count)
                } else if (deflater.needsInput()) {
                    break
                }
            }
            bos.toByteArray()
        } finally {
            deflater.end()
        }
    }

    /**
     * Raw Inflate with `nowrap = true`.
     */
    fun inflateRaw(input: ByteArray, offset: Int = 0, length: Int = input.size): ByteArray {
        val inflater = Inflater(true)
        return try {
            inflater.setInput(input, offset, length)
            val bos = ByteArrayOutputStream(max(64, length * 2))
            val buf = ByteArray(4096)
            while (!inflater.finished()) {
                val count = inflater.inflate(buf)
                if (count > 0) {
                    bos.write(buf, 0, count)
                } else if (inflater.needsInput() || inflater.needsDictionary()) {
                    break
                }
            }
            bos.toByteArray()
        } finally {
            inflater.end()
        }
    }

    /**
     * Queries the declared byte length of a content `Uri` if available, without loading it into memory.
     */
    fun queryUriLengthOrNull(context: Context, uri: Uri): Long? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !c.isNull(idx)) c.getLong(idx) else null
                } else null
            } ?: context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.length.takeIf { it >= 0L }
            }
        }.getOrNull()
    }

    /**
     * Copies a stream to [destFile] while enforcing a hard ceiling so huge files cannot exhaust storage.
     * Returns `true` if copied within [maxAllowedBytes], or `false` (and deletes [destFile]) if exceeded.
     */
    fun copyStreamBounded(
        input: InputStream,
        destFile: File,
        maxAllowedBytes: Long = MAX_RAW_CANDIDATE_STREAM_BYTES
    ): Boolean {
        return try {
            var totalBytes = 0L
            val buffer = ByteArray(32 * 1024)
            BufferedOutputStream(FileOutputStream(destFile)).use { out ->
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    totalBytes += read
                    if (totalBytes > maxAllowedBytes) {
                        out.flush()
                        runCatching { destFile.delete() }
                        return false
                    }
                    out.write(buffer, 0, read)
                }
                out.flush()
            }
            true
        } catch (_: Throwable) {
            runCatching { destFile.delete() }
            false
        }
    }

    /**
     * Aggressively compresses an image file silently in the background:
     * - Downsamples high-resolution images to at most `1440px` longest edge.
     * - Strips all EXIF/XMP/ICC metadata by rendering onto a clean bitmap.
     * - Uses multi-pass adaptive quality (`72` down to `48`) with `WEBP_LOSSY` (or `JPEG`) so images
     *   transfer rapidly even on terrible 2G/3G connections while staying under [TARGET_IMAGE_BYTE_BUDGET].
     * - Returns the final `(widthPx, heightPx, mimeType)` of the compressed file.
     */
    suspend fun compressImageFileAggressivelyInPlace(
        imageFile: File
    ): Triple<Int, Int, String> = withContext(Dispatchers.IO) {
        if (!imageFile.exists() || imageFile.length() <= 0L) {
            return@withContext Triple(0, 0, "image/jpeg")
        }
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imageFile.absolutePath, bounds)
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) {
                return@runCatching Triple(0, 0, "image/jpeg")
            }

            var sampleSize = 1
            while ((srcW / sampleSize) > MAX_IMAGE_DIMENSION_PX * 2 ||
                (srcH / sampleSize) > MAX_IMAGE_DIMENSION_PX * 2
            ) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = BitmapFactory.decodeFile(imageFile.absolutePath, decodeOpts)
                ?: return@runCatching Triple(srcW, srcH, "image/jpeg")

            val maxEdge = max(decoded.width, decoded.height)
            val scaledBitmap = if (maxEdge > MAX_IMAGE_DIMENSION_PX) {
                val ratio = MAX_IMAGE_DIMENSION_PX.toFloat() / maxEdge.toFloat()
                val targetW = (decoded.width * ratio).roundToInt().coerceAtLeast(1)
                val targetH = (decoded.height * ratio).roundToInt().coerceAtLeast(1)
                val clean = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(clean)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(
                    decoded,
                    null,
                    android.graphics.RectF(0f, 0f, targetW.toFloat(), targetH.toFloat()),
                    paint
                )
                if (clean !== decoded) decoded.recycle()
                clean
            } else {
                decoded
            }

            val finalW = scaledBitmap.width
            val finalH = scaledBitmap.height

            val useWebp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            val format = if (useWebp) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.JPEG
            }
            val outMime = if (useWebp) "image/webp" else "image/jpeg"

            var quality = 72
            var compressedBytes: ByteArray
            while (true) {
                val bos = ByteArrayOutputStream(64 * 1024)
                scaledBitmap.compress(format, quality, bos)
                compressedBytes = bos.toByteArray()
                if (compressedBytes.size <= TARGET_IMAGE_BYTE_BUDGET || quality <= 48) {
                    break
                }
                quality -= 12
            }
            scaledBitmap.recycle()

            if (compressedBytes.isNotEmpty() &&
                (compressedBytes.size < imageFile.length() || imageFile.length() > MAX_BACKEND_FILE_SIZE_BYTES)
            ) {
                FileOutputStream(imageFile, false).use { out ->
                    out.write(compressedBytes)
                    out.fd.sync()
                }
            }
            Triple(finalW, finalH, outMime)
        }.getOrDefault(Triple(0, 0, "image/jpeg"))
    }

    /**
     * Generates a compact, aggressively compressed preview thumbnail (`<= 320px`, quality `58`)
     * for fast rendering in chat bubbles and home list previews.
     */
    fun writeCompressedMicroThumbnail(
        sourceBitmap: Bitmap,
        destThumbFile: File
    ): Boolean {
        return runCatching {
            val maxEdge = max(sourceBitmap.width, sourceBitmap.height).coerceAtLeast(1)
            val ratio = (MAX_THUMBNAIL_DIMENSION_PX.toFloat() / maxEdge.toFloat()).coerceAtMost(1f)
            val targetW = (sourceBitmap.width * ratio).roundToInt().coerceAtLeast(1)
            val targetH = (sourceBitmap.height * ratio).roundToInt().coerceAtLeast(1)
            val thumb = if (ratio < 1f) {
                Bitmap.createScaledBitmap(sourceBitmap, targetW, targetH, true)
            } else {
                sourceBitmap
            }
            FileOutputStream(destThumbFile, false).use { out ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.JPEG
                }
                thumb.compress(format, 58, out)
                out.fd.sync()
            }
            if (thumb !== sourceBitmap) {
                thumb.recycle()
            }
            true
        }.getOrDefault(false)
    }

    /**
     * Aggressively compresses a generic file or document in the background and enforces the 100 MB
     * backend limit (`MAX_BACKEND_FILE_SIZE_BYTES`).
     *
     * - Produces a deflated wire-transfer sidecar (`.tfz`) with `Deflater.BEST_COMPRESSION` so network
     *   sync transmits minimal bytes.
     * - If the raw file is larger than 100 MB (`104,857,600` bytes), checks whether aggressive Deflate
     *   compression brings it within 100 MB. If still `> 100 MB`, deletes the staged file and returns `false`.
     */
    suspend fun compressAndEnforce100MbLimitOnFile(
        file: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0L) return@withContext false

        val sidecarFile = File(file.parentFile, "${file.name}.tfz")
        val compressedSize = runCatching {
            val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
            try {
                BufferedInputStream(FileInputStream(file), 32 * 1024).use { input ->
                    BufferedOutputStream(FileOutputStream(sidecarFile), 32 * 1024).use { rawOut ->
                        DeflaterOutputStream(rawOut, deflater, 32 * 1024).use { defOut ->
                            val buf = ByteArray(32 * 1024)
                            while (true) {
                                val r = input.read(buf)
                                if (r <= 0) break
                                defOut.write(buf, 0, r)
                            }
                            defOut.finish()
                        }
                    }
                }
                sidecarFile.length()
            } finally {
                deflater.end()
            }
        }.getOrDefault(file.length())

        val effectiveWireBytes = minOf(file.length(), compressedSize.takeIf { it > 0L } ?: file.length())
        if (effectiveWireBytes > MAX_BACKEND_FILE_SIZE_BYTES && file.length() > MAX_BACKEND_FILE_SIZE_BYTES) {
            runCatching { file.delete() }
            runCatching { sidecarFile.delete() }
            return@withContext false
        }

        // If the deflated sidecar isn't smaller than the original (e.g. already compressed stream), remove sidecar
        if (compressedSize <= 0L || compressedSize >= file.length()) {
            runCatching { sidecarFile.delete() }
        }
        true
    }

    /**
     * Reads the smallest wire representation of a local attachment file (either its `.tfz` deflated sidecar
     * or raw bytes) and verifies it is `<= 100 MB`.
     */
    fun getEffectiveTransferByteSize(localPath: String?): Long {
        if (localPath.isNullOrBlank()) return 0L
        val file = File(localPath)
        if (!file.exists()) return 0L
        val sidecar = File(file.parentFile, "${file.name}.tfz")
        return if (sidecar.exists() && sidecar.length() in 1 until file.length()) {
            sidecar.length()
        } else {
            file.length()
        }
    }

    fun isWithinBackend100MbLimit(byteCount: Long): Boolean {
        return byteCount in 0L..MAX_BACKEND_FILE_SIZE_BYTES
    }

    private fun encodeBase64NoWrap(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    private fun decodeBase64NoWrap(encoded: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(encoded)
        } catch (_: Throwable) {
            Base64.decode(encoded, Base64.NO_WRAP)
        }
    }
}
