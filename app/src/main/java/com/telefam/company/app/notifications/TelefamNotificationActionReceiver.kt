package com.telefam.company.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.telefam.company.app.data.ChatMessageEntity
import com.telefam.company.app.data.ChatOutboxEntity
import com.telefam.company.app.data.TelefamAppDatabase
import com.telefam.company.app.data.TelefamPrivacySyncWorker
import com.telefam.company.app.domain.ChatMessageType
import com.telefam.company.app.security.TelefamAggressiveCompressionEngine
import com.telefam.company.app.security.TelefamCryptoAndTelemetry
import com.telefam.company.app.security.TelefamSignalE2eeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/**
 * Handles inline notification actions directly from the Android status bar:
 * - Inline Reply (E2EE Signal Double Ratchet encryption + Room persistence + Outbox sync)
 * - Mark as Read (Updates Room + queues SEEN_RECEIPT + removes notification from status bar)
 * - Mute Conversation (Mutes chat notifications + removes notification from status bar)
 * - Dismiss Urgent Call / Live Location alert
 */
class TelefamNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    TelefamNotificationEngine.ACTION_MARK_AS_READ -> {
                        handleMarkAsRead(appContext, intent)
                    }
                    TelefamNotificationEngine.ACTION_DIRECT_REPLY -> {
                        handleDirectReply(appContext, intent)
                    }
                    TelefamNotificationEngine.ACTION_MUTE_CHAT -> {
                        val chatId = intent.getStringExtra(TelefamNotificationEngine.EXTRA_CHAT_ID).orEmpty()
                        if (chatId.isNotBlank()) {
                            TelefamNotificationEngine.setChatMuted(appContext, chatId, true)
                        }
                    }
                    TelefamNotificationEngine.ACTION_DISMISS_CALL_OR_LIVE -> {
                        val notifId = intent.getIntExtra(TelefamNotificationEngine.EXTRA_NOTIFICATION_ID, -1)
                        if (notifId > 0) {
                            runCatching { NotificationManagerCompat.from(appContext).cancel(notifId) }
                        }
                    }
                }
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }

    private suspend fun handleMarkAsRead(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra(TelefamNotificationEngine.EXTRA_CHAT_ID).orEmpty()
        val peerUserId = intent.getStringExtra(TelefamNotificationEngine.EXTRA_PEER_USER_ID).orEmpty()
        val ownerUserId = intent.getStringExtra(TelefamNotificationEngine.EXTRA_OWNER_USER_ID).orEmpty()
        if (chatId.isBlank()) return

        // Immediately remove the notification from the Android status bar
        TelefamNotificationEngine.cancelChatNotification(context, chatId)

        val dao = TelefamAppDatabase.getInstance(context).telefamDao()
        val resolvedOwnerId = ownerUserId.ifBlank {
            dao.getChatById(chatId)?.ownerUserId
                ?: dao.getLatestActiveSession(System.currentTimeMillis())?.userId.orEmpty()
        }
        val resolvedPeerId = peerUserId.ifBlank {
            dao.getChatById(chatId)?.peerUserId.orEmpty()
        }

        dao.clearChatUnreadCount(chatId)
        if (resolvedOwnerId.isNotBlank() && resolvedPeerId.isNotBlank()) {
            val now = System.currentTimeMillis()
            val privacy = dao.getPrivacySettings(resolvedOwnerId)
            val sendReadReceipts = privacy?.readReceipts?.uppercase() != "NOBODY"
            if (sendReadReceipts) {
                dao.markIncomingMessagesSeen(chatId, resolvedOwnerId, now)
                val idempKey = TelefamCryptoAndTelemetry.sha256Hex(
                    "seen_notif:$chatId:$resolvedOwnerId:${now / 5000L}".toByteArray(Charsets.UTF_8)
                )
                dao.enqueueChatOutbox(
                    ChatOutboxEntity(
                        outboxId = UUID.randomUUID().toString(),
                        ownerUserId = resolvedOwnerId,
                        chatId = chatId,
                        targetUserId = resolvedPeerId,
                        actionType = "SEEN_RECEIPT",
                        payloadJson = JSONObject().apply {
                            put("chatId", chatId)
                            put("timestampMs", now)
                        }.toString(),
                        idempotencyKey = idempKey,
                        createdAtMs = now
                    )
                )
                TelefamPrivacySyncWorker.enqueueSync(context)
            }
        }
    }

    private suspend fun handleDirectReply(context: Context, intent: Intent) {
        val remoteInputBundle = RemoteInput.getResultsFromIntent(intent) ?: return
        val replyText = remoteInputBundle.getCharSequence(TelefamNotificationEngine.KEY_TEXT_REPLY)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (replyText.isBlank()) return

        val chatId = intent.getStringExtra(TelefamNotificationEngine.EXTRA_CHAT_ID).orEmpty()
        val peerUserId = intent.getStringExtra(TelefamNotificationEngine.EXTRA_PEER_USER_ID).orEmpty()
        val ownerUserId = intent.getStringExtra(TelefamNotificationEngine.EXTRA_OWNER_USER_ID).orEmpty()
        val repliedToMsgId = intent.getStringExtra(TelefamNotificationEngine.EXTRA_MESSAGE_ID)
        if (chatId.isBlank()) return

        val dao = TelefamAppDatabase.getInstance(context).telefamDao()
        val chat = dao.getChatById(chatId) ?: return
        val resolvedOwnerId = ownerUserId.ifBlank { chat.ownerUserId }
        val resolvedPeerId = peerUserId.ifBlank { chat.peerUserId }

        if (dao.isEitherBlocked(resolvedOwnerId, resolvedPeerId) > 0 || chat.peerIsDeleted) {
            TelefamNotificationEngine.cancelChatNotification(context, chatId)
            return
        }

        val now = System.currentTimeMillis()
        val signalEngine = TelefamSignalE2eeEngine(dao)
        val repliedMsg = repliedToMsgId?.let { dao.getMessageById(it) }

        // Encrypt the inline reply using Signal X3DH + Double Ratchet (with silent Deflate Level 9)
        val envelope = signalEngine.encryptPayload(
            ownerUserId = resolvedOwnerId,
            peerUserId = resolvedPeerId,
            envelopeType = "SEND",
            messageType = ChatMessageType.TEXT.name,
            text = replyText,
            replyToMessageId = repliedMsg?.messageId,
            replyToSenderName = chat.peerFullName,
            replyToPreviewText = repliedMsg?.decryptedText?.take(80)
        )

        val disappearingSec = dao.getDisappearingSetting(chatId)?.durationSeconds
            ?: chat.disappearingDurationSeconds
        val expiresAtMs = if (disappearingSec > 0L) now + disappearingSec * 1000L else null
        val newMessageId = "msg_${UUID.randomUUID()}"

        val messageEntity = ChatMessageEntity(
            messageId = newMessageId,
            chatId = chatId,
            ownerUserId = resolvedOwnerId,
            senderUserId = resolvedOwnerId,
            recipientUserId = resolvedPeerId,
            messageType = ChatMessageType.TEXT.name,
            decryptedText = replyText,
            replyToMessageId = repliedMsg?.messageId,
            replyToSenderName = if (repliedMsg != null) chat.peerFullName else null,
            replyToPreviewText = repliedMsg?.decryptedText?.take(80),
            replyToMessageType = repliedMsg?.messageType,
            ciphertextBase64 = envelope.ciphertextBase64,
            ratchetHeaderJson = envelope.ratchetHeaderJson,
            deliveryStatus = "SENDING",
            expiresAtMs = expiresAtMs,
            createdAtMs = now
        )

        dao.upsertChatMessage(messageEntity)
        dao.upsertChat(
            chat.copy(
                lastMessageId = newMessageId,
                lastMessageType = ChatMessageType.TEXT.name,
                lastMessagePreview = replyText,
                lastMessageSenderId = resolvedOwnerId,
                lastMessageDeliveryStatus = "SENDING",
                lastMessageMediaIconUrl = null,
                lastMessageTimestampMs = now,
                unreadCount = 0,
                updatedAtMs = now
            )
        )

        // Mark incoming messages seen & queue outgoing E2EE reply in Outbox
        dao.markIncomingMessagesSeen(chatId, resolvedOwnerId, now)
        val idempKey = TelefamCryptoAndTelemetry.sha256Hex(
            "reply_notif:$newMessageId:$now".toByteArray(Charsets.UTF_8)
        )
        val payloadJson = JSONObject().apply {
            put("messageId", newMessageId)
            put("chatId", chatId)
            put("messageType", ChatMessageType.TEXT.name)
            put("ciphertextBase64", envelope.ciphertextBase64)
            put("ratchetHeaderJson", envelope.ratchetHeaderJson)
            put("isViewOnce", false)
            put("isForwarded", false)
            put("attachmentSizeBytes", TelefamAggressiveCompressionEngine.compressTextAggressiveToBase64(replyText).length.toLong())
            put("contentCompression", "deflate-lvl9")
            if (repliedMsg != null) put("replyToMessageId", repliedMsg.messageId)
            if (expiresAtMs != null) put("expiresAtMs", expiresAtMs)
        }.toString()

        dao.enqueueChatOutbox(
            ChatOutboxEntity(
                outboxId = "outbox_$newMessageId",
                ownerUserId = resolvedOwnerId,
                chatId = chatId,
                targetUserId = resolvedPeerId,
                messageId = newMessageId,
                actionType = "SEND",
                payloadJson = payloadJson,
                idempotencyKey = idempKey,
                createdAtMs = now
            )
        )

        // Dismiss the notification from the bar once replied & marked read
        TelefamNotificationEngine.cancelChatNotification(context, chatId)
        TelefamPrivacySyncWorker.enqueueSync(context)
    }
}
