package com.telefam.company.app.ui.components

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.telefam.company.app.domain.ChatContactPayload
import com.telefam.company.app.domain.ChatDocumentCategory
import com.telefam.company.app.domain.ChatDocumentPayload
import com.telefam.company.app.domain.ChatLocationPayload
import com.telefam.company.app.domain.ChatMediaAttachmentItem
import com.telefam.company.app.security.TelefamAggressiveCompressionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.Locale
import java.util.UUID

data class DeviceLocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val placeTitle: String,
    val addressLine: String
)

object TelefamAttachmentDeviceHelper {

    const val MAX_ATTACHMENT_BYTES: Long = TelefamAggressiveCompressionEngine.MAX_BACKEND_FILE_SIZE_BYTES

    suspend fun importPickedMediaUris(
        context: Context,
        uris: List<Uri>
    ): List<ChatMediaAttachmentItem> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri -> importSingleMediaUri(context, uri) }
    }

    suspend fun importSingleMediaUri(
        context: Context,
        uri: Uri
    ): ChatMediaAttachmentItem? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val rawMime = resolver.getType(uri).orEmpty()
            val fileName = queryDisplayName(context, uri)
                ?: "media_${System.currentTimeMillis()}"
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(rawMime)
                ?: fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                .ifBlank { if (rawMime.startsWith("video/")) "mp4" else "jpg" }
            val mimeType = rawMime.ifBlank {
                if (ext in listOf("mp4", "mkv", "mov", "webm", "3gp")) "video/mp4" else "image/jpeg"
            }
            val isVideo = mimeType.startsWith("video/", ignoreCase = true)

            // Fast pre-check for non-image files exceeding 100 MB
            val declaredLength = TelefamAggressiveCompressionEngine.queryUriLengthOrNull(context, uri)
            if (isVideo && declaredLength != null && declaredLength > MAX_ATTACHMENT_BYTES) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Maximum allowed file size is 100 MB", Toast.LENGTH_SHORT).show()
                }
                return@runCatching null
            }

            val mediaDir = File(context.filesDir, "telefam_chat_attachments").apply { mkdirs() }
            val safeId = "att_${UUID.randomUUID()}"
            val destFile = File(mediaDir, "${safeId}.$ext")

            val streamCopied = resolver.openInputStream(uri)?.use { input ->
                TelefamAggressiveCompressionEngine.copyStreamBounded(
                    input = input,
                    destFile = destFile,
                    maxAllowedBytes = if (isVideo) MAX_ATTACHMENT_BYTES else MAX_ATTACHMENT_BYTES * 2L
                )
            } ?: false

            if (!streamCopied || !destFile.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Maximum allowed file size is 100 MB", Toast.LENGTH_SHORT).show()
                }
                return@runCatching null
            }

            buildMediaAttachmentFromFile(
                context = context,
                file = destFile,
                displayFileName = fileName,
                mimeType = mimeType,
                isVideo = isVideo
            )
        }.getOrNull()
    }

    fun createCameraCaptureTarget(
        context: Context,
        isVideo: Boolean
    ): Pair<File, Uri> {
        val dir = File(context.filesDir, "telefam_camera_captures").apply { mkdirs() }
        val ext = if (isVideo) "mp4" else "jpg"
        val file = File(dir, "cam_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$ext")
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        return file to uri
    }

    suspend fun importCapturedCameraFile(
        context: Context,
        file: File,
        isVideo: Boolean
    ): ChatMediaAttachmentItem? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0L) return@withContext null
        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
        val displayFileName = if (isVideo) {
            "Camera_Video_${System.currentTimeMillis()}.mp4"
        } else {
            "Camera_Photo_${System.currentTimeMillis()}.jpg"
        }
        buildMediaAttachmentFromFile(
            context = context,
            file = file,
            displayFileName = displayFileName,
            mimeType = mimeType,
            isVideo = isVideo
        )
    }

    private suspend fun buildMediaAttachmentFromFile(
        context: Context,
        file: File,
        displayFileName: String,
        mimeType: String,
        isVideo: Boolean
    ): ChatMediaAttachmentItem? {
        val itemId = "media_${UUID.randomUUID()}"
        var width = 0
        var height = 0
        var durationMs = 0L
        var resolvedMime = mimeType
        var thumbnailPath: String? = null

        if (isVideo) {
            val withinLimit = TelefamAggressiveCompressionEngine.compressAndEnforce100MbLimitOnFile(file)
            if (!withinLimit || !file.exists() || file.length() > MAX_ATTACHMENT_BYTES) {
                runCatching { file.delete() }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Maximum allowed file size is 100 MB", Toast.LENGTH_SHORT).show()
                }
                return null
            }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0
                val frameBmp = retriever.getFrameAtTime(200_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0L)
                if (frameBmp != null) {
                    val thumbDir = File(context.filesDir, "telefam_chat_thumbs").apply { mkdirs() }
                    val thumbFile = File(thumbDir, "thumb_${itemId}.webp")
                    TelefamAggressiveCompressionEngine.writeCompressedMicroThumbnail(frameBmp, thumbFile)
                    frameBmp.recycle()
                    thumbnailPath = thumbFile.absolutePath
                }
            } catch (_: Throwable) {
            } finally {
                runCatching { retriever.release() }
            }
        } else {
            // Silently and aggressively compress image in place on background thread
            val (compW, compH, compMime) = TelefamAggressiveCompressionEngine.compressImageFileAggressivelyInPlace(file)
            if (file.length() > MAX_ATTACHMENT_BYTES) {
                runCatching { file.delete() }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Maximum allowed file size is 100 MB", Toast.LENGTH_SHORT).show()
                }
                return null
            }
            width = compW
            height = compH
            resolvedMime = compMime

            // Also build a micro-thumbnail for instant chat list & bubble preview rendering
            runCatching {
                val decoded = BitmapFactory.decodeFile(file.absolutePath)
                if (decoded != null) {
                    val thumbDir = File(context.filesDir, "telefam_chat_thumbs").apply { mkdirs() }
                    val thumbFile = File(thumbDir, "thumb_${itemId}.webp")
                    if (TelefamAggressiveCompressionEngine.writeCompressedMicroThumbnail(decoded, thumbFile)) {
                        thumbnailPath = thumbFile.absolutePath
                    }
                    decoded.recycle()
                }
            }
            if (thumbnailPath == null) {
                thumbnailPath = file.absolutePath
            }
        }

        return ChatMediaAttachmentItem(
            itemId = itemId,
            localPath = file.absolutePath,
            thumbnailPath = thumbnailPath,
            mimeType = resolvedMime,
            fileName = displayFileName,
            fileSizeBytes = file.length(),
            widthPx = width,
            heightPx = height,
            durationMs = durationMs,
            isVideo = isVideo
        )
    }

    suspend fun importPickedDocumentUri(
        context: Context,
        uri: Uri,
        caption: String = ""
    ): ChatDocumentPayload? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val displayName = queryDisplayName(context, uri)
                ?: "Document_${System.currentTimeMillis()}"
            val ext = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            val resolvedMime = resolver.getType(uri)
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                ?: "application/octet-stream"

            val docsDir = File(context.filesDir, "telefam_chat_docs").apply { mkdirs() }
            val safeFileName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(docsDir, "${System.currentTimeMillis()}_$safeFileName")

            val copied = resolver.openInputStream(uri)?.use { input ->
                TelefamAggressiveCompressionEngine.copyStreamBounded(
                    input = input,
                    destFile = targetFile
                )
            } ?: false

            if (!copied || !targetFile.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Maximum allowed file size is 100 MB", Toast.LENGTH_SHORT).show()
                }
                return@runCatching null
            }

            // Silently compress in background & enforce 100 MB backend limit
            val withinLimit = TelefamAggressiveCompressionEngine.compressAndEnforce100MbLimitOnFile(targetFile)
            if (!withinLimit || !targetFile.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Maximum allowed file size is 100 MB", Toast.LENGTH_SHORT).show()
                }
                return@runCatching null
            }

            val category = ChatDocumentPayload.inferCategory(resolvedMime, displayName)
            var audioDurationMs = 0L
            if (category == ChatDocumentCategory.AUDIO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(targetFile.absolutePath)
                    audioDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                } catch (_: Throwable) {
                } finally {
                    runCatching { retriever.release() }
                }
            }

            val wireSize = TelefamAggressiveCompressionEngine.getEffectiveTransferByteSize(targetFile.absolutePath)

            ChatDocumentPayload(
                documentId = "doc_${UUID.randomUUID()}",
                localPath = targetFile.absolutePath,
                fileName = displayName,
                mimeType = resolvedMime,
                fileSizeBytes = wireSize.takeIf { it > 0L } ?: targetFile.length(),
                category = category,
                packageNameHint = if (category == ChatDocumentCategory.APK_APP) displayName.removeSuffix(".apk") else "",
                audioDurationMs = audioDurationMs,
                caption = caption.trim()
            )
        }.getOrNull()
    }

    suspend fun importPickedContactUri(
        context: Context,
        contactUri: Uri
    ): ChatContactPayload? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            var contactId = ""
            var displayName = ""

            resolver.query(
                contactUri,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    if (idIdx >= 0) contactId = cursor.getString(idIdx).orEmpty()
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx).orEmpty()
                }
            }

            val phones = mutableListOf<String>()
            val emails = mutableListOf<String>()
            var organization = ""

            if (contactId.isNotBlank()) {
                resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { pCursor ->
                    val numIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (pCursor.moveToNext()) {
                        if (numIdx >= 0) {
                            val raw = pCursor.getString(numIdx)?.trim().orEmpty()
                            if (raw.isNotBlank() && raw !in phones) phones.add(raw)
                        }
                    }
                }

                resolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                    "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { eCursor ->
                    val emailIdx = eCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    while (eCursor.moveToNext()) {
                        if (emailIdx >= 0) {
                            val raw = eCursor.getString(emailIdx)?.trim().orEmpty()
                            if (raw.isNotBlank() && raw !in emails) emails.add(raw)
                        }
                    }
                }

                resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Organization.COMPANY),
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
                    null
                )?.use { oCursor ->
                    val orgIdx = oCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
                    if (oCursor.moveToFirst() && orgIdx >= 0) {
                        organization = oCursor.getString(orgIdx)?.trim().orEmpty()
                    }
                }
            }

            val primaryPhone = phones.firstOrNull().orEmpty()
            val primaryEmail = emails.firstOrNull().orEmpty()
            ChatContactPayload(
                displayName = displayName.ifBlank { primaryPhone.ifBlank { "Device Contact" } },
                phoneNumber = primaryPhone,
                secondaryPhones = phones.drop(1),
                email = primaryEmail,
                organization = organization
            )
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentDeviceLocation(
        context: Context
    ): DeviceLocationSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@runCatching null
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                if (loc != null) {
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
            }
            val resolvedLoc = bestLocation ?: return@runCatching null
            val lat = resolvedLoc.latitude
            val lon = resolvedLoc.longitude
            val acc = resolvedLoc.accuracy.takeIf { it > 0f } ?: 12.0f

            var placeTitle = "Current GPS Location"
            var addressLine = String.format(Locale.US, "%.5f, %.5f", lat, lon)
            runCatching {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    val first = addresses?.firstOrNull()
                    if (first != null) {
                        placeTitle = first.featureName
                            ?.takeIf { it.isNotBlank() && !it.all { c -> c.isDigit() } }
                            ?: first.thoroughfare
                            ?: first.locality
                            ?: "Current GPS Location"
                        addressLine = first.getAddressLine(0)
                            ?: listOfNotNull(first.thoroughfare, first.locality, first.countryName)
                                .joinToString(", ")
                                .ifBlank { addressLine }
                    }
                }
            }

            DeviceLocationSnapshot(
                latitude = lat,
                longitude = lon,
                accuracyMeters = acc,
                placeTitle = placeTitle,
                addressLine = addressLine
            )
        }.getOrNull()
    }

    suspend fun saveMediaItemToDeviceDownloads(
        context: Context,
        item: ChatMediaAttachmentItem
    ): Boolean = withContext(Dispatchers.IO) {
        val srcFile = File(item.localPath)
        if (!srcFile.exists()) return@withContext false
        val saved = copyFileToDownloads(
            context = context,
            sourceFile = srcFile,
            fileName = item.fileName.ifBlank { srcFile.name },
            mimeType = item.mimeType.ifBlank { if (item.isVideo) "video/mp4" else "image/jpeg" }
        )
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                if (saved) "Saved ${item.fileName} to Downloads" else "Could not save file to device",
                Toast.LENGTH_SHORT
            ).show()
        }
        saved
    }

    suspend fun saveAllMediaItemsToDeviceDownloads(
        context: Context,
        items: List<ChatMediaAttachmentItem>
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (item in items) {
            val srcFile = File(item.localPath)
            if (srcFile.exists()) {
                val ok = copyFileToDownloads(
                    context = context,
                    sourceFile = srcFile,
                    fileName = item.fileName.ifBlank { srcFile.name },
                    mimeType = item.mimeType.ifBlank { if (item.isVideo) "video/mp4" else "image/jpeg" }
                )
                if (ok) count++
            }
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                if (count > 0) "Downloaded $count ${if (count == 1) "file" else "files"} to device"
                else "Unable to save files",
                Toast.LENGTH_SHORT
            ).show()
        }
        count
    }

    suspend fun saveDocumentToDeviceDownloads(
        context: Context,
        doc: ChatDocumentPayload
    ): Boolean = withContext(Dispatchers.IO) {
        val srcFile = File(doc.localPath)
        if (!srcFile.exists()) return@withContext false
        val ok = copyFileToDownloads(
            context = context,
            sourceFile = srcFile,
            fileName = doc.fileName.ifBlank { srcFile.name },
            mimeType = doc.mimeType
        )
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                if (ok) "Saved ${doc.fileName} to Downloads" else "Could not save ${doc.fileName}",
                Toast.LENGTH_SHORT
            ).show()
        }
        ok
    }

    private fun copyFileToDownloads(
        context: Context,
        sourceFile: File,
        fileName: String,
        mimeType: String
    ): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Telefam")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                val destDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Telefam"
                ).apply { mkdirs() }
                val destFile = File(destDir, fileName)
                sourceFile.copyTo(destFile, overwrite = true)
                true
            }
        }.getOrDefault(false)
    }

    fun openOrInstallDocumentFile(
        context: Context,
        doc: ChatDocumentPayload
    ) {
        runCatching {
            val file = File(doc.localPath)
            if (!file.exists()) {
                Toast.makeText(context, "File is not available on disk", Toast.LENGTH_SHORT).show()
                return
            }
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)
            val resolvedMime = if (doc.category == ChatDocumentCategory.APK_APP) {
                "application/vnd.android.package-archive"
            } else {
                doc.mimeType.ifBlank { "*/*" }
            }
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, resolvedMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, doc.fileName).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }.onFailure {
            Toast.makeText(context, "No application found to open ${doc.fileName}", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatBytesReadable(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return "${DecimalFormat("#.#").format(kb)} KB"
        val mb = kb / 1024.0
        if (mb < 1024.0) return "${DecimalFormat("#.##").format(mb)} MB"
        val gb = mb / 1024.0
        return "${DecimalFormat("#.##").format(gb)} GB"
    }

    /**
     * Builds an OpenStreetMap HTTPS URL for our secure in-app browser (`TelefamInAppBrowserDialog`)
     * with the server-issued live session token and coordinates embedded so locations NEVER open externally.
     */
    fun buildInAppLocationViewerUrl(location: ChatLocationPayload): String {
        val lat = String.format(Locale.US, "%.6f", location.latitude)
        val lon = String.format(Locale.US, "%.6f", location.longitude)
        val tokenParam = if (!location.liveSessionToken.isNullOrBlank()) {
            "&tf_live_token=${Uri.encode(location.liveSessionToken)}"
        } else ""
        return "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=16/$lat/$lon$tokenParam"
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            }.getOrNull()
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) result = it.getString(idx)
                }
            }
        }
        if (result.isNullOrBlank()) {
            result = uri.lastPathSegment?.substringAfterLast('/')
        }
        return result
    }
}
