package com.telefam.company.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OutlinedFlag
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.TelefamRed
import com.example.ui.theme.isTelefamDarkTheme
import com.telefam.company.app.data.AccountSettingsAndSecurityEntity
import com.telefam.company.app.domain.AppPermissionType
import com.telefam.company.app.domain.AppThemeOption
import com.telefam.company.app.domain.CountryDialCode
import com.telefam.company.app.domain.HelpSupportArticle
import com.telefam.company.app.domain.LegalPolicyDocument
import com.telefam.company.app.domain.PrivacyControlField
import com.telefam.company.app.domain.SafetyTopicGuide
import com.telefam.company.app.domain.SettingsSubScreen
import com.telefam.company.app.domain.SocialPrivacyOption
import com.telefam.company.app.ui.TelefamUiState
import com.telefam.company.app.ui.components.ImageCropperDialog

@Composable
fun SettingsAndPrivacyScreen(
    uiState: TelefamUiState,
    onBackClick: () -> Unit,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit,
    onClearBanners: () -> Unit,
    onRetryLastAction: () -> Unit = {},
    onOpenChatPrivacyAndAppearance: () -> Unit,
    // Profile editing callbacks reused in Account
    onFullNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onApplySuggestedUsername: (String) -> Unit,
    onCountrySelected: (CountryDialCode) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onAvatarUriPicked: (Uri) -> Unit,
    onCancelCrop: () -> Unit,
    onConfirmCrop: (scale: Float, offsetXNorm: Float, offsetYNorm: Float, rotationDeg: Float) -> Unit,
    onSaveOwnerProfileEdits: (String) -> Unit,
    // Account & Security callbacks
    onSendVerificationOtp: (String) -> Unit,
    onConfirmChangeEmail: (newEmail: String, otpCode: String) -> Unit,
    onSubmitPasswordChange: (currentPass: String, newPass: String, confirmPass: String) -> Unit,
    onSaveRecoverySettings: (recoveryEmail: String, recoveryPhone: String) -> Unit,
    onConfirmDeactivateAccount: (reason: String, autoReenableDays: Int, otpCode: String) -> Unit,
    onConfirmDeleteAccount30Days: (reason: String, otpCode: String) -> Unit,
    onEnableTwoStep: (method: String, code: String) -> Unit,
    onDisableTwoStep: () -> Unit,
    onRegisterPasskey: (deviceLabel: String) -> Unit,
    onRevokePasskey: () -> Unit,
    onLogoutDevice: (deviceRecordId: String) -> Unit,
    onRevokeConnectedApp: (connectionId: String) -> Unit,
    onOpenPermissionDetail: (AppPermissionType) -> Unit,
    onSystemPermissionUpdated: (AppPermissionType, Boolean) -> Unit,
    // Privacy & Content callbacks
    onTogglePrivateAccount: (Boolean) -> Unit,
    onOpenPrivacyRuleSelector: (PrivacyControlField) -> Unit,
    onSelectPrivacyRule: (PrivacyControlField, SocialPrivacyOption) -> Unit,
    onSetAppThemeMode: (AppThemeOption) -> Unit = {},
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit,
    // Share Profile & QR callbacks
    onDownloadQrCode: () -> Unit,
    onRegenerateQrCode: () -> Unit = {},
    onOpenPublicUserProfile: (String) -> Unit,
    // Blocked Accounts callbacks
    onBlockedSearchQueryChanged: (String) -> Unit,
    onUnblockAccount: (userId: String, username: String) -> Unit,
    // Report a Problem & Support callbacks
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
    onOpenAboutDetail: (String) -> Unit,
    onConfirmLogOut: () -> Unit
) {
    var showLogoutConfirmDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        if (uiState.pendingCropBitmap != null) {
            onCancelCrop()
        } else {
            onBackClick()
        }
    }

    val isDark = isTelefamDarkTheme()
    val pageBackground = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF6F7F9)
    val cardBackground = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val sectionTitleColor = if (isDark) Color(0xFF9BA5B4) else Color(0xFF5B6B7F)

    val topTitle = when (uiState.settingsSubScreen) {
        SettingsSubScreen.ROOT -> stringResource(R.string.settings_and_privacy_title)
        SettingsSubScreen.ACCOUNT -> "Account"
        SettingsSubScreen.ACCOUNT_EDIT_USERNAME -> "Edit username"
        SettingsSubScreen.ACCOUNT_EDIT_NAME -> "Edit name"
        SettingsSubScreen.ACCOUNT_EDIT_EMAIL -> "Email address"
        SettingsSubScreen.ACCOUNT_EDIT_PHONE -> "Phone number"
        SettingsSubScreen.ACCOUNT_CHANGE_PASSWORD -> "Change password"
        SettingsSubScreen.ACCOUNT_RECOVERY -> "Account recovery"
        SettingsSubScreen.ACCOUNT_DEACTIVATE -> "Deactivate account"
        SettingsSubScreen.ACCOUNT_DELETE -> "Delete account"
        SettingsSubScreen.PRIVACY_ACCOUNT -> "Privacy"
        SettingsSubScreen.SECURITY_AND_PERMISSIONS -> "Security & permissions"
        SettingsSubScreen.SECURITY_TWO_STEP -> "2-step verification"
        SettingsSubScreen.SECURITY_PASSKEY -> "Passkey"
        SettingsSubScreen.SECURITY_MANAGE_DEVICES -> "Manage devices"
        SettingsSubScreen.SECURITY_LOGIN_HISTORY -> "Login history"
        SettingsSubScreen.SECURITY_ALERTS -> "Security alerts"
        SettingsSubScreen.SECURITY_PERMISSION_DETAIL -> uiState.activePermissionType.title
        SettingsSubScreen.SECURITY_CONNECTED_APPS -> "Manage connected apps"
        SettingsSubScreen.SHARE_PROFILE -> "Share profile"
        SettingsSubScreen.NOTIFICATIONS -> "Notifications"
        SettingsSubScreen.NOTIFICATIONS_PUSH_DETAIL -> "Push notifications"
        SettingsSubScreen.NOTIFICATIONS_EMAIL_DETAIL -> "Email notifications"
        SettingsSubScreen.CONTENT_PREFERENCES -> "Content preferences"
        SettingsSubScreen.APPEARANCE_THEME -> "Appearance"
        SettingsSubScreen.CONTENT_LANGUAGES -> "Content languages"
        SettingsSubScreen.CONTENT_TOPICS -> "Topics and interests"
        SettingsSubScreen.CONTENT_SUGGESTED -> "Suggested content"
        SettingsSubScreen.CONTENT_VIDEO_QUALITY -> "Video quality"
        SettingsSubScreen.CONTENT_AUTOPLAY -> "Autoplay"
        SettingsSubScreen.CONTENT_FILTERING -> "Content filtering"
        SettingsSubScreen.PRIVACY_CONTROLS -> "Privacy controls"
        SettingsSubScreen.PRIVACY_RULE_SELECTOR -> uiState.activePrivacyControlField.title
        SettingsSubScreen.BLOCKED_ACCOUNTS -> "Blocked accounts"
        SettingsSubScreen.REPORT_PROBLEM -> "Report a problem"
        SettingsSubScreen.SAFETY_CENTER -> "Safety Center"
        SettingsSubScreen.SAFETY_TOPIC_DETAIL -> uiState.activeSafetyGuide.title
        SettingsSubScreen.HELP_AND_SUPPORT -> "Help & support"
        SettingsSubScreen.HELP_ARTICLE_DETAIL -> uiState.activeHelpArticle.categoryTitle
        SettingsSubScreen.HELP_CONTACT_SUPPORT -> "Contact support"
        SettingsSubScreen.HELP_MY_REQUESTS -> "My support requests"
        SettingsSubScreen.TERMS_AND_POLICIES -> "Terms and policies"
        SettingsSubScreen.LEGAL_DOCUMENT_DETAIL -> uiState.activeLegalDocument.title
        SettingsSubScreen.ABOUT -> "About"
        SettingsSubScreen.ABOUT_DETAIL -> when (uiState.activeAboutDetailKey) {
            "WHATS_NEW" -> "What's new"
            "COPYRIGHT" -> "Copyright & open source"
            else -> "About the app"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .testTag("settings_and_privacy_root")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header Bar matching reference image: Left '<' back icon + Centered bold title
            Surface(
                color = pageBackground,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(48.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.back_to_login),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Text(
                        text = topTitle,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 52.dp)
                            .testTag("settings_top_bar_title")
                    )
                }
            }

            // Status / Error Banner with Retry Support
            SettingsFeedbackBanners(
                statusMessage = uiState.settingsStatusBanner ?: uiState.ownerStatusBanner,
                errorMessage = uiState.settingsErrorBanner ?: uiState.errorMessage,
                canRetryError = uiState.canRetrySettingsError,
                onRetry = onRetryLastAction,
                onDismiss = onClearBanners
            )

            // Main Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                when (uiState.settingsSubScreen) {
                    SettingsSubScreen.ROOT -> {
                        SettingsRootContent(
                            uiState = uiState,
                            cardBackground = cardBackground,
                            sectionTitleColor = sectionTitleColor,
                            isDark = isDark,
                            onNavigateSubScreen = onNavigateSubScreen,
                            onLogOutClick = { showLogoutConfirmDialog = true }
                        )
                    }

                    SettingsSubScreen.ACCOUNT,
                    SettingsSubScreen.ACCOUNT_EDIT_USERNAME,
                    SettingsSubScreen.ACCOUNT_EDIT_NAME,
                    SettingsSubScreen.ACCOUNT_EDIT_EMAIL,
                    SettingsSubScreen.ACCOUNT_EDIT_PHONE,
                    SettingsSubScreen.ACCOUNT_CHANGE_PASSWORD,
                    SettingsSubScreen.ACCOUNT_RECOVERY,
                    SettingsSubScreen.ACCOUNT_DEACTIVATE,
                    SettingsSubScreen.ACCOUNT_DELETE,
                    SettingsSubScreen.PRIVACY_ACCOUNT,
                    SettingsSubScreen.SECURITY_AND_PERMISSIONS,
                    SettingsSubScreen.SECURITY_TWO_STEP,
                    SettingsSubScreen.SECURITY_PASSKEY,
                    SettingsSubScreen.SECURITY_MANAGE_DEVICES,
                    SettingsSubScreen.SECURITY_LOGIN_HISTORY,
                    SettingsSubScreen.SECURITY_ALERTS,
                    SettingsSubScreen.SECURITY_PERMISSION_DETAIL,
                    SettingsSubScreen.SECURITY_CONNECTED_APPS,
                    SettingsSubScreen.SHARE_PROFILE -> {
                        SettingsAccountSecurityAndShareRouter(
                            uiState = uiState,
                            cardBackground = cardBackground,
                            sectionTitleColor = sectionTitleColor,
                            onNavigateSubScreen = onNavigateSubScreen,
                            onFullNameChanged = onFullNameChanged,
                            onUsernameChanged = onUsernameChanged,
                            onApplySuggestedUsername = onApplySuggestedUsername,
                            onCountrySelected = onCountrySelected,
                            onPhoneNumberChanged = onPhoneNumberChanged,
                            onAvatarUriPicked = onAvatarUriPicked,
                            onSaveOwnerProfileEdits = onSaveOwnerProfileEdits,
                            onSendVerificationOtp = onSendVerificationOtp,
                            onConfirmChangeEmail = onConfirmChangeEmail,
                            onSubmitPasswordChange = onSubmitPasswordChange,
                            onSaveRecoverySettings = onSaveRecoverySettings,
                            onConfirmDeactivateAccount = onConfirmDeactivateAccount,
                            onConfirmDeleteAccount30Days = onConfirmDeleteAccount30Days,
                            onTogglePrivateAccount = onTogglePrivateAccount,
                            onOpenPrivacyRuleSelector = onOpenPrivacyRuleSelector,
                            onUpdateAccountSettings = onUpdateAccountSettings,
                            onEnableTwoStep = onEnableTwoStep,
                            onDisableTwoStep = onDisableTwoStep,
                            onRegisterPasskey = onRegisterPasskey,
                            onRevokePasskey = onRevokePasskey,
                            onLogoutDevice = onLogoutDevice,
                            onRevokeConnectedApp = onRevokeConnectedApp,
                            onOpenPermissionDetail = onOpenPermissionDetail,
                            onSystemPermissionUpdated = onSystemPermissionUpdated,
                            onDownloadQrCode = onDownloadQrCode,
                            onRegenerateQrCode = onRegenerateQrCode,
                            onOpenPublicUserProfile = onOpenPublicUserProfile
                        )
                    }

                    else -> {
                        SettingsContentSafetyAndSupportRouter(
                            uiState = uiState,
                            cardBackground = cardBackground,
                            sectionTitleColor = sectionTitleColor,
                            onNavigateSubScreen = onNavigateSubScreen,
                            onOpenChatPrivacyAndAppearance = onOpenChatPrivacyAndAppearance,
                            onOpenPrivacyRuleSelector = onOpenPrivacyRuleSelector,
                            onSelectPrivacyRule = onSelectPrivacyRule,
                            onSetAppThemeMode = onSetAppThemeMode,
                            onUpdateAccountSettings = onUpdateAccountSettings,
                            onBlockedSearchQueryChanged = onBlockedSearchQueryChanged,
                            onUnblockAccount = onUnblockAccount,
                            onSubmitTicket = onSubmitTicket,
                            onClearSubmittedTicket = onClearSubmittedTicket,
                            onUpdateSupportTicket = onUpdateSupportTicket,
                            onOpenSafetyGuide = onOpenSafetyGuide,
                            onHelpSearchQueryChanged = onHelpSearchQueryChanged,
                            onOpenHelpArticle = onOpenHelpArticle,
                            onSubmitHelpFeedback = onSubmitHelpFeedback,
                            onOpenLegalDocument = onOpenLegalDocument,
                            onAcknowledgeLegalDocument = onAcknowledgeLegalDocument,
                            onDownloadLegalDocument = onDownloadLegalDocument,
                            onOpenAboutDetail = onOpenAboutDetail
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // Photo Crop Overlay when user changes Profile Photo from Account screen
        if (uiState.pendingCropBitmap != null) {
            ImageCropperDialog(
                bitmap = uiState.pendingCropBitmap,
                onCancel = onCancelCrop,
                onConfirmCrop = { scale, offX, offY, rot ->
                    onConfirmCrop(scale, offX, offY, rot)
                }
            )
        }
    }

    // Log out Confirmation Dialog matching specification:
    // "Log out? Are you sure you want to log out of this account? [Cancel] [Log out]"
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_logout_confirm_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_logout_confirm_message),
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("settings_logout_cancel_button")
                ) {
                    Text(stringResource(R.string.crop_cancel_button))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onConfirmLogOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelefamRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("settings_logout_confirm_button")
                ) {
                    Text(
                        text = stringResource(R.string.settings_logout_button),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}

/**
 * Exact 1:1 Root Layout matching uploaded reference image `file_00000000f4048208a63591681eb6b599.png`:
 * - Account (4 rows)
 * - Content & activity (2 rows)
 * - Privacy and safety (4 rows)
 * - Support & about (3 rows)
 * - Bottom pill [ ->] Log out ]
 */
@Composable
private fun SettingsRootContent(
    uiState: TelefamUiState,
    cardBackground: Color,
    sectionTitleColor: Color,
    isDark: Boolean,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit,
    onLogOutClick: () -> Unit
) {
    val activeThemeLabel = AppThemeOption.fromWire(uiState.accountSettings.appThemeMode).label

    // Section 1: Account
    SettingsSectionHeader(
        title = stringResource(R.string.settings_section_account),
        color = sectionTitleColor
    )
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.PersonOutline,
            title = stringResource(R.string.settings_row_account_title),
            subtitle = stringResource(R.string.settings_row_account_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.ACCOUNT) },
            showDivider = true,
            testTag = "settings_row_account"
        )
        SettingsNavRow(
            icon = Icons.Outlined.VerifiedUser,
            title = stringResource(R.string.settings_row_privacy_account_title),
            subtitle = stringResource(R.string.settings_row_privacy_account_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.PRIVACY_ACCOUNT) },
            showDivider = true,
            testTag = "settings_row_privacy_account"
        )
        SettingsNavRow(
            icon = Icons.Outlined.VerifiedUser,
            title = stringResource(R.string.settings_row_security_title),
            subtitle = stringResource(R.string.settings_row_security_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.SECURITY_AND_PERMISSIONS) },
            showDivider = true,
            testTag = "settings_row_security_permissions"
        )
        SettingsNavRow(
            icon = Icons.Outlined.AccountBox,
            title = stringResource(R.string.settings_row_share_profile_title),
            subtitle = stringResource(R.string.settings_row_share_profile_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.SHARE_PROFILE) },
            showDivider = false,
            testTag = "settings_row_share_profile"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Section 2: Content & activity
    SettingsSectionHeader(
        title = stringResource(R.string.settings_section_content_activity),
        color = sectionTitleColor
    )
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.Notifications,
            title = stringResource(R.string.settings_row_notifications_title),
            subtitle = stringResource(R.string.settings_row_notifications_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.NOTIFICATIONS) },
            showDivider = true,
            testTag = "settings_row_notifications"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Visibility,
            title = stringResource(R.string.settings_row_content_prefs_title),
            subtitle = stringResource(R.string.settings_row_content_prefs_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.CONTENT_PREFERENCES) },
            showDivider = true,
            testTag = "settings_row_content_preferences"
        )
        SettingsNavRow(
            icon = Icons.Outlined.DarkMode,
            title = "Appearance",
            subtitle = "Light, Dark, or System preference",
            valueLabel = activeThemeLabel,
            onClick = { onNavigateSubScreen(SettingsSubScreen.APPEARANCE_THEME) },
            showDivider = false,
            testTag = "settings_row_appearance_theme"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Section 3: Privacy and safety
    SettingsSectionHeader(
        title = stringResource(R.string.settings_section_privacy_safety),
        color = sectionTitleColor
    )
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.settings_row_privacy_controls_title),
            subtitle = stringResource(R.string.settings_row_privacy_controls_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.PRIVACY_CONTROLS) },
            showDivider = true,
            testTag = "settings_row_privacy_controls"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Block,
            title = stringResource(R.string.settings_row_blocked_accounts_title),
            subtitle = stringResource(R.string.settings_row_blocked_accounts_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.BLOCKED_ACCOUNTS) },
            showDivider = true,
            testTag = "settings_row_blocked_accounts"
        )
        SettingsNavRow(
            icon = Icons.Outlined.OutlinedFlag,
            title = stringResource(R.string.settings_row_report_problem_title),
            subtitle = stringResource(R.string.settings_row_report_problem_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.REPORT_PROBLEM) },
            showDivider = true,
            testTag = "settings_row_report_problem"
        )
        SettingsNavRow(
            icon = Icons.Outlined.VerifiedUser,
            title = stringResource(R.string.settings_row_safety_center_title),
            subtitle = stringResource(R.string.settings_row_safety_center_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.SAFETY_CENTER) },
            showDivider = false,
            testTag = "settings_row_safety_center"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Section 4: Support & about
    SettingsSectionHeader(
        title = stringResource(R.string.settings_section_support_about),
        color = sectionTitleColor
    )
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            title = stringResource(R.string.settings_row_help_support_title),
            subtitle = stringResource(R.string.settings_row_help_support_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.HELP_AND_SUPPORT) },
            showDivider = true,
            testTag = "settings_row_help_support"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Description,
            title = stringResource(R.string.settings_row_terms_policies_title),
            subtitle = stringResource(R.string.settings_row_terms_policies_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.TERMS_AND_POLICIES) },
            showDivider = true,
            testTag = "settings_row_terms_policies"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.settings_row_about_title),
            subtitle = stringResource(R.string.settings_row_about_subtitle),
            onClick = { onNavigateSubScreen(SettingsSubScreen.ABOUT) },
            showDivider = false,
            testTag = "settings_row_about"
        )
    }

    Spacer(modifier = Modifier.height(22.dp))

    // Bottom Pill Log out Button matching reference image
    val logoutPillBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEDEFF3)
    Surface(
        onClick = onLogOutClick,
        shape = RoundedCornerShape(26.dp),
        color = logoutPillBg,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("settings_logout_pill_button")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.settings_logout_button),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// --- Shared Reusable Settings Components ---

@Composable
fun SettingsSectionHeader(
    title: String,
    color: Color = Color(0xFF5B6B7F)
) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsExplanationCard(
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = TelefamRed.copy(alpha = 0.06f),
        border = BorderStroke(0.8.dp, TelefamRed.copy(alpha = 0.16f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TelefamRed,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsGroupCard(
    cardBackground: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBackground,
        tonalElevation = 0.5.dp,
        shadowElevation = 0.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun SettingsNavRow(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    valueLabel: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = titleColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        color = Color(0xFF6B7888)
                    )
                }
            }

            if (!valueLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = valueLabel,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7888),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF9AA4B2),
                modifier = Modifier.size(21.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 54.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF6B7888)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TelefamRed
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
fun SettingsRadioRow(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = TelefamRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.8.sp,
                        lineHeight = 17.sp,
                        color = Color(0xFF6B7888)
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun SettingsFeedbackBanners(
    statusMessage: String?,
    errorMessage: String?,
    canRetryError: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .padding(horizontal = 16.dp)
    ) {
        AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
            Surface(
                color = Color(0xFFFDECEA),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .testTag("settings_error_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = TelefamRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage.orEmpty(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8A000A),
                        modifier = Modifier.weight(1f)
                    )
                    if (canRetryError) {
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TelefamRed),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .height(30.dp)
                                .testTag("settings_error_retry_button")
                        ) {
                            Text(
                                text = "Retry",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TelefamRed
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.crop_cancel_button),
                            tint = Color(0xFF8A000A),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = !statusMessage.isNullOrBlank() && errorMessage.isNullOrBlank()) {
            Surface(
                color = Color(0xFFEAF7EE),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .testTag("settings_status_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
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
                        text = statusMessage.orEmpty(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF135A29),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.crop_cancel_button),
                            tint = Color(0xFF135A29),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
