package com.telefam.company.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.TelefamAuthBorderDark
import com.example.ui.theme.TelefamAuthBorderLight
import com.example.ui.theme.TelefamError
import com.example.ui.theme.TelefamRed
import com.example.ui.theme.TelefamRedDark
import com.example.ui.theme.TelefamSuccess
import com.example.ui.theme.TelefamTopWaveDark
import com.example.ui.theme.TelefamTopWaveLight
import com.example.ui.theme.TelefamWarning
import com.example.ui.theme.isTelefamDarkTheme
import com.telefam.company.app.domain.AuthMode
import com.telefam.company.app.domain.TelefamValidators
import com.telefam.company.app.ui.TelefamUiState
import com.telefam.company.app.ui.components.GoogleColorLogoIcon
import com.telefam.company.app.ui.components.TelefamPaperPlaneLogo

/**
 * Pixel-faithful Auth Screen matching Reference Image 1 (Sign up / Login / OTP / Forgot Password / Google Auth)
 * with adaptive layout support for Android phones, foldables, tablets, and desktop windows.
 */
@Composable
fun AuthScreen(
    uiState: TelefamUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onOtpChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onPrimaryAction: () -> Unit,
    onSwitchMode: (AuthMode) -> Unit,
    onContinueWithGoogle: () -> Unit,
    onSignInWithPasskey: () -> Unit = {},
    onResendOtp: () -> Unit,
    onRequestLoginOtpInstead: () -> Unit
) {
    val isDark = isTelefamDarkTheme()
    val topWaveColor = if (isDark) TelefamTopWaveDark else TelefamTopWaveLight

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .testTag("auth_screen_root")
    ) {
        val isExpandedDesktopOrTablet = maxWidth >= 840.dp

        // 1. Top Soft Blush Curved Wave (Exact curve from Reference Image 1)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.TopCenter)
        ) {
            val w = size.width
            val h = size.height
            val topWavePath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.72f)
                cubicTo(
                    w * 0.76f, h * 0.12f,
                    w * 0.34f, h * 0.92f,
                    0f, h * 0.36f
                )
                close()
            }
            drawPath(path = topWavePath, color = topWaveColor)
        }

        // 2. Bottom Vibrant Red Curved Wave (Exact S-curve from Reference Image 1)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val h = size.height
            val bottomWavePath = Path().apply {
                moveTo(0f, h * 0.12f)
                cubicTo(
                    w * 0.35f, h * 0.56f,
                    w * 0.68f, h * 0.08f,
                    w, h * 0.42f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path = bottomWavePath, color = TelefamRed)
        }

        if (isExpandedDesktopOrTablet) {
            // Canonical Split Two-Pane Layout for Tablets & Desktops
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TelefamBrandHeader(logoSize = 112)
                    Spacer(modifier = Modifier.height(24.dp))
                    ForgotPasswordBottomWaveLink(
                        authMode = uiState.authMode,
                        onForgotPasswordClick = { onSwitchMode(AuthMode.FORGOT_PASSWORD_REQUEST) },
                        onBackToLoginClick = { onSwitchMode(AuthMode.LOGIN) },
                        textColor = TelefamRed
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthCardSurface(
                        uiState = uiState,
                        isDark = isDark,
                        onEmailChanged = onEmailChanged,
                        onPasswordChanged = onPasswordChanged,
                        onConfirmPasswordChanged = onConfirmPasswordChanged,
                        onOtpChanged = onOtpChanged,
                        onTogglePasswordVisibility = onTogglePasswordVisibility,
                        onToggleConfirmPasswordVisibility = onToggleConfirmPasswordVisibility,
                        onPrimaryAction = onPrimaryAction,
                        onSwitchMode = onSwitchMode,
                        onContinueWithGoogle = onContinueWithGoogle,
                        onSignInWithPasskey = onSignInWithPasskey,
                        onResendOtp = onResendOtp,
                        onRequestLoginOtpInstead = onRequestLoginOtpInstead
                    )
                }
            }
        } else {
            // Compact Phone & Medium Portrait Layout (1:1 Reference Image 1)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Centered Red Circle Paper Plane Logo + "Telefam" + "Stay connected. Stay together."
                TelefamBrandHeader(logoSize = 90)

                Spacer(modifier = Modifier.height(22.dp))

                // Elevated White Rounded Card
                AuthCardSurface(
                    uiState = uiState,
                    isDark = isDark,
                    onEmailChanged = onEmailChanged,
                    onPasswordChanged = onPasswordChanged,
                    onConfirmPasswordChanged = onConfirmPasswordChanged,
                    onOtpChanged = onOtpChanged,
                    onTogglePasswordVisibility = onTogglePasswordVisibility,
                    onToggleConfirmPasswordVisibility = onToggleConfirmPasswordVisibility,
                    onPrimaryAction = onPrimaryAction,
                    onSwitchMode = onSwitchMode,
                    onContinueWithGoogle = onContinueWithGoogle,
                    onSignInWithPasskey = onSignInWithPasskey,
                    onResendOtp = onResendOtp,
                    onRequestLoginOtpInstead = onRequestLoginOtpInstead
                )

                Spacer(modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.height(32.dp))

                // "Forgot password?" underlined in white centered inside the bottom red wave
                ForgotPasswordBottomWaveLink(
                    authMode = uiState.authMode,
                    onForgotPasswordClick = { onSwitchMode(AuthMode.FORGOT_PASSWORD_REQUEST) },
                    onBackToLoginClick = { onSwitchMode(AuthMode.LOGIN) },
                    textColor = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TelefamBrandHeader(
    logoSize: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TelefamPaperPlaneLogo(
            size = logoSize.dp,
            animateFloatPulse = true,
            modifier = Modifier
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    ambientColor = TelefamRed.copy(alpha = 0.4f),
                    spotColor = TelefamRed.copy(alpha = 0.45f)
                )
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            ),
            color = TelefamRed
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AuthCardSurface(
    uiState: TelefamUiState,
    isDark: Boolean,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onOtpChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onPrimaryAction: () -> Unit,
    onSwitchMode: (AuthMode) -> Unit,
    onContinueWithGoogle: () -> Unit,
    onSignInWithPasskey: () -> Unit,
    onResendOtp: () -> Unit,
    onRequestLoginOtpInstead: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val fieldBorderColor = if (isDark) TelefamAuthBorderDark else TelefamAuthBorderLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card Title & Subtitle based on AuthMode
            val (titleText, subtitleText) = when (uiState.authMode) {
                AuthMode.SIGN_UP -> stringResource(R.string.sign_up_title) to stringResource(R.string.sign_up_subtitle)
                AuthMode.LOGIN -> stringResource(R.string.login_title) to stringResource(R.string.login_subtitle)
                AuthMode.VERIFY_SIGNUP_OTP -> stringResource(R.string.verify_otp_title) to stringResource(
                    R.string.verify_otp_subtitle,
                    uiState.email.ifBlank { "your email" }
                )
                AuthMode.FORGOT_PASSWORD_REQUEST -> stringResource(R.string.forgot_password_title) to stringResource(
                    R.string.forgot_password_subtitle
                )
                AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET -> stringResource(R.string.reset_password_title) to stringResource(
                    R.string.reset_password_subtitle
                )
                AuthMode.NEW_DEVICE_VERIFY_OTP -> stringResource(R.string.new_device_otp_title) to stringResource(
                    R.string.new_device_otp_subtitle,
                    uiState.telemetry?.locationLabel ?: "New Device"
                )
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TelefamRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("auth_card_title")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Rate Limit Cooldown Banners (15-min Login Lock or 30-min OTP Lock)
            if (uiState.loginCooldownRemainingSec > 0 && uiState.authMode == AuthMode.LOGIN) {
                Spacer(modifier = Modifier.height(12.dp))
                CooldownWarningCard(
                    message = stringResource(
                        R.string.login_cooldown_banner,
                        formatCountdown(uiState.loginCooldownRemainingSec)
                    ),
                    actionLabel = stringResource(R.string.request_login_otp),
                    onActionClick = onRequestLoginOtpInstead
                )
            }

            if (uiState.otpCooldownRemainingSec > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                CooldownWarningCard(
                    message = stringResource(
                        R.string.otp_cooldown_banner,
                        formatCountdown(uiState.otpCooldownRemainingSec)
                    ),
                    actionLabel = null,
                    onActionClick = null
                )
            }

            // Info or Error Banner
            uiState.infoBannerMessage?.let { info ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TelefamSuccess.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TelefamSuccess,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            uiState.errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TelefamError.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = TelefamError,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TelefamError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Login Saved Accounts (from Log Out, Deactivation, or 30-Day Deletion Recovery)
            if (uiState.authMode == AuthMode.LOGIN && uiState.quickLoginAccounts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Saved accounts on this device",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.quickLoginAccounts.take(3).forEach { savedAcc ->
                        val statusBadge = when (savedAcc.accountStatus) {
                            "DEACTIVATED" -> "Deactivated • Sign in to reactivate"
                            "PENDING_DELETION_30D" -> "Scheduled for deletion • Sign in within 30d to recover"
                            else -> "Tap to fill email"
                        }
                        Surface(
                            onClick = { onEmailChanged(savedAcc.email) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                1.dp,
                                if (uiState.email.equals(savedAcc.email, ignoreCase = true)) TelefamRed else fieldBorderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_login_account_${savedAcc.username}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = TelefamRed.copy(alpha = 0.14f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = savedAcc.fullName.ifBlank { savedAcc.email }.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = TelefamRed
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${savedAcc.fullName} (@${savedAcc.username})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${savedAcc.email} • $statusBadge",
                                        fontSize = 11.8.sp,
                                        color = if (savedAcc.accountStatus == "PENDING_DELETION_30D") TelefamRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Email Field (Shown in SIGN_UP, LOGIN, FORGOT_PASSWORD_REQUEST)
            if (uiState.authMode in listOf(
                    AuthMode.SIGN_UP,
                    AuthMode.LOGIN,
                    AuthMode.FORGOT_PASSWORD_REQUEST
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.email_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = onEmailChanged,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.email_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = stringResource(R.string.email_label),
                                tint = TelefamRed
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = if (uiState.authMode == AuthMode.FORGOT_PASSWORD_REQUEST) ImeAction.Done else ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = fieldBorderColor,
                            focusedBorderColor = TelefamRed,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )
                }
            }

            // OTP Input + 60s Countdown Timer (Shown in VERIFY_SIGNUP_OTP, NEW_DEVICE_VERIFY_OTP, FORGOT_PASSWORD_VERIFY_AND_RESET)
            if (uiState.authMode in listOf(
                    AuthMode.VERIFY_SIGNUP_OTP,
                    AuthMode.NEW_DEVICE_VERIFY_OTP,
                    AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.otp_code_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (uiState.otpResendCountdownSec > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = TelefamRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(
                                        R.string.resend_otp_in,
                                        formatCountdown(uiState.otpResendCountdownSec.toLong())
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TelefamRed,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("otp_countdown_timer_text")
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.resend_otp_now),
                                style = MaterialTheme.typography.labelMedium,
                                color = TelefamRed,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable(enabled = uiState.otpCooldownRemainingSec == 0L) { onResendOtp() }
                                    .testTag("otp_resend_button")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.otpCode,
                        onValueChange = onOtpChanged,
                        placeholder = {
                            Text(
                                text = if (uiState.authMode == AuthMode.NEW_DEVICE_VERIFY_OTP) {
                                    "6-digit code or XXXX-XXXX recovery code"
                                } else {
                                    stringResource(R.string.otp_code_placeholder)
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = stringResource(R.string.otp_code_label),
                                tint = TelefamRed
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (uiState.authMode == AuthMode.NEW_DEVICE_VERIFY_OTP) {
                                KeyboardType.Ascii
                            } else {
                                KeyboardType.NumberPassword
                            },
                            imeAction = if (uiState.authMode == AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET) ImeAction.Next else ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = fieldBorderColor,
                            focusedBorderColor = TelefamRed,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_otp_input")
                    )
                    if (uiState.authMode == AuthMode.NEW_DEVICE_VERIFY_OTP) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter your 6-digit verification code (email or authenticator app) or an 8-character backup recovery code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Password Field (Shown in SIGN_UP, LOGIN, FORGOT_PASSWORD_VERIFY_AND_RESET)
            if (uiState.authMode in listOf(
                    AuthMode.SIGN_UP,
                    AuthMode.LOGIN,
                    AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET
                )
            ) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (uiState.authMode == AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET) {
                            stringResource(R.string.new_password_label)
                        } else {
                            stringResource(R.string.password_label)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChanged,
                        placeholder = {
                            Text(
                                text = if (uiState.authMode == AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET) {
                                    stringResource(R.string.new_password_placeholder)
                                } else {
                                    stringResource(R.string.password_placeholder)
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = stringResource(R.string.password_label),
                                tint = TelefamRed
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = onTogglePasswordVisibility,
                                modifier = Modifier.testTag("toggle_password_visibility")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isPasswordVisible) {
                                        Icons.Outlined.Visibility
                                    } else {
                                        Icons.Outlined.VisibilityOff
                                    },
                                    contentDescription = "Toggle password visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (uiState.isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (uiState.authMode == AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onPrimaryAction()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = fieldBorderColor,
                            focusedBorderColor = TelefamRed,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    // Password strength indicator on Sign up & Reset Password
                    if (uiState.authMode in listOf(AuthMode.SIGN_UP, AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET) &&
                        uiState.password.isNotEmpty()
                    ) {
                        val strength = TelefamValidators.calculatePasswordStrength(uiState.password)
                        val strengthColor = when {
                            strength >= 0.8f -> TelefamSuccess
                            strength >= 0.5f -> TelefamWarning
                            else -> TelefamError
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { strength },
                            color = strengthColor,
                            trackColor = fieldBorderColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
            }

            // Confirm New Password Field (Shown in FORGOT_PASSWORD_VERIFY_AND_RESET)
            if (uiState.authMode == AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.confirm_password_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = onConfirmPasswordChanged,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.confirm_password_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = stringResource(R.string.confirm_password_label),
                                tint = TelefamRed
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = onToggleConfirmPasswordVisibility) {
                                Icon(
                                    imageVector = if (uiState.isConfirmPasswordVisible) {
                                        Icons.Outlined.Visibility
                                    } else {
                                        Icons.Outlined.VisibilityOff
                                    },
                                    contentDescription = "Toggle confirm password visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (uiState.isConfirmPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = fieldBorderColor,
                            focusedBorderColor = TelefamRed,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_confirm_password_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Solid Red Button ("Sign up", "Login", "Send Verification OTP", "Verify & Continue", "Update Password")
            val primaryBtnLabel = when (uiState.authMode) {
                AuthMode.SIGN_UP -> stringResource(R.string.sign_up_button)
                AuthMode.LOGIN -> stringResource(R.string.login_button)
                AuthMode.FORGOT_PASSWORD_REQUEST -> stringResource(R.string.send_otp_button)
                AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET -> stringResource(R.string.reset_password_button)
                AuthMode.VERIFY_SIGNUP_OTP, AuthMode.NEW_DEVICE_VERIFY_OTP -> stringResource(R.string.verify_otp_button)
            }

            val isActionDisabledByCooldown =
                (uiState.authMode == AuthMode.LOGIN && uiState.loginCooldownRemainingSec > 0) ||
                    (uiState.authMode in listOf(
                        AuthMode.VERIFY_SIGNUP_OTP,
                        AuthMode.NEW_DEVICE_VERIFY_OTP,
                        AuthMode.FORGOT_PASSWORD_REQUEST,
                        AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET
                    ) && uiState.otpCooldownRemainingSec > 0)

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onPrimaryAction()
                },
                enabled = !uiState.isLoading && !isActionDisabledByCooldown,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelefamRed,
                    contentColor = Color.White,
                    disabledContainerColor = TelefamRed.copy(alpha = 0.45f),
                    disabledContentColor = Color.White.copy(alpha = 0.8f)
                ),
                contentPadding = PaddingValues(vertical = 15.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_primary_submit_button")
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = primaryBtnLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // "or" Divider + "Continue with Google" Button (Shown in SIGN_UP and LOGIN matching Reference Image 1)
            if (uiState.authMode in listOf(AuthMode.SIGN_UP, AuthMode.LOGIN)) {
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.or_divider),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onContinueWithGoogle,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_with_google_button")
                ) {
                    GoogleColorLogoIcon(size = 20.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.continue_with_google),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (uiState.authMode == AuthMode.LOGIN) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onSignInWithPasskey,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sign_in_with_passkey_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Sign in with a passkey",
                            tint = TelefamRed,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sign in with a passkey",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Footer Switch Link ("Already have an account? Login" or "Don't have an account? Sign up")
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (uiState.authMode) {
                    AuthMode.SIGN_UP -> {
                        Text(
                            text = stringResource(R.string.already_have_account) + "  ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.login_title),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = TelefamRed,
                            modifier = Modifier
                                .clickable { onSwitchMode(AuthMode.LOGIN) }
                                .padding(vertical = 4.dp)
                                .testTag("switch_to_login_link")
                        )
                    }
                    AuthMode.LOGIN -> {
                        Text(
                            text = stringResource(R.string.dont_have_account) + "  ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.sign_up_title),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = TelefamRed,
                            modifier = Modifier
                                .clickable { onSwitchMode(AuthMode.SIGN_UP) }
                                .padding(vertical = 4.dp)
                                .testTag("switch_to_signup_link")
                        )
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.back_to_login),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = TelefamRed,
                            modifier = Modifier
                                .clickable { onSwitchMode(AuthMode.LOGIN) }
                                .padding(vertical = 4.dp)
                                .testTag("back_to_login_link")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForgotPasswordBottomWaveLink(
    authMode: AuthMode,
    onForgotPasswordClick: () -> Unit,
    onBackToLoginClick: () -> Unit,
    textColor: Color
) {
    val isForgotFlow = authMode in listOf(
        AuthMode.FORGOT_PASSWORD_REQUEST,
        AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET
    )
    Text(
        text = if (isForgotFlow) {
            stringResource(R.string.back_to_login)
        } else {
            stringResource(R.string.forgot_password_link)
        },
        style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline
        ),
        color = textColor,
        modifier = Modifier
            .clickable {
                if (isForgotFlow) onBackToLoginClick() else onForgotPasswordClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("forgot_password_bottom_link")
    )
}

@Composable
private fun CooldownWarningCard(
    message: String,
    actionLabel: String?,
    onActionClick: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = TelefamWarning.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, TelefamWarning.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cooldown_warning_banner")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = TelefamWarning,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TelefamRed),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("request_otp_on_locked_login_button")
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = TelefamRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatCountdown(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
