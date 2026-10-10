package com.telefam.company.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbDownOffAlt
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OutlinedFlag
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TelefamRed
import com.telefam.company.app.data.AccountSettingsAndSecurityEntity
import com.telefam.company.app.domain.AppThemeOption
import com.telefam.company.app.domain.DiscoverableUserItem
import com.telefam.company.app.domain.HelpSupportArticle
import com.telefam.company.app.domain.LegalPolicyDocument
import com.telefam.company.app.domain.PrivacyControlField
import com.telefam.company.app.domain.SafetyTopicGuide
import com.telefam.company.app.domain.SettingsSubScreen
import com.telefam.company.app.domain.SocialPrivacyOption
import com.telefam.company.app.domain.TelefamSettingsCatalog
import com.telefam.company.app.ui.TelefamUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsContentSafetyAndSupportRouter(
    uiState: TelefamUiState,
    cardBackground: Color,
    sectionTitleColor: Color,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit,
    onOpenChatPrivacyAndAppearance: () -> Unit,
    onOpenPrivacyRuleSelector: (PrivacyControlField) -> Unit,
    onSelectPrivacyRule: (PrivacyControlField, SocialPrivacyOption) -> Unit,
    onSetAppThemeMode: (AppThemeOption) -> Unit = {},
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit,
    onBlockedSearchQueryChanged: (String) -> Unit,
    onUnblockAccount: (userId: String, username: String) -> Unit,
    onSubmitTicket: (
        ticketType: String,
        category: String,
        subject: String,
        description: String,
        screenshotUri: Uri?
    ) -> Unit,
    onClearSubmittedTicket: () -> Unit,
    onUpdateSupportTicket: (ticketId: String, followUpText: String, markResolved: Boolean) -> Unit = { _, _, _ -> },
    onOpenSafetyGuide: (SafetyTopicGuide) -> Unit,
    onHelpSearchQueryChanged: (String) -> Unit,
    onOpenHelpArticle: (HelpSupportArticle) -> Unit,
    onSubmitHelpFeedback: (articleId: String, isHelpful: Boolean) -> Unit = { _, _ -> },
    onOpenLegalDocument: (LegalPolicyDocument) -> Unit,
    onAcknowledgeLegalDocument: (LegalPolicyDocument) -> Unit = {},
    onDownloadLegalDocument: (LegalPolicyDocument) -> Unit = {},
    onOpenAboutDetail: (String) -> Unit
) {
    when (uiState.settingsSubScreen) {
        SettingsSubScreen.NOTIFICATIONS -> {
            NotificationsSettingsSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                sectionTitleColor = sectionTitleColor,
                onNavigateSubScreen = onNavigateSubScreen,
                onUpdateAccountSettings = onUpdateAccountSettings
            )
        }

        SettingsSubScreen.NOTIFICATIONS_PUSH_DETAIL -> {
            NotificationsPushDetailSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onUpdateAccountSettings = onUpdateAccountSettings
            )
        }

        SettingsSubScreen.NOTIFICATIONS_EMAIL_DETAIL -> {
            NotificationsEmailDetailSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onUpdateAccountSettings = onUpdateAccountSettings
            )
        }

        SettingsSubScreen.CONTENT_PREFERENCES -> {
            ContentPreferencesMainSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                sectionTitleColor = sectionTitleColor,
                onNavigateSubScreen = onNavigateSubScreen
            )
        }

        SettingsSubScreen.APPEARANCE_THEME -> {
            ContentAppThemeSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onSetAppThemeMode = onSetAppThemeMode
            )
        }

        SettingsSubScreen.CONTENT_LANGUAGES -> {
            ContentLanguagesSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onUpdateAccountSettings = onUpdateAccountSettings
            )
        }

        SettingsSubScreen.CONTENT_TOPICS -> {
            ContentTopicsSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onUpdateAccountSettings = onUpdateAccountSettings
            )
        }

        SettingsSubScreen.CONTENT_SUGGESTED -> {
            ContentSuggestedSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onUpdateAccountSettings = onUpdateAccountSettings
            )
        }

        SettingsSubScreen.CONTENT_VIDEO_QUALITY -> {
            ContentVideoQualitySubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onUpdateAccountSettings = onUpdateAccountSettings
            )
        }

        SettingsSubScreen.CONTENT_AUTOPLAY -> {
            ContentAutoplaySubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onUpdateAccountSettings = onUpdateAccountSettings
            )
        }

        SettingsSubScreen.CONTENT_FILTERING -> {
            ContentFilteringSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onUpdateAccountSettings = onUpdateAccountSettings
            )
        }

        SettingsSubScreen.PRIVACY_CONTROLS -> {
            PrivacyControlsDetailedSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                sectionTitleColor = sectionTitleColor,
                onOpenPrivacyRuleSelector = onOpenPrivacyRuleSelector,
                onOpenChatPrivacyAndAppearance = onOpenChatPrivacyAndAppearance
            )
        }

        SettingsSubScreen.PRIVACY_RULE_SELECTOR -> {
            PrivacyRuleSelectorSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onSelectPrivacyRule = onSelectPrivacyRule
            )
        }

        SettingsSubScreen.BLOCKED_ACCOUNTS -> {
            BlockedAccountsSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onBlockedSearchQueryChanged = onBlockedSearchQueryChanged,
                onUnblockAccount = onUnblockAccount
            )
        }

        SettingsSubScreen.REPORT_PROBLEM -> {
            ReportProblemSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onSubmitTicket = onSubmitTicket,
                onClearSubmittedTicket = onClearSubmittedTicket
            )
        }

        SettingsSubScreen.SAFETY_CENTER -> {
            SafetyCenterSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                sectionTitleColor = sectionTitleColor,
                onNavigateSubScreen = onNavigateSubScreen,
                onOpenSafetyGuide = onOpenSafetyGuide
            )
        }

        SettingsSubScreen.SAFETY_TOPIC_DETAIL -> {
            SafetyTopicDetailSubScreen(
                guide = uiState.activeSafetyGuide,
                cardBackground = cardBackground,
                onNavigateSubScreen = onNavigateSubScreen
            )
        }

        SettingsSubScreen.HELP_AND_SUPPORT -> {
            HelpAndSupportMainSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                sectionTitleColor = sectionTitleColor,
                onHelpSearchQueryChanged = onHelpSearchQueryChanged,
                onOpenHelpArticle = onOpenHelpArticle,
                onNavigateSubScreen = onNavigateSubScreen
            )
        }

        SettingsSubScreen.HELP_ARTICLE_DETAIL -> {
            val feedbackEntries = uiState.accountSettings.helpfulArticlesCsv
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val activeArtId = uiState.activeHelpArticle.id
            val helpfulStatus = when {
                feedbackEntries.contains("$activeArtId:YES") -> true
                feedbackEntries.contains("$activeArtId:NO") -> false
                else -> null
            }
            HelpArticleDetailSubScreen(
                article = uiState.activeHelpArticle,
                helpfulStatus = helpfulStatus,
                cardBackground = cardBackground,
                onSubmitHelpFeedback = onSubmitHelpFeedback,
                onNavigateSubScreen = onNavigateSubScreen
            )
        }

        SettingsSubScreen.HELP_CONTACT_SUPPORT -> {
            HelpContactSupportSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onSubmitTicket = onSubmitTicket,
                onClearSubmittedTicket = onClearSubmittedTicket
            )
        }

        SettingsSubScreen.HELP_MY_REQUESTS -> {
            HelpMySupportRequestsSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onUpdateSupportTicket = onUpdateSupportTicket
            )
        }

        SettingsSubScreen.TERMS_AND_POLICIES -> {
            TermsAndPoliciesSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onOpenLegalDocument = onOpenLegalDocument
            )
        }

        SettingsSubScreen.LEGAL_DOCUMENT_DETAIL -> {
            val activeDoc = uiState.activeLegalDocument
            val ackTimestampMs = uiState.accountSettings.acceptedPoliciesCsv
                .split(",")
                .map { it.trim() }
                .firstOrNull { it.startsWith("${activeDoc.id}:") }
                ?.substringAfter(":")
                ?.toLongOrNull()
            LegalDocumentDetailSubScreen(
                document = activeDoc,
                acknowledgedTimestampMs = ackTimestampMs,
                cardBackground = cardBackground,
                onAcknowledgeLegalDocument = onAcknowledgeLegalDocument,
                onDownloadLegalDocument = onDownloadLegalDocument,
                onNavigateSubScreen = onNavigateSubScreen
            )
        }

        SettingsSubScreen.ABOUT -> {
            AboutAppSubScreen(
                cardBackground = cardBackground,
                sectionTitleColor = sectionTitleColor,
                onOpenAboutDetail = onOpenAboutDetail
            )
        }

        SettingsSubScreen.ABOUT_DETAIL -> {
            AboutDetailSubScreen(
                detailKey = uiState.activeAboutDetailKey,
                cardBackground = cardBackground
            )
        }

        else -> Unit
    }
}

// --- 5. NOTIFICATIONS SUB-SCREENS ---

@Composable
private fun NotificationsSettingsSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    sectionTitleColor: Color,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit,
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit
) {
    val acc = uiState.accountSettings

    SettingsExplanationCard(
        title = "Control your notification alerts",
        description = "Choose which activity notifications you receive on your device or by email."
    )

    SettingsSectionHeader(title = "Interactions", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsSwitchRow(
            title = "Likes",
            subtitle = "Notify me when someone likes my posts or videos",
            checked = acc.notifyLikes,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Likes notifications ${if (enabled) "enabled" else "disabled"}.",
                    "NotifLikes -> $enabled"
                ) { it.copy(notifyLikes = enabled) }
            },
            showDivider = true,
            testTag = "notif_switch_likes"
        )
        SettingsSwitchRow(
            title = "Comments",
            subtitle = "Notify me when someone comments on my content",
            checked = acc.notifyComments,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Comments notifications ${if (enabled) "enabled" else "disabled"}.",
                    "NotifComments -> $enabled"
                ) { it.copy(notifyComments = enabled) }
            },
            showDivider = true,
            testTag = "notif_switch_comments"
        )
        SettingsSwitchRow(
            title = "New followers",
            subtitle = "Notify me when someone starts following me",
            checked = acc.notifyNewFollowers,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "New followers notifications ${if (enabled) "enabled" else "disabled"}.",
                    "NotifNewFollowers -> $enabled"
                ) { it.copy(notifyNewFollowers = enabled) }
            },
            showDivider = true,
            testTag = "notif_switch_new_followers"
        )
        SettingsSwitchRow(
            title = "Mentions",
            subtitle = "Notify me when someone @mentions my username",
            checked = acc.notifyMentions,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Mentions notifications ${if (enabled) "enabled" else "disabled"}.",
                    "NotifMentions -> $enabled"
                ) { it.copy(notifyMentions = enabled) }
            },
            showDivider = false,
            testTag = "notif_switch_mentions"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Messages", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsSwitchRow(
            title = "Direct messages",
            subtitle = "Receive alerts for new direct messages and chat requests",
            checked = acc.notifyDirectMessages,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Direct message notifications ${if (enabled) "enabled" else "disabled"}.",
                    "NotifDirectMessages -> $enabled"
                ) { it.copy(notifyDirectMessages = enabled) }
            },
            showDivider = false,
            testTag = "notif_switch_direct_messages"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Content", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsSwitchRow(
            title = "Recommended content",
            subtitle = "Suggested posts and videos based on your interests",
            checked = acc.notifyRecommendedContent,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Recommended content notifications ${if (enabled) "enabled" else "disabled"}.",
                    "NotifRecommendedContent -> $enabled"
                ) { it.copy(notifyRecommendedContent = enabled) }
            },
            showDivider = true,
            testTag = "notif_switch_recommended"
        )
        SettingsSwitchRow(
            title = "Live notifications",
            subtitle = "Alerts when creators you follow go live",
            checked = acc.notifyLiveNotifications,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Live notifications ${if (enabled) "enabled" else "disabled"}.",
                    "NotifLive -> $enabled"
                ) { it.copy(notifyLiveNotifications = enabled) }
            },
            showDivider = false,
            testTag = "notif_switch_live"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Other", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsSwitchRow(
            title = "Email notifications",
            subtitle = "Receive account updates and summaries via email",
            checked = acc.notifyEmailNotifications,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Email notifications ${if (enabled) "enabled" else "disabled"}.",
                    "NotifEmail -> $enabled"
                ) { it.copy(notifyEmailNotifications = enabled) }
            },
            showDivider = false,
            testTag = "notif_switch_email"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Notification settings", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.Notifications,
            title = "Push notifications",
            subtitle = "Pause all alerts or schedule quiet hours",
            valueLabel = if (!acc.pushMasterEnabled) "Paused" else "Active",
            onClick = { onNavigateSubScreen(SettingsSubScreen.NOTIFICATIONS_PUSH_DETAIL) },
            showDivider = true,
            testTag = "notif_row_push_detail"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Email,
            title = "Email",
            subtitle = "Choose which email digests and product updates you receive",
            onClick = { onNavigateSubScreen(SettingsSubScreen.NOTIFICATIONS_EMAIL_DETAIL) },
            showDivider = false,
            testTag = "notif_row_email_detail"
        )
    }
}

@Composable
private fun NotificationsPushDetailSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit
) {
    val acc = uiState.accountSettings

    SettingsExplanationCard(
        title = "Push notification controls",
        description = "Customize how push alerts are delivered to your phone, including temporary pause and nighttime quiet hours."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsSwitchRow(
            title = "Pause all push notifications",
            subtitle = "Temporarily mute non-security push alerts on this device",
            checked = !acc.pushMasterEnabled,
            onCheckedChange = { paused ->
                onUpdateAccountSettings(
                    if (paused) "Push notifications paused." else "Push notifications resumed.",
                    "PushMasterEnabled -> ${!paused}"
                ) { it.copy(pushMasterEnabled = !paused) }
            },
            showDivider = true,
            testTag = "push_pause_all_switch"
        )
        SettingsSwitchRow(
            title = "Quiet hours (22:00 – 07:00)",
            subtitle = "Deliver notifications silently overnight without sound or vibration",
            checked = acc.pushQuietHoursEnabled,
            onCheckedChange = { quiet ->
                onUpdateAccountSettings(
                    "Quiet hours ${if (quiet) "enabled" else "disabled"}.",
                    "PushQuietHours -> $quiet"
                ) { it.copy(pushQuietHoursEnabled = quiet) }
            },
            showDivider = false,
            testTag = "push_quiet_hours_switch"
        )
    }
}

@Composable
private fun NotificationsEmailDetailSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit
) {
    val acc = uiState.accountSettings

    SettingsExplanationCard(
        title = "Email notification preferences",
        description = "Choose exactly what you receive in your email inbox. Security and verification OTP emails are always enabled to protect your account."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsSwitchRow(
            title = "Security & login alerts",
            subtitle = "Critical alerts for new device sign-ins and OTP verification codes",
            checked = acc.emailSecurityAlerts,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Email security alerts ${if (enabled) "enabled" else "updated"}.",
                    "EmailSecurityAlerts -> $enabled"
                ) { it.copy(emailSecurityAlerts = enabled) }
            },
            showDivider = true,
            testTag = "email_security_alerts_switch"
        )
        SettingsSwitchRow(
            title = "Product updates & tips",
            subtitle = "Learn about new Telefam features and creator tools",
            checked = acc.emailProductUpdates,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Product update emails ${if (enabled) "enabled" else "disabled"}.",
                    "EmailProductUpdates -> $enabled"
                ) { it.copy(emailProductUpdates = enabled) }
            },
            showDivider = true,
            testTag = "email_product_updates_switch"
        )
        SettingsSwitchRow(
            title = "Weekly community digest",
            subtitle = "A weekly summary of top posts from people you follow",
            checked = acc.notifyEmailNotifications,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Weekly digest emails ${if (enabled) "enabled" else "disabled"}.",
                    "EmailWeeklyDigest -> $enabled"
                ) { it.copy(notifyEmailNotifications = enabled) }
            },
            showDivider = false,
            testTag = "email_weekly_digest_switch"
        )
    }
}

// --- 6. CONTENT PREFERENCES SUB-SCREENS ---

@Composable
private fun ContentPreferencesMainSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    sectionTitleColor: Color,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit
) {
    val acc = uiState.accountSettings

    SettingsExplanationCard(
        title = "Customize appearance and content preferences",
        description = "Control your app theme, preferred languages, topics, playback quality, autoplay behavior, and sensitivity filtering."
    )

    SettingsSectionHeader(title = "Display", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.DarkMode,
            title = "Appearance",
            subtitle = "Switch between Light, Dark, or System preference",
            valueLabel = AppThemeOption.fromWire(acc.appThemeMode).label,
            onClick = { onNavigateSubScreen(SettingsSubScreen.APPEARANCE_THEME) },
            showDivider = false,
            testTag = "content_row_app_theme"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Content preferences", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            title = "Languages",
            subtitle = "Languages you prefer for videos and posts",
            valueLabel = acc.contentLanguagesCsv,
            onClick = { onNavigateSubScreen(SettingsSubScreen.CONTENT_LANGUAGES) },
            showDivider = true,
            testTag = "content_row_languages"
        )
        SettingsNavRow(
            title = "Topics and interests",
            subtitle = "Choose topics to tailor your feed",
            valueLabel = acc.selectedTopicsCsv,
            onClick = { onNavigateSubScreen(SettingsSubScreen.CONTENT_TOPICS) },
            showDivider = true,
            testTag = "content_row_topics"
        )
        SettingsNavRow(
            title = "Suggested content",
            subtitle = "Manage personalized recommendations or reset feed",
            valueLabel = if (acc.suggestedPrioritizeFollowing) "Personalized" else "Standard",
            onClick = { onNavigateSubScreen(SettingsSubScreen.CONTENT_SUGGESTED) },
            showDivider = true,
            testTag = "content_row_suggested"
        )
        SettingsNavRow(
            title = "Video quality",
            subtitle = "Adjust streaming resolution and data usage",
            valueLabel = acc.videoQualityMode,
            onClick = { onNavigateSubScreen(SettingsSubScreen.CONTENT_VIDEO_QUALITY) },
            showDivider = true,
            testTag = "content_row_video_quality"
        )
        SettingsNavRow(
            title = "Autoplay",
            subtitle = "Control when videos play automatically",
            valueLabel = acc.autoplayMode,
            onClick = { onNavigateSubScreen(SettingsSubScreen.CONTENT_AUTOPLAY) },
            showDivider = true,
            testTag = "content_row_autoplay"
        )
        SettingsNavRow(
            title = "Content filtering",
            subtitle = "Manage sensitive content levels",
            valueLabel = acc.contentFilteringLevel.replace("_", " "),
            onClick = { onNavigateSubScreen(SettingsSubScreen.CONTENT_FILTERING) },
            showDivider = false,
            testTag = "content_row_filtering"
        )
    }
}

@Composable
private fun ContentAppThemeSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onSetAppThemeMode: (AppThemeOption) -> Unit
) {
    val currentPref = AppThemeOption.fromWire(uiState.accountSettings.appThemeMode)

    SettingsExplanationCard(
        title = "App appearance & theme",
        description = "Choose your preferred display theme for Telefam."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        AppThemeOption.entries.forEachIndexed { idx, pref ->
            SettingsRadioRow(
                title = pref.label,
                subtitle = pref.description,
                selected = currentPref == pref,
                onClick = { onSetAppThemeMode(pref) },
                showDivider = idx < AppThemeOption.entries.lastIndex,
                testTag = "app_theme_option_${pref.wireCode}"
            )
        }
    }
}

@Composable
private fun ContentLanguagesSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit
) {
    val selectedSet = uiState.accountSettings.contentLanguagesCsv
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    SettingsExplanationCard(
        title = "Content languages",
        description = "Select the languages you understand. The recommendation system uses these preferences when selecting suggested content and creators."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        TelefamSettingsCatalog.AVAILABLE_CONTENT_LANGUAGES.forEachIndexed { idx, lang ->
            val isChecked = lang in selectedSet
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val nextSet = if (isChecked && selectedSet.size > 1) {
                                selectedSet - lang
                            } else {
                                selectedSet + lang
                            }
                            val csv = nextSet.joinToString(",")
                            onUpdateAccountSettings(
                                "Content languages updated ($csv).",
                                "ContentLanguages -> $csv"
                            ) { it.copy(contentLanguagesCsv = csv) }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("content_lang_$lang"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = TelefamRed)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = lang,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (idx < TelefamSettingsCatalog.AVAILABLE_CONTENT_LANGUAGES.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContentTopicsSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit
) {
    val selectedTopics = uiState.accountSettings.selectedTopicsCsv
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    SettingsExplanationCard(
        title = "Topics and interests",
        description = "Tap topics you care about to prioritize relevant creators, videos, and community discussions."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelefamSettingsCatalog.AVAILABLE_CONTENT_TOPICS.forEach { topic ->
                    val active = topic in selectedTopics
                    Surface(
                        onClick = {
                            val next = if (active && selectedTopics.size > 1) {
                                selectedTopics - topic
                            } else {
                                selectedTopics + topic
                            }
                            val csv = next.joinToString(",")
                            onUpdateAccountSettings(
                                "Topics updated ($csv).",
                                "ContentTopics -> $csv"
                            ) { it.copy(selectedTopicsCsv = csv) }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (active) TelefamRed else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("topic_chip_$topic")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (active) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = topic,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentSuggestedSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit
) {
    val acc = uiState.accountSettings

    SettingsExplanationCard(
        title = "Suggested content controls",
        description = "Manage how Telefam suggests videos, posts, and people you may know, or refresh your recommendation history."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsSwitchRow(
            title = "Personalized recommendations",
            subtitle = "Suggest content based on your selected languages, topics, and interactions",
            checked = acc.suggestedPrioritizeFollowing,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Suggested content personalization ${if (enabled) "enabled" else "disabled"}.",
                    "SuggestedPersonalized -> $enabled"
                ) { it.copy(suggestedPrioritizeFollowing = enabled) }
            },
            showDivider = false,
            testTag = "suggested_personalized_switch"
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Refresh recommendation feed", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "If your suggested feed no longer matches your interests, reset your recommendation signals to start fresh.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    onUpdateAccountSettings(
                        "Recommendation feed refreshed with your active languages and topics.",
                        "ResetSuggestedFeed"
                    ) {
                        it.copy(
                            selectedTopicsCsv = "Technology,Community,Education",
                            suggestedPrioritizeFollowing = true
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TelefamRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_suggested_feed_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = TelefamRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset suggested content", color = TelefamRed, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ContentVideoQualitySubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit
) {
    val current = uiState.accountSettings.videoQualityMode
    val options = listOf(
        Triple("AUTO", "Auto (Recommended)", "Adjusts video quality automatically based on your network connection speed"),
        Triple("HIGH_1080P", "Highest quality (1080p HD)", "Always stream in crisp high definition; uses more mobile data"),
        Triple("DATA_SAVER", "Data saver", "Reduces video resolution on cellular networks to save up to 50% data")
    )

    SettingsExplanationCard(
        title = "Video playback quality",
        description = "Choose how Telefam balances video clarity and mobile data consumption."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        options.forEachIndexed { idx, (code, label, sub) ->
            SettingsRadioRow(
                title = label,
                subtitle = sub,
                selected = current == code,
                onClick = {
                    onUpdateAccountSettings(
                        "Video quality set to $label.",
                        "VideoQuality -> $code"
                    ) { it.copy(videoQualityMode = code) }
                },
                showDivider = idx < options.lastIndex,
                testTag = "video_quality_$code"
            )
        }
    }
}

@Composable
private fun ContentAutoplaySubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit
) {
    val current = uiState.accountSettings.autoplayMode
    val options = listOf(
        Triple("WIFI_AND_CELLULAR", "On mobile data and Wi-Fi", "Videos play automatically as you scroll"),
        Triple("WIFI_ONLY", "On Wi-Fi only", "Videos autoplay only when connected to Wi-Fi"),
        Triple("NEVER", "Never autoplay videos", "Tap any video manually when you want to watch it")
    )

    SettingsExplanationCard(
        title = "Video autoplay",
        description = "Control whether videos start playing automatically in your feed."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        options.forEachIndexed { idx, (code, label, sub) ->
            SettingsRadioRow(
                title = label,
                subtitle = sub,
                selected = current == code,
                onClick = {
                    onUpdateAccountSettings(
                        "Autoplay updated to $label.",
                        "Autoplay -> $code"
                    ) { it.copy(autoplayMode = code) }
                },
                showDivider = idx < options.lastIndex,
                testTag = "autoplay_mode_$code"
            )
        }
    }
}

@Composable
private fun ContentFilteringSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit
) {
    val current = uiState.accountSettings.contentFilteringLevel
    val options = listOf(
        Triple(
            "LESS_SENSITIVE",
            "Less sensitive content",
            "Applies strict filtering to hide mature discussions, dramatic news, or borderline topics"
        ),
        Triple(
            "STANDARD",
            "Standard (Recommended)",
            "Balanced filtering that hides graphic or inappropriate material while keeping broad community topics"
        ),
        Triple(
            "MORE_SENSITIVE",
            "More sensitive content",
            "Shows a wider range of mature community discussions that still comply with Community Guidelines"
        )
    )

    SettingsExplanationCard(
        title = "Content filtering",
        description = "Choose how much potentially sensitive content may appear in your suggested feed and discovery results."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        options.forEachIndexed { idx, (code, label, sub) ->
            SettingsRadioRow(
                title = label,
                subtitle = sub,
                selected = current == code,
                onClick = {
                    onUpdateAccountSettings(
                        "Content filtering set to $label.",
                        "ContentFiltering -> $code"
                    ) { it.copy(contentFilteringLevel = code) }
                },
                showDivider = idx < options.lastIndex,
                testTag = "content_filter_$code"
            )
        }
    }
}

// --- 7. PRIVACY CONTROLS & SELECTOR SUB-SCREENS ---

@Composable
private fun PrivacyControlsDetailedSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    sectionTitleColor: Color,
    onOpenPrivacyRuleSelector: (PrivacyControlField) -> Unit,
    onOpenChatPrivacyAndAppearance: () -> Unit
) {
    val acc = uiState.accountSettings

    SettingsExplanationCard(
        title = "Detailed privacy management",
        description = "Configure granular visibility, interaction permissions, and content reuse rules. Tap any setting to change who has access."
    )

    SettingsSectionHeader(title = "Visibility", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            title = "Who can see my videos",
            subtitle = "Control audience visibility for your published videos",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanSeeVideos, true).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.WHO_CAN_SEE_VIDEOS) },
            showDivider = true,
            testTag = "privacy_ctrl_videos"
        )
        SettingsNavRow(
            title = "Who can see my likes",
            subtitle = "Control who can view posts you have liked",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanSeeLikes, true).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.WHO_CAN_SEE_LIKES) },
            showDivider = true,
            testTag = "privacy_ctrl_likes"
        )
        SettingsNavRow(
            title = "Who can see my following and followers list",
            subtitle = "Control visibility of your social graph",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanSeeFollowList, true).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.WHO_CAN_SEE_FOLLOW_LIST) },
            showDivider = false,
            testTag = "privacy_ctrl_follow_list"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Interactions", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            title = "Comments",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanComment, false).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.COMMENTS) },
            showDivider = true,
            testTag = "privacy_ctrl_comments"
        )
        SettingsNavRow(
            title = "Mentions",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanMention, false).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.MENTIONS) },
            showDivider = true,
            testTag = "privacy_ctrl_mentions"
        )
        SettingsNavRow(
            title = "Tags",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanTag, false).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.TAGS) },
            showDivider = true,
            testTag = "privacy_ctrl_tags"
        )
        SettingsNavRow(
            title = "Messages",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanMessage, false).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.MESSAGES) },
            showDivider = false,
            testTag = "privacy_ctrl_messages"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Content permissions", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            title = "Downloads",
            subtitle = "Allow others to save your videos and photos",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanDownload, false).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.DOWNLOADS) },
            showDivider = true,
            testTag = "privacy_ctrl_downloads"
        )
        SettingsNavRow(
            title = "Reuse/remix",
            subtitle = "Allow others to remix or duet with your posts",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanReuseRemix, false).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.REUSE_REMIX) },
            showDivider = true,
            testTag = "privacy_ctrl_reuse_remix"
        )
        SettingsNavRow(
            title = "Chat privacy & appearance",
            subtitle = "Read receipts, last seen, chat themes, and bubble colors",
            onClick = onOpenChatPrivacyAndAppearance,
            showDivider = false,
            testTag = "privacy_ctrl_chat_appearance"
        )
    }
}

@Composable
private fun PrivacyRuleSelectorSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onSelectPrivacyRule: (PrivacyControlField, SocialPrivacyOption) -> Unit
) {
    val field = uiState.activePrivacyControlField
    val acc = uiState.accountSettings
    val currentWire = when (field) {
        PrivacyControlField.WHO_CAN_SEE_VIDEOS -> acc.whoCanSeeVideos
        PrivacyControlField.WHO_CAN_SEE_LIKES -> acc.whoCanSeeLikes
        PrivacyControlField.WHO_CAN_SEE_FOLLOW_LIST -> acc.whoCanSeeFollowList
        PrivacyControlField.COMMENTS -> acc.whoCanComment
        PrivacyControlField.MENTIONS -> acc.whoCanMention
        PrivacyControlField.TAGS -> acc.whoCanTag
        PrivacyControlField.MESSAGES -> acc.whoCanMessage
        PrivacyControlField.DOWNLOADS -> acc.whoCanDownload
        PrivacyControlField.REUSE_REMIX -> acc.whoCanReuseRemix
    }
    val selectedOption = SocialPrivacyOption.fromWire(currentWire, field.usesOnlyMeOption)
    val options = SocialPrivacyOption.optionsFor(field)

    SettingsExplanationCard(
        title = field.questionTitle,
        description = field.explanation
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        options.forEachIndexed { idx, option ->
            SettingsRadioRow(
                title = option.label,
                subtitle = option.subtitle,
                selected = selectedOption == option,
                onClick = { onSelectPrivacyRule(field, option) },
                showDivider = idx < options.lastIndex,
                testTag = "privacy_option_${field.name}_${option.wireCode}"
            )
        }
    }
}

// --- 8. BLOCKED ACCOUNTS SUB-SCREEN ---

@Composable
private fun BlockedAccountsSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onBlockedSearchQueryChanged: (String) -> Unit,
    onUnblockAccount: (userId: String, username: String) -> Unit
) {
    var pendingUnblockUser by remember { mutableStateOf<DiscoverableUserItem?>(null) }

    SettingsExplanationCard(
        title = "Manage blocked accounts",
        description = "Blocked accounts cannot view your profile, message you, or interact with your posts. Unblocking allows them to see your public posts again according to your privacy settings, and does not automatically follow them."
    )

    OutlinedTextField(
        value = uiState.blockedAccountsSearchQuery,
        onValueChange = onBlockedSearchQueryChanged,
        placeholder = { Text("Search blocked accounts") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .testTag("blocked_accounts_search_input")
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        if (uiState.blockedAccountItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null,
                    tint = Color(0xFF9AA4B2),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.blockedAccountsSearchQuery.isBlank()) {
                        "No blocked accounts"
                    } else {
                        "No blocked accounts match \"${uiState.blockedAccountsSearchQuery}\""
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Accounts you block from profiles or contacts will appear here.",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7888)
                )
            }
        } else {
            uiState.blockedAccountItems.forEachIndexed { idx, item ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TelefamRed.copy(alpha = 0.14f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = item.fullName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = TelefamRed
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "@${item.username}",
                                fontSize = 13.sp,
                                color = Color(0xFF6B7888)
                            )
                        }
                        OutlinedButton(
                            onClick = { pendingUnblockUser = item },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TelefamRed),
                            modifier = Modifier.testTag("unblock_button_${item.username}")
                        ) {
                            Text("Unblock", color = TelefamRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    if (idx < uiState.blockedAccountItems.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    pendingUnblockUser?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingUnblockUser = null },
            title = { Text("Unblock @${target.username}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "After unblocking, @${target.username} will be able to see your public posts and interact with you according to your privacy settings. Unblocking does not automatically follow them again."
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingUnblockUser = null }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val user = target
                        pendingUnblockUser = null
                        onUnblockAccount(user.userId, user.username)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                    modifier = Modifier.testTag("confirm_unblock_dialog_button")
                ) {
                    Text("Unblock", color = Color.White)
                }
            }
        )
    }
}

// --- 9. REPORT A PROBLEM SUB-SCREEN ---

@Composable
private fun ReportProblemSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onSubmitTicket: (
        ticketType: String,
        category: String,
        subject: String,
        description: String,
        screenshotUri: Uri?
    ) -> Unit,
    onClearSubmittedTicket: () -> Unit
) {
    var selectedCategory by rememberSaveable { mutableStateOf(TelefamSettingsCatalog.PROBLEM_REPORT_CATEGORIES.first()) }
    var descriptionText by rememberSaveable { mutableStateOf("") }
    var screenshotUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val screenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            screenshotUriString = uri.toString()
        }
    }

    if (!uiState.lastSubmittedTicketId.isNullOrBlank()) {
        // Step 3: Confirmation Screen matching specification
        SettingsGroupCard(cardBackground = cardBackground) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF1B7F3B),
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Report submitted",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reference ID: ${uiState.lastSubmittedTicketId}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TelefamRed
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Thank you for helping us improve the platform.\nWe'll review your report and contact you if more information is needed.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        descriptionText = ""
                        screenshotUriString = null
                        onClearSubmittedTicket()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit another report", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }

    SettingsExplanationCard(
        title = "Report a problem or technical issue",
        description = "Select a category, describe what happened, and optionally attach a screenshot. Our support and engineering teams review all reports."
    )

    // Step 1: Select category
    SettingsSectionHeader(title = "Step 1 — Select category")
    SettingsGroupCard(cardBackground = cardBackground) {
        TelefamSettingsCatalog.PROBLEM_REPORT_CATEGORIES.forEachIndexed { idx, cat ->
            SettingsRadioRow(
                title = cat,
                selected = selectedCategory == cat,
                onClick = { selectedCategory = cat },
                showDivider = idx < TelefamSettingsCatalog.PROBLEM_REPORT_CATEGORIES.lastIndex,
                testTag = "report_problem_cat_$cat"
            )
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Step 2: Describe the issue & attach screenshot
    SettingsSectionHeader(title = "Step 2 — Describe the issue")
    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = { Text("Tell us what happened and steps to reproduce") },
                minLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_problem_description_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    screenshotPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_problem_attach_screenshot_button")
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (screenshotUriString != null) "Screenshot attached ✓ (Tap to change)" else "Attach screenshot",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSubmitTicket(
                        "PROBLEM_REPORT",
                        selectedCategory,
                        "Problem report: $selectedCategory",
                        descriptionText,
                        screenshotUriString?.let { Uri.parse(it) }
                    )
                },
                enabled = descriptionText.trim().length >= 8,
                colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("report_problem_submit_button")
            ) {
                Text("Submit report", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// --- 10. SAFETY CENTER & TOPIC DETAIL SUB-SCREENS ---

@Composable
private fun SafetyCenterSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    sectionTitleColor: Color,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit,
    onOpenSafetyGuide: (SafetyTopicGuide) -> Unit
) {
    val acc = uiState.accountSettings

    SettingsExplanationCard(
        title = "Telefam Safety Center",
        description = "Review your account protection status, explore safety guides, and access direct controls for privacy, harassment prevention, and digital wellbeing."
    )

    SettingsSectionHeader(title = "Account safety status", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.VerifiedUser,
            title = "2-step verification",
            subtitle = if (acc.twoStepEnabled) "Active (${acc.twoStepMethod}) • Protects new device sign-ins" else "Recommended • Add a second verification step",
            valueLabel = if (acc.twoStepEnabled) "Active" else "Set up",
            onClick = { onNavigateSubScreen(SettingsSubScreen.SECURITY_TWO_STEP) },
            showDivider = true,
            testTag = "safety_checkup_two_step"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Lock,
            title = "Passkey sign-in",
            subtitle = if (acc.passkeyEnabled) "Your passkey is saved. You can log in next time with passkey" else "Sign in securely with device screen lock or biometrics",
            valueLabel = if (acc.passkeyEnabled) "Saved" else "Set up",
            onClick = { onNavigateSubScreen(SettingsSubScreen.SECURITY_PASSKEY) },
            showDivider = true,
            testTag = "safety_checkup_passkey"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Block,
            title = "Audience & interaction privacy",
            subtitle = if (acc.isPrivateAccount) "Private account • Only approved followers can view" else "Public account • Customize who can comment, mention, or message",
            valueLabel = if (acc.isPrivateAccount) "Private" else "Manage",
            onClick = { onNavigateSubScreen(SettingsSubScreen.PRIVACY_CONTROLS) },
            showDivider = false,
            testTag = "safety_checkup_privacy"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Safety guides & resources", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        TelefamSettingsCatalog.SAFETY_GUIDES.forEachIndexed { idx, guide ->
            SettingsNavRow(
                icon = Icons.Outlined.VerifiedUser,
                title = guide.title,
                subtitle = guide.subtitle,
                onClick = { onOpenSafetyGuide(guide) },
                showDivider = idx < TelefamSettingsCatalog.SAFETY_GUIDES.lastIndex,
                testTag = "safety_topic_${guide.id}"
            )
        }
    }
}

@Composable
private fun SafetyTopicDetailSubScreen(
    guide: SafetyTopicGuide,
    cardBackground: Color,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit
) {
    SettingsExplanationCard(
        title = guide.title,
        description = guide.overview
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Recommended safety steps",
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            guide.bulletPoints.forEach { point ->
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = TelefamRed,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = point,
                        fontSize = 13.8.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = { onNavigateSubScreen(guide.targetSubScreen) },
                colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("safety_guide_action_button")
            ) {
                Text(guide.primaryActionLabel, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            if (guide.secondarySubScreen != guide.targetSubScreen) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onNavigateSubScreen(guide.secondarySubScreen) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("safety_guide_secondary_button")
                ) {
                    Text(guide.secondaryActionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// --- 11. HELP & SUPPORT SUB-SCREENS ---

@Composable
private fun HelpAndSupportMainSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    sectionTitleColor: Color,
    onHelpSearchQueryChanged: (String) -> Unit,
    onOpenHelpArticle: (HelpSupportArticle) -> Unit,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit
) {
    val query = uiState.helpSearchQuery.trim()
    val filteredArticles = if (query.isBlank()) {
        TelefamSettingsCatalog.HELP_ARTICLES
    } else {
        TelefamSettingsCatalog.HELP_ARTICLES.filter { art ->
            art.categoryTitle.contains(query, ignoreCase = true) ||
                art.title.contains(query, ignoreCase = true) ||
                art.summary.contains(query, ignoreCase = true) ||
                art.keywords.any { it.contains(query, ignoreCase = true) }
        }
    }

    OutlinedTextField(
        value = uiState.helpSearchQuery,
        onValueChange = onHelpSearchQueryChanged,
        placeholder = { Text("Search help articles") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("help_search_input")
    )

    SettingsSectionHeader(title = "Popular topics", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        if (filteredArticles.isEmpty()) {
            Box(modifier = Modifier.padding(18.dp)) {
                Text("No help articles match \"$query\". Try contacting support below.", fontSize = 13.5.sp)
            }
        } else {
            filteredArticles.forEachIndexed { idx, article ->
                SettingsNavRow(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    title = article.categoryTitle,
                    subtitle = article.title,
                    onClick = { onOpenHelpArticle(article) },
                    showDivider = idx < filteredArticles.lastIndex,
                    testTag = "help_topic_${article.id}"
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Still need help?", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.SupportAgent,
            title = "Contact support",
            subtitle = "Submit a direct support ticket to our team",
            onClick = { onNavigateSubScreen(SettingsSubScreen.HELP_CONTACT_SUPPORT) },
            showDivider = true,
            testTag = "help_row_contact_support"
        )
        SettingsNavRow(
            icon = Icons.Outlined.OutlinedFlag,
            title = "Report a problem",
            subtitle = "Report a technical bug or feature issue",
            onClick = { onNavigateSubScreen(SettingsSubScreen.REPORT_PROBLEM) },
            showDivider = true,
            testTag = "help_row_report_problem"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Description,
            title = "My support requests",
            subtitle = "Track status and add follow-ups to your submitted requests",
            valueLabel = "${uiState.supportTickets.size}",
            onClick = { onNavigateSubScreen(SettingsSubScreen.HELP_MY_REQUESTS) },
            showDivider = false,
            testTag = "help_row_my_requests"
        )
    }
}

@Composable
private fun HelpArticleDetailSubScreen(
    article: HelpSupportArticle,
    helpfulStatus: Boolean?,
    cardBackground: Color,
    onSubmitHelpFeedback: (articleId: String, isHelpful: Boolean) -> Unit,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit
) {
    SettingsExplanationCard(
        title = article.title,
        description = article.summary
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Step-by-step instructions",
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            article.steps.forEachIndexed { idx, step ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${idx + 1}.",
                        fontWeight = FontWeight.Bold,
                        color = TelefamRed,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(
                        text = step,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { onNavigateSubScreen(article.relatedSubScreen) },
                colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("help_article_primary_action_button")
            ) {
                Text(article.actionButtonLabel, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Was this article helpful?",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onSubmitHelpFeedback(article.id, true) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (helpfulStatus == true) Color(0xFF1B7F3B) else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("help_article_helpful_yes")
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUpOffAlt,
                        contentDescription = null,
                        tint = if (helpfulStatus == true) Color(0xFF1B7F3B) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (helpfulStatus == true) "Helpful ✓" else "Yes, helpful",
                        color = if (helpfulStatus == true) Color(0xFF1B7F3B) else MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = { onSubmitHelpFeedback(article.id, false) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (helpfulStatus == false) TelefamRed else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("help_article_helpful_no")
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbDownOffAlt,
                        contentDescription = null,
                        tint = if (helpfulStatus == false) TelefamRed else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (helpfulStatus == false) "Recorded" else "Needs work",
                        color = if (helpfulStatus == false) TelefamRed else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onNavigateSubScreen(SettingsSubScreen.HELP_CONTACT_SUPPORT) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Still need help? Contact support")
            }
        }
    }
}

@Composable
private fun HelpContactSupportSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onSubmitTicket: (
        ticketType: String,
        category: String,
        subject: String,
        description: String,
        screenshotUri: Uri?
    ) -> Unit,
    onClearSubmittedTicket: () -> Unit
) {
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var screenshotUriStr by rememberSaveable { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) screenshotUriStr = uri.toString()
    }

    if (!uiState.lastSubmittedTicketId.isNullOrBlank()) {
        SettingsGroupCard(cardBackground = cardBackground) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1B7F3B), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Support request submitted", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Ticket ID: ${uiState.lastSubmittedTicketId}", fontFamily = FontFamily.Monospace, color = TelefamRed)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Our support team will review your request and respond via your verified email address.", fontSize = 13.5.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        subject = ""
                        description = ""
                        screenshotUriStr = null
                        onClearSubmittedTicket()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed)
                ) {
                    Text("Done", color = Color.White)
                }
            }
        }
        return
    }

    SettingsExplanationCard(
        title = "Contact Telefam Support",
        description = "Provide a subject and details about your request. You can track all submitted requests in 'My support requests'."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_support_subject_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("How can we help you?") },
                minLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_support_description_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (screenshotUriStr != null) "Screenshot attached ✓" else "Attach screenshot (optional)")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onSubmitTicket(
                        "SUPPORT_REQUEST",
                        "Support",
                        subject.ifBlank { "Support Inquiry" },
                        description,
                        screenshotUriStr?.let { Uri.parse(it) }
                    )
                },
                enabled = description.trim().length >= 8,
                colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("contact_support_submit_button")
            ) {
                Text("Send to Support", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun HelpMySupportRequestsSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onUpdateSupportTicket: (ticketId: String, followUpText: String, markResolved: Boolean) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.US) }
    var activeReplyTicketId by rememberSaveable { mutableStateOf<String?>(null) }
    var followUpInput by rememberSaveable { mutableStateOf("") }

    SettingsExplanationCard(
        title = "My support requests & problem reports",
        description = "View the status and reference IDs of all reports and support tickets submitted from your account. You can add follow-up information or mark resolved requests."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        if (uiState.supportTickets.isEmpty()) {
            Box(modifier = Modifier.padding(20.dp)) {
                Text("You haven't submitted any support requests or problem reports yet.", fontSize = 14.sp)
            }
        } else {
            uiState.supportTickets.forEachIndexed { idx, ticket ->
                val isResolved = ticket.status.equals("RESOLVED", ignoreCase = true)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ticket.ticketId,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TelefamRed
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isResolved) Color(0xFFE8EEF5) else Color(0xFFEAF7EE)
                        ) {
                            Text(
                                text = ticket.status,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isResolved) Color(0xFF475569) else Color(0xFF1B7F3B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${ticket.category} — ${ticket.subject}", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(ticket.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (ticket.userFollowUpReply.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Follow-up: ${ticket.userFollowUpReply}",
                            fontSize = 12.6.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (ticket.supportResponseNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ticket.supportResponseNote,
                            fontSize = 12.2.sp,
                            color = Color(0xFF6B7888)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateFormat.format(Date(ticket.createdAtMs)),
                        fontSize = 11.8.sp,
                        color = Color(0xFF6B7888)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                activeReplyTicketId = if (activeReplyTicketId == ticket.ticketId) null else ticket.ticketId
                                followUpInput = ""
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("ticket_followup_toggle_${ticket.ticketId}")
                        ) {
                            Text("Add follow-up", fontSize = 12.5.sp)
                        }

                        if (!isResolved) {
                            OutlinedButton(
                                onClick = {
                                    onUpdateSupportTicket(ticket.ticketId, "", true)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("ticket_status_toggle_${ticket.ticketId}")
                            ) {
                                Text("Mark resolved", fontSize = 12.5.sp)
                            }
                        }
                    }

                    if (activeReplyTicketId == ticket.ticketId) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = followUpInput,
                            onValueChange = { followUpInput = it },
                            label = { Text("Additional details for support") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ticket_followup_input_${ticket.ticketId}")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val note = followUpInput
                                activeReplyTicketId = null
                                followUpInput = ""
                                onUpdateSupportTicket(ticket.ticketId, note, false)
                            },
                            enabled = followUpInput.trim().length >= 3,
                            colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save follow-up note", color = Color.White, fontSize = 12.5.sp)
                        }
                    }
                }
                if (idx < uiState.supportTickets.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}

// --- 12. TERMS AND POLICIES SUB-SCREENS ---

@Composable
private fun TermsAndPoliciesSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onOpenLegalDocument: (LegalPolicyDocument) -> Unit
) {
    val acceptedSet = uiState.accountSettings.acceptedPoliciesCsv
        .split(",")
        .map { it.trim().substringBefore(":") }
        .filter { it.isNotBlank() }
        .toSet()

    SettingsExplanationCard(
        title = "Legal documents & platform policies",
        description = "Review Telefam's governing Terms of Service, Privacy Policy, Community Guidelines, Copyright Policy, and Cookie Policy. Open any document to read full clauses, record your acceptance, or download an official copy."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        TelefamSettingsCatalog.LEGAL_DOCUMENTS.forEachIndexed { idx, doc ->
            val isAccepted = doc.id in acceptedSet
            SettingsNavRow(
                icon = Icons.Outlined.Description,
                title = doc.title,
                subtitle = "${doc.version} • ${doc.subtitle}",
                valueLabel = if (isAccepted) "Accepted ✓" else "Review",
                onClick = { onOpenLegalDocument(doc) },
                showDivider = idx < TelefamSettingsCatalog.LEGAL_DOCUMENTS.lastIndex,
                testTag = "legal_doc_${doc.id}"
            )
        }
    }
}

@Composable
private fun LegalDocumentDetailSubScreen(
    document: LegalPolicyDocument,
    acknowledgedTimestampMs: Long?,
    cardBackground: Color,
    onAcknowledgeLegalDocument: (LegalPolicyDocument) -> Unit,
    onDownloadLegalDocument: (LegalPolicyDocument) -> Unit,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    SettingsExplanationCard(
        title = "${document.title} (${document.version})",
        description = "${document.effectiveDate} • ${document.subtitle}"
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(18.dp)) {
            document.sections.forEachIndexed { idx, (heading, body) ->
                Text(
                    text = heading,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = body,
                    fontSize = 13.6.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (idx < document.sections.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            if (acknowledgedTimestampMs != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEAF7EE),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF1B7F3B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acknowledged on ${dateFormat.format(Date(acknowledgedTimestampMs))} (${document.version})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF135A29)
                        )
                    }
                }
            } else {
                Button(
                    onClick = { onAcknowledgeLegalDocument(document) },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("legal_doc_acknowledge_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acknowledge & Accept Policy", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = { onDownloadLegalDocument(document) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("legal_doc_download_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download official copy", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { onNavigateSubScreen(document.relatedSubScreen) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("legal_doc_related_action_button")
            ) {
                Text(document.relatedActionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// --- 13. ABOUT SUB-SCREENS ---

@Composable
private fun AboutAppSubScreen(
    cardBackground: Color,
    sectionTitleColor: Color,
    onOpenAboutDetail: (String) -> Unit
) {
    SettingsGroupCard(cardBackground = cardBackground) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = TelefamRed,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("T", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Telefam", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Version 1.0.0 (Build 100)", fontSize = 13.5.sp, color = Color(0xFF6B7888))
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Application details", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.Info,
            title = "About the app",
            subtitle = "Platform overview, version information, and security principles",
            onClick = { onOpenAboutDetail("ABOUT_APP") },
            showDivider = true,
            testTag = "about_row_app"
        )
        SettingsNavRow(
            icon = Icons.Outlined.VerifiedUser,
            title = "What's new",
            subtitle = "Release notes for Version 1.0.0 (Build 100)",
            onClick = { onOpenAboutDetail("WHATS_NEW") },
            showDivider = true,
            testTag = "about_row_whats_new"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Lock,
            title = "Copyright & open source",
            subtitle = "Attribution and open-source software notices",
            onClick = { onOpenAboutDetail("COPYRIGHT") },
            showDivider = false,
            testTag = "about_row_copyright"
        )
    }
}

@Composable
private fun AboutDetailSubScreen(
    detailKey: String,
    cardBackground: Color
) {
    val (title, paragraphs) = when (detailKey) {
        "WHATS_NEW" -> "What's new in Telefam 1.0.0 (Build 100)" to listOf(
            "• Comprehensive Settings and privacy center with 13 dedicated management categories.",
            "• Enhanced account security with Passkey sign-in, 2-step verification (Email & Authenticator app), backup recovery codes, and active device session management.",
            "• Instant appearance customization with Light, Dark, and System default theme modes.",
            "• 30-day account deletion recovery grace period and timed temporary account deactivation.",
            "• Profile QR code sharing and granular audience controls for videos, likes, follower lists, comments, mentions, tags, and downloads."
        )
        "COPYRIGHT" -> "Copyright & Open Source Notices" to listOf(
            "© 2026 Telefam Company. All rights reserved.",
            "Telefam for Android is built using modern open-source libraries licensed under the Apache License, Version 2.0, including Android Jetpack Compose, Material Design 3, AndroidX Security, and Kotlin Coroutines."
        )
        else -> "About Telefam" to listOf(
            "Telefam is a modern social and communication platform designed to help people connect, share creativity, and communicate with confidence.",
            "Every account is protected by multi-layered security controls, transparent privacy preferences, and dedicated safety tools.",
            "Version: 1.0.0\nBuild: 100\nPlatform: Android"
        )
    }

    SettingsExplanationCard(
        title = title,
        description = "Telefam Official Release • Version 1.0.0 (Build 100)"
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(18.dp)) {
            paragraphs.forEach { para ->
                Text(
                    text = para,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}
