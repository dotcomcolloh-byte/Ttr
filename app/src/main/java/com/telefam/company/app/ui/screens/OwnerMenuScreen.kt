package com.telefam.company.app.ui.screens

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.TelefamRed
import com.example.ui.theme.isTelefamDarkTheme
import com.telefam.company.app.ui.TelefamUiState
import java.io.File

private data class OwnerMenuGridItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val showVerifiedActionPill: Boolean = false,
    val testTag: String
)

/**
 * Owner Profile Menu Screen matching the uploaded reference image (`file_000000009e68820898a340b10258dfd5.png`):
 * - Crimson Red diagonal-wave top header with `< Menu` and Real Owner profile details
 *   (real profile photo or initial, real full name, real `@username`, real bio if set, and right chevron `>`)
 * - All buttons arranged in a responsive Grid (2 columns on phones, 3 columns on tablets/desktop):
 *   1. Dashboard (on top first)
 *   2. Stars
 *   3. Subscription
 *   4. Monetization
 *   5. Wallet
 *   6. Linked device
 *   7. Create campaign
 *   8. Get Verified (with red "Get Verified >" badge)
 *   Alongside the other buttons:
 *   9. Live Stream
 *   10. Market
 *   11. Groups
 *   12. Memories
 * - Pure Lucide SVG vector icons inside red rounded-square badges (zero emojis, zero demo/fake data)
 * - Every button displays a localized, theme-aware "Coming Soon" banner when tapped
 */
@Composable
fun OwnerMenuScreen(
    uiState: TelefamUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isTelefamDarkTheme()

    val screenBackground = if (isDark) {
        MaterialTheme.colorScheme.background
    } else {
        Color(0xFFF6F7FA)
    }

    val cardBackground = if (isDark) {
        Color(0xFF1A222D)
    } else {
        Color.White
    }

    val cardBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFEDF0F5)
    }

    // Real authenticated owner profile details (no demo or seeded fallback)
    val savedProfile = uiState.savedProfile
    val ownerFullName = savedProfile?.fullName?.takeIf { it.isNotBlank() }
        ?: uiState.fullName.takeIf { it.isNotBlank() }
        ?: uiState.activeSession?.email?.substringBefore('@').orEmpty()
    val ownerUsername = savedProfile?.username?.takeIf { it.isNotBlank() }
        ?: uiState.username.takeIf { it.isNotBlank() }
        ?: uiState.activeSession?.email?.substringBefore('@').orEmpty()
    val ownerBio = savedProfile?.bio?.takeIf { it.isNotBlank() }
        ?: uiState.bio.takeIf { it.isNotBlank() }.orEmpty()

    val comingSoonTitle = stringResource(R.string.coming_soon_title)
    val menuTitle = stringResource(R.string.owner_menu_title)

    // 8 Buttons from the uploaded image (placed ON TOP FIRST)
    val dashboardLabel = stringResource(R.string.owner_menu_dashboard)
    val dashboardDesc = stringResource(R.string.owner_menu_dashboard_desc)
    val starsLabel = stringResource(R.string.owner_menu_stars)
    val starsDesc = stringResource(R.string.owner_menu_stars_desc)
    val subscriptionLabel = stringResource(R.string.owner_menu_subscription)
    val subscriptionDesc = stringResource(R.string.owner_menu_subscription_desc)
    val monetizationLabel = stringResource(R.string.owner_menu_monetization)
    val monetizationDesc = stringResource(R.string.owner_menu_monetization_desc)
    val walletLabel = stringResource(R.string.owner_menu_wallet)
    val walletDesc = stringResource(R.string.owner_menu_wallet_desc)
    val linkedDeviceLabel = stringResource(R.string.owner_menu_linked_device)
    val linkedDeviceDesc = stringResource(R.string.owner_menu_linked_device_desc)
    val createCampaignLabel = stringResource(R.string.owner_menu_create_campaign)
    val createCampaignDesc = stringResource(R.string.owner_menu_create_campaign_desc)
    val getVerifiedLabel = stringResource(R.string.owner_menu_get_verified)
    val getVerifiedDesc = stringResource(R.string.owner_menu_get_verified_desc)

    // Remaining buttons alongside them
    val liveStreamLabel = stringResource(R.string.owner_menu_live_stream)
    val liveStreamDesc = stringResource(R.string.owner_menu_live_stream_desc)
    val marketLabel = stringResource(R.string.owner_menu_market)
    val marketDesc = stringResource(R.string.owner_menu_market_desc)
    val groupsLabel = stringResource(R.string.owner_menu_groups)
    val groupsDesc = stringResource(R.string.owner_menu_groups_desc)
    val memoriesLabel = stringResource(R.string.owner_menu_memories)
    val memoriesDesc = stringResource(R.string.owner_menu_memories_desc)

    var activeComingSoonFeature by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler {
        if (activeComingSoonFeature != null) {
            activeComingSoonFeature = null
        } else {
            onBackClick()
        }
    }

    val allGridButtons = listOf(
        // Top 8 buttons from the reference image first:
        OwnerMenuGridItem(
            id = "dashboard",
            title = dashboardLabel,
            subtitle = dashboardDesc,
            icon = LucideOwnerMenuIcons.BarChart,
            testTag = "owner_menu_btn_dashboard"
        ),
        OwnerMenuGridItem(
            id = "stars",
            title = starsLabel,
            subtitle = starsDesc,
            icon = LucideOwnerMenuIcons.Star,
            testTag = "owner_menu_btn_stars"
        ),
        OwnerMenuGridItem(
            id = "subscription",
            title = subscriptionLabel,
            subtitle = subscriptionDesc,
            icon = LucideOwnerMenuIcons.Crown,
            testTag = "owner_menu_btn_subscription"
        ),
        OwnerMenuGridItem(
            id = "monetization",
            title = monetizationLabel,
            subtitle = monetizationDesc,
            icon = LucideOwnerMenuIcons.DollarBadge,
            testTag = "owner_menu_btn_monetization"
        ),
        OwnerMenuGridItem(
            id = "wallet",
            title = walletLabel,
            subtitle = walletDesc,
            icon = LucideOwnerMenuIcons.Wallet,
            testTag = "owner_menu_btn_wallet"
        ),
        OwnerMenuGridItem(
            id = "linked_device",
            title = linkedDeviceLabel,
            subtitle = linkedDeviceDesc,
            icon = LucideOwnerMenuIcons.LinkedDevices,
            testTag = "owner_menu_btn_linked_device"
        ),
        OwnerMenuGridItem(
            id = "create_campaign",
            title = createCampaignLabel,
            subtitle = createCampaignDesc,
            icon = LucideOwnerMenuIcons.Megaphone,
            testTag = "owner_menu_btn_create_campaign"
        ),
        OwnerMenuGridItem(
            id = "get_verified",
            title = getVerifiedLabel,
            subtitle = getVerifiedDesc,
            icon = LucideOwnerMenuIcons.BadgeCheck,
            showVerifiedActionPill = true,
            testTag = "owner_menu_btn_get_verified"
        ),
        // Alongside the remaining buttons:
        OwnerMenuGridItem(
            id = "live_stream",
            title = liveStreamLabel,
            subtitle = liveStreamDesc,
            icon = LucideOwnerMenuIcons.RadioLive,
            testTag = "owner_menu_btn_live_stream"
        ),
        OwnerMenuGridItem(
            id = "market",
            title = marketLabel,
            subtitle = marketDesc,
            icon = LucideOwnerMenuIcons.Store,
            testTag = "owner_menu_btn_market"
        ),
        OwnerMenuGridItem(
            id = "groups",
            title = groupsLabel,
            subtitle = groupsDesc,
            icon = LucideOwnerMenuIcons.Users,
            testTag = "owner_menu_btn_groups"
        ),
        OwnerMenuGridItem(
            id = "memories",
            title = memoriesLabel,
            subtitle = memoriesDesc,
            icon = LucideOwnerMenuIcons.MemoriesClock,
            testTag = "owner_menu_btn_memories"
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(screenBackground)
            .testTag("owner_menu_screen_root")
    ) {
        val isExpandedScreen = maxWidth >= 700.dp
        val gridColumns = if (isExpandedScreen) 3 else 2
        val maxContentWidth = if (maxWidth >= 840.dp) 920.dp else 700.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Crimson Red Wave Header Banner matching the reference image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFD90817),
                                TelefamRed,
                                Color(0xFF9E040F)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 600f)
                        )
                    )
            ) {
                // Subtle diagonal crimson silk waves matching reference header artwork
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val wave1 = Path().apply {
                        moveTo(w * 0.25f, 0f)
                        lineTo(w * 0.68f, 0f)
                        lineTo(w * 0.32f, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = wave1,
                        color = Color.White.copy(alpha = 0.06f)
                    )

                    val wave2 = Path().apply {
                        moveTo(w * 0.58f, 0f)
                        lineTo(w, 0f)
                        lineTo(w * 0.72f, h)
                        lineTo(w * 0.36f, h)
                        close()
                    }
                    drawPath(
                        path = wave2,
                        color = Color.Black.copy(alpha = 0.08f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = maxContentWidth)
                            .padding(horizontal = 16.dp)
                    ) {
                        // Top "< Menu" Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("owner_menu_back_button")
                            ) {
                                Icon(
                                    imageVector = LucideOwnerMenuIcons.ChevronLeft,
                                    contentDescription = stringResource(R.string.crop_cancel_button),
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            Text(
                                text = menuTitle,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.testTag("owner_menu_title")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real Owner Profile Identity Row inside the Red Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    activeComingSoonFeature = ownerFullName.ifBlank { menuTitle }
                                }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                .testTag("owner_menu_owner_card"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val localPath = savedProfile?.avatarLocalPath
                            val hasLocalFile = remember(localPath) {
                                !localPath.isNullOrBlank() && File(localPath).exists()
                            }

                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.2.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .testTag("owner_menu_owner_avatar"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasLocalFile && localPath != null) {
                                    AsyncImage(
                                        model = File(localPath),
                                        contentDescription = ownerFullName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Text(
                                        text = ownerFullName.trim().take(1).uppercase().ifBlank { "T" },
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = ownerFullName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("owner_menu_owner_name")
                                )

                                if (ownerUsername.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "@$ownerUsername",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.92f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("owner_menu_owner_username")
                                    )
                                }

                                if (ownerBio.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = ownerBio,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.84f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("owner_menu_owner_bio")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = LucideOwnerMenuIcons.ChevronRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Main Body Container: Coming Soon Banner + All Buttons in Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxContentWidth)
                    .padding(horizontal = 14.dp)
            ) {
                // Coming Soon Status Banner
                AnimatedVisibility(
                    visible = activeComingSoonFeature != null,
                    enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut()
                ) {
                    val featureLabel = activeComingSoonFeature.orEmpty()
                    Surface(
                        color = cardBackground,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TelefamRed.copy(alpha = 0.28f)),
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("owner_menu_coming_soon_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(TelefamRed.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = TelefamRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = comingSoonTitle,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TelefamRed
                                    )
                                    Text(
                                        text = stringResource(R.string.coming_soon_desc, featureLabel),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { activeComingSoonFeature = null },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("owner_menu_dismiss_coming_soon_button")
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

                // Responsive Grid of All Menu Buttons (Image buttons on top first, alongside the remaining buttons)
                val rows = allGridButtons.chunked(gridColumns)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (colIndex in 0 until gridColumns) {
                                val item = rowItems.getOrNull(colIndex)
                                if (item != null) {
                                    OwnerMenuGridButtonCard(
                                        item = item,
                                        isDark = isDark,
                                        cardBackground = cardBackground,
                                        cardBorderColor = cardBorderColor,
                                        onClick = { activeComingSoonFeature = item.title },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun OwnerMenuGridButtonCard(
    item: OwnerMenuGridItem,
    isDark: Boolean,
    cardBackground: Color,
    cardBorderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = cardBackground,
        border = BorderStroke(0.8.dp, cardBorderColor),
        shadowElevation = if (isDark) 0.dp else 2.dp,
        modifier = modifier
            .heightIn(min = 138.dp)
            .testTag(item.testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Red Rounded-Square Icon Badge + Right Chevron (or Verified Pill)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TelefamRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (item.showVerifiedActionPill) {
                    Surface(
                        color = TelefamRed,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = LucideOwnerMenuIcons.BadgeCheck,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = item.title,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = LucideOwnerMenuIcons.ChevronRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = LucideOwnerMenuIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Column: Title & Subtitle matching the reference image
            Column {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.subtitle,
                    fontSize = 12.sp,
                    lineHeight = 15.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Pure Lucide SVG Vector Icons (24x24 viewport, 2dp rounded strokes) for Owner Menu Screen.
 */
object LucideOwnerMenuIcons {

    val ArrowLeft: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideArrowLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 12f)
                lineTo(5f, 12f)
                moveTo(12f, 19f)
                lineTo(5f, 12f)
                lineTo(12f, 5f)
            }
        }.build()
    }

    val ChevronLeft: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideChevronLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 18f)
                lineTo(9f, 12f)
                lineTo(15f, 6f)
            }
        }.build()
    }

    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 18f)
                lineTo(15f, 12f)
                lineTo(9f, 6f)
            }
        }.build()
    }

    val BarChart: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideBarChart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Bar 1 (short left)
                moveTo(5f, 14f)
                lineTo(7.5f, 14f)
                lineTo(7.5f, 19f)
                lineTo(5f, 19f)
                close()
                // Bar 2 (medium middle)
                moveTo(10.75f, 10f)
                lineTo(13.25f, 10f)
                lineTo(13.25f, 19f)
                lineTo(10.75f, 19f)
                close()
                // Bar 3 (tall right)
                moveTo(16.5f, 5f)
                lineTo(19f, 5f)
                lineTo(19f, 19f)
                lineTo(16.5f, 19f)
                close()
            }
        }.build()
    }

    val Star: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideStar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3f)
                lineTo(14.8f, 8.6f)
                lineTo(21f, 9.5f)
                lineTo(16.5f, 13.9f)
                lineTo(17.6f, 20.1f)
                lineTo(12f, 17.2f)
                lineTo(6.4f, 20.1f)
                lineTo(7.5f, 13.9f)
                lineTo(3f, 9.5f)
                lineTo(9.2f, 8.6f)
                close()
            }
        }.build()
    }

    val Crown: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideCrown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 7f)
                lineTo(7.5f, 12f)
                lineTo(12f, 6f)
                lineTo(16.5f, 12f)
                lineTo(21f, 7f)
                lineTo(19f, 16f)
                lineTo(5f, 16f)
                close()
                moveTo(5f, 19f)
                lineTo(19f, 19f)
            }
        }.build()
    }

    val DollarBadge: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideDollarBadge",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Outer coin circle
                moveTo(21f, 12f)
                arcTo(9f, 9f, 0f, true, true, 3f, 12f)
                arcTo(9f, 9f, 0f, true, true, 21f, 12f)
                // Dollar vertical bar
                moveTo(12f, 6.5f)
                lineTo(12f, 17.5f)
                // Dollar S curve
                moveTo(14.8f, 9f)
                curveTo(14.2f, 7.8f, 10f, 7.6f, 9.5f, 9.5f)
                curveTo(9f, 11.3f, 15f, 11.7f, 14.5f, 14.2f)
                curveTo(14f, 16.2f, 9.8f, 16f, 9.2f, 14.6f)
            }
        }.build()
    }

    val Wallet: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideWallet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Top wallet fold
                moveTo(19f, 7f)
                lineTo(6f, 7f)
                curveTo(4.3f, 7f, 3f, 5.7f, 3f, 4.5f)
                curveTo(3f, 3.3f, 4.3f, 3f, 6f, 3f)
                lineTo(17f, 3f)
                // Main wallet body
                moveTo(3f, 5f)
                lineTo(3f, 18f)
                curveTo(3f, 19.7f, 4.3f, 21f, 6f, 21f)
                lineTo(19f, 21f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                lineTo(21f, 9f)
                curveTo(21f, 7.9f, 20.1f, 7f, 19f, 7f)
                // Clasp pocket on right
                moveTo(21f, 12f)
                lineTo(16.5f, 12f)
                curveTo(15.4f, 12f, 14.5f, 12.9f, 14.5f, 14f)
                curveTo(14.5f, 15.1f, 15.4f, 16f, 16.5f, 16f)
                lineTo(21f, 16f)
            }
        }.build()
    }

    val LinkedDevices: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideLinkedDevices",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Back larger device
                moveTo(14f, 9f)
                lineTo(14f, 5f)
                curveTo(14f, 3.9f, 13.1f, 3f, 12f, 3f)
                lineTo(6f, 3f)
                curveTo(4.9f, 3f, 4f, 3.9f, 4f, 5f)
                lineTo(4f, 16f)
                curveTo(4f, 17.1f, 4.9f, 18f, 6f, 18f)
                lineTo(9f, 18f)
                // Front linked smartphone
                moveTo(13f, 9f)
                lineTo(19f, 9f)
                curveTo(20.1f, 9f, 21f, 9.9f, 21f, 11f)
                lineTo(21f, 19f)
                curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
                lineTo(13f, 21f)
                curveTo(11.9f, 21f, 11f, 20.1f, 11f, 19f)
                lineTo(11f, 11f)
                curveTo(11f, 9.9f, 11.9f, 9f, 13f, 9f)
                close()
                // Home indicator dot
                moveTo(16f, 18f)
                lineTo(16.02f, 18f)
            }
        }.build()
    }

    val Megaphone: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideMegaphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Horn cone
                moveTo(3f, 10f)
                lineTo(11f, 9.5f)
                lineTo(19f, 5f)
                lineTo(19f, 17f)
                lineTo(11f, 13.5f)
                lineTo(3f, 13f)
                close()
                // Handle
                moveTo(6.5f, 13.5f)
                lineTo(8f, 19f)
                lineTo(10.5f, 19f)
                lineTo(9.5f, 13.5f)
                // Sound wave
                moveTo(21.5f, 9.5f)
                curveTo(22.3f, 10.5f, 22.3f, 11.8f, 21.5f, 12.8f)
            }
        }.build()
    }

    val BadgeCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideBadgeCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Scalloped rosette seal outline
                moveTo(12f, 2.5f)
                lineTo(14.8f, 4.2f)
                lineTo(18f, 3.8f)
                lineTo(19.2f, 6.8f)
                lineTo(21.5f, 9f)
                lineTo(20.5f, 12f)
                lineTo(21.5f, 15f)
                lineTo(19.2f, 17.2f)
                lineTo(18f, 20.2f)
                lineTo(14.8f, 19.8f)
                lineTo(12f, 21.5f)
                lineTo(9.2f, 19.8f)
                lineTo(6f, 20.2f)
                lineTo(4.8f, 17.2f)
                lineTo(2.5f, 15f)
                lineTo(3.5f, 12f)
                lineTo(2.5f, 9f)
                lineTo(4.8f, 6.8f)
                lineTo(6f, 3.8f)
                lineTo(9.2f, 4.2f)
                close()
                // Inner checkmark
                moveTo(9f, 12.2f)
                lineTo(11.1f, 14.3f)
                lineTo(15.3f, 10f)
            }
        }.build()
    }

    val Store: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideStore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 9f)
                lineTo(5f, 4f)
                lineTo(19f, 4f)
                lineTo(21f, 9f)
                close()
                moveTo(4f, 9f)
                lineTo(4f, 20f)
                lineTo(20f, 20f)
                lineTo(20f, 9f)
                moveTo(9f, 20f)
                lineTo(9f, 14f)
                lineTo(15f, 14f)
                lineTo(15f, 20f)
            }
        }.build()
    }

    val Clapperboard: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideClapperboard",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                lineTo(20f, 4f)
                lineTo(20f, 20f)
                lineTo(4f, 20f)
                close()
                moveTo(4f, 9f)
                lineTo(20f, 9f)
                moveTo(8f, 4f)
                lineTo(10f, 9f)
                moveTo(14f, 4f)
                lineTo(16f, 9f)
                moveTo(10f, 12.5f)
                lineTo(15f, 15f)
                lineTo(10f, 17.5f)
                close()
            }
        }.build()
    }

    val Users: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideUsers",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 7f)
                arcTo(4f, 4f, 0f, true, true, 5f, 7f)
                arcTo(4f, 4f, 0f, true, true, 13f, 7f)
                moveTo(2f, 21f)
                lineTo(2f, 19f)
                curveTo(2f, 16.8f, 4.2f, 15f, 7f, 15f)
                lineTo(11f, 15f)
                curveTo(13.8f, 15f, 16f, 16.8f, 16f, 19f)
                lineTo(16f, 21f)
                moveTo(16f, 3.13f)
                curveTo(17.76f, 3.58f, 19f, 5.14f, 19f, 7f)
                curveTo(19f, 8.86f, 17.76f, 10.42f, 16f, 10.87f)
                moveTo(22f, 21f)
                lineTo(22f, 19f)
                curveTo(22f, 17.1f, 20.5f, 15.5f, 18.5f, 15.1f)
            }
        }.build()
    }

    val MemoriesClock: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideMemoriesClock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 3f)
                lineTo(3f, 8f)
                lineTo(8f, 8f)
                moveTo(3.5f, 8f)
                curveTo(5.1f, 4.5f, 8.6f, 2.5f, 12.5f, 2.8f)
                curveTo(17.5f, 3.1f, 21.2f, 7.2f, 21f, 12.2f)
                curveTo(20.8f, 17.2f, 16.6f, 21.1f, 11.6f, 21f)
                curveTo(7.4f, 20.9f, 3.9f, 18f, 3.1f, 14f)
                moveTo(12f, 7f)
                lineTo(12f, 12f)
                lineTo(15.5f, 14f)
            }
        }.build()
    }

    val Gamepad2: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideGamepad2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 6f)
                lineTo(18f, 6f)
                curveTo(20.2f, 6f, 22f, 8.2f, 21.8f, 11f)
                lineTo(21.2f, 16.5f)
                curveTo(21f, 18.2f, 19.4f, 19f, 18f, 18f)
                lineTo(15.2f, 15.5f)
                lineTo(8.8f, 15.5f)
                lineTo(6f, 18f)
                curveTo(4.6f, 19f, 3f, 18.2f, 2.8f, 16.5f)
                lineTo(2.2f, 11f)
                curveTo(2f, 8.2f, 3.8f, 6f, 6f, 6f)
                close()
            }
        }.build()
    }

    val RadioLive: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideRadioLive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(14f, 12f)
                arcTo(2f, 2f, 0f, true, true, 10f, 12f)
                arcTo(2f, 2f, 0f, true, true, 14f, 12f)
                moveTo(7.8f, 7.8f)
                curveTo(5.5f, 10.1f, 5.5f, 13.9f, 7.8f, 16.2f)
                moveTo(16.2f, 7.8f)
                curveTo(18.5f, 10.1f, 18.5f, 13.9f, 16.2f, 16.2f)
                moveTo(4.9f, 4.9f)
                curveTo(1f, 8.8f, 1f, 15.2f, 4.9f, 19.1f)
                moveTo(19.1f, 4.9f)
                curveTo(23f, 8.8f, 23f, 15.2f, 19.1f, 19.1f)
            }
        }.build()
    }
}
