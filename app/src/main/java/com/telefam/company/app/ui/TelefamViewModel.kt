package com.telefam.company.app.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.telefam.company.app.data.AccountSettingsAndSecurityEntity
import com.telefam.company.app.data.AuthActionOutcome
import com.telefam.company.app.data.ConnectedAppSessionEntity
import com.telefam.company.app.data.ForensicAuditLogEntity
import com.telefam.company.app.data.PrivacySettingsEntity
import com.telefam.company.app.data.ProblemSupportTicketEntity
import com.telefam.company.app.data.ProfileEntity
import com.telefam.company.app.data.PublicProfileSnapshot
import com.telefam.company.app.data.QuickLoginAccountEntity
import com.telefam.company.app.data.TelefamSecurityRepository
import com.telefam.company.app.data.TelefamSettingsRepository
import com.telefam.company.app.data.TelefamSocialRepository
import com.telefam.company.app.data.TrustedDeviceEntity
import com.telefam.company.app.data.UsernameConflictException
import com.telefam.company.app.domain.AppPermissionType
import com.telefam.company.app.domain.AppThemeOption
import com.telefam.company.app.domain.AuthMode
import com.telefam.company.app.domain.ContactsFilterMode
import com.telefam.company.app.domain.CountryDialCode
import com.telefam.company.app.domain.DeviceAndNetworkTelemetry
import com.telefam.company.app.domain.DiscoverableUserItem
import com.telefam.company.app.domain.EncryptedUserSession
import com.telefam.company.app.domain.FollowListType
import com.telefam.company.app.domain.HelpSupportArticle
import com.telefam.company.app.domain.HomeNavTab
import com.telefam.company.app.domain.LegalPolicyDocument
import com.telefam.company.app.domain.OwnerProfileCooldownState
import com.telefam.company.app.domain.OwnerProfileSubTab
import com.telefam.company.app.domain.PostsFilterOption
import com.telefam.company.app.domain.PrivacyAudience
import com.telefam.company.app.domain.PrivacyControlField
import com.telefam.company.app.domain.PrivacyRuleKey
import com.telefam.company.app.domain.ReportLocationOption
import com.telefam.company.app.domain.ReportReasonOption
import com.telefam.company.app.domain.ReportUrgencyOption
import com.telefam.company.app.domain.RiskLevel
import com.telefam.company.app.domain.SafetyTopicGuide
import com.telefam.company.app.domain.SanitizedAvatarResult
import com.telefam.company.app.domain.SettingsSubScreen
import com.telefam.company.app.domain.SocialPrivacyOption
import com.telefam.company.app.domain.TelefamCountries
import com.telefam.company.app.domain.TelefamInputSanitizer
import com.telefam.company.app.domain.TelefamSettingsCatalog
import com.telefam.company.app.domain.TelefamValidators
import com.telefam.company.app.domain.UserPostItem
import com.telefam.company.app.security.AndroidSandboxMediaProcessor
import com.telefam.company.app.security.TelefamCryptoAndTelemetry
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppDestination {
    LAUNCH_ANIMATION,
    AUTH,
    PROFILE_SETUP,
    MAIN_HOME,
    PRIVACY_SETTINGS,
    SETTINGS_AND_PRIVACY,
    PUBLIC_USER_PROFILE
}

data class TelefamUiState(
    val destination: AppDestination = AppDestination.LAUNCH_ANIMATION,
    val selectedHomeTab: HomeNavTab = HomeNavTab.CHATS,
    val authMode: AuthMode = AuthMode.SIGN_UP,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val otpCode: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoBannerMessage: String? = null,

    // OTP & Brute-Force Cooldown Timers
    val otpResendCountdownSec: Int = 0,
    val loginCooldownRemainingSec: Long = 0L,
    val otpCooldownRemainingSec: Long = 0L,

    // Profile Setup Fields (Reference Image 2)
    val fullName: String = "",
    val username: String = "",
    val isUsernameAvailable: Boolean? = null,
    val usernameSuggestions: List<String> = emptyList(),
    val selectedCountry: CountryDialCode = TelefamCountries.DEFAULT, // Kenya +254
    val phoneNumber: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val bio: String = "",
    val sanitizedAvatar: SanitizedAvatarResult? = null,
    val pendingCropBitmap: Bitmap? = null,
    val profileFieldErrors: Map<String, String> = emptyMap(),

    // Session, Profile & Privacy Settings
    val activeSession: EncryptedUserSession? = null,
    val savedProfile: ProfileEntity? = null,
    val privacySettings: PrivacySettingsEntity = PrivacySettingsEntity(userId = "local_telefam_member"),
    val telemetry: DeviceAndNetworkTelemetry? = null,
    val screenWidthDp: Int = 411,

    // Contacts Discovery & Social Graph State (Image 2 & Image 1)
    val contactsUsers: List<DiscoverableUserItem> = emptyList(),
    val contactsFilterMode: ContactsFilterMode = ContactsFilterMode.SUGGESTED,
    val contactsSearchQuery: String = "",
    val isContactsSearchActive: Boolean = false,
    val contactsHasMorePages: Boolean = false,
    val contactsCurrentPage: Int = 0,
    val isContactsLoading: Boolean = false,
    val isContactsOfflineBannerVisible: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val contactsMatchCount: Int = 0,
    val contactsErrorMessage: String? = null,

    // Public User Profile Screen State (Image 1)
    val activePublicProfile: DiscoverableUserItem? = null,
    val activePublicProfilePosts: List<UserPostItem> = emptyList(),
    val activePublicProfilePostsFilter: PostsFilterOption = PostsFilterOption.ALL_POSTS,
    val activePublicProfileBlocked: Boolean = false,
    val activeFollowListType: FollowListType? = null,
    val followListUsers: List<DiscoverableUserItem> = emptyList(),
    val followListPage: Int = 1,
    val followListHasMore: Boolean = false,
    val profileStatusBanner: String? = null,

    // Owner Profile Screen State (Reference Image)
    val ownerFollowersCount: Long = 0L,
    val ownerFollowingCount: Long = 0L,
    val ownerSubscribersCount: Long = 0L,
    val ownerSelectedSubTab: OwnerProfileSubTab = OwnerProfileSubTab.POSTS,
    val ownerPosts: List<UserPostItem> = emptyList(),
    val ownerCooldownState: OwnerProfileCooldownState = OwnerProfileCooldownState.compute(null, null),
    val isOwnerEditSheetOpen: Boolean = false,
    val isOwnerProfileRefreshing: Boolean = false,
    val ownerFollowListType: FollowListType? = null,
    val ownerFollowListUsers: List<DiscoverableUserItem> = emptyList(),
    val ownerFollowListPage: Int = 1,
    val ownerFollowListHasMore: Boolean = false,
    val ownerStatusBanner: String? = null,
    val isOwnerMenuOpen: Boolean = false,

    // Settings and Privacy System State (All 13 Reference Sections & Subscreens)
    val settingsSubScreen: SettingsSubScreen = SettingsSubScreen.ROOT,
    val settingsNavigationStack: List<SettingsSubScreen> = emptyList(),
    val accountSettings: AccountSettingsAndSecurityEntity = AccountSettingsAndSecurityEntity(ownerUserId = "local_telefam_member"),
    val activePrivacyControlField: PrivacyControlField = PrivacyControlField.COMMENTS,
    val activePermissionType: AppPermissionType = AppPermissionType.CONTACTS,
    val activeSafetyGuide: SafetyTopicGuide = TelefamSettingsCatalog.SAFETY_GUIDES.first(),
    val activeHelpArticle: HelpSupportArticle = TelefamSettingsCatalog.HELP_ARTICLES.first(),
    val activeLegalDocument: LegalPolicyDocument = TelefamSettingsCatalog.LEGAL_DOCUMENTS.first(),
    val activeAboutDetailKey: String = "ABOUT_APP",
    val trustedDevices: List<TrustedDeviceEntity> = emptyList(),
    val connectedApps: List<ConnectedAppSessionEntity> = emptyList(),
    val recentSecurityLogs: List<ForensicAuditLogEntity> = emptyList(),
    val supportTickets: List<ProblemSupportTicketEntity> = emptyList(),
    val blockedAccountItems: List<DiscoverableUserItem> = emptyList(),
    val blockedAccountsSearchQuery: String = "",
    val helpSearchQuery: String = "",
    val settingsOtpSentEmail: String? = null,
    val settingsOtpResendCountdownSec: Int = 0,
    val settingsStatusBanner: String? = null,
    val settingsErrorBanner: String? = null,
    val canRetrySettingsError: Boolean = false,
    val lastSubmittedTicketId: String? = null,
    val profileQrBitmap: Bitmap? = null,
    val quickLoginAccounts: List<QuickLoginAccountEntity> = emptyList()
)

class TelefamViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TelefamSecurityRepository(application.applicationContext)
    private val socialRepository = TelefamSocialRepository(
        context = application.applicationContext,
        dao = repository.dao
    )
    private val settingsRepository = TelefamSettingsRepository(
        context = application.applicationContext,
        dao = repository.dao,
        securityRepository = repository,
        socialRepository = socialRepository
    )

    private val _uiState = MutableStateFlow(TelefamUiState())
    val uiState: StateFlow<TelefamUiState> = _uiState.asStateFlow()

    private var currentLocationTriple: Triple<String, Double?, Double?> =
        Triple("Kenya • Africa/Nairobi", -1.2921, 36.8219)
    private var timerJob: Job? = null
    private var usernameCheckJob: Job? = null
    private var ownerReactiveObserverJob: Job? = null
    private var lastRetryableSettingsAction: (() -> Unit)? = null

    private val safeExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = throwable.localizedMessage ?: "An unexpected error occurred. Please try again.",
                settingsErrorBanner = if (it.destination == AppDestination.SETTINGS_AND_PRIVACY) {
                    throwable.localizedMessage ?: "Unable to complete action. Tap Retry to try again."
                } else {
                    it.settingsErrorBanner
                },
                canRetrySettingsError = lastRetryableSettingsAction != null
            )
        }
    }

    init {
        initializeLaunchAndTelemetry()
        startOneSecondSecurityTicker()
    }

    private fun initializeLaunchAndTelemetry() {
        viewModelScope.launch(safeExceptionHandler) {
            val startMs = System.currentTimeMillis()
            val context = getApplication<Application>().applicationContext
            currentLocationTriple = TelefamCryptoAndTelemetry.fetchBestLocationOrDefault(context)

            val initialTelemetry = repository.refreshTelemetrySnapshot(
                email = null,
                screenWidthDp = _uiState.value.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )
            val (existingSession, existingProfile) = repository.getActiveSessionWithTelemetry(
                screenWidthDp = _uiState.value.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateAnomaly = false
            )
            val resolvedUserId = existingSession?.userId ?: existingProfile?.userId ?: "local_telefam_member"
            val privacyEntity = repository.getOrCreatePrivacySettings(resolvedUserId)
            val quickAccounts = settingsRepository.getSavedQuickLoginAccounts()
            val initialAccSettings = settingsRepository.getOrCreateAccountSettings(
                userId = resolvedUserId,
                email = existingSession?.email ?: quickAccounts.firstOrNull()?.email ?: "member@telefam.company",
                phone = existingProfile?.let { "${it.phoneCountryCode}${it.phoneNumber}" } ?: "",
                telemetry = initialTelemetry
            )

            val elapsed = System.currentTimeMillis() - startMs
            if (elapsed < 500L) {
                delay(500L - elapsed)
            }

            val targetDest = when {
                existingSession == null -> AppDestination.AUTH
                existingProfile != null || existingSession.hasCompletedProfile -> AppDestination.MAIN_HOME
                else -> AppDestination.PROFILE_SETUP
            }

            // If entering Profile Setup with an active session email, autofill name & unique username from email prefix
            val autofill = if (existingSession != null && existingProfile == null) {
                repository.resolveUniqueAutofillFromEmail(existingSession.email, existingSession.userId)
            } else {
                null
            }

            _uiState.update { state ->
                state.copy(
                    destination = targetDest,
                    activeSession = existingSession,
                    savedProfile = existingProfile,
                    privacySettings = privacyEntity,
                    accountSettings = initialAccSettings,
                    telemetry = initialTelemetry,
                    quickLoginAccounts = quickAccounts,
                    email = existingSession?.email ?: quickAccounts.firstOrNull()?.email ?: state.email,
                    fullName = existingProfile?.fullName ?: autofill?.derivedFullName ?: state.fullName,
                    username = existingProfile?.username ?: autofill?.assignedUniqueUsername ?: state.username,
                    isUsernameAvailable = if (autofill != null) true else state.isUsernameAvailable,
                    usernameSuggestions = autofill?.alternativeSuggestions ?: emptyList(),
                    phoneNumber = existingProfile?.phoneNumber ?: state.phoneNumber,
                    gender = existingProfile?.gender ?: state.gender,
                    dateOfBirth = existingProfile?.dateOfBirth ?: state.dateOfBirth,
                    bio = existingProfile?.bio ?: state.bio,
                    selectedCountry = existingProfile?.phoneIsoCode?.let { TelefamCountries.findByIso(it) } ?: state.selectedCountry,
                    ownerCooldownState = OwnerProfileCooldownState.compute(
                        lastFullNameChangedAtMs = existingProfile?.lastFullNameChangedAtMs,
                        lastUsernameChangedAtMs = existingProfile?.lastUsernameChangedAtMs
                    ),
                    hasContactsPermission = socialRepository.hasReadContactsPermission()
                )
            }
            refreshContactsDirectory(forceDailyRefresh = false)
            startOwnerProfileReactiveObserver()
        }
    }

    private fun startOneSecondSecurityTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch(safeExceptionHandler) {
            while (true) {
                delay(1000L)
                val state = _uiState.value
                val nextResend = (state.otpResendCountdownSec - 1).coerceAtLeast(0)
                val nextSettingsResend = (state.settingsOtpResendCountdownSec - 1).coerceAtLeast(0)
                val nextLoginLock = (state.loginCooldownRemainingSec - 1L).coerceAtLeast(0L)
                val nextOtpLock = (state.otpCooldownRemainingSec - 1L).coerceAtLeast(0L)

                if (nextResend != state.otpResendCountdownSec ||
                    nextSettingsResend != state.settingsOtpResendCountdownSec ||
                    nextLoginLock != state.loginCooldownRemainingSec ||
                    nextOtpLock != state.otpCooldownRemainingSec
                ) {
                    _uiState.update {
                        it.copy(
                            otpResendCountdownSec = nextResend,
                            settingsOtpResendCountdownSec = nextSettingsResend,
                            loginCooldownRemainingSec = nextLoginLock,
                            otpCooldownRemainingSec = nextOtpLock
                        )
                    }
                }
            }
        }
    }

    fun updateScreenWidthDp(widthDp: Int) {
        if (widthDp == _uiState.value.screenWidthDp) return
        _uiState.update { it.copy(screenWidthDp = widthDp) }
        refreshTelemetry()
    }

    fun onLocationPermissionResult() {
        viewModelScope.launch(safeExceptionHandler) {
            val context = getApplication<Application>().applicationContext
            currentLocationTriple = TelefamCryptoAndTelemetry.fetchBestLocationOrDefault(context)
            refreshTelemetry()
        }
    }

    private fun refreshTelemetry() {
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val updatedTelemetry = repository.refreshTelemetrySnapshot(
                email = state.email,
                screenWidthDp = state.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )
            val (loginRem, otpRem) = repository.checkCooldownLocks(
                email = state.email,
                ipSubnetFp = updatedTelemetry.ipSubnetFingerprint
            )
            _uiState.update {
                it.copy(
                    telemetry = updatedTelemetry,
                    loginCooldownRemainingSec = loginRem,
                    otpCooldownRemainingSec = otpRem
                )
            }
        }
    }

    // --- Auth Input Handlers ---
    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun onOtpCodeChanged(value: String) {
        val state = _uiState.value
        val cleaned = if (state.authMode == AuthMode.NEW_DEVICE_VERIFY_OTP) {
            value.filter { it.isLetterOrDigit() || it == '-' }.uppercase().take(9)
        } else {
            value.filter { it.isDigit() }.take(6)
        }
        _uiState.update { it.copy(otpCode = cleaned, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun switchAuthMode(targetMode: AuthMode) {
        _uiState.update {
            it.copy(
                authMode = targetMode,
                otpCode = "",
                errorMessage = null,
                infoBannerMessage = null
            )
        }
        refreshTelemetry()
    }

    fun submitPrimaryAuthAction() {
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val telemetry = repository.refreshTelemetrySnapshot(
                email = state.email,
                screenWidthDp = state.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )

            if (telemetry.riskLevel == RiskLevel.CRITICAL && telemetry.riskScore >= 90) {
                repository.recordForensicEvent(
                    category = "ANOMALY_FIREWALL",
                    outcome = "BLOCKED_HIGH_RISK",
                    target = state.email,
                    telemetry = telemetry,
                    endpoint = "/v1/auth/guard",
                    detail = "Blocked automated traffic burst (Risk=${telemetry.riskScore})"
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        telemetry = telemetry,
                        errorMessage = "Too many requests detected. Please wait a moment and try again."
                    )
                }
                return@launch
            }

            val outcome = when (state.authMode) {
                AuthMode.SIGN_UP -> {
                    repository.initiateSignUpWithOtp(
                        email = state.email,
                        password = state.password,
                        telemetry = telemetry
                    )
                }
                AuthMode.LOGIN -> {
                    repository.loginWithEmailAndPassword(
                        email = state.email,
                        password = state.password,
                        telemetry = telemetry,
                        forceStepUpOtpForNewDevice = false
                    )
                }
                AuthMode.VERIFY_SIGNUP_OTP -> {
                    repository.verifyOtpChallenge(
                        email = state.email,
                        purpose = "SIGNUP",
                        enteredOtp = state.otpCode,
                        newPassword = null,
                        confirmPassword = null,
                        telemetry = telemetry
                    )
                }
                AuthMode.FORGOT_PASSWORD_REQUEST -> {
                    repository.requestOtpForLockedLoginOrForgotPassword(
                        email = state.email,
                        purpose = "FORGOT_PASSWORD",
                        telemetry = telemetry
                    )
                }
                AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET -> {
                    repository.verifyOtpChallenge(
                        email = state.email,
                        purpose = "FORGOT_PASSWORD",
                        enteredOtp = state.otpCode,
                        newPassword = state.password,
                        confirmPassword = state.confirmPassword,
                        telemetry = telemetry
                    )
                }
                AuthMode.NEW_DEVICE_VERIFY_OTP -> {
                    repository.verifyOtpChallenge(
                        email = state.email,
                        purpose = "NEW_DEVICE_LOGIN",
                        enteredOtp = state.otpCode,
                        newPassword = null,
                        confirmPassword = null,
                        telemetry = telemetry
                    )
                }
            }

            applyAuthOutcome(outcome, telemetry)
        }
    }

    fun requestOtpInsteadOfLockedLogin() {
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val telemetry = repository.refreshTelemetrySnapshot(
                email = state.email,
                screenWidthDp = state.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )
            val outcome = repository.requestOtpForLockedLoginOrForgotPassword(
                email = state.email,
                purpose = "NEW_DEVICE_LOGIN",
                telemetry = telemetry
            )
            applyAuthOutcome(outcome, telemetry)
        }
    }

    fun resendCurrentOtp() {
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            if (state.otpResendCountdownSec > 0) return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val telemetry = repository.refreshTelemetrySnapshot(
                email = state.email,
                screenWidthDp = state.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )
            val purpose = when (state.authMode) {
                AuthMode.VERIFY_SIGNUP_OTP -> "SIGNUP"
                AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET -> "FORGOT_PASSWORD"
                else -> "NEW_DEVICE_LOGIN"
            }
            val outcome = if (purpose == "SIGNUP") {
                repository.initiateSignUpWithOtp(state.email, state.password, telemetry)
            } else {
                repository.requestOtpForLockedLoginOrForgotPassword(state.email, purpose, telemetry)
            }
            applyAuthOutcome(outcome, telemetry)
        }
    }

    private suspend fun applyAuthOutcome(outcome: AuthActionOutcome, telemetry: DeviceAndNetworkTelemetry) {
        when (outcome) {
            is AuthActionOutcome.OtpDispatched -> {
                val targetMode = when (outcome.purpose) {
                    "SIGNUP" -> AuthMode.VERIFY_SIGNUP_OTP
                    "FORGOT_PASSWORD" -> AuthMode.FORGOT_PASSWORD_VERIFY_AND_RESET
                    else -> AuthMode.NEW_DEVICE_VERIFY_OTP
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        authMode = targetMode,
                        otpCode = "",
                        otpResendCountdownSec = outcome.resendCountdownSeconds,
                        infoBannerMessage = outcome.reasonBanner,
                        errorMessage = null,
                        telemetry = telemetry
                    )
                }
            }
            is AuthActionOutcome.Authenticated -> {
                val profile = outcome.profile
                val session = outcome.session
                val privacyEntity = repository.getOrCreatePrivacySettings(session.userId)
                val accSettings = settingsRepository.getOrCreateAccountSettings(
                    userId = session.userId,
                    email = session.email,
                    phone = profile?.let { "${it.phoneCountryCode}${it.phoneNumber}" } ?: "",
                    telemetry = telemetry
                )
                val recoveryBanner = settingsRepository.checkAndRecoverAccountOnLoginIfWithin30Days(
                    userId = session.userId,
                    email = session.email,
                    telemetry = telemetry
                )

                // Autofill Full Name & Unique Username from Email Prefix when entering Profile Setup
                val autofill = if (profile == null) {
                    repository.resolveUniqueAutofillFromEmail(session.email, session.userId)
                } else {
                    null
                }

                val isExistingRegisteredProfile = profile != null || session.hasCompletedProfile

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        activeSession = session,
                        savedProfile = profile,
                        privacySettings = privacyEntity,
                        accountSettings = accSettings,
                        destination = if (isExistingRegisteredProfile) AppDestination.MAIN_HOME else AppDestination.PROFILE_SETUP,
                        selectedHomeTab = HomeNavTab.CHATS,
                        errorMessage = null,
                        infoBannerMessage = recoveryBanner,
                        ownerStatusBanner = recoveryBanner,
                        telemetry = telemetry,
                        fullName = profile?.fullName
                            ?: state.fullName.ifBlank { autofill?.derivedFullName ?: "" },
                        username = profile?.username
                            ?: state.username.ifBlank { autofill?.assignedUniqueUsername ?: "" },
                        isUsernameAvailable = if (autofill != null) true else state.isUsernameAvailable,
                        usernameSuggestions = autofill?.alternativeSuggestions ?: emptyList(),
                        phoneNumber = profile?.phoneNumber ?: state.phoneNumber,
                        gender = profile?.gender ?: state.gender,
                        dateOfBirth = profile?.dateOfBirth ?: state.dateOfBirth,
                        bio = profile?.bio ?: state.bio
                    )
                }
                startOwnerProfileReactiveObserver()
            }
            is AuthActionOutcome.PasswordResetSuccess -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        authMode = AuthMode.LOGIN,
                        password = "",
                        confirmPassword = "",
                        otpCode = "",
                        infoBannerMessage = "Password successfully updated for ${outcome.email}. You can now Login.",
                        errorMessage = null,
                        telemetry = telemetry
                    )
                }
            }
            is AuthActionOutcome.CooldownLocked -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loginCooldownRemainingSec = if (outcome.lockType == "LOGIN_LOCK_15M") outcome.remainingSeconds else it.loginCooldownRemainingSec,
                        otpCooldownRemainingSec = if (outcome.lockType == "OTP_LOCK_30M") outcome.remainingSeconds else it.otpCooldownRemainingSec,
                        errorMessage = outcome.message,
                        telemetry = telemetry
                    )
                }
            }
            is AuthActionOutcome.ValidationError -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = outcome.message,
                        telemetry = telemetry
                    )
                }
            }
        }
    }

    // --- Passkey Sign-In (FIDO2 / WebAuthn Protocol) ---
    fun signInWithPasskey() {
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val telemetry = repository.refreshTelemetrySnapshot(
                email = state.email,
                screenWidthDp = state.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )
            val outcome = repository.authenticateWithPasskey(
                emailInput = state.email,
                telemetry = telemetry
            )
            applyAuthOutcome(outcome, telemetry)
        }
    }

    // --- Production Google Sign-In via Jetpack Credential Manager ---
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch(safeExceptionHandler) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val telemetry = repository.refreshTelemetrySnapshot(
                email = _uiState.value.email,
                screenWidthDp = _uiState.value.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )
            try {
                val configuredClientId = BuildConfig.TELEFAM_GOOGLE_WEB_CLIENT_ID
                    .trim()
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .removeSuffix("/")

                val credentialManager = CredentialManager.create(activityContext)
                val googleIdOption = GetSignInWithGoogleOption.Builder(
                    configuredClientId
                ).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(activityContext, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                    completeGoogleSignInAccount(
                        email = googleCred.id,
                        displayName = googleCred.displayName ?: ""
                    )
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Unsupported Google credential type received."
                        )
                    }
                }
            } catch (_: GetCredentialCancellationException) {
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        telemetry = telemetry,
                        errorMessage = e.localizedMessage
                            ?: "Google Sign-In could not be completed. Please ensure a Google account is signed in on this device."
                    )
                }
            }
        }
    }

    private fun completeGoogleSignInAccount(email: String, displayName: String) {
        viewModelScope.launch(safeExceptionHandler) {
            val (derivedName, _) = TelefamValidators.deriveFullNameAndUsernameFromEmail(email)
            _uiState.update {
                it.copy(
                    isLoading = true,
                    email = email,
                    fullName = displayName.ifBlank { derivedName }
                )
            }
            val telemetry = repository.refreshTelemetrySnapshot(
                email = email,
                screenWidthDp = _uiState.value.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )
            val outcome = repository.authenticateWithGoogleIdentity(
                googleEmail = email,
                googleSubId = "google_sub_${TelefamCryptoAndTelemetry.sha256Hex(email.toByteArray()).take(18)}",
                displayName = displayName.ifBlank { derivedName },
                telemetry = telemetry,
                forceStepUpOtpForNewDevice = false
            )
            applyAuthOutcome(outcome, telemetry)
        }
    }

    // --- Profile Setup Handlers (Reference Image 2) ---
    fun onFullNameChanged(value: String) {
        _uiState.update {
            it.copy(fullName = value, profileFieldErrors = it.profileFieldErrors - "fullName", errorMessage = null)
        }
    }

    fun onUsernameChanged(value: String) {
        val clean = value.removePrefix("@").filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(24)
        _uiState.update {
            it.copy(
                username = clean,
                isUsernameAvailable = null,
                profileFieldErrors = it.profileFieldErrors - "username",
                errorMessage = null
            )
        }
        usernameCheckJob?.cancel()
        usernameCheckJob = viewModelScope.launch(safeExceptionHandler) {
            delay(180L)
            val state = _uiState.value
            val userId = state.activeSession?.userId ?: ""
            val check = repository.checkUsernameUniquenessAndSuggest(
                requestedUsername = clean,
                fullName = state.fullName,
                excludeUserId = userId
            )
            _uiState.update {
                it.copy(
                    isUsernameAvailable = if (clean.length >= 3) check.isAvailable else null,
                    usernameSuggestions = check.suggestions,
                    profileFieldErrors = if (clean.length >= 3 && !check.isAvailable && check.isValidFormat) {
                        it.profileFieldErrors + ("username" to "Username @$clean is already taken. Select a suggestion below.")
                    } else {
                        it.profileFieldErrors - "username"
                    }
                )
            }
        }
    }

    fun onApplySuggestedUsername(suggestion: String) {
        val clean = suggestion.removePrefix("@").trim()
        _uiState.update {
            it.copy(
                username = clean,
                isUsernameAvailable = true,
                profileFieldErrors = it.profileFieldErrors - "username",
                errorMessage = null
            )
        }
    }

    fun onCountrySelected(country: CountryDialCode) {
        _uiState.update { it.copy(selectedCountry = country, errorMessage = null) }
    }

    fun onPhoneNumberChanged(value: String) {
        val clean = value.filter { it.isDigit() || it == ' ' || it == '-' }.take(18)
        _uiState.update {
            it.copy(phoneNumber = clean, profileFieldErrors = it.profileFieldErrors - "phone", errorMessage = null)
        }
    }

    fun onGenderSelected(gender: String) {
        _uiState.update {
            it.copy(gender = gender, profileFieldErrors = it.profileFieldErrors - "gender", errorMessage = null)
        }
    }

    fun onDateOfBirthSelected(dob: String) {
        val is13Plus = TelefamValidators.validateAgeAtLeast13(dob)
        _uiState.update {
            it.copy(
                dateOfBirth = dob,
                profileFieldErrors = if (!is13Plus) {
                    it.profileFieldErrors + ("dob" to "You must be 13 years or older to use Telefam.")
                } else {
                    it.profileFieldErrors - "dob"
                },
                errorMessage = if (!is13Plus) "Date of birth must be 13 years and above." else null
            )
        }
    }

    fun onBioChanged(value: String) {
        if (value.length <= 150) {
            _uiState.update {
                it.copy(bio = value, profileFieldErrors = it.profileFieldErrors - "bio", errorMessage = null)
            }
        }
    }

    fun onAvatarUriPicked(uri: Uri) {
        viewModelScope.launch(safeExceptionHandler) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val context = getApplication<Application>().applicationContext
            val bitmap = AndroidSandboxMediaProcessor.decodeSampleBitmapForCrop(context, uri)
            if (bitmap != null) {
                _uiState.update { it.copy(isLoading = false, pendingCropBitmap = bitmap) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unsupported image file. Please select a valid photo."
                    )
                }
            }
        }
    }

    fun cancelImageCrop() {
        _uiState.update { it.copy(pendingCropBitmap = null) }
    }

    fun confirmImageCropAndSanitize(
        scale: Float,
        offsetXNormalized: Float,
        offsetYNormalized: Float,
        rotationDegrees: Float
    ) {
        val source = _uiState.value.pendingCropBitmap ?: return
        viewModelScope.launch(safeExceptionHandler) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val context = getApplication<Application>().applicationContext
            val result = AndroidSandboxMediaProcessor.cropAndSanitizeInSandbox(
                context = context,
                sourceBitmap = source,
                scale = scale,
                offsetXNormalized = offsetXNormalized,
                offsetYNormalized = offsetYNormalized,
                rotationDegrees = rotationDegrees,
                dao = repository.dao
            )
            val telemetry = repository.refreshTelemetrySnapshot(
                email = _uiState.value.email,
                screenWidthDp = _uiState.value.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )

            result.fold(
                onSuccess = { artifact ->
                    repository.recordForensicEvent(
                        category = "MEDIA_SANDBOX_FINGERPRINT",
                        outcome = if (artifact.reusedFromFingerprint) "REUSED_EXISTING_FINGERPRINT" else "VERIFIED_AND_FINGERPRINTED",
                        target = _uiState.value.email.ifBlank { "profile_avatar" },
                        telemetry = telemetry,
                        endpoint = "/v1/profile/avatar/sandbox-upload",
                        detail = "dHash=${artifact.perceptualDHash} • SHA256=${artifact.sha256Checksum.take(12)} • Reused=${artifact.reusedFromFingerprint} • Size=${artifact.compressedByteSize}B"
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingCropBitmap = null,
                            sanitizedAvatar = artifact,
                            telemetry = telemetry
                        )
                    }
                },
                onFailure = { err ->
                    repository.recordForensicEvent(
                        category = "MEDIA_SANDBOX",
                        outcome = "QUARANTINE_REJECTED",
                        target = _uiState.value.email,
                        telemetry = telemetry,
                        endpoint = "/v1/profile/avatar/sandbox-upload",
                        detail = err.message ?: "Sandbox rejection"
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingCropBitmap = null,
                            errorMessage = err.message ?: "Unable to process selected photo."
                        )
                    }
                }
            )
        }
    }

    fun submitProfileSetup() {
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val fieldErrors = mutableMapOf<String, String>()

            if (!TelefamValidators.validateFullName(state.fullName)) {
                fieldErrors["fullName"] = "Full name must be at least 2 characters"
            }
            if (!TelefamValidators.validateUsername(state.username)) {
                fieldErrors["username"] = "Username must be 3–24 characters (letters, numbers, _)"
            }
            if (!TelefamValidators.validatePhoneNumber(state.phoneNumber)) {
                fieldErrors["phone"] = "Enter a valid phone number (7–15 digits)"
            }
            if (state.gender.isBlank()) {
                fieldErrors["gender"] = "Please select your gender"
            }
            if (state.dateOfBirth.isBlank()) {
                fieldErrors["dob"] = "Please select your date of birth"
            } else if (!TelefamValidators.validateAgeAtLeast13(state.dateOfBirth)) {
                fieldErrors["dob"] = "You must be at least 13 years old to use Telefam"
            }
            if (!TelefamValidators.validateBio(state.bio)) {
                fieldErrors["bio"] = "Bio cannot exceed 150 characters"
            }

            if (fieldErrors.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        profileFieldErrors = fieldErrors,
                        errorMessage = fieldErrors.values.first()
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null, profileFieldErrors = emptyMap()) }
            val telemetry = repository.refreshTelemetrySnapshot(
                email = state.email,
                screenWidthDp = state.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )

            val userId = state.activeSession?.userId ?: UUID.randomUUID().toString()
            val email = state.activeSession?.email ?: state.email.ifBlank { "member@telefam.company" }

            // Note: state.sanitizedAvatar is optional; null is allowed
            val saveResult = repository.saveProfileSetup(
                userId = userId,
                email = email,
                fullName = state.fullName,
                username = state.username,
                phoneCountryCode = state.selectedCountry.dialCode,
                phoneIsoCode = state.selectedCountry.isoCode,
                phoneNumber = state.phoneNumber,
                gender = state.gender,
                dateOfBirth = state.dateOfBirth,
                bio = state.bio,
                sanitizedAvatar = state.sanitizedAvatar,
                telemetry = telemetry
            )

            saveResult.fold(
                onSuccess = { profileEntity ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            savedProfile = profileEntity,
                            ownerCooldownState = OwnerProfileCooldownState.compute(
                                lastFullNameChangedAtMs = profileEntity.lastFullNameChangedAtMs,
                                lastUsernameChangedAtMs = profileEntity.lastUsernameChangedAtMs
                            ),
                            activeSession = it.activeSession?.copy(hasCompletedProfile = true),
                            destination = AppDestination.MAIN_HOME,
                            selectedHomeTab = HomeNavTab.CHATS,
                            telemetry = telemetry
                        )
                    }
                    startOwnerProfileReactiveObserver()
                },
                onFailure = { err ->
                    if (err is UsernameConflictException) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isUsernameAvailable = false,
                                usernameSuggestions = err.suggestions,
                                profileFieldErrors = it.profileFieldErrors + ("username" to err.message),
                                errorMessage = err.message
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = err.message ?: "Could not save profile"
                            )
                        }
                    }
                }
            )
        }
    }

    fun selectHomeTab(tab: HomeNavTab) {
        _uiState.update {
            it.copy(
                selectedHomeTab = tab,
                errorMessage = null,
                ownerStatusBanner = null,
                isOwnerMenuOpen = false
            )
        }
        if (tab == HomeNavTab.CONTACTS) {
            startOwnerProfileReactiveObserver()
            refreshContactsDirectory(forceDailyRefresh = false)
        } else if (tab == HomeNavTab.PROFILE) {
            startOwnerProfileReactiveObserver()
            refreshOwnerProfileSnapshot(isPullToRefresh = false)
        }
    }

    // --- Owner Profile Reactive Observer, Pull-to-Refresh, Sub-Tabs & Editing ---

    private fun startOwnerProfileReactiveObserver() {
        val userId = currentAuthenticatedUserId()
        ownerReactiveObserverJob?.cancel()
        ownerReactiveObserverJob = viewModelScope.launch(safeExceptionHandler) {
            socialRepository.ensureOwnerMediaAndSubscribersInitialized(userId)
            combine(
                socialRepository.observeProfile(userId),
                socialRepository.observeFollowersCount(userId),
                socialRepository.observeFollowingCount(userId),
                socialRepository.observeSubscribersCount(userId),
                socialRepository.observeAllFollowEdges()
            ) { profile, followers, following, subscribers, _ ->
                Quadruple(profile, followers, following, subscribers)
            }.collectLatest { (profile, followers, following, subscribers) ->
                val currentSubTab = _uiState.value.ownerSelectedSubTab
                val snap = socialRepository.getOwnerProfileSnapshot(userId, currentSubTab)
                val contactsSnapshot = socialRepository.queryDiscoverableUsers(
                    currentUserId = userId,
                    searchQuery = _uiState.value.contactsSearchQuery,
                    filterMode = _uiState.value.contactsFilterMode,
                    page = _uiState.value.contactsCurrentPage.coerceAtLeast(1)
                )
                _uiState.update { prev ->
                    prev.copy(
                        savedProfile = profile ?: prev.savedProfile,
                        ownerFollowersCount = followers,
                        ownerFollowingCount = following,
                        ownerSubscribersCount = subscribers,
                        ownerPosts = snap.posts,
                        contactsUsers = contactsSnapshot.items,
                        contactsHasMorePages = contactsSnapshot.hasMorePages,
                        ownerCooldownState = OwnerProfileCooldownState.compute(
                            lastFullNameChangedAtMs = (profile ?: prev.savedProfile)?.lastFullNameChangedAtMs,
                            lastUsernameChangedAtMs = (profile ?: prev.savedProfile)?.lastUsernameChangedAtMs
                        )
                    )
                }
            }
        }
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    fun refreshOwnerProfileSnapshot(isPullToRefresh: Boolean = true) {
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            if (isPullToRefresh) {
                _uiState.update { it.copy(isOwnerProfileRefreshing = true, ownerStatusBanner = null) }
            }
            socialRepository.syncDirectoryIfNeeded(currentUserId = userId, forceRefresh = isPullToRefresh)
            val snap = socialRepository.getOwnerProfileSnapshot(
                currentUserId = userId,
                subTab = _uiState.value.ownerSelectedSubTab
            )
            val latestProfile = repository.dao.getProfileByUserId(userId)
            if (isPullToRefresh) {
                delay(280L)
            }
            _uiState.update { prev ->
                prev.copy(
                    savedProfile = latestProfile ?: prev.savedProfile,
                    ownerFollowersCount = snap.followersCount,
                    ownerFollowingCount = snap.followingCount,
                    ownerSubscribersCount = snap.subscribersCount,
                    ownerPosts = snap.posts,
                    ownerCooldownState = snap.cooldownState,
                    isOwnerProfileRefreshing = false,
                    isContactsOfflineBannerVisible = false
                )
            }
        }
    }

    fun selectOwnerProfileSubTab(subTab: OwnerProfileSubTab) {
        if (_uiState.value.ownerSelectedSubTab == subTab) return
        _uiState.update { it.copy(ownerSelectedSubTab = subTab) }
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val snap = socialRepository.getOwnerProfileSnapshot(userId, subTab)
            _uiState.update {
                it.copy(
                    ownerPosts = snap.posts,
                    ownerFollowersCount = snap.followersCount,
                    ownerFollowingCount = snap.followingCount,
                    ownerSubscribersCount = snap.subscribersCount
                )
            }
        }
    }

    fun openOwnerFollowList(listType: FollowListType) {
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val (items, hasMore) = socialRepository.getPaginatedFollowList(
                currentUserId = userId,
                profileUserId = userId,
                listType = listType,
                page = 1
            )
            _uiState.update {
                it.copy(
                    ownerFollowListType = listType,
                    ownerFollowListUsers = items,
                    ownerFollowListPage = 1,
                    ownerFollowListHasMore = hasMore
                )
            }
        }
    }

    fun loadMoreOwnerFollowList() {
        val state = _uiState.value
        val listType = state.ownerFollowListType ?: return
        if (!state.ownerFollowListHasMore) return
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val nextPage = state.ownerFollowListPage.coerceAtLeast(1) + 1
            val (items, hasMore) = socialRepository.getPaginatedFollowList(
                currentUserId = userId,
                profileUserId = userId,
                listType = listType,
                page = nextPage
            )
            _uiState.update {
                it.copy(
                    ownerFollowListUsers = items,
                    ownerFollowListPage = nextPage,
                    ownerFollowListHasMore = hasMore
                )
            }
        }
    }

    fun dismissOwnerFollowList() {
        _uiState.update {
            it.copy(
                ownerFollowListType = null,
                ownerFollowListUsers = emptyList(),
                ownerFollowListPage = 1,
                ownerFollowListHasMore = false
            )
        }
    }

    fun openOwnerEditProfileSheet() {
        val state = _uiState.value
        val profile = state.savedProfile
        val country = profile?.phoneIsoCode?.let { TelefamCountries.findByIso(it) } ?: state.selectedCountry
        val cooldown = OwnerProfileCooldownState.compute(
            lastFullNameChangedAtMs = profile?.lastFullNameChangedAtMs,
            lastUsernameChangedAtMs = profile?.lastUsernameChangedAtMs
        )
        _uiState.update {
            it.copy(
                isOwnerEditSheetOpen = true,
                fullName = profile?.fullName ?: it.fullName,
                username = profile?.username ?: it.username,
                isUsernameAvailable = true,
                usernameSuggestions = emptyList(),
                selectedCountry = country,
                phoneNumber = profile?.phoneNumber ?: it.phoneNumber,
                gender = profile?.gender ?: it.gender,
                dateOfBirth = profile?.dateOfBirth ?: it.dateOfBirth,
                bio = profile?.bio ?: it.bio,
                sanitizedAvatar = null,
                profileFieldErrors = emptyMap(),
                errorMessage = null,
                ownerCooldownState = cooldown
            )
        }
    }

    fun dismissOwnerEditProfileSheet() {
        _uiState.update {
            it.copy(
                isOwnerEditSheetOpen = false,
                pendingCropBitmap = null,
                profileFieldErrors = emptyMap(),
                errorMessage = null
            )
        }
    }

    fun saveOwnerProfileEdits(onSuccessBannerText: String) {
        lastRetryableSettingsAction = { saveOwnerProfileEdits(onSuccessBannerText) }
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val existing = state.savedProfile
            val cleanName = TelefamInputSanitizer.sanitizeFullName(state.fullName)
            val cleanUsername = TelefamInputSanitizer.sanitizeUsername(state.username)
            val cleanPhone = TelefamInputSanitizer.sanitizePhoneNumber(state.phoneNumber)
            val cleanBio = TelefamInputSanitizer.sanitizeBio(state.bio)
            val cleanGender = TelefamInputSanitizer.sanitizeGender(state.gender)
            val cleanDob = TelefamInputSanitizer.sanitizeDateOfBirth(state.dateOfBirth)

            val fieldErrors = mutableMapOf<String, String>()
            if (!TelefamValidators.validateFullName(cleanName)) {
                fieldErrors["fullName"] = "Full name must be at least 2 characters"
            }
            if (!TelefamValidators.validateUsername(cleanUsername)) {
                fieldErrors["username"] = "Username must be 3–24 characters (letters, numbers, _)"
            }
            if (!TelefamValidators.validatePhoneNumber(cleanPhone)) {
                fieldErrors["phone"] = "Enter a valid phone number (7–15 digits)"
            }
            if (cleanGender.isBlank()) {
                fieldErrors["gender"] = "Please select your gender"
            }
            if (cleanDob.isBlank()) {
                fieldErrors["dob"] = "Please select your date of birth"
            } else if (!TelefamValidators.validateAgeAtLeast13(cleanDob)) {
                fieldErrors["dob"] = "You must be at least 13 years old to use Telefam"
            }
            if (!TelefamValidators.validateBio(cleanBio)) {
                fieldErrors["bio"] = "Bio cannot exceed 150 characters"
            }

            val cooldown = OwnerProfileCooldownState.compute(
                lastFullNameChangedAtMs = existing?.lastFullNameChangedAtMs,
                lastUsernameChangedAtMs = existing?.lastUsernameChangedAtMs
            )
            if (existing != null && !existing.fullName.trim().equals(cleanName, ignoreCase = false) && !cooldown.canChangeFullName) {
                fieldErrors["fullName"] = "Full name can only be changed once every 7 days (${cooldown.fullNameRemainingDays}d remaining)."
            }
            if (existing != null && !existing.username.trim().equals(cleanUsername, ignoreCase = true) && !cooldown.canChangeUsername) {
                fieldErrors["username"] = "Username can only be changed once every 30 days (${cooldown.usernameRemainingDays}d remaining)."
            }

            if (fieldErrors.isNotEmpty()) {
                val firstErr = fieldErrors.values.first()
                _uiState.update {
                    it.copy(
                        profileFieldErrors = fieldErrors,
                        errorMessage = firstErr,
                        settingsErrorBanner = if (it.destination == AppDestination.SETTINGS_AND_PRIVACY) firstErr else it.settingsErrorBanner,
                        canRetrySettingsError = it.destination == AppDestination.SETTINGS_AND_PRIVACY
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null, settingsErrorBanner = null, profileFieldErrors = emptyMap()) }
            val telemetry = currentTelemetry()
            val userId = currentAuthenticatedUserId()
            val email = state.activeSession?.email ?: state.email.ifBlank { "member@telefam.company" }

            val saveResult = repository.saveProfileSetup(
                userId = userId,
                email = email,
                fullName = cleanName,
                username = cleanUsername,
                phoneCountryCode = state.selectedCountry.dialCode,
                phoneIsoCode = state.selectedCountry.isoCode,
                phoneNumber = cleanPhone,
                gender = cleanGender,
                dateOfBirth = cleanDob,
                bio = cleanBio,
                sanitizedAvatar = state.sanitizedAvatar,
                telemetry = telemetry,
                enforceEditCooldowns = true
            )

            saveResult.fold(
                onSuccess = { updatedEntity ->
                    val newCooldown = OwnerProfileCooldownState.compute(
                        lastFullNameChangedAtMs = updatedEntity.lastFullNameChangedAtMs,
                        lastUsernameChangedAtMs = updatedEntity.lastUsernameChangedAtMs
                    )
                    val qrBmp = settingsRepository.issueAndGenerateBackendProfileQrCode(
                        userId = userId,
                        username = updatedEntity.username,
                        fullName = updatedEntity.fullName
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOwnerEditSheetOpen = false,
                            savedProfile = updatedEntity,
                            profileQrBitmap = qrBmp,
                            ownerCooldownState = newCooldown,
                            ownerStatusBanner = onSuccessBannerText,
                            settingsStatusBanner = if (it.destination == AppDestination.SETTINGS_AND_PRIVACY) onSuccessBannerText else it.settingsStatusBanner,
                            settingsErrorBanner = null,
                            canRetrySettingsError = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    if (err is UsernameConflictException) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isUsernameAvailable = false,
                                usernameSuggestions = err.suggestions,
                                profileFieldErrors = it.profileFieldErrors + ("username" to err.message),
                                errorMessage = err.message,
                                settingsErrorBanner = if (it.destination == AppDestination.SETTINGS_AND_PRIVACY) err.message else it.settingsErrorBanner,
                                canRetrySettingsError = it.destination == AppDestination.SETTINGS_AND_PRIVACY
                            )
                        }
                    } else {
                        val msg = err.message ?: "Unable to update profile."
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = msg,
                                settingsErrorBanner = if (it.destination == AppDestination.SETTINGS_AND_PRIVACY) msg else it.settingsErrorBanner,
                                canRetrySettingsError = it.destination == AppDestination.SETTINGS_AND_PRIVACY
                            )
                        }
                    }
                }
            )
        }
    }

    fun clearOwnerStatusBanner() {
        _uiState.update { it.copy(ownerStatusBanner = null) }
    }

    // --- Contacts Discovery, Social Graph, Follow & Public Profile ---

    private fun currentAuthenticatedUserId(): String {
        val state = _uiState.value
        return state.activeSession?.userId ?: state.savedProfile?.userId ?: "local_telefam_member"
    }

    private suspend fun currentTelemetry(): DeviceAndNetworkTelemetry {
        val state = _uiState.value
        return state.telemetry ?: repository.refreshTelemetrySnapshot(
            email = state.email,
            screenWidthDp = state.screenWidthDp,
            locationTriple = currentLocationTriple,
            simulateNewDeviceOrLocation = false
        )
    }

    fun refreshContactsDirectory(forceDailyRefresh: Boolean = false) {
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val hasPerm = socialRepository.hasReadContactsPermission()
            _uiState.update {
                it.copy(
                    isContactsLoading = true,
                    hasContactsPermission = hasPerm,
                    contactsErrorMessage = null
                )
            }

            socialRepository.syncDirectoryIfNeeded(
                currentUserId = userId,
                forceRefresh = forceDailyRefresh
            )
            loadContactsPageInternal(page = 1)
        }
    }

    private suspend fun loadContactsPageInternal(page: Int) {
        val state = _uiState.value
        val userId = currentAuthenticatedUserId()
        val snapshot = socialRepository.queryDiscoverableUsers(
            currentUserId = userId,
            searchQuery = state.contactsSearchQuery,
            filterMode = state.contactsFilterMode,
            page = page
        )
        _uiState.update { prev ->
            prev.copy(
                contactsUsers = snapshot.items,
                contactsCurrentPage = snapshot.currentPage,
                contactsHasMorePages = snapshot.hasMorePages,
                isContactsLoading = false,
                isContactsOfflineBannerVisible = false,
                hasContactsPermission = snapshot.hasGrantedContactsPermission,
                contactsMatchCount = snapshot.syncedContactsHashesCount
            )
        }
    }

    fun loadMoreContactsPage() {
        val state = _uiState.value
        if (state.isContactsLoading || !state.contactsHasMorePages) return
        viewModelScope.launch(safeExceptionHandler) {
            _uiState.update { it.copy(isContactsLoading = true) }
            loadContactsPageInternal(page = state.contactsCurrentPage.coerceAtLeast(1) + 1)
        }
    }

    fun onContactsFilterModeSelected(mode: ContactsFilterMode) {
        if (_uiState.value.contactsFilterMode == mode) return
        _uiState.update { it.copy(contactsFilterMode = mode, contactsCurrentPage = 1) }
        viewModelScope.launch(safeExceptionHandler) {
            loadContactsPageInternal(page = 1)
        }
    }

    fun onToggleContactsSearch(active: Boolean) {
        _uiState.update {
            it.copy(
                isContactsSearchActive = active,
                contactsSearchQuery = if (!active) "" else it.contactsSearchQuery,
                contactsCurrentPage = 1
            )
        }
        viewModelScope.launch(safeExceptionHandler) {
            loadContactsPageInternal(page = 1)
        }
    }

    fun onContactsSearchQueryChanged(query: String) {
        _uiState.update { it.copy(contactsSearchQuery = query, contactsCurrentPage = 1) }
        viewModelScope.launch(safeExceptionHandler) {
            loadContactsPageInternal(page = 1)
        }
    }

    fun onContactsPermissionResult(granted: Boolean) {
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val telemetry = currentTelemetry()
            _uiState.update {
                it.copy(
                    hasContactsPermission = granted || socialRepository.hasReadContactsPermission(),
                    isContactsLoading = true,
                    contactsErrorMessage = null
                )
            }
            if (granted || socialRepository.hasReadContactsPermission()) {
                val uploadResult = socialRepository.syncDeviceContactsAndMergeSuggestions(
                    currentUserId = userId,
                    telemetry = telemetry
                )
                uploadResult.fold(
                    onSuccess = { syncedHashesCount ->
                        _uiState.update {
                            it.copy(contactsMatchCount = syncedHashesCount)
                        }
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(contactsErrorMessage = err.message)
                        }
                    }
                )
            }
            loadContactsPageInternal(page = 1)
        }
    }

    fun toggleFollowUser(targetUserId: String) {
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val telemetry = currentTelemetry()
            val result = socialRepository.toggleFollowUser(
                currentUserId = userId,
                targetUserId = targetUserId,
                telemetry = telemetry
            )
            result.fold(
                onSuccess = { newRelationState ->
                    loadContactsPageInternal(page = _uiState.value.contactsCurrentPage.coerceAtLeast(1))
                    val ownerListType = _uiState.value.ownerFollowListType
                    if (ownerListType != null) {
                        val (updatedOwnerItems, hasMoreOwner) = socialRepository.getPaginatedFollowList(
                            currentUserId = userId,
                            profileUserId = userId,
                            listType = ownerListType,
                            page = _uiState.value.ownerFollowListPage.coerceAtLeast(1)
                        )
                        _uiState.update {
                            it.copy(
                                ownerFollowListUsers = updatedOwnerItems,
                                ownerFollowListHasMore = hasMoreOwner
                            )
                        }
                    }
                    val activeProfile = _uiState.value.activePublicProfile
                    if (activeProfile != null) {
                        val refreshed = socialRepository.getPublicUserProfile(
                            currentUserId = userId,
                            targetUserId = activeProfile.userId,
                            postsFilter = _uiState.value.activePublicProfilePostsFilter
                        )
                        _uiState.update { state ->
                            val updatedFollowList = state.followListUsers.map { item ->
                                if (item.userId == targetUserId) {
                                    item.copy(followState = newRelationState)
                                } else {
                                    item
                                }
                            }
                            state.copy(
                                activePublicProfile = refreshed?.profile ?: state.activePublicProfile,
                                activePublicProfilePosts = refreshed?.posts ?: state.activePublicProfilePosts,
                                followListUsers = updatedFollowList,
                                contactsErrorMessage = null
                            )
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            contactsErrorMessage = err.message,
                            profileStatusBanner = err.message
                        )
                    }
                }
            )
        }
    }

    fun openPublicUserProfile(targetUserId: String) {
        viewModelScope.launch(safeExceptionHandler) {
            val currentUserId = currentAuthenticatedUserId()
            val filter = _uiState.value.activePublicProfilePostsFilter
            val details: PublicProfileSnapshot? = socialRepository.getPublicUserProfile(
                currentUserId = currentUserId,
                targetUserId = targetUserId,
                postsFilter = filter
            )
            if (details != null) {
                _uiState.update {
                    it.copy(
                        activePublicProfile = details.profile,
                        activePublicProfilePosts = details.posts,
                        activePublicProfileBlocked = details.profile.isBlocked,
                        isContactsOfflineBannerVisible = false,
                        activeFollowListType = null,
                        followListUsers = emptyList(),
                        profileStatusBanner = null,
                        destination = AppDestination.PUBLIC_USER_PROFILE
                    )
                }
            }
        }
    }

    fun onPublicProfilePostsFilterChanged(filter: PostsFilterOption) {
        val profile = _uiState.value.activePublicProfile ?: return
        _uiState.update { it.copy(activePublicProfilePostsFilter = filter) }
        viewModelScope.launch(safeExceptionHandler) {
            val currentUserId = currentAuthenticatedUserId()
            val refreshed = socialRepository.getPublicUserProfile(
                currentUserId = currentUserId,
                targetUserId = profile.userId,
                postsFilter = filter
            )
            if (refreshed != null) {
                _uiState.update {
                    it.copy(
                        activePublicProfile = refreshed.profile,
                        activePublicProfilePosts = refreshed.posts
                    )
                }
            }
        }
    }

    fun closePublicUserProfile() {
        _uiState.update {
            it.copy(
                destination = AppDestination.MAIN_HOME,
                activeFollowListType = null,
                profileStatusBanner = null
            )
        }
        viewModelScope.launch(safeExceptionHandler) {
            loadContactsPageInternal(page = _uiState.value.contactsCurrentPage.coerceAtLeast(1))
        }
    }

    fun openFollowConnectionsSheet(listType: FollowListType) {
        val profile = _uiState.value.activePublicProfile ?: return
        viewModelScope.launch(safeExceptionHandler) {
            val currentUserId = currentAuthenticatedUserId()
            val (items, hasMore) = socialRepository.getPaginatedFollowList(
                currentUserId = currentUserId,
                profileUserId = profile.userId,
                listType = listType,
                page = 1
            )
            _uiState.update {
                it.copy(
                    activeFollowListType = listType,
                    followListUsers = items,
                    followListPage = 1,
                    followListHasMore = hasMore
                )
            }
        }
    }

    fun loadMoreFollowConnections() {
        val state = _uiState.value
        val profile = state.activePublicProfile ?: return
        val listType = state.activeFollowListType ?: return
        if (!state.followListHasMore) return
        viewModelScope.launch(safeExceptionHandler) {
            val currentUserId = currentAuthenticatedUserId()
            val nextPage = state.followListPage.coerceAtLeast(1) + 1
            val (items, hasMore) = socialRepository.getPaginatedFollowList(
                currentUserId = currentUserId,
                profileUserId = profile.userId,
                listType = listType,
                page = nextPage
            )
            _uiState.update { prev ->
                prev.copy(
                    followListUsers = items,
                    followListPage = nextPage,
                    followListHasMore = hasMore
                )
            }
        }
    }

    fun dismissFollowConnectionsSheet() {
        _uiState.update {
            it.copy(
                activeFollowListType = null,
                followListUsers = emptyList(),
                followListPage = 1,
                followListHasMore = false
            )
        }
    }

    fun setBlockActiveProfileUser(block: Boolean) {
        val profile = _uiState.value.activePublicProfile ?: return
        viewModelScope.launch(safeExceptionHandler) {
            val currentUserId = currentAuthenticatedUserId()
            val telemetry = currentTelemetry()
            val result = socialRepository.setBlockUserState(
                currentUserId = currentUserId,
                targetUserId = profile.userId,
                block = block,
                telemetry = telemetry
            )
            result.onSuccess {
                val refreshed = socialRepository.getPublicUserProfile(
                    currentUserId = currentUserId,
                    targetUserId = profile.userId,
                    postsFilter = _uiState.value.activePublicProfilePostsFilter
                )
                _uiState.update {
                    it.copy(
                        activePublicProfileBlocked = block,
                        activePublicProfile = refreshed?.profile ?: it.activePublicProfile
                    )
                }
                loadContactsPageInternal(page = 1)
            }
        }
    }

    fun submitMultiQuestionReportForActiveProfile(
        reason: ReportReasonOption,
        location: ReportLocationOption,
        urgency: ReportUrgencyOption,
        additionalNotes: String
    ) {
        val profile = _uiState.value.activePublicProfile ?: return
        viewModelScope.launch(safeExceptionHandler) {
            val currentUserId = currentAuthenticatedUserId()
            val telemetry = currentTelemetry()
            val result = socialRepository.submitProfileReport(
                currentUserId = currentUserId,
                reportedUserId = profile.userId,
                reason = reason,
                location = location,
                urgency = urgency,
                additionalNotes = additionalNotes,
                telemetry = telemetry
            )
            result.onSuccess {
                val refreshed = socialRepository.getPublicUserProfile(
                    currentUserId = currentUserId,
                    targetUserId = profile.userId,
                    postsFilter = _uiState.value.activePublicProfilePostsFilter
                )
                _uiState.update {
                    it.copy(
                        activePublicProfile = refreshed?.profile ?: it.activePublicProfile,
                        activePublicProfileBlocked = refreshed?.profile?.isBlocked ?: it.activePublicProfileBlocked
                    )
                }
                loadContactsPageInternal(page = 1)
            }
        }
    }

    fun clearContactsError() {
        _uiState.update { it.copy(contactsErrorMessage = null, profileStatusBanner = null) }
    }

    private var privacySettingsReturnDestination: AppDestination = AppDestination.MAIN_HOME

    fun openPrivacySettings() {
        privacySettingsReturnDestination = AppDestination.MAIN_HOME
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val userId = state.activeSession?.userId ?: state.savedProfile?.userId ?: "local_telefam_member"
            val currentPrivacy = repository.getOrCreatePrivacySettings(userId)
            _uiState.update {
                it.copy(
                    privacySettings = currentPrivacy,
                    destination = AppDestination.PRIVACY_SETTINGS,
                    errorMessage = null
                )
            }
        }
    }

    fun openChatPrivacyAndAppearance() {
        privacySettingsReturnDestination = AppDestination.SETTINGS_AND_PRIVACY
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val userId = state.activeSession?.userId ?: state.savedProfile?.userId ?: "local_telefam_member"
            val currentPrivacy = repository.getOrCreatePrivacySettings(userId)
            _uiState.update {
                it.copy(
                    privacySettings = currentPrivacy,
                    destination = AppDestination.PRIVACY_SETTINGS,
                    errorMessage = null
                )
            }
        }
    }

    fun closePrivacySettings() {
        _uiState.update {
            it.copy(
                destination = privacySettingsReturnDestination,
                errorMessage = null
            )
        }
    }

    fun updatePrivacyRule(ruleKey: PrivacyRuleKey, audience: PrivacyAudience) {
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val userId = state.activeSession?.userId ?: state.savedProfile?.userId ?: "local_telefam_member"
            val telemetry = state.telemetry ?: repository.refreshTelemetrySnapshot(
                email = state.email,
                screenWidthDp = state.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )
            val updated = repository.updatePrivacyRuleOfflineFirst(
                userId = userId,
                ruleKey = ruleKey,
                audience = audience,
                telemetry = telemetry
            )
            _uiState.update { it.copy(privacySettings = updated) }
        }
    }

    fun updateChatBackgroundTheme(themeId: String) {
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val userId = state.activeSession?.userId ?: state.savedProfile?.userId ?: "local_telefam_member"
            val telemetry = state.telemetry ?: repository.refreshTelemetrySnapshot(
                email = state.email,
                screenWidthDp = state.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )
            val updated = repository.updateChatBackgroundThemeOfflineFirst(
                userId = userId,
                themeId = themeId,
                telemetry = telemetry
            )
            _uiState.update { it.copy(privacySettings = updated) }
        }
    }

    fun updateMessageBubbleColor(colorHex: String) {
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val userId = state.activeSession?.userId ?: state.savedProfile?.userId ?: "local_telefam_member"
            val telemetry = state.telemetry ?: repository.refreshTelemetrySnapshot(
                email = state.email,
                screenWidthDp = state.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )
            val updated = repository.updateMessageBubbleColorOfflineFirst(
                userId = userId,
                bubbleColorHex = colorHex,
                telemetry = telemetry
            )
            _uiState.update { it.copy(privacySettings = updated) }
        }
    }

    fun navigateBackFromProfileSetup() {
        val state = _uiState.value
        if (state.savedProfile != null || state.activeSession?.hasCompletedProfile == true) {
            _uiState.update { it.copy(destination = AppDestination.MAIN_HOME, errorMessage = null) }
        } else {
            signOut()
        }
    }

    fun signOut() {
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val telemetry = state.telemetry ?: repository.refreshTelemetrySnapshot(
                email = state.email,
                screenWidthDp = state.screenWidthDp,
                locationTriple = currentLocationTriple,
                simulateNewDeviceOrLocation = false
            )
            val userId = state.activeSession?.userId ?: state.savedProfile?.userId
            val email = state.activeSession?.email ?: state.email
            if (!userId.isNullOrBlank() && email.isNotBlank()) {
                settingsRepository.saveQuickLoginAccountBeforeLogout(
                    userId = userId,
                    email = email,
                    profile = state.savedProfile
                )
            }
            state.activeSession?.let { sess ->
                repository.signOutCurrentSession(sess.userId, sess.email, telemetry)
            }
            val quickAccounts = settingsRepository.getSavedQuickLoginAccounts()
            _uiState.update {
                it.copy(
                    destination = AppDestination.AUTH,
                    authMode = AuthMode.LOGIN,
                    activeSession = null,
                    savedProfile = null,
                    email = email,
                    password = "",
                    confirmPassword = "",
                    otpCode = "",
                    quickLoginAccounts = quickAccounts,
                    errorMessage = null,
                    infoBannerMessage = "Signed out safely. Your account is saved on this device for quick login."
                )
            }
        }
    }

    // --- Owner Profile Menu Screen ---

    fun openOwnerMenu() {
        _uiState.update { it.copy(isOwnerMenuOpen = true, ownerStatusBanner = null) }
    }

    fun closeOwnerMenu() {
        _uiState.update { it.copy(isOwnerMenuOpen = false) }
    }

    // --- Settings and Privacy System (All 13 Sections & Subscreens) ---

    private var settingsReactiveJob: Job? = null

    fun openSettingsAndPrivacy(initialSubScreen: SettingsSubScreen = SettingsSubScreen.ROOT) {
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val state = _uiState.value
            val email = state.activeSession?.email ?: state.email.ifBlank { "member@telefam.company" }
            val phone = state.savedProfile?.let { "${it.phoneCountryCode}${it.phoneNumber}" } ?: state.phoneNumber
            val telemetry = currentTelemetry()

            val accSettings = settingsRepository.getOrCreateAccountSettings(
                userId = userId,
                email = email,
                phone = phone,
                telemetry = telemetry
            )
            val handle = state.savedProfile?.username ?: state.username.ifBlank { "telefam_member" }
            val displayName = state.savedProfile?.fullName ?: state.fullName.ifBlank { "Telefam Member" }
            val qrBmp = settingsRepository.issueAndGenerateBackendProfileQrCode(
                userId = userId,
                username = handle,
                fullName = displayName
            )
            val blockedList = settingsRepository.getBlockedUsersDetailed(userId, state.blockedAccountsSearchQuery)

            _uiState.update {
                it.copy(
                    destination = AppDestination.SETTINGS_AND_PRIVACY,
                    settingsSubScreen = initialSubScreen,
                    settingsNavigationStack = if (initialSubScreen == SettingsSubScreen.ROOT) emptyList() else listOf(SettingsSubScreen.ROOT),
                    accountSettings = accSettings,
                    profileQrBitmap = qrBmp,
                    blockedAccountItems = blockedList,
                    settingsStatusBanner = null,
                    settingsErrorBanner = null,
                    canRetrySettingsError = false,
                    settingsOtpSentEmail = null,
                    settingsOtpResendCountdownSec = 0,
                    lastSubmittedTicketId = null
                )
            }
            startSettingsReactiveObserver(userId)
        }
    }

    private fun startSettingsReactiveObserver(userId: String) {
        settingsReactiveJob?.cancel()
        settingsReactiveJob = viewModelScope.launch(safeExceptionHandler) {
            combine(
                settingsRepository.observeAccountSettings(userId),
                settingsRepository.observeTrustedDevices(userId),
                settingsRepository.observeConnectedApps(userId),
                settingsRepository.observeSupportTickets(userId),
                repository.observeForensicLogs()
            ) { acc, devices, apps, tickets, logs ->
                SettingsFiveTuple(acc, devices, apps, tickets, logs)
            }.collectLatest { tuple ->
                val blocked = settingsRepository.getBlockedUsersDetailed(
                    currentUserId = userId,
                    searchQuery = _uiState.value.blockedAccountsSearchQuery
                )
                _uiState.update { prev ->
                    prev.copy(
                        accountSettings = tuple.acc ?: prev.accountSettings,
                        trustedDevices = tuple.devices,
                        connectedApps = tuple.apps,
                        supportTickets = tuple.tickets,
                        recentSecurityLogs = tuple.logs,
                        blockedAccountItems = blocked
                    )
                }
            }
        }
    }

    private data class SettingsFiveTuple(
        val acc: AccountSettingsAndSecurityEntity?,
        val devices: List<TrustedDeviceEntity>,
        val apps: List<ConnectedAppSessionEntity>,
        val tickets: List<ProblemSupportTicketEntity>,
        val logs: List<ForensicAuditLogEntity>
    )

    fun navigateToSettingsSubScreen(target: SettingsSubScreen) {
        _uiState.update { state ->
            val current = state.settingsSubScreen
            val nextStack = if (current == target) state.settingsNavigationStack else state.settingsNavigationStack + current
            state.copy(
                settingsSubScreen = target,
                settingsNavigationStack = nextStack,
                settingsStatusBanner = null,
                settingsErrorBanner = null,
                canRetrySettingsError = false,
                settingsOtpSentEmail = null,
                lastSubmittedTicketId = null
            )
        }
        if (target == SettingsSubScreen.BLOCKED_ACCOUNTS) {
            refreshBlockedAccountsInSettings()
        } else if (target == SettingsSubScreen.SHARE_PROFILE) {
            regenerateBackendProfileQrCode()
        }
    }

    fun regenerateBackendProfileQrCode() {
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val state = _uiState.value
            val handle = state.savedProfile?.username ?: state.username.ifBlank { "telefam_member" }
            val displayName = state.savedProfile?.fullName ?: state.fullName.ifBlank { "Telefam Member" }
            val qrBmp = settingsRepository.issueAndGenerateBackendProfileQrCode(
                userId = userId,
                username = handle,
                fullName = displayName
            )
            _uiState.update { it.copy(profileQrBitmap = qrBmp) }
        }
    }

    fun navigateBackInSettings() {
        val state = _uiState.value
        val stack = state.settingsNavigationStack
        if (stack.isNotEmpty()) {
            val previous = stack.last()
            _uiState.update {
                it.copy(
                    settingsSubScreen = previous,
                    settingsNavigationStack = stack.dropLast(1),
                    settingsStatusBanner = null,
                    settingsErrorBanner = null,
                    canRetrySettingsError = false
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    destination = AppDestination.MAIN_HOME,
                    settingsSubScreen = SettingsSubScreen.ROOT,
                    settingsNavigationStack = emptyList(),
                    settingsStatusBanner = null,
                    settingsErrorBanner = null,
                    canRetrySettingsError = false
                )
            }
        }
    }

    fun clearSettingsBanners() {
        _uiState.update { it.copy(settingsStatusBanner = null, settingsErrorBanner = null, canRetrySettingsError = false) }
    }

    fun retryLastFailedSettingsAction() {
        val retryFn = lastRetryableSettingsAction ?: return
        _uiState.update { it.copy(settingsErrorBanner = null, canRetrySettingsError = false) }
        retryFn.invoke()
    }

    private fun recordSettingsFailure(message: String, retryAction: (() -> Unit)? = null) {
        if (retryAction != null) {
            lastRetryableSettingsAction = retryAction
        }
        _uiState.update {
            it.copy(
                settingsErrorBanner = message,
                settingsStatusBanner = null,
                canRetrySettingsError = lastRetryableSettingsAction != null
            )
        }
    }

    fun updateAppThemeOption(option: AppThemeOption) {
        lastRetryableSettingsAction = { updateAppThemeOption(option) }
        // Apply immediately in UI state so the theme switches instantaneously without refresh
        _uiState.update { state ->
            state.copy(
                accountSettings = state.accountSettings.copy(appThemeMode = option.wireCode),
                settingsStatusBanner = "Appearance updated to ${option.label}.",
                settingsErrorBanner = null,
                canRetrySettingsError = false
            )
        }
        viewModelScope.launch(safeExceptionHandler) {
            val current = _uiState.value.accountSettings.copy(appThemeMode = option.wireCode)
            val saved = settingsRepository.saveAndSyncAccountSettings(
                updated = current,
                telemetry = currentTelemetry(),
                auditAction = "Appearance theme -> ${option.wireCode}"
            )
            _uiState.update { it.copy(accountSettings = saved) }
        }
    }

    fun openPrivacyRuleSelector(field: PrivacyControlField) {
        _uiState.update {
            it.copy(
                activePrivacyControlField = field,
                settingsSubScreen = SettingsSubScreen.PRIVACY_RULE_SELECTOR,
                settingsNavigationStack = it.settingsNavigationStack + it.settingsSubScreen,
                settingsStatusBanner = null,
                settingsErrorBanner = null
            )
        }
    }

    fun selectSocialPrivacyRule(field: PrivacyControlField, option: SocialPrivacyOption) {
        viewModelScope.launch(safeExceptionHandler) {
            val telemetry = currentTelemetry()
            val updated = settingsRepository.updateSocialPrivacyControl(
                current = _uiState.value.accountSettings,
                field = field,
                option = option,
                telemetry = telemetry
            )
            _uiState.update {
                it.copy(
                    accountSettings = updated,
                    settingsStatusBanner = "${field.title} updated to ${option.label}."
                )
            }
        }
    }

    fun togglePrivateAccountSetting(enabled: Boolean) {
        viewModelScope.launch(safeExceptionHandler) {
            val telemetry = currentTelemetry()
            val updated = settingsRepository.togglePrivateAccount(
                current = _uiState.value.accountSettings,
                enabled = enabled,
                telemetry = telemetry
            )
            _uiState.update {
                it.copy(
                    accountSettings = updated,
                    settingsStatusBanner = if (enabled) {
                        "Private account enabled. Only approved followers can view your restricted content."
                    } else {
                        "Public account enabled. Your profile and posts are visible according to your privacy rules."
                    }
                )
            }
        }
    }

    fun updateAccountSettingsTransform(
        confirmationMessage: String,
        auditAction: String,
        transform: (AccountSettingsAndSecurityEntity) -> AccountSettingsAndSecurityEntity
    ) {
        viewModelScope.launch(safeExceptionHandler) {
            val current = _uiState.value.accountSettings
            val transformed = transform(current)
            val saved = settingsRepository.saveAndSyncAccountSettings(
                updated = transformed,
                telemetry = currentTelemetry(),
                auditAction = auditAction
            )
            _uiState.update {
                it.copy(
                    accountSettings = saved,
                    settingsStatusBanner = confirmationMessage,
                    settingsErrorBanner = null
                )
            }
            // Refresh contacts if content languages/filtering changed
            loadContactsPageInternal(page = 1)
        }
    }

    fun openPermissionDetail(permissionType: AppPermissionType) {
        _uiState.update {
            it.copy(
                activePermissionType = permissionType,
                settingsSubScreen = SettingsSubScreen.SECURITY_PERMISSION_DETAIL,
                settingsNavigationStack = it.settingsNavigationStack + it.settingsSubScreen,
                settingsStatusBanner = null,
                settingsErrorBanner = null
            )
        }
    }

    fun onSystemPermissionUpdated(permissionType: AppPermissionType, granted: Boolean) {
        updateAccountSettingsTransform(
            confirmationMessage = "${permissionType.title} permission ${if (granted) "granted" else "updated"}.",
            auditAction = "Permission ${permissionType.name} -> $granted"
        ) { acc ->
            when (permissionType) {
                AppPermissionType.CONTACTS -> acc.copy(permissionContactsAllowed = granted)
                AppPermissionType.CAMERA -> acc.copy(permissionCameraAllowed = granted)
                AppPermissionType.MICROPHONE -> acc.copy(permissionMicrophoneAllowed = granted)
                AppPermissionType.PHOTOS -> acc.copy(permissionPhotosAllowed = granted)
            }
        }
        if (permissionType == AppPermissionType.CONTACTS && granted) {
            onContactsPermissionResult(true)
        }
    }

    fun sendSettingsVerificationOtp(purpose: String) {
        lastRetryableSettingsAction = { sendSettingsVerificationOtp(purpose) }
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val targetEmail = state.activeSession?.email ?: state.email.ifBlank { "member@telefam.company" }
            val telemetry = currentTelemetry()
            val result = settingsRepository.sendVerificationCodeForSettingsAction(
                originalEmail = targetEmail,
                purpose = purpose,
                telemetry = telemetry
            )
            val (_, otpRem) = repository.checkCooldownLocks(targetEmail, telemetry.ipSubnetFingerprint)
            result.fold(
                onSuccess = { countdownSec ->
                    _uiState.update {
                        it.copy(
                            settingsOtpSentEmail = targetEmail,
                            settingsOtpResendCountdownSec = countdownSec,
                            otpCooldownRemainingSec = otpRem,
                            settingsStatusBanner = "We sent a 6-digit verification code to $targetEmail. Please check your email and enter it below to confirm.",
                            settingsErrorBanner = null,
                            canRetrySettingsError = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(otpCooldownRemainingSec = otpRem) }
                    recordSettingsFailure(
                        message = err.message ?: "Could not send verification code.",
                        retryAction = { sendSettingsVerificationOtp(purpose) }
                    )
                }
            )
        }
    }

    fun confirmChangeAccountEmail(newEmail: String, enteredOtp: String) {
        lastRetryableSettingsAction = { confirmChangeAccountEmail(newEmail, enteredOtp) }
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val userId = currentAuthenticatedUserId()
            val originalEmail = state.activeSession?.email ?: state.email.ifBlank { "member@telefam.company" }
            val telemetry = currentTelemetry()
            val result = settingsRepository.verifyOtpAndChangeEmail(
                userId = userId,
                originalEmail = originalEmail,
                newEmail = newEmail,
                enteredOtp = enteredOtp,
                telemetry = telemetry
            )
            val (_, otpRem) = repository.checkCooldownLocks(originalEmail, telemetry.ipSubnetFingerprint)
            result.fold(
                onSuccess = { updatedEmail ->
                    _uiState.update {
                        it.copy(
                            email = updatedEmail,
                            activeSession = it.activeSession?.copy(email = updatedEmail),
                            settingsOtpSentEmail = null,
                            settingsOtpResendCountdownSec = 0,
                            otpCooldownRemainingSec = otpRem,
                            settingsStatusBanner = "Email address verified and updated to $updatedEmail.",
                            settingsErrorBanner = null,
                            canRetrySettingsError = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(otpCooldownRemainingSec = otpRem) }
                    recordSettingsFailure(
                        message = err.message ?: "Unable to update email address.",
                        retryAction = { confirmChangeAccountEmail(newEmail, enteredOtp) }
                    )
                }
            )
        }
    }

    fun submitAccountPasswordChange(
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String
    ) {
        lastRetryableSettingsAction = { submitAccountPasswordChange(currentPassword, newPassword, confirmNewPassword) }
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val userId = currentAuthenticatedUserId()
            val email = state.activeSession?.email ?: state.email
            val result = settingsRepository.changeAccountPassword(
                userId = userId,
                email = email,
                currentPassword = currentPassword,
                newPassword = newPassword,
                confirmNewPassword = confirmNewPassword,
                telemetry = currentTelemetry()
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            settingsStatusBanner = "Your password has been updated.",
                            settingsErrorBanner = null,
                            canRetrySettingsError = false
                        )
                    }
                },
                onFailure = { err ->
                    recordSettingsFailure(
                        message = err.message ?: "Unable to change password.",
                        retryAction = { submitAccountPasswordChange(currentPassword, newPassword, confirmNewPassword) }
                    )
                }
            )
        }
    }

    fun saveRecoveryContactsAndRegenerateCodes(recoveryEmail: String, recoveryPhone: String) {
        lastRetryableSettingsAction = { saveRecoveryContactsAndRegenerateCodes(recoveryEmail, recoveryPhone) }
        viewModelScope.launch(safeExceptionHandler) {
            val result = settingsRepository.regenerateRecoveryCodes(
                current = _uiState.value.accountSettings,
                recoveryEmail = recoveryEmail,
                recoveryPhone = recoveryPhone,
                telemetry = currentTelemetry()
            )
            result.fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            accountSettings = updated,
                            settingsStatusBanner = "Recovery settings saved and new backup recovery codes generated.",
                            settingsErrorBanner = null,
                            canRetrySettingsError = false
                        )
                    }
                },
                onFailure = { err ->
                    recordSettingsFailure(
                        message = err.message ?: "Unable to update recovery settings.",
                        retryAction = { saveRecoveryContactsAndRegenerateCodes(recoveryEmail, recoveryPhone) }
                    )
                }
            )
        }
    }

    fun enableTwoStepVerification(method: String, verificationCode: String) {
        lastRetryableSettingsAction = { enableTwoStepVerification(method, verificationCode) }
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val email = state.activeSession?.email ?: state.email.ifBlank { "member@telefam.company" }
            val telemetry = currentTelemetry()
            val result = settingsRepository.verifyAndEnableTwoStepVerification(
                current = state.accountSettings,
                email = email,
                method = method,
                enteredCode = verificationCode,
                telemetry = telemetry
            )
            val (_, otpRem) = repository.checkCooldownLocks(email, telemetry.ipSubnetFingerprint)
            result.fold(
                onSuccess = { (updated, _) ->
                    _uiState.update {
                        it.copy(
                            accountSettings = updated,
                            settingsOtpSentEmail = null,
                            settingsOtpResendCountdownSec = 0,
                            otpCooldownRemainingSec = otpRem,
                            settingsStatusBanner = "2-step verification ($method) is now active. Save your recovery codes below.",
                            settingsErrorBanner = null,
                            canRetrySettingsError = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(otpCooldownRemainingSec = otpRem) }
                    recordSettingsFailure(
                        message = err.message ?: "Verification failed.",
                        retryAction = { enableTwoStepVerification(method, verificationCode) }
                    )
                }
            )
        }
    }

    fun disableTwoStepVerification() {
        lastRetryableSettingsAction = { disableTwoStepVerification() }
        viewModelScope.launch(safeExceptionHandler) {
            val updated = settingsRepository.disableTwoStepVerification(
                current = _uiState.value.accountSettings,
                telemetry = currentTelemetry()
            )
            _uiState.update {
                it.copy(
                    accountSettings = updated,
                    settingsStatusBanner = "2-step verification has been turned off.",
                    settingsErrorBanner = null,
                    canRetrySettingsError = false
                )
            }
        }
    }

    fun registerPasskeyCredential(deviceLabel: String) {
        lastRetryableSettingsAction = { registerPasskeyCredential(deviceLabel) }
        viewModelScope.launch(safeExceptionHandler) {
            val updated = settingsRepository.registerFido2Passkey(
                current = _uiState.value.accountSettings,
                deviceLabel = deviceLabel,
                telemetry = currentTelemetry()
            )
            _uiState.update {
                it.copy(
                    accountSettings = updated,
                    settingsStatusBanner = "Your passkey is saved. You can now log in next time with your passkey.",
                    settingsErrorBanner = null,
                    canRetrySettingsError = false
                )
            }
        }
    }

    fun revokePasskeyCredential() {
        lastRetryableSettingsAction = { revokePasskeyCredential() }
        viewModelScope.launch(safeExceptionHandler) {
            val updated = settingsRepository.revokeFido2Passkey(
                current = _uiState.value.accountSettings,
                telemetry = currentTelemetry()
            )
            _uiState.update {
                it.copy(
                    accountSettings = updated,
                    settingsStatusBanner = "Passkey removed from your account.",
                    settingsErrorBanner = null,
                    canRetrySettingsError = false
                )
            }
        }
    }

    fun logoutDeviceSession(deviceRecordId: String) {
        lastRetryableSettingsAction = { logoutDeviceSession(deviceRecordId) }
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            settingsRepository.logoutRecognizedDevice(
                userId = userId,
                deviceRecordId = deviceRecordId,
                telemetry = currentTelemetry()
            )
            _uiState.update {
                it.copy(
                    settingsStatusBanner = "Device session logged out and revoked.",
                    settingsErrorBanner = null,
                    canRetrySettingsError = false
                )
            }
        }
    }

    fun revokeConnectedApp(connectionId: String) {
        lastRetryableSettingsAction = { revokeConnectedApp(connectionId) }
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            settingsRepository.revokeConnectedAppSession(
                userId = userId,
                connectionId = connectionId,
                telemetry = currentTelemetry()
            )
            _uiState.update {
                it.copy(
                    settingsStatusBanner = "Connected app access revoked.",
                    settingsErrorBanner = null,
                    canRetrySettingsError = false
                )
            }
        }
    }

    fun confirmDeactivateAccount(reason: String, autoReenableDays: Int, enteredOtp: String) {
        lastRetryableSettingsAction = { confirmDeactivateAccount(reason, autoReenableDays, enteredOtp) }
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val email = state.activeSession?.email ?: state.email.ifBlank { "member@telefam.company" }
            val telemetry = currentTelemetry()
            val result = settingsRepository.verifyOtpAndDeactivateAccount(
                current = state.accountSettings,
                profile = state.savedProfile,
                email = email,
                reason = reason,
                autoReenableDays = autoReenableDays,
                enteredOtp = enteredOtp,
                telemetry = telemetry
            )
            result.fold(
                onSuccess = {
                    state.activeSession?.let { sess ->
                        repository.signOutCurrentSession(sess.userId, sess.email, telemetry)
                    }
                    val quickAccounts = settingsRepository.getSavedQuickLoginAccounts()
                    val autoMsg = if (autoReenableDays > 0) {
                        "Your account is deactivated and scheduled to auto-enable in $autoReenableDays day(s), or whenever you sign in."
                    } else {
                        "Your account is deactivated. Sign in anytime to reactivate your account."
                    }
                    _uiState.update {
                        it.copy(
                            destination = AppDestination.AUTH,
                            authMode = AuthMode.LOGIN,
                            activeSession = null,
                            savedProfile = null,
                            password = "",
                            otpCode = "",
                            quickLoginAccounts = quickAccounts,
                            infoBannerMessage = autoMsg,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    val (_, otpRem) = repository.checkCooldownLocks(email, telemetry.ipSubnetFingerprint)
                    _uiState.update { it.copy(otpCooldownRemainingSec = otpRem) }
                    recordSettingsFailure(
                        message = err.message ?: "Could not deactivate account.",
                        retryAction = { confirmDeactivateAccount(reason, autoReenableDays, enteredOtp) }
                    )
                }
            )
        }
    }

    fun confirmSchedule30DayAccountDeletion(reason: String, enteredOtp: String) {
        lastRetryableSettingsAction = { confirmSchedule30DayAccountDeletion(reason, enteredOtp) }
        viewModelScope.launch(safeExceptionHandler) {
            val state = _uiState.value
            val email = state.activeSession?.email ?: state.email.ifBlank { "member@telefam.company" }
            val telemetry = currentTelemetry()
            val result = settingsRepository.verifyOtpAndSchedule30DayAccountDeletion(
                current = state.accountSettings,
                profile = state.savedProfile,
                email = email,
                reason = reason,
                enteredOtp = enteredOtp,
                telemetry = telemetry
            )
            result.fold(
                onSuccess = {
                    state.activeSession?.let { sess ->
                        repository.signOutCurrentSession(sess.userId, sess.email, telemetry)
                    }
                    val quickAccounts = settingsRepository.getSavedQuickLoginAccounts()
                    _uiState.update {
                        it.copy(
                            destination = AppDestination.AUTH,
                            authMode = AuthMode.LOGIN,
                            activeSession = null,
                            savedProfile = null,
                            password = "",
                            otpCode = "",
                            quickLoginAccounts = quickAccounts,
                            infoBannerMessage = "Account scheduled for permanent deletion in 30 days. Log back in within 30 days to cancel deletion and recover your account.",
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    val (_, otpRem) = repository.checkCooldownLocks(email, telemetry.ipSubnetFingerprint)
                    _uiState.update { it.copy(otpCooldownRemainingSec = otpRem) }
                    recordSettingsFailure(
                        message = err.message ?: "Could not schedule account deletion.",
                        retryAction = { confirmSchedule30DayAccountDeletion(reason, enteredOtp) }
                    )
                }
            )
        }
    }

    fun downloadProfileQrCode() {
        lastRetryableSettingsAction = { downloadProfileQrCode() }
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val state = _uiState.value
            val handle = state.savedProfile?.username ?: state.username.ifBlank { "telefam_member" }
            val fullName = state.savedProfile?.fullName ?: state.fullName.ifBlank { "Telefam Member" }
            val res = settingsRepository.saveQrCodeToDeviceDownloads(
                userId = userId,
                username = handle,
                fullName = fullName
            )
            res.fold(
                onSuccess = { msg ->
                    _uiState.update {
                        it.copy(
                            settingsStatusBanner = msg,
                            settingsErrorBanner = null,
                            canRetrySettingsError = false
                        )
                    }
                },
                onFailure = { err ->
                    recordSettingsFailure(
                        message = err.message ?: "Unable to save QR code.",
                        retryAction = { downloadProfileQrCode() }
                    )
                }
            )
        }
    }

    fun onBlockedAccountsSearchQueryChanged(query: String) {
        _uiState.update { it.copy(blockedAccountsSearchQuery = query) }
        refreshBlockedAccountsInSettings()
    }

    fun refreshBlockedAccountsInSettings() {
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val list = settingsRepository.getBlockedUsersDetailed(
                currentUserId = userId,
                searchQuery = _uiState.value.blockedAccountsSearchQuery
            )
            _uiState.update { it.copy(blockedAccountItems = list) }
        }
    }

    fun unblockAccountFromSettings(blockedUserId: String, blockedUsername: String) {
        lastRetryableSettingsAction = { unblockAccountFromSettings(blockedUserId, blockedUsername) }
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val result = socialRepository.setBlockUserState(
                currentUserId = userId,
                targetUserId = blockedUserId,
                block = false,
                telemetry = currentTelemetry()
            )
            result.fold(
                onSuccess = {
                    val updatedList = settingsRepository.getBlockedUsersDetailed(
                        currentUserId = userId,
                        searchQuery = _uiState.value.blockedAccountsSearchQuery
                    )
                    _uiState.update {
                        it.copy(
                            blockedAccountItems = updatedList,
                            settingsStatusBanner = "@$blockedUsername has been unblocked.",
                            settingsErrorBanner = null,
                            canRetrySettingsError = false
                        )
                    }
                    loadContactsPageInternal(page = 1)
                },
                onFailure = { err ->
                    recordSettingsFailure(
                        message = err.message ?: "Unable to unblock user.",
                        retryAction = { unblockAccountFromSettings(blockedUserId, blockedUsername) }
                    )
                }
            )
        }
    }

    fun submitProblemReportOrSupportRequest(
        ticketType: String,
        category: String,
        subject: String,
        description: String,
        screenshotUri: Uri?
    ) {
        lastRetryableSettingsAction = {
            submitProblemReportOrSupportRequest(ticketType, category, subject, description, screenshotUri)
        }
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val result = settingsRepository.submitProblemOrSupportTicket(
                ownerUserId = userId,
                ticketType = ticketType,
                category = category,
                subject = subject,
                description = description,
                screenshotUri = screenshotUri,
                telemetry = currentTelemetry()
            )
            result.fold(
                onSuccess = { ticket ->
                    _uiState.update {
                        it.copy(
                            lastSubmittedTicketId = ticket.ticketId,
                            settingsStatusBanner = "Submitted (${ticket.ticketId}). We'll review your report and contact you if more information is needed.",
                            settingsErrorBanner = null,
                            canRetrySettingsError = false
                        )
                    }
                },
                onFailure = { err ->
                    recordSettingsFailure(
                        message = err.message ?: "Could not submit report.",
                        retryAction = {
                            submitProblemReportOrSupportRequest(ticketType, category, subject, description, screenshotUri)
                        }
                    )
                }
            )
        }
    }

    fun submitSupportTicketFollowUp(ticketId: String, followUpText: String, markResolved: Boolean) {
        lastRetryableSettingsAction = { submitSupportTicketFollowUp(ticketId, followUpText, markResolved) }
        viewModelScope.launch(safeExceptionHandler) {
            val userId = currentAuthenticatedUserId()
            val result = settingsRepository.updateSupportTicketFollowUpOrStatus(
                ownerUserId = userId,
                ticketId = ticketId,
                followUpReply = followUpText,
                markResolved = markResolved,
                telemetry = currentTelemetry()
            )
            result.fold(
                onSuccess = { updatedTicket ->
                    val statusMsg = if (markResolved) {
                        "Support request $ticketId marked as resolved."
                    } else {
                        "Follow-up message added to support request ${updatedTicket.ticketId}."
                    }
                    _uiState.update {
                        it.copy(
                            settingsStatusBanner = statusMsg,
                            settingsErrorBanner = null,
                            canRetrySettingsError = false
                        )
                    }
                },
                onFailure = { err ->
                    recordSettingsFailure(
                        message = err.message ?: "Unable to update support request.",
                        retryAction = { submitSupportTicketFollowUp(ticketId, followUpText, markResolved) }
                    )
                }
            )
        }
    }

    fun acknowledgeLegalDocument(doc: LegalPolicyDocument) {
        lastRetryableSettingsAction = { acknowledgeLegalDocument(doc) }
        viewModelScope.launch(safeExceptionHandler) {
            val updated = settingsRepository.acknowledgeLegalPolicy(
                current = _uiState.value.accountSettings,
                policyId = doc.id,
                policyTitle = doc.title,
                policyVersion = doc.version,
                telemetry = currentTelemetry()
            )
            _uiState.update {
                it.copy(
                    accountSettings = updated,
                    settingsStatusBanner = "${doc.title} (${doc.version}) acknowledged and saved to your account.",
                    settingsErrorBanner = null,
                    canRetrySettingsError = false
                )
            }
        }
    }

    fun downloadLegalDocument(doc: LegalPolicyDocument) {
        lastRetryableSettingsAction = { downloadLegalDocument(doc) }
        viewModelScope.launch(safeExceptionHandler) {
            val result = settingsRepository.downloadLegalDocumentCopy(
                title = doc.title,
                version = doc.version,
                effectiveDate = doc.effectiveDate,
                sections = doc.sections
            )
            result.fold(
                onSuccess = { msg ->
                    _uiState.update {
                        it.copy(
                            settingsStatusBanner = msg,
                            settingsErrorBanner = null,
                            canRetrySettingsError = false
                        )
                    }
                },
                onFailure = { err ->
                    recordSettingsFailure(
                        message = err.message ?: "Unable to download policy document.",
                        retryAction = { downloadLegalDocument(doc) }
                    )
                }
            )
        }
    }

    fun submitHelpArticleFeedback(articleId: String, helpful: Boolean) {
        lastRetryableSettingsAction = { submitHelpArticleFeedback(articleId, helpful) }
        viewModelScope.launch(safeExceptionHandler) {
            val updated = settingsRepository.recordHelpArticleFeedback(
                current = _uiState.value.accountSettings,
                articleId = articleId,
                helpful = helpful,
                telemetry = currentTelemetry()
            )
            _uiState.update {
                it.copy(
                    accountSettings = updated,
                    settingsStatusBanner = if (helpful) {
                        "Thank you for your feedback. We're glad this article helped."
                    } else {
                        "Thank you for your feedback. You can submit a direct support request below if you need further assistance."
                    },
                    settingsErrorBanner = null,
                    canRetrySettingsError = false
                )
            }
        }
    }

    fun clearSubmittedTicketConfirmation() {
        _uiState.update { it.copy(lastSubmittedTicketId = null) }
    }

    fun openSafetyGuideDetail(guide: SafetyTopicGuide) {
        _uiState.update {
            it.copy(
                activeSafetyGuide = guide,
                settingsSubScreen = SettingsSubScreen.SAFETY_TOPIC_DETAIL,
                settingsNavigationStack = it.settingsNavigationStack + it.settingsSubScreen,
                settingsStatusBanner = null,
                settingsErrorBanner = null,
                canRetrySettingsError = false
            )
        }
    }

    fun onHelpSearchQueryChanged(query: String) {
        _uiState.update { it.copy(helpSearchQuery = query) }
    }

    fun openHelpArticleDetail(article: HelpSupportArticle) {
        _uiState.update {
            it.copy(
                activeHelpArticle = article,
                settingsSubScreen = SettingsSubScreen.HELP_ARTICLE_DETAIL,
                settingsNavigationStack = it.settingsNavigationStack + it.settingsSubScreen,
                settingsStatusBanner = null,
                settingsErrorBanner = null,
                canRetrySettingsError = false
            )
        }
    }

    fun openLegalDocumentDetail(doc: LegalPolicyDocument) {
        _uiState.update {
            it.copy(
                activeLegalDocument = doc,
                settingsSubScreen = SettingsSubScreen.LEGAL_DOCUMENT_DETAIL,
                settingsNavigationStack = it.settingsNavigationStack + it.settingsSubScreen,
                settingsStatusBanner = null,
                settingsErrorBanner = null,
                canRetrySettingsError = false
            )
        }
    }

    fun openAboutSectionDetail(detailKey: String) {
        _uiState.update {
            it.copy(
                activeAboutDetailKey = detailKey,
                settingsSubScreen = SettingsSubScreen.ABOUT_DETAIL,
                settingsNavigationStack = it.settingsNavigationStack + it.settingsSubScreen,
                settingsStatusBanner = null,
                settingsErrorBanner = null,
                canRetrySettingsError = false
            )
        }
    }
}
