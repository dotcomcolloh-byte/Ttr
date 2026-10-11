package com.telefam.company.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import com.example.ui.theme.TelefamAvatarCircleDark
import com.example.ui.theme.TelefamAvatarCircleLight
import com.example.ui.theme.isTelefamDarkTheme
import com.telefam.company.app.data.ActiveVoiceBubblePlaybackState
import com.telefam.company.app.data.ActiveVoiceRecordingState
import com.telefam.company.app.data.ForwardRecipientTarget
import com.telefam.company.app.data.PrivacySettingsEntity
import com.telefam.company.app.domain.ChatAttachmentCategory
import com.telefam.company.app.domain.ChatAttachmentItem
import com.telefam.company.app.domain.ChatBackgroundThemeOption
import com.telefam.company.app.domain.ChatDisappearingOption
import com.telefam.company.app.domain.ChatMessageItem
import com.telefam.company.app.domain.ChatMessageType
import com.telefam.company.app.domain.ChatReactionChoice
import com.telefam.company.app.domain.E2eeVerificationSnapshot
import com.telefam.company.app.domain.KlipyMediaItem
import com.telefam.company.app.domain.KlipyMediaTab
import com.telefam.company.app.domain.OneToOneChatListItem
import com.telefam.company.app.domain.PrivacyAudience
import com.telefam.company.app.domain.ReportLocationOption
import com.telefam.company.app.domain.ReportReasonOption
import com.telefam.company.app.domain.ReportUrgencyOption
import com.telefam.company.app.domain.TelefamBubbleColors
import com.telefam.company.app.ui.components.LucideIconKind
import com.telefam.company.app.ui.components.TelefamLucideIcon
import com.telefam.company.app.ui.components.toLucideIconKind
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun OneToOneChatScreen(
    ownerUserId: String,
    chatItem: OneToOneChatListItem,
    messages: List<ChatMessageItem>,
    attachments: List<ChatAttachmentItem>,
    privacySettings: PrivacySettingsEntity,
    inputText: String,
    replyingToMessage: ChatMessageItem?,
    editingMessage: ChatMessageItem?,
    selectedMessageIds: Set<String>,
    isSelectModeActive: Boolean,
    voiceRecordingState: ActiveVoiceRecordingState,
    bubblePlaybackState: ActiveVoiceBubblePlaybackState,
    activeKlipyTab: KlipyMediaTab,
    klipySearchQuery: String,
    klipyPagingItems: LazyPagingItems<KlipyMediaItem>,
    klipyRecentItems: List<KlipyMediaItem>,
    previewKlipyItem: KlipyMediaItem?,
    e2eeSnapshot: E2eeVerificationSnapshot?,
    forwardCandidates: List<ForwardRecipientTarget>,
    forwardSearchQuery: String,
    selectedForwardUserIds: Set<String>,
    isForwardPickerOpen: Boolean,
    onBackClick: () -> Unit,
    onOpenPeerProfile: (String) -> Unit,
    onInputTextChanged: (String) -> Unit,
    onSendTextOrEdit: () -> Unit,
    onSendKlipyMedia: (KlipyMediaItem) -> Unit,
    onSelectKlipyTab: (KlipyMediaTab) -> Unit,
    onKlipySearchQueryChanged: (String) -> Unit,
    onPreviewKlipyItem: (KlipyMediaItem?) -> Unit,
    onStartVoiceRecording: () -> Unit,
    onStopVoiceRecordingToPreview: () -> Unit,
    onToggleVoicePreviewPlayback: () -> Unit,
    onToggleVoiceViewOnce: () -> Unit,
    onCancelVoiceRecording: () -> Unit,
    onSendPreviewedVoiceNote: () -> Unit,
    onToggleBubbleVoicePlayback: (ChatMessageItem) -> Unit,
    onStartReply: (ChatMessageItem) -> Unit,
    onCancelReplyOrEdit: () -> Unit,
    onStartEdit: (ChatMessageItem) -> Unit,
    onToggleReaction: (String, ChatReactionChoice) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onUnsendMessage: (String) -> Unit,
    onDeleteSingleMessageForMe: (String) -> Unit,
    onEnterSelectMode: (String) -> Unit,
    onToggleSelectMessage: (String) -> Unit,
    onSelectAllMessages: () -> Unit,
    onExitSelectMode: () -> Unit,
    onDeleteSelectedMessages: () -> Unit,
    onOpenForwardForSingleMessage: (ChatMessageItem) -> Unit,
    onOpenForwardForSelectedMessages: () -> Unit,
    onForwardSearchQueryChanged: (String) -> Unit,
    onToggleForwardRecipient: (String) -> Unit,
    onConfirmForwardSelection: () -> Unit,
    onDismissForwardPicker: () -> Unit,
    onSelectDisappearingOption: (ChatDisappearingOption) -> Unit,
    onClearChat: () -> Unit,
    onToggleBlockPeer: () -> Unit,
    onSubmitReportPeer: (
        reason: ReportReasonOption,
        location: ReportLocationOption,
        urgency: ReportUrgencyOption,
        blockAfterReport: Boolean,
        additionalDetails: String
    ) -> Unit,
    onOpenE2eeVerificationScreen: () -> Unit,
    onSetE2eeVerified: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val isDark = isTelefamDarkTheme()
    val circleTint = if (isDark) TelefamAvatarCircleDark else TelefamAvatarCircleLight

    // Enforce screenshot privacy setting from Room silently via FLAG_SECURE (no banner)
    val screenshotsDisallowed = remember(privacySettings.whoCanScreenshotChats) {
        PrivacyAudience.fromStorageValue(privacySettings.whoCanScreenshotChats) == PrivacyAudience.NOBODY
    }
    DisposableEffect(screenshotsDisallowed) {
        val window = (context as? Activity)?.window
        if (screenshotsDisallowed) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Read Copy & Forward permissions from Room PrivacySettingsEntity
    val allowCopy = remember(privacySettings.whoCanCopyMessages) {
        PrivacyAudience.fromStorageValue(privacySettings.whoCanCopyMessages) != PrivacyAudience.NOBODY
    }
    val allowForward = remember(privacySettings.whoCanShareChats) {
        PrivacyAudience.fromStorageValue(privacySettings.whoCanShareChats) != PrivacyAudience.NOBODY
    }

    // Use the app's existing default theme colours
    val bubbleOption = remember(privacySettings.bubbleColorHex) {
        TelefamBubbleColors.findByHex(privacySettings.bubbleColorHex)
    }

    val outgoingBubbleColor = if (bubbleOption.colorHex.equals(TelefamBubbleColors.DEFAULT_HEX, ignoreCase = true)) {
        MaterialTheme.colorScheme.primary
    } else {
        runCatching { Color(android.graphics.Color.parseColor(bubbleOption.colorHex)) }
            .getOrDefault(MaterialTheme.colorScheme.primary)
    }
    val chatCanvasBackground = if (isDark) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.surface
    }

    var isKlipySheetOpen by remember { mutableStateOf(false) }
    var activeLongPressedMessage by remember { mutableStateOf<ChatMessageItem?>(null) }
    var isOverflowMenuExpanded by remember { mutableStateOf(false) }
    var activeOverflowModal by remember { mutableStateOf<ChatOverflowSubModal?>(null) }
    var selectedAttachmentCategory by remember { mutableStateOf(ChatAttachmentCategory.MEDIA) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartVoiceRecording()
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    BackHandler {
        when {
            activeLongPressedMessage != null -> activeLongPressedMessage = null
            isKlipySheetOpen -> isKlipySheetOpen = false
            activeOverflowModal != null -> activeOverflowModal = null
            isForwardPickerOpen -> onDismissForwardPicker()
            isSelectModeActive -> onExitSelectMode()
            else -> onBackClick()
        }
    }

    val pinnedMessages = remember(messages) { messages.filter { it.isPinned } }
    val favouritedMessages = remember(messages) { messages.filter { it.isFavourited } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(chatCanvasBackground)
            .imePadding()
            .testTag("one_to_one_chat_screen_root")
    ) {
        // 1. Top App Bar (Normal vs Select Mode)
        if (isSelectModeActive) {
            SelectModeChatTopBar(
                selectedCount = selectedMessageIds.size,
                totalCount = messages.size,
                allowForward = allowForward,
                onCloseSelectMode = onExitSelectMode,
                onMarkAll = onSelectAllMessages,
                onForwardSelected = onOpenForwardForSelectedMessages,
                onDeleteSelected = onDeleteSelectedMessages
            )
        } else {
            ChatTopAppBar(
                chatItem = chatItem,
                circleTint = circleTint,
                isOverflowExpanded = isOverflowMenuExpanded,
                onBackClick = onBackClick,
                onOpenPeerProfile = { onOpenPeerProfile(chatItem.peerUserId) },
                onVoiceCallClick = {
                    Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                },
                onVideoCallClick = {
                    Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                },
                onToggleOverflowMenu = { isOverflowMenuExpanded = !isOverflowMenuExpanded },
                onDismissOverflowMenu = { isOverflowMenuExpanded = false },
                onOpenOverflowModal = { modal ->
                    isOverflowMenuExpanded = false
                    activeOverflowModal = modal
                },
                onToggleBlockPeer = {
                    isOverflowMenuExpanded = false
                    onToggleBlockPeer()
                }
            )
        }

        // 2. Pinned Message Bar (if any message is pinned)
        val topPinned = pinnedMessages.lastOrNull()
        if (topPinned != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_pinned_message_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.PIN,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 16.dp
                        )
                        Column {
                            Text(
                                text = "Pinned message (${pinnedMessages.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = topPinned.decryptedText.ifBlank { topPinned.messageType.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = { onTogglePin(topPinned.messageId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.CLOSE_X,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 15.dp
                        )
                    }
                }
            }
        }

        // 3. Message List + Small E2EE Banner
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("chat_messages_lazy_column"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(key = "e2ee_encryption_banner") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        onClick = onOpenE2eeVerificationScreen,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("e2ee_encryption_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TelefamLucideIcon(
                                kind = if (chatItem.e2eeSafetyNumberVerified) {
                                    LucideIconKind.SHIELD_VERIFIED
                                } else {
                                    LucideIconKind.LOCK_E2EE
                                },
                                tint = MaterialTheme.colorScheme.primary,
                                size = 13.dp
                            )
                            Text(
                                text = "Messages are end-to-end encrypted. Tap to verify.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(
                items = messages,
                key = { it.messageId }
            ) { message ->
                val isSelected = message.messageId in selectedMessageIds
                OneToOneMessageBubbleRow(
                    message = message,
                    ownerUserId = ownerUserId,
                    peerName = chatItem.peerFullName,
                    peerAvatarUrl = chatItem.peerAvatarUrl,
                    circleTint = circleTint,
                    outgoingBubbleColor = outgoingBubbleColor,
                    isSelectModeActive = isSelectModeActive,
                    isSelected = isSelected,
                    bubblePlaybackState = bubblePlaybackState,
                    onClick = {
                        if (isSelectModeActive) {
                            onToggleSelectMessage(message.messageId)
                        }
                    },
                    onLongPress = {
                        if (isSelectModeActive) {
                            onToggleSelectMessage(message.messageId)
                        } else {
                            activeLongPressedMessage = message
                        }
                    },
                    onSwipeToReply = {
                        if (!isSelectModeActive) {
                            onStartReply(message)
                        }
                    },
                    onToggleVoicePlayback = { onToggleBubbleVoicePlayback(message) },
                    onToggleReaction = { choice -> onToggleReaction(message.messageId, choice) }
                )
            }
        }

        // 4. Bottom Input Bar OR Blocked / Deleted Banner
        when {
            chatItem.peerIsDeleted -> {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .testTag("chat_deleted_user_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.BAN_BLOCK,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 16.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "User not found",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            chatItem.isBlockedEitherWay -> {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .testTag("chat_blocked_user_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.BAN_BLOCK,
                            tint = MaterialTheme.colorScheme.error,
                            size = 16.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You can't message this user",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            else -> {
                ChatInputBarSection(
                    inputText = inputText,
                    replyingToMessage = replyingToMessage,
                    editingMessage = editingMessage,
                    voiceRecordingState = voiceRecordingState,
                    onInputTextChanged = onInputTextChanged,
                    onCancelReplyOrEdit = onCancelReplyOrEdit,
                    onOpenStickerSheet = { isKlipySheetOpen = true },
                    onAttachmentComingSoon = {
                        Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                    },
                    onSendTextOrEdit = onSendTextOrEdit,
                    onStartVoiceRecording = {
                        val hasAudioPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasAudioPerm) {
                            onStartVoiceRecording()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopVoiceRecordingToPreview = onStopVoiceRecordingToPreview,
                    onToggleVoicePreviewPlayback = onToggleVoicePreviewPlayback,
                    onToggleVoiceViewOnce = onToggleVoiceViewOnce,
                    onCancelVoiceRecording = onCancelVoiceRecording,
                    onSendPreviewedVoiceNote = onSendPreviewedVoiceNote
                )
            }
        }
    }

    // KLIPY Sticker / GIF / Clip Sheet
    if (isKlipySheetOpen) {
        KlipyMediaBottomSheet(
            selectedTab = activeKlipyTab,
            searchQuery = klipySearchQuery,
            pagingItems = klipyPagingItems,
            recentItems = klipyRecentItems,
            fallbackItems = klipyRecentItems,
            previewItem = previewKlipyItem,
            onSelectTab = onSelectKlipyTab,
            onSearchQueryChange = onKlipySearchQueryChanged,
            onPreviewMediaItem = onPreviewKlipyItem,
            onSendMediaItem = { item ->
                onSendKlipyMedia(item)
                isKlipySheetOpen = false
            },
            onDismiss = { isKlipySheetOpen = false }
        )
    }

    // Message Long-Press Bottom Sheet
    val targetMessage = activeLongPressedMessage
    if (targetMessage != null) {
        MessageLongPressActionSheet(
            message = targetMessage,
            ownerUserId = ownerUserId,
            nowMs = System.currentTimeMillis(),
            allowCopy = allowCopy,
            allowForward = allowForward,
            onReact = { choice ->
                onToggleReaction(targetMessage.messageId, choice)
                activeLongPressedMessage = null
            },
            onReply = {
                onStartReply(targetMessage)
                activeLongPressedMessage = null
            },
            onForward = {
                onOpenForwardForSingleMessage(targetMessage)
                activeLongPressedMessage = null
            },
            onCopyText = {
                if (allowCopy && targetMessage.decryptedText.isNotBlank()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Message", targetMessage.decryptedText))
                }
                activeLongPressedMessage = null
            },
            onTogglePin = {
                onTogglePin(targetMessage.messageId)
                activeLongPressedMessage = null
            },
            onToggleFavourite = {
                onToggleFavourite(targetMessage.messageId)
                activeLongPressedMessage = null
            },
            onEnterSelectMode = {
                onEnterSelectMode(targetMessage.messageId)
                activeLongPressedMessage = null
            },
            onDeleteLocally = {
                onDeleteSingleMessageForMe(targetMessage.messageId)
                activeLongPressedMessage = null
            },
            onStartEdit = {
                onStartEdit(targetMessage)
                activeLongPressedMessage = null
            },
            onUnsendForBoth = {
                onUnsendMessage(targetMessage.messageId)
                activeLongPressedMessage = null
            },
            onDismiss = { activeLongPressedMessage = null }
        )
    }

    // Forward Contacts Picker Bottom Sheet
    if (isForwardPickerOpen && allowForward) {
        ForwardMessagesBottomSheet(
            candidates = forwardCandidates,
            selectedUserIds = selectedForwardUserIds,
            searchQuery = forwardSearchQuery,
            circleTint = circleTint,
            onSearchQueryChange = onForwardSearchQueryChanged,
            onToggleRecipient = onToggleForwardRecipient,
            onConfirmForward = onConfirmForwardSelection,
            onDismiss = onDismissForwardPicker
        )
    }

    // 3-Dots Overflow Sub-Modal
    val currentModal = activeOverflowModal
    if (currentModal != null) {
        ChatOverflowModalSheet(
            modalType = currentModal,
            peerDisplayName = chatItem.peerFullName,
            attachments = attachments,
            selectedAttachmentCategory = selectedAttachmentCategory,
            favouritedMessages = favouritedMessages,
            currentDisappearingOption = chatItem.disappearingOption,
            e2eeSnapshot = e2eeSnapshot,
            onSelectAttachmentCategory = { selectedAttachmentCategory = it },
            onSelectDisappearingOption = { option ->
                onSelectDisappearingOption(option)
                activeOverflowModal = null
            },
            onConfirmClearChat = {
                onClearChat()
                activeOverflowModal = null
            },
            onSubmitReportUser = { reason, location, urgency, blockAfterReport, details ->
                onSubmitReportPeer(reason, location, urgency, blockAfterReport, details)
                activeOverflowModal = null
            },
            onToggleE2eeVerified = onSetE2eeVerified,
            onUnfavouriteMessage = onToggleFavourite,
            onDismiss = { activeOverflowModal = null }
        )
    }
}

@Composable
private fun ChatTopAppBar(
    chatItem: OneToOneChatListItem,
    circleTint: Color,
    isOverflowExpanded: Boolean,
    onBackClick: () -> Unit,
    onOpenPeerProfile: () -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onToggleOverflowMenu: () -> Unit,
    onDismissOverflowMenu: () -> Unit,
    onOpenOverflowModal: (ChatOverflowSubModal) -> Unit,
    onToggleBlockPeer: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_top_app_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("chat_back_button")
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.ARROW_LEFT,
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 21.dp
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenPeerProfile)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("chat_header_peer_info"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(42.dp)) {
                        PeerAvatarCircle(
                            avatarUrl = if (chatItem.peerIsDeleted) null else chatItem.peerAvatarUrl,
                            displayName = if (chatItem.peerIsDeleted) "User not found" else chatItem.peerFullName,
                            size = 42.dp,
                            circleTint = circleTint
                        )
                        if (chatItem.peerIsOnline && !chatItem.isBlockedEitherWay && !chatItem.peerIsDeleted) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF16A34A))
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (chatItem.peerIsDeleted) "User not found" else chatItem.peerFullName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val subtitleText = when {
                            chatItem.peerIsDeleted -> "Deleted account"
                            chatItem.isBlockedEitherWay -> "You can't message this user"
                            chatItem.peerIsTyping -> "typing..."
                            chatItem.peerIsOnline -> "online"
                            chatItem.peerLastSeenMs != null -> "last seen ${formatChatListTimestamp(chatItem.peerLastSeenMs)}"
                            else -> "@${chatItem.peerUsername.removePrefix("@")}"
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontWeight = if (chatItem.peerIsTyping || chatItem.peerIsOnline) FontWeight.SemiBold else FontWeight.Normal,
                                fontStyle = if (chatItem.peerIsTyping) FontStyle.Italic else FontStyle.Normal
                            ),
                            color = if (chatItem.peerIsTyping || chatItem.peerIsOnline) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right side: Video call, Voice call, 3-dots menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onVideoCallClick,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("chat_video_call_button")
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.VIDEO_CALL,
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 20.dp
                    )
                }

                IconButton(
                    onClick = onVoiceCallClick,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("chat_voice_call_button")
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.PHONE_CALL,
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 20.dp
                    )
                }

                Box {
                    IconButton(
                        onClick = onToggleOverflowMenu,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("chat_more_options_button")
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.MORE_VERTICAL,
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = 20.dp
                        )
                    }

                    DropdownMenu(
                        expanded = isOverflowExpanded,
                        onDismissRequest = onDismissOverflowMenu
                    ) {
                        DropdownMenuItem(
                            text = { Text("Media, Links & Docs") },
                            leadingIcon = {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.MEDIA_GALLERY,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    size = 18.dp
                                )
                            },
                            onClick = { onOpenOverflowModal(ChatOverflowSubModal.MEDIA_LINKS_DOCS) },
                            modifier = Modifier.testTag("menu_media_links_docs")
                        )
                        DropdownMenuItem(
                            text = { Text("Favourited messages") },
                            leadingIcon = {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.STAR,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    size = 18.dp
                                )
                            },
                            onClick = { onOpenOverflowModal(ChatOverflowSubModal.FAVOURITED_MESSAGES) },
                            modifier = Modifier.testTag("menu_favourited_messages")
                        )
                        DropdownMenuItem(
                            text = { Text("Disappearing messages (${chatItem.disappearingOption.displayLabel})") },
                            leadingIcon = {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.TIMER_DISAPPEARING,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    size = 18.dp
                                )
                            },
                            onClick = { onOpenOverflowModal(ChatOverflowSubModal.DISAPPEARING_MESSAGES) },
                            modifier = Modifier.testTag("menu_disappearing_messages")
                        )
                        DropdownMenuItem(
                            text = { Text(if (chatItem.isBlockedByMe) "Unblock user" else "Block user") },
                            leadingIcon = {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.BAN_BLOCK,
                                    tint = MaterialTheme.colorScheme.error,
                                    size = 18.dp
                                )
                            },
                            onClick = onToggleBlockPeer,
                            modifier = Modifier.testTag("menu_block_user")
                        )
                        DropdownMenuItem(
                            text = { Text("Report user") },
                            leadingIcon = {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.FLAG_REPORT,
                                    tint = MaterialTheme.colorScheme.error,
                                    size = 18.dp
                                )
                            },
                            onClick = { onOpenOverflowModal(ChatOverflowSubModal.REPORT_USER) },
                            modifier = Modifier.testTag("menu_report_user")
                        )
                        DropdownMenuItem(
                            text = { Text("Clear chat") },
                            leadingIcon = {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.TRASH,
                                    tint = MaterialTheme.colorScheme.error,
                                    size = 18.dp
                                )
                            },
                            onClick = { onOpenOverflowModal(ChatOverflowSubModal.CLEAR_CHAT_CONFIRM) },
                            modifier = Modifier.testTag("menu_clear_chat")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectModeChatTopBar(
    selectedCount: Int,
    totalCount: Int,
    allowForward: Boolean,
    onCloseSelectMode: () -> Unit,
    onMarkAll: () -> Unit,
    onForwardSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_select_mode_top_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCloseSelectMode,
                    modifier = Modifier.size(42.dp)
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.CLOSE_X,
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 20.dp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onMarkAll,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .testTag("select_mode_mark_all_button")
                ) {
                    Text(
                        text = if (selectedCount == totalCount && totalCount > 0) "Unmark All" else "Mark All",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                if (allowForward) {
                    IconButton(
                        onClick = onForwardSelected,
                        enabled = selectedCount > 0,
                        modifier = Modifier.testTag("select_mode_forward_button")
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.FORWARD,
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = 20.dp
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteSelected,
                    enabled = selectedCount > 0,
                    modifier = Modifier.testTag("select_mode_delete_button")
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.TRASH,
                        tint = MaterialTheme.colorScheme.error,
                        size = 20.dp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OneToOneMessageBubbleRow(
    message: ChatMessageItem,
    ownerUserId: String,
    peerName: String,
    peerAvatarUrl: String?,
    circleTint: Color,
    outgoingBubbleColor: Color,
    isSelectModeActive: Boolean,
    isSelected: Boolean,
    bubblePlaybackState: ActiveVoiceBubblePlaybackState,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeToReply: () -> Unit,
    onToggleVoicePlayback: () -> Unit,
    onToggleReaction: (ChatReactionChoice) -> Unit
) {
    val isOutgoing = message.isOutgoing(ownerUserId)
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }
    var isTextExpanded by remember(message.messageId) { mutableStateOf(false) }

    val bubbleContainerColor = if (isOutgoing) {
        outgoingBubbleColor
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val bubbleTextColor = if (isOutgoing) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val bubbleSubtleColor = if (isOutgoing) {
        Color.White.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
            .pointerInput(message.messageId, isSelectModeActive) {
                if (!isSelectModeActive) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffsetX.value >= 72f) {
                                onSwipeToReply()
                            }
                            coroutineScope.launch {
                                dragOffsetX.animateTo(0f, tween(180))
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                dragOffsetX.animateTo(0f, tween(180))
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val next = (dragOffsetX.value + dragAmount).coerceIn(0f, 110f)
                            coroutineScope.launch {
                                dragOffsetX.snapTo(next)
                            }
                        }
                    )
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 2.dp)
            .testTag("chat_message_row_${message.messageId}"),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isSelectModeActive) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 8.dp)
                    .size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                TelefamLucideIcon(
                    kind = LucideIconKind.SELECT_CHECK,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 20.dp,
                    filled = isSelected
                )
            }
        }

        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Surface(
                shape = bubbleShape,
                color = bubbleContainerColor,
                tonalElevation = if (isOutgoing) 0.dp else 1.dp,
                modifier = Modifier.testTag("chat_bubble_${message.messageId}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // "Forwarded" label on forwarded messages
                    if (message.isForwarded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.FORWARD,
                                tint = bubbleSubtleColor,
                                size = 12.dp
                            )
                            Text(
                                text = "Forwarded",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = bubbleSubtleColor
                            )
                        }
                    }

                    // Reply preview inside bubble
                    if (!message.replyToMessageId.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = if (isOutgoing) 0.16f else 0.06f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                Text(
                                    text = message.replyToSenderName ?: peerName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = bubbleTextColor
                                )
                                Text(
                                    text = message.replyToPreviewText ?: "Message",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = bubbleSubtleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Message Content by Type
                    when (message.messageType) {
                        ChatMessageType.TEXT -> {
                            val textContent = message.decryptedText
                            val lineCountEstimate = remember(textContent) {
                                textContent.count { it == '\n' } + 1 + (textContent.length / 38)
                            }
                            val isLongText = lineCountEstimate > 8 || textContent.length > 320

                            Text(
                                text = textContent,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.5.sp,
                                    lineHeight = 20.sp
                                ),
                                color = bubbleTextColor,
                                maxLines = if (isLongText && !isTextExpanded) 8 else Int.MAX_VALUE,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isLongText) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isTextExpanded) "Read less" else "Read more",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = bubbleTextColor,
                                    modifier = Modifier
                                        .clickable { isTextExpanded = !isTextExpanded }
                                        .testTag("bubble_read_more_toggle_${message.messageId}")
                                )
                            }
                        }

                        ChatMessageType.STICKER, ChatMessageType.GIF, ChatMessageType.CLIP -> {
                            BubbleMediaLoopingContent(
                                message = message,
                                textColor = bubbleTextColor
                            )
                        }

                        ChatMessageType.VOICE_NOTE -> {
                            BubbleVoiceNotePlayer(
                                message = message,
                                isPlaying = bubblePlaybackState.messageId == message.messageId && bubblePlaybackState.isPlaying,
                                progressFraction = if (bubblePlaybackState.messageId == message.messageId) {
                                    bubblePlaybackState.progressFraction
                                } else 0f,
                                textColor = bubbleTextColor,
                                subtleColor = bubbleSubtleColor,
                                onTogglePlay = onToggleVoicePlayback
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Bottom Metadata Row: Pin/Star + Edited + Time + Status Icon (or Status on row)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (message.isPinned) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.PIN,
                                tint = bubbleSubtleColor,
                                size = 11.dp
                            )
                        }
                        if (message.isFavourited) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.STAR,
                                tint = bubbleSubtleColor,
                                size = 11.dp,
                                filled = true
                            )
                        }
                        if (message.isEdited) {
                            Text(
                                text = "edited",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontStyle = FontStyle.Italic
                                ),
                                color = bubbleSubtleColor
                            )
                        }
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.createdAtMs)),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = bubbleSubtleColor
                        )
                    }
                }
            }

            // Reactions Row below bubble
            if (message.reactions.isNotEmpty()) {
                val grouped = remember(message.reactions) {
                    message.reactions.groupBy { it.reactionKey }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    grouped.forEach { (key, list) ->
                        val choice = ChatReactionChoice.fromKey(key)
                        val reactedByMe = list.any { it.reactorUserId == ownerUserId }
                        Surface(
                            onClick = { onToggleReaction(choice) },
                            shape = RoundedCornerShape(50),
                            color = if (reactedByMe) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                0.8.dp,
                                if (reactedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TelefamLucideIcon(
                                    kind = choice.toLucideIconKind(),
                                    tint = MaterialTheme.colorScheme.primary,
                                    size = 13.dp,
                                    filled = true
                                )
                                if (list.size > 1) {
                                    Text(
                                        text = list.size.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Status on the bubble row (for outgoing messages):
        // clock = sending, one tick = sent, small cycle check in red theme accent = delivered, receiver's mini avatar = seen
        if (isOutgoing) {
            Spacer(modifier = Modifier.width(5.dp))
            Box(modifier = Modifier.padding(bottom = 4.dp)) {
                ChatDeliveryStatusIndicator(
                    status = message.deliveryStatus,
                    peerAvatarUrl = peerAvatarUrl,
                    peerName = peerName,
                    circleTint = circleTint,
                    size = 15.dp
                )
            }
        }
    }
}

@Composable
private fun BubbleMediaLoopingContent(
    message: ChatMessageItem,
    textColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_media_loop")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubble_media_pulse"
    )

    val pathOrUrl = message.mediaLocalPath ?: message.mediaPreviewUrl ?: message.mediaUrl.orEmpty()
    val isSynthetic = pathOrUrl.startsWith("https://cdn.klipy.co/")

    Box(
        modifier = Modifier
            .width(190.dp)
            .height(145.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        if (!isSynthetic && pathOrUrl.isNotBlank()) {
            val model: Any = if (pathOrUrl.startsWith("/")) File(pathOrUrl) else pathOrUrl
            AsyncImage(
                model = model,
                contentDescription = message.mediaTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(10.dp)
            ) {
                val icon = when (message.messageType) {
                    ChatMessageType.STICKER -> LucideIconKind.STICKER
                    ChatMessageType.GIF -> LucideIconKind.GIF
                    else -> LucideIconKind.CLIP_VIDEO
                }
                Box(
                    modifier = Modifier
                        .size((44f * pulse).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    TelefamLucideIcon(
                        kind = icon,
                        tint = textColor,
                        size = 24.dp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.mediaTitle.ifBlank { message.messageType.name },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Muted looping badge for GIFs and Clips
        if (message.messageType == ChatMessageType.GIF || message.messageType == ChatMessageType.CLIP) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text = if (message.messageType == ChatMessageType.GIF) "GIF • LOOP" else "CLIP • MUTED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BubbleVoiceNotePlayer(
    message: ChatMessageItem,
    isPlaying: Boolean,
    progressFraction: Float,
    textColor: Color,
    subtleColor: Color,
    onTogglePlay: () -> Unit
) {
    if (message.isViewOnce && message.isViewOnceConsumed) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            TelefamLucideIcon(
                kind = LucideIconKind.VIEW_ONCE_ONE,
                tint = subtleColor,
                size = 18.dp
            )
            Text(
                text = "Opened (View once voice note)",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = subtleColor
            )
        }
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .widthIn(min = 200.dp, max = 260.dp)
            .padding(vertical = 2.dp)
    ) {
        Surface(
            onClick = onTogglePlay,
            shape = CircleShape,
            color = textColor.copy(alpha = 0.18f),
            modifier = Modifier
                .size(38.dp)
                .testTag("voice_bubble_play_button_${message.messageId}")
        ) {
            Box(contentAlignment = Alignment.Center) {
                TelefamLucideIcon(
                    kind = if (isPlaying) LucideIconKind.PAUSE else LucideIconKind.PLAY,
                    tint = textColor,
                    size = 18.dp,
                    filled = true
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            val samples = remember(message.voiceWaveformSamples) {
                message.voiceWaveformSamples.ifEmpty {
                    listOf(0.25f, 0.5f, 0.8f, 0.4f, 0.9f, 0.6f, 0.35f, 0.75f, 0.55f, 0.85f, 0.45f, 0.3f)
                }
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                val barCount = samples.size.coerceAtLeast(1)
                val spacing = 3.dp.toPx()
                val totalSpacing = spacing * (barCount - 1)
                val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(2.dp.toPx())

                samples.forEachIndexed { idx, amp ->
                    val fraction = idx.toFloat() / barCount.toFloat()
                    val active = fraction <= progressFraction
                    val barH = (size.height * amp.coerceIn(0.18f, 1f))
                    val topY = (size.height - barH) / 2f
                    drawRoundRect(
                        color = if (active) textColor else subtleColor.copy(alpha = 0.45f),
                        topLeft = Offset(idx * (barWidth + spacing), topY),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sec = (message.voiceDurationMs / 1000L).coerceAtLeast(1L)
                Text(
                    text = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = subtleColor
                )

                if (message.isViewOnce) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.VIEW_ONCE_ONE,
                            tint = textColor,
                            size = 12.dp
                        )
                        Text(
                            text = "View once",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBarSection(
    inputText: String,
    replyingToMessage: ChatMessageItem?,
    editingMessage: ChatMessageItem?,
    voiceRecordingState: ActiveVoiceRecordingState,
    onInputTextChanged: (String) -> Unit,
    onCancelReplyOrEdit: () -> Unit,
    onOpenStickerSheet: () -> Unit,
    onAttachmentComingSoon: () -> Unit,
    onSendTextOrEdit: () -> Unit,
    onStartVoiceRecording: () -> Unit,
    onStopVoiceRecordingToPreview: () -> Unit,
    onToggleVoicePreviewPlayback: () -> Unit,
    onToggleVoiceViewOnce: () -> Unit,
    onCancelVoiceRecording: () -> Unit,
    onSendPreviewedVoiceNote: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("chat_input_bar_section")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Reply or Edit Context Banner above input
            AnimatedVisibility(visible = replyingToMessage != null || editingMessage != null) {
                val bannerTitle = if (editingMessage != null) {
                    "Editing message"
                } else {
                    "Replying to ${replyingToMessage?.replyToSenderName ?: "message"}"
                }
                val bannerSub = editingMessage?.decryptedText
                    ?: replyingToMessage?.decryptedText?.ifBlank { replyingToMessage.messageType.name }.orEmpty()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = if (editingMessage != null) LucideIconKind.PENCIL_EDIT else LucideIconKind.REPLY,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 16.dp
                        )
                        Column {
                            Text(
                                text = bannerTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = bannerSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onCancelReplyOrEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.CLOSE_X,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 15.dp
                        )
                    }
                }
            }

            // Voice Note Active Recording OR Preview Player Bar
            when {
                voiceRecordingState.isRecording -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("voice_recording_live_bar"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onCancelVoiceRecording,
                            modifier = Modifier.size(38.dp)
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.TRASH,
                                tint = MaterialTheme.colorScheme.error,
                                size = 20.dp
                            )
                        }

                        val sec = (voiceRecordingState.elapsedMs / 1000L)
                        Text(
                            text = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Live waveform
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                        ) {
                            val samples = voiceRecordingState.waveformSamples.takeLast(28).ifEmpty { listOf(0.25f) }
                            val barW = 3.dp.toPx()
                            val gap = 3.dp.toPx()
                            samples.forEachIndexed { idx, amp ->
                                val h = (size.height * amp.coerceIn(0.15f, 1f))
                                drawRoundRect(
                                    color = Color(0xFFEF233C),
                                    topLeft = Offset(idx * (barW + gap), (size.height - h) / 2f),
                                    size = Size(barW, h),
                                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                )
                            }
                        }

                        Surface(
                            onClick = onStopVoiceRecordingToPreview,
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("voice_stop_to_preview_button")
                        ) {
                            Text(
                                text = "Stop & Preview",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                voiceRecordingState.isPreviewReady -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("voice_preview_player_bar"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onCancelVoiceRecording,
                            modifier = Modifier.size(36.dp)
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.TRASH,
                                tint = MaterialTheme.colorScheme.error,
                                size = 19.dp
                            )
                        }

                        Surface(
                            onClick = onToggleVoicePreviewPlayback,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("voice_preview_play_pause_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                TelefamLucideIcon(
                                    kind = if (voiceRecordingState.isPlayingPreview) LucideIconKind.PAUSE else LucideIconKind.PLAY,
                                    tint = MaterialTheme.colorScheme.primary,
                                    size = 17.dp,
                                    filled = true
                                )
                            }
                        }

                        val sec = (voiceRecordingState.elapsedMs / 1000L).coerceAtLeast(1L)
                        Text(
                            text = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // "View once" toggle
                        Surface(
                            onClick = onToggleVoiceViewOnce,
                            shape = RoundedCornerShape(16.dp),
                            color = if (voiceRecordingState.isViewOnce) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.testTag("voice_view_once_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.VIEW_ONCE_ONE,
                                    tint = if (voiceRecordingState.isViewOnce) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    size = 14.dp
                                )
                                Text(
                                    text = "View once",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (voiceRecordingState.isViewOnce) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        // Send voice note button
                        Surface(
                            onClick = onSendPreviewedVoiceNote,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("voice_preview_send_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.SEND,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    size = 18.dp
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Standard Input Row: Sticker button + Text field + File button (Coming soon) + Mic / Send
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onOpenStickerSheet,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("chat_open_klipy_button")
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.STICKER,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 21.dp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (inputText.isEmpty()) {
                                        Text(
                                            text = "Message...",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    BasicTextField(
                                        value = inputText,
                                        onValueChange = onInputTextChanged,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        maxLines = 5,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("chat_message_text_input")
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // File attachment button (shows "Coming soon" toast)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onAttachmentComingSoon)
                                        .testTag("chat_file_attachment_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.PAPERCLIP,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 18.dp
                                    )
                                }
                            }
                        }

                        if (inputText.isNotBlank()) {
                            Surface(
                                onClick = onSendTextOrEdit,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("chat_send_message_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.SEND,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        size = 19.dp
                                    )
                                }
                            }
                        } else {
                            // Press and hold (or tap) to record voice note; release to stop into preview player
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                onStartVoiceRecording()
                                                val released = tryAwaitRelease()
                                                if (released) {
                                                    onStopVoiceRecordingToPreview()
                                                }
                                            }
                                        )
                                    }
                                    .testTag("chat_voice_record_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.MIC,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    size = 20.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun E2eeVerificationScreen(
    snapshot: E2eeVerificationSnapshot?,
    onBackClick: () -> Unit,
    onToggleVerified: (Boolean) -> Unit
) {
    val context = LocalContext.current
    BackHandler(onBack = onBackClick)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxSize()
            .testTag("e2ee_verification_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("e2ee_verification_back_button")
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.ARROW_LEFT,
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 21.dp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "End-to-End Encryption",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                E2eeVerificationSheetContent(
                    snapshot = snapshot,
                    onToggleVerified = onToggleVerified,
                    onCopySafetyNumber = { safetyNumber ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Safety Number", safetyNumber))
                        Toast.makeText(context, "Safety number copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
