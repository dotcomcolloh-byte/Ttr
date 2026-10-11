package com.telefam.company.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessaging
import com.telefam.company.app.data.ChatMessageEntity
import com.telefam.company.app.data.TelefamAppDatabase
import com.telefam.company.app.data.TelefamFcmTokenRegisterPayload
import com.telefam.company.app.data.TelefamFcmTokenRevokePayload
import com.telefam.company.app.data.TelefamNetworkClient
import com.telefam.company.app.security.TelefamCryptoAndTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

enum class TelefamDeepLinkTargetType {
    CHAT_CONVERSATION,
    INCOMING_CALL,
    LIVE_LOCATION,
    SOCIAL_FOLLOWER,
    SOCIAL_POST_ACTIVITY,
    SECURITY_DEVICE_ALERT
}

data class TelefamNotificationDeepLink(
    val eventId: String = java.util.UUID.randomUUID().toString(),
    val targetType: TelefamDeepLinkTargetType,
    val chatId: String? = null,
    val peerUserId: String? = null,
    val messageId: String? = null,
    val socialActorUserId: String? = null,
    val postId: String? = null
)

private data class CachedConversationNotifLine(
    val messageId: String,
    val senderName: String,
    val senderUserId: String,
    val summaryText: String,
    val timestampMs: Long
)

object TelefamNotificationEngine {

    const val CHANNEL_DIRECT_MESSAGES = "telefam_channel_messages"
    const val CHANNEL_URGENT_CALLS_AND_LIVE = "telefam_channel_urgent"
    const val CHANNEL_SOCIAL_ACTIVITY = "telefam_channel_social"
    const val CHANNEL_SECURITY_ALERTS = "telefam_channel_security"

    const val ACTION_DIRECT_REPLY = "com.telefam.company.app.action.NOTIFICATION_DIRECT_REPLY"
    const val ACTION_MARK_AS_READ = "com.telefam.company.app.action.NOTIFICATION_MARK_AS_READ"
    const val ACTION_MUTE_CHAT = "com.telefam.company.app.action.NOTIFICATION_MUTE_CHAT"
    const val ACTION_DISMISS_CALL_OR_LIVE = "com.telefam.company.app.action.NOTIFICATION_DISMISS_URGENT"

    const val KEY_TEXT_REPLY = "telefam_key_inline_text_reply"

    const val EXTRA_DEEPLINK_TYPE = "telefam_deeplink_type"
    const val EXTRA_CHAT_ID = "telefam_deeplink_chat_id"
    const val EXTRA_PEER_USER_ID = "telefam_deeplink_peer_user_id"
    const val EXTRA_OWNER_USER_ID = "telefam_deeplink_owner_user_id"
    const val EXTRA_MESSAGE_ID = "telefam_deeplink_message_id"
    const val EXTRA_SOCIAL_ACTOR_ID = "telefam_deeplink_social_actor_id"
    const val EXTRA_NOTIFICATION_ID = "telefam_deeplink_notification_id"

    private const val PREFS_NOTIFICATIONS = "telefam_notification_prefs"
    private const val KEY_MUTED_CHAT_IDS = "muted_chat_ids_set"
    private const val KEY_LAST_FCM_TOKEN = "registered_fcm_token"
    private const val KEY_LAST_FCM_USER_ID = "registered_fcm_user_id"

    private const val GROUP_KEY_MESSAGES = "com.telefam.company.app.GROUP_DIRECT_MESSAGES"
    private const val SUMMARY_NOTIFICATION_ID = 770000

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var isAppInForeground: Boolean = false
        private set

    @Volatile
    var activeOpenChatId: String? = null
        private set

    private val conversationHistoryByChat = ConcurrentHashMap<String, MutableList<CachedConversationNotifLine>>()

    private val _pendingDeepLink = MutableStateFlow<TelefamNotificationDeepLink?>(null)
    val pendingDeepLink: StateFlow<TelefamNotificationDeepLink?> = _pendingDeepLink.asStateFlow()

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val messagesChannel = NotificationChannel(
            CHANNEL_DIRECT_MESSAGES,
            "Direct Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "End-to-end encrypted 1:1 messages, photos, videos, voice notes, polls, and events"
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 180L, 90L, 180L)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            setSound(defaultSound, audioAttrs)
        }

        val urgentChannel = NotificationChannel(
            CHANNEL_URGENT_CALLS_AND_LIVE,
            "Calls & Live Location",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming voice/video calls and active live location sharing sessions"
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 300L, 150L, 300L)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        val socialChannel = NotificationChannel(
            CHANNEL_SOCIAL_ACTIVITY,
            "Likes, Followers & Comments",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "New followers, post likes, comments, and community activity"
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        val securityChannel = NotificationChannel(
            CHANNEL_SECURITY_ALERTS,
            "Security & Device Sign-In Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New device sign-ins, 2-Step Verification alerts, and account security notices"
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        manager.createNotificationChannels(
            listOf(messagesChannel, urgentChannel, socialChannel, securityChannel)
        )
    }

    fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun onAppForegroundStateChanged(context: Context, inForeground: Boolean) {
        isAppInForeground = inForeground
        if (inForeground) {
            activeOpenChatId?.let { openChatId ->
                cancelChatNotification(context, openChatId)
            }
        }
    }

    fun setActiveOpenChat(context: Context, chatId: String?) {
        activeOpenChatId = chatId
        if (!chatId.isNullOrBlank()) {
            cancelChatNotification(context, chatId)
        }
    }

    fun shouldPostNotificationForChat(context: Context, chatId: String): Boolean {
        if (chatId.isBlank()) return false
        if (isChatMuted(context, chatId)) return false
        if (isAppInForeground && activeOpenChatId == chatId) return false
        return true
    }

    fun isChatMuted(context: Context, chatId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_MUTED_CHAT_IDS, emptySet()).orEmpty()
        return chatId in set
    }

    fun setChatMuted(context: Context, chatId: String, muted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_MUTED_CHAT_IDS, emptySet()).orEmpty().toMutableSet()
        if (muted) current.add(chatId) else current.remove(chatId)
        prefs.edit().putStringSet(KEY_MUTED_CHAT_IDS, current).apply()
        if (muted) {
            cancelChatNotification(context, chatId)
        }
    }

    fun notificationIdForChat(chatId: String): Int {
        return 100_000 + (abs(chatId.hashCode()) % 600_000)
    }

    fun cancelChatNotification(context: Context, chatId: String) {
        if (chatId.isBlank()) return
        conversationHistoryByChat.remove(chatId)
        runCatching {
            val nm = NotificationManagerCompat.from(context)
            nm.cancel(notificationIdForChat(chatId))
            if (conversationHistoryByChat.isEmpty()) {
                nm.cancel(SUMMARY_NOTIFICATION_ID)
            }
        }
    }

    fun onMessageUnsentOrDeleted(
        context: Context,
        ownerUserId: String,
        chatId: String,
        messageId: String
    ) {
        if (chatId.isBlank() || messageId.isBlank()) return
        val existingList = conversationHistoryByChat[chatId]
        if (existingList != null) {
            synchronized(existingList) {
                existingList.removeAll { it.messageId == messageId }
            }
            if (existingList.isEmpty()) {
                cancelChatNotification(context, chatId)
                return
            }
        } else {
            cancelChatNotification(context, chatId)
            return
        }

        engineScope.launch {
            val dao = TelefamAppDatabase.getInstance(context).telefamDao()
            val latestRemaining = existingList.lastOrNull() ?: run {
                cancelChatNotification(context, chatId)
                return@launch
            }
            val msgEntity = dao.getMessageById(latestRemaining.messageId)
            if (msgEntity != null) {
                postIncomingChatMessageNotification(
                    context = context,
                    ownerUserId = ownerUserId,
                    message = msgEntity,
                    forcePostEvenIfAlreadyInHistory = true
                )
            } else {
                cancelChatNotification(context, chatId)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun postIncomingChatMessageNotification(
        context: Context,
        ownerUserId: String,
        message: ChatMessageEntity,
        forcePostEvenIfAlreadyInHistory: Boolean = false
    ) = withContext(Dispatchers.IO) {
        ensureChannelsCreated(context)
        if (!hasPostNotificationsPermission(context)) return@withContext
        if (message.senderUserId == ownerUserId) return@withContext
        if (!shouldPostNotificationForChat(context, message.chatId)) return@withContext

        val dao = TelefamAppDatabase.getInstance(context).telefamDao()
        if (dao.isEitherBlocked(ownerUserId, message.senderUserId) > 0) return@withContext
        val chat = dao.getChatById(message.chatId)
        if (chat?.peerIsDeleted == true) return@withContext

        val senderProfile = dao.getProfileByUserId(message.senderUserId)
        val senderDisplayName = chat?.peerFullName?.takeIf { it.isNotBlank() }
            ?: senderProfile?.fullName?.takeIf { it.isNotBlank() }
            ?: senderProfile?.username?.takeIf { it.isNotBlank() }
            ?: "Telefam Contact"
        val senderAvatarPath = chat?.peerAvatarUrl ?: senderProfile?.avatarLocalPath ?: senderProfile?.avatarCdnUrl

        val summaryLine = TelefamNotificationPreviewRenderer.buildNotificationSummaryLine(message)
        val newEntry = CachedConversationNotifLine(
            messageId = message.messageId,
            senderName = senderDisplayName,
            senderUserId = message.senderUserId,
            summaryText = summaryLine,
            timestampMs = message.createdAtMs
        )

        val history = conversationHistoryByChat.getOrPut(message.chatId) { mutableListOf() }
        synchronized(history) {
            if (!forcePostEvenIfAlreadyInHistory) {
                history.removeAll { it.messageId == message.messageId }
                history.add(newEntry)
                while (history.size > 6) {
                    history.removeAt(0)
                }
            }
        }

        val avatarBitmap = TelefamNotificationPreviewRenderer.buildCircularSenderAvatarBitmap(
            senderDisplayName,
            senderAvatarPath
        )
        val senderPerson = Person.Builder()
            .setName(senderDisplayName)
            .setKey(message.senderUserId)
            .setIcon(IconCompat.createWithBitmap(avatarBitmap))
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(senderPerson)
            .setConversationTitle(senderDisplayName)
            .setGroupConversation(false)

        val snapshotLines = synchronized(history) { history.toList() }
        for (line in snapshotLines) {
            val linePerson = if (line.senderUserId == message.senderUserId) {
                senderPerson
            } else {
                Person.Builder().setName(line.senderName).setKey(line.senderUserId).build()
            }
            messagingStyle.addMessage(
                NotificationCompat.MessagingStyle.Message(
                    line.summaryText,
                    line.timestampMs,
                    linePerson
                )
            )
        }

        val expandedPreviewBitmap = if (!message.isViewOnce && !message.isViewOnceConsumed) {
            TelefamNotificationPreviewRenderer.buildRichExpandedPreviewBitmap(message)
        } else null

        val notifId = notificationIdForChat(message.chatId)
        val openChatPendingIntent = buildDeepLinkPendingIntent(
            context = context,
            requestCode = notifId + 1,
            deepLink = TelefamNotificationDeepLink(
                targetType = TelefamDeepLinkTargetType.CHAT_CONVERSATION,
                chatId = message.chatId,
                peerUserId = message.senderUserId,
                messageId = message.messageId
            ),
            ownerUserId = ownerUserId,
            notificationId = notifId
        )

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply to $senderDisplayName...")
            .build()
        val replyIntent = Intent(context, TelefamNotificationActionReceiver::class.java).apply {
            action = ACTION_DIRECT_REPLY
            putExtra(EXTRA_CHAT_ID, message.chatId)
            putExtra(EXTRA_PEER_USER_ID, message.senderUserId)
            putExtra(EXTRA_OWNER_USER_ID, ownerUserId)
            putExtra(EXTRA_MESSAGE_ID, message.messageId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 10,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_telefam_notification,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()

        val markReadIntent = Intent(context, TelefamNotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_AS_READ
            putExtra(EXTRA_CHAT_ID, message.chatId)
            putExtra(EXTRA_PEER_USER_ID, message.senderUserId)
            putExtra(EXTRA_OWNER_USER_ID, ownerUserId)
            putExtra(EXTRA_MESSAGE_ID, message.messageId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 20,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val markReadAction = NotificationCompat.Action.Builder(
            R.drawable.ic_telefam_notification,
            "Mark as Read",
            markReadPendingIntent
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()

        val muteIntent = Intent(context, TelefamNotificationActionReceiver::class.java).apply {
            action = ACTION_MUTE_CHAT
            putExtra(EXTRA_CHAT_ID, message.chatId)
            putExtra(EXTRA_PEER_USER_ID, message.senderUserId)
            putExtra(EXTRA_OWNER_USER_ID, ownerUserId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val mutePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 30,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val muteAction = NotificationCompat.Action.Builder(
            R.drawable.ic_telefam_notification,
            "Mute",
            mutePendingIntent
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MUTE)
            .setShowsUserInterface(false)
            .build()

        val publicLockscreenNotification = NotificationCompat.Builder(context, CHANNEL_DIRECT_MESSAGES)
            .setSmallIcon(R.drawable.ic_telefam_notification)
            .setColor(Color.rgb(200, 8, 21))
            .setContentTitle("Telefam")
            .setContentText("New encrypted message")
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val builder = NotificationCompat.Builder(context, CHANNEL_DIRECT_MESSAGES)
            .setSmallIcon(R.drawable.ic_telefam_notification)
            .setColor(Color.rgb(200, 8, 21))
            .setContentTitle(senderDisplayName)
            .setContentText(summaryLine)
            .setSubText(if (snapshotLines.size > 1) "${snapshotLines.size} new messages" else "End-to-end encrypted")
            .setLargeIcon(expandedPreviewBitmap ?: avatarBitmap)
            .setContentIntent(openChatPendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(forcePostEvenIfAlreadyInHistory)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicLockscreenNotification)
            .setGroup(GROUP_KEY_MESSAGES)
            .setWhen(message.createdAtMs)
            .setShowWhen(true)
            .addPerson(senderPerson)
            .addAction(replyAction)
            .addAction(markReadAction)
            .addAction(muteAction)

        if (expandedPreviewBitmap != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(expandedPreviewBitmap)
                    .bigLargeIcon(avatarBitmap)
                    .setBigContentTitle(senderDisplayName)
                    .setSummaryText(summaryLine)
            )
        } else {
            builder.setStyle(messagingStyle)
        }

        val notificationManager = NotificationManagerCompat.from(context)
        runCatching {
            notificationManager.notify(notifId, builder.build())
            if (conversationHistoryByChat.size > 1) {
                val summaryNotification = NotificationCompat.Builder(context, CHANNEL_DIRECT_MESSAGES)
                    .setSmallIcon(R.drawable.ic_telefam_notification)
                    .setColor(Color.rgb(200, 8, 21))
                    .setContentTitle("Telefam")
                    .setContentText("${conversationHistoryByChat.size} active conversations")
                    .setGroup(GROUP_KEY_MESSAGES)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(publicLockscreenNotification)
                    .build()
                notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun postIncomingCallOrLiveLocationNotification(
        context: Context,
        ownerUserId: String,
        chatId: String,
        peerUserId: String,
        peerDisplayName: String,
        isCall: Boolean,
        isVideoCall: Boolean = false,
        subtitle: String = ""
    ) {
        ensureChannelsCreated(context)
        if (!hasPostNotificationsPermission(context)) return
        if (isChatMuted(context, chatId)) return

        val notifId = (if (isCall) 710_000 else 720_000) + (abs(chatId.hashCode()) % 40_000)
        val deepLink = TelefamNotificationDeepLink(
            targetType = if (isCall) TelefamDeepLinkTargetType.INCOMING_CALL else TelefamDeepLinkTargetType.LIVE_LOCATION,
            chatId = chatId,
            peerUserId = peerUserId
        )
        val openPendingIntent = buildDeepLinkPendingIntent(
            context = context,
            requestCode = notifId + 1,
            deepLink = deepLink,
            ownerUserId = ownerUserId,
            notificationId = notifId
        )

        val dismissIntent = Intent(context, TelefamNotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_CALL_OR_LIVE
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val dismissPending = PendingIntent.getBroadcast(
            context,
            notifId + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isCall) {
            if (isVideoCall) "Incoming E2EE Video Call · $peerDisplayName"
            else "Incoming E2EE Audio Call · $peerDisplayName"
        } else {
            "Live Location Shared · $peerDisplayName"
        }
        val body = subtitle.ifBlank {
            if (isCall) "Tap to answer in Telefam" else "Tap to view real-time location in secure map"
        }

        val avatarBmp = TelefamNotificationPreviewRenderer.buildCircularSenderAvatarBitmap(peerDisplayName, null)
        val notification = NotificationCompat.Builder(context, CHANNEL_URGENT_CALLS_AND_LIVE)
            .setSmallIcon(R.drawable.ic_telefam_notification)
            .setColor(Color.rgb(200, 8, 21))
            .setContentTitle(title)
            .setContentText(body)
            .setLargeIcon(avatarBmp)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isCall) NotificationCompat.CATEGORY_CALL else NotificationCompat.CATEGORY_LOCATION_SHARING)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_telefam_notification,
                if (isCall) "Open Call" else "View Live Map",
                openPendingIntent
            )
            .addAction(
                R.drawable.ic_telefam_notification,
                if (isCall) "Decline" else "Dismiss",
                dismissPending
            )
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }
    }

    @SuppressLint("MissingPermission")
    fun postSocialActivityNotification(
        context: Context,
        ownerUserId: String,
        actorUserId: String,
        actorDisplayName: String,
        activityKind: String,
        commentPreview: String = "",
        postId: String? = null
    ) {
        ensureChannelsCreated(context)
        if (!hasPostNotificationsPermission(context)) return
        if (isAppInForeground) return

        val notifId = 810_000 + (abs("$actorUserId:$activityKind".hashCode()) % 70_000)
        val targetType = if (activityKind.uppercase(Locale.ROOT) == "FOLLOW") {
            TelefamDeepLinkTargetType.SOCIAL_FOLLOWER
        } else {
            TelefamDeepLinkTargetType.SOCIAL_POST_ACTIVITY
        }

        val openPending = buildDeepLinkPendingIntent(
            context = context,
            requestCode = notifId + 1,
            deepLink = TelefamNotificationDeepLink(
                targetType = targetType,
                socialActorUserId = actorUserId,
                postId = postId
            ),
            ownerUserId = ownerUserId,
            notificationId = notifId
        )

        val (title, body) = when (activityKind.uppercase(Locale.ROOT)) {
            "FOLLOW" -> "New Follower" to "$actorDisplayName started following you"
            "LIKE" -> "New Like" to "$actorDisplayName liked your post"
            "COMMENT" -> "New Comment from $actorDisplayName" to commentPreview.ifBlank { "$actorDisplayName commented on your post" }
            else -> "Telefam Activity" to "$actorDisplayName interacted with your profile"
        }

        val avatarBmp = TelefamNotificationPreviewRenderer.buildCircularSenderAvatarBitmap(actorDisplayName, null)
        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL_ACTIVITY)
            .setSmallIcon(R.drawable.ic_telefam_notification)
            .setColor(Color.rgb(200, 8, 21))
            .setContentTitle(title)
            .setContentText(body)
            .setLargeIcon(avatarBmp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(openPending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }
    }

    @SuppressLint("MissingPermission")
    fun postSecurityDeviceLoginNotification(
        context: Context,
        ownerUserId: String,
        title: String,
        body: String
    ) {
        ensureChannelsCreated(context)
        if (!hasPostNotificationsPermission(context)) return

        val notifId = 910_000 + (abs(title.hashCode()) % 30_000)
        val openPending = buildDeepLinkPendingIntent(
            context = context,
            requestCode = notifId + 1,
            deepLink = TelefamNotificationDeepLink(
                targetType = TelefamDeepLinkTargetType.SECURITY_DEVICE_ALERT
            ),
            ownerUserId = ownerUserId,
            notificationId = notifId
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SECURITY_ALERTS)
            .setSmallIcon(R.drawable.ic_telefam_notification)
            .setColor(Color.rgb(200, 8, 21))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }
    }

    fun syncFcmRegistrationToken(
        context: Context,
        ownerUserId: String,
        explicitToken: String? = null
    ) {
        if (ownerUserId.isBlank() || ownerUserId == "local_telefam_member") return
        engineScope.launch {
            runCatching {
                val resolvedToken = explicitToken?.takeIf { it.isNotBlank() }
                    ?: runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
                    ?: "fcm_local_${TelefamCryptoAndTelemetry.sha256Hex(ownerUserId.toByteArray(Charsets.UTF_8)).take(32)}"

                val prefs = context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(KEY_LAST_FCM_TOKEN, resolvedToken)
                    .putString(KEY_LAST_FCM_USER_ID, ownerUserId)
                    .apply()

                val tokenSha256 = TelefamCryptoAndTelemetry.sha256Hex(resolvedToken.toByteArray(Charsets.UTF_8))
                val deviceFp = TelefamCryptoAndTelemetry.sha256Hex(
                    "fcm_device:$ownerUserId:${Build.MODEL}".toByteArray(Charsets.UTF_8)
                ).take(24)
                val now = System.currentTimeMillis()
                val idempKey = TelefamCryptoAndTelemetry.sha256Hex(
                    "fcm_reg:$ownerUserId:$tokenSha256:${now / 60_000L}".toByteArray(Charsets.UTF_8)
                )

                TelefamNetworkClient.api.registerDeviceFcmToken(
                    rlsUserId = ownerUserId,
                    idempotencyKey = idempKey,
                    payload = TelefamFcmTokenRegisterPayload(
                        userId = ownerUserId,
                        fcmToken = resolvedToken,
                        fcmTokenSha256 = tokenSha256,
                        deviceFingerprint = deviceFp,
                        appVersion = BuildConfig.VERSION_NAME,
                        platform = "ANDROID",
                        e2eeWakeOnlyEnabled = true,
                        idempotencyKey = idempKey,
                        registeredAtMs = now
                    )
                )
            }
        }
    }

    fun revokeFcmRegistrationToken(
        context: Context,
        ownerUserId: String,
        reason: String = "USER_LOGOUT"
    ) {
        conversationHistoryByChat.clear()
        runCatching {
            NotificationManagerCompat.from(context).cancelAll()
        }
        engineScope.launch {
            runCatching {
                val prefs = context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE)
                val cachedToken = prefs.getString(KEY_LAST_FCM_TOKEN, null).orEmpty()
                val effectiveUserId = ownerUserId.ifBlank {
                    prefs.getString(KEY_LAST_FCM_USER_ID, "").orEmpty()
                }
                prefs.edit()
                    .remove(KEY_LAST_FCM_TOKEN)
                    .remove(KEY_LAST_FCM_USER_ID)
                    .apply()

                runCatching { FirebaseMessaging.getInstance().deleteToken().await() }

                if (effectiveUserId.isNotBlank() && cachedToken.isNotBlank()) {
                    val tokenSha256 = TelefamCryptoAndTelemetry.sha256Hex(cachedToken.toByteArray(Charsets.UTF_8))
                    val deviceFp = TelefamCryptoAndTelemetry.sha256Hex(
                        "fcm_device:$effectiveUserId:${Build.MODEL}".toByteArray(Charsets.UTF_8)
                    ).take(24)
                    val now = System.currentTimeMillis()
                    val idempKey = TelefamCryptoAndTelemetry.sha256Hex(
                        "fcm_rev:$effectiveUserId:$tokenSha256:$now".toByteArray(Charsets.UTF_8)
                    )
                    TelefamNetworkClient.api.revokeDeviceFcmToken(
                        rlsUserId = effectiveUserId,
                        idempotencyKey = idempKey,
                        payload = TelefamFcmTokenRevokePayload(
                            userId = effectiveUserId,
                            fcmTokenSha256 = tokenSha256,
                            deviceFingerprint = deviceFp,
                            reason = reason,
                            idempotencyKey = idempKey,
                            revokedAtMs = now
                        )
                    )
                }
            }
        }
    }

    fun handleIncomingActivityIntent(context: Context, intent: Intent?) {
        if (intent == null) return
        val rawType = intent.getStringExtra(EXTRA_DEEPLINK_TYPE) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val chatId = intent.getStringExtra(EXTRA_CHAT_ID)
        val peerUserId = intent.getStringExtra(EXTRA_PEER_USER_ID)
        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        val socialActorId = intent.getStringExtra(EXTRA_SOCIAL_ACTOR_ID)

        if (!chatId.isNullOrBlank()) {
            cancelChatNotification(context, chatId)
        } else if (notifId > 0) {
            runCatching { NotificationManagerCompat.from(context).cancel(notifId) }
        }

        val targetType = runCatching { TelefamDeepLinkTargetType.valueOf(rawType) }
            .getOrDefault(TelefamDeepLinkTargetType.CHAT_CONVERSATION)

        _pendingDeepLink.value = TelefamNotificationDeepLink(
            targetType = targetType,
            chatId = chatId,
            peerUserId = peerUserId,
            messageId = messageId,
            socialActorUserId = socialActorId
        )

        intent.removeExtra(EXTRA_DEEPLINK_TYPE)
    }

    fun consumePendingDeepLink() {
        _pendingDeepLink.value = null
    }

    private fun buildDeepLinkPendingIntent(
        context: Context,
        requestCode: Int,
        deepLink: TelefamNotificationDeepLink,
        ownerUserId: String,
        notificationId: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEPLINK_TYPE, deepLink.targetType.name)
            putExtra(EXTRA_CHAT_ID, deepLink.chatId)
            putExtra(EXTRA_PEER_USER_ID, deepLink.peerUserId)
            putExtra(EXTRA_OWNER_USER_ID, ownerUserId)
            putExtra(EXTRA_MESSAGE_ID, deepLink.messageId)
            putExtra(EXTRA_SOCIAL_ACTOR_ID, deepLink.socialActorUserId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
