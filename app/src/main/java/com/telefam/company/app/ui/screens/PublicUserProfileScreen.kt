package com.telefam.company.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.TelefamRed
import com.example.ui.theme.isTelefamDarkTheme
import com.telefam.company.app.domain.DiscoverableUserItem
import com.telefam.company.app.domain.FollowListType
import com.telefam.company.app.domain.FollowRelationState
import com.telefam.company.app.domain.PostsFilterOption
import com.telefam.company.app.domain.ReportLocationOption
import com.telefam.company.app.domain.ReportReasonOption
import com.telefam.company.app.domain.ReportUrgencyOption
import com.telefam.company.app.domain.TelefamCountries
import com.telefam.company.app.domain.TelefamNumberFormatter
import com.telefam.company.app.domain.UserPostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicUserProfileScreen(
    profile: DiscoverableUserItem,
    posts: List<UserPostItem>,
    postsFilter: PostsFilterOption,
    isOffline: Boolean,
    activeFollowListType: FollowListType?,
    followListUsers: List<DiscoverableUserItem>,
    followListHasMore: Boolean,
    onBackClick: () -> Unit,
    onToggleFollowProfileUser: () -> Unit,
    onToggleFollowAnyUser: (String) -> Unit,
    onOpenOtherUserProfile: (String) -> Unit,
    onMessageComingSoonClick: () -> Unit,
    onSubscribeComingSoonClick: () -> Unit,
    onPostComingSoonClick: (UserPostItem) -> Unit,
    onPostsFilterChange: (PostsFilterOption) -> Unit,
    onOpenFollowList: (FollowListType) -> Unit,
    onDismissFollowList: () -> Unit,
    onLoadMoreFollowList: () -> Unit,
    onCopyProfileUrlConfirmed: (String) -> Unit,
    onToggleBlockUser: (Boolean) -> Unit,
    onSubmitReport: (ReportReasonOption, ReportLocationOption, ReportUrgencyOption, String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showPostsFilterMenu by remember { mutableStateOf(false) }
    var showReportSheet by rememberSaveable { mutableStateOf(false) }

    val isDark = isTelefamDarkTheme()
    val pageBg = if (isDark) MaterialTheme.colorScheme.background else Color.White
    val secondaryButtonBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F1F4)

    val comingSoonTitle = stringResource(R.string.coming_soon_title)
    val msgLabel = stringResource(R.string.profile_action_message)
    val subLabel = stringResource(R.string.profile_action_subscribe)
    val postsLabel = stringResource(R.string.profile_posts_header)
    val urlCopiedText = stringResource(R.string.profile_url_copied)
    val reportSubmittedText = stringResource(R.string.report_submitted_confirmation)
    var localBannerMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val countryInfo = remember(profile.countryIsoCode) {
        TelefamCountries.findByIso(profile.countryIsoCode)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            // Top Bar matching Image 1: Back Arrow | Full Name | 3-Dots Overflow Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("public_profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_to_login),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = profile.fullName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("public_profile_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.menu_options),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.profile_menu_copy_url),
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                val url = "https://telefam.company/@${profile.username.removePrefix("@")}"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Telefam Profile URL", url))
                                localBannerMessage = "$urlCopiedText ($url)"
                                onCopyProfileUrlConfirmed(url)
                            },
                            modifier = Modifier.testTag("profile_menu_copy_url_item")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.profile_menu_report),
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = TelefamRed
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                showReportSheet = true
                            },
                            modifier = Modifier.testTag("profile_menu_report_item")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (profile.isBlocked) {
                                        stringResource(R.string.profile_menu_unblock)
                                    } else {
                                        stringResource(R.string.profile_menu_block)
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    color = TelefamRed
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = TelefamRed
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onToggleBlockUser(!profile.isBlocked)
                            },
                            modifier = Modifier.testTag("profile_menu_block_item")
                        )
                    }
                }
            }

            // Status / Coming Soon / Confirmation Banner
            AnimatedVisibility(visible = localBannerMessage != null) {
                Surface(
                    color = TelefamRed.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("public_profile_status_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = localBannerMessage.orEmpty(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TelefamRed,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { localBannerMessage = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.crop_cancel_button),
                                tint = TelefamRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Scrollable Profile Content + 3-Column Posts Grid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Centered Avatar matching Image 1
                UserAvatarCircle(
                    fullName = profile.fullName,
                    userId = profile.userId,
                    avatarLocalPath = profile.avatarLocalPath,
                    avatarCdnUrl = profile.avatarCdnUrl,
                    sizeDp = 104
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Full Name & @username
                Text(
                    text = profile.fullName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "@${profile.username.removePrefix("@")}",
                    fontSize = 14.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Bio text
                if (profile.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = profile.bio,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )
                }

                // Location & Country pill
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TelefamRed,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val locationWithCountry = buildString {
                        append(profile.locationLabel)
                        if (countryInfo != null && !profile.locationLabel.contains(countryInfo.name, ignoreCase = true)) {
                            append(" • ${countryInfo.name}")
                        }
                        if (countryInfo != null) {
                            append(" (${countryInfo.flagEmoji} ${countryInfo.isoCode})")
                        }
                    }
                    Text(
                        text = locationWithCountry,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Stats Row: Following (clickable) | Followers (clickable) | Likes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStatColumn(
                        countText = TelefamNumberFormatter.formatCompactCount(profile.followingCount),
                        label = stringResource(R.string.profile_stat_following),
                        onClick = { onOpenFollowList(FollowListType.FOLLOWING) },
                        testTag = "profile_stat_following_button",
                        modifier = Modifier.weight(1f)
                    )

                    VerticalDivider(
                        modifier = Modifier.height(28.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )

                    ProfileStatColumn(
                        countText = TelefamNumberFormatter.formatCompactCount(profile.followersCount),
                        label = stringResource(R.string.profile_stat_followers),
                        onClick = { onOpenFollowList(FollowListType.FOLLOWERS) },
                        testTag = "profile_stat_followers_button",
                        modifier = Modifier.weight(1f)
                    )

                    VerticalDivider(
                        modifier = Modifier.height(28.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )

                    ProfileStatColumn(
                        countText = TelefamNumberFormatter.formatCompactCount(profile.likesCount),
                        label = stringResource(R.string.profile_stat_likes),
                        onClick = onSubscribeComingSoonClick,
                        testTag = "profile_stat_likes_button",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (profile.isBlocked) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TelefamRed.copy(alpha = 0.10f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.profile_blocked_banner),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TelefamRed
                            )
                            TextButton(
                                onClick = { onToggleBlockUser(false) },
                                modifier = Modifier.testTag("profile_unblock_banner_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.profile_menu_unblock),
                                    color = TelefamRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // 3 Action Buttons Row matching Image 1: Follow | Message | Subscribe
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isPrimaryRed = profile.followState == FollowRelationState.NONE ||
                            profile.followState == FollowRelationState.FOLLOW_BACK
                        val followLabel = when (profile.followState) {
                            FollowRelationState.NONE -> stringResource(R.string.action_follow)
                            FollowRelationState.FOLLOW_BACK -> stringResource(R.string.action_follow_back)
                            FollowRelationState.FOLLOWING -> stringResource(R.string.action_following)
                            FollowRelationState.FRIENDS -> stringResource(R.string.action_friends)
                        }

                        Button(
                            onClick = onToggleFollowProfileUser,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPrimaryRed) TelefamRed else secondaryButtonBg,
                                contentColor = if (isPrimaryRed) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("public_profile_follow_button")
                        ) {
                            Text(
                                text = followLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        val msgComingSoonDesc = stringResource(R.string.coming_soon_desc, msgLabel)
                        val subComingSoonDesc = stringResource(R.string.coming_soon_desc, subLabel)
                        val postsComingSoonDesc = stringResource(R.string.coming_soon_desc, postsLabel)

                        Button(
                            onClick = {
                                localBannerMessage = "$comingSoonTitle: $msgComingSoonDesc"
                                onMessageComingSoonClick()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = secondaryButtonBg,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("public_profile_message_button")
                        ) {
                            Text(
                                text = stringResource(R.string.profile_action_message),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                localBannerMessage = "$comingSoonTitle: $subComingSoonDesc"
                                onSubscribeComingSoonClick()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = secondaryButtonBg,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("public_profile_subscribe_button")
                        ) {
                            Text(
                                text = stringResource(R.string.profile_action_subscribe),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )

                // Posts Section Header matching Image 1: "Posts" + "All Posts ▾"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.profile_posts_header),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box {
                        val currentFilterLabel = when (postsFilter) {
                            PostsFilterOption.ALL_POSTS -> stringResource(R.string.posts_filter_all)
                            PostsFilterOption.MOST_VIEWED -> stringResource(R.string.posts_filter_most_viewed)
                            PostsFilterOption.LATEST -> stringResource(R.string.posts_filter_latest)
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = secondaryButtonBg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showPostsFilterMenu = true }
                                .testTag("profile_posts_filter_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentFilterLabel,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showPostsFilterMenu,
                            onDismissRequest = { showPostsFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.posts_filter_all)) },
                                onClick = {
                                    showPostsFilterMenu = false
                                    onPostsFilterChange(PostsFilterOption.ALL_POSTS)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.posts_filter_most_viewed)) },
                                onClick = {
                                    showPostsFilterMenu = false
                                    onPostsFilterChange(PostsFilterOption.MOST_VIEWED)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.posts_filter_latest)) },
                                onClick = {
                                    showPostsFilterMenu = false
                                    onPostsFilterChange(PostsFilterOption.LATEST)
                                }
                            )
                        }
                    }
                }

                // 3-Column Posts Grid matching Image 1 (with Views & Likes for future)
                if (posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 42.dp),
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        rows.forEach { rowPosts ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                for (colIndex in 0 until 3) {
                                    val post = rowPosts.getOrNull(colIndex)
                                    if (post != null) {
                                        val postsComingSoonDesc = stringResource(R.string.coming_soon_desc, postsLabel)
                                        ProfilePostGridTile(
                                            post = post,
                                            onClick = {
                                                localBannerMessage = "$comingSoonTitle: $postsComingSoonDesc"
                                                onPostComingSoonClick(post)
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

    // Clickable Followers / Following List Modal Bottom Sheet with Pagination
    if (activeFollowListType != null) {
        FollowConnectionsBottomSheet(
            listType = activeFollowListType,
            users = followListUsers,
            hasMore = followListHasMore,
            onDismiss = onDismissFollowList,
            onUserClick = { uid ->
                onDismissFollowList()
                onOpenOtherUserProfile(uid)
            },
            onFollowToggleClick = onToggleFollowAnyUser,
            onLoadMore = onLoadMoreFollowList
        )
    }

    // Multi-Question Report Bottom Sheet
    if (showReportSheet) {
        MultiQuestionReportBottomSheet(
            reportedUsername = profile.username,
            onDismiss = { showReportSheet = false },
            onSubmit = { reason, location, urgency, notes ->
                showReportSheet = false
                localBannerMessage = reportSubmittedText
                onSubmitReport(reason, location, urgency, notes)
            }
        )
    }
}

@Composable
private fun ProfileStatColumn(
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfilePostGridTile(
    post: UserPostItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .testTag("profile_post_tile_${post.postId}")
    ) {
        Image(
            painter = painterResource(id = post.mediaDrawableRes),
            contentDescription = post.caption,
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
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.62f)
                        )
                    )
                )
        )

        // Bottom overlay: Views count (left, matching Image 1) + Likes count (right, for future)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = TelefamNumberFormatter.formatCompactCount(post.likesCount),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FollowConnectionsBottomSheet(
    listType: FollowListType,
    users: List<DiscoverableUserItem>,
    hasMore: Boolean,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit,
    onFollowToggleClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp, max = 560.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when (listType) {
                        FollowListType.FOLLOWING -> stringResource(R.string.profile_stat_following)
                        FollowListType.FOLLOWERS -> stringResource(R.string.profile_stat_followers)
                        FollowListType.SUBSCRIBERS -> stringResource(R.string.owner_profile_stat_subscribers)
                    },
                    fontSize = 18.sp,
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
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )

            if (users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.contacts_empty_title),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = users,
                        key = { _, u -> u.userId }
                    ) { index, item ->
                        DiscoverableUserRow(
                            user = item,
                            index = index,
                            onRowClick = { onUserClick(item.userId) },
                            onFollowActionClick = { onFollowToggleClick(item.userId) }
                        )
                    }

                    if (hasMore) {
                        item(key = "follow_list_load_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = onLoadMore,
                                    modifier = Modifier.testTag("follow_list_load_more_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.load_more_button),
                                        color = TelefamRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiQuestionReportBottomSheet(
    reportedUsername: String,
    onDismiss: () -> Unit,
    onSubmit: (ReportReasonOption, ReportLocationOption, ReportUrgencyOption, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedReason by rememberSaveable { mutableStateOf(ReportReasonOption.SPAM_OR_BOT) }
    var selectedLocation by rememberSaveable { mutableStateOf(ReportLocationOption.PROFILE_DETAILS) }
    var selectedUrgency by rememberSaveable { mutableStateOf(ReportUrgencyOption.STANDARD_REVIEW) }
    var additionalNotes by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${stringResource(R.string.report_sheet_title)} (@${reportedUsername.removePrefix("@")})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.report_sheet_subtitle),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.crop_cancel_button)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Question 1: Reason
            Text(
                text = stringResource(R.string.report_question_1_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            ReportReasonOption.entries.forEach { option ->
                ReportChoiceRow(
                    label = stringResource(option.labelRes),
                    selected = selectedReason == option,
                    onClick = { selectedReason = option },
                    testTag = "report_q1_${option.wireCode}"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Question 2: Location of issue
            Text(
                text = stringResource(R.string.report_question_2_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            ReportLocationOption.entries.forEach { option ->
                ReportChoiceRow(
                    label = stringResource(option.labelRes),
                    selected = selectedLocation == option,
                    onClick = { selectedLocation = option },
                    testTag = "report_q2_${option.wireCode}"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Question 3: Action / Urgency
            Text(
                text = stringResource(R.string.report_question_3_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            ReportUrgencyOption.entries.forEach { option ->
                ReportChoiceRow(
                    label = stringResource(option.labelRes),
                    selected = selectedUrgency == option,
                    onClick = { selectedUrgency = option },
                    testTag = "report_q3_${option.wireCode}"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { if (it.length <= 300) additionalNotes = it },
                label = { Text(stringResource(R.string.report_notes_label)) },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_additional_notes_input")
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    onSubmit(selectedReason, selectedLocation, selectedUrgency, additionalNotes)
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelefamRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("report_submit_button")
            ) {
                Text(
                    text = stringResource(R.string.report_submit_button),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReportChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) {
            TelefamRed.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (selected) TelefamRed.copy(alpha = 0.45f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = TelefamRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
