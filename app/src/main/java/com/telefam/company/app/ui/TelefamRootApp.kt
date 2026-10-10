package com.telefam.company.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.resolveTelefamDarkTheme
import com.telefam.company.app.ui.screens.AuthScreen
import com.telefam.company.app.ui.screens.MainHomeScreen
import com.telefam.company.app.ui.screens.PrivacySettingsScreen
import com.telefam.company.app.ui.screens.ProfileSetupScreen
import com.telefam.company.app.ui.screens.PublicUserProfileScreen
import com.telefam.company.app.ui.screens.SettingsAndPrivacyScreen
import com.telefam.company.app.ui.screens.TelefamLaunchScreen

@Composable
fun TelefamRootApp(
    viewModel: TelefamViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resolvedDarkTheme = resolveTelefamDarkTheme(uiState.accountSettings.appThemeMode)

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onLocationPermissionResult()
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    MyApplicationTheme(darkTheme = resolvedDarkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val currentWidthDp = maxWidth.value.toInt()
            LaunchedEffect(currentWidthDp) {
                viewModel.updateScreenWidthDp(currentWidthDp)
            }

            Crossfade(
                targetState = uiState.destination,
                animationSpec = tween(durationMillis = 260),
                label = "telefam_root_navigation"
            ) { destination ->
                when (destination) {
                    AppDestination.LAUNCH_ANIMATION -> {
                        TelefamLaunchScreen()
                    }
                    AppDestination.AUTH -> {
                        AuthScreen(
                            uiState = uiState,
                            onEmailChanged = viewModel::onEmailChanged,
                            onPasswordChanged = viewModel::onPasswordChanged,
                            onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
                            onOtpChanged = viewModel::onOtpCodeChanged,
                            onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                            onToggleConfirmPasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
                            onPrimaryAction = viewModel::submitPrimaryAuthAction,
                            onSwitchMode = viewModel::switchAuthMode,
                            onContinueWithGoogle = { viewModel.signInWithGoogle(context) },
                            onSignInWithPasskey = viewModel::signInWithPasskey,
                            onResendOtp = viewModel::resendCurrentOtp,
                            onRequestLoginOtpInstead = viewModel::requestOtpInsteadOfLockedLogin
                        )
                    }
                    AppDestination.PROFILE_SETUP -> {
                        ProfileSetupScreen(
                            uiState = uiState,
                            onFullNameChanged = viewModel::onFullNameChanged,
                            onUsernameChanged = viewModel::onUsernameChanged,
                            onApplySuggestedUsername = viewModel::onApplySuggestedUsername,
                            onCountrySelected = viewModel::onCountrySelected,
                            onPhoneNumberChanged = viewModel::onPhoneNumberChanged,
                            onGenderSelected = viewModel::onGenderSelected,
                            onDateOfBirthSelected = viewModel::onDateOfBirthSelected,
                            onBioChanged = viewModel::onBioChanged,
                            onAvatarUriPicked = viewModel::onAvatarUriPicked,
                            onCancelCrop = viewModel::cancelImageCrop,
                            onConfirmCrop = viewModel::confirmImageCropAndSanitize,
                            onSubmitProfile = viewModel::submitProfileSetup,
                            onBackClick = viewModel::navigateBackFromProfileSetup
                        )
                    }
                    AppDestination.MAIN_HOME -> {
                        MainHomeScreen(
                            uiState = uiState,
                            selectedTab = uiState.selectedHomeTab,
                            onSelectTab = viewModel::selectHomeTab,
                            onOpenPrivacySettings = viewModel::openPrivacySettings,
                            onContactsFilterSelected = viewModel::onContactsFilterModeSelected,
                            onToggleContactsSearch = viewModel::onToggleContactsSearch,
                            onContactsSearchQueryChanged = viewModel::onContactsSearchQueryChanged,
                            onContactsPermissionResult = viewModel::onContactsPermissionResult,
                            onToggleFollowUser = viewModel::toggleFollowUser,
                            onOpenUserProfile = viewModel::openPublicUserProfile,
                            onLoadMoreContacts = viewModel::loadMoreContactsPage,
                            onRefreshContacts = { viewModel.refreshContactsDirectory(forceDailyRefresh = true) },
                            onDismissContactsError = viewModel::clearContactsError,
                            onOwnerPullToRefresh = { viewModel.refreshOwnerProfileSnapshot(isPullToRefresh = true) },
                            onOwnerSelectSubTab = viewModel::selectOwnerProfileSubTab,
                            onOwnerOpenFollowList = viewModel::openOwnerFollowList,
                            onOwnerLoadMoreFollowList = viewModel::loadMoreOwnerFollowList,
                            onOwnerDismissFollowList = viewModel::dismissOwnerFollowList,
                            onOwnerOpenEditProfile = viewModel::openOwnerEditProfileSheet,
                            onOwnerDismissEditProfile = viewModel::dismissOwnerEditProfileSheet,
                            onOwnerFullNameChanged = viewModel::onFullNameChanged,
                            onOwnerUsernameChanged = viewModel::onUsernameChanged,
                            onOwnerApplySuggestedUsername = viewModel::onApplySuggestedUsername,
                            onOwnerCountrySelected = viewModel::onCountrySelected,
                            onOwnerPhoneNumberChanged = viewModel::onPhoneNumberChanged,
                            onOwnerGenderSelected = viewModel::onGenderSelected,
                            onOwnerDateOfBirthSelected = viewModel::onDateOfBirthSelected,
                            onOwnerBioChanged = viewModel::onBioChanged,
                            onOwnerAvatarUriPicked = viewModel::onAvatarUriPicked,
                            onOwnerCancelCrop = viewModel::cancelImageCrop,
                            onOwnerConfirmCrop = viewModel::confirmImageCropAndSanitize,
                            onOwnerSaveProfileEdits = viewModel::saveOwnerProfileEdits,
                            onOwnerClearStatusBanner = viewModel::clearOwnerStatusBanner,
                            onOpenSettingsAndPrivacy = { viewModel.openSettingsAndPrivacy() },
                            onOpenOwnerMenu = viewModel::openOwnerMenu,
                            onCloseOwnerMenu = viewModel::closeOwnerMenu
                        )
                    }
                    AppDestination.PRIVACY_SETTINGS -> {
                        PrivacySettingsScreen(
                            privacySettings = uiState.privacySettings,
                            onBackClick = viewModel::closePrivacySettings,
                            onUpdatePrivacyRule = viewModel::updatePrivacyRule,
                            onUpdateChatTheme = viewModel::updateChatBackgroundTheme,
                            onUpdateBubbleColor = viewModel::updateMessageBubbleColor
                        )
                    }
                    AppDestination.SETTINGS_AND_PRIVACY -> {
                        SettingsAndPrivacyScreen(
                            uiState = uiState,
                            onBackClick = viewModel::navigateBackInSettings,
                            onNavigateSubScreen = viewModel::navigateToSettingsSubScreen,
                            onClearBanners = viewModel::clearSettingsBanners,
                            onRetryLastAction = viewModel::retryLastFailedSettingsAction,
                            onOpenChatPrivacyAndAppearance = viewModel::openChatPrivacyAndAppearance,
                            onFullNameChanged = viewModel::onFullNameChanged,
                            onUsernameChanged = viewModel::onUsernameChanged,
                            onApplySuggestedUsername = viewModel::onApplySuggestedUsername,
                            onCountrySelected = viewModel::onCountrySelected,
                            onPhoneNumberChanged = viewModel::onPhoneNumberChanged,
                            onAvatarUriPicked = viewModel::onAvatarUriPicked,
                            onCancelCrop = viewModel::cancelImageCrop,
                            onConfirmCrop = viewModel::confirmImageCropAndSanitize,
                            onSaveOwnerProfileEdits = viewModel::saveOwnerProfileEdits,
                            onSendVerificationOtp = viewModel::sendSettingsVerificationOtp,
                            onConfirmChangeEmail = viewModel::confirmChangeAccountEmail,
                            onSubmitPasswordChange = viewModel::submitAccountPasswordChange,
                            onSaveRecoverySettings = viewModel::saveRecoveryContactsAndRegenerateCodes,
                            onConfirmDeactivateAccount = viewModel::confirmDeactivateAccount,
                            onConfirmDeleteAccount30Days = viewModel::confirmSchedule30DayAccountDeletion,
                            onEnableTwoStep = viewModel::enableTwoStepVerification,
                            onDisableTwoStep = viewModel::disableTwoStepVerification,
                            onRegisterPasskey = viewModel::registerPasskeyCredential,
                            onRevokePasskey = viewModel::revokePasskeyCredential,
                            onLogoutDevice = viewModel::logoutDeviceSession,
                            onRevokeConnectedApp = viewModel::revokeConnectedApp,
                            onOpenPermissionDetail = viewModel::openPermissionDetail,
                            onSystemPermissionUpdated = viewModel::onSystemPermissionUpdated,
                            onTogglePrivateAccount = viewModel::togglePrivateAccountSetting,
                            onOpenPrivacyRuleSelector = viewModel::openPrivacyRuleSelector,
                            onSelectPrivacyRule = viewModel::selectSocialPrivacyRule,
                            onSetAppThemeMode = viewModel::updateAppThemeOption,
                            onUpdateAccountSettings = viewModel::updateAccountSettingsTransform,
                            onDownloadQrCode = viewModel::downloadProfileQrCode,
                            onRegenerateQrCode = viewModel::regenerateBackendProfileQrCode,
                            onOpenPublicUserProfile = viewModel::openPublicUserProfile,
                            onBlockedSearchQueryChanged = viewModel::onBlockedAccountsSearchQueryChanged,
                            onUnblockAccount = viewModel::unblockAccountFromSettings,
                            onSubmitTicket = viewModel::submitProblemReportOrSupportRequest,
                            onClearSubmittedTicket = viewModel::clearSubmittedTicketConfirmation,
                            onUpdateSupportTicket = viewModel::submitSupportTicketFollowUp,
                            onOpenSafetyGuide = viewModel::openSafetyGuideDetail,
                            onHelpSearchQueryChanged = viewModel::onHelpSearchQueryChanged,
                            onOpenHelpArticle = viewModel::openHelpArticleDetail,
                            onSubmitHelpFeedback = viewModel::submitHelpArticleFeedback,
                            onOpenLegalDocument = viewModel::openLegalDocumentDetail,
                            onAcknowledgeLegalDocument = viewModel::acknowledgeLegalDocument,
                            onDownloadLegalDocument = viewModel::downloadLegalDocument,
                            onOpenAboutDetail = viewModel::openAboutSectionDetail,
                            onConfirmLogOut = viewModel::signOut
                        )
                    }
                    AppDestination.PUBLIC_USER_PROFILE -> {
                        val activeProfile = uiState.activePublicProfile
                        if (activeProfile != null) {
                            PublicUserProfileScreen(
                                profile = activeProfile,
                                posts = uiState.activePublicProfilePosts,
                                postsFilter = uiState.activePublicProfilePostsFilter,
                                isOffline = false,
                                activeFollowListType = uiState.activeFollowListType,
                                followListUsers = uiState.followListUsers,
                                followListHasMore = uiState.followListHasMore,
                                onBackClick = viewModel::closePublicUserProfile,
                                onToggleFollowProfileUser = { viewModel.toggleFollowUser(activeProfile.userId) },
                                onToggleFollowAnyUser = viewModel::toggleFollowUser,
                                onOpenOtherUserProfile = viewModel::openPublicUserProfile,
                                onMessageComingSoonClick = {},
                                onSubscribeComingSoonClick = {},
                                onPostComingSoonClick = {},
                                onPostsFilterChange = viewModel::onPublicProfilePostsFilterChanged,
                                onOpenFollowList = viewModel::openFollowConnectionsSheet,
                                onDismissFollowList = viewModel::dismissFollowConnectionsSheet,
                                onLoadMoreFollowList = viewModel::loadMoreFollowConnections,
                                onCopyProfileUrlConfirmed = {},
                                onToggleBlockUser = viewModel::setBlockActiveProfileUser,
                                onSubmitReport = viewModel::submitMultiQuestionReportForActiveProfile
                            )
                        } else {
                            MainHomeScreen(
                                uiState = uiState,
                                selectedTab = uiState.selectedHomeTab,
                                onSelectTab = viewModel::selectHomeTab,
                                onOpenPrivacySettings = viewModel::openPrivacySettings,
                                onContactsFilterSelected = viewModel::onContactsFilterModeSelected,
                                onToggleContactsSearch = viewModel::onToggleContactsSearch,
                                onContactsSearchQueryChanged = viewModel::onContactsSearchQueryChanged,
                                onContactsPermissionResult = viewModel::onContactsPermissionResult,
                                onToggleFollowUser = viewModel::toggleFollowUser,
                                onOpenUserProfile = viewModel::openPublicUserProfile,
                                onLoadMoreContacts = viewModel::loadMoreContactsPage,
                                onRefreshContacts = { viewModel.refreshContactsDirectory(forceDailyRefresh = true) },
                                onDismissContactsError = viewModel::clearContactsError,
                                onOwnerPullToRefresh = { viewModel.refreshOwnerProfileSnapshot(isPullToRefresh = true) },
                                onOwnerSelectSubTab = viewModel::selectOwnerProfileSubTab,
                                onOwnerOpenFollowList = viewModel::openOwnerFollowList,
                                onOwnerLoadMoreFollowList = viewModel::loadMoreOwnerFollowList,
                                onOwnerDismissFollowList = viewModel::dismissOwnerFollowList,
                                onOwnerOpenEditProfile = viewModel::openOwnerEditProfileSheet,
                                onOwnerDismissEditProfile = viewModel::dismissOwnerEditProfileSheet,
                                onOwnerFullNameChanged = viewModel::onFullNameChanged,
                                onOwnerUsernameChanged = viewModel::onUsernameChanged,
                                onOwnerApplySuggestedUsername = viewModel::onApplySuggestedUsername,
                                onOwnerCountrySelected = viewModel::onCountrySelected,
                                onOwnerPhoneNumberChanged = viewModel::onPhoneNumberChanged,
                                onOwnerGenderSelected = viewModel::onGenderSelected,
                                onOwnerDateOfBirthSelected = viewModel::onDateOfBirthSelected,
                                onOwnerBioChanged = viewModel::onBioChanged,
                                onOwnerAvatarUriPicked = viewModel::onAvatarUriPicked,
                                onOwnerCancelCrop = viewModel::cancelImageCrop,
                                onOwnerConfirmCrop = viewModel::confirmImageCropAndSanitize,
                                onOwnerSaveProfileEdits = viewModel::saveOwnerProfileEdits,
                                onOwnerClearStatusBanner = viewModel::clearOwnerStatusBanner,
                                onOpenSettingsAndPrivacy = { viewModel.openSettingsAndPrivacy() },
                                onOpenOwnerMenu = viewModel::openOwnerMenu,
                                onCloseOwnerMenu = viewModel::closeOwnerMenu
                            )
                        }
                    }
                }
            }
        }
    }
}
