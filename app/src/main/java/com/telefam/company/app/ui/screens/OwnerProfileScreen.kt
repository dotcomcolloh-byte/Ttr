package com.telefam.company.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.TelefamError
import com.example.ui.theme.TelefamInputBorderDark
import com.example.ui.theme.TelefamInputBorderLight
import com.example.ui.theme.TelefamRed
import com.example.ui.theme.isTelefamDarkTheme
import com.telefam.company.app.domain.CountryDialCode
import com.telefam.company.app.domain.FollowListType
import com.telefam.company.app.domain.OwnerProfileSubTab
import com.telefam.company.app.domain.TelefamNumberFormatter
import com.telefam.company.app.domain.UserPostItem
import com.telefam.company.app.ui.TelefamUiState
import com.telefam.company.app.ui.components.ImageCropperDialog
import com.telefam.company.app.ui.components.KenyaFlagVectorIcon
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Production-ready Owner Profile Screen matching the uploaded reference image
 * (`file_00000000d8d88208a1661f3fd477a9ac (1).png`):
 * - Top bar with centered `@username` and 3-dots menu button ("Coming Soon")
 * - Pull-down to refresh (`PullToRefreshBox`) + real-time reactive Room updates without manual refresh
 * - Left circular profile photo cached locally (`avatarLocalPath`) + `+` badge for quick photo crop/update
 * - Right header with Full Name, `@username`, and multi-line Bio
 * - Clickable Followers, Following, and Subscribers counts opening paginated bottom sheets
 *   with Follow Back, Friends, and Unfollow actions
 * - "Edit profile" button opening the full Owner Profile Editor (with photo cropping, upload sanitization,
 *   7-day name change cooldown, 30-day unique username cooldown, phone number, bio, date of birth, gender)
 * - "Settings" button ("Coming Soon")
 * - 4 Sub-tabs (Posts grid, Saved bookmark, Reshared arrows, Private lock) with red active underline
 * - 3-column media grid with top-right duration pill (`02:14`) and bottom-left play + view count (`12.4K`)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerProfileScreen(
    uiState: TelefamUiState,
    onPullToRefresh: () -> Unit,
    onSelectSubTab: (OwnerProfileSubTab) -> Unit,
    onOpenFollowList: (FollowListType) -> Unit,
    onLoadMoreFollowList: () -> Unit,
    onDismissFollowList: () -> Unit,
    onToggleFollowUser: (String) -> Unit,
    onOpenOtherUserProfile: (String) -> Unit,
    onOpenEditProfile: () -> Unit,
    onDismissEditProfile: () -> Unit,
    onFullNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onApplySuggestedUsername: (String) -> Unit,
    onCountrySelected: (CountryDialCode) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onGenderSelected: (String) -> Unit,
    onDateOfBirthSelected: (String) -> Unit,
    onBioChanged: (String) -> Unit,
    onAvatarUriPicked: (Uri) -> Unit,
    onCancelCrop: () -> Unit,
    onConfirmCrop: (scale: Float, offsetXNorm: Float, offsetYNorm: Float, rotationDeg: Float) -> Unit,
    onSaveOwnerProfileEdits: (String) -> Unit,
    onClearStatusBanner: () -> Unit,
    onOpenSettingsAndPrivacy: () -> Unit = {},
    onOpenOwnerMenu: () -> Unit = {},
    onCloseOwnerMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isTelefamDarkTheme()
    val pageBg = if (isDark) MaterialTheme.colorScheme.background else Color.White
    val pillButtonBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF2F3F5)
    val pillButtonText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1C1E21)

    val savedProfile = uiState.savedProfile
    val displayFullName = savedProfile?.fullName?.takeIf { it.isNotBlank() }
        ?: uiState.fullName.takeIf { it.isNotBlank() }
        ?: uiState.activeSession?.email?.substringBefore('@').orEmpty()
    val displayUsername = savedProfile?.username?.takeIf { it.isNotBlank() }
        ?: uiState.username.takeIf { it.isNotBlank() }
        ?: uiState.activeSession?.email?.substringBefore('@').orEmpty()
    val displayBio = savedProfile?.bio?.takeIf { it.isNotBlank() }
        ?: uiState.bio

    val comingSoonTitle = stringResource(R.string.coming_soon_title)
    val settingsLabel = stringResource(R.string.owner_profile_settings_button)
    val menuLabel = stringResource(R.string.owner_menu_title)
    val postsLabel = stringResource(R.string.owner_profile_tab_posts)
    val savedConfirmText = stringResource(R.string.owner_profile_saved_confirmation)

    var localComingSoonBanner by rememberSaveable { mutableStateOf<String?>(null) }
    var localOwnerMenuOpen by rememberSaveable { mutableStateOf(false) }

    if (uiState.isOwnerMenuOpen || localOwnerMenuOpen) {
        OwnerMenuScreen(
            uiState = uiState,
            onBackClick = {
                localOwnerMenuOpen = false
                onCloseOwnerMenu()
            },
            modifier = modifier
        )
        return
    }

    val quickAvatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onOpenEditProfile()
            onAvatarUriPicked(uri)
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isOwnerProfileRefreshing,
        onRefresh = onPullToRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
            .testTag("owner_profile_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                // 1. Top Bar: Centered @username + Menu button on right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (displayUsername.isNotBlank()) "@$displayUsername" else "",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("owner_profile_top_username")
                    )

                    IconButton(
                        onClick = {
                            localOwnerMenuOpen = true
                            onOpenOwnerMenu()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(48.dp)
                            .testTag("owner_profile_top_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = menuLabel,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Status Banner
                val bannerText = uiState.ownerStatusBanner ?: localComingSoonBanner
                AnimatedVisibility(visible = bannerText != null) {
                    Surface(
                        color = TelefamRed.copy(alpha = 0.09f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("owner_profile_status_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TelefamRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = bannerText.orEmpty(),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (bannerText != null) {
                                IconButton(
                                    onClick = {
                                        localComingSoonBanner = null
                                        onClearStatusBanner()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.crop_cancel_button),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Profile Identity Header (Left: Circular Photo + '+' badge, Right: Full Name, @username, Bio)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Avatar with '+' badge
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clickable {
                                quickAvatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("owner_profile_avatar_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        val localPath = savedProfile?.avatarLocalPath
                        val hasLocalFile = remember(localPath) {
                            !localPath.isNullOrBlank() && File(localPath).exists()
                        }

                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                    shape = CircleShape
                                )
                                .background(TelefamRed.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasLocalFile && localPath != null) {
                                AsyncImage(
                                    model = File(localPath),
                                    contentDescription = displayFullName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = displayFullName.trim().take(1).uppercase(),
                                    color = TelefamRed,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Small Red '+' Badge at bottom-right matching reference image
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-2).dp, y = (-2).dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(TelefamRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.upload_profile_photo),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Name, @username, Bio
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displayFullName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("owner_profile_full_name")
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "@$displayUsername",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("owner_profile_handle")
                        )

                        if (displayBio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = displayBio,
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("owner_profile_bio")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Clickable Stats Row: Followers | Following | Subscribers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OwnerStatColumn(
                        countText = TelefamNumberFormatter.formatCompactCount(uiState.ownerFollowersCount),
                        label = stringResource(R.string.profile_stat_followers),
                        onClick = { onOpenFollowList(FollowListType.FOLLOWERS) },
                        testTag = "owner_stat_followers_button",
                        modifier = Modifier.weight(1f)
                    )

                    VerticalDivider(
                        modifier = Modifier.height(28.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )

                    OwnerStatColumn(
                        countText = TelefamNumberFormatter.formatCompactCount(uiState.ownerFollowingCount),
                        label = stringResource(R.string.profile_stat_following),
                        onClick = { onOpenFollowList(FollowListType.FOLLOWING) },
                        testTag = "owner_stat_following_button",
                        modifier = Modifier.weight(1f)
                    )

                    VerticalDivider(
                        modifier = Modifier.height(28.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )

                    OwnerStatColumn(
                        countText = TelefamNumberFormatter.formatCompactCount(uiState.ownerSubscribersCount),
                        label = stringResource(R.string.owner_profile_stat_subscribers),
                        onClick = { onOpenFollowList(FollowListType.SUBSCRIBERS) },
                        testTag = "owner_stat_subscribers_button",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Action Buttons Row: [ Edit profile ]  [ Settings ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onOpenEditProfile,
                        shape = RoundedCornerShape(10.dp),
                        color = pillButtonBg,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("owner_edit_profile_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = pillButtonText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.owner_profile_edit_button),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pillButtonText
                            )
                        }
                    }

                    Surface(
                        onClick = onOpenSettingsAndPrivacy,
                        shape = RoundedCornerShape(10.dp),
                        color = pillButtonBg,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("owner_settings_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = pillButtonText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = settingsLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pillButtonText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. 4 Sub-Tabs Bar (Posts Grid, Saved Bookmark, Reshared Repeat, Private Lock)
                OwnerSubTabsRow(
                    selectedSubTab = uiState.ownerSelectedSubTab,
                    onSelectSubTab = onSelectSubTab
                )

                // 6. 3-Column Media Grid with Duration Top-Right & Views Bottom-Left
                val posts = uiState.ownerPosts
                if (posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.profile_no_posts_yet),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val rows = posts.chunked(3)
                    val postsComingSoonDesc = stringResource(R.string.coming_soon_desc, postsLabel)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, start = 1.dp, end = 1.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        rows.forEach { rowPosts ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                for (colIdx in 0 until 3) {
                                    val post = rowPosts.getOrNull(colIdx)
                                    if (post != null) {
                                        OwnerPostGridTile(
                                            post = post,
                                            onClick = {
                                                localComingSoonBanner = "$comingSoonTitle: $postsComingSoonDesc"
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Clickable Followers / Following / Subscribers Modal Bottom Sheet with Pagination
    if (uiState.ownerFollowListType != null) {
        FollowConnectionsBottomSheet(
            listType = uiState.ownerFollowListType,
            users = uiState.ownerFollowListUsers,
            hasMore = uiState.ownerFollowListHasMore,
            onDismiss = onDismissFollowList,
            onUserClick = { targetUserId ->
                onDismissFollowList()
                onOpenOtherUserProfile(targetUserId)
            },
            onFollowToggleClick = onToggleFollowUser,
            onLoadMore = onLoadMoreFollowList
        )
    }

    // Full Owner Edit Profile Bottom Sheet with Photo Cropping & 7d/30d Cooldowns
    if (uiState.isOwnerEditSheetOpen) {
        OwnerEditProfileModalSheet(
            uiState = uiState,
            onDismiss = onDismissEditProfile,
            onFullNameChanged = onFullNameChanged,
            onUsernameChanged = onUsernameChanged,
            onApplySuggestedUsername = onApplySuggestedUsername,
            onCountrySelected = onCountrySelected,
            onPhoneNumberChanged = onPhoneNumberChanged,
            onGenderSelected = onGenderSelected,
            onDateOfBirthSelected = onDateOfBirthSelected,
            onBioChanged = onBioChanged,
            onAvatarUriPicked = onAvatarUriPicked,
            onSaveClick = { onSaveOwnerProfileEdits(savedConfirmText) }
        )
    }

    // Interactive Circular Photo Cropper Dialog (triggered when picking a new photo)
    uiState.pendingCropBitmap?.let { bmp ->
        ImageCropperDialog(
            bitmap = bmp,
            onCancel = onCancelCrop,
            onConfirmCrop = onConfirmCrop
        )
    }
}

@Composable
private fun OwnerStatColumn(
    countText: String,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = countText,
            fontSize = 17.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OwnerSubTabsRow(
    selectedSubTab: OwnerProfileSubTab,
    onSelectSubTab: (OwnerProfileSubTab) -> Unit
) {
    val tabs = listOf(
        Triple(OwnerProfileSubTab.POSTS, Icons.Default.GridOn, stringResource(R.string.owner_profile_tab_posts)),
        Triple(OwnerProfileSubTab.SAVED, Icons.Outlined.BookmarkBorder, stringResource(R.string.owner_profile_tab_saved)),
        Triple(OwnerProfileSubTab.RESHARED, Icons.Default.Repeat, stringResource(R.string.owner_profile_tab_reshared)),
        Triple(OwnerProfileSubTab.LOCKED, Icons.Outlined.Lock, stringResource(R.string.owner_profile_tab_locked))
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tab, icon, contentDesc) ->
                val isSelected = selectedSubTab == tab
                val tint = if (isSelected) {
                    TelefamRed
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectSubTab(tab) }
                        .testTag("owner_subtab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = contentDesc,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(2.5.dp)
                            .background(
                                color = if (isSelected) TelefamRed else Color.Transparent,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun OwnerPostGridTile(
    post: UserPostItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .clickable(onClick = onClick)
            .testTag("owner_post_tile_${post.postId}")
    ) {
        Image(
            painter = painterResource(id = post.mediaDrawableRes),
            contentDescription = post.caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle dark gradient at bottom for views legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f))
                    )
                )
        )

        // Top-right video duration pill matching reference image (e.g. "02:14")
        if (post.durationLabel.isNotBlank()) {
            Surface(
                color = Color.Black.copy(alpha = 0.58f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text = post.durationLabel,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        // Bottom-left Play icon + compact view count matching reference image (e.g. "▷ 12.4K")
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = TelefamNumberFormatter.formatCompactCount(post.viewsCount),
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerEditProfileModalSheet(
    uiState: TelefamUiState,
    onDismiss: () -> Unit,
    onFullNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onApplySuggestedUsername: (String) -> Unit,
    onCountrySelected: (CountryDialCode) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onGenderSelected: (String) -> Unit,
    onDateOfBirthSelected: (String) -> Unit,
    onBioChanged: (String) -> Unit,
    onAvatarUriPicked: (Uri) -> Unit,
    onSaveClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isTelefamDarkTheme()
    val inputBorderColor = if (isDark) TelefamInputBorderDark else TelefamInputBorderLight

    var showCountryDialog by remember { mutableStateOf(false) }
    var showGenderMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onAvatarUriPicked(uri)
        }
    }

    val cooldown = uiState.ownerCooldownState

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sheet Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.owner_profile_edit_title),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.crop_cancel_button)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )

            // Error Banner inside Edit Sheet
            uiState.errorMessage?.let { errorMsg ->
                Surface(
                    color = TelefamError.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TelefamError.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("owner_edit_error_banner")
                ) {
                    Text(
                        text = errorMsg,
                        color = TelefamError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 1. Editable Avatar with Crop & Upload Sanitization
            val previewPath = uiState.sanitizedAvatar?.secureFilePath ?: uiState.savedProfile?.avatarLocalPath
            val hasPreviewFile = remember(previewPath) {
                !previewPath.isNullOrBlank() && File(previewPath).exists()
            }

            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(94.dp)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("owner_edit_avatar_picker"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .border(2.dp, TelefamRed.copy(alpha = 0.5f), CircleShape)
                        .background(TelefamRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasPreviewFile && previewPath != null) {
                        AsyncImage(
                            model = File(previewPath),
                            contentDescription = stringResource(R.string.upload_profile_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = uiState.fullName.trim().take(1).uppercase().ifBlank { "T" },
                            color = TelefamRed,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(TelefamRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.upload_profile_photo),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.upload_profile_photo),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TelefamRed,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // 2. Full Name Field (7-day change cooldown rule)
            Column(modifier = Modifier.fillMaxWidth()) {
                EditFieldHeader(
                    icon = Icons.Default.Person,
                    label = stringResource(R.string.full_name_label)
                )
                OutlinedTextField(
                    value = uiState.fullName,
                    onValueChange = onFullNameChanged,
                    enabled = cooldown.canChangeFullName,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = inputBorderColor,
                        focusedBorderColor = TelefamRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_edit_full_name_input")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (cooldown.canChangeFullName) {
                        stringResource(R.string.owner_profile_name_cooldown_hint)
                    } else {
                        stringResource(R.string.owner_profile_name_cooldown_locked_format, cooldown.fullNameRemainingDays)
                    },
                    fontSize = 11.5.sp,
                    color = if (cooldown.canChangeFullName) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        TelefamError
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Username Field (30-day change cooldown + uniqueness + suggestions)
            Column(modifier = Modifier.fillMaxWidth()) {
                EditFieldHeader(
                    icon = Icons.Default.AlternateEmail,
                    label = stringResource(R.string.username_label)
                )
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = onUsernameChanged,
                    enabled = cooldown.canChangeUsername,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = if (uiState.isUsernameAvailable == false) TelefamError else inputBorderColor,
                        focusedBorderColor = if (uiState.isUsernameAvailable == false) TelefamError else TelefamRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_edit_username_input")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (cooldown.canChangeUsername) {
                        stringResource(R.string.owner_profile_username_cooldown_hint)
                    } else {
                        stringResource(R.string.owner_profile_username_cooldown_locked_format, cooldown.usernameRemainingDays)
                    },
                    fontSize = 11.5.sp,
                    color = if (cooldown.canChangeUsername) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        TelefamError
                    }
                )

                if (uiState.isUsernameAvailable == false && uiState.usernameSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.usernameSuggestions.forEachIndexed { idx, suggestion ->
                            Surface(
                                onClick = { onApplySuggestedUsername(suggestion) },
                                shape = RoundedCornerShape(50),
                                color = TelefamRed.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, TelefamRed.copy(alpha = 0.35f)),
                                modifier = Modifier.testTag("owner_edit_username_suggestion_$idx")
                            ) {
                                Text(
                                    text = "@$suggestion",
                                    fontSize = 12.sp,
                                    color = TelefamRed,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Phone Number Field with Country Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                EditFieldHeader(
                    icon = Icons.Default.Call,
                    label = stringResource(R.string.phone_number_label)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .border(1.dp, inputBorderColor, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable { showCountryDialog = true }
                            .padding(horizontal = 12.dp)
                            .testTag("owner_edit_country_selector"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (uiState.selectedCountry.isoCode == "KE") {
                            KenyaFlagVectorIcon(modifier = Modifier.size(width = 24.dp, height = 16.dp))
                        } else {
                            Text(text = uiState.selectedCountry.flagEmoji, fontSize = 18.sp)
                        }
                        Text(
                            text = uiState.selectedCountry.dialCode,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(inputBorderColor)
                    )

                    BasicTextField(
                        value = uiState.phoneNumber,
                        onValueChange = onPhoneNumberChanged,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(TelefamRed),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                            .testTag("owner_edit_phone_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Gender Selector
            val genderOptions = listOf(
                stringResource(R.string.gender_male),
                stringResource(R.string.gender_female),
                stringResource(R.string.gender_prefer_not_to_say)
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                EditFieldHeader(
                    icon = Icons.Default.People,
                    label = stringResource(R.string.gender_label)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, inputBorderColor, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showGenderMenu = true }
                            .padding(horizontal = 14.dp)
                            .testTag("owner_edit_gender_selector"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.gender.ifBlank { stringResource(R.string.gender_placeholder) },
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showGenderMenu,
                        onDismissRequest = { showGenderMenu = false }
                    ) {
                        genderOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onGenderSelected(option)
                                    showGenderMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Date of Birth Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                EditFieldHeader(
                    icon = Icons.Default.CalendarMonth,
                    label = stringResource(R.string.dob_label)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(1.dp, inputBorderColor, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 14.dp)
                        .testTag("owner_edit_dob_selector"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.dateOfBirth.ifBlank { stringResource(R.string.dob_placeholder) },
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Bio Field (up to 150 characters)
            Column(modifier = Modifier.fillMaxWidth()) {
                EditFieldHeader(
                    icon = Icons.Outlined.Sms,
                    label = stringResource(R.string.bio_label)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .border(1.dp, inputBorderColor, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    if (uiState.bio.isEmpty()) {
                        Text(
                            text = stringResource(R.string.bio_placeholder),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    BasicTextField(
                        value = uiState.bio,
                        onValueChange = onBioChanged,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(TelefamRed),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 18.dp)
                            .testTag("owner_edit_bio_input")
                    )
                    Text(
                        text = "${uiState.bio.length}/150",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Changes Button
            Button(
                onClick = onSaveClick,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelefamRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("owner_edit_save_button")
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.owner_profile_save_changes),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showCountryDialog) {
        SearchableCountryPickerDialog(
            selectedCountry = uiState.selectedCountry,
            onSelectCountry = {
                onCountrySelected(it)
                showCountryDialog = false
            },
            onDismiss = { showCountryDialog = false }
        )
    }

    if (showDatePicker) {
        val utc = TimeZone.getTimeZone("UTC")
        val maxAllowedMillis = remember {
            Calendar.getInstance(utc).apply { add(Calendar.YEAR, -13) }.timeInMillis
        }
        val minAllowedMillis = remember {
            Calendar.getInstance(utc).apply { add(Calendar.YEAR, -120) }.timeInMillis
        }
        val defaultEighteenMillis = remember {
            Calendar.getInstance(utc).apply { add(Calendar.YEAR, -18) }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = defaultEighteenMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis in minAllowedMillis..maxAllowedMillis
                }

                override fun isSelectableYear(year: Int): Boolean {
                    val currentYear = Calendar.getInstance(utc).get(Calendar.YEAR)
                    return year in (currentYear - 120)..(currentYear - 13)
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis ?: defaultEighteenMillis
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }
                        onDateOfBirthSelected(sdf.format(Date(millis)))
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = TelefamRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.crop_cancel_button))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun EditFieldHeader(
    icon: ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TelefamRed,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
