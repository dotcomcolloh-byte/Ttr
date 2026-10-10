package com.telefam.company.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.TelefamAvatarCircleDark
import com.example.ui.theme.TelefamAvatarCircleLight
import com.example.ui.theme.TelefamRed
import com.example.ui.theme.isTelefamDarkTheme
import com.telefam.company.app.data.PrivacySettingsEntity
import com.telefam.company.app.domain.ChatBackgroundThemeOption
import com.telefam.company.app.domain.MessageBubbleColorOption
import com.telefam.company.app.domain.PrivacyAudience
import com.telefam.company.app.domain.PrivacyRuleKey
import com.telefam.company.app.domain.TelefamBubbleColors
import com.telefam.company.app.domain.TelefamChatThemes

private sealed class ActivePrivacySheet {
    data class AudienceRule(
        val key: PrivacyRuleKey,
        @StringRes val titleRes: Int,
        @StringRes val descRes: Int,
        val icon: ImageVector
    ) : ActivePrivacySheet()

    data object ChatThemes : ActivePrivacySheet()
    data object BubbleColors : ActivePrivacySheet()
}

fun parseHexColorSafe(hex: String, fallback: Color = TelefamRed): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(fallback)
}

/**
 * Privacy, Chat Background Theme (6 Non-Human Image Themes), and Message Bubble Colour Screen
 * matching the uploaded Privacy reference image.
 *
 * Features:
 * - Red Telefam top header with back button, "Privacy" title, and subtitle
 *   "Control who can interact with you and your messages."
 * - Rounded top-corner sheet with all 11 settings rows in exact order:
 *   1. Message Requests (Anyone / Nobody)
 *   2. Who Can Call Me (Anyone / Nobody)
 *   3. Who Can Screenshot Chats (Anyone / Nobody)
 *   4. Who Can Share Chats (Anyone / Nobody)
 *   5. Who Can Copy Messages (Anyone / Nobody)
 *   6. Who Can Download My Media (Anyone / Nobody)
 *   7. Chats Theme (6 Non-Human Image Background Themes)
 *   8. Message Bubbles Colour (10 Colours, defaulting to Telefam Red #C80815 first)
 *   9. Read Receipts (Anyone / Nobody)
 *   10. Typing Indicators (Anyone / Nobody)
 *   11. Online Indicators (Anyone / Nobody)
 * - Responsive across Phones, Tablets, and Desktop (side-by-side Live Theme & Bubble Preview on >= 840.dp).
 * - Offline-First Room persistence + automatic background retry via TelefamPrivacySyncWorker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    privacySettings: PrivacySettingsEntity,
    onBackClick: () -> Unit,
    onUpdatePrivacyRule: (PrivacyRuleKey, PrivacyAudience) -> Unit,
    onUpdateChatTheme: (String) -> Unit,
    onUpdateBubbleColor: (String) -> Unit
) {
    var activeSheet by remember { mutableStateOf<ActivePrivacySheet?>(null) }

    BackHandler {
        if (activeSheet != null) {
            activeSheet = null
        } else {
            onBackClick()
        }
    }

    val isDark = isTelefamDarkTheme()
    val circleTint = if (isDark) TelefamAvatarCircleDark else TelefamAvatarCircleLight
    val selectedTheme = remember(privacySettings.chatThemeId) {
        TelefamChatThemes.findById(privacySettings.chatThemeId)
    }
    val selectedBubbleOption = remember(privacySettings.bubbleColorHex) {
        TelefamBubbleColors.findByHex(privacySettings.bubbleColorHex)
    }
    val selectedBubbleColor = remember(selectedBubbleOption.colorHex) {
        parseHexColorSafe(selectedBubbleOption.colorHex, TelefamRed)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(TelefamRed)
            .testTag("privacy_settings_screen_root")
    ) {
        val isDesktopOrExpanded = maxWidth >= 840.dp
        val isTablet = maxWidth in 600.dp..839.dp
        val isWideScreen = isDesktopOrExpanded || isTablet

        Column(modifier = Modifier.fillMaxSize()) {
            // Red Top Header matching the uploaded Privacy screenshot
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TelefamRed)
                    .statusBarsPadding()
                    .padding(
                        horizontal = if (isDesktopOrExpanded) 28.dp else 16.dp,
                        vertical = 12.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("privacy_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.privacy_title),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.privacy_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.privacy_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp
                        ),
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Rounded Top-Corner White/Surface Sheet
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                if (isDesktopOrExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left Column: Exact 11 Privacy Settings Rows
                        Surface(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            PrivacySettingsRowsList(
                                privacySettings = privacySettings,
                                selectedTheme = selectedTheme,
                                selectedBubbleOption = selectedBubbleOption,
                                selectedBubbleColor = selectedBubbleColor,
                                circleTint = circleTint,
                                onOpenAudienceSheet = { key, titleRes, descRes, icon ->
                                    activeSheet = ActivePrivacySheet.AudienceRule(
                                        key = key,
                                        titleRes = titleRes,
                                        descRes = descRes,
                                        icon = icon
                                    )
                                },
                                onOpenThemesSheet = {
                                    activeSheet = ActivePrivacySheet.ChatThemes
                                },
                                onOpenBubbleColorsSheet = {
                                    activeSheet = ActivePrivacySheet.BubbleColors
                                }
                            )
                        }

                        // Right Column: Live Interactive Desktop Chat Theme & Bubble Colour Preview
                        DesktopLiveThemeAndBubblePanel(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight(),
                            selectedTheme = selectedTheme,
                            selectedBubbleOption = selectedBubbleOption,
                            selectedBubbleColor = selectedBubbleColor,
                            onSelectTheme = onUpdateChatTheme,
                            onSelectBubbleColor = onUpdateBubbleColor
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        PrivacySettingsRowsList(
                            modifier = Modifier
                                .widthIn(max = if (isTablet) 720.dp else 600.dp)
                                .fillMaxSize(),
                            privacySettings = privacySettings,
                            selectedTheme = selectedTheme,
                            selectedBubbleOption = selectedBubbleOption,
                            selectedBubbleColor = selectedBubbleColor,
                            circleTint = circleTint,
                            onOpenAudienceSheet = { key, titleRes, descRes, icon ->
                                activeSheet = ActivePrivacySheet.AudienceRule(
                                    key = key,
                                    titleRes = titleRes,
                                    descRes = descRes,
                                    icon = icon
                                )
                            },
                            onOpenThemesSheet = {
                                activeSheet = ActivePrivacySheet.ChatThemes
                            },
                            onOpenBubbleColorsSheet = {
                                activeSheet = ActivePrivacySheet.BubbleColors
                            }
                        )
                    }
                }
            }
        }

        // Interactive Modal Bottom Sheet for Audience ("Anyone" / "Nobody"),
        // 6 Non-Human Image Chat Themes, or Message Bubbles Colour
        val currentSheet = activeSheet
        if (currentSheet != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { activeSheet = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                when (currentSheet) {
                    is ActivePrivacySheet.AudienceRule -> {
                        val currentAudience = privacySettings.audienceFor(currentSheet.key)
                        PrivacyAudienceSelectorSheetContent(
                            title = stringResource(currentSheet.titleRes),
                            description = stringResource(currentSheet.descRes),
                            icon = currentSheet.icon,
                            circleTint = circleTint,
                            selectedAudience = currentAudience,
                            onSelectAudience = { chosen ->
                                onUpdatePrivacyRule(currentSheet.key, chosen)
                                activeSheet = null
                            }
                        )
                    }

                    is ActivePrivacySheet.ChatThemes -> {
                        ChatBackgroundThemesSheetContent(
                            selectedTheme = selectedTheme,
                            selectedBubbleColor = selectedBubbleColor,
                            isWideScreen = isWideScreen,
                            onSelectTheme = { themeId ->
                                onUpdateChatTheme(themeId)
                            },
                            onDone = { activeSheet = null }
                        )
                    }

                    is ActivePrivacySheet.BubbleColors -> {
                        MessageBubbleColorsSheetContent(
                            selectedTheme = selectedTheme,
                            selectedBubbleOption = selectedBubbleOption,
                            selectedBubbleColor = selectedBubbleColor,
                            onSelectBubbleColor = { hex ->
                                onUpdateBubbleColor(hex)
                            },
                            onDone = { activeSheet = null }
                        )
                    }
                }
            }
        }
    }
}

private fun PrivacySettingsEntity.audienceFor(key: PrivacyRuleKey): PrivacyAudience {
    val raw = when (key) {
        PrivacyRuleKey.MESSAGE_REQUESTS -> messageRequests
        PrivacyRuleKey.WHO_CAN_CALL_ME -> whoCanCallMe
        PrivacyRuleKey.WHO_CAN_SCREENSHOT_CHATS -> whoCanScreenshotChats
        PrivacyRuleKey.WHO_CAN_SHARE_CHATS -> whoCanShareChats
        PrivacyRuleKey.WHO_CAN_COPY_MESSAGES -> whoCanCopyMessages
        PrivacyRuleKey.WHO_CAN_DOWNLOAD_MEDIA -> whoCanDownloadMedia
        PrivacyRuleKey.READ_RECEIPTS -> readReceipts
        PrivacyRuleKey.TYPING_INDICATORS -> typingIndicators
        PrivacyRuleKey.ONLINE_INDICATORS -> onlineIndicators
    }
    return PrivacyAudience.fromWire(raw)
}

@Composable
private fun PrivacySettingsRowsList(
    modifier: Modifier = Modifier,
    privacySettings: PrivacySettingsEntity,
    selectedTheme: ChatBackgroundThemeOption,
    selectedBubbleOption: MessageBubbleColorOption,
    selectedBubbleColor: Color,
    circleTint: Color,
    onOpenAudienceSheet: (PrivacyRuleKey, Int, Int, ImageVector) -> Unit,
    onOpenThemesSheet: () -> Unit,
    onOpenBubbleColorsSheet: () -> Unit
) {
    val anyoneText = stringResource(R.string.privacy_option_anyone)
    val nobodyText = stringResource(R.string.privacy_option_nobody)

    fun labelFor(raw: String): String =
        if (PrivacyAudience.fromWire(raw) == PrivacyAudience.NOBODY) nobodyText else anyoneText

    LazyColumn(
        modifier = modifier
            .navigationBarsPadding()
            .testTag("privacy_settings_lazy_list"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp)
    ) {
        // 1. Message Requests
        item(key = "message_requests") {
            PrivacySettingItemRow(
                testTag = "privacy_row_message_requests",
                icon = Icons.Outlined.ChatBubbleOutline,
                title = stringResource(R.string.privacy_message_requests_title),
                subtitle = stringResource(R.string.privacy_message_requests_desc),
                badgeText = labelFor(privacySettings.messageRequests),
                circleTint = circleTint,
                onClick = {
                    onOpenAudienceSheet(
                        PrivacyRuleKey.MESSAGE_REQUESTS,
                        R.string.privacy_message_requests_title,
                        R.string.privacy_message_requests_desc,
                        Icons.Outlined.ChatBubbleOutline
                    )
                }
            )
            PrivacyRowDivider()
        }

        // 2. Who Can Call Me
        item(key = "who_can_call_me") {
            PrivacySettingItemRow(
                testTag = "privacy_row_who_can_call",
                icon = Icons.Outlined.Call,
                title = stringResource(R.string.privacy_who_can_call_title),
                subtitle = stringResource(R.string.privacy_who_can_call_desc),
                badgeText = labelFor(privacySettings.whoCanCallMe),
                circleTint = circleTint,
                onClick = {
                    onOpenAudienceSheet(
                        PrivacyRuleKey.WHO_CAN_CALL_ME,
                        R.string.privacy_who_can_call_title,
                        R.string.privacy_who_can_call_desc,
                        Icons.Outlined.Call
                    )
                }
            )
            PrivacyRowDivider()
        }

        // 3. Who Can Screenshot Chats
        item(key = "who_can_screenshot_chats") {
            PrivacySettingItemRow(
                testTag = "privacy_row_who_can_screenshot",
                icon = Icons.Outlined.PhoneAndroid,
                title = stringResource(R.string.privacy_who_can_screenshot_title),
                subtitle = stringResource(R.string.privacy_who_can_screenshot_desc),
                badgeText = labelFor(privacySettings.whoCanScreenshotChats),
                circleTint = circleTint,
                onClick = {
                    onOpenAudienceSheet(
                        PrivacyRuleKey.WHO_CAN_SCREENSHOT_CHATS,
                        R.string.privacy_who_can_screenshot_title,
                        R.string.privacy_who_can_screenshot_desc,
                        Icons.Outlined.PhoneAndroid
                    )
                }
            )
            PrivacyRowDivider()
        }

        // 4. Who Can Share Chats
        item(key = "who_can_share_chats") {
            PrivacySettingItemRow(
                testTag = "privacy_row_who_can_share",
                icon = Icons.Outlined.Share,
                title = stringResource(R.string.privacy_who_can_share_title),
                subtitle = stringResource(R.string.privacy_who_can_share_desc),
                badgeText = labelFor(privacySettings.whoCanShareChats),
                circleTint = circleTint,
                onClick = {
                    onOpenAudienceSheet(
                        PrivacyRuleKey.WHO_CAN_SHARE_CHATS,
                        R.string.privacy_who_can_share_title,
                        R.string.privacy_who_can_share_desc,
                        Icons.Outlined.Share
                    )
                }
            )
            PrivacyRowDivider()
        }

        // 5. Who Can Copy Messages
        item(key = "who_can_copy_messages") {
            PrivacySettingItemRow(
                testTag = "privacy_row_who_can_copy",
                icon = Icons.Outlined.ContentCopy,
                title = stringResource(R.string.privacy_who_can_copy_title),
                subtitle = stringResource(R.string.privacy_who_can_copy_desc),
                badgeText = labelFor(privacySettings.whoCanCopyMessages),
                circleTint = circleTint,
                onClick = {
                    onOpenAudienceSheet(
                        PrivacyRuleKey.WHO_CAN_COPY_MESSAGES,
                        R.string.privacy_who_can_copy_title,
                        R.string.privacy_who_can_copy_desc,
                        Icons.Outlined.ContentCopy
                    )
                }
            )
            PrivacyRowDivider()
        }

        // 6. Who Can Download My Media
        item(key = "who_can_download_media") {
            PrivacySettingItemRow(
                testTag = "privacy_row_who_can_download",
                icon = Icons.Outlined.FileDownload,
                title = stringResource(R.string.privacy_who_can_download_title),
                subtitle = stringResource(R.string.privacy_who_can_download_desc),
                badgeText = labelFor(privacySettings.whoCanDownloadMedia),
                circleTint = circleTint,
                onClick = {
                    onOpenAudienceSheet(
                        PrivacyRuleKey.WHO_CAN_DOWNLOAD_MEDIA,
                        R.string.privacy_who_can_download_title,
                        R.string.privacy_who_can_download_desc,
                        Icons.Outlined.FileDownload
                    )
                }
            )
            PrivacyRowDivider()
        }

        // 7. Chats Theme (6 Non-Human Image Background Themes)
        item(key = "chats_theme") {
            PrivacySettingItemRow(
                testTag = "privacy_row_chats_theme",
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.privacy_chats_theme_title),
                subtitle = stringResource(R.string.privacy_chats_theme_desc),
                badgeText = stringResource(selectedTheme.titleRes),
                circleTint = circleTint,
                previewDrawableRes = selectedTheme.drawableRes,
                onClick = onOpenThemesSheet
            )
            PrivacyRowDivider()
        }

        // 8. Message Bubbles Colour (Default to Red #C80815 First)
        item(key = "message_bubbles_colour") {
            PrivacySettingItemRow(
                testTag = "privacy_row_bubble_color",
                icon = Icons.Outlined.FormatPaint,
                title = stringResource(R.string.privacy_bubble_color_title),
                subtitle = stringResource(R.string.privacy_bubble_color_desc),
                badgeText = stringResource(selectedBubbleOption.nameRes),
                circleTint = circleTint,
                previewSwatchColor = selectedBubbleColor,
                onClick = onOpenBubbleColorsSheet
            )
            PrivacyRowDivider()
        }

        // 9. Read Receipts
        item(key = "read_receipts") {
            PrivacySettingItemRow(
                testTag = "privacy_row_read_receipts",
                icon = Icons.Outlined.DoneAll,
                title = stringResource(R.string.privacy_read_receipts_title),
                subtitle = stringResource(R.string.privacy_read_receipts_desc),
                badgeText = labelFor(privacySettings.readReceipts),
                circleTint = circleTint,
                onClick = {
                    onOpenAudienceSheet(
                        PrivacyRuleKey.READ_RECEIPTS,
                        R.string.privacy_read_receipts_title,
                        R.string.privacy_read_receipts_desc,
                        Icons.Outlined.DoneAll
                    )
                }
            )
            PrivacyRowDivider()
        }

        // 10. Typing Indicators
        item(key = "typing_indicators") {
            PrivacySettingItemRow(
                testTag = "privacy_row_typing_indicators",
                icon = Icons.Outlined.MoreHoriz,
                title = stringResource(R.string.privacy_typing_indicators_title),
                subtitle = stringResource(R.string.privacy_typing_indicators_desc),
                badgeText = labelFor(privacySettings.typingIndicators),
                circleTint = circleTint,
                onClick = {
                    onOpenAudienceSheet(
                        PrivacyRuleKey.TYPING_INDICATORS,
                        R.string.privacy_typing_indicators_title,
                        R.string.privacy_typing_indicators_desc,
                        Icons.Outlined.MoreHoriz
                    )
                }
            )
            PrivacyRowDivider()
        }

        // 11. Online Indicators
        item(key = "online_indicators") {
            PrivacySettingItemRow(
                testTag = "privacy_row_online_indicators",
                icon = Icons.Outlined.Visibility,
                title = stringResource(R.string.privacy_online_indicators_title),
                subtitle = stringResource(R.string.privacy_online_indicators_desc),
                badgeText = labelFor(privacySettings.onlineIndicators),
                circleTint = circleTint,
                onClick = {
                    onOpenAudienceSheet(
                        PrivacyRuleKey.ONLINE_INDICATORS,
                        R.string.privacy_online_indicators_title,
                        R.string.privacy_online_indicators_desc,
                        Icons.Outlined.Visibility
                    )
                }
            )
        }
    }
}

@Composable
private fun PrivacyRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.7.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun PrivacySettingItemRow(
    testTag: String,
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    circleTint: Color,
    previewDrawableRes: Int? = null,
    previewSwatchColor: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Soft Pink/Blush Circular Badge with TelefamRed Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(circleTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TelefamRed,
                modifier = Modifier.size(21.dp)
            )
        }

        // Title, Subtitle & Current Value Indicator
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.8.sp,
                    lineHeight = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Current Value Pill / Swatch + Right Arrow
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (previewDrawableRes != null) {
                Image(
                    painter = painterResource(id = previewDrawableRes),
                    contentDescription = badgeText,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, TelefamRed, CircleShape)
                )
            } else if (previewSwatchColor != null) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(previewSwatchColor)
                        .border(
                            1.5.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            CircleShape
                        )
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = circleTint.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = TelefamRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PrivacyAudienceSelectorSheetContent(
    title: String,
    description: String,
    icon: ImageVector,
    circleTint: Color,
    selectedAudience: PrivacyAudience,
    onSelectAudience: (PrivacyAudience) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag("privacy_audience_sheet")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(circleTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = TelefamRed,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Option 1: Anyone
        AudienceOptionCard(
            testTag = "privacy_option_anyone",
            label = stringResource(R.string.privacy_option_anyone),
            selected = selectedAudience == PrivacyAudience.ANYONE,
            onClick = { onSelectAudience(PrivacyAudience.ANYONE) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Option 2: Nobody
        AudienceOptionCard(
            testTag = "privacy_option_nobody",
            label = stringResource(R.string.privacy_option_nobody),
            selected = selectedAudience == PrivacyAudience.NOBODY,
            onClick = { onSelectAudience(PrivacyAudience.NOBODY) }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun AudienceOptionCard(
    testTag: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            TelefamRed.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                TelefamRed
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.5.sp
                ),
                color = if (selected) TelefamRed else MaterialTheme.colorScheme.onSurface
            )
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = TelefamRed
                )
            )
        }
    }
}

@Composable
private fun ChatBackgroundThemesSheetContent(
    selectedTheme: ChatBackgroundThemeOption,
    selectedBubbleColor: Color,
    isWideScreen: Boolean,
    onSelectTheme: (String) -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("chat_themes_sheet")
    ) {
        Text(
            text = stringResource(R.string.privacy_chats_theme_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.privacy_chats_theme_desc),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Live Preview of Selected Non-Human Chat Theme + Message Bubble Colour
        LiveChatWallpaperAndBubblesPreview(
            selectedTheme = selectedTheme,
            selectedBubbleColor = selectedBubbleColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 6 Non-Human Image Background Themes Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isWideScreen) 3 else 2),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(TelefamChatThemes.ALL, key = { it.id }) { themeOption ->
                val isSelected = themeOption.id == selectedTheme.id
                ChatThemeThumbnailCard(
                    themeOption = themeOption,
                    isSelected = isSelected,
                    onClick = { onSelectTheme(themeOption.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("apply_chat_theme_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TelefamRed,
                contentColor = Color.White
            )
        ) {
            Text(
                text = stringResource(R.string.apply_selection),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ChatThemeThumbnailCard(
    themeOption: ChatBackgroundThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val title = stringResource(themeOption.titleRes)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.35f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("chat_theme_card_${themeOption.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) TelefamRed else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = themeOption.drawableRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.68f)
                            )
                        )
                    )
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(TelefamRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun MessageBubbleColorsSheetContent(
    selectedTheme: ChatBackgroundThemeOption,
    selectedBubbleOption: MessageBubbleColorOption,
    selectedBubbleColor: Color,
    onSelectBubbleColor: (String) -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("bubble_colors_sheet")
    ) {
        Text(
            text = stringResource(R.string.privacy_bubble_color_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.privacy_bubble_color_desc),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Live Preview showing selected Bubble Colour on top of selected Non-Human Chat Theme
        LiveChatWallpaperAndBubblesPreview(
            selectedTheme = selectedTheme,
            selectedBubbleColor = selectedBubbleColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 10 Colour Choices (Defaulting to Telefam Red #C80815 first)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(TelefamBubbleColors.ALL, key = { it.colorHex }) { option ->
                val isSelected = option.colorHex.equals(selectedBubbleOption.colorHex, ignoreCase = true)
                val swatchColor = parseHexColorSafe(option.colorHex, TelefamRed)
                val colorName = stringResource(option.nameRes)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectBubbleColor(option.colorHex) }
                        .testTag("bubble_color_option_${option.colorHex.removePrefix("#")}"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        swatchColor.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            swatchColor
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swatchColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = colorName,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = colorName,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.8.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("apply_bubble_color_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TelefamRed,
                contentColor = Color.White
            )
        ) {
            Text(
                text = stringResource(R.string.apply_selection),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun LiveChatWallpaperAndBubblesPreview(
    selectedTheme: ChatBackgroundThemeOption,
    selectedBubbleColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .testTag("live_chat_theme_preview"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = selectedTheme.drawableRes),
                contentDescription = stringResource(selectedTheme.titleRes),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle readability scrim over non-human image wallpaper
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.22f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Incoming chat bubble
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomEnd = 16.dp,
                        bottomStart = 4.dp
                    ),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 2.dp,
                    modifier = Modifier.widthIn(max = 290.dp)
                ) {
                    Text(
                        text = stringResource(R.string.preview_incoming_msg),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.2.sp,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // Outgoing chat bubble in selected bubble colour (Defaults to TelefamRed #C80815)
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 4.dp
                    ),
                    color = selectedBubbleColor,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .widthIn(max = 290.dp)
                ) {
                    Text(
                        text = stringResource(R.string.preview_outgoing_msg),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.2.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopLiveThemeAndBubblePanel(
    modifier: Modifier = Modifier,
    selectedTheme: ChatBackgroundThemeOption,
    selectedBubbleOption: MessageBubbleColorOption,
    selectedBubbleColor: Color,
    onSelectTheme: (String) -> Unit,
    onSelectBubbleColor: (String) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.privacy_chats_theme_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LiveChatWallpaperAndBubblesPreview(
                    selectedTheme = selectedTheme,
                    selectedBubbleColor = selectedBubbleColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                )
            }

            item {
                Text(
                    text = stringResource(R.string.privacy_chats_theme_desc),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TelefamChatThemes.ALL.chunked(3).forEach { rowThemes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowThemes.forEach { themeOption ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ChatThemeThumbnailCard(
                                        themeOption = themeOption,
                                        isSelected = themeOption.id == selectedTheme.id,
                                        onClick = { onSelectTheme(themeOption.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.privacy_bubble_color_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelefamBubbleColors.ALL.forEach { option ->
                        val isSelected = option.colorHex.equals(selectedBubbleOption.colorHex, ignoreCase = true)
                        val swatch = parseHexColorSafe(option.colorHex, TelefamRed)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onSelectBubbleColor(option.colorHex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(option.nameRes),
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
