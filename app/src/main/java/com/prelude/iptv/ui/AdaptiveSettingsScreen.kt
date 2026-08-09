package com.prelude.iptv.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.prelude.iptv.R
import com.prelude.iptv.category.CategoryEditorState
import com.prelude.iptv.category.CategoryLayout
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.SourceLoadProgress
import com.prelude.iptv.player.BufferPolicy
import com.prelude.iptv.ui.components.settings.buildSettingsSources
import com.prelude.iptv.ui.epg.EpgSourceOption
import com.prelude.iptv.ui.epg.EpgStatus
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.localization.localizedAutoFrameRate
import com.prelude.iptv.ui.localization.localizedPlayerMode
import com.prelude.iptv.ui.mobile.settings.MobilePremiumSettingsScreen
import com.prelude.iptv.ui.tv.settings.TvPremiumSettingsScreen

@Composable
fun AdaptiveSettingsScreen(
    playlists: List<Playlist>,
    currentIndex: Int,
    currentChannelCount: Int,
    sourceProgress: Map<String, SourceLoadProgress>,
    profileName: String,
    playerMode: String,
    autoFrameRateMode: String,
    bufferProfile: String,
    tvHomeEnabled: Boolean,
    tvHomeMyListEnabled: Boolean,
    fontScale: Float,
    epgEnabled: Boolean,
    epgLoaded: Boolean,
    epgStatus: EpgStatus,
    epgSources: List<EpgSourceOption>,
    tmdbConfigured: Boolean,
    subtitlesConfigured: Boolean,
    version: String,
    onAddSource: (Int) -> Unit,
    onOpenSource: (Int) -> Unit,
    onEditSource: (Int) -> Unit,
    onDeleteSource: (Int) -> Unit,
    onRefreshCurrentSource: () -> Unit,
    onDialog: (String) -> Unit,
    onToggleEpg: () -> Unit,
    onSearchEpg: () -> Unit,
    onUseEpgSource: (String) -> Unit,
    onCloseEpgSearch: () -> Unit,
    categoryEditorState: CategoryEditorState,
    onEditCategories: () -> Unit,
    onCategoryLayoutChange: (String, CategoryLayout) -> Unit,
    onSaveCategories: () -> Unit,
    onClearHomeHistory: (String) -> Unit,
    onToggleTvHome: () -> Unit,
    onToggleTvHomeMyList: () -> Unit,
    onClearTmdbCache: () -> Unit,
    onShare: () -> Unit,
    onNavigationCollapsedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sources = buildSettingsSources(playlists, currentIndex, currentChannelCount, sourceProgress)
    val activePlaylist = playlists.getOrNull(currentIndex)
    var pendingDelete by remember { mutableStateOf<com.prelude.iptv.ui.components.settings.SettingsSourceUi?>(null) }
    val playerLabel = localizedPlayerMode(playerMode)
    val autoFrameRateLabel = localizedAutoFrameRate(autoFrameRateMode)
    val bufferLabel = stringResource(BufferPolicy.fromStorage(bufferProfile).labelRes())
    if (isTvDevice()) {
        TvPremiumSettingsScreen(
            sources = sources,
            profileName = profileName,
            playerLabel = playerLabel,
            autoFrameRateLabel = autoFrameRateLabel,
            bufferLabel = bufferLabel,
            tvHomeEnabled = tvHomeEnabled,
            tvHomeMyListEnabled = tvHomeMyListEnabled,
            fontScale = fontScale,
            epgEnabled = epgEnabled,
            tmdbConfigured = tmdbConfigured,
            subtitlesConfigured = subtitlesConfigured,
            version = version,
            onAddSource = onAddSource,
            onOpenSource = onOpenSource,
            onEditSource = onEditSource,
            onDeleteSource = { index -> pendingDelete = sources.firstOrNull { it.index == index } },
            onRefreshCurrentSource = onRefreshCurrentSource,
            onDialog = onDialog,
            onToggleEpg = onToggleEpg,
            onToggleTvHome = onToggleTvHome,
            onToggleTvHomeMyList = onToggleTvHomeMyList,
            onClearTmdbCache = onClearTmdbCache,
            onShare = onShare,
            modifier = modifier
        )
    } else {
        MobilePremiumSettingsScreen(
            sources = sources,
            profileName = profileName,
            playerLabel = playerLabel,
            fontScale = fontScale,
            epgEnabled = epgEnabled,
            epgLoaded = epgLoaded,
            epgStatus = epgStatus,
            epgSources = epgSources,
            currentEpgUrl = activePlaylist?.epgUrl.orEmpty(),
            currentSourceType = sources.firstOrNull { it.current }?.typeLabel ?: stringResource(R.string.epg_settings_unknown_source),
            tmdbConfigured = tmdbConfigured,
            subtitlesConfigured = subtitlesConfigured,
            version = version,
            onAddSource = onAddSource,
            onOpenSource = onOpenSource,
            onEditSource = onEditSource,
            onDeleteSource = { index -> pendingDelete = sources.firstOrNull { it.index == index } },
            onRefreshCurrentSource = onRefreshCurrentSource,
            onDialog = onDialog,
            onToggleEpg = onToggleEpg,
            onSearchEpg = onSearchEpg,
            onUseEpgSource = onUseEpgSource,
            onCloseEpgSearch = onCloseEpgSearch,
            categoryEditorState = categoryEditorState,
            onEditCategories = onEditCategories,
            onCategoryLayoutChange = onCategoryLayoutChange,
            onSaveCategories = onSaveCategories,
            onClearHomeHistory = onClearHomeHistory,
            onClearTmdbCache = onClearTmdbCache,
            onShare = onShare,
            onNavigationCollapsedChange = onNavigationCollapsedChange,
            modifier = modifier
        )
    }
    pendingDelete?.let { source ->
        val deleteFocus = rememberInitialFocus(key = source.index)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = IptvColors.SurfaceRaised,
            title = { Text(stringResource(R.string.settings_delete_source_title), color = Color.White, fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    stringResource(R.string.settings_delete_source_message, source.name),
                    color = IptvColors.TextSecondary
                )
            },
            confirmButton = {
                TvDialogTextButton(
                    label = stringResource(R.string.settings_delete),
                    color = IptvColors.Error,
                    modifier = Modifier.focusRequester(deleteFocus),
                    onClick = { onDeleteSource(source.index); pendingDelete = null }
                )
            },
            dismissButton = {
                TvDialogTextButton(label = stringResource(R.string.settings_cancel), color = Color.White, onClick = { pendingDelete = null })
            }
        )
    }

}
