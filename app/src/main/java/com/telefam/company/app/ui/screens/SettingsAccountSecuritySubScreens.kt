package com.telefam.company.app.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.TelefamRed
import com.telefam.company.app.data.AccountSettingsAndSecurityEntity
import com.telefam.company.app.domain.AppPermissionType
import com.telefam.company.app.domain.CountryDialCode
import com.telefam.company.app.domain.DeactivationDurationOption
import com.telefam.company.app.domain.PrivacyControlField
import com.telefam.company.app.domain.SettingsSubScreen
import com.telefam.company.app.domain.SocialPrivacyOption
import com.telefam.company.app.domain.TelefamSettingsCatalog
import com.telefam.company.app.domain.TelefamValidators
import com.telefam.company.app.ui.TelefamUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsAccountSecurityAndShareRouter(
    uiState: TelefamUiState,
    cardBackground: Color,
    sectionTitleColor: Color,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit,
    onFullNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onApplySuggestedUsername: (String) -> Unit,
    onCountrySelected: (CountryDialCode) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onAvatarUriPicked: (Uri) -> Unit,
    onSaveOwnerProfileEdits: (String) -> Unit,
    onSendVerificationOtp: (String) -> Unit,
    onConfirmChangeEmail: (newEmail: String, otpCode: String) -> Unit,
    onSubmitPasswordChange: (currentPass: String, newPass: String, confirmPass: String) -> Unit,
    onSaveRecoverySettings: (recoveryEmail: String, recoveryPhone: String) -> Unit,
    onConfirmDeactivateAccount: (reason: String, autoReenableDays: Int, otpCode: String) -> Unit,
    onConfirmDeleteAccount30Days: (reason: String, otpCode: String) -> Unit,
    onTogglePrivateAccount: (Boolean) -> Unit,
    onOpenPrivacyRuleSelector: (PrivacyControlField) -> Unit,
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit,
    onEnableTwoStep: (method: String, code: String) -> Unit,
    onDisableTwoStep: () -> Unit,
    onRegisterPasskey: (deviceLabel: String) -> Unit,
    onRevokePasskey: () -> Unit,
    onLogoutDevice: (deviceRecordId: String) -> Unit,
    onRevokeConnectedApp: (connectionId: String) -> Unit,
    onOpenPermissionDetail: (AppPermissionType) -> Unit,
    onSystemPermissionUpdated: (AppPermissionType, Boolean) -> Unit,
    onDownloadQrCode: () -> Unit,
    onRegenerateQrCode: () -> Unit = {},
    onOpenPublicUserProfile: (String) -> Unit
) {
    when (uiState.settingsSubScreen) {
        SettingsSubScreen.ACCOUNT -> {
            AccountSettingsMainSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                sectionTitleColor = sectionTitleColor,
                onNavigateSubScreen = onNavigateSubScreen,
                onAvatarUriPicked = onAvatarUriPicked
            )
        }

        SettingsSubScreen.ACCOUNT_EDIT_USERNAME -> {
            AccountEditUsernameSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onUsernameChanged = onUsernameChanged,
                onApplySuggestedUsername = onApplySuggestedUsername,
                onSave = { onSaveOwnerProfileEdits("Username updated.") }
            )
        }

        SettingsSubScreen.ACCOUNT_EDIT_NAME -> {
            AccountEditNameSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onFullNameChanged = onFullNameChanged,
                onSave = { draft ->
                    onFullNameChanged(draft)
                    onSaveOwnerProfileEdits("Name updated.")
                }
            )
        }

        SettingsSubScreen.ACCOUNT_EDIT_EMAIL -> {
            AccountEditEmailSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onSendVerificationOtp = { onSendVerificationOtp("CHANGE_EMAIL") },
                onConfirmChangeEmail = onConfirmChangeEmail
            )
        }

        SettingsSubScreen.ACCOUNT_EDIT_PHONE -> {
            AccountEditPhoneSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onCountrySelected = onCountrySelected,
                onPhoneNumberChanged = onPhoneNumberChanged,
                onSave = { onSaveOwnerProfileEdits("Phone number updated.") }
            )
        }

        SettingsSubScreen.ACCOUNT_CHANGE_PASSWORD -> {
            AccountChangePasswordSubScreen(
                cardBackground = cardBackground,
                onSubmitPasswordChange = onSubmitPasswordChange
            )
        }

        SettingsSubScreen.ACCOUNT_RECOVERY -> {
            AccountRecoverySubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onSaveRecoverySettings = onSaveRecoverySettings
            )
        }

        SettingsSubScreen.ACCOUNT_DEACTIVATE -> {
            AccountDeactivateSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onSendOtp = { onSendVerificationOtp("DEACTIVATE_ACCOUNT") },
                onConfirmDeactivate = onConfirmDeactivateAccount
            )
        }

        SettingsSubScreen.ACCOUNT_DELETE -> {
            AccountDelete30DaysSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onSendOtp = { onSendVerificationOtp("DELETE_ACCOUNT_30D") },
                onConfirmDelete = onConfirmDeleteAccount30Days
            )
        }

        SettingsSubScreen.PRIVACY_ACCOUNT -> {
            PrivacyAccountSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                sectionTitleColor = sectionTitleColor,
                onTogglePrivateAccount = onTogglePrivateAccount,
                onOpenPrivacyRuleSelector = onOpenPrivacyRuleSelector,
                onUpdateAccountSettings = onUpdateAccountSettings
            )
        }

        SettingsSubScreen.SECURITY_AND_PERMISSIONS -> {
            SecurityAndPermissionsSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                sectionTitleColor = sectionTitleColor,
                onNavigateSubScreen = onNavigateSubScreen,
                onOpenPermissionDetail = onOpenPermissionDetail
            )
        }

        SettingsSubScreen.SECURITY_TWO_STEP -> {
            SecurityTwoStepSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onSendEmailOtp = { onSendVerificationOtp("TWO_STEP_SETUP") },
                onEnableTwoStep = onEnableTwoStep,
                onDisableTwoStep = onDisableTwoStep
            )
        }

        SettingsSubScreen.SECURITY_PASSKEY -> {
            SecurityPasskeySubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onRegisterPasskey = onRegisterPasskey,
                onRevokePasskey = onRevokePasskey
            )
        }

        SettingsSubScreen.SECURITY_MANAGE_DEVICES -> {
            SecurityManageDevicesSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onLogoutDevice = onLogoutDevice
            )
        }

        SettingsSubScreen.SECURITY_LOGIN_HISTORY -> {
            SecurityLoginHistorySubScreen(
                uiState = uiState,
                cardBackground = cardBackground
            )
        }

        SettingsSubScreen.SECURITY_ALERTS -> {
            SecurityAlertsSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onUpdateAccountSettings = onUpdateAccountSettings
            )
        }

        SettingsSubScreen.SECURITY_PERMISSION_DETAIL -> {
            SecurityPermissionDetailSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onSystemPermissionUpdated = onSystemPermissionUpdated
            )
        }

        SettingsSubScreen.SECURITY_CONNECTED_APPS -> {
            SecurityConnectedAppsSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onRevokeConnectedApp = onRevokeConnectedApp
            )
        }

        SettingsSubScreen.SHARE_PROFILE -> {
            ShareProfileQrSubScreen(
                uiState = uiState,
                cardBackground = cardBackground,
                onDownloadQrCode = onDownloadQrCode,
                onRegenerateQrCode = onRegenerateQrCode,
                onOpenPublicUserProfile = onOpenPublicUserProfile
            )
        }

        else -> Unit
    }
}

// --- 1. ACCOUNT MAIN SUB-SCREEN ---

@Composable
private fun AccountSettingsMainSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    sectionTitleColor: Color,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit,
    onAvatarUriPicked: (Uri) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAvatarUriPicked(uri)
        }
    }

    val profile = uiState.savedProfile
    val username = profile?.username ?: uiState.username.ifBlank { "telefam_member" }
    val fullName = profile?.fullName ?: uiState.fullName.ifBlank { "Telefam Member" }
    val email = uiState.activeSession?.email ?: uiState.email.ifBlank { "member@telefam.company" }
    val phone = if (profile != null && profile.phoneNumber.isNotBlank()) {
        "${profile.phoneCountryCode} ${profile.phoneNumber}"
    } else if (uiState.phoneNumber.isNotBlank()) {
        "${uiState.selectedCountry.dialCode} ${uiState.phoneNumber}"
    } else {
        "Add phone"
    }

    SettingsExplanationCard(
        title = "Manage your account information",
        description = "Update your profile identity, verified contact details, password, backup recovery options, or account lifecycle settings."
    )

    SettingsSectionHeader(title = "Profile", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        // Profile photo row with live avatar preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("account_row_profile_photo"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Profile photo",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Update your profile photo",
                    fontSize = 12.8.sp,
                    color = Color(0xFF6B7888)
                )
            }
            val avatarPath = uiState.sanitizedAvatar?.secureFilePath ?: profile?.avatarLocalPath
            if (!avatarPath.isNullOrBlank() && File(avatarPath).exists()) {
                AsyncImage(
                    model = File(avatarPath),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = TelefamRed.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = fullName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = TelefamRed
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 52.dp, end = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
        SettingsNavRow(
            icon = Icons.Outlined.PersonOutline,
            title = "Username",
            subtitle = "Unique handle • Changeable every 30 days",
            valueLabel = "@$username",
            onClick = { onNavigateSubScreen(SettingsSubScreen.ACCOUNT_EDIT_USERNAME) },
            showDivider = true,
            testTag = "account_row_username"
        )
        SettingsNavRow(
            icon = Icons.Outlined.PersonOutline,
            title = "Name",
            subtitle = "Display name • Changeable every 7 days",
            valueLabel = fullName,
            onClick = { onNavigateSubScreen(SettingsSubScreen.ACCOUNT_EDIT_NAME) },
            showDivider = false,
            testTag = "account_row_name"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Contact information", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.Email,
            title = "Email",
            subtitle = "Requires verification code sent to original email",
            valueLabel = email,
            onClick = { onNavigateSubScreen(SettingsSubScreen.ACCOUNT_EDIT_EMAIL) },
            showDivider = true,
            testTag = "account_row_email"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Phone,
            title = "Phone number",
            subtitle = "Used for account security and friend discovery",
            valueLabel = phone,
            onClick = { onNavigateSubScreen(SettingsSubScreen.ACCOUNT_EDIT_PHONE) },
            showDivider = false,
            testTag = "account_row_phone"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Security", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.Lock,
            title = "Password",
            subtitle = "Verify current password before setting a new one",
            onClick = { onNavigateSubScreen(SettingsSubScreen.ACCOUNT_CHANGE_PASSWORD) },
            showDivider = true,
            testTag = "account_row_password"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Restore,
            title = "Account recovery",
            subtitle = "Backup email, recovery phone, and 8-digit recovery codes",
            onClick = { onNavigateSubScreen(SettingsSubScreen.ACCOUNT_RECOVERY) },
            showDivider = false,
            testTag = "account_row_recovery"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Account management", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.PauseCircleOutline,
            title = "Deactivate account",
            subtitle = "Temporarily hide your profile with optional auto-reactivation",
            onClick = { onNavigateSubScreen(SettingsSubScreen.ACCOUNT_DEACTIVATE) },
            showDivider = true,
            testTag = "account_row_deactivate"
        )
        SettingsNavRow(
            icon = Icons.Outlined.DeleteForever,
            title = "Delete account",
            subtitle = "Permanent deletion after 30 days • Recoverable by logging in",
            titleColor = TelefamRed,
            onClick = { onNavigateSubScreen(SettingsSubScreen.ACCOUNT_DELETE) },
            showDivider = false,
            testTag = "account_row_delete"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountEditUsernameSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onUsernameChanged: (String) -> Unit,
    onApplySuggestedUsername: (String) -> Unit,
    onSave: () -> Unit
) {
    SettingsExplanationCard(
        title = "Change your unique @username",
        description = "Your username is unique across Telefam. You can change your username once every 30 days."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!uiState.ownerCooldownState.canChangeUsername) {
                Surface(
                    color = Color(0xFFFFF4E5),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.owner_profile_username_cooldown_locked_format,
                            uiState.ownerCooldownState.usernameRemainingDays
                        ),
                        fontSize = 13.sp,
                        color = Color(0xFF7A4500),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = uiState.username,
                onValueChange = onUsernameChanged,
                enabled = uiState.ownerCooldownState.canChangeUsername,
                label = { Text("Username") },
                prefix = { Text("@") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_edit_username_input")
            )

            Spacer(modifier = Modifier.height(8.dp))
            val isAvail = uiState.isUsernameAvailable != false
            val statusColor = if (isAvail) Color(0xFF1B7F3B) else TelefamRed
            Text(
                text = if (isAvail) {
                    "✓ @${uiState.username} is available"
                } else {
                    "This username is unavailable or invalid (3-24 lowercase letters, numbers, or underscores)."
                },
                fontSize = 12.8.sp,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )

            if (uiState.usernameSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Available suggestions:",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.usernameSuggestions.forEach { suggestion ->
                        Surface(
                            onClick = { onApplySuggestedUsername(suggestion) },
                            shape = RoundedCornerShape(16.dp),
                            color = TelefamRed.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "@$suggestion",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TelefamRed,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSave,
                enabled = uiState.ownerCooldownState.canChangeUsername && isAvail,
                colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_save_username_button")
            ) {
                Text("Save username", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun AccountEditNameSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onFullNameChanged: (String) -> Unit,
    onSave: (String) -> Unit
) {
    var draftName by rememberSaveable(uiState.fullName) { mutableStateOf(uiState.fullName) }

    SettingsExplanationCard(
        title = "Update your display name",
        description = "Your display name appears on your profile and in conversations. You can update your name once every 7 days."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!uiState.ownerCooldownState.canChangeFullName) {
                Surface(
                    color = Color(0xFFFFF4E5),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.owner_profile_name_cooldown_locked_format,
                            uiState.ownerCooldownState.fullNameRemainingDays
                        ),
                        fontSize = 13.sp,
                        color = Color(0xFF7A4500),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = draftName,
                onValueChange = {
                    draftName = it
                    onFullNameChanged(it)
                },
                enabled = uiState.ownerCooldownState.canChangeFullName,
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_edit_name_input")
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.owner_profile_name_cooldown_hint),
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSave(draftName) },
                enabled = uiState.ownerCooldownState.canChangeFullName && draftName.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_save_name_button")
            ) {
                Text("Save name", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun AccountEditEmailSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onSendVerificationOtp: () -> Unit,
    onConfirmChangeEmail: (newEmail: String, otpCode: String) -> Unit
) {
    val currentEmail = uiState.activeSession?.email ?: uiState.email.ifBlank { "member@telefam.company" }
    var newEmailInput by rememberSaveable { mutableStateOf("") }
    var otpInput by rememberSaveable { mutableStateOf("") }
    val isRateLimited = uiState.otpCooldownRemainingSec > 0
    val canSendOtp = !isRateLimited && uiState.settingsOtpResendCountdownSec == 0

    SettingsExplanationCard(
        title = "Change your account email address",
        description = "For your security, we will send a 6-digit verification code to your current email address ($currentEmail) to confirm this change."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Current verified email",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currentEmail,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isRateLimited) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFFFF4E5),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Too many verification attempts. Please wait ${formatSettingsCooldownSeconds(uiState.otpCooldownRemainingSec)} before requesting or verifying a code.",
                        fontSize = 12.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7A4500),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onSendVerificationOtp,
                enabled = canSendOtp,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (canSendOtp) TelefamRed else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("settings_email_send_otp_button")
            ) {
                val buttonLabel = when {
                    uiState.settingsOtpResendCountdownSec > 0 ->
                        "Resend code to email in ${uiState.settingsOtpResendCountdownSec}s"
                    !uiState.settingsOtpSentEmail.isNullOrBlank() ->
                        "Resend verification code to $currentEmail"
                    else ->
                        "Send verification code to $currentEmail"
                }
                Text(
                    text = buttonLabel,
                    color = if (canSendOtp) TelefamRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            if (!uiState.settingsOtpSentEmail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFEAF7EE),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "A 6-digit verification code has been sent to ${uiState.settingsOtpSentEmail}. Please check your email inbox and enter the code below to confirm.",
                        fontSize = 12.8.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF135A29),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = otpInput,
                onValueChange = { if (it.length <= 6) otpInput = it.filter(Char::isDigit) },
                enabled = !isRateLimited,
                label = { Text("6-digit verification code from your email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_email_otp_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = newEmailInput,
                onValueChange = { newEmailInput = it },
                enabled = !isRateLimited,
                label = { Text("New email address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_new_email_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onConfirmChangeEmail(newEmailInput, otpInput) },
                enabled = !isRateLimited && otpInput.length == 6 && newEmailInput.contains("@"),
                colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_confirm_email_change_button")
            ) {
                Text("Confirm & Update Email", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun AccountEditPhoneSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onCountrySelected: (CountryDialCode) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    var showCountryMenu by remember { mutableStateOf(false) }

    SettingsExplanationCard(
        title = "Update your phone number",
        description = "Your phone number helps secure your account and allows people who already have your number to connect with you."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box {
                OutlinedButton(
                    onClick = { showCountryMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${uiState.selectedCountry.flagEmoji} ${uiState.selectedCountry.name} (${uiState.selectedCountry.dialCode})",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                DropdownMenu(
                    expanded = showCountryMenu,
                    onDismissRequest = { showCountryMenu = false }
                ) {
                    com.telefam.company.app.domain.TelefamCountries.ALL.take(45).forEach { country ->
                        DropdownMenuItem(
                            text = { Text("${country.flagEmoji} ${country.name} (${country.dialCode})") },
                            onClick = {
                                onCountrySelected(country)
                                showCountryMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = onPhoneNumberChanged,
                label = { Text("Phone number") },
                prefix = { Text("${uiState.selectedCountry.dialCode} ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_edit_phone_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_save_phone_button")
            ) {
                Text("Save phone number", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun AccountChangePasswordSubScreen(
    cardBackground: Color,
    onSubmitPasswordChange: (currentPass: String, newPass: String, confirmPass: String) -> Unit
) {
    var currentPass by rememberSaveable { mutableStateOf("") }
    var newPass by rememberSaveable { mutableStateOf("") }
    var confirmPass by rememberSaveable { mutableStateOf("") }

    SettingsExplanationCard(
        title = "Change your account password",
        description = "Enter your current password first before setting a new password. Strong passwords require at least 8 characters with uppercase, lowercase, numbers, and symbols."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = currentPass,
                onValueChange = { currentPass = it },
                label = { Text("Current password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_current_password_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = newPass,
                onValueChange = { newPass = it },
                label = { Text("New password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_new_password_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPass,
                onValueChange = { confirmPass = it },
                label = { Text("Confirm new password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_confirm_password_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSubmitPasswordChange(currentPass, newPass, confirmPass) },
                enabled = currentPass.isNotBlank() && newPass.length >= 8 && confirmPass.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_update_password_button")
            ) {
                Text("Update password", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun AccountRecoverySubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onSaveRecoverySettings: (recoveryEmail: String, recoveryPhone: String) -> Unit
) {
    val context = LocalContext.current
    val acc = uiState.accountSettings
    var recoveryEmail by rememberSaveable(acc.recoveryEmail) { mutableStateOf(acc.recoveryEmail) }
    var recoveryPhone by rememberSaveable(acc.recoveryPhone) { mutableStateOf(acc.recoveryPhone) }
    val codes = acc.backupRecoveryCodesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    SettingsExplanationCard(
        title = "Account recovery & backup codes",
        description = "Configure a backup email address, recovery phone number, and offline 8-character recovery codes so you can always regain access to your account."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = recoveryEmail,
                onValueChange = { recoveryEmail = it },
                label = { Text("Recovery email address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_recovery_email_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = recoveryPhone,
                onValueChange = { recoveryPhone = it },
                label = { Text("Recovery phone number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_recovery_phone_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Backup Recovery Codes",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Store these single-use backup codes in a safe place. Each code can be used if you lose access to your phone or email.",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    codes.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            pair.forEach { code ->
                                Text(
                                    text = code,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("Telefam Recovery Codes", codes.joinToString("\n"))
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy codes")
                }

                Button(
                    onClick = { onSaveRecoverySettings(recoveryEmail, recoveryPhone) },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_save_recovery_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & New Codes", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AccountDeactivateSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onSendOtp: () -> Unit,
    onConfirmDeactivate: (reason: String, autoReenableDays: Int, otpCode: String) -> Unit
) {
    var selectedDuration by rememberSaveable { mutableStateOf(DeactivationDurationOption.SEVEN_DAYS) }
    var selectedReason by rememberSaveable { mutableStateOf(TelefamSettingsCatalog.DEACTIVATION_REASONS.first()) }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    SettingsExplanationCard(
        title = "Deactivate your account temporarily",
        description = "Deactivating hides your profile, posts, and activity from other people. You can schedule when your account automatically re-enables, or reactivate anytime simply by logging in."
    )

    SettingsSectionHeader(title = "When should we automatically re-enable your account?")
    SettingsGroupCard(cardBackground = cardBackground) {
        DeactivationDurationOption.entries.forEachIndexed { idx, option ->
            SettingsRadioRow(
                title = option.label,
                subtitle = option.description,
                selected = selectedDuration == option,
                onClick = { selectedDuration = option },
                showDivider = idx < DeactivationDurationOption.entries.lastIndex,
                testTag = "deactivate_duration_${option.days}"
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    SettingsSectionHeader(title = "Reason for deactivating")
    SettingsGroupCard(cardBackground = cardBackground) {
        TelefamSettingsCatalog.DEACTIVATION_REASONS.forEachIndexed { idx, reason ->
            SettingsRadioRow(
                title = reason,
                selected = selectedReason == reason,
                onClick = { selectedReason = reason },
                showDivider = idx < TelefamSettingsCatalog.DEACTIVATION_REASONS.lastIndex,
                testTag = "deactivate_reason_$idx"
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    SettingsGroupCard(cardBackground = cardBackground) {
        val currentEmail = uiState.activeSession?.email ?: uiState.email.ifBlank { "member@telefam.company" }
        val isRateLimited = uiState.otpCooldownRemainingSec > 0
        val canSendOtp = !isRateLimited && uiState.settingsOtpResendCountdownSec == 0
        Column(modifier = Modifier.padding(16.dp)) {
            if (isRateLimited) {
                Surface(
                    color = Color(0xFFFFF4E5),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Too many verification attempts. Please wait ${formatSettingsCooldownSeconds(uiState.otpCooldownRemainingSec)} before trying again.",
                        fontSize = 12.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7A4500),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = onSendOtp,
                enabled = canSendOtp,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (canSendOtp) TelefamRed else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deactivate_send_otp_button")
            ) {
                val btnText = when {
                    uiState.settingsOtpResendCountdownSec > 0 ->
                        "Resend code to email in ${uiState.settingsOtpResendCountdownSec}s"
                    !uiState.settingsOtpSentEmail.isNullOrBlank() ->
                        "Resend verification code to $currentEmail"
                    else ->
                        "Send verification code to $currentEmail"
                }
                Text(
                    text = btnText,
                    color = if (canSendOtp) TelefamRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!uiState.settingsOtpSentEmail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We sent a 6-digit verification code to ${uiState.settingsOtpSentEmail}. Enter it below to confirm.",
                    fontSize = 12.8.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1B7F3B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it.filter(Char::isDigit) },
                enabled = !isRateLimited,
                label = { Text("Enter 6-digit verification code from email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deactivate_otp_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { showConfirmDialog = true },
                enabled = !isRateLimited && otpCode.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("deactivate_submit_button")
            ) {
                Text("Deactivate account", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Deactivate your account?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your profile and posts will be hidden immediately. ${selectedDuration.description} You can also reactivate anytime by signing back in."
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onConfirmDeactivate(selectedReason, selectedDuration.days, otpCode)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed)
                ) {
                    Text("Confirm Deactivation", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun AccountDelete30DaysSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onSendOtp: () -> Unit,
    onConfirmDelete: (reason: String, otpCode: String) -> Unit
) {
    var selectedReason by rememberSaveable { mutableStateOf(TelefamSettingsCatalog.DELETION_REASONS.first()) }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFDECEA),
        border = BorderStroke(1.dp, TelefamRed.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = TelefamRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "30-Day Recovery Window Before Permanent Deletion",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF8A000A)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Your account will be deactivated immediately and scheduled for permanent deletion in 30 days.\n" +
                    "• During the next 30 days, you can recover your account at any time simply by logging back in.\n" +
                    "• After 30 days, your profile, posts, followers, messages, and settings are permanently deleted forever and cannot be recovered.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFF6E0008)
            )
        }
    }

    SettingsSectionHeader(title = "Why are you deleting your account?")
    SettingsGroupCard(cardBackground = cardBackground) {
        TelefamSettingsCatalog.DELETION_REASONS.forEachIndexed { idx, reason ->
            SettingsRadioRow(
                title = reason,
                selected = selectedReason == reason,
                onClick = { selectedReason = reason },
                showDivider = idx < TelefamSettingsCatalog.DELETION_REASONS.lastIndex,
                testTag = "delete_reason_$idx"
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    SettingsGroupCard(cardBackground = cardBackground) {
        val currentEmail = uiState.activeSession?.email ?: uiState.email.ifBlank { "member@telefam.company" }
        val isRateLimited = uiState.otpCooldownRemainingSec > 0
        val canSendOtp = !isRateLimited && uiState.settingsOtpResendCountdownSec == 0
        Column(modifier = Modifier.padding(16.dp)) {
            if (isRateLimited) {
                Surface(
                    color = Color(0xFFFFF4E5),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Too many verification attempts. Please wait ${formatSettingsCooldownSeconds(uiState.otpCooldownRemainingSec)} before trying again.",
                        fontSize = 12.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7A4500),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = onSendOtp,
                enabled = canSendOtp,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (canSendOtp) TelefamRed else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delete_send_otp_button")
            ) {
                val btnText = when {
                    uiState.settingsOtpResendCountdownSec > 0 ->
                        "Resend code to email in ${uiState.settingsOtpResendCountdownSec}s"
                    !uiState.settingsOtpSentEmail.isNullOrBlank() ->
                        "Resend verification code to $currentEmail"
                    else ->
                        "Send verification code to $currentEmail"
                }
                Text(
                    text = btnText,
                    color = if (canSendOtp) TelefamRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!uiState.settingsOtpSentEmail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We sent a 6-digit verification code to ${uiState.settingsOtpSentEmail}. Enter it below to confirm.",
                    fontSize = 12.8.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1B7F3B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it.filter(Char::isDigit) },
                enabled = !isRateLimited,
                label = { Text("Enter 6-digit verification code from email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delete_otp_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { showConfirmDialog = true },
                enabled = !isRateLimited && otpCode.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("delete_account_submit_button")
            ) {
                Text("Continue to delete account", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Schedule permanent account deletion?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your account will be scheduled for permanent deletion in 30 days. If you change your mind, simply sign back into your account within 30 days to cancel deletion and restore everything. After 30 days, your account is deleted forever."
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onConfirmDelete(selectedReason, otpCode)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                    modifier = Modifier.testTag("delete_account_final_confirm_button")
                ) {
                    Text("Delete after 30 days", color = Color.White)
                }
            }
        )
    }
}

// --- 2. PRIVACY (ACCOUNT SECTION) SUB-SCREEN ---

@Composable
private fun PrivacyAccountSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    sectionTitleColor: Color,
    onTogglePrivateAccount: (Boolean) -> Unit,
    onOpenPrivacyRuleSelector: (PrivacyControlField) -> Unit,
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit
) {
    val acc = uiState.accountSettings

    SettingsExplanationCard(
        title = "Control who can view and interact with your account",
        description = "Manage your account visibility and choose who can view your content or interact with your profile."
    )

    SettingsSectionHeader(title = "Account", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsSwitchRow(
            title = "Private account",
            subtitle = "Only approved followers can see your videos, likes, and restricted content",
            checked = acc.isPrivateAccount,
            onCheckedChange = onTogglePrivateAccount,
            showDivider = true,
            testTag = "privacy_switch_private_account"
        )
        SettingsSwitchRow(
            title = "Activity status",
            subtitle = "Allow followers you follow back to see when you were last active",
            checked = acc.activityStatusEnabled,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Activity status ${if (enabled) "turned on" else "turned off"}.",
                    "ActivityStatus -> $enabled"
                ) { it.copy(activityStatusEnabled = enabled) }
            },
            showDivider = false,
            testTag = "privacy_switch_activity_status"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Interactions", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            title = "Comments",
            subtitle = "Control who can comment on your posts",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanComment, false).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.COMMENTS) },
            showDivider = true,
            testTag = "privacy_row_comments"
        )
        SettingsNavRow(
            title = "Mentions",
            subtitle = "Choose who can @mention your account",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanMention, false).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.MENTIONS) },
            showDivider = true,
            testTag = "privacy_row_mentions"
        )
        SettingsNavRow(
            title = "Tags",
            subtitle = "Choose who can tag you in posts and photos",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanTag, false).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.TAGS) },
            showDivider = true,
            testTag = "privacy_row_tags"
        )
        SettingsNavRow(
            title = "Messages",
            subtitle = "Choose who can send you direct messages",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanMessage, false).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.MESSAGES) },
            showDivider = false,
            testTag = "privacy_row_messages"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Content", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            title = "Downloads",
            subtitle = "Allow others to download your shared videos and photos",
            valueLabel = SocialPrivacyOption.fromWire(acc.whoCanDownload, false).label,
            onClick = { onOpenPrivacyRuleSelector(PrivacyControlField.DOWNLOADS) },
            showDivider = false,
            testTag = "privacy_row_downloads"
        )
    }
}

// --- 3. SECURITY & PERMISSIONS SUB-SCREENS ---

@Composable
private fun SecurityAndPermissionsSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    sectionTitleColor: Color,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit,
    onOpenPermissionDetail: (AppPermissionType) -> Unit
) {
    val acc = uiState.accountSettings

    SettingsExplanationCard(
        title = "Protect your account and device permissions",
        description = "Configure 2-step verification, passkeys, active device sessions, security alerts, and system permissions."
    )

    SettingsSectionHeader(title = "Password", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.Lock,
            title = "Change password",
            subtitle = "Update your account password",
            onClick = { onNavigateSubScreen(SettingsSubScreen.ACCOUNT_CHANGE_PASSWORD) },
            showDivider = false,
            testTag = "security_row_change_password"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Two-step verification & Passkey", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.Shield,
            title = "2-step verification",
            subtitle = "Require a verification code when signing in on new devices",
            valueLabel = if (acc.twoStepEnabled) "On (${acc.twoStepMethod})" else "Off",
            onClick = { onNavigateSubScreen(SettingsSubScreen.SECURITY_TWO_STEP) },
            showDivider = true,
            testTag = "security_row_two_step"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Key,
            title = "Passkey",
            subtitle = "Sign in faster and more securely using your device screen lock or biometrics",
            valueLabel = if (acc.passkeyEnabled) "Enabled" else "Set up",
            onClick = { onNavigateSubScreen(SettingsSubScreen.SECURITY_PASSKEY) },
            showDivider = false,
            testTag = "security_row_passkey"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Your devices", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.Devices,
            title = "Manage devices",
            subtitle = "Review logged-in devices and log out unrecognized sessions",
            valueLabel = "${uiState.trustedDevices.size} active",
            onClick = { onNavigateSubScreen(SettingsSubScreen.SECURITY_MANAGE_DEVICES) },
            showDivider = false,
            testTag = "security_row_manage_devices"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Security activity", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.History,
            title = "Login history",
            subtitle = "Recent sign-ins, locations, and session activity",
            onClick = { onNavigateSubScreen(SettingsSubScreen.SECURITY_LOGIN_HISTORY) },
            showDivider = true,
            testTag = "security_row_login_history"
        )
        SettingsNavRow(
            icon = Icons.Outlined.NotificationsActive,
            title = "Security alerts",
            subtitle = "Alerts for unrecognized sign-in attempts and credential changes",
            valueLabel = if (acc.emailSecurityAlerts) "On" else "Off",
            onClick = { onNavigateSubScreen(SettingsSubScreen.SECURITY_ALERTS) },
            showDivider = false,
            testTag = "security_row_security_alerts"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Permissions", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.Contacts,
            title = "Contacts",
            subtitle = AppPermissionType.CONTACTS.summary,
            valueLabel = if (acc.permissionContactsAllowed) "Allowed" else "Ask",
            onClick = { onOpenPermissionDetail(AppPermissionType.CONTACTS) },
            showDivider = true,
            testTag = "security_row_perm_contacts"
        )
        SettingsNavRow(
            icon = Icons.Outlined.CameraAlt,
            title = "Camera",
            subtitle = AppPermissionType.CAMERA.summary,
            valueLabel = if (acc.permissionCameraAllowed) "Allowed" else "Ask",
            onClick = { onOpenPermissionDetail(AppPermissionType.CAMERA) },
            showDivider = true,
            testTag = "security_row_perm_camera"
        )
        SettingsNavRow(
            icon = Icons.Outlined.Mic,
            title = "Microphone",
            subtitle = AppPermissionType.MICROPHONE.summary,
            valueLabel = if (acc.permissionMicrophoneAllowed) "Allowed" else "Ask",
            onClick = { onOpenPermissionDetail(AppPermissionType.MICROPHONE) },
            showDivider = true,
            testTag = "security_row_perm_microphone"
        )
        SettingsNavRow(
            icon = Icons.Outlined.PhotoLibrary,
            title = "Photos",
            subtitle = AppPermissionType.PHOTOS.summary,
            valueLabel = if (acc.permissionPhotosAllowed) "Allowed" else "Selected",
            onClick = { onOpenPermissionDetail(AppPermissionType.PHOTOS) },
            showDivider = false,
            testTag = "security_row_perm_photos"
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    SettingsSectionHeader(title = "Connected apps", color = sectionTitleColor)
    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsNavRow(
            icon = Icons.Outlined.Link,
            title = "Manage connected apps",
            subtitle = "Third-party services and authorized sessions",
            valueLabel = "${uiState.connectedApps.size} connected",
            onClick = { onNavigateSubScreen(SettingsSubScreen.SECURITY_CONNECTED_APPS) },
            showDivider = false,
            testTag = "security_row_connected_apps"
        )
    }
}

@Composable
private fun SecurityTwoStepSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onSendEmailOtp: () -> Unit,
    onEnableTwoStep: (method: String, code: String) -> Unit,
    onDisableTwoStep: () -> Unit
) {
    val acc = uiState.accountSettings
    val currentEmail = uiState.activeSession?.email ?: uiState.email.ifBlank { "member@telefam.company" }
    var selectedMethod by rememberSaveable { mutableStateOf(acc.twoStepMethod.ifBlank { "EMAIL" }) }
    var codeInput by rememberSaveable { mutableStateOf("") }
    var showConfirmEnableDialog by rememberSaveable { mutableStateOf(false) }
    val recoveryCodes = acc.backupRecoveryCodesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val isRateLimited = uiState.otpCooldownRemainingSec > 0
    val canSendOtp = !isRateLimited && uiState.settingsOtpResendCountdownSec == 0

    SettingsExplanationCard(
        title = "Two-step verification",
        description = "Protect your account by requiring a 6-digit verification code whenever you sign in from a new device."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = if (acc.twoStepEnabled) Color(0xFF1B7F3B) else Color(0xFF6B7888)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (acc.twoStepEnabled) {
                        "Status: Active (${acc.twoStepMethod})"
                    } else {
                        "Status: Off"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
            }

            if (isRateLimited) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFFFF4E5),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Too many verification attempts. Please wait ${formatSettingsCooldownSeconds(uiState.otpCooldownRemainingSec)} before requesting or verifying a code.",
                        fontSize = 12.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7A4500),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            SettingsRadioRow(
                title = "Email verification",
                subtitle = "Receive a 6-digit verification code at $currentEmail",
                selected = selectedMethod == "EMAIL",
                onClick = { selectedMethod = "EMAIL" },
                showDivider = true,
                testTag = "two_step_method_email"
            )
            SettingsRadioRow(
                title = "Authenticator App",
                subtitle = "Use an authenticator app to generate 6-digit verification codes",
                selected = selectedMethod == "AUTHENTICATOR",
                onClick = { selectedMethod = "AUTHENTICATOR" },
                showDivider = false,
                testTag = "two_step_method_authenticator"
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedMethod == "EMAIL") {
                OutlinedButton(
                    onClick = onSendEmailOtp,
                    enabled = canSendOtp,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (canSendOtp) TelefamRed else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("two_step_send_email_otp_button")
                ) {
                    val btnLabel = when {
                        uiState.settingsOtpResendCountdownSec > 0 ->
                            "Resend code to email in ${uiState.settingsOtpResendCountdownSec}s"
                        !uiState.settingsOtpSentEmail.isNullOrBlank() ->
                            "Resend verification code to $currentEmail"
                        else ->
                            "Send verification code to $currentEmail"
                    }
                    Text(
                        text = btnLabel,
                        color = if (canSendOtp) TelefamRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (!uiState.settingsOtpSentEmail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFEAF7EE),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "We sent a 6-digit verification code to ${uiState.settingsOtpSentEmail}. Please check your email and enter it below to confirm.",
                            fontSize = 12.8.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF135A29),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Authenticator setup key:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = acc.authenticatorSecretBase32.ifBlank { "JBSWY3DPEHPK3PXP" },
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Enter the 6-digit code from your authenticator app to confirm setup.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = codeInput,
                onValueChange = { if (it.length <= 6) codeInput = it.filter(Char::isDigit) },
                enabled = !isRateLimited,
                label = { Text("Enter 6-digit verification code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("two_step_code_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (acc.twoStepEnabled) {
                    OutlinedButton(
                        onClick = onDisableTwoStep,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Turn off 2FA")
                    }
                }
                Button(
                    onClick = { showConfirmEnableDialog = true },
                    enabled = !isRateLimited && codeInput.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("two_step_activate_button")
                ) {
                    Text(
                        text = if (acc.twoStepEnabled) "Confirm & Update 2FA" else "Confirm & Activate 2FA",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (acc.twoStepEnabled && recoveryCodes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Backup Recovery Codes:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = recoveryCodes.joinToString("   •   "),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    if (showConfirmEnableDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmEnableDialog = false },
            title = { Text("Confirm 2-step verification", fontWeight = FontWeight.Bold) },
            text = {
                Text("Confirm enabling 2-step verification with your 6-digit verification code?")
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmEnableDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmEnableDialog = false
                        onEnableTwoStep(selectedMethod, codeInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed)
                ) {
                    Text("Confirm", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun SecurityPasskeySubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onRegisterPasskey: (deviceLabel: String) -> Unit,
    onRevokePasskey: () -> Unit
) {
    val acc = uiState.accountSettings
    var showConfirmSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showConfirmRemoveDialog by rememberSaveable { mutableStateOf(false) }

    SettingsExplanationCard(
        title = "Passkey sign-in",
        description = "Sign in quickly and securely without entering your password."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (acc.passkeyEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF1B7F3B),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Passkey saved",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your passkey is saved. You can now log in next time with your passkey.",
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showConfirmRemoveDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TelefamRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("passkey_revoke_button")
                ) {
                    Text("Remove Passkey", color = TelefamRed, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(
                    text = "Set up a passkey for your account so you can log in next time with your passkey.",
                    fontSize = 13.8.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showConfirmSaveDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("passkey_enable_button")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Passkey", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }

    if (showConfirmSaveDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmSaveDialog = false },
            title = { Text("Confirm passkey", fontWeight = FontWeight.Bold) },
            text = {
                Text("Confirm saving a passkey for your account? Once saved, you can log in next time with your passkey.")
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmSaveDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSaveDialog = false
                        onRegisterPasskey("")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                    modifier = Modifier.testTag("passkey_confirm_save_button")
                ) {
                    Text("Confirm", color = Color.White)
                }
            }
        )
    }

    if (showConfirmRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmRemoveDialog = false },
            title = { Text("Remove passkey?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to remove your saved passkey from this account?")
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmRemoveDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmRemoveDialog = false
                        onRevokePasskey()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                    modifier = Modifier.testTag("passkey_confirm_remove_button")
                ) {
                    Text("Remove", color = Color.White)
                }
            }
        )
    }
}

private fun formatSettingsCooldownSeconds(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}

@Composable
private fun SecurityManageDevicesSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onLogoutDevice: (deviceRecordId: String) -> Unit
) {
    val currentFp = uiState.telemetry?.serverVerifiedDeviceHash

    SettingsExplanationCard(
        title = "Your logged-in devices",
        description = "Review all phones and computers currently signed into your Telefam account. If you don't recognize a device, log it out immediately."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        if (uiState.trustedDevices.isEmpty()) {
            Box(modifier = Modifier.padding(20.dp)) {
                Text("Current Android device • Active now", fontWeight = FontWeight.SemiBold)
            }
        } else {
            uiState.trustedDevices.forEachIndexed { idx, device ->
                val isCurrent = device.serverDeviceHash == currentFp || idx == 0
                val relativeTime = if (isCurrent) {
                    "Current device\nLast active: Now"
                } else {
                    val daysAgo = ((System.currentTimeMillis() - device.lastSeenMs) / (24L * 3600_000L)).coerceAtLeast(1)
                    "Last active: $daysAgo day(s) ago • ${device.locationLabel}"
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (device.osDescription.contains("iPhone", ignoreCase = true)) {
                                Icons.Default.PhoneAndroid
                            } else {
                                Icons.Default.Smartphone
                            },
                            contentDescription = null,
                            tint = if (isCurrent) Color(0xFF1B7F3B) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.osDescription,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = relativeTime,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = if (isCurrent) Color(0xFF1B7F3B) else Color(0xFF6B7888)
                            )
                        }
                        if (!isCurrent) {
                            OutlinedButton(
                                onClick = { onLogoutDevice(device.deviceRecordId) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, TelefamRed),
                                modifier = Modifier.testTag("logout_device_${device.deviceRecordId}")
                            ) {
                                Text("Log out", color = TelefamRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                    if (idx < uiState.trustedDevices.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityLoginHistorySubScreen(
    uiState: TelefamUiState,
    cardBackground: Color
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.US) }

    SettingsExplanationCard(
        title = "Login & authentication history",
        description = "Audit log of sign-in events, OTP verifications, and session activity recorded for your account."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        if (uiState.recentSecurityLogs.isEmpty()) {
            Box(modifier = Modifier.padding(20.dp)) {
                Text("Signed in on this device • Verified session active", fontSize = 14.sp)
            }
        } else {
            uiState.recentSecurityLogs.take(20).forEachIndexed { idx, log ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "${log.eventCategory} • ${log.eventOutcome}".replace("_", " "),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = log.forensicDetail,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${dateFormat.format(Date(log.timestampMs))} • ${log.locationLabel}",
                            fontSize = 11.8.sp,
                            color = Color(0xFF6B7888)
                        )
                    }
                    if (idx < uiState.recentSecurityLogs.take(20).lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityAlertsSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onUpdateAccountSettings: (
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) -> Unit
) {
    val acc = uiState.accountSettings
    val alertLogs = uiState.recentSecurityLogs.filter {
        it.eventCategory.contains("LOCK", ignoreCase = true) ||
            it.eventOutcome.contains("FAIL", ignoreCase = true) ||
            it.eventCategory.contains("SECURITY", ignoreCase = true) ||
            it.eventCategory.contains("SETTINGS", ignoreCase = true)
    }

    SettingsExplanationCard(
        title = "Real-time security alerts",
        description = "Receive immediate push and email notifications whenever a new device signs in, a password or email change is requested, or failed login attempts trigger a cooldown."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        SettingsSwitchRow(
            title = "Unrecognized login & credential alerts",
            subtitle = "Notify me immediately on new device sign-ins and security changes",
            checked = acc.emailSecurityAlerts,
            onCheckedChange = { enabled ->
                onUpdateAccountSettings(
                    "Security alerts ${if (enabled) "enabled" else "disabled"}.",
                    "SecurityAlerts -> $enabled"
                ) { it.copy(emailSecurityAlerts = enabled, smsSecurityAlerts = enabled) }
            },
            showDivider = false,
            testTag = "security_alerts_switch"
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    SettingsSectionHeader(title = "Recent security events")
    SettingsGroupCard(cardBackground = cardBackground) {
        if (alertLogs.isEmpty()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1B7F3B))
                Spacer(modifier = Modifier.width(10.dp))
                Text("No suspicious security alerts detected on your account.", fontSize = 13.5.sp)
            }
        } else {
            alertLogs.take(10).forEachIndexed { idx, log ->
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("${log.eventCategory} (${log.eventOutcome})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(log.forensicDetail, fontSize = 12.8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (idx < alertLogs.take(10).lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun SecurityPermissionDetailSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onSystemPermissionUpdated: (AppPermissionType, Boolean) -> Unit
) {
    val context = LocalContext.current
    val permType = uiState.activePermissionType
    val acc = uiState.accountSettings

    val isCurrentlyAllowed = when (permType) {
        AppPermissionType.CONTACTS -> acc.permissionContactsAllowed
        AppPermissionType.CAMERA -> acc.permissionCameraAllowed
        AppPermissionType.MICROPHONE -> acc.permissionMicrophoneAllowed
        AppPermissionType.PHOTOS -> acc.permissionPhotosAllowed
    }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onSystemPermissionUpdated(permType, granted)
    }

    SettingsExplanationCard(
        title = "${permType.title} permission",
        description = permType.detailedExplanation
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsSwitchRow(
                title = "Allow ${permType.title} access",
                subtitle = permType.summary,
                checked = isCurrentlyAllowed,
                onCheckedChange = { wantGrant ->
                    if (wantGrant) {
                        when (permType) {
                            AppPermissionType.CONTACTS -> runtimePermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            AppPermissionType.CAMERA -> runtimePermissionLauncher.launch(Manifest.permission.CAMERA)
                            AppPermissionType.MICROPHONE -> runtimePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            AppPermissionType.PHOTOS -> onSystemPermissionUpdated(AppPermissionType.PHOTOS, true)
                        }
                    } else {
                        onSystemPermissionUpdated(permType, false)
                    }
                },
                showDivider = false,
                testTag = "permission_toggle_${permType.name}"
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Android System App Settings")
            }
        }
    }
}

@Composable
private fun SecurityConnectedAppsSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onRevokeConnectedApp: (connectionId: String) -> Unit
) {
    SettingsExplanationCard(
        title = "Manage connected apps & services",
        description = "Review third-party integrations and companion clients that have authorized access to your Telefam account. Revoke access at any time."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        if (uiState.connectedApps.isEmpty()) {
            Box(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "No third-party connected apps have active access to your account.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            uiState.connectedApps.forEachIndexed { idx, app ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            Text(
                                text = "${app.developerDomain} • Scopes: ${app.scopesGrantedCsv}",
                                fontSize = 12.8.sp,
                                color = Color(0xFF6B7888)
                            )
                        }
                        OutlinedButton(
                            onClick = { onRevokeConnectedApp(app.connectionId) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TelefamRed),
                            modifier = Modifier.testTag("revoke_app_${app.connectionId}")
                        ) {
                            Text("Revoke", color = TelefamRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    if (idx < uiState.connectedApps.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// --- 4. SHARE PROFILE (QR CODE) SUB-SCREEN ---

@Composable
private fun ShareProfileQrSubScreen(
    uiState: TelefamUiState,
    cardBackground: Color,
    onDownloadQrCode: () -> Unit,
    onRegenerateQrCode: () -> Unit,
    onOpenPublicUserProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val profile = uiState.savedProfile
    val username = profile?.username ?: uiState.username.ifBlank { "telefam_member" }
    val displayName = profile?.fullName ?: uiState.fullName.ifBlank { "Telefam Member" }
    val shareHandle = "@$username"
    var showScanLookupDialog by rememberSaveable { mutableStateOf(false) }
    var scannedHandleInput by rememberSaveable { mutableStateOf("") }
    var localCopiedBanner by rememberSaveable { mutableStateOf<String?>(null) }

    SettingsExplanationCard(
        title = "Share your profile with QR code",
        description = "Anyone on Telefam can scan your profile QR code or search your @$username handle to connect with you immediately."
    )

    SettingsGroupCard(cardBackground = cardBackground) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.profileQrBitmap != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    color = Color.White,
                    modifier = Modifier.size(230.dp)
                ) {
                    Image(
                        bitmap = uiState.profileQrBitmap.asImageBitmap(),
                        contentDescription = "Profile QR Code for @$username",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .testTag("share_profile_qr_image")
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "QR Code",
                    tint = TelefamRed,
                    modifier = Modifier.size(180.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = displayName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = shareHandle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7888)
            )

            if (!localCopiedBanner.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = localCopiedBanner!!,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B7F3B)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // [Share profile] button
            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "$displayName ($shareHandle) on Telefam")
                        putExtra(Intent.EXTRA_TEXT, "Connect with $displayName ($shareHandle) on Telefam.")
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(shareIntent, "Share profile"))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TelefamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("share_profile_system_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share profile", fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // [Copy @username] button
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Telefam Handle", shareHandle))
                    localCopiedBanner = "Username copied: $shareHandle"
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("share_profile_copy_link_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy username ($shareHandle)", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // [Download QR code] button
            OutlinedButton(
                onClick = onDownloadQrCode,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("share_profile_download_qr_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download QR code", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // [Refresh QR code] button
            OutlinedButton(
                onClick = onRegenerateQrCode,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("share_profile_regenerate_qr_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh QR code", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // [Scan / Open QR profile] button
            OutlinedButton(
                onClick = { showScanLookupDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("share_profile_scan_qr_button")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open profile by QR or @username", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showScanLookupDialog) {
        AlertDialog(
            onDismissRequest = { showScanLookupDialog = false },
            title = { Text("Open profile by @username", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter a Telefam @username to open their profile directly:",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = scannedHandleInput,
                        onValueChange = { scannedHandleInput = it },
                        label = { Text("@username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showScanLookupDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = scannedHandleInput
                            .trim()
                            .substringAfterLast("@")
                            .substringAfterLast("/")
                            .lowercase(Locale.US)
                        val match = uiState.contactsUsers.firstOrNull {
                            it.username.equals(cleaned, ignoreCase = true) || it.userId.equals(cleaned, ignoreCase = true)
                        }
                        showScanLookupDialog = false
                        if (match != null) {
                            onOpenPublicUserProfile(match.userId)
                        } else if (uiState.contactsUsers.isNotEmpty()) {
                            onOpenPublicUserProfile(uiState.contactsUsers.first().userId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelefamRed)
                ) {
                    Text("Open Profile", color = Color.White)
                }
            }
        )
    }
}
