package com.prelude.iptv.ui.mobile.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.prelude.iptv.ui.IptvColors

internal val PremiumMobileBottomBarHeight: Dp = 68.dp
internal val PremiumMobileMiniPlayerHeight: Dp = 68.dp
internal val PremiumMobileBottomDockFallback: Dp = 78.dp
private val PremiumMobileBottomBarSafeGap: Dp = 10.dp
private val PremiumMobileBottomBarHorizontalGap: Dp = 14.dp

/**
 * Shared visual contract between the single mobile navigation and the inline
 * player. There can only be one mobile playback overlay in the app shell.
 */
internal object MobilePlayerDockState {
    var isDocked by mutableStateOf(false)
    var navigationHeightPx by mutableIntStateOf(0)
}

@Composable
internal fun premiumMobileNavigationContentPadding(extra: Dp = 14.dp): Dp =
    PremiumMobileBottomBarHeight +
        PremiumMobileBottomBarSafeGap +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        extra

/**
 * The single mobile navigation used by all main pages.
 *
 * [collapsed] turns the full bar into a small floating menu button. Tapping the
 * button temporarily expands the complete navigation without changing page
 * content or scroll position.
 */
@Composable
internal fun PremiumMobileBottomNavigation(
    selected: String,
    onHome: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onLive: () -> Unit,
    onSearch: () -> Unit,
    onMyList: () -> Unit,
    onSettings: () -> Unit,
    collapsed: Boolean = false,
    modifier: Modifier = Modifier
) {
    var manuallyExpanded by remember { mutableStateOf(false) }
    var browseOpen by remember { mutableStateOf(false) }

    /**
     * Every destination goes through the same exit path.  Previously the
     * callback changed page while the locally expanded hamburger/menu state
     * stayed alive.  That was especially visible when the selected
     * destination did not change (Search -> Search, Settings -> Settings).
     */
    fun closeMenuAndNavigate(action: () -> Unit) {
        browseOpen = false
        manuallyExpanded = false
        action()
    }

    LaunchedEffect(collapsed) {
        if (!collapsed) manuallyExpanded = false
    }
    LaunchedEffect(selected) {
        // Also close transient UI when navigation is driven externally.
        browseOpen = false
        manuallyExpanded = false
    }
    val playerDocked = MobilePlayerDockState.isDocked
    // The HTML-approved dock always shows the full menu below the mini player.
    val compact = collapsed && !manuallyExpanded && !playerDocked
    LaunchedEffect(compact, selected) {
        if (compact) browseOpen = false
    }

    BackHandler(enabled = browseOpen || manuallyExpanded) {
        if (browseOpen) browseOpen = false else manuallyExpanded = false
    }

    Box(modifier.fillMaxWidth().zIndex(20f)) {
        AnimatedVisibility(
            visible = browseOpen && !compact,
            enter = fadeIn() + scaleIn(initialScale = .94f),
            exit = fadeOut() + scaleOut(targetScale = .94f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 24.dp)
                .padding(
                    bottom = PremiumMobileBottomBarHeight +
                        PremiumMobileBottomBarSafeGap +
                        (if (playerDocked) PremiumMobileMiniPlayerHeight else 0.dp) +
                        20.dp
                )
        ) {
            MobileBrowsePanel(
                selected = selected,
                onLive = { closeMenuAndNavigate(onLive) },
                onMovies = { closeMenuAndNavigate(onMovies) },
                onSeries = { closeMenuAndNavigate(onSeries) },
                onMyList = { closeMenuAndNavigate(onMyList) },
            )
        }

        AnimatedVisibility(
            visible = !compact,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FullNavigation(
                selected = selected,
                onHome = { closeMenuAndNavigate(onHome) },
                onSearch = { closeMenuAndNavigate(onSearch) },
                onSettings = { closeMenuAndNavigate(onSettings) },
                onBrowse = { browseOpen = !browseOpen },
                dockedTop = playerDocked,
            )
        }

        AnimatedVisibility(
            visible = compact,
            enter = fadeIn() + scaleIn(initialScale = .72f),
            exit = fadeOut() + scaleOut(targetScale = .72f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(start = 18.dp, bottom = PremiumMobileBottomBarSafeGap)
        ) {
            Surface(
                shape = CircleShape,
                color = IptvColors.TextPrimary,
                contentColor = IptvColors.Primary,
                shadowElevation = 16.dp,
                modifier = Modifier.size(62.dp)
            ) {
                IconButton(onClick = { manuallyExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Άνοιγμα μενού",
                        tint = IptvColors.Primary,
                        modifier = Modifier.size(31.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FullNavigation(
    selected: String,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onBrowse: () -> Unit,
    dockedTop: Boolean,
) {
    val browseSelected = selected == "live" || selected == "movies" ||
        selected == "series" || selected == "library"
    val browseSection = when (selected) {
        "live" -> "Live TV"
        "movies" -> "Movies"
        "series" -> "Series"
        "library" -> "Favorites"
        else -> ""
    }
    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { MobilePlayerDockState.navigationHeightPx = it.height }
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(
                start = PremiumMobileBottomBarHorizontalGap,
                end = PremiumMobileBottomBarHorizontalGap,
                bottom = PremiumMobileBottomBarSafeGap
            )
    ) {
        Surface(
            color = Color.Black.copy(alpha = .84f),
            contentColor = IptvColors.TextPrimary,
            shape = RoundedCornerShape(
                topStart = if (dockedTop) 0.dp else 22.dp,
                topEnd = if (dockedTop) 0.dp else 22.dp,
                bottomStart = 22.dp,
                bottomEnd = 22.dp,
            ),
            tonalElevation = 0.dp,
            shadowElevation = 18.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(PremiumMobileBottomBarHeight)
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PremiumMobileNavItem("Home", Icons.Default.Home, selected == "home", onHome, Modifier.weight(1f))
                PremiumMobileNavItem(
                    label = "Browse",
                    icon = Icons.Default.GridView,
                    selected = browseSelected,
                    onClick = onBrowse,
                    modifier = Modifier.weight(1f),
                    secondary = browseSection,
                )
                PremiumMobileNavItem("Search", Icons.Default.Search, selected == "search", onSearch, Modifier.weight(1f))
                PremiumMobileNavItem("Settings", Icons.Default.Settings, selected == "settings", onSettings, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PremiumMobileNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: String = "",
) {
    Column(
        modifier
            .padding(horizontal = 2.dp)
            .background(
                if (selected) IptvColors.TextPrimary else Color.Transparent,
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) IptvColors.Primary else IptvColors.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (selected) IptvColors.Primary else IptvColors.TextSecondary,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        if (secondary.isNotBlank()) {
            Text(
                text = secondary,
                color = IptvColors.Primary,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        } else {
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
private fun MobileBrowsePanel(
    selected: String,
    onLive: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onMyList: () -> Unit,
) {
    Surface(
        color = IptvColors.Surface.copy(alpha = .98f),
        contentColor = IptvColors.TextPrimary,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 0.dp,
        shadowElevation = 22.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                BrowseDestination(
                    title = "Live TV",
                    subtitle = "Ζωντανά κανάλια",
                    icon = Icons.Default.LiveTv,
                    selected = selected == "live",
                    onClick = onLive,
                    modifier = Modifier.weight(1f),
                )
                BrowseDestination(
                    title = "Movies",
                    subtitle = "Ταινίες",
                    icon = Icons.Default.Movie,
                    selected = selected == "movies",
                    onClick = onMovies,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                BrowseDestination(
                    title = "Series",
                    subtitle = "Σειρές",
                    icon = Icons.Default.Tv,
                    selected = selected == "series",
                    onClick = onSeries,
                    modifier = Modifier.weight(1f),
                )
                BrowseDestination(
                    title = "Favorites",
                    subtitle = "Η λίστα μου",
                    icon = Icons.Default.Bookmark,
                    selected = selected == "library",
                    onClick = onMyList,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BrowseDestination(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) IptvColors.SurfaceSelected else IptvColors.SurfaceRaised)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (selected) IptvColors.Primary else IptvColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = IptvColors.TextPrimary, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, color = IptvColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(subtitle, color = IptvColors.TextTertiary, fontSize = 8.sp, maxLines = 1)
        }
    }
}
