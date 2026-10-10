package com.telefam.company.backend.security

import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

data class SanitizedMediaArtifact(
    val storageKey: String,
    val sha256Checksum: String,
    val perceptualDHash: String,
    val verifiedMimeType: String,
    val finalByteSize: Int,
    val width: Int,
    val height: Int,
    val reusedFromFingerprint: Boolean,
    val verifiedAtIso: String
)

/**
 * Isolated Sandbox Media Processor for Telefam Backend.
 * 1. Writes untrusted upload into quarantined temporary sandbox file with non-executable permissions.
 * 2. Inspects magic header bytes (JPEG, PNG, WebP) and rejects polyglot/spoofed MIME payloads.
 * 3. Decodes pixel matrix into a clean BufferedImage, stripping all EXIF/GPS/XMP/IPTC metadata and embedded scripts.
 * 4. Re-encodes compressed avatar (max 512x512) and promotes verified file to secure storage.
 */
class SandboxImageProcessor(
    private val sandboxDir: File,
    private val secureStorageDir: File,
    private val maxUploadBytes: Long
) {
    init {
        sandboxDir.mkdirs()
        secureStorageDir.mkdirs()
    }

    fun processInSandbox(rawBytes: ByteArray, declaredContentType: String?): SanitizedMediaArtifact {
        require(rawBytes.isNotEmpty()) { "Uploaded image payload is empty" }
        require(rawBytes.size <= maxUploadBytes) {
            "Image size (${rawBytes.size} bytes) exceeds maximum allowed limit ($maxUploadBytes bytes)"
        }

        val magicMime = detectMagicMimeType(rawBytes)
            ?: throw IllegalArgumentException("Untrusted or unsupported image magic header bytes")

        if (!declaredContentType.isNullOrBlank() &&
            !declaredContentType.equals(magicMime, ignoreCase = true) &&
            !declaredContentType.equals("application/octet-stream", ignoreCase = true)
        ) {
            throw IllegalArgumentException("Declared MIME ($declaredContentType) mismatches magic header ($magicMime)")
        }

        val sandboxFile = File(sandboxDir, "quarantine_${UUID.randomUUID()}.tmp")
        try {
            sandboxFile.writeBytes(rawBytes)
            sandboxFile.setExecutable(false, false)

            // Decode pixels inside isolated stream, discarding all EXIF/ICC/XMP metadata chunks
            val sourceImage = ImageIO.read(ByteArrayInputStream(rawBytes))
                ?: throw IllegalArgumentException("Corrupted image stream rejected by sandbox decoder")

            require(sourceImage.width in 64..4096 && sourceImage.height in 64..4096) {
                "Invalid image dimensions (${sourceImage.width}x${sourceImage.height})"
            }

            val targetSize = 512
            val cleanBitmap = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB)
            val g2d: Graphics2D = cleanBitmap.createGraphics()
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g2d.drawImage(sourceImage, 0, 0, targetSize, targetSize, null)
            } finally {
                g2d.dispose()
            }

            val outStream = ByteArrayOutputStream()
            val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
            val writeParam = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = 0.84f
            }
            ImageIO.createImageOutputStream(outStream).use { ios ->
                writer.output = ios
                // Passing null for streamMetadata and imageMetadata strips all EXIF/metadata
                writer.write(null, IIOImage(cleanBitmap, null, null), writeParam)
                writer.dispose()
            }

            val sanitizedBytes = outStream.toByteArray()
            val sha256 = MessageDigest.getInstance("SHA-256")
                .digest(sanitizedBytes)
                .joinToString("") { "%02x".format(it) }
            val dHash = computePerceptualDHash(cleanBitmap)

            val storageKey = "avatar_${sha256.take(24)}_${dHash}.jpg"
            val destinationFile = File(secureStorageDir, storageKey)
            val reused = destinationFile.exists()
            if (!reused) {
                destinationFile.writeBytes(sanitizedBytes)
                destinationFile.setReadOnly()
            }

            return SanitizedMediaArtifact(
                storageKey = "https://cdn.telefam.company/avatars/$storageKey",
                sha256Checksum = sha256,
                perceptualDHash = dHash,
                verifiedMimeType = "image/jpeg",
                finalByteSize = sanitizedBytes.size,
                width = targetSize,
                height = targetSize,
                reusedFromFingerprint = reused,
                verifiedAtIso = Instant.now().toString()
            )
        } finally {
            if (sandboxFile.exists()) {
                sandboxFile.delete()
            }
        }
    }

    private fun computePerceptualDHash(image: BufferedImage): String {
        val small = BufferedImage(9, 8, BufferedImage.TYPE_INT_RGB)
        val g = small.createGraphics()
        try {
            g.drawImage(image, 0, 0, 9, 8, null)
        } finally {
            g.dispose()
        }
        var bits = 0UL
        var bitIndex = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val leftRgb = small.getRGB(x, y)
                val rightRgb = small.getRGB(x + 1, y)
                val leftLuma = ((leftRgb shr 16 and 0xFF) * 299 + (leftRgb shr 8 and 0xFF) * 587 + (leftRgb and 0xFF) * 114)
                val rightLuma = ((rightRgb shr 16 and 0xFF) * 299 + (rightRgb shr 8 and 0xFF) * 587 + (rightRgb and 0xFF) * 114)
                if (leftLuma > rightLuma) {
                    bits = bits or (1UL shl bitIndex)
                }
                bitIndex++
            }
        }
        return bits.toString(16).padStart(16, '0')
    }

    private fun detectMagicMimeType(bytes: ByteArray): String? {
        if (bytes.size < 12) return null
        // JPEG: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return "image/jpeg"
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) {
            return "image/png"
        }
        // WebP: RIFF....WEBP
        if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return "image/webp"
        }
        return null
    }
}
