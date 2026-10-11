package com.telefam.company.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.telefam.company.app.domain.ChatDisappearingOption
import com.telefam.company.app.domain.ChatMessageDeliveryStatus
import com.telefam.company.app.domain.ChatMessageType
import com.telefam.company.app.domain.OneToOneChatListItem
import com.telefam.company.app.ui.components.LucideIconKind
import com.telefam.company.app.ui.components.TelefamLucideIcon
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun OneToOneChatsListSection(
    chats: List<OneToOneChatListItem>,
    searchQuery: String,
    circleTint: Color,
    onOpenChat: (String) -> Unit,
    onStartChatClick: () -> Unit,
    emptyStateIllustration: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredChats = remember(chats, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            chats
        } else {
            chats.filter { item ->
                item.peerFullName.contains(q, ignoreCase = true) ||
                    item.peerUsername.contains(q, ignoreCase = true) ||
                    item.lastMessagePreview.contains(q, ignoreCase = true)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (chats.isEmpty() && searchQuery.isBlank()) {
            // Real Empty State (No demos, mocks or seeded data)
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .testTag("home_chats_empty_state"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                emptyStateIllustration()

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.start_conversation_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.start_conversation_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onStartChatClick,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 2.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("start_conversation_button")
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.MESSAGE_PLUS,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start chat",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("one_to_one_chats_lazy_list"),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
            ) {
                if (filteredChats.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.SEARCH,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 32.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No matching conversations",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredChats,
                        key = { it.chatId }
                    ) { chatItem ->
                        OneToOneChatListRow(
                            item = chatItem,
                            circleTint = circleTint,
                            onClick = { onOpenChat(chatItem.chatId) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            // Floating "Start chat" button on bottom-end when chats list is visible
            Surface(
                onClick = onStartChatClick,
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 18.dp)
                    .height(48.dp)
                    .testTag("fab_start_chat_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.MESSAGE_PLUS,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        size = 18.dp
                    )
                    Text(
                        text = "Start chat",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun OneToOneChatListRow(
    item: OneToOneChatListItem,
    circleTint: Color,
    onClick: () -> Unit
) {
    val isOutgoingLast = item.lastMessageSenderId == item.ownerUserId && item.lastMessageId != null
    val isRestricted = item.peerIsDeleted || item.isBlockedEitherWay

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_list_row_${item.chatId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Peer Avatar + Online Indicator Dot
        Box(modifier = Modifier.size(52.dp)) {
            PeerAvatarCircle(
                avatarUrl = if (item.peerIsDeleted) null else item.peerAvatarUrl,
                displayName = if (item.peerIsDeleted) "?" else item.peerFullName,
                size = 52.dp,
                circleTint = circleTint
            )

            if (item.peerIsOnline && !isRestricted) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .testTag("chat_row_online_dot_${item.chatId}")
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Top Line: Peer Name + Disappearing Timer Icon + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (item.peerIsDeleted) "User not found" else item.peerFullName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (item.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 15.5.sp
                        ),
                        color = if (item.peerIsDeleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.disappearingOption != ChatDisappearingOption.OFF && !isRestricted) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.TIMER_DISAPPEARING,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 13.dp
                        )
                    }
                }

                if (item.lastMessageTimestampMs > 0L) {
                    Text(
                        text = formatChatListTimestamp(item.lastMessageTimestampMs),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = if (item.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (item.unreadCount > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Line: Status Icon + Media/Mic Icon + Preview / Typing / Blocked / Deleted + Unread Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    when {
                        item.peerIsDeleted -> {
                            TelefamLucideIcon(
                                kind = LucideIconKind.BAN_BLOCK,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 14.dp
                            )
                            Text(
                                text = "User not found",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        item.isBlockedEitherWay -> {
                            TelefamLucideIcon(
                                kind = LucideIconKind.BAN_BLOCK,
                                tint = MaterialTheme.colorScheme.error,
                                size = 14.dp
                            )
                            Text(
                                text = "You can't message this user",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        item.peerIsTyping -> {
                            Text(
                                text = "typing...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontStyle = FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }

                        item.draftText.isNotBlank() -> {
                            Text(
                                text = "Draft: ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.draftText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        item.lastMessageId == null -> {
                            TelefamLucideIcon(
                                kind = LucideIconKind.LOCK_E2EE,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 13.dp
                            )
                            Text(
                                text = "Tap to start an encrypted chat",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        else -> {
                            // Outgoing status icon
                            if (isOutgoingLast) {
                                ChatDeliveryStatusIndicator(
                                    status = item.lastMessageDeliveryStatus,
                                    peerAvatarUrl = item.peerAvatarUrl,
                                    peerName = item.peerFullName,
                                    circleTint = circleTint,
                                    size = 15.dp
                                )
                            }

                            // Mic icon if last message is a voice note, or small icon preview + label for Sticker/GIF/Clip
                            when (item.lastMessageType) {
                                ChatMessageType.VOICE_NOTE -> {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.MIC,
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 15.dp
                                    )
                                    Text(
                                        text = item.lastMessagePreview.ifBlank { "Voice note" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = if (item.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = if (item.unreadCount > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                ChatMessageType.STICKER, ChatMessageType.GIF, ChatMessageType.CLIP -> {
                                    SmallMediaPreviewBadge(
                                        messageType = item.lastMessageType,
                                        previewUrl = item.lastMessageMediaIconUrl
                                    )
                                    val label = when (item.lastMessageType) {
                                        ChatMessageType.STICKER -> "Sticker"
                                        ChatMessageType.GIF -> "GIF"
                                        ChatMessageType.CLIP -> "Clip"
                                        else -> "Media"
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = if (item.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = if (item.unreadCount > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                ChatMessageType.IMAGE, ChatMessageType.VIDEO, ChatMessageType.MEDIA_ALBUM -> {
                                    SmallMediaPreviewBadge(
                                        messageType = item.lastMessageType,
                                        previewUrl = item.lastMessageMediaIconUrl
                                    )
                                    val fallbackLabel = when (item.lastMessageType) {
                                        ChatMessageType.IMAGE -> "Photo"
                                        ChatMessageType.VIDEO -> "Video"
                                        ChatMessageType.MEDIA_ALBUM -> "Album"
                                        else -> "Media"
                                    }
                                    Text(
                                        text = item.lastMessagePreview.ifBlank { fallbackLabel },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = if (item.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = if (item.unreadCount > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                ChatMessageType.LOCATION -> {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.MAP_PIN,
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 15.dp
                                    )
                                    Text(
                                        text = item.lastMessagePreview.ifBlank { "Location" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = if (item.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = if (item.unreadCount > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                ChatMessageType.CONTACT -> {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.USER_CONTACT,
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 15.dp
                                    )
                                    Text(
                                        text = item.lastMessagePreview.ifBlank { "Contact" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = if (item.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = if (item.unreadCount > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                ChatMessageType.DOCUMENT -> {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.FILE_DOC,
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 15.dp
                                    )
                                    Text(
                                        text = item.lastMessagePreview.ifBlank { "Document" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = if (item.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = if (item.unreadCount > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                ChatMessageType.POLL -> {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.BAR_CHART_POLL,
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 15.dp
                                    )
                                    Text(
                                        text = item.lastMessagePreview.ifBlank { "Poll" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = if (item.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = if (item.unreadCount > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                ChatMessageType.EVENT -> {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.CALENDAR_EVENT,
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 15.dp
                                    )
                                    Text(
                                        text = item.lastMessagePreview.ifBlank { "Event" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = if (item.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = if (item.unreadCount > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                ChatMessageType.TEXT -> {
                                    Text(
                                        text = item.lastMessagePreview,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = if (item.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = if (item.unreadCount > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Unread Count Badge (clears on open)
                if (item.unreadCount > 0 && !isRestricted) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .widthIn(min = 20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp)
                            .testTag("chat_unread_badge_${item.chatId}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmallMediaPreviewBadge(
    messageType: ChatMessageType,
    previewUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconKind = when (messageType) {
        ChatMessageType.STICKER -> LucideIconKind.STICKER
        ChatMessageType.GIF -> LucideIconKind.GIF
        ChatMessageType.CLIP, ChatMessageType.VIDEO -> LucideIconKind.CLIP_VIDEO
        ChatMessageType.IMAGE, ChatMessageType.MEDIA_ALBUM -> LucideIconKind.MEDIA_GALLERY
        else -> LucideIconKind.MEDIA_GALLERY
    }
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        if (!previewUrl.isNullOrBlank() && !previewUrl.startsWith("https://cdn.klipy.co/")) {
            val dataModel: Any = if (previewUrl.startsWith("/")) File(previewUrl) else previewUrl
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(dataModel)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false)
                    .build(),
                contentDescription = messageType.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (messageType == ChatMessageType.VIDEO) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.Center
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.PLAY,
                        tint = Color.White,
                        size = 10.dp,
                        filled = true
                    )
                }
            }
        } else {
            TelefamLucideIcon(
                kind = iconKind,
                tint = MaterialTheme.colorScheme.primary,
                size = 13.dp,
                strokeWidthDp = 1.7.dp
            )
        }
    }
}

/**
 * Status on the bubble row and chat list row:
 * - clock = sending (`SENDING`)
 * - one tick = sent (`SENT`)
 * - small cycle check in the red theme accent = delivered (`DELIVERED`)
 * - receiver's mini avatar = seen (`SEEN`)
 */
@Composable
fun ChatDeliveryStatusIndicator(
    status: ChatMessageDeliveryStatus,
    peerAvatarUrl: String?,
    peerName: String,
    circleTint: Color,
    size: Dp = 15.dp,
    tintOverride: Color? = null
) {
    when (status) {
        ChatMessageDeliveryStatus.SENDING -> {
            TelefamLucideIcon(
                kind = LucideIconKind.CLOCK,
                tint = tintOverride ?: MaterialTheme.colorScheme.onSurfaceVariant,
                size = size,
                strokeWidthDp = 1.8.dp
            )
        }

        ChatMessageDeliveryStatus.SENT -> {
            TelefamLucideIcon(
                kind = LucideIconKind.CHECK_SINGLE,
                tint = tintOverride ?: MaterialTheme.colorScheme.onSurfaceVariant,
                size = size,
                strokeWidthDp = 1.9.dp
            )
        }

        ChatMessageDeliveryStatus.DELIVERED -> {
            // Small cycle check in the red theme accent = delivered
            TelefamLucideIcon(
                kind = LucideIconKind.CIRCLE_CHECK_DELIVERED,
                tint = MaterialTheme.colorScheme.primary,
                size = size,
                strokeWidthDp = 1.9.dp,
                filled = true
            )
        }

        ChatMessageDeliveryStatus.SEEN -> {
            // Receiver's mini avatar = seen
            PeerAvatarCircle(
                avatarUrl = peerAvatarUrl,
                displayName = peerName,
                size = size,
                circleTint = circleTint
            )
        }

        ChatMessageDeliveryStatus.REJECTED_BLOCKED,
        ChatMessageDeliveryStatus.REJECTED_USER_NOT_FOUND -> {
            TelefamLucideIcon(
                kind = LucideIconKind.BAN_BLOCK,
                tint = MaterialTheme.colorScheme.error,
                size = size,
                strokeWidthDp = 1.8.dp
            )
        }
    }
}

@Composable
fun PeerAvatarCircle(
    avatarUrl: String?,
    displayName: String,
    size: Dp,
    circleTint: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initials = remember(displayName) {
        displayName.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(circleTint),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        if (!avatarUrl.isNullOrBlank()) {
            val dataModel: Any = if (avatarUrl.startsWith("/")) File(avatarUrl) else avatarUrl
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(dataModel)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false)
                    .build(),
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

fun formatChatListTimestamp(timestampMs: Long): String {
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = timestampMs }
    val sameDay = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
    return if (sameDay) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestampMs))
    } else {
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestampMs))
    }
}
