package com.telefam.company.app.domain

import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject

enum class ChatMessageType(val wireValue: String) {
    TEXT("TEXT"),
    STICKER("STICKER"),
    GIF("GIF"),
    CLIP("CLIP"),
    VOICE_NOTE("VOICE_NOTE"),
    IMAGE("IMAGE"),
    VIDEO("VIDEO"),
    MEDIA_ALBUM("MEDIA_ALBUM"),
    LOCATION("LOCATION"),
    CONTACT("CONTACT"),
    DOCUMENT("DOCUMENT"),
    POLL("POLL"),
    EVENT("EVENT");

    companion object {
        fun fromWire(value: String?): ChatMessageType =
            entries.find { it.wireValue.equals(value, ignoreCase = true) } ?: TEXT
    }
}

enum class ChatComposerAttachmentOption(
    val id: String,
    val label: String
) {
    GALLERY("GALLERY", "Gallery"),
    CAMERA("CAMERA", "Camera"),
    LOCATION("LOCATION", "Location"),
    CONTACT("CONTACT", "Contact"),
    DOCUMENT("DOCUMENT", "Document"),
    POLL("POLL", "Poll"),
    EVENT("EVENT", "Event")
}

data class ChatMediaAttachmentItem(
    val itemId: String = java.util.UUID.randomUUID().toString(),
    val localPath: String,
    val thumbnailPath: String? = null,
    val fileName: String = "",
    val mimeType: String = "image/jpeg",
    val fileSizeBytes: Long = 0L,
    val isVideo: Boolean = false,
    val durationMs: Long = 0L,
    val widthPx: Int = 1080,
    val heightPx: Int = 1080
) {
    val entryId: String get() = itemId
    val localFilePath: String get() = localPath
    val thumbnailFilePath: String? get() = thumbnailPath
    val byteSize: Long get() = fileSizeBytes
}

typealias ChatMediaAlbumEntry = ChatMediaAttachmentItem

data class ChatMediaAlbumPayload(
    val items: List<ChatMediaAttachmentItem>,
    val caption: String = ""
)

data class ChatLocationPayload(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 8.5f,
    val placeTitle: String = "Shared Location",
    val addressLine: String = "",
    val addressSubtitle: String = addressLine,
    val isLiveLocation: Boolean = false,
    val liveSessionToken: String? = null,
    val liveDurationMinutes: Int = 0,
    val expiresAtMs: Long? = null,
    val liveExpiresAtMs: Long? = expiresAtMs,
    val isViewOnce: Boolean = false,
    val caption: String = ""
) {
    fun isLiveActiveAt(nowMs: Long = System.currentTimeMillis()): Boolean {
        val exp = expiresAtMs ?: liveExpiresAtMs
        return isLiveLocation && (exp == null || exp > nowMs)
    }
}

data class ChatContactPayload(
    val displayName: String = "Shared Contact",
    val phoneNumber: String = "",
    val secondaryPhones: List<String> = emptyList(),
    val email: String = "",
    val organization: String = "",
    val photoUri: String? = null,
    val caption: String = "",
    val contactId: String = "contact_${displayName.hashCode()}",
    val fullName: String = displayName,
    val formattedPhone: String = phoneNumber,
    val dialablePhone: String = phoneNumber
)

enum class ChatDocumentCategory {
    DOC,
    DOCUMENT,
    AUDIO,
    APP_APK,
    APK_APP,
    ARCHIVE,
    SPREADSHEET,
    PRESENTATION,
    PDF,
    WORD_DOC,
    CODE_TEXT,
    OTHER_FILE,
    OTHER;

    companion object {
        fun inferCategory(fileName: String, mimeType: String): ChatDocumentCategory {
            val lowerName = fileName.lowercase()
            val lowerMime = mimeType.lowercase()
            return when {
                lowerName.endsWith(".apk") || lowerName.endsWith(".aab") ||
                    lowerMime == "application/vnd.android.package-archive" -> APK_APP
                lowerMime.startsWith("audio/") ||
                    lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") ||
                    lowerName.endsWith(".m4a") || lowerName.endsWith(".ogg") ||
                    lowerName.endsWith(".flac") || lowerName.endsWith(".aac") -> AUDIO
                lowerName.endsWith(".zip") || lowerName.endsWith(".rar") ||
                    lowerName.endsWith(".7z") || lowerName.endsWith(".tar") ||
                    lowerName.endsWith(".gz") || lowerMime.contains("zip") ||
                    lowerMime.contains("compressed") -> ARCHIVE
                lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx") ||
                    lowerName.endsWith(".csv") || lowerMime.contains("spreadsheet") ||
                    lowerMime.contains("excel") || lowerMime.contains("csv") -> SPREADSHEET
                lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx") ||
                    lowerMime.contains("presentation") || lowerMime.contains("powerpoint") -> PRESENTATION
                lowerName.endsWith(".pdf") || lowerMime.contains("pdf") -> PDF
                else -> DOCUMENT
            }
        }
    }
}

typealias ChatDocumentKind = ChatDocumentCategory

data class ChatDocumentPayload(
    val fileName: String,
    val localPath: String = "",
    val mimeType: String = "application/octet-stream",
    val fileSizeBytes: Long = 0L,
    val category: ChatDocumentCategory = ChatDocumentCategory.inferCategory(fileName, mimeType),
    val packageNameHint: String? = null,
    val audioDurationMs: Long = 0L,
    val sha256Checksum: String = "",
    val caption: String = "",
    val documentId: String = "doc_${fileName.hashCode()}",
    val localFilePath: String = localPath,
    val extension: String = fileName.substringAfterLast('.', "").uppercase(),
    val byteSize: Long = fileSizeBytes,
    val kind: ChatDocumentKind = category
) {
    val isApkPackage: Boolean
        get() = category == ChatDocumentCategory.APP_APK ||
            category == ChatDocumentCategory.APK_APP ||
            extension.equals("APK", ignoreCase = true) ||
            extension.equals("AAB", ignoreCase = true) ||
            mimeType.equals("application/vnd.android.package-archive", ignoreCase = true)

    val isAudioDocument: Boolean
        get() = category == ChatDocumentCategory.AUDIO || mimeType.startsWith("audio/", ignoreCase = true)

    companion object {
        fun inferCategory(mimeType: String, fileName: String): ChatDocumentCategory {
            return ChatDocumentCategory.inferCategory(fileName = fileName, mimeType = mimeType)
        }
    }
}

data class ChatPollOptionItem(
    val optionId: String,
    val text: String,
    val imageLocalPath: String? = null,
    val voterUserIds: List<String> = emptyList()
)

data class ChatPollPayload(
    val pollId: String,
    val question: String,
    val options: List<ChatPollOptionItem>,
    val allowMultipleVotes: Boolean = false,
    val isAnonymous: Boolean = false,
    val showVoteCounts: Boolean = true,
    val isClosed: Boolean = false,
    val createdByUserId: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
) {
    val totalVotes: Int
        get() = options.sumOf { it.voterUserIds.size }

    val totalUniqueVoters: Int
        get() = options.flatMap { it.voterUserIds }.distinct().size

    fun totalVotesCount(): Int = totalVotes
}

enum class ChatEventRsvpStatus(val wireValue: String, val label: String) {
    GOING("GOING", "Going"),
    MAYBE("MAYBE", "Maybe"),
    DECLINED("DECLINED", "Can't Go")
}

data class ChatEventPayload(
    val eventId: String,
    val title: String,
    val description: String = "",
    val startEpochMs: Long = System.currentTimeMillis() + 3600_000L,
    val endEpochMs: Long? = startEpochMs + 3600_000L,
    val locationLabel: String = "",
    val meetingLink: String = "",
    val isOnlineMeeting: Boolean = meetingLink.isNotBlank(),
    val coverImagePath: String? = null,
    val allowRsvp: Boolean = true,
    val rsvpGoingUserIds: List<String> = emptyList(),
    val rsvpMaybeUserIds: List<String> = emptyList(),
    val rsvpDeclinedUserIds: List<String> = emptyList(),
    val createdByUserId: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val startTimeMs: Long = startEpochMs,
    val endTimeMs: Long = endEpochMs ?: (startEpochMs + 3600_000L),
    val goingUserIds: List<String> = rsvpGoingUserIds,
    val maybeUserIds: List<String> = rsvpMaybeUserIds,
    val declinedUserIds: List<String> = rsvpDeclinedUserIds
) {
    fun rsvpForUser(userId: String): ChatEventRsvpStatus? = when {
        userId in goingUserIds || userId in rsvpGoingUserIds -> ChatEventRsvpStatus.GOING
        userId in maybeUserIds || userId in rsvpMaybeUserIds -> ChatEventRsvpStatus.MAYBE
        userId in declinedUserIds || userId in rsvpDeclinedUserIds -> ChatEventRsvpStatus.DECLINED
        else -> null
    }
}

object ChatAttachmentPayloadCodec {

    fun encodeAlbum(payload: ChatMediaAlbumPayload): String {
        val arr = JSONArray()
        payload.items.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("itemId", e.itemId)
                    put("entryId", e.itemId)
                    put("localPath", e.localPath)
                    put("localFilePath", e.localPath)
                    if (e.thumbnailPath != null) {
                        put("thumbnailPath", e.thumbnailPath)
                        put("thumbnailFilePath", e.thumbnailPath)
                    }
                    put("mimeType", e.mimeType)
                    put("isVideo", e.isVideo)
                    put("durationMs", e.durationMs)
                    put("widthPx", e.widthPx)
                    put("heightPx", e.heightPx)
                    put("fileSizeBytes", e.fileSizeBytes)
                    put("byteSize", e.fileSizeBytes)
                    put("fileName", e.fileName)
                }
            )
        }
        return JSONObject().apply {
            put("schema", "telefam_album_v1")
            put("caption", payload.caption)
            put("entries", arr)
        }.toString()
    }

    fun encodeMediaAlbum(entries: List<ChatMediaAttachmentItem>): String {
        return encodeAlbum(ChatMediaAlbumPayload(items = entries))
    }

    fun decodeAlbum(rawJson: String?): ChatMediaAlbumPayload? {
        if (rawJson.isNullOrBlank() || !rawJson.trim().startsWith("{")) return null
        return runCatching {
            val obj = JSONObject(rawJson)
            val caption = obj.optString("caption", "")
            val arr = obj.optJSONArray("entries") ?: obj.optJSONArray("items") ?: JSONArray()
            val items = buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val localPath = item.optString("localPath").ifBlank { item.optString("localFilePath", "") }
                    val thumbPath = item.optString("thumbnailPath").ifBlank { item.optString("thumbnailFilePath", "") }.takeIf { it.isNotBlank() }
                    add(
                        ChatMediaAttachmentItem(
                            itemId = item.optString("itemId").ifBlank { item.optString("entryId", "media_$i") },
                            localPath = localPath,
                            thumbnailPath = thumbPath,
                            fileName = item.optString("fileName", ""),
                            mimeType = item.optString("mimeType", "image/jpeg"),
                            fileSizeBytes = item.optLong("fileSizeBytes", item.optLong("byteSize", 0L)),
                            isVideo = item.optBoolean("isVideo", false),
                            durationMs = item.optLong("durationMs", 0L),
                            widthPx = item.optInt("widthPx", 1080),
                            heightPx = item.optInt("heightPx", 1080)
                        )
                    )
                }
            }
            if (items.isEmpty()) null else ChatMediaAlbumPayload(items = items, caption = caption)
        }.getOrNull()
    }

    fun decodeMediaAlbum(rawJson: String?, fallbackMessage: ChatMessageItem? = null): List<ChatMediaAttachmentItem> {
        val decoded = decodeAlbum(rawJson)
        if (decoded != null && decoded.items.isNotEmpty()) return decoded.items
        if (fallbackMessage != null) {
            val path = fallbackMessage.mediaLocalPath ?: fallbackMessage.mediaPreviewUrl ?: fallbackMessage.mediaUrl.orEmpty()
            if (path.isNotBlank()) {
                val isVid = fallbackMessage.messageType == ChatMessageType.VIDEO ||
                    fallbackMessage.mediaMimeType.startsWith("video/", ignoreCase = true)
                return listOf(
                    ChatMediaAttachmentItem(
                        itemId = fallbackMessage.mediaId ?: fallbackMessage.messageId,
                        localPath = path,
                        thumbnailPath = fallbackMessage.mediaPreviewUrl ?: path,
                        mimeType = fallbackMessage.mediaMimeType.ifBlank { if (isVid) "video/mp4" else "image/jpeg" },
                        isVideo = isVid,
                        durationMs = fallbackMessage.voiceDurationMs,
                        fileName = fallbackMessage.mediaTitle
                    )
                )
            }
        }
        return emptyList()
    }

    fun encodeLocation(payload: ChatLocationPayload): String {
        val addr = payload.addressLine.ifBlank { payload.addressSubtitle }
        val exp = payload.expiresAtMs ?: payload.liveExpiresAtMs
        return JSONObject().apply {
            put("schema", "telefam_location_v1")
            put("latitude", payload.latitude)
            put("longitude", payload.longitude)
            put("accuracyMeters", payload.accuracyMeters.toDouble())
            put("placeTitle", payload.placeTitle)
            put("addressLine", addr)
            put("addressSubtitle", addr)
            put("isLiveLocation", payload.isLiveLocation)
            if (payload.liveSessionToken != null) put("liveSessionToken", payload.liveSessionToken)
            put("liveDurationMinutes", payload.liveDurationMinutes)
            if (exp != null) {
                put("expiresAtMs", exp)
                put("liveExpiresAtMs", exp)
            }
            put("isViewOnce", payload.isViewOnce)
            put("caption", payload.caption)
        }.toString()
    }

    fun decodeLocation(rawJson: String?, fallbackTitle: String = "", fallbackCaption: String = ""): ChatLocationPayload? {
        if (rawJson.isNullOrBlank() || !rawJson.trim().startsWith("{")) return null
        return runCatching {
            val obj = JSONObject(rawJson)
            val addr = obj.optString("addressLine").ifBlank { obj.optString("addressSubtitle", "") }
            val exp = obj.optLong("expiresAtMs", obj.optLong("liveExpiresAtMs", 0L)).takeIf { it > 0L }
            ChatLocationPayload(
                latitude = obj.optDouble("latitude", -1.286389),
                longitude = obj.optDouble("longitude", 36.817223),
                accuracyMeters = obj.optDouble("accuracyMeters", 8.5).toFloat(),
                placeTitle = obj.optString("placeTitle", fallbackTitle.ifBlank { "Shared Location" }),
                addressLine = addr,
                addressSubtitle = addr,
                isLiveLocation = obj.optBoolean("isLiveLocation", false),
                liveSessionToken = obj.optString("liveSessionToken").takeIf { it.isNotBlank() },
                liveDurationMinutes = obj.optInt("liveDurationMinutes", 0),
                expiresAtMs = exp,
                liveExpiresAtMs = exp,
                isViewOnce = obj.optBoolean("isViewOnce", false),
                caption = obj.optString("caption", fallbackCaption)
            )
        }.getOrNull()
    }

    fun encodeContact(payload: ChatContactPayload): String {
        val phonesArr = JSONArray().apply {
            payload.secondaryPhones.forEach { put(it) }
        }
        val name = payload.displayName.ifBlank { payload.fullName }
        val phone = payload.phoneNumber.ifBlank { payload.formattedPhone }
        return JSONObject().apply {
            put("schema", "telefam_contact_v1")
            put("contactId", payload.contactId)
            put("displayName", name)
            put("fullName", name)
            put("phoneNumber", phone)
            put("formattedPhone", phone)
            put("dialablePhone", payload.dialablePhone.ifBlank { phone })
            put("secondaryPhones", phonesArr)
            put("email", payload.email)
            put("organization", payload.organization)
            if (payload.photoUri != null) put("photoUri", payload.photoUri)
            put("caption", payload.caption)
        }.toString()
    }

    fun decodeContact(rawJson: String?): ChatContactPayload? {
        if (rawJson.isNullOrBlank() || !rawJson.trim().startsWith("{")) return null
        return runCatching {
            val obj = JSONObject(rawJson)
            val phonesArr = obj.optJSONArray("secondaryPhones") ?: JSONArray()
            val sec = buildList {
                for (i in 0 until phonesArr.length()) add(phonesArr.getString(i))
            }
            val name = obj.optString("displayName").ifBlank { obj.optString("fullName", "Shared Contact") }
            val phone = obj.optString("phoneNumber").ifBlank { obj.optString("formattedPhone", "") }
            ChatContactPayload(
                displayName = name,
                phoneNumber = phone,
                secondaryPhones = sec,
                email = obj.optString("email", ""),
                organization = obj.optString("organization", ""),
                photoUri = obj.optString("photoUri").takeIf { it.isNotBlank() },
                caption = obj.optString("caption", ""),
                contactId = obj.optString("contactId", "contact_1"),
                fullName = name,
                formattedPhone = phone,
                dialablePhone = obj.optString("dialablePhone", phone)
            )
        }.getOrNull()
    }

    fun encodeDocument(payload: ChatDocumentPayload): String {
        val path = payload.localPath.ifBlank { payload.localFilePath }
        val size = if (payload.fileSizeBytes > 0L) payload.fileSizeBytes else payload.byteSize
        return JSONObject().apply {
            put("schema", "telefam_document_v1")
            put("documentId", payload.documentId)
            put("fileName", payload.fileName)
            put("localPath", path)
            put("localFilePath", path)
            put("mimeType", payload.mimeType)
            put("extension", payload.extension)
            put("fileSizeBytes", size)
            put("byteSize", size)
            put("category", payload.category.name)
            put("kind", payload.category.name)
            if (payload.packageNameHint != null) put("packageNameHint", payload.packageNameHint)
            put("audioDurationMs", payload.audioDurationMs)
            put("sha256Checksum", payload.sha256Checksum)
            put("caption", payload.caption)
        }.toString()
    }

    fun decodeDocument(rawJson: String?, fallbackMessage: ChatMessageItem? = null): ChatDocumentPayload? {
        if (!rawJson.isNullOrBlank() && rawJson.trim().startsWith("{")) {
            val parsed = runCatching {
                val obj = JSONObject(rawJson)
                val catStr = obj.optString("category").ifBlank { obj.optString("kind", ChatDocumentCategory.OTHER.name) }
                val cat = ChatDocumentCategory.entries.find { it.name == catStr } ?: ChatDocumentCategory.OTHER
                val path = obj.optString("localPath").ifBlank { obj.optString("localFilePath", fallbackMessage?.mediaLocalPath.orEmpty()) }
                val size = obj.optLong("fileSizeBytes", obj.optLong("byteSize", 0L))
                val fName = obj.optString("fileName", "Document")
                ChatDocumentPayload(
                    fileName = fName,
                    localPath = path,
                    mimeType = obj.optString("mimeType", "application/octet-stream"),
                    fileSizeBytes = size,
                    category = cat,
                    packageNameHint = obj.optString("packageNameHint").takeIf { it.isNotBlank() },
                    audioDurationMs = obj.optLong("audioDurationMs", 0L),
                    sha256Checksum = obj.optString("sha256Checksum", ""),
                    caption = obj.optString("caption", fallbackMessage?.decryptedText.orEmpty()),
                    documentId = obj.optString("documentId", "doc_1"),
                    localFilePath = path,
                    extension = obj.optString("extension", fName.substringAfterLast('.', "FILE").uppercase()),
                    byteSize = size,
                    kind = cat
                )
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    fun encodePoll(payload: ChatPollPayload): String {
        val optArr = JSONArray()
        payload.options.forEach { opt ->
            val votersArr = JSONArray().apply {
                opt.voterUserIds.forEach { put(it) }
            }
            optArr.put(
                JSONObject().apply {
                    put("optionId", opt.optionId)
                    put("text", opt.text)
                    if (!opt.imageLocalPath.isNullOrBlank()) put("imageLocalPath", opt.imageLocalPath)
                    put("voterUserIds", votersArr)
                }
            )
        }
        return JSONObject().apply {
            put("schema", "telefam_poll_v1")
            put("pollId", payload.pollId)
            put("question", payload.question)
            put("options", optArr)
            put("allowMultipleVotes", payload.allowMultipleVotes)
            put("isAnonymous", payload.isAnonymous)
            put("showVoteCounts", payload.showVoteCounts)
            put("isClosed", payload.isClosed)
            put("createdByUserId", payload.createdByUserId)
            put("createdAtMs", payload.createdAtMs)
        }.toString()
    }

    fun decodePoll(rawJson: String?): ChatPollPayload? {
        if (rawJson.isNullOrBlank() || !rawJson.trim().startsWith("{")) return null
        return runCatching {
            val obj = JSONObject(rawJson)
            val optArr = obj.optJSONArray("options") ?: JSONArray()
            val options = buildList {
                for (i in 0 until optArr.length()) {
                    val o = optArr.getJSONObject(i)
                    val vArr = o.optJSONArray("voterUserIds") ?: JSONArray()
                    val voters = buildList {
                        for (j in 0 until vArr.length()) {
                            val uid = vArr.optString(j)
                            if (uid.isNotBlank()) add(uid)
                        }
                    }
                    add(
                        ChatPollOptionItem(
                            optionId = o.optString("optionId", "opt_$i"),
                            text = o.optString("text", "Option ${i + 1}"),
                            imageLocalPath = o.optString("imageLocalPath").takeIf { it.isNotBlank() },
                            voterUserIds = voters
                        )
                    )
                }
            }
            ChatPollPayload(
                pollId = obj.optString("pollId", "poll_1"),
                question = obj.optString("question", "Poll"),
                options = options,
                allowMultipleVotes = obj.optBoolean("allowMultipleVotes", false),
                isAnonymous = obj.optBoolean("isAnonymous", false),
                showVoteCounts = obj.optBoolean("showVoteCounts", true),
                isClosed = obj.optBoolean("isClosed", false),
                createdByUserId = obj.optString("createdByUserId", ""),
                createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    fun encodeEvent(payload: ChatEventPayload): String {
        fun listToJson(list: List<String>) = JSONArray().apply { list.forEach { put(it) } }
        val going = payload.goingUserIds.ifEmpty { payload.rsvpGoingUserIds }
        val maybe = payload.maybeUserIds.ifEmpty { payload.rsvpMaybeUserIds }
        val declined = payload.declinedUserIds.ifEmpty { payload.rsvpDeclinedUserIds }
        val startMs = if (payload.startEpochMs > 0L) payload.startEpochMs else payload.startTimeMs
        val endMs = payload.endEpochMs ?: payload.endTimeMs
        return JSONObject().apply {
            put("schema", "telefam_event_v1")
            put("eventId", payload.eventId)
            put("title", payload.title)
            put("description", payload.description)
            put("startEpochMs", startMs)
            put("startTimeMs", startMs)
            put("endEpochMs", endMs)
            put("endTimeMs", endMs)
            put("locationLabel", payload.locationLabel)
            put("meetingLink", payload.meetingLink)
            put("isOnlineMeeting", payload.isOnlineMeeting)
            if (!payload.coverImagePath.isNullOrBlank()) put("coverImagePath", payload.coverImagePath)
            put("allowRsvp", payload.allowRsvp)
            put("goingUserIds", listToJson(going))
            put("rsvpGoingUserIds", listToJson(going))
            put("maybeUserIds", listToJson(maybe))
            put("rsvpMaybeUserIds", listToJson(maybe))
            put("declinedUserIds", listToJson(declined))
            put("rsvpDeclinedUserIds", listToJson(declined))
            put("createdByUserId", payload.createdByUserId)
            put("createdAtMs", payload.createdAtMs)
        }.toString()
    }

    fun decodeEvent(rawJson: String?): ChatEventPayload? {
        if (rawJson.isNullOrBlank() || !rawJson.trim().startsWith("{")) return null
        return runCatching {
            val obj = JSONObject(rawJson)
            fun jsonToList(key1: String, key2: String): List<String> {
                val arr = obj.optJSONArray(key1) ?: obj.optJSONArray(key2) ?: JSONArray()
                return buildList {
                    for (i in 0 until arr.length()) {
                        val v = arr.optString(i)
                        if (v.isNotBlank()) add(v)
                    }
                }
            }
            val startMs = obj.optLong("startEpochMs", obj.optLong("startTimeMs", System.currentTimeMillis()))
            val endMs = obj.optLong("endEpochMs", obj.optLong("endTimeMs", startMs + 3600_000L))
            val going = jsonToList("goingUserIds", "rsvpGoingUserIds")
            val maybe = jsonToList("maybeUserIds", "rsvpMaybeUserIds")
            val declined = jsonToList("declinedUserIds", "rsvpDeclinedUserIds")
            val meetingLink = obj.optString("meetingLink", "")
            ChatEventPayload(
                eventId = obj.optString("eventId", "evt_1"),
                title = obj.optString("title", "Event"),
                description = obj.optString("description", ""),
                startEpochMs = startMs,
                endEpochMs = endMs,
                locationLabel = obj.optString("locationLabel", ""),
                meetingLink = meetingLink,
                isOnlineMeeting = obj.optBoolean("isOnlineMeeting", meetingLink.isNotBlank()),
                coverImagePath = obj.optString("coverImagePath").takeIf { it.isNotBlank() },
                allowRsvp = obj.optBoolean("allowRsvp", true),
                rsvpGoingUserIds = going,
                rsvpMaybeUserIds = maybe,
                rsvpDeclinedUserIds = declined,
                createdByUserId = obj.optString("createdByUserId", ""),
                createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis()),
                startTimeMs = startMs,
                endTimeMs = endMs,
                goingUserIds = going,
                maybeUserIds = maybe,
                declinedUserIds = declined
            )
        }.getOrNull()
    }
}

typealias TelefamAttachmentPayloadCodec = ChatAttachmentPayloadCodec

enum class ChatMessageDeliveryStatus(val wireValue: String) {
    SENDING("SENDING"),
    SENT("SENT"),
    DELIVERED("DELIVERED"),
    SEEN("SEEN"),
    REJECTED_BLOCKED("REJECTED_BLOCKED"),
    REJECTED_USER_NOT_FOUND("REJECTED_USER_NOT_FOUND");

    companion object {
        fun fromWire(value: String?): ChatMessageDeliveryStatus =
            entries.find { it.wireValue.equals(value, ignoreCase = true) } ?: SENT
    }
}

typealias MessageDeliveryStatus = ChatMessageDeliveryStatus

enum class ChatReactionChoice(val key: String, val label: String) {
    HEART("HEART", "Love"),
    THUMBS_UP("THUMBS_UP", "Like"),
    FIRE("FIRE", "Fire"),
    LAUGH("LAUGH", "Haha"),
    SURPRISED("SURPRISED", "Wow"),
    PRAY("PRAY", "Thanks");

    val wireKey: String get() = key

    companion object {
        fun fromWire(key: String?): ChatReactionChoice =
            entries.find { it.key.equals(key, ignoreCase = true) } ?: HEART

        fun fromKey(key: String?): ChatReactionChoice = fromWire(key)
    }
}

enum class ChatDisappearingOption(val durationSeconds: Long, val displayLabel: String) {
    HOURS_24(86_400L, "24 hours"),
    DAYS_7(604_800L, "7 days"),
    MONTH_1(2_592_000L, "1 month"),
    OFF(0L, "Off");

    val label: String get() = displayLabel

    companion object {
        fun fromSeconds(seconds: Long): ChatDisappearingOption =
            entries.find { it.durationSeconds == seconds } ?: OFF
    }
}

enum class KlipyMediaTab(
    val wireValue: String,
    val displayTitle: String,
    val label: String,
    val subtitle: String,
    val searchPlaceholder: String
) {
    STICKERS("STICKERS", "Stickers", "Stickers", "Stickers", "Search stickers..."),
    GIFS("GIFS", "GIFs", "GIFs", "GIFs", "Search GIFs..."),
    CLIPS("CLIPS", "Clips", "Clips", "Clips", "Search clips...");

    companion object {
        fun fromWire(value: String?): KlipyMediaTab =
            entries.find { it.wireValue.equals(value, ignoreCase = true) } ?: STICKERS
    }
}

data class KlipyMediaItem(
    val itemId: String,
    val mediaTab: KlipyMediaTab,
    val title: String,
    val mediaUrl: String,
    val previewUrl: String,
    val localCachedPath: String? = null,
    val mimeType: String = "image/gif",
    val widthPx: Int = 240,
    val heightPx: Int = 240,
    val durationMs: Long = 2400L,
    val accentHex: String = "#C80815",
    val isRecent: Boolean = false
) {
    val id: String get() = itemId
    val tab: KlipyMediaTab get() = mediaTab
}

data class OneToOneChatListItem(
    val chatId: String,
    val ownerUserId: String,
    val peerUserId: String,
    val peerFullName: String,
    val peerUsername: String,
    val peerAvatarUrl: String? = null,
    val peerBio: String = "",
    val peerIsOnline: Boolean = false,
    val peerLastSeenMs: Long? = null,
    val peerIsTyping: Boolean = false,
    val peerIsDeleted: Boolean = false,
    val isBlockedByMe: Boolean = false,
    val isBlockedByPeer: Boolean = false,
    val lastMessageId: String? = null,
    val lastMessageType: ChatMessageType = ChatMessageType.TEXT,
    val lastMessagePreview: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageDeliveryStatus: ChatMessageDeliveryStatus = ChatMessageDeliveryStatus.SENT,
    val lastMessageMediaIconUrl: String? = null,
    val lastMessageTimestampMs: Long = 0L,
    val unreadCount: Int = 0,
    val disappearingOption: ChatDisappearingOption = ChatDisappearingOption.OFF,
    val e2eeSafetyNumberVerified: Boolean = false,
    val draftText: String = ""
) {
    val isBlockedEitherWay: Boolean
        get() = isBlockedByMe || isBlockedByPeer

    val canSendMessages: Boolean
        get() = !peerIsDeleted && !isBlockedEitherWay
}

data class ChatMessageReactionItem(
    val messageId: String,
    val reactorUserId: String,
    val reactionKey: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class ChatMessageItem(
    val messageId: String,
    val chatId: String,
    val ownerUserId: String,
    val senderUserId: String,
    val recipientUserId: String,
    val messageType: ChatMessageType,
    val decryptedText: String = "",
    val mediaId: String? = null,
    val mediaUrl: String? = null,
    val mediaPreviewUrl: String? = null,
    val mediaLocalPath: String? = null,
    val mediaTitle: String = "",
    val mediaMimeType: String = "",
    val voiceDurationMs: Long = 0L,
    val voiceWaveformSamples: List<Float> = emptyList(),
    val isViewOnce: Boolean = false,
    val isViewOnceConsumed: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToPreviewText: String? = null,
    val replyToMessageType: ChatMessageType? = null,
    val isForwarded: Boolean = false,
    val isEdited: Boolean = false,
    val editedAtMs: Long? = null,
    val isPinned: Boolean = false,
    val isFavourited: Boolean = false,
    val reactions: List<ChatMessageReactionItem> = emptyList(),
    val deliveryStatus: ChatMessageDeliveryStatus = ChatMessageDeliveryStatus.SENDING,
    val ciphertextBase64: String = "",
    val ratchetHeaderJson: String = "",
    val expiresAtMs: Long? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    val deliveredAtMs: Long? = null,
    val seenAtMs: Long? = null
) {
    val isOutgoing: Boolean
        get() = senderUserId == ownerUserId

    fun isOutgoing(currentUserId: String): Boolean = senderUserId == currentUserId

    fun isEditableAt(nowMs: Long = System.currentTimeMillis()): Boolean {
        return senderUserId == ownerUserId &&
            messageType == ChatMessageType.TEXT &&
            !isViewOnce &&
            !isViewOnceConsumed &&
            (nowMs - createdAtMs) <= 24L * 60 * 60 * 1000L
    }

    fun isUnsendableAt(nowMs: Long = System.currentTimeMillis()): Boolean {
        return senderUserId == ownerUserId &&
            (nowMs - createdAtMs) <= 24L * 60 * 60 * 1000L
    }
}

enum class ChatAttachmentType {
    MEDIA,
    LINKS,
    DOCS
}

typealias ChatAttachmentCategory = ChatAttachmentType

data class ChatAttachmentItem(
    val attachmentId: String,
    val messageId: String,
    val chatId: String,
    val type: ChatAttachmentType,
    val title: String,
    val subtitleOrUrl: String,
    val previewUrl: String? = null,
    val mimeType: String = "",
    val timestampMs: Long = System.currentTimeMillis()
) {
    val category: ChatAttachmentCategory get() = type
    val subtitle: String get() = subtitleOrUrl
}

data class E2eeVerificationSnapshot(
    val chatId: String,
    val localUserId: String,
    val peerUserId: String,
    val peerFullName: String,
    val peerUsername: String,
    val localIdentityPublicKeyFingerprintHex: String,
    val peerIdentityPublicKeyFingerprintHex: String,
    val safetyNumberGroups: List<String>, // 12 groups of 5 digits = 60 digits
    val qrCodePayload: String,
    val qrCodeBitmap: Bitmap? = null,
    val ratchetSendCount: Int = 1,
    val ratchetRecvCount: Int = 1,
    val protocolDescription: String = "Signal Protocol (X3DH + Double Ratchet + AES-256-GCM)",
    val isVerified: Boolean = false
) {
    val peerDisplayName: String get() = peerFullName
    val localIdentityFingerprintHex: String get() = localIdentityPublicKeyFingerprintHex
    val peerIdentityFingerprintHex: String get() = peerIdentityPublicKeyFingerprintHex
    val qrVerificationPayload: String get() = qrCodePayload
    val qrPayload: String get() = qrCodePayload
    val safetyNumberFormatted: String
        get() = safetyNumberGroups.chunked(4).joinToString("\n") { it.joinToString("  ") }
    val sendingChainLength: Int get() = ratchetSendCount
    val receivingChainLength: Int get() = ratchetRecvCount
}

typealias ForwardRecipientTarget = com.telefam.company.app.data.ForwardRecipientTarget
