package com.telefam.company.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.TelefamRed
import com.example.ui.theme.isTelefamDarkTheme
import com.telefam.company.app.domain.ContactsFilterMode
import com.telefam.company.app.domain.DiscoverableUserItem
import com.telefam.company.app.domain.FollowRelationState
import com.telefam.company.app.domain.TelefamCountries
import java.io.File
import kotlin.math.abs

@Composable
fun ContactsScreen(
    users: List<DiscoverableUserItem>,
    searchQuery: String,
    filterMode: ContactsFilterMode,
    hasMorePages: Boolean,
    hasGrantedContactsPermission: Boolean,
    syncedContactsHashesCount: Int,
    isOffline: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterModeChange: (ContactsFilterMode) -> Unit,
    onToggleFollowClick: (String) -> Unit,
    onUserRowClick: (String) -> Unit,
    onLoadNextPage: () -> Unit,
    onRefreshClick: () -> Unit,
    onContactsPermissionResult: (Boolean) -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showInlineSearch by rememberSaveable { mutableStateOf(searchQuery.isNotBlank()) }
    var dismissContactsCard by rememberSaveable { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onContactsPermissionResult(granted)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(filterMode) {
        if (users.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    val shouldLoadMore by remember(users.size, hasMorePages) {
        derivedStateOf {
            if (!hasMorePages || users.isEmpty()) {
                false
            } else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= (users.size - 2).coerceAtLeast(0)
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadNextPage()
        }
    }

    val isDark = isTelefamDarkTheme()
    val pageBg = if (isDark) MaterialTheme.colorScheme.background else Color.White

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
            // Top Header matching Image 2 ("Suggested For You" + Back + Search)
            Surface(
                color = pageBg,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onBackClick != null) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("contacts_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back_to_login),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Text(
                            text = stringResource(R.string.suggested_for_you_title),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = if (onBackClick != null) TextAlign.Center else TextAlign.Start
                        )

                        IconButton(
                            onClick = onRefreshClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("contacts_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.contacts_refresh_button),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                showInlineSearch = !showInlineSearch
                                if (!showInlineSearch && searchQuery.isNotEmpty()) {
                                    onSearchQueryChange("")
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("contacts_search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (showInlineSearch) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = stringResource(R.string.search_chats_placeholder),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Expandable Search Bar (keywords, names, @usernames)
                    AnimatedVisibility(visible = showInlineSearch || searchQuery.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TelefamRed,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.contacts_search_placeholder),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(TelefamRed),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("contacts_search_input")
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchQueryChange("") },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("contacts_search_clear_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.crop_cancel_button),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Subtitles Filter Bar: Suggested | All | Followers | Following | Friends
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContactsFilterPill(
                            label = stringResource(R.string.contacts_filter_suggested),
                            selected = filterMode == ContactsFilterMode.SUGGESTED,
                            onClick = { onFilterModeChange(ContactsFilterMode.SUGGESTED) },
                            testTag = "contacts_filter_suggested_pill"
                        )
                        ContactsFilterPill(
                            label = stringResource(R.string.contacts_filter_all),
                            selected = filterMode == ContactsFilterMode.ALL,
                            onClick = { onFilterModeChange(ContactsFilterMode.ALL) },
                            testTag = "contacts_filter_all_pill"
                        )
                        ContactsFilterPill(
                            label = stringResource(R.string.contacts_filter_followers),
                            selected = filterMode == ContactsFilterMode.FOLLOWERS,
                            onClick = { onFilterModeChange(ContactsFilterMode.FOLLOWERS) },
                            testTag = "contacts_filter_followers_pill"
                        )
                        ContactsFilterPill(
                            label = stringResource(R.string.contacts_filter_following),
                            selected = filterMode == ContactsFilterMode.FOLLOWING,
                            onClick = { onFilterModeChange(ContactsFilterMode.FOLLOWING) },
                            testTag = "contacts_filter_following_pill"
                        )
                        ContactsFilterPill(
                            label = stringResource(R.string.contacts_filter_friends),
                            selected = filterMode == ContactsFilterMode.FRIENDS,
                            onClick = { onFilterModeChange(ContactsFilterMode.FRIENDS) },
                            testTag = "contacts_filter_friends_pill"
                        )
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                }
            }

            // Contacts Access Request Banner (if permission not yet granted)
            AnimatedVisibility(visible = !hasGrantedContactsPermission && !dismissContactsCard) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("contacts_access_request_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TelefamRed.copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    tint = TelefamRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.contacts_access_title),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.contacts_access_message),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TelefamRed,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("contacts_allow_access_button")
                        ) {
                            Text(
                                text = stringResource(R.string.contacts_allow_button),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Registered Users List (Matching Image 2)
            if (users.isEmpty()) {
                val emptyTitle = when {
                    searchQuery.isNotBlank() -> stringResource(R.string.contacts_empty_title)
                    filterMode == ContactsFilterMode.FOLLOWERS -> stringResource(R.string.contacts_empty_followers_title)
                    filterMode == ContactsFilterMode.FOLLOWING -> stringResource(R.string.contacts_empty_following_title)
                    filterMode == ContactsFilterMode.FRIENDS -> stringResource(R.string.contacts_empty_friends_title)
                    else -> stringResource(R.string.contacts_empty_title)
                }
                val emptySubtitle = when {
                    searchQuery.isNotBlank() -> stringResource(R.string.contacts_empty_subtitle)
                    filterMode == ContactsFilterMode.FOLLOWERS -> stringResource(R.string.contacts_empty_followers_subtitle)
                    filterMode == ContactsFilterMode.FOLLOWING -> stringResource(R.string.contacts_empty_following_subtitle)
                    filterMode == ContactsFilterMode.FRIENDS -> stringResource(R.string.contacts_empty_friends_subtitle)
                    else -> stringResource(R.string.contacts_empty_subtitle)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = TelefamRed.copy(alpha = 0.10f),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = null,
                                    tint = TelefamRed,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = emptyTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = emptySubtitle,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("contacts_users_lazy_list"),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    itemsIndexed(
                        items = users,
                        key = { _, item -> item.userId }
                    ) { index, item ->
                        DiscoverableUserRow(
                            user = item,
                            index = index,
                            onRowClick = { onUserRowClick(item.userId) },
                            onFollowActionClick = { onToggleFollowClick(item.userId) }
                        )
                    }

                    if (hasMorePages) {
                        item(key = "load_more_footer") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    onClick = onLoadNextPage,
                                    shape = RoundedCornerShape(50),
                                    color = TelefamRed.copy(alpha = 0.10f),
                                    modifier = Modifier.testTag("contacts_load_more_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.load_more_button),
                                        color = TelefamRed,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 9.dp)
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

@Composable
private fun ContactsFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) TelefamRed else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun DiscoverableUserRow(
    user: DiscoverableUserItem,
    index: Int = 0,
    onRowClick: () -> Unit,
    onFollowActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitleText = when {
        user.isMatchedFromContacts -> stringResource(R.string.from_your_contacts_badge)
        index % 2 == 0 && user.mutualConnectionsCount > 0 -> stringResource(
            R.string.mutual_connections_format,
            user.mutualConnectionsCount
        )
        user.bio.isNotBlank() -> user.bio.replace("\n", " ")
        user.mutualConnectionsCount > 0 -> stringResource(
            R.string.mutual_connections_format,
            user.mutualConnectionsCount
        )
        else -> {
            val country = TelefamCountries.findByIso(user.countryIsoCode)
            if (country != null) "${country.flagEmoji} ${user.locationLabel}" else user.locationLabel
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .testTag("contact_user_row_${user.username}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatarCircle(
            fullName = user.fullName,
            userId = user.userId,
            avatarLocalPath = user.avatarLocalPath,
            avatarCdnUrl = user.avatarCdnUrl,
            sizeDp = 54
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.fullName,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.username.removePrefix("@")}",
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitleText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        FollowRelationshipPillButton(
            followState = user.followState,
            onClick = onFollowActionClick,
            testTag = "follow_btn_${user.username}"
        )
    }
}

@Composable
fun FollowRelationshipPillButton(
    followState: FollowRelationState,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val isPrimaryRed = followState == FollowRelationState.NONE || followState == FollowRelationState.FOLLOW_BACK

    val containerColor = if (isPrimaryRed) {
        TelefamRed
    } else if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Color(0xFFECECEE)
    }

    val contentColor = if (isPrimaryRed) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val label = when (followState) {
        FollowRelationState.NONE -> stringResource(R.string.action_follow)
        FollowRelationState.FOLLOW_BACK -> stringResource(R.string.action_follow_back)
        FollowRelationState.FOLLOWING -> stringResource(R.string.action_following)
        FollowRelationState.FRIENDS -> stringResource(R.string.action_friends)
    }

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        modifier = modifier
            .heightIn(min = 38.dp)
            .widthIn(min = 106.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun UserAvatarCircle(
    fullName: String,
    userId: String,
    avatarLocalPath: String?,
    avatarCdnUrl: String?,
    sizeDp: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarFile = remember(avatarLocalPath) {
        avatarLocalPath?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
    }
    val modelToLoad: Any? = avatarFile ?: avatarCdnUrl?.takeIf { it.isNotBlank() }

    val avatarPalette = listOf(
        Color(0xFF2D4356),
        Color(0xFF435B66),
        Color(0xFF6B4F4F),
        Color(0xFF3F4E4F),
        Color(0xFF5C469C),
        Color(0xFF852999),
        Color(0xFF1F6E8C),
        Color(0xFF9E4784)
    )
    val bgTint = avatarPalette[abs(userId.hashCode()) % avatarPalette.size]
    val initials = remember(fullName) {
        val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts.first().first().uppercaseChar()}${parts.last().first().uppercaseChar()}"
            parts.size == 1 -> parts.first().take(2).uppercase()
            else -> "TF"
        }
    }

    Surface(
        shape = CircleShape,
        color = bgTint,
        modifier = modifier
            .size(sizeDp.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CircleShape)
    ) {
        if (modelToLoad != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(modelToLoad)
                    .crossfade(true)
                    .build(),
                contentDescription = fullName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = (sizeDp * 0.34f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
