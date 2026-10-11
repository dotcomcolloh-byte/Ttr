package com.telefam.company.app.domain

import android.graphics.Bitmap

enum class ChatMessageType(val wireValue: String) {
    TEXT("TEXT"),
    STICKER("STICKER"),
    GIF("GIF"),
    CLIP("CLIP"),
    VOICE_NOTE("VOICE_NOTE");

    companion object {
        fun fromWire(value: String?): ChatMessageType =
            entries.find { it.wireValue.equals(value, ignoreCase = true) } ?: TEXT
    }
}

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
