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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.R
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.navigation.PrimaryContentDestination

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
@Suppress("UNUSED_PARAMETER")
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
    showSettingsAction: Boolean = false,
    modifier: Modifier = Modifier
) {
    var manuallyExpanded by remember { mutableStateOf(false) }

    /**
     * Every destination goes through the same exit path so the compact menu
     * cannot remain expanded over the next screen.
     */
    fun closeMenuAndNavigate(action: () -> Unit) {
        manuallyExpanded = false
        action()
    }

    LaunchedEffect(collapsed) {
        if (!collapsed) manuallyExpanded = false
    }
    LaunchedEffect(selected) {
        // Also close transient UI when navigation is driven externally.
        manuallyExpanded = false
    }
    val playerDocked = MobilePlayerDockState.isDocked
    // The HTML-approved dock always shows the full menu below the mini player.
    val compact = collapsed && !manuallyExpanded && !playerDocked
    BackHandler(enabled = manuallyExpanded) {
        manuallyExpanded = false
    }

    Box(modifier.fillMaxWidth().zIndex(20f)) {
        AnimatedVisibility(
            visible = !compact,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FullNavigation(
                selected = selected,
                onHome = { closeMenuAndNavigate(onHome) },
                onLive = { closeMenuAndNavigate(onLive) },
                onMovies = { closeMenuAndNavigate(onMovies) },
                onSeries = { closeMenuAndNavigate(onSeries) },
                onSearch = { closeMenuAndNavigate(onSearch) },
                dockedTop = playerDocked,
            )
        }

        AnimatedVisibility(
            visible = showSettingsAction && !compact,
            enter = fadeIn() + scaleIn(initialScale = .8f),
            exit = fadeOut() + scaleOut(targetScale = .8f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(
                    end = 20.dp,
                    bottom = PremiumMobileBottomBarHeight +
                        PremiumMobileBottomBarSafeGap +
                        (if (playerDocked) PremiumMobileMiniPlayerHeight else 0.dp) +
                        12.dp,
                ),
        ) {
            MobileSettingsAction(onClick = { closeMenuAndNavigate(onSettings) })
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
                        contentDescription = stringResource(R.string.a11y_open_navigation),
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
    onLive: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onSearch: () -> Unit,
    dockedTop: Boolean,
) {
    val selectedPrimaryRoute = PrimaryContentDestination.selectionRoute(selected)
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
                PrimaryContentDestination.ordered.forEach { destination ->
                    val (icon, action) = when (destination) {
                        PrimaryContentDestination.HOME -> Icons.Default.Home to onHome
                        PrimaryContentDestination.LIVE -> Icons.Default.LiveTv to onLive
                        PrimaryContentDestination.MOVIES -> Icons.Default.Movie to onMovies
                        PrimaryContentDestination.SERIES -> Icons.Default.Tv to onSeries
                        PrimaryContentDestination.SEARCH -> Icons.Default.Search to onSearch
                    }
                    PremiumMobileNavItem(
                        destination = destination,
                        icon = icon,
                        selectedRoute = selectedPrimaryRoute,
                        onClick = action,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumMobileNavItem(
    destination: PrimaryContentDestination,
    icon: ImageVector,
    selectedRoute: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = destination.route == selectedRoute
    val label = stringResource(destination.labelRes())
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
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}
