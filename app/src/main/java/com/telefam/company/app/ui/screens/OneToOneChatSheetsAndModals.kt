package com.telefam.company.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import com.telefam.company.app.data.ForwardRecipientTarget
import com.telefam.company.app.domain.ChatAttachmentCategory
import com.telefam.company.app.domain.ChatAttachmentItem
import com.telefam.company.app.domain.ChatDisappearingOption
import com.telefam.company.app.domain.ChatMessageItem
import com.telefam.company.app.domain.ChatMessageType
import com.telefam.company.app.domain.ChatReactionChoice
import com.telefam.company.app.domain.E2eeVerificationSnapshot
import com.telefam.company.app.domain.KlipyMediaItem
import com.telefam.company.app.domain.KlipyMediaTab
import com.telefam.company.app.domain.ReportLocationOption
import com.telefam.company.app.domain.ReportReasonOption
import com.telefam.company.app.domain.ReportUrgencyOption
import com.telefam.company.app.ui.components.LucideIconKind
import com.telefam.company.app.ui.components.TelefamLucideIcon
import com.telefam.company.app.ui.components.toLucideIconKind
import java.io.File
import java.security.MessageDigest

enum class ChatOverflowSubModal {
    MEDIA_LINKS_DOCS,
    FAVOURITED_MESSAGES,
    DISAPPEARING_MESSAGES,
    CLEAR_CHAT_CONFIRM,
    REPORT_USER,
    E2EE_VERIFICATION
}

/**
 * KLIPY Media Sheet:
 * - Tabs with subtitles: Stickers | GIFs | Clips
 * - Search bar that searches the active tab
 * - Paging 3 grid, skeleton loading, error with retry, cached recents available offline
 * - Tap an item -> preview dialog -> Send
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KlipyMediaBottomSheet(
    selectedTab: KlipyMediaTab,
    searchQuery: String,
    pagingItems: LazyPagingItems<KlipyMediaItem>,
    recentItems: List<KlipyMediaItem> = emptyList(),
    fallbackItems: List<KlipyMediaItem> = emptyList(),
    previewItem: KlipyMediaItem? = null,
    onSelectTab: (KlipyMediaTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPreviewMediaItem: (KlipyMediaItem?) -> Unit = {},
    onSendMediaItem: (KlipyMediaItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var localPreviewItem by remember(previewItem) { mutableStateOf(previewItem) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("klipy_media_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 440.dp, max = 600.dp)
                .padding(horizontal = 16.dp)
        ) {
            // Header + Tabs (Stickers | GIFs | Clips) with Subtitles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KlipyMediaTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val tabIcon = when (tab) {
                        KlipyMediaTab.STICKERS -> LucideIconKind.STICKER
                        KlipyMediaTab.GIFS -> LucideIconKind.GIF
                        KlipyMediaTab.CLIPS -> LucideIconKind.CLIP_VIDEO
                    }
                    Surface(
                        onClick = { onSelectTab(tab) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("klipy_tab_${tab.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TelefamLucideIcon(
                                    kind = tabIcon,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    size = 15.dp
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = tab.displayTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Text(
                                text = tab.subtitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar that searches the active tab
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                placeholder = {
                    Text(
                        text = "Search ${selectedTab.displayTitle} (${selectedTab.subtitle})...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp)
                    )
                },
                leadingIcon = {
                    TelefamLucideIcon(
                        kind = LucideIconKind.SEARCH,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 18.dp
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.CLOSE_X,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 16.dp
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("klipy_search_input")
            )

            // Offline-First Cached Recents Row
            if (recentItems.isNotEmpty() && searchQuery.isBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Recent ${selectedTab.displayTitle} (Offline Ready)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("klipy_recents_row")
                ) {
                    items(recentItems, key = { "recent_${it.itemId}" }) { recent ->
                        Surface(
                            onClick = {
                                localPreviewItem = recent
                                onPreviewMediaItem(recent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                Text(
                                    text = recent.title,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val refreshState = pagingItems.loadState.refresh
            val displayList: List<KlipyMediaItem> = remember(
                pagingItems.itemCount,
                fallbackItems,
                recentItems,
                selectedTab,
                searchQuery
            ) {
                if (pagingItems.itemCount > 0) {
                    (0 until pagingItems.itemCount).mapNotNull { pagingItems[it] }
                } else {
                    val source = fallbackItems.ifEmpty { recentItems }
                    val q = searchQuery.trim()
                    if (q.isEmpty()) source
                    else source.filter { it.title.contains(q, ignoreCase = true) }
                }
            }

            val columns = if (selectedTab == KlipyMediaTab.STICKERS) 3 else 2

            when {
                refreshState is LoadState.Loading && displayList.isEmpty() -> {
                    // Skeleton loading grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("klipy_skeleton_grid"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(6) {
                            KlipySkeletonCard(selectedTab = selectedTab)
                        }
                    }
                }

                refreshState is LoadState.Error && displayList.isEmpty() -> {
                    // Error state with Retry button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp)
                            .testTag("klipy_error_retry_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.ALERT_TRIANGLE,
                            tint = MaterialTheme.colorScheme.error,
                            size = 32.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Couldn't load ${selectedTab.displayTitle}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { pagingItems.retry() },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("klipy_retry_button")
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.REFRESH_RETRY,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                size = 16.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("klipy_media_grid"),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = displayList,
                            key = { it.itemId }
                        ) { mediaItem ->
                            KlipyMediaGridCard(
                                item = mediaItem,
                                onClick = {
                                    localPreviewItem = mediaItem
                                    onPreviewMediaItem(mediaItem)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Tap an item -> Preview Dialog -> Send
    val activePreview = localPreviewItem
    if (activePreview != null) {
        KlipyItemPreviewDialog(
            item = activePreview,
            onDismiss = {
                localPreviewItem = null
                onPreviewMediaItem(null)
            },
            onSend = {
                val chosen = activePreview
                localPreviewItem = null
                onPreviewMediaItem(null)
                onSendMediaItem(chosen)
            }
        )
    }
}

@Composable
private fun KlipySkeletonCard(selectedTab: KlipyMediaTab) {
    val infiniteTransition = rememberInfiniteTransition(label = "klipy_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "klipy_skeleton_alpha"
    )
    val aspect = when (selectedTab) {
        KlipyMediaTab.STICKERS -> 1f
        KlipyMediaTab.GIFS -> 1.35f
        KlipyMediaTab.CLIPS -> 1.5f
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

@Composable
fun KlipyItemPreviewDialog(
    item: KlipyMediaItem,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val iconKind = when (item.mediaTab) {
                    KlipyMediaTab.STICKERS -> LucideIconKind.STICKER
                    KlipyMediaTab.GIFS -> LucideIconKind.GIF
                    KlipyMediaTab.CLIPS -> LucideIconKind.CLIP_VIDEO
                }
                TelefamLucideIcon(
                    kind = iconKind,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 20.dp
                )
                Text(
                    text = item.title.ifBlank { item.mediaTab.displayTitle },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    KlipyMediaGridCard(
                        item = item,
                        onClick = onSend
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${item.mediaTab.displayTitle} • ${item.mediaTab.subtitle}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSend,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("klipy_preview_send_button")
            ) {
                TelefamLucideIcon(
                    kind = LucideIconKind.SEND,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    size = 16.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun KlipyMediaGridCard(
    item: KlipyMediaItem,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "klipy_card_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "klipy_pulse_anim"
    )

    val aspect = when (item.mediaTab) {
        KlipyMediaTab.STICKERS -> 1f
        KlipyMediaTab.GIFS -> 1.35f
        KlipyMediaTab.CLIPS -> 1.5f
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .testTag("klipy_item_${item.itemId}")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val localOrRemoteUrl = item.localCachedPath ?: item.previewUrl
            val isSyntheticCatalogUrl = localOrRemoteUrl.startsWith("https://cdn.klipy.co/")

            if (!isSyntheticCatalogUrl && localOrRemoteUrl.isNotBlank()) {
                val modelData: Any = if (localOrRemoteUrl.startsWith("/")) File(localOrRemoteUrl) else localOrRemoteUrl
                AsyncImage(
                    model = modelData,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Animated vector artwork for Stickers, GIFs, and Clips
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((38f * pulseScale).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconKind = when (item.mediaTab) {
                            KlipyMediaTab.STICKERS -> LucideIconKind.STICKER
                            KlipyMediaTab.GIFS -> LucideIconKind.GIF
                            KlipyMediaTab.CLIPS -> LucideIconKind.CLIP_VIDEO
                        }
                        TelefamLucideIcon(
                            kind = iconKind,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 22.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Badge for GIF / Clip duration
            if (item.mediaTab == KlipyMediaTab.GIFS || item.mediaTab == KlipyMediaTab.CLIPS) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    val badgeText = if (item.mediaTab == KlipyMediaTab.GIFS) {
                        "GIF"
                    } else {
                        val sec = ((item.durationMs ?: 4000L) / 1000L).coerceAtLeast(1L)
                        "0:${sec.toString().padStart(2, '0')}"
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Long-Press Message Action Sheet:
 * - Reaction Row (Lucide SVG vector icons)
 * - Reply, Forward (hidden if privacy setting disallows forwarding), Copy (hidden if privacy setting disallows copy),
 *   Pin, Favourite, Select, Delete
 * - Edit (owner messages), Unsend (owner messages)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageLongPressActionSheet(
    message: ChatMessageItem,
    ownerUserId: String,
    nowMs: Long,
    allowCopy: Boolean = true,
    allowForward: Boolean = true,
    onReact: (ChatReactionChoice) -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onCopyText: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavourite: () -> Unit,
    onEnterSelectMode: () -> Unit,
    onDeleteLocally: () -> Unit,
    onStartEdit: () -> Unit,
    onUnsendForBoth: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val myReactions = remember(message.reactions, ownerUserId) {
        message.reactions.filter { it.reactorUserId == ownerUserId }.map { it.reactionKey }.toSet()
    }
    val canEdit = message.isEditableAt(nowMs)
    val canUnsend = message.isUnsendableAt(nowMs)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("message_long_press_action_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // 1. Reaction Row (Lucide SVG icons only)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("message_reaction_picker_row")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChatReactionChoice.entries.forEach { choice ->
                        val isSelected = choice.key in myReactions
                        Surface(
                            onClick = { onReact(choice) },
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("reaction_choice_${choice.key.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                TelefamLucideIcon(
                                    kind = choice.toLucideIconKind(),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    size = 22.dp,
                                    filled = isSelected
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Context Action Items
            MessageSheetActionRow(
                icon = LucideIconKind.REPLY,
                label = "Reply",
                testTag = "action_sheet_reply",
                onClick = onReply
            )

            if (allowCopy && message.messageType == ChatMessageType.TEXT && message.decryptedText.isNotBlank()) {
                MessageSheetActionRow(
                    icon = LucideIconKind.COPY,
                    label = "Copy",
                    testTag = "action_sheet_copy",
                    onClick = onCopyText
                )
            }

            if (allowForward && !message.isViewOnce) {
                MessageSheetActionRow(
                    icon = LucideIconKind.FORWARD,
                    label = "Forward",
                    testTag = "action_sheet_forward",
                    onClick = onForward
                )
            }

            MessageSheetActionRow(
                icon = LucideIconKind.PIN,
                label = if (message.isPinned) "Unpin" else "Pin",
                testTag = "action_sheet_pin",
                onClick = onTogglePin
            )

            MessageSheetActionRow(
                icon = LucideIconKind.STAR,
                label = if (message.isFavourited) "Remove from Favourites" else "Favourite",
                testTag = "action_sheet_favourite",
                onClick = onToggleFavourite
            )

            MessageSheetActionRow(
                icon = LucideIconKind.SELECT_CHECK,
                label = "Select",
                testTag = "action_sheet_select",
                onClick = onEnterSelectMode
            )

            if (canEdit) {
                MessageSheetActionRow(
                    icon = LucideIconKind.PENCIL_EDIT,
                    label = "Edit",
                    testTag = "action_sheet_edit",
                    onClick = onStartEdit
                )
            }

            if (canUnsend) {
                MessageSheetActionRow(
                    icon = LucideIconKind.UNSEND_UNDO,
                    label = "Unsend",
                    tint = MaterialTheme.colorScheme.primary,
                    testTag = "action_sheet_unsend",
                    onClick = onUnsendForBoth
                )
            }

            MessageSheetActionRow(
                icon = LucideIconKind.TRASH,
                label = "Delete",
                tint = MaterialTheme.colorScheme.error,
                testTag = "action_sheet_delete_local",
                onClick = onDeleteLocally
            )
        }
    }
}

@Composable
private fun MessageSheetActionRow(
    icon: LucideIconKind,
    label: String,
    testTag: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TelefamLucideIcon(
            kind = icon,
            tint = tint,
            size = 20.dp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            ),
            color = tint
        )
    }
}

/**
 * Multi-Contact Forward Modal:
 * - Pick one or multiple contacts
 * - Blocked or deleted users are automatically skipped/disabled
 * - Re-encrypts per recipient and adds "Forwarded" label on bubble
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardMessagesBottomSheet(
    candidates: List<ForwardRecipientTarget>,
    selectedUserIds: Set<String>,
    searchQuery: String,
    circleTint: Color,
    onSearchQueryChange: (String) -> Unit,
    onToggleRecipient: (String) -> Unit,
    onConfirmForward: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filtered = remember(candidates, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) candidates
        else candidates.filter {
            it.fullName.contains(q, ignoreCase = true) ||
                it.username.contains(q, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("forward_messages_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 400.dp, max = 560.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Forward to contacts",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onConfirmForward,
                    enabled = selectedUserIds.isNotEmpty(),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("confirm_forward_send_button")
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.FORWARD,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        size = 16.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Send (${selectedUserIds.size})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                placeholder = { Text("Search contacts...") },
                leadingIcon = {
                    TelefamLucideIcon(
                        kind = LucideIconKind.SEARCH,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 18.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("forward_contacts_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No eligible contacts found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(
                        items = filtered,
                        key = { it.userId }
                    ) { recipient ->
                        val isSelected = recipient.userId in selectedUserIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleRecipient(recipient.userId) }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                                .testTag("forward_recipient_row_${recipient.userId}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PeerAvatarCircle(
                                avatarUrl = recipient.avatarUrl,
                                displayName = recipient.fullName,
                                size = 42.dp,
                                circleTint = circleTint
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recipient.fullName,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (recipient.username.isNotBlank()) {
                                    Text(
                                        text = "@${recipient.username.removePrefix("@")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TelefamLucideIcon(
                                kind = LucideIconKind.SELECT_CHECK,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                size = 22.dp,
                                filled = isSelected
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3-Dots Overflow Sub-Modals:
 * - Media, Links & Docs (searchable lists)
 * - Favourited Messages
 * - Disappearing Messages (24 hours, 7 days, 1 month, Off)
 * - Clear Chat Confirmation (local + backend)
 * - Report User (questions + answers + Submit)
 * - E2EE Safety Number Verification
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatOverflowModalSheet(
    modalType: ChatOverflowSubModal,
    peerDisplayName: String = "",
    attachments: List<ChatAttachmentItem>,
    selectedAttachmentCategory: ChatAttachmentCategory,
    favouritedMessages: List<ChatMessageItem>,
    currentDisappearingOption: ChatDisappearingOption,
    e2eeSnapshot: E2eeVerificationSnapshot?,
    onSelectAttachmentCategory: (ChatAttachmentCategory) -> Unit,
    onSelectDisappearingOption: (ChatDisappearingOption) -> Unit,
    onConfirmClearChat: () -> Unit,
    onSubmitReportUser: (
        reason: ReportReasonOption,
        location: ReportLocationOption,
        urgency: ReportUrgencyOption,
        blockAfterReport: Boolean,
        additionalDetails: String
    ) -> Unit = { _, _, _, _, _ -> },
    onToggleE2eeVerified: (Boolean) -> Unit,
    onUnfavouriteMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var attachmentSearchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("chat_overflow_sub_modal_${modalType.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            when (modalType) {
                ChatOverflowSubModal.MEDIA_LINKS_DOCS -> {
                    Text(
                        text = "Media, Links & Docs",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Searchable list input for Media, Links & Docs
                    OutlinedTextField(
                        value = attachmentSearchQuery,
                        onValueChange = { attachmentSearchQuery = it },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        placeholder = { Text("Search ${selectedAttachmentCategory.name.lowercase()}...") },
                        leadingIcon = {
                            TelefamLucideIcon(
                                kind = LucideIconKind.SEARCH,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 18.dp
                            )
                        },
                        trailingIcon = {
                            if (attachmentSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { attachmentSearchQuery = "" }) {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.CLOSE_X,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 16.dp
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("media_links_docs_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChatAttachmentCategory.entries.forEach { cat ->
                            val selected = cat == selectedAttachmentCategory
                            val label = when (cat) {
                                ChatAttachmentCategory.MEDIA -> "Media"
                                ChatAttachmentCategory.LINKS -> "Links"
                                ChatAttachmentCategory.DOCS -> "Docs"
                            }
                            Surface(
                                onClick = { onSelectAttachmentCategory(cat) },
                                shape = RoundedCornerShape(50),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val filtered = remember(attachments, selectedAttachmentCategory, attachmentSearchQuery) {
                        val q = attachmentSearchQuery.trim()
                        attachments.filter { item ->
                            item.category == selectedAttachmentCategory &&
                                (q.isEmpty() ||
                                    item.title.contains(q, ignoreCase = true) ||
                                    item.subtitle.contains(q, ignoreCase = true))
                        }
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No ${selectedAttachmentCategory.name.lowercase()} found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered, key = { it.attachmentId }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val icon = when (item.category) {
                                            ChatAttachmentCategory.MEDIA -> LucideIconKind.MEDIA_GALLERY
                                            ChatAttachmentCategory.LINKS -> LucideIconKind.LINK_CHAIN
                                            ChatAttachmentCategory.DOCS -> LucideIconKind.FILE_DOC
                                        }
                                        TelefamLucideIcon(
                                            kind = icon,
                                            tint = MaterialTheme.colorScheme.primary,
                                            size = 20.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ChatOverflowSubModal.FAVOURITED_MESSAGES -> {
                    Text(
                        text = "Favourited Messages",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (favouritedMessages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Long-press any message to add it to Favourites",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(favouritedMessages, key = { it.messageId }) { msg ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = msg.decryptedText.ifBlank { msg.messageType.name },
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatChatListTimestamp(msg.createdAtMs),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onUnfavouriteMessage(msg.messageId) }) {
                                            TelefamLucideIcon(
                                                kind = LucideIconKind.STAR,
                                                tint = MaterialTheme.colorScheme.primary,
                                                size = 18.dp,
                                                filled = true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ChatOverflowSubModal.DISAPPEARING_MESSAGES -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.TIMER_DISAPPEARING,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 22.dp
                        )
                        Text(
                            text = "Disappearing Messages",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "New messages sent in this 1:1 chat will automatically disappear after the selected timer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ChatDisappearingOption.entries.forEach { option ->
                        val selected = option == currentDisappearingOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectDisappearingOption(option) }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                                .testTag("disappearing_option_${option.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectDisappearingOption(option) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option.displayLabel,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                ChatOverflowSubModal.CLEAR_CHAT_CONFIRM -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.TRASH,
                            tint = MaterialTheme.colorScheme.error,
                            size = 22.dp
                        )
                        Text(
                            text = "Clear Chat History?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This removes all messages, reactions, pins, and media in this chat locally in Room and queues a clear-chat sync for your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = onConfirmClearChat,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_clear_chat_button")
                        ) {
                            Text("Clear chat", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                ChatOverflowSubModal.REPORT_USER -> {
                    var selectedReason by remember { mutableStateOf(ReportReasonOption.HARASSMENT_OR_BULLYING) }
                    var selectedLocation by remember { mutableStateOf(ReportLocationOption.DIRECT_MESSAGES) }
                    var selectedUrgency by remember { mutableStateOf(ReportUrgencyOption.STANDARD_REVIEW) }
                    var blockAfterReport by remember { mutableStateOf(true) }
                    var additionalDetails by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.FLAG_REPORT,
                                tint = MaterialTheme.colorScheme.error,
                                size = 22.dp
                            )
                            Text(
                                text = "Report ${peerDisplayName.ifBlank { "User" }}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "1. Why are you reporting this chat?",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ReportReasonOption.entries.forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedReason = reason }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedReason == reason,
                                    onClick = { selectedReason = reason }
                                )
                                Text(
                                    text = reason.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "2. Where did the issue happen?",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ReportLocationOption.entries.forEach { loc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedLocation = loc }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedLocation == loc,
                                    onClick = { selectedLocation = loc }
                                )
                                Text(
                                    text = loc.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "3. How urgent is this report?",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ReportUrgencyOption.entries.forEach { urg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedUrgency = urg }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedUrgency == urg,
                                    onClick = { selectedUrgency = urg }
                                )
                                Text(
                                    text = urg.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = additionalDetails,
                            onValueChange = { additionalDetails = it },
                            label = { Text("Additional details (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_report_details_input"),
                            minLines = 2,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Also block this user",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = blockAfterReport,
                                onCheckedChange = { blockAfterReport = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                onSubmitReportUser(
                                    selectedReason,
                                    selectedLocation,
                                    selectedUrgency,
                                    blockAfterReport,
                                    additionalDetails
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_chat_user_report_button")
                        ) {
                            Text("Submit Report", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                ChatOverflowSubModal.E2EE_VERIFICATION -> {
                    E2eeVerificationSheetContent(
                        snapshot = e2eeSnapshot,
                        onToggleVerified = onToggleE2eeVerified,
                        onCopySafetyNumber = { safetyNumber ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Safety Number", safetyNumber))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun E2eeVerificationSheetContent(
    snapshot: E2eeVerificationSnapshot?,
    onToggleVerified: (Boolean) -> Unit,
    onCopySafetyNumber: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelefamLucideIcon(
                kind = LucideIconKind.SHIELD_VERIFIED,
                tint = MaterialTheme.colorScheme.primary,
                size = 24.dp
            )
            Text(
                text = "Verify Security Code",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Messages and voice notes in this 1:1 chat are protected with Signal X3DH + Double Ratchet end-to-end encryption.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (snapshot != null) {
            // Deterministic QR Matrix generated from both identity keys
            SafetyNumberQrMatrix(
                qrPayload = snapshot.qrPayload,
                modifier = Modifier
                    .size(164.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    )
                    .background(Color.White)
                    .padding(12.dp)
                    .testTag("e2ee_safety_number_qr")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 60-digit Safety Number formatted in 12 blocks of 5 digits
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopySafetyNumber(snapshot.safetyNumberFormatted) }
                    .testTag("e2ee_safety_number_box")
            ) {
                Text(
                    text = snapshot.safetyNumberFormatted,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mark as Verified",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ratchet Chain: #${snapshot.sendingChainLength} out / #${snapshot.receivingChainLength} in",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = snapshot.isVerified,
                    onCheckedChange = onToggleVerified,
                    modifier = Modifier.testTag("e2ee_verified_switch")
                )
            }
        }
    }
}

@Composable
private fun SafetyNumberQrMatrix(
    qrPayload: String,
    modifier: Modifier = Modifier
) {
    val digestBytes = remember(qrPayload) {
        MessageDigest.getInstance("SHA-256").digest(qrPayload.toByteArray(Charsets.UTF_8))
    }

    Canvas(modifier = modifier) {
        val gridCount = 21
        val cellW = size.width / gridCount
        val cellH = size.height / gridCount

        for (r in 0 until gridCount) {
            for (c in 0 until gridCount) {
                val isFinderTopLeft = r < 7 && c < 7
                val isFinderTopRight = r < 7 && c >= gridCount - 7
                val isFinderBottomLeft = r >= gridCount - 7 && c < 7

                val drawDark = if (isFinderTopLeft || isFinderTopRight || isFinderBottomLeft) {
                    val lr = when {
                        r < 7 -> r
                        else -> r - (gridCount - 7)
                    }
                    val lc = when {
                        c < 7 -> c
                        else -> c - (gridCount - 7)
                    }
                    val onOuterBorder = lr == 0 || lr == 6 || lc == 0 || lc == 6
                    val inInnerCenter = lr in 2..4 && lc in 2..4
                    onOuterBorder || inInnerCenter
                } else {
                    val bitIdx = (r * gridCount + c) % (digestBytes.size * 8)
                    val byteVal = digestBytes[bitIdx / 8].toInt() and 0xFF
                    ((byteVal ushr (bitIdx % 8)) and 1) == 1
                }

                if (drawDark) {
                    drawRoundRect(
                        color = Color(0xFF111827),
                        topLeft = Offset(c * cellW, r * cellH),
                        size = Size(cellW * 0.92f, cellH * 0.92f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }
        }
    }
}
