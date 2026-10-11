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
import com.telefam.company.app.data.ActiveVoiceBubblePlaybackState
import com.telefam.company.app.data.ActiveVoiceRecordingState
import com.telefam.company.app.data.ForwardRecipientTarget
import com.telefam.company.app.data.TelefamChatRepository
import com.telefam.company.app.domain.ChatAttachmentItem
import com.telefam.company.app.domain.ChatDisappearingOption
import com.telefam.company.app.domain.ChatMessageItem
import com.telefam.company.app.domain.ChatMessageType
import com.telefam.company.app.domain.ChatReactionChoice
import com.telefam.company.app.domain.E2eeVerificationSnapshot
import com.telefam.company.app.domain.KlipyMediaItem
import com.telefam.company.app.domain.KlipyMediaTab
import com.telefam.company.app.domain.OneToOneChatListItem
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
import com.telefam.company.app.notifications.TelefamDeepLinkTargetType
import com.telefam.company.app.notifications.TelefamNotificationDeepLink
import com.telefam.company.app.notifications.TelefamNotificationEngine
import com.telefam.company.app.security.AndroidSandboxMediaProcessor
import com.telefam.company.app.security.TelefamCryptoAndTelemetry
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    PUBLIC_USER_PROFILE,
    ONE_TO_ONE_CHAT,
    E2EE_VERIFICATION
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
    val quickLoginAccounts: List<QuickLoginAccountEntity> = emptyList(),

    // 1:1 Individual Chats (Offline-First Room Source of Truth + Signal E2EE + Klipy + Voice Notes)
    val chatListItems: List<OneToOneChatListItem> = emptyList(),
    val chatListSearchQuery: String = "",
    val isChatListSearchActive: Boolean = false,
    val activeChatId: String? = null,
    val activeChatItem: OneToOneChatListItem? = null,
    val activeChatMessages: List<ChatMessageItem> = emptyList(),
    val activeChatAttachments: List<ChatAttachmentItem> = emptyList(),
    val chatInputText: String = "",
    val replyingToMessage: ChatMessageItem? = null,
    val editingMessage: ChatMessageItem? = null,
    val selectedMessageIds: Set<String> = emptySet(),
    val isSelectModeActive: Boolean = false,
    val activeKlipyTab: KlipyMediaTab = KlipyMediaTab.STICKERS,
    val klipySearchQuery: String = "",
    val klipyRecentItems: List<KlipyMediaItem> = emptyList(),
    val previewKlipyItem: KlipyMediaItem? = null,
    val activeE2eeVerification: E2eeVerificationSnapshot? = null,
    val forwardCandidates: List<ForwardRecipientTarget> = emptyList(),
    val forwardSearchQuery: String = "",
    val selectedForwardUserIds: Set<String> = emptySet(),
    val messagesPendingForward: List<ChatMessageItem> = emptyList(),
    val isForwardPickerOpen: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
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
    val chatRepository = TelefamChatRepository(
        context = application.applicationContext,
        dao = repository.dao
    )

    private val _uiState = MutableStateFlow(TelefamUiState())
    val uiState: StateFlow<TelefamUiState> = _uiState.asStateFlow()

    val voiceRecordingState: StateFlow<ActiveVoiceRecordingState> = chatRepository.voiceRecordingState
    val bubblePlaybackState: StateFlow<ActiveVoiceBubblePlaybackState> = chatRepository.bubblePlaybackState

    val klipyPagedFlow: Flow<PagingData<KlipyMediaItem>> = _uiState
        .map { state ->
            Triple(
                state.activeSession?.userId ?: state.savedProfile?.userId ?: "local_telefam_member",
                state.activeKlipyTab,
                state.klipySearchQuery
            )
        }
        .distinctUntilChanged()
        .flatMapLatest { (userId, tab, query) ->
            chatRepository.klipyProvider.observePagedMedia(
                ownerUserId = userId,
                tab = tab,
                searchQuery = query
            )
        }
        .cachedIn(viewModelScope)

    private var currentLocationTriple: Triple<String, Double?, Double?> =
        Triple("Kenya • Africa/Nairobi", -1.2921, 36.8219)
    private var timerJob: Job? = null
    private var usernameCheckJob: Job? = null
    private var ownerReactiveObserverJob: Job? = null
    private var chatListReactiveObserverJob: Job? = null
    private var activeChatObserverJob: Job? = null
    private var klipyRecentsObserverJob: Job? = null
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
        startOneToOneChatsReactiveObserver(userId)
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
                TelefamNotificationEngine.revokeFcmRegistrationToken(getApplication<Application>().applicationContext, sess.userId, "USER_LOGOUT")
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

    // =========================================================================
    // 1:1 INDIVIDUAL CHATS (OFFLINE-FIRST ROOM SOURCE OF TRUTH + SIGNAL E2EE)
    // =========================================================================

    private fun startOneToOneChatsReactiveObserver(userId: String) {
        if (userId.isBlank()) return
        chatRepository.ensureRealtimeConnectionAndPreKeys(userId)

        chatListReactiveObserverJob?.cancel()
        chatListReactiveObserverJob = viewModelScope.launch(safeExceptionHandler) {
            chatRepository.observeOneToOneChatList(userId).collectLatest { chats ->
                _uiState.update { prev ->
                    val currentActiveId = prev.activeChatId
                    val refreshedActiveItem = if (currentActiveId != null) {
                        chats.find { it.chatId == currentActiveId } ?: prev.activeChatItem
                    } else {
                        null
                    }
                    prev.copy(
                        chatListItems = chats,
                        activeChatItem = refreshedActiveItem
                    )
                }
            }
        }

        observeKlipyRecentsForActiveTab(userId, _uiState.value.activeKlipyTab)
    }

    private fun observeKlipyRecentsForActiveTab(userId: String, tab: KlipyMediaTab) {
        klipyRecentsObserverJob?.cancel()
        klipyRecentsObserverJob = viewModelScope.launch(safeExceptionHandler) {
            chatRepository.klipyProvider.observeRecents(userId, tab).collectLatest { recents ->
                _uiState.update { it.copy(klipyRecentItems = recents) }
            }
        }
    }

    fun onToggleChatListSearch(active: Boolean) {
        _uiState.update {
            it.copy(
                isChatListSearchActive = active,
                chatListSearchQuery = if (!active) "" else it.chatListSearchQuery
            )
        }
    }

    fun onChatListSearchQueryChanged(query: String) {
        _uiState.update { it.copy(chatListSearchQuery = query) }
    }

    /**
     * "Start chat" button on the Main Chats Screen opens the existing CONTACTS screen
     * so the user can open a profile or tap message to open the 1:1 inbox.
     */
    fun onStartChatFromChatsTab() {
        _uiState.update {
            it.copy(
                destination = AppDestination.MAIN_HOME,
                selectedHomeTab = HomeNavTab.CONTACTS,
                isOwnerMenuOpen = false
            )
        }
        refreshContactsDirectory(forceDailyRefresh = false)
    }

    /**
     * Opens or creates a real 1:1 chat with the active Public User Profile when the user taps "Message".
     */
    fun openChatWithActivePublicProfile() {
        val profile = _uiState.value.activePublicProfile ?: return
        openOneToOneChatWithDiscoverableUser(profile)
    }

    /**
     * Opens or creates a real 1:1 chat with a contact/user from Contacts or Public Profile.
     */
    fun openOneToOneChatWithDiscoverableUser(user: DiscoverableUserItem) {
        viewModelScope.launch(safeExceptionHandler) {
            val ownerId = currentAuthenticatedUserId()
            val chatId = chatRepository.openOrCreateOneToOneChat(
                ownerUserId = ownerId,
                peerUserId = user.userId,
                peerFullName = user.fullName,
                peerUsername = user.username,
                peerAvatarUrl = user.avatarCdnUrl,
                peerBio = user.bio,
                peerIsOnline = true,
                peerLastSeenMs = System.currentTimeMillis(),
                peerIsDeleted = false
            )
            openOneToOneChatById(chatId)
        }
    }

    /**
     * Opens an existing 1:1 chat from the Main Chats list by `chatId`.
     * Clears unread count, loads draft from Room, and observes messages/attachments from Room.
     */
    fun openOneToOneChatById(chatId: String) {
        TelefamNotificationEngine.setActiveOpenChat(getApplication<Application>().applicationContext, chatId)
        viewModelScope.launch(safeExceptionHandler) {
            val ownerId = currentAuthenticatedUserId()
            val draftEntity = repository.dao.getDraftForChat(chatId)
            val chatEntity = repository.dao.getChatById(chatId)
            if (chatEntity != null) {
                chatRepository.markChatMessagesSeen(ownerId, chatId, chatEntity.peerUserId)
            }

            _uiState.update { prev ->
                val item = prev.chatListItems.find { it.chatId == chatId } ?: chatEntity?.let { ce ->
                    OneToOneChatListItem(
                        chatId = ce.chatId,
                        ownerUserId = ce.ownerUserId,
                        peerUserId = ce.peerUserId,
                        peerFullName = if (ce.peerIsDeleted) "User not found" else ce.peerFullName,
                        peerUsername = ce.peerUsername,
                        peerAvatarUrl = if (ce.peerIsDeleted) null else ce.peerAvatarUrl,
                        peerBio = ce.peerBio,
                        peerIsOnline = ce.peerIsOnline,
                        peerLastSeenMs = ce.peerLastSeenMs,
                        peerIsTyping = ce.peerIsTyping,
                        peerIsDeleted = ce.peerIsDeleted,
                        isBlockedByMe = false,
                        isBlockedByPeer = false,
                        lastMessageId = ce.lastMessageId,
                        lastMessageType = runCatching { ChatMessageType.valueOf(ce.lastMessageType) }.getOrDefault(ChatMessageType.TEXT),
                        lastMessagePreview = ce.lastMessagePreview,
                        lastMessageSenderId = ce.lastMessageSenderId,
                        lastMessageDeliveryStatus = com.telefam.company.app.domain.ChatMessageDeliveryStatus.SENT,
                        lastMessageMediaIconUrl = ce.lastMessageMediaIconUrl,
                        lastMessageTimestampMs = ce.lastMessageTimestampMs,
                        unreadCount = 0,
                        disappearingOption = ChatDisappearingOption.fromSeconds(ce.disappearingDurationSeconds),
                        e2eeSafetyNumberVerified = ce.e2eeSafetyNumberVerified,
                        draftText = draftEntity?.draftText.orEmpty()
                    )
                }
                prev.copy(
                    destination = AppDestination.ONE_TO_ONE_CHAT,
                    activeChatId = chatId,
                    activeChatItem = item,
                    chatInputText = draftEntity?.draftText.orEmpty(),
                    replyingToMessage = null,
                    editingMessage = null,
                    selectedMessageIds = emptySet(),
                    isSelectModeActive = false,
                    previewKlipyItem = null
                )
            }

            activeChatObserverJob?.cancel()
            activeChatObserverJob = viewModelScope.launch(safeExceptionHandler) {
                chatRepository.observeMessagesForChat(ownerId, chatId).collectLatest { messages ->
                    val attachments = chatRepository.extractAttachmentsFromMessages(messages)
                    val draftReplyId = repository.dao.getDraftForChat(chatId)?.replyToMessageId
                    _uiState.update { prev ->
                        val resolvedReply = prev.replyingToMessage
                            ?: draftReplyId?.let { rId -> messages.find { it.messageId == rId } }
                        prev.copy(
                            activeChatMessages = messages,
                            activeChatAttachments = attachments,
                            replyingToMessage = resolvedReply
                        )
                    }
                }
            }
        }
    }

    fun closeActiveOneToOneChat() {
        TelefamNotificationEngine.setActiveOpenChat(getApplication<Application>().applicationContext, null)
        chatRepository.stopBubbleVoicePlayback()
        chatRepository.stopVoicePreviewPlayback()
        activeChatObserverJob?.cancel()
        _uiState.update {
            it.copy(
                destination = AppDestination.MAIN_HOME,
                selectedHomeTab = HomeNavTab.CHATS,
                activeChatId = null,
                activeChatItem = null,
                activeChatMessages = emptyList(),
                activeChatAttachments = emptyList(),
                replyingToMessage = null,
                editingMessage = null,
                selectedMessageIds = emptySet(),
                isSelectModeActive = false,
                previewKlipyItem = null
            )
        }
    }

    fun onChatInputTextChanged(newText: String) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        _uiState.update { it.copy(chatInputText = newText) }
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.updateDraftAndTyping(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId,
                draftText = newText,
                replyToMessageId = _uiState.value.replyingToMessage?.messageId
            )
        }
    }

    fun onSwipeOrSelectReplyMessage(message: ChatMessageItem) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        _uiState.update {
            it.copy(
                replyingToMessage = message,
                editingMessage = null
            )
        }
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.updateDraftAndTyping(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId,
                draftText = _uiState.value.chatInputText,
                replyToMessageId = message.messageId
            )
        }
    }

    fun onStartEditingMessage(message: ChatMessageItem) {
        val ownerId = currentAuthenticatedUserId()
        if (message.senderUserId != ownerId || message.messageType != ChatMessageType.TEXT) return
        _uiState.update {
            it.copy(
                editingMessage = message,
                replyingToMessage = null,
                chatInputText = message.decryptedText
            )
        }
    }

    fun onCancelReplyOrEditBanner() {
        val state = _uiState.value
        val wasEditing = state.editingMessage != null
        val chatId = state.activeChatId
        val peerId = state.activeChatItem?.peerUserId
        val ownerId = currentAuthenticatedUserId()
        _uiState.update {
            it.copy(
                replyingToMessage = null,
                editingMessage = null,
                chatInputText = if (wasEditing) "" else it.chatInputText
            )
        }
        if (chatId != null && peerId != null) {
            viewModelScope.launch(safeExceptionHandler) {
                chatRepository.updateDraftAndTyping(
                    ownerUserId = ownerId,
                    chatId = chatId,
                    peerUserId = peerId,
                    draftText = _uiState.value.chatInputText,
                    replyToMessageId = null
                )
            }
        }
    }

    fun sendCurrentTextOrSaveEdit() {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val activeChat = state.activeChatItem ?: return
        if (!activeChat.canSendMessages) return

        val text = state.chatInputText.trim()
        if (text.isEmpty()) return

        val ownerId = currentAuthenticatedUserId()
        val editingTarget = state.editingMessage
        val replyTarget = state.replyingToMessage

        _uiState.update {
            it.copy(
                chatInputText = "",
                replyingToMessage = null,
                editingMessage = null
            )
        }

        viewModelScope.launch(safeExceptionHandler) {
            if (editingTarget != null) {
                chatRepository.editMessage(
                    ownerUserId = ownerId,
                    chatId = chatId,
                    peerUserId = activeChat.peerUserId,
                    messageId = editingTarget.messageId,
                    newText = text
                )
            } else {
                chatRepository.sendMessage(
                    ownerUserId = ownerId,
                    chatId = chatId,
                    peerUserId = activeChat.peerUserId,
                    messageType = ChatMessageType.TEXT,
                    text = text,
                    replyToMessage = replyTarget,
                    peerDisplayNameForReply = activeChat.peerFullName
                )
            }
        }
    }

    // --- Klipy Stickers | GIFs | Clips Sheet & Preview Dialog ---

    fun onSelectKlipyTab(tab: KlipyMediaTab) {
        if (_uiState.value.activeKlipyTab == tab) return
        val ownerId = currentAuthenticatedUserId()
        _uiState.update { it.copy(activeKlipyTab = tab) }
        observeKlipyRecentsForActiveTab(ownerId, tab)
    }

    fun onKlipySearchQueryChanged(query: String) {
        _uiState.update { it.copy(klipySearchQuery = query) }
    }

    fun onOpenKlipyItemPreview(item: KlipyMediaItem?) {
        _uiState.update { it.copy(previewKlipyItem = item) }
    }

    fun confirmSendKlipyMediaItem(item: KlipyMediaItem) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val activeChat = state.activeChatItem ?: return
        if (!activeChat.canSendMessages) return

        val ownerId = currentAuthenticatedUserId()
        val replyTarget = state.replyingToMessage
        val msgType = when (item.mediaTab) {
            KlipyMediaTab.STICKERS -> ChatMessageType.STICKER
            KlipyMediaTab.GIFS -> ChatMessageType.GIF
            KlipyMediaTab.CLIPS -> ChatMessageType.CLIP
        }

        _uiState.update {
            it.copy(
                previewKlipyItem = null,
                replyingToMessage = null
            )
        }

        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.sendMessage(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = activeChat.peerUserId,
                messageType = msgType,
                text = item.title,
                klipyItem = item,
                replyToMessage = replyTarget,
                peerDisplayNameForReply = activeChat.peerFullName
            )
        }
    }

    // --- Voice Note Recording, Preview Player, View-Once Toggle & Send ---

    fun startVoiceNoteHoldRecording() {
        val activeChat = _uiState.value.activeChatItem ?: return
        if (!activeChat.canSendMessages) return
        chatRepository.startVoiceNoteRecording()
    }

    fun stopVoiceNoteHoldRecording() {
        chatRepository.stopVoiceNoteRecording()
    }

    fun discardRecordedVoiceNote() {
        chatRepository.discardRecordedVoiceNote()
    }

    fun toggleRecordedVoiceNoteViewOnce() {
        chatRepository.toggleVoiceNoteViewOnce()
    }

    fun toggleRecordedVoicePreviewPlay() {
        chatRepository.toggleVoicePreviewPlayback()
    }

    fun sendRecordedVoiceNote() {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val activeChat = state.activeChatItem ?: return
        if (!activeChat.canSendMessages) return

        val recState = voiceRecordingState.value
        val filePath = recState.recordedFilePath ?: return
        val durationMs = recState.recordedDurationMs.coerceAtLeast(1000L)
        val waveform = recState.recordedWaveformSamples
        val isViewOnce = recState.isViewOnceEnabled
        val replyTarget = state.replyingToMessage
        val ownerId = currentAuthenticatedUserId()

        chatRepository.stopVoicePreviewPlayback()
        _uiState.update { it.copy(replyingToMessage = null) }

        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.sendMessage(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = activeChat.peerUserId,
                messageType = ChatMessageType.VOICE_NOTE,
                text = "",
                voiceFilePath = filePath,
                voiceDurationMs = durationMs,
                voiceWaveformSamples = waveform,
                isViewOnce = isViewOnce,
                replyToMessage = replyTarget,
                peerDisplayNameForReply = activeChat.peerFullName
            )
            // Clear recording preview state without deleting the sent file
            chatRepository.stopVoicePreviewPlayback()
            // Reset state in repository
            resetVoicePreviewAfterSend()
        }
    }

    private fun resetVoicePreviewAfterSend() {
        chatRepository.discardStateOnlyAfterSend()
    }

    fun toggleBubbleVoiceNotePlayback(message: ChatMessageItem) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        chatRepository.toggleBubbleVoiceNotePlayback(
            ownerUserId = ownerId,
            chatId = chatId,
            peerUserId = peerId,
            message = message
        )
    }

    // --- Long Press Message Actions, Reactions, Pin, Favourite, Unsend, Select Mode & Forward ---

    fun reactToMessage(message: ChatMessageItem, choice: ChatReactionChoice) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.toggleMessageReaction(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId,
                messageId = message.messageId,
                reactionChoice = choice
            )
        }
    }

    fun togglePinMessage(message: ChatMessageItem) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.togglePinMessage(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId,
                messageId = message.messageId
            )
        }
    }

    fun toggleFavouriteMessage(message: ChatMessageItem) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.toggleFavouriteMessage(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId,
                messageId = message.messageId
            )
        }
    }

    fun unsendMessageForBothDevices(message: ChatMessageItem) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        if (message.senderUserId != ownerId) return
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.unsendMessage(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId,
                messageId = message.messageId
            )
        }
    }

    fun deleteSingleMessage(message: ChatMessageItem) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.deleteMessages(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId,
                messageIds = listOf(message.messageId)
            )
        }
    }

    fun enterMessageSelectMode(initialMessageId: String) {
        _uiState.update {
            it.copy(
                isSelectModeActive = true,
                selectedMessageIds = setOf(initialMessageId)
            )
        }
    }

    fun toggleSelectedMessageId(messageId: String) {
        _uiState.update { state ->
            val next = if (messageId in state.selectedMessageIds) {
                state.selectedMessageIds - messageId
            } else {
                state.selectedMessageIds + messageId
            }
            state.copy(
                selectedMessageIds = next,
                isSelectModeActive = next.isNotEmpty()
            )
        }
    }

    fun toggleMarkAllMessagesInSelectMode() {
        _uiState.update { state ->
            val allIds = state.activeChatMessages.map { it.messageId }.toSet()
            val next = if (state.selectedMessageIds.size == allIds.size && allIds.isNotEmpty()) {
                emptySet()
            } else {
                allIds
            }
            state.copy(
                selectedMessageIds = next,
                isSelectModeActive = true
            )
        }
    }

    fun exitMessageSelectMode() {
        _uiState.update {
            it.copy(
                isSelectModeActive = false,
                selectedMessageIds = emptySet()
            )
        }
    }

    fun deleteSelectedMessagesInSelectMode() {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val selectedIds = state.selectedMessageIds.toList()
        if (selectedIds.isEmpty()) return
        val ownerId = currentAuthenticatedUserId()

        _uiState.update {
            it.copy(
                isSelectModeActive = false,
                selectedMessageIds = emptySet()
            )
        }

        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.deleteMessages(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId,
                messageIds = selectedIds
            )
        }
    }

    /**
     * Checks whether Copy or Forward is permitted by the owner's PrivacySettingsEntity in Room.
     * - Copy follows `whoCanCopyMessages` (`NOBODY` disallows copy).
     * - Forward follows `whoCanShareChats` (`NOBODY` disallows forwarding, hiding Forward everywhere).
     */
    fun isCopyMessageAllowedByPrivacy(): Boolean {
        val raw = _uiState.value.privacySettings.whoCanCopyMessages
        return PrivacyAudience.fromStorageValue(raw) != PrivacyAudience.NOBODY
    }

    fun isForwardMessageAllowedByPrivacy(): Boolean {
        val raw = _uiState.value.privacySettings.whoCanShareChats
        return PrivacyAudience.fromStorageValue(raw) != PrivacyAudience.NOBODY
    }

    fun openForwardContactsPicker(messages: List<ChatMessageItem>) {
        if (!isForwardMessageAllowedByPrivacy()) return
        val validMessages = messages.filter { !it.isViewOnce && !it.isViewOnceConsumed }
        if (validMessages.isEmpty()) return

        viewModelScope.launch(safeExceptionHandler) {
            val ownerId = currentAuthenticatedUserId()
            val allProfiles = repository.dao.getAllDiscoverableProfilesExcept(ownerId)
            val existingChats = _uiState.value.chatListItems
            val deletedPeerIds = existingChats.filter { it.peerIsDeleted }.map { it.peerUserId }.toSet()

            val candidates = mutableListOf<ForwardRecipientTarget>()
            val seenIds = mutableSetOf<String>()

            // Include existing 1:1 chat peers first, then Contacts from Room, skipping blocked or deleted users
            for (chat in existingChats) {
                if (chat.peerIsDeleted || chat.isBlockedEitherWay) continue
                if (seenIds.add(chat.peerUserId)) {
                    candidates.add(
                        ForwardRecipientTarget(
                            userId = chat.peerUserId,
                            fullName = chat.peerFullName,
                            username = chat.peerUsername,
                            avatarUrl = chat.peerAvatarUrl,
                            bio = chat.peerBio,
                            isOnline = chat.peerIsOnline,
                            isDeleted = false,
                            isBlocked = false
                        )
                    )
                }
            }

            for (profile in allProfiles) {
                if (profile.userId in deletedPeerIds) continue
                val isBlocked = repository.dao.isEitherBlocked(ownerId, profile.userId) > 0
                if (isBlocked) continue
                if (seenIds.add(profile.userId)) {
                    candidates.add(
                        ForwardRecipientTarget(
                            userId = profile.userId,
                            fullName = profile.fullName,
                            username = profile.username,
                            avatarUrl = profile.avatarCdnUrl,
                            bio = profile.bio,
                            isOnline = true,
                            isDeleted = false,
                            isBlocked = false
                        )
                    )
                }
            }

            _uiState.update {
                it.copy(
                    isForwardPickerOpen = true,
                    messagesPendingForward = validMessages,
                    forwardCandidates = candidates,
                    forwardSearchQuery = "",
                    selectedForwardUserIds = emptySet()
                )
            }
        }
    }

    fun onForwardSearchQueryChanged(query: String) {
        _uiState.update { it.copy(forwardSearchQuery = query) }
    }

    fun toggleForwardRecipientSelection(userId: String) {
        _uiState.update { state ->
            val next = if (userId in state.selectedForwardUserIds) {
                state.selectedForwardUserIds - userId
            } else {
                state.selectedForwardUserIds + userId
            }
            state.copy(selectedForwardUserIds = next)
        }
    }

    fun dismissForwardContactsPicker() {
        _uiState.update {
            it.copy(
                isForwardPickerOpen = false,
                messagesPendingForward = emptyList(),
                selectedForwardUserIds = emptySet(),
                forwardSearchQuery = ""
            )
        }
    }

    fun confirmForwardSelectedMessages() {
        val state = _uiState.value
        val messages = state.messagesPendingForward
        val selectedTargets = state.forwardCandidates.filter { it.userId in state.selectedForwardUserIds }
        if (messages.isEmpty() || selectedTargets.isEmpty()) return
        val ownerId = currentAuthenticatedUserId()

        _uiState.update {
            it.copy(
                isForwardPickerOpen = false,
                messagesPendingForward = emptyList(),
                selectedForwardUserIds = emptySet(),
                forwardSearchQuery = "",
                isSelectModeActive = false,
                selectedMessageIds = emptySet()
            )
        }

        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.forwardMessagesToRecipients(
                ownerUserId = ownerId,
                messagesToForward = messages,
                recipients = selectedTargets
            )
        }
    }

    // --- 3-Dots Chat Menu Actions (Disappearing, Clear Chat, Block, Report, E2EE Verification) ---

    fun setActiveChatDisappearingOption(option: ChatDisappearingOption) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.setDisappearingMessagesTimer(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId,
                option = option
            )
        }
    }

    fun clearActiveChatHistory() {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.clearChatHistory(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId
            )
        }
    }

    fun toggleBlockActiveChatUser() {
        val activeChat = _uiState.value.activeChatItem ?: return
        val shouldBlock = !activeChat.isBlockedByMe
        viewModelScope.launch(safeExceptionHandler) {
            val ownerId = currentAuthenticatedUserId()
            val telemetry = currentTelemetry()
            socialRepository.setBlockUserState(
                currentUserId = ownerId,
                targetUserId = activeChat.peerUserId,
                block = shouldBlock,
                telemetry = telemetry
            )
        }
    }

    fun submitReportForActiveChatUser(
        reason: ReportReasonOption,
        location: ReportLocationOption,
        urgency: ReportUrgencyOption,
        blockAfterReport: Boolean = true,
        additionalNotes: String = ""
    ) {
        val activeChat = _uiState.value.activeChatItem ?: return
        viewModelScope.launch(safeExceptionHandler) {
            val ownerId = currentAuthenticatedUserId()
            val telemetry = currentTelemetry()
            socialRepository.submitProfileReport(
                currentUserId = ownerId,
                reportedUserId = activeChat.peerUserId,
                reason = reason,
                location = location,
                urgency = urgency,
                additionalNotes = additionalNotes,
                telemetry = telemetry
            )
            if (blockAfterReport && !activeChat.isBlockedByMe) {
                socialRepository.setBlockUserState(
                    currentUserId = ownerId,
                    targetUserId = activeChat.peerUserId,
                    block = true,
                    telemetry = telemetry
                )
            }
        }
    }

    fun openActiveChatE2eeVerificationScreen() {
        val chatId = _uiState.value.activeChatId ?: return
        val ownerId = currentAuthenticatedUserId()
        viewModelScope.launch(safeExceptionHandler) {
            val snapshot = chatRepository.getE2eeVerificationSnapshot(ownerId, chatId)
            if (snapshot != null) {
                _uiState.update {
                    it.copy(
                        activeE2eeVerification = snapshot,
                        destination = AppDestination.E2EE_VERIFICATION
                    )
                }
            }
        }
    }

    fun setActiveChatE2eeVerified(verified: Boolean) {
        val chatId = _uiState.value.activeChatId ?: _uiState.value.activeE2eeVerification?.chatId ?: return
        val ownerId = currentAuthenticatedUserId()
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.setE2eeSafetyNumberVerified(chatId, verified)
            val refreshed = chatRepository.getE2eeVerificationSnapshot(ownerId, chatId)
            _uiState.update {
                it.copy(activeE2eeVerification = refreshed)
            }
        }
    }

    fun toggleActiveChatE2eeVerified() {
        val snap = _uiState.value.activeE2eeVerification ?: return
        setActiveChatE2eeVerified(!snap.isVerified)
    }

    fun closeE2eeVerificationScreen() {
        _uiState.update {
            it.copy(destination = AppDestination.ONE_TO_ONE_CHAT)
        }
    }

    // --- Rich File & Attachment Sending (Gallery, Camera, Location, Contact, Document, Poll, Event) ---

    fun sendMediaAttachmentItems(
        items: List<com.telefam.company.app.domain.ChatMediaAttachmentItem>,
        caption: String,
        isViewOnce: Boolean
    ) {
        if (items.isEmpty()) return
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val activeChat = state.activeChatItem ?: return
        if (!activeChat.canSendMessages) return

        val ownerId = currentAuthenticatedUserId()
        val replyTarget = state.replyingToMessage
        val cleanCaption = caption.trim()
        val effectiveViewOnce = isViewOnce && items.size == 1

        _uiState.update { it.copy(replyingToMessage = null) }

        viewModelScope.launch(safeExceptionHandler) {
            if (items.size == 1) {
                val single = items.first()
                val msgType = if (single.isVideo) ChatMessageType.VIDEO else ChatMessageType.IMAGE
                val singleAlbumPayload = com.telefam.company.app.domain.ChatMediaAlbumPayload(
                    items = listOf(single),
                    caption = cleanCaption
                )
                val encodedJson = com.telefam.company.app.domain.ChatAttachmentPayloadCodec.encodeAlbum(singleAlbumPayload)
                chatRepository.sendMessage(
                    ownerUserId = ownerId,
                    chatId = chatId,
                    peerUserId = activeChat.peerUserId,
                    messageType = msgType,
                    text = cleanCaption,
                    voiceDurationMs = single.durationMs,
                    isViewOnce = effectiveViewOnce,
                    replyToMessage = replyTarget,
                    peerDisplayNameForReply = activeChat.peerFullName,
                    attachmentMediaId = single.itemId,
                    attachmentMediaUrl = encodedJson,
                    attachmentPreviewUrl = single.thumbnailPath ?: single.localPath,
                    attachmentLocalPath = single.localPath,
                    attachmentTitle = single.fileName,
                    attachmentMimeType = single.mimeType
                )
            } else {
                val albumPayload = com.telefam.company.app.domain.ChatMediaAlbumPayload(
                    items = items,
                    caption = cleanCaption
                )
                val encodedJson = com.telefam.company.app.domain.ChatAttachmentPayloadCodec.encodeAlbum(albumPayload)
                val firstItem = items.first()
                chatRepository.sendMessage(
                    ownerUserId = ownerId,
                    chatId = chatId,
                    peerUserId = activeChat.peerUserId,
                    messageType = ChatMessageType.MEDIA_ALBUM,
                    text = cleanCaption,
                    voiceDurationMs = firstItem.durationMs,
                    isViewOnce = false,
                    replyToMessage = replyTarget,
                    peerDisplayNameForReply = activeChat.peerFullName,
                    attachmentMediaId = "album_${java.util.UUID.randomUUID()}",
                    attachmentMediaUrl = encodedJson,
                    attachmentPreviewUrl = firstItem.thumbnailPath ?: firstItem.localPath,
                    attachmentLocalPath = firstItem.localPath,
                    attachmentTitle = "Album (${items.size} items)",
                    attachmentMimeType = "multipart/mixed"
                )
            }
        }
    }

    fun sendLocationAttachment(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        placeTitle: String,
        addressLine: String,
        isLiveLocation: Boolean,
        liveDurationMinutes: Int,
        isViewOnce: Boolean,
        caption: String
    ) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val activeChat = state.activeChatItem ?: return
        if (!activeChat.canSendMessages) return

        val ownerId = currentAuthenticatedUserId()
        val replyTarget = state.replyingToMessage
        val cleanCaption = caption.trim()

        _uiState.update { it.copy(replyingToMessage = null) }

        viewModelScope.launch(safeExceptionHandler) {
            val (liveToken, expiresAtMs) = if (isLiveLocation || isViewOnce) {
                chatRepository.issueServerLiveLocationToken(
                    ownerUserId = ownerId,
                    chatId = chatId,
                    peerUserId = activeChat.peerUserId,
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = accuracyMeters,
                    durationMinutes = if (isLiveLocation) liveDurationMinutes else 30,
                    isViewOnce = isViewOnce
                )
            } else {
                "" to null
            }

            val locationPayload = com.telefam.company.app.domain.ChatLocationPayload(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracyMeters,
                placeTitle = placeTitle.trim().ifBlank { "Shared Location" },
                addressLine = addressLine.trim(),
                isLiveLocation = isLiveLocation,
                liveSessionToken = liveToken,
                expiresAtMs = expiresAtMs,
                isViewOnce = isViewOnce,
                caption = cleanCaption
            )
            val encodedJson = com.telefam.company.app.domain.ChatAttachmentPayloadCodec.encodeLocation(locationPayload)
            chatRepository.sendMessage(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = activeChat.peerUserId,
                messageType = ChatMessageType.LOCATION,
                text = cleanCaption.ifBlank { locationPayload.placeTitle },
                isViewOnce = isViewOnce,
                replyToMessage = replyTarget,
                peerDisplayNameForReply = activeChat.peerFullName,
                attachmentMediaId = "loc_${java.util.UUID.randomUUID()}",
                attachmentMediaUrl = encodedJson,
                attachmentTitle = locationPayload.placeTitle,
                attachmentMimeType = "application/vnd.telefam.location+json"
            )
        }
    }

    fun sendContactAttachment(
        contact: com.telefam.company.app.domain.ChatContactPayload,
        caption: String = ""
    ) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val activeChat = state.activeChatItem ?: return
        if (!activeChat.canSendMessages) return

        val ownerId = currentAuthenticatedUserId()
        val replyTarget = state.replyingToMessage
        val cleanCaption = caption.trim()
        val finalContact = if (cleanCaption.isNotBlank()) contact.copy(caption = cleanCaption) else contact
        val encodedJson = com.telefam.company.app.domain.ChatAttachmentPayloadCodec.encodeContact(finalContact)

        _uiState.update { it.copy(replyingToMessage = null) }

        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.sendMessage(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = activeChat.peerUserId,
                messageType = ChatMessageType.CONTACT,
                text = cleanCaption.ifBlank { finalContact.displayName },
                isViewOnce = false,
                replyToMessage = replyTarget,
                peerDisplayNameForReply = activeChat.peerFullName,
                attachmentMediaId = "contact_${java.util.UUID.randomUUID()}",
                attachmentMediaUrl = encodedJson,
                attachmentTitle = finalContact.displayName,
                attachmentMimeType = "text/vcard"
            )
        }
    }

    fun sendDocumentAttachment(
        document: com.telefam.company.app.domain.ChatDocumentPayload
    ) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val activeChat = state.activeChatItem ?: return
        if (!activeChat.canSendMessages) return

        val ownerId = currentAuthenticatedUserId()
        val replyTarget = state.replyingToMessage
        val encodedJson = com.telefam.company.app.domain.ChatAttachmentPayloadCodec.encodeDocument(document)

        _uiState.update { it.copy(replyingToMessage = null) }

        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.sendMessage(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = activeChat.peerUserId,
                messageType = ChatMessageType.DOCUMENT,
                text = document.caption.ifBlank { document.fileName },
                voiceDurationMs = document.audioDurationMs,
                isViewOnce = false,
                replyToMessage = replyTarget,
                peerDisplayNameForReply = activeChat.peerFullName,
                attachmentMediaId = "doc_${java.util.UUID.randomUUID()}",
                attachmentMediaUrl = encodedJson,
                attachmentLocalPath = document.localPath,
                attachmentTitle = document.fileName,
                attachmentMimeType = document.mimeType
            )
        }
    }

    fun sendPollAttachment(
        poll: com.telefam.company.app.domain.ChatPollPayload
    ) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val activeChat = state.activeChatItem ?: return
        if (!activeChat.canSendMessages) return

        val ownerId = currentAuthenticatedUserId()
        val replyTarget = state.replyingToMessage
        val finalPoll = poll.copy(createdByUserId = ownerId)
        val encodedJson = com.telefam.company.app.domain.ChatAttachmentPayloadCodec.encodePoll(finalPoll)

        _uiState.update { it.copy(replyingToMessage = null) }

        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.sendMessage(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = activeChat.peerUserId,
                messageType = ChatMessageType.POLL,
                text = finalPoll.question,
                isViewOnce = false,
                replyToMessage = replyTarget,
                peerDisplayNameForReply = activeChat.peerFullName,
                attachmentMediaId = finalPoll.pollId,
                attachmentMediaUrl = encodedJson,
                attachmentTitle = finalPoll.question,
                attachmentMimeType = "application/vnd.telefam.poll+json"
            )
        }
    }

    fun sendEventAttachment(
        event: com.telefam.company.app.domain.ChatEventPayload
    ) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val activeChat = state.activeChatItem ?: return
        if (!activeChat.canSendMessages) return

        val ownerId = currentAuthenticatedUserId()
        val replyTarget = state.replyingToMessage
        val finalEvent = event.copy(
            createdByUserId = ownerId,
            goingUserIds = (event.goingUserIds + ownerId).distinct()
        )
        val encodedJson = com.telefam.company.app.domain.ChatAttachmentPayloadCodec.encodeEvent(finalEvent)

        _uiState.update { it.copy(replyingToMessage = null) }

        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.sendMessage(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = activeChat.peerUserId,
                messageType = ChatMessageType.EVENT,
                text = finalEvent.title,
                isViewOnce = false,
                replyToMessage = replyTarget,
                peerDisplayNameForReply = activeChat.peerFullName,
                attachmentMediaId = finalEvent.eventId,
                attachmentMediaUrl = encodedJson,
                attachmentTitle = finalEvent.title,
                attachmentMimeType = "application/vnd.telefam.event+json"
            )
        }
    }

    fun voteOnPollMessage(messageId: String, selectedOptionIds: List<String>) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.voteOnPollMessage(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId,
                messageId = messageId,
                selectedOptionIds = selectedOptionIds
            )
        }
    }

    fun rsvpToEventMessage(messageId: String, rsvpStatus: String) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.rsvpToEventMessage(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId,
                messageId = messageId,
                rsvpStatus = rsvpStatus
            )
        }
    }

    fun consumeViewOnceMessage(messageId: String) {
        val state = _uiState.value
        val chatId = state.activeChatId ?: return
        val peerId = state.activeChatItem?.peerUserId ?: return
        val ownerId = currentAuthenticatedUserId()
        viewModelScope.launch(safeExceptionHandler) {
            chatRepository.consumeViewOnceVoiceNote(
                ownerUserId = ownerId,
                chatId = chatId,
                peerUserId = peerId,
                messageId = messageId
            )
        }
    }

    fun handleNotificationDeepLink(deepLink: TelefamNotificationDeepLink) {
        val state = _uiState.value
        if (state.activeSession == null && state.savedProfile == null) return
        TelefamNotificationEngine.consumePendingDeepLink()

        when (deepLink.targetType) {
            TelefamDeepLinkTargetType.CHAT_CONVERSATION,
            TelefamDeepLinkTargetType.INCOMING_CALL,
            TelefamDeepLinkTargetType.LIVE_LOCATION -> {
                val targetChatId = deepLink.chatId
                val peerId = deepLink.peerUserId
                if (!targetChatId.isNullOrBlank()) {
                    openOneToOneChatById(targetChatId)
                } else if (!peerId.isNullOrBlank()) {
                    val existing = state.chatListItems.firstOrNull { it.peerUserId == peerId }
                    if (existing != null) {
                        openOneToOneChatById(existing.chatId)
                    } else {
                        val user = state.contactsUsers.firstOrNull { it.userId == peerId }
                        if (user != null) {
                            openOneToOneChatWithDiscoverableUser(user)
                        }
                    }
                }
            }
            TelefamDeepLinkTargetType.SOCIAL_FOLLOWER -> {
                val actorId = deepLink.socialActorUserId
                val actor = state.contactsUsers.firstOrNull { it.userId == actorId }
                if (actor != null) {
                    openPublicUserProfile(actor.userId)
                } else {
                    _uiState.update {
                        it.copy(
                            destination = AppDestination.MAIN_HOME,
                            selectedHomeTab = HomeNavTab.PROFILE
                        )
                    }
                    openOwnerFollowList(FollowListType.FOLLOWERS)
                }
            }
            TelefamDeepLinkTargetType.SOCIAL_POST_ACTIVITY -> {
                _uiState.update {
                    it.copy(
                        destination = AppDestination.MAIN_HOME,
                        selectedHomeTab = HomeNavTab.PROFILE
                    )
                }
            }
            TelefamDeepLinkTargetType.SECURITY_DEVICE_ALERT -> {
                _uiState.update {
                    it.copy(
                        destination = AppDestination.SETTINGS_AND_PRIVACY,
                        settingsSubScreen = SettingsSubScreen.ROOT,
                        settingsNavigationStack = emptyList()
                    )
                }
            }
        }
    }
}
