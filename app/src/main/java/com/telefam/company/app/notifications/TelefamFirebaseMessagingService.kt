package com.telefam.company.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.telefam.company.app.data.ChatEntity
import com.telefam.company.app.data.ChatMessageEntity
import com.telefam.company.app.data.TelefamAppDatabase
import com.telefam.company.app.security.TelefamSignalE2eeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Production FCM Wake & Data Push Service (`com.google.firebase.MESSAGING_EVENT`).
 * - Handles token rotation via `onNewToken` and registers with `/v1/notifications/fcm-token/register`.
 * - Processes data-only E2EE push payloads (`INCOMING_MESSAGE`, `UNSEND`, `MARK_READ_SYNC`,
 *   `INCOMING_CALL`, `LIVE_LOCATION`, `SOCIAL_ACTIVITY`, `SECURITY_DEVICE_ALERT`) when the user
 *   is outside the app, decrypting locally via `TelefamSignalE2eeEngine` so Firebase never sees plaintext.
 */
class TelefamFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val appContext = applicationContext
        serviceScope.launch {
            val dao = TelefamAppDatabase.getInstance(appContext).telefamDao()
            val activeSession = dao.getLatestActiveSession(System.currentTimeMillis())
            val ownerUserId = activeSession?.userId.orEmpty()
            if (ownerUserId.isNotBlank()) {
                TelefamNotificationEngine.syncFcmRegistrationToken(
                    context = appContext,
                    ownerUserId = ownerUserId,
                    explicitToken = token
                )
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        if (data.isEmpty()) return

        val appContext = applicationContext
        serviceScope.launch {
            val dao = TelefamAppDatabase.getInstance(appContext).telefamDao()
            val activeSession = dao.getLatestActiveSession(System.currentTimeMillis())
            val ownerUserId = data["recipientUserId"]?.takeIf { it.isNotBlank() }
                ?: activeSession?.userId.orEmpty()
            if (ownerUserId.isBlank()) return@launch

            val accountSettings = dao.getAccountSettingsAndSecurity(ownerUserId)
            if (accountSettings?.pushMasterEnabled == false) return@launch

            when (val eventType = data["eventType"]?.uppercase().orEmpty()) {
                "INCOMING_MESSAGE", "E2EE_CHAT_MESSAGE" -> {
                    if (accountSettings?.notifyDirectMessages == false) return@launch
                    val chatId = data["chatId"].orEmpty()
                    val messageId = data["messageId"].orEmpty()
                    val senderUserId = data["senderUserId"].orEmpty()
                    val ciphertextBase64 = data["ciphertextBase64"].orEmpty()
                    val ratchetHeaderJson = data["ratchetHeaderJson"].orEmpty()
                    if (chatId.isBlank() || messageId.isBlank() || senderUserId.isBlank() || ciphertextBase64.isBlank()) {
                        return@launch
                    }

                    // Avoid duplicate decryption if WebSocket already processed this message
                    val existingMsg = dao.getMessageById(messageId)
                    if (existingMsg != null) {
                        TelefamNotificationEngine.postIncomingChatMessageNotification(
                            context = appContext,
                            ownerUserId = ownerUserId,
                            message = existingMsg
                        )
                        return@launch
                    }

                    val signalEngine = TelefamSignalE2eeEngine(dao)
                    val decrypted = signalEngine.decryptEnvelope(
                        ownerUserId = ownerUserId,
                        peerUserId = senderUserId,
                        ciphertextBase64 = ciphertextBase64,
                        ratchetHeaderJson = ratchetHeaderJson,
                        isOutgoingFromMe = false
                    ) ?: return@launch

                    val now = System.currentTimeMillis()
                    val disappearing = dao.getDisappearingSetting(chatId)?.durationSeconds ?: 0L
                    val expiresAt = if (disappearing > 0L) now + disappearing * 1000L else null

                    val msgEntity = ChatMessageEntity(
                        messageId = messageId,
                        chatId = chatId,
                        ownerUserId = ownerUserId,
                        senderUserId = senderUserId,
                        recipientUserId = ownerUserId,
                        messageType = decrypted.messageType,
                        decryptedText = decrypted.text,
                        mediaId = decrypted.mediaId,
                        mediaUrl = decrypted.mediaUrl,
                        mediaPreviewUrl = decrypted.mediaPreviewUrl,
                        mediaTitle = decrypted.mediaTitle,
                        mediaMimeType = decrypted.mediaMimeType,
                        voiceDurationMs = decrypted.voiceDurationMs,
                        voiceWaveformCsv = decrypted.voiceWaveformCsv,
                        isViewOnce = decrypted.isViewOnce,
                        replyToMessageId = decrypted.replyToMessageId,
                        replyToSenderName = decrypted.replyToSenderName,
                        replyToPreviewText = decrypted.replyToPreviewText,
                        replyToMessageType = decrypted.replyToMessageType,
                        isForwarded = decrypted.isForwarded,
                        ciphertextBase64 = ciphertextBase64,
                        ratchetHeaderJson = ratchetHeaderJson,
                        deliveryStatus = "DELIVERED",
                        deliveredAtMs = now,
                        expiresAtMs = expiresAt,
                        createdAtMs = now
                    )
                    dao.upsertChatMessage(msgEntity)

                    val previewLine = TelefamNotificationPreviewRenderer.buildNotificationSummaryLine(msgEntity)
                    val existingChat = dao.getChatById(chatId)
                    if (existingChat != null) {
                        dao.upsertChat(
                            existingChat.copy(
                                lastMessageId = messageId,
                                lastMessageType = decrypted.messageType,
                                lastMessagePreview = previewLine,
                                lastMessageSenderId = senderUserId,
                                lastMessageDeliveryStatus = "DELIVERED",
                                lastMessageMediaIconUrl = decrypted.mediaPreviewUrl ?: decrypted.mediaUrl,
                                lastMessageTimestampMs = now,
                                unreadCount = existingChat.unreadCount + 1,
                                updatedAtMs = now
                            )
                        )
                    } else {
                        val senderProfile = dao.getProfileByUserId(senderUserId)
                        val senderName = data["senderDisplayName"]?.takeIf { it.isNotBlank() }
                            ?: senderProfile?.fullName?.takeIf { it.isNotBlank() }
                            ?: "Telefam Contact"
                        val senderUsername = senderProfile?.username.orEmpty()
                        dao.upsertChat(
                            ChatEntity(
                                chatId = chatId,
                                ownerUserId = ownerUserId,
                                peerUserId = senderUserId,
                                peerFullName = senderName,
                                peerUsername = senderUsername,
                                peerAvatarUrl = senderProfile?.avatarLocalPath ?: senderProfile?.avatarCdnUrl,
                                peerBio = senderProfile?.bio.orEmpty(),
                                lastMessageId = messageId,
                                lastMessageType = decrypted.messageType,
                                lastMessagePreview = previewLine,
                                lastMessageSenderId = senderUserId,
                                lastMessageDeliveryStatus = "DELIVERED",
                                lastMessageMediaIconUrl = decrypted.mediaPreviewUrl ?: decrypted.mediaUrl,
                                lastMessageTimestampMs = now,
                                unreadCount = 1,
                                createdAtMs = now,
                                updatedAtMs = now
                            )
                        )
                    }

                    TelefamNotificationEngine.postIncomingChatMessageNotification(
                        context = appContext,
                        ownerUserId = ownerUserId,
                        message = msgEntity
                    )
                }

                "UNSEND" -> {
                    val chatId = data["chatId"].orEmpty()
                    val messageId = data["messageId"].orEmpty()
                    if (messageId.isNotBlank()) {
                        dao.deleteMessageHard(messageId)
                        dao.deleteReactionsForMessage(messageId)
                        if (chatId.isNotBlank()) {
                            dao.deletePin(chatId, messageId)
                        }
                        dao.deleteFavouritesForMessage(messageId)
                        TelefamNotificationEngine.onMessageUnsentOrDeleted(
                            context = appContext,
                            ownerUserId = ownerUserId,
                            chatId = chatId,
                            messageId = messageId
                        )
                    }
                }

                "MARK_READ_SYNC" -> {
                    val chatId = data["chatId"].orEmpty()
                    if (chatId.isNotBlank()) {
                        dao.clearChatUnreadCount(chatId)
                        TelefamNotificationEngine.cancelChatNotification(appContext, chatId)
                    }
                }

                "INCOMING_CALL", "LIVE_LOCATION" -> {
                    if (accountSettings?.notifyLiveNotifications == false) return@launch
                    val chatId = data["chatId"].orEmpty()
                    val peerUserId = data["senderUserId"].orEmpty()
                    val peerName = data["senderDisplayName"].orEmpty().ifBlank { "Telefam Contact" }
                    val isCall = eventType == "INCOMING_CALL"
                    val isVideo = data["isVideoCall"]?.toBoolean() ?: false
                    val subtitle = data["subtitle"].orEmpty()
                    TelefamNotificationEngine.postIncomingCallOrLiveLocationNotification(
                        context = appContext,
                        ownerUserId = ownerUserId,
                        chatId = chatId,
                        peerUserId = peerUserId,
                        peerDisplayName = peerName,
                        isCall = isCall,
                        isVideoCall = isVideo,
                        subtitle = subtitle
                    )
                }

                "SOCIAL_ACTIVITY" -> {
                    val kind = data["activityKind"].orEmpty().uppercase()
                    val allowed = when (kind) {
                        "FOLLOW" -> accountSettings?.notifyNewFollowers != false
                        "LIKE" -> accountSettings?.notifyLikes != false
                        "COMMENT" -> accountSettings?.notifyComments != false
                        else -> true
                    }
                    if (!allowed) return@launch
                    val actorUserId = data["actorUserId"].orEmpty()
                    val actorName = data["actorDisplayName"].orEmpty().ifBlank { "Telefam Member" }
                    val commentPreview = data["commentPreview"].orEmpty()
                    val postId = data["postId"]
                    TelefamNotificationEngine.postSocialActivityNotification(
                        context = appContext,
                        ownerUserId = ownerUserId,
                        actorUserId = actorUserId,
                        actorDisplayName = actorName,
                        activityKind = kind,
                        commentPreview = commentPreview,
                        postId = postId
                    )
                }

                "SECURITY_DEVICE_ALERT" -> {
                    val title = data["title"].orEmpty().ifBlank { "New Device Sign-In Detected" }
                    val body = data["body"].orEmpty().ifBlank {
                        "Your Telefam account was accessed from a new device. Tap to review trusted devices."
                    }
                    TelefamNotificationEngine.postSecurityDeviceLoginNotification(
                        context = appContext,
                        ownerUserId = ownerUserId,
                        title = title,
                        body = body
                    )
                }
            }
        }
    }
}
