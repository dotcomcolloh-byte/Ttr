package com.telefam.company.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.example.ui.theme.TelefamAvatarCircleDark
import com.example.ui.theme.TelefamAvatarCircleLight
import com.example.ui.theme.TelefamPlaceholderDark
import com.example.ui.theme.TelefamPlaceholderLight
import com.example.ui.theme.TelefamRed
import com.example.ui.theme.isTelefamDarkTheme
import com.telefam.company.app.domain.ContactsFilterMode
import com.telefam.company.app.domain.CountryDialCode
import com.telefam.company.app.domain.FollowListType
import com.telefam.company.app.domain.HomeNavTab
import com.telefam.company.app.domain.OwnerProfileSubTab
import com.telefam.company.app.ui.TelefamUiState
import com.telefam.company.app.ui.components.TelefamPaperPlaneLogo
import java.io.File

/**
 * Main Home Screen matching the uploaded Telefam Home reference image:
 * - Slim Red Top Header with a slim white pill Search Bar ("Search chats, people, groups")
 *   with compact search icon and a 3-dots More Options icon on the right (all triggering Coming Soon).
 * - Rounded top-corner content sheet directly below the slim red header.
 * - Default Chats empty state with soft blush circular chat-bubbles badge, "Start a conversation",
 *   subtitle "Tap the button below to chat with your friends, family or groups", and red
 *   "Start Conversation" button.
 * - Bottom Navigation Bar with 5 items:
 *   1. Chats
 *   2. Contacts
 *   3. Post (Floating red circular '+' button in the middle)
 *   4. Feeds
 *   5. Profile (Real user avatar loaded via Coil from locally saved offline storage)
 * - Adaptive Desktop layout (for >= 840.dp width) with slim red header, desktop navigation sidebar,
 *   and responsive workspace.
 */
@Composable
fun MainHomeScreen(
    uiState: TelefamUiState,
    selectedTab: HomeNavTab,
    onSelectTab: (HomeNavTab) -> Unit,
    onOpenPrivacySettings: () -> Unit = {},
    onContactsFilterSelected: (ContactsFilterMode) -> Unit = {},
    onToggleContactsSearch: (Boolean) -> Unit = {},
    onContactsSearchQueryChanged: (String) -> Unit = {},
    onContactsPermissionResult: (Boolean) -> Unit = {},
    onToggleFollowUser: (String) -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    onLoadMoreContacts: () -> Unit = {},
    onRefreshContacts: () -> Unit = {},
    onDismissContactsError: () -> Unit = {},
    onOwnerPullToRefresh: () -> Unit = {},
    onOwnerSelectSubTab: (OwnerProfileSubTab) -> Unit = {},
    onOwnerOpenFollowList: (FollowListType) -> Unit = {},
    onOwnerLoadMoreFollowList: () -> Unit = {},
    onOwnerDismissFollowList: () -> Unit = {},
    onOwnerOpenEditProfile: () -> Unit = {},
    onOwnerDismissEditProfile: () -> Unit = {},
    onOwnerFullNameChanged: (String) -> Unit = {},
    onOwnerUsernameChanged: (String) -> Unit = {},
    onOwnerApplySuggestedUsername: (String) -> Unit = {},
    onOwnerCountrySelected: (CountryDialCode) -> Unit = {},
    onOwnerPhoneNumberChanged: (String) -> Unit = {},
    onOwnerGenderSelected: (String) -> Unit = {},
    onOwnerDateOfBirthSelected: (String) -> Unit = {},
    onOwnerBioChanged: (String) -> Unit = {},
    onOwnerAvatarUriPicked: (Uri) -> Unit = {},
    onOwnerCancelCrop: () -> Unit = {},
    onOwnerConfirmCrop: (scale: Float, offsetXNorm: Float, offsetYNorm: Float, rotationDeg: Float) -> Unit = { _, _, _, _ -> },
    onOwnerSaveProfileEdits: (String) -> Unit = {},
    onOwnerClearStatusBanner: () -> Unit = {},
    onOpenSettingsAndPrivacy: () -> Unit = {},
    onOpenOwnerMenu: () -> Unit = {},
    onCloseOwnerMenu: () -> Unit = {},
    onOpenOneToOneChat: (String) -> Unit = {},
    onChatSearchQueryChanged: (String) -> Unit = {}
) {
    val comingSoonTitle = stringResource(R.string.coming_soon_title)
    var activeComingSoonFeature by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedTab != HomeNavTab.CHATS || activeComingSoonFeature != null) {
        if (activeComingSoonFeature != null) {
            activeComingSoonFeature = null
        } else if (selectedTab == HomeNavTab.PROFILE && uiState.isOwnerMenuOpen) {
            onCloseOwnerMenu()
        } else if (selectedTab != HomeNavTab.CHATS) {
            onSelectTab(HomeNavTab.CHATS)
        }
    }

    val isDark = isTelefamDarkTheme()
    val placeholderColor = if (isDark) TelefamPlaceholderDark else TelefamPlaceholderLight
    val circleTint = if (isDark) TelefamAvatarCircleDark else TelefamAvatarCircleLight

    val searchLabel = stringResource(R.string.search_chats_placeholder)
    val postLabel = stringResource(R.string.nav_post)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(TelefamRed)
            .testTag("main_home_screen_root")
    ) {
        val isDesktopLayout = maxWidth >= 840.dp

        if (isDesktopLayout) {
            DesktopHomeLayout(
                uiState = uiState,
                selectedTab = selectedTab,
                circleTint = circleTint,
                placeholderColor = placeholderColor,
                onSelectTab = { tab ->
                    onSelectTab(tab)
                    if (tab == HomeNavTab.POST) {
                        activeComingSoonFeature = postLabel
                    } else {
                        activeComingSoonFeature = null
                    }
                },
                onSearchClicked = { activeComingSoonFeature = searchLabel },
                onChatSearchQueryChanged = onChatSearchQueryChanged,
                onMoreOptionsClicked = onOpenPrivacySettings,
                onStartConversationClicked = { onSelectTab(HomeNavTab.CONTACTS) },
                onOpenOneToOneChat = onOpenOneToOneChat,
                onContactsFilterSelected = onContactsFilterSelected,
                onToggleContactsSearch = onToggleContactsSearch,
                onContactsSearchQueryChanged = onContactsSearchQueryChanged,
                onContactsPermissionResult = onContactsPermissionResult,
                onToggleFollowUser = onToggleFollowUser,
                onOpenUserProfile = onOpenUserProfile,
                onLoadMoreContacts = onLoadMoreContacts,
                onRefreshContacts = onRefreshContacts,
                onDismissContactsError = onDismissContactsError,
                onOwnerPullToRefresh = onOwnerPullToRefresh,
                onOwnerSelectSubTab = onOwnerSelectSubTab,
                onOwnerOpenFollowList = onOwnerOpenFollowList,
                onOwnerLoadMoreFollowList = onOwnerLoadMoreFollowList,
                onOwnerDismissFollowList = onOwnerDismissFollowList,
                onOwnerOpenEditProfile = onOwnerOpenEditProfile,
                onOwnerDismissEditProfile = onOwnerDismissEditProfile,
                onOwnerFullNameChanged = onOwnerFullNameChanged,
                onOwnerUsernameChanged = onOwnerUsernameChanged,
                onOwnerApplySuggestedUsername = onOwnerApplySuggestedUsername,
                onOwnerCountrySelected = onOwnerCountrySelected,
                onOwnerPhoneNumberChanged = onOwnerPhoneNumberChanged,
                onOwnerGenderSelected = onOwnerGenderSelected,
                onOwnerDateOfBirthSelected = onOwnerDateOfBirthSelected,
                onOwnerBioChanged = onOwnerBioChanged,
                onOwnerAvatarUriPicked = onOwnerAvatarUriPicked,
                onOwnerCancelCrop = onOwnerCancelCrop,
                onOwnerConfirmCrop = onOwnerConfirmCrop,
                onOwnerSaveProfileEdits = onOwnerSaveProfileEdits,
                onOwnerClearStatusBanner = onOwnerClearStatusBanner,
                onOpenSettingsAndPrivacy = onOpenSettingsAndPrivacy,
                onOpenOwnerMenu = onOpenOwnerMenu,
                onCloseOwnerMenu = onCloseOwnerMenu
            )
        } else {
            MobileAndTabletHomeLayout(
                uiState = uiState,
                selectedTab = selectedTab,
                circleTint = circleTint,
                placeholderColor = placeholderColor,
                onSelectTab = { tab ->
                    onSelectTab(tab)
                    if (tab == HomeNavTab.POST) {
                        activeComingSoonFeature = postLabel
                    } else {
                        activeComingSoonFeature = null
                    }
                },
                onSearchClicked = { activeComingSoonFeature = searchLabel },
                onChatSearchQueryChanged = onChatSearchQueryChanged,
                onMoreOptionsClicked = onOpenPrivacySettings,
                onStartConversationClicked = { onSelectTab(HomeNavTab.CONTACTS) },
                onOpenOneToOneChat = onOpenOneToOneChat,
                onContactsFilterSelected = onContactsFilterSelected,
                onToggleContactsSearch = onToggleContactsSearch,
                onContactsSearchQueryChanged = onContactsSearchQueryChanged,
                onContactsPermissionResult = onContactsPermissionResult,
                onToggleFollowUser = onToggleFollowUser,
                onOpenUserProfile = onOpenUserProfile,
                onLoadMoreContacts = onLoadMoreContacts,
                onRefreshContacts = onRefreshContacts,
                onDismissContactsError = onDismissContactsError,
                onOwnerPullToRefresh = onOwnerPullToRefresh,
                onOwnerSelectSubTab = onOwnerSelectSubTab,
                onOwnerOpenFollowList = onOwnerOpenFollowList,
                onOwnerLoadMoreFollowList = onOwnerLoadMoreFollowList,
                onOwnerDismissFollowList = onOwnerDismissFollowList,
                onOwnerOpenEditProfile = onOwnerOpenEditProfile,
                onOwnerDismissEditProfile = onOwnerDismissEditProfile,
                onOwnerFullNameChanged = onOwnerFullNameChanged,
                onOwnerUsernameChanged = onOwnerUsernameChanged,
                onOwnerApplySuggestedUsername = onOwnerApplySuggestedUsername,
                onOwnerCountrySelected = onOwnerCountrySelected,
                onOwnerPhoneNumberChanged = onOwnerPhoneNumberChanged,
                onOwnerGenderSelected = onOwnerGenderSelected,
                onOwnerDateOfBirthSelected = onOwnerDateOfBirthSelected,
                onOwnerBioChanged = onOwnerBioChanged,
                onOwnerAvatarUriPicked = onOwnerAvatarUriPicked,
                onOwnerCancelCrop = onOwnerCancelCrop,
                onOwnerConfirmCrop = onOwnerConfirmCrop,
                onOwnerSaveProfileEdits = onOwnerSaveProfileEdits,
                onOwnerClearStatusBanner = onOwnerClearStatusBanner,
                onOpenSettingsAndPrivacy = onOpenSettingsAndPrivacy,
                onOpenOwnerMenu = onOpenOwnerMenu,
                onCloseOwnerMenu = onCloseOwnerMenu
            )
        }

        // Animated Coming Soon Banner / Toast Sheet for Search, 3-Dots, Start Conversation, Post
        AnimatedVisibility(
            visible = activeComingSoonFeature != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp, start = 16.dp, end = 16.dp)
        ) {
            val featureName = activeComingSoonFeature ?: comingSoonTitle
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, TelefamRed.copy(alpha = 0.25f)),
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .testTag("coming_soon_popup_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(circleTint),
                            contentAlignment = Alignment.Center
                        ) {
                            TelefamChatBubblesIllustration(
                                modifier = Modifier.size(18.dp),
                                tint = TelefamRed
                            )
                        }
                        Column {
                            Text(
                                text = comingSoonTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = TelefamRed
                            )
                            Text(
                                text = stringResource(R.string.coming_soon_desc, featureName),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { activeComingSoonFeature = null },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("dismiss_coming_soon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.crop_cancel_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileAndTabletHomeLayout(
    uiState: TelefamUiState,
    selectedTab: HomeNavTab,
    circleTint: Color,
    placeholderColor: Color,
    onSelectTab: (HomeNavTab) -> Unit,
    onSearchClicked: () -> Unit,
    onChatSearchQueryChanged: (String) -> Unit,
    onMoreOptionsClicked: () -> Unit,
    onStartConversationClicked: () -> Unit,
    onOpenOneToOneChat: (String) -> Unit,
    onContactsFilterSelected: (ContactsFilterMode) -> Unit,
    onToggleContactsSearch: (Boolean) -> Unit,
    onContactsSearchQueryChanged: (String) -> Unit,
    onContactsPermissionResult: (Boolean) -> Unit,
    onToggleFollowUser: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onLoadMoreContacts: () -> Unit,
    onRefreshContacts: () -> Unit,
    onDismissContactsError: () -> Unit,
    onOwnerPullToRefresh: () -> Unit,
    onOwnerSelectSubTab: (OwnerProfileSubTab) -> Unit,
    onOwnerOpenFollowList: (FollowListType) -> Unit,
    onOwnerLoadMoreFollowList: () -> Unit,
    onOwnerDismissFollowList: () -> Unit,
    onOwnerOpenEditProfile: () -> Unit,
    onOwnerDismissEditProfile: () -> Unit,
    onOwnerFullNameChanged: (String) -> Unit,
    onOwnerUsernameChanged: (String) -> Unit,
    onOwnerApplySuggestedUsername: (String) -> Unit,
    onOwnerCountrySelected: (CountryDialCode) -> Unit,
    onOwnerPhoneNumberChanged: (String) -> Unit,
    onOwnerGenderSelected: (String) -> Unit,
    onOwnerDateOfBirthSelected: (String) -> Unit,
    onOwnerBioChanged: (String) -> Unit,
    onOwnerAvatarUriPicked: (Uri) -> Unit,
    onOwnerCancelCrop: () -> Unit,
    onOwnerConfirmCrop: (scale: Float, offsetXNorm: Float, offsetYNorm: Float, rotationDeg: Float) -> Unit,
    onOwnerSaveProfileEdits: (String) -> Unit,
    onOwnerClearStatusBanner: () -> Unit,
    onOpenSettingsAndPrivacy: () -> Unit = {},
    onOpenOwnerMenu: () -> Unit = {},
    onCloseOwnerMenu: () -> Unit = {}
) {
    val isFullEdgeTab = selectedTab == HomeNavTab.CONTACTS || selectedTab == HomeNavTab.PROFILE
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFullEdgeTab) MaterialTheme.colorScheme.surface else TelefamRed)
    ) {
        if (!isFullEdgeTab) {
            // 1. Slim Red Top Header with Slim Search Bar & 3-Dots Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding( start = 16.dp, end = 8.dp, top = 6.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Slim Pill Search Bar (Real live Room filter on CHATS tab)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .testTag("home_search_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_chats_placeholder),
                            tint = placeholderColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.chatListSearchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_chats_placeholder),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = placeholderColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = uiState.chatListSearchQuery,
                                onValueChange = onChatSearchQueryChanged,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(TelefamRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_chats_search_input")
                            )
                        }
                        if (uiState.chatListSearchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.crop_cancel_button),
                                tint = placeholderColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onChatSearchQueryChanged("") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 3-Dots Vertical Menu Button
                IconButton(
                    onClick = onMoreOptionsClicked,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("home_more_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.menu_options),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Rounded Main Sheet Area + Bottom Navigation Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = if (isFullEdgeTab) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main Tab Content
                val useZeroHorizontalPadding = isFullEdgeTab || selectedTab == HomeNavTab.CHATS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = if (useZeroHorizontalPadding) 0.dp else 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HomeTabBodyContent(
                        uiState = uiState,
                        selectedTab = selectedTab,
                        circleTint = circleTint,
                        onStartConversationClicked = onStartConversationClicked,
                        onOpenOneToOneChat = onOpenOneToOneChat,
                        onContactsFilterSelected = onContactsFilterSelected,
                        onToggleContactsSearch = onToggleContactsSearch,
                        onContactsSearchQueryChanged = onContactsSearchQueryChanged,
                        onContactsPermissionResult = onContactsPermissionResult,
                        onToggleFollowUser = onToggleFollowUser,
                        onOpenUserProfile = onOpenUserProfile,
                        onLoadMoreContacts = onLoadMoreContacts,
                        onRefreshContacts = onRefreshContacts,
                        onDismissContactsError = onDismissContactsError,
                        onOwnerPullToRefresh = onOwnerPullToRefresh,
                        onOwnerSelectSubTab = onOwnerSelectSubTab,
                        onOwnerOpenFollowList = onOwnerOpenFollowList,
                        onOwnerLoadMoreFollowList = onOwnerLoadMoreFollowList,
                        onOwnerDismissFollowList = onOwnerDismissFollowList,
                        onOwnerOpenEditProfile = onOwnerOpenEditProfile,
                        onOwnerDismissEditProfile = onOwnerDismissEditProfile,
                        onOwnerFullNameChanged = onOwnerFullNameChanged,
                        onOwnerUsernameChanged = onOwnerUsernameChanged,
                        onOwnerApplySuggestedUsername = onOwnerApplySuggestedUsername,
                        onOwnerCountrySelected = onOwnerCountrySelected,
                        onOwnerPhoneNumberChanged = onOwnerPhoneNumberChanged,
                        onOwnerGenderSelected = onOwnerGenderSelected,
                        onOwnerDateOfBirthSelected = onOwnerDateOfBirthSelected,
                        onOwnerBioChanged = onOwnerBioChanged,
                        onOwnerAvatarUriPicked = onOwnerAvatarUriPicked,
                        onOwnerCancelCrop = onOwnerCancelCrop,
                        onOwnerConfirmCrop = onOwnerConfirmCrop,
                        onOwnerSaveProfileEdits = onOwnerSaveProfileEdits,
                        onOwnerClearStatusBanner = onOwnerClearStatusBanner,
                        onOpenSettingsAndPrivacy = onOpenSettingsAndPrivacy,
                        onOpenOwnerMenu = onOpenOwnerMenu,
                        onCloseOwnerMenu = onCloseOwnerMenu
                    )
                }

                // Bottom Navigation Bar with Floating Red Center Post Button & Offline Local Coil Avatar
                if (!(selectedTab == HomeNavTab.PROFILE && uiState.isOwnerMenuOpen)) {
                    TelefamBottomNavigationBar(
                        uiState = uiState,
                        selectedTab = selectedTab,
                        onSelectTab = onSelectTab
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopHomeLayout(
    uiState: TelefamUiState,
    selectedTab: HomeNavTab,
    circleTint: Color,
    placeholderColor: Color,
    onSelectTab: (HomeNavTab) -> Unit,
    onSearchClicked: () -> Unit,
    onChatSearchQueryChanged: (String) -> Unit,
    onMoreOptionsClicked: () -> Unit,
    onStartConversationClicked: () -> Unit,
    onOpenOneToOneChat: (String) -> Unit,
    onContactsFilterSelected: (ContactsFilterMode) -> Unit,
    onToggleContactsSearch: (Boolean) -> Unit,
    onContactsSearchQueryChanged: (String) -> Unit,
    onContactsPermissionResult: (Boolean) -> Unit,
    onToggleFollowUser: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onLoadMoreContacts: () -> Unit,
    onRefreshContacts: () -> Unit,
    onDismissContactsError: () -> Unit,
    onOwnerPullToRefresh: () -> Unit,
    onOwnerSelectSubTab: (OwnerProfileSubTab) -> Unit,
    onOwnerOpenFollowList: (FollowListType) -> Unit,
    onOwnerLoadMoreFollowList: () -> Unit,
    onOwnerDismissFollowList: () -> Unit,
    onOwnerOpenEditProfile: () -> Unit,
    onOwnerDismissEditProfile: () -> Unit,
    onOwnerFullNameChanged: (String) -> Unit,
    onOwnerUsernameChanged: (String) -> Unit,
    onOwnerApplySuggestedUsername: (String) -> Unit,
    onOwnerCountrySelected: (CountryDialCode) -> Unit,
    onOwnerPhoneNumberChanged: (String) -> Unit,
    onOwnerGenderSelected: (String) -> Unit,
    onOwnerDateOfBirthSelected: (String) -> Unit,
    onOwnerBioChanged: (String) -> Unit,
    onOwnerAvatarUriPicked: (Uri) -> Unit,
    onOwnerCancelCrop: () -> Unit,
    onOwnerConfirmCrop: (scale: Float, offsetXNorm: Float, offsetYNorm: Float, rotationDeg: Float) -> Unit,
    onOwnerSaveProfileEdits: (String) -> Unit,
    onOwnerClearStatusBanner: () -> Unit,
    onOpenSettingsAndPrivacy: () -> Unit = {},
    onOpenOwnerMenu: () -> Unit = {},
    onCloseOwnerMenu: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TelefamRed)
    ) {
        // Slim Desktop Red Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Brand Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelefamPaperPlaneLogo(size = 30.dp, animateFloatPulse = false)
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color.White
                )
            }

            // Center: Slim Search Bar
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 520.dp)
                    .weight(1f, fill = false)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .testTag("home_search_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_chats_placeholder),
                        tint = placeholderColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.chatListSearchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_chats_placeholder),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = placeholderColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = uiState.chatListSearchQuery,
                            onValueChange = onChatSearchQueryChanged,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(TelefamRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_chats_search_input")
                        )
                    }
                }
            }

            // Right: User Avatar + 3-Dots Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UserOfflineAvatarBadge(
                    uiState = uiState,
                    size = 30.dp,
                    isSelected = selectedTab == HomeNavTab.PROFILE,
                    selectedBorderColor = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onSelectTab(HomeNavTab.PROFILE) }
                )
                IconButton(
                    onClick = onMoreOptionsClicked,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("home_more_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.menu_options),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Desktop Main Rounded Workspace with Left Navigation Rail & Main Content Canvas
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Desktop Sidebar Navigation
                Column(
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Prominent Floating-Style Red Post Button on Desktop
                        Button(
                            onClick = { onSelectTab(HomeNavTab.POST) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TelefamRed,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("nav_tab_post")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.nav_post),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        DesktopNavRow(
                            label = stringResource(R.string.nav_chats),
                            icon = if (selectedTab == HomeNavTab.CHATS) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                            selected = selectedTab == HomeNavTab.CHATS,
                            circleTint = circleTint,
                            testTag = "nav_tab_chats",
                            onClick = { onSelectTab(HomeNavTab.CHATS) }
                        )

                        DesktopNavRow(
                            label = stringResource(R.string.nav_contacts),
                            icon = if (selectedTab == HomeNavTab.CONTACTS) Icons.Filled.People else Icons.Outlined.PeopleOutline,
                            selected = selectedTab == HomeNavTab.CONTACTS,
                            circleTint = circleTint,
                            testTag = "nav_tab_contacts",
                            onClick = { onSelectTab(HomeNavTab.CONTACTS) }
                        )

                        DesktopNavRow(
                            label = stringResource(R.string.nav_feeds),
                            icon = if (selectedTab == HomeNavTab.FEEDS) Icons.Filled.DynamicFeed else Icons.Outlined.DynamicFeed,
                            selected = selectedTab == HomeNavTab.FEEDS,
                            circleTint = circleTint,
                            testTag = "nav_tab_feeds",
                            onClick = { onSelectTab(HomeNavTab.FEEDS) }
                        )
                    }

                    // Bottom Desktop Profile Card (with Offline Local Coil Avatar)
                    Surface(
                        onClick = { onSelectTab(HomeNavTab.PROFILE) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedTab == HomeNavTab.PROFILE) circleTint else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = if (selectedTab == HomeNavTab.PROFILE) BorderStroke(1.dp, TelefamRed.copy(alpha = 0.4f)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nav_tab_profile")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            UserOfflineAvatarBadge(
                                uiState = uiState,
                                size = 36.dp,
                                isSelected = selectedTab == HomeNavTab.PROFILE,
                                selectedBorderColor = TelefamRed
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                val displayName = uiState.savedProfile?.fullName?.ifBlank { null }
                                    ?: uiState.fullName.ifBlank { stringResource(R.string.nav_profile) }
                                val displayUsername = uiState.savedProfile?.username?.ifBlank { null }
                                    ?: uiState.username
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedTab == HomeNavTab.PROFILE) TelefamRed else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (displayUsername.isNotBlank()) {
                                    Text(
                                        text = "@$displayUsername",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // Right Main Desktop Content Canvas
                val isEdgeDesktopTab = selectedTab == HomeNavTab.CONTACTS || selectedTab == HomeNavTab.PROFILE || selectedTab == HomeNavTab.CHATS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(if (isEdgeDesktopTab) 0.dp else 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HomeTabBodyContent(
                        uiState = uiState,
                        selectedTab = selectedTab,
                        circleTint = circleTint,
                        onStartConversationClicked = onStartConversationClicked,
                        onOpenOneToOneChat = onOpenOneToOneChat,
                        onContactsFilterSelected = onContactsFilterSelected,
                        onToggleContactsSearch = onToggleContactsSearch,
                        onContactsSearchQueryChanged = onContactsSearchQueryChanged,
                        onContactsPermissionResult = onContactsPermissionResult,
                        onToggleFollowUser = onToggleFollowUser,
                        onOpenUserProfile = onOpenUserProfile,
                        onLoadMoreContacts = onLoadMoreContacts,
                        onRefreshContacts = onRefreshContacts,
                        onDismissContactsError = onDismissContactsError,
                        onOwnerPullToRefresh = onOwnerPullToRefresh,
                        onOwnerSelectSubTab = onOwnerSelectSubTab,
                        onOwnerOpenFollowList = onOwnerOpenFollowList,
                        onOwnerLoadMoreFollowList = onOwnerLoadMoreFollowList,
                        onOwnerDismissFollowList = onOwnerDismissFollowList,
                        onOwnerOpenEditProfile = onOwnerOpenEditProfile,
                        onOwnerDismissEditProfile = onOwnerDismissEditProfile,
                        onOwnerFullNameChanged = onOwnerFullNameChanged,
                        onOwnerUsernameChanged = onOwnerUsernameChanged,
                        onOwnerApplySuggestedUsername = onOwnerApplySuggestedUsername,
                        onOwnerCountrySelected = onOwnerCountrySelected,
                        onOwnerPhoneNumberChanged = onOwnerPhoneNumberChanged,
                        onOwnerGenderSelected = onOwnerGenderSelected,
                        onOwnerDateOfBirthSelected = onOwnerDateOfBirthSelected,
                        onOwnerBioChanged = onOwnerBioChanged,
                        onOwnerAvatarUriPicked = onOwnerAvatarUriPicked,
                        onOwnerCancelCrop = onOwnerCancelCrop,
                        onOwnerConfirmCrop = onOwnerConfirmCrop,
                        onOwnerSaveProfileEdits = onOwnerSaveProfileEdits,
                        onOwnerClearStatusBanner = onOwnerClearStatusBanner,
                        onOpenSettingsAndPrivacy = onOpenSettingsAndPrivacy,
                        onOpenOwnerMenu = onOpenOwnerMenu,
                        onCloseOwnerMenu = onCloseOwnerMenu
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopNavRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    circleTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) circleTint else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) TelefamRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = if (selected) TelefamRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeTabBodyContent(
    uiState: TelefamUiState,
    selectedTab: HomeNavTab,
    circleTint: Color,
    onStartConversationClicked: () -> Unit,
    onOpenOneToOneChat: (String) -> Unit = {},
    onContactsFilterSelected: (ContactsFilterMode) -> Unit,
    onToggleContactsSearch: (Boolean) -> Unit,
    onContactsSearchQueryChanged: (String) -> Unit,
    onContactsPermissionResult: (Boolean) -> Unit,
    onToggleFollowUser: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onLoadMoreContacts: () -> Unit,
    onRefreshContacts: () -> Unit,
    onDismissContactsError: () -> Unit,
    onOwnerPullToRefresh: () -> Unit,
    onOwnerSelectSubTab: (OwnerProfileSubTab) -> Unit,
    onOwnerOpenFollowList: (FollowListType) -> Unit,
    onOwnerLoadMoreFollowList: () -> Unit,
    onOwnerDismissFollowList: () -> Unit,
    onOwnerOpenEditProfile: () -> Unit,
    onOwnerDismissEditProfile: () -> Unit,
    onOwnerFullNameChanged: (String) -> Unit,
    onOwnerUsernameChanged: (String) -> Unit,
    onOwnerApplySuggestedUsername: (String) -> Unit,
    onOwnerCountrySelected: (CountryDialCode) -> Unit,
    onOwnerPhoneNumberChanged: (String) -> Unit,
    onOwnerGenderSelected: (String) -> Unit,
    onOwnerDateOfBirthSelected: (String) -> Unit,
    onOwnerBioChanged: (String) -> Unit,
    onOwnerAvatarUriPicked: (Uri) -> Unit,
    onOwnerCancelCrop: () -> Unit,
    onOwnerConfirmCrop: (scale: Float, offsetXNorm: Float, offsetYNorm: Float, rotationDeg: Float) -> Unit,
    onOwnerSaveProfileEdits: (String) -> Unit,
    onOwnerClearStatusBanner: () -> Unit,
    onOpenSettingsAndPrivacy: () -> Unit = {},
    onOpenOwnerMenu: () -> Unit = {},
    onCloseOwnerMenu: () -> Unit = {}
) {
    when (selectedTab) {
        HomeNavTab.CHATS -> {
            OneToOneChatsListSection(
                chats = uiState.chatListItems,
                searchQuery = uiState.chatListSearchQuery,
                circleTint = circleTint,
                onOpenChat = onOpenOneToOneChat,
                onStartChatClick = onStartConversationClicked,
                emptyStateIllustration = {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(circleTint),
                        contentAlignment = Alignment.Center
                    ) {
                        TelefamChatBubblesIllustration(
                            modifier = Modifier.size(40.dp),
                            tint = TelefamRed
                        )
                    }
                }
            )
        }

        HomeNavTab.CONTACTS -> {
            ContactsScreen(
                users = uiState.contactsUsers,
                searchQuery = uiState.contactsSearchQuery,
                filterMode = uiState.contactsFilterMode,
                hasMorePages = uiState.contactsHasMorePages,
                hasGrantedContactsPermission = uiState.hasContactsPermission,
                syncedContactsHashesCount = uiState.contactsMatchCount,
                isOffline = uiState.isContactsOfflineBannerVisible,
                onSearchQueryChange = onContactsSearchQueryChanged,
                onFilterModeChange = onContactsFilterSelected,
                onToggleFollowClick = onToggleFollowUser,
                onUserRowClick = onOpenUserProfile,
                onLoadNextPage = onLoadMoreContacts,
                onRefreshClick = onRefreshContacts,
                onContactsPermissionResult = onContactsPermissionResult
            )
        }

        HomeNavTab.PROFILE -> {
            OwnerProfileScreen(
                uiState = uiState,
                onPullToRefresh = onOwnerPullToRefresh,
                onSelectSubTab = onOwnerSelectSubTab,
                onOpenFollowList = onOwnerOpenFollowList,
                onLoadMoreFollowList = onOwnerLoadMoreFollowList,
                onDismissFollowList = onOwnerDismissFollowList,
                onToggleFollowUser = onToggleFollowUser,
                onOpenOtherUserProfile = onOpenUserProfile,
                onOpenEditProfile = onOwnerOpenEditProfile,
                onDismissEditProfile = onOwnerDismissEditProfile,
                onFullNameChanged = onOwnerFullNameChanged,
                onUsernameChanged = onOwnerUsernameChanged,
                onApplySuggestedUsername = onOwnerApplySuggestedUsername,
                onCountrySelected = onOwnerCountrySelected,
                onPhoneNumberChanged = onOwnerPhoneNumberChanged,
                onGenderSelected = onOwnerGenderSelected,
                onDateOfBirthSelected = onOwnerDateOfBirthSelected,
                onBioChanged = onOwnerBioChanged,
                onAvatarUriPicked = onOwnerAvatarUriPicked,
                onCancelCrop = onOwnerCancelCrop,
                onConfirmCrop = onOwnerConfirmCrop,
                onSaveOwnerProfileEdits = onOwnerSaveProfileEdits,
                onClearStatusBanner = onOwnerClearStatusBanner,
                onOpenSettingsAndPrivacy = onOpenSettingsAndPrivacy,
                onOpenOwnerMenu = onOpenOwnerMenu,
                onCloseOwnerMenu = onCloseOwnerMenu
            )
        }

        HomeNavTab.POST,
        HomeNavTab.FEEDS -> {
            val tabTitle = when (selectedTab) {
                HomeNavTab.CONTACTS -> stringResource(R.string.nav_contacts)
                HomeNavTab.POST -> stringResource(R.string.nav_post)
                HomeNavTab.FEEDS -> stringResource(R.string.nav_feeds)
                HomeNavTab.PROFILE -> stringResource(R.string.nav_profile)
                HomeNavTab.CHATS -> stringResource(R.string.nav_chats)
            }
            val tabIcon = when (selectedTab) {
                HomeNavTab.CONTACTS -> Icons.Outlined.PeopleOutline
                HomeNavTab.POST -> Icons.Default.Add
                HomeNavTab.FEEDS -> Icons.Outlined.DynamicFeed
                HomeNavTab.PROFILE -> Icons.Default.Person
                HomeNavTab.CHATS -> Icons.Outlined.ChatBubbleOutline
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .testTag("home_coming_soon_section"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (selectedTab == HomeNavTab.PROFILE) {
                    UserOfflineAvatarBadge(
                        uiState = uiState,
                        size = 84.dp,
                        isSelected = true,
                        selectedBorderColor = TelefamRed
                    )
                    val fullName = uiState.savedProfile?.fullName?.ifBlank { null } ?: uiState.fullName
                    val username = uiState.savedProfile?.username?.ifBlank { null } ?: uiState.username
                    if (fullName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = fullName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (username.isNotBlank()) {
                        Text(
                            text = "@$username",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = TelefamRed
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(circleTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tabIcon,
                            contentDescription = tabTitle,
                            tint = TelefamRed,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = circleTint
                ) {
                    Text(
                        text = stringResource(R.string.coming_soon_title),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = TelefamRed,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = tabTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.coming_soon_desc, tabTitle),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Custom Bottom Navigation Bar matching the uploaded image:
 * - Chats | Contacts | Floating Red '+' Post in Middle | Feeds | Profile (with real local Coil avatar)
 */
@Composable
private fun TelefamBottomNavigationBar(
    uiState: TelefamUiState,
    selectedTab: HomeNavTab,
    onSelectTab: (HomeNavTab) -> Unit
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .testTag("home_bottom_navigation_bar")
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1. Chats
            BottomNavStandardItem(
                label = stringResource(R.string.nav_chats),
                icon = if (selectedTab == HomeNavTab.CHATS) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                selected = selectedTab == HomeNavTab.CHATS,
                inactiveColor = inactiveColor,
                testTag = "nav_tab_chats",
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(HomeNavTab.CHATS) }
            )

            // 2. Contacts
            BottomNavStandardItem(
                label = stringResource(R.string.nav_contacts),
                icon = if (selectedTab == HomeNavTab.CONTACTS) Icons.Filled.People else Icons.Outlined.PeopleOutline,
                selected = selectedTab == HomeNavTab.CONTACTS,
                inactiveColor = inactiveColor,
                testTag = "nav_tab_contacts",
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(HomeNavTab.CONTACTS) }
            )

            // 3. Post (Floating Red Circular Button in Middle)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelectTab(HomeNavTab.POST) }
                    )
                    .testTag("nav_tab_post"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = (-12).dp)
                        .size(48.dp)
                        .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(TelefamRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.nav_post),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.nav_post),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == HomeNavTab.POST) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selectedTab == HomeNavTab.POST) TelefamRed else inactiveColor,
                    modifier = Modifier.offset(y = (-6).dp),
                    maxLines = 1
                )
            }

            // 4. Feeds
            BottomNavStandardItem(
                label = stringResource(R.string.nav_feeds),
                icon = if (selectedTab == HomeNavTab.FEEDS) Icons.Filled.DynamicFeed else Icons.Outlined.DynamicFeed,
                selected = selectedTab == HomeNavTab.FEEDS,
                inactiveColor = inactiveColor,
                testTag = "nav_tab_feeds",
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(HomeNavTab.FEEDS) }
            )

            // 5. Profile (Real User Avatar saved locally via Coil for offline)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelectTab(HomeNavTab.PROFILE) }
                    )
                    .padding(vertical = 6.dp)
                    .testTag("nav_tab_profile"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                UserOfflineAvatarBadge(
                    uiState = uiState,
                    size = 24.dp,
                    isSelected = selectedTab == HomeNavTab.PROFILE,
                    selectedBorderColor = TelefamRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.nav_profile),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == HomeNavTab.PROFILE) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selectedTab == HomeNavTab.PROFILE) TelefamRed else inactiveColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BottomNavStandardItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    inactiveColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint = if (selected) TelefamRed else inactiveColor
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = tint,
            maxLines = 1
        )
    }
}

/**
 * Loads the user's real avatar using Coil from locally saved internal storage (`avatarLocalPath`)
 * with full disk & memory cache enabled so it renders 100% offline.
 */
@Composable
fun UserOfflineAvatarBadge(
    uiState: TelefamUiState,
    size: Dp,
    isSelected: Boolean,
    selectedBorderColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localPath = uiState.savedProfile?.avatarLocalPath ?: uiState.sanitizedAvatar?.secureFilePath
    val cdnUrl = uiState.savedProfile?.avatarCdnUrl ?: uiState.sanitizedAvatar?.cdnStorageUrl
    val localFile = remember(localPath) {
        localPath?.let { File(it) }?.takeIf { it.exists() }
    }

    val modelData: Any? = localFile ?: cdnUrl?.takeIf { it.isNotBlank() }
    val borderMod = if (isSelected) {
        Modifier.border(1.8.dp, selectedBorderColor, CircleShape)
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), CircleShape)
    }

    Box(
        modifier = modifier
            .size(size)
            .then(borderMod)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (modelData != null) {
            val imageRequest = remember(modelData) {
                ImageRequest.Builder(context)
                    .data(modelData)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.nav_profile),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = stringResource(R.string.nav_profile),
                tint = TelefamRed,
                modifier = Modifier.size(size * 0.62f)
            )
        }
    }
}

/**
 * Custom vector illustration matching the Telefam Chat Bubbles icon in the center of the Home screen.
 */
@Composable
private fun TelefamChatBubblesIllustration(
    modifier: Modifier = Modifier,
    tint: Color = TelefamRed
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.075f

        // Primary rounded chat bubble
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.10f, h * 0.14f),
            size = Size(w * 0.68f, h * 0.54f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Chat bubble tail on bottom-left
        val tailPath = Path().apply {
            moveTo(w * 0.22f, h * 0.68f)
            lineTo(w * 0.14f, h * 0.84f)
            lineTo(w * 0.34f, h * 0.68f)
        }
        drawPath(
            path = tailPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Two subtle message lines inside primary bubble
        drawLine(
            color = tint,
            start = Offset(w * 0.26f, h * 0.35f),
            end = Offset(w * 0.62f, h * 0.35f),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.26f, h * 0.48f),
            end = Offset(w * 0.50f, h * 0.48f),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )

        // Secondary overlapping chat bubble accent on bottom-right
        drawRoundRect(
            color = tint.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.42f, h * 0.44f),
            size = Size(w * 0.48f, h * 0.38f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
            style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
