#!/usr/bin/env python3
"""Static architecture/focus/routing contracts for safe large-file refactors."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java"
failures: list[str] = []
warnings: list[str] = []
passes: list[str] = []


def text(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def require(condition: bool, message: str) -> None:
    (passes if condition else failures).append(message)


# Size budget: MainViewModel is now below the emergency threshold. PlayerActivity
# remains a staged migration because moving engine/subtitle state all at once is
# more dangerous than leaving an explicit warning and tested seams.
for path in sorted(SRC.rglob("*.kt")):
    lines = len(path.read_text(encoding="utf-8").splitlines())
    rel = path.relative_to(ROOT)
    if lines > 1500:
        warnings.append(f"large file: {rel} = {lines} lines")

main_vm = text("app/src/main/java/com/prelude/iptv/ui/MainViewModel.kt")
player = text("app/src/main/java/com/prelude/iptv/PlayerActivity.kt")
require(len(main_vm.splitlines()) < 2000, "MainViewModel stays below 2,000 lines")
require("private fun cacheKey" in main_vm and main_vm.count("private fun cacheKey") == 1,
        "MainViewModel has one catalog key boundary")
require("intent.getStringExtra" not in player and "intent.getIntExtra" not in player,
        "PlayerActivity reads launch data only through PlayerLaunchRequest")

# Every direct player route must use the typed boundary; raw extras are forbidden.
raw_player_extra = re.compile(r"putExtra\(\s*[\"'](?:url|title|kind|tvgId|posKey|sourceId)[\"']")
violations: list[str] = []
for path in SRC.rglob("*.kt"):
    if path.name == "PlayerLaunchRequest.kt":
        continue
    body = path.read_text(encoding="utf-8")
    if raw_player_extra.search(body):
        violations.append(str(path.relative_to(ROOT)))
require(not violations, "No raw PlayerActivity extras outside PlayerLaunchRequest" +
        (f" ({', '.join(violations)})" if violations else ""))

all_source = "\n".join(p.read_text(encoding="utf-8") for p in SRC.rglob("*.kt"))
require("nativeKeyEvent" not in all_source, "Compose TV input avoids nativeKeyEvent internals")
require("Runtime.getRuntime().exit" not in all_source, "Profile routing never kills the new task/process")

raw_skip = []
for path in SRC.rglob("*.kt"):
    body = path.read_text(encoding="utf-8")
    if '"skip_profile_gate"' in body and path.name != "AppRouteContract.kt":
        raw_skip.append(str(path.relative_to(ROOT)))
require(not raw_skip, "Profile-gate extra is centralized in AppRouteContract")

# Top-level TV destinations must have a reliable initial-focus boundary.
focus_contracts = {
    "app/src/main/java/com/prelude/iptv/ui/tv/home/TvPremiumHomeScreen.kt": "rememberInitialFocus",
    "app/src/main/java/com/prelude/iptv/ui/tv/epg/TvEpgScreen.kt": "rememberInitialFocus",
    "app/src/main/java/com/prelude/iptv/ui/tv/search/TvPremiumSearchScreen.kt": "rememberInitialFocus",
    "app/src/main/java/com/prelude/iptv/ui/tv/settings/TvPremiumSettingsScreen.kt": "rememberInitialFocus",
    "app/src/main/java/com/prelude/iptv/ui/tv/details/TvPremiumDetailScreen.kt": "rememberInitialFocus",
    "app/src/main/java/com/prelude/iptv/ui/tv/live/TvLiveRail.kt": "rememberInitialFocus",
    "app/src/main/java/com/prelude/iptv/ui/tv/library/TvLibraryComponents.kt": "rememberInitialFocus",
}
for rel, marker in focus_contracts.items():
    require(marker in text(rel), f"TV focus boundary: {Path(rel).name}")

interaction = text("app/src/main/java/com/prelude/iptv/ui/TvInteraction.kt")
require("requestFocusWithRetry" in interaction and "INITIAL_FOCUS_ATTEMPTS" in interaction,
        "Initial focus retries across lazy-layout frames")
chrome = text("app/src/main/java/com/prelude/iptv/player/PlayerChromeController.kt")
player_host = text("app/src/main/java/com/prelude/iptv/ui/player/PlayerHost.kt")
tv_overlay = text("app/src/main/java/com/prelude/iptv/ui/player/TvPlaybackOverlay.kt")
mobile_overlay = text("app/src/main/java/com/prelude/iptv/ui/player/MobilePlaybackOverlay.kt")
playback_engine = text("app/src/main/java/com/prelude/iptv/player/PlaybackEngine.kt")
require("TvPlaybackOverlay(" in player and "PlayerHost(" in tv_overlay,
        "External PlayerActivity delegates visibility, timers and focus to the shared overlay/host")
require("scheduleFocusAttempt" in chrome and "maxFocusRetries" in chrome,
        "Player chrome focus recovery is bounded and testable")
require("handler.postDelayed(hideOverlay" not in player and "restoreOverlayFocus" not in player,
        "PlayerActivity no longer owns chrome auto-hide/focus retry runnables")
require(".onPreviewKeyEvent" in player_host and
        "Key.ChannelUp" in player_host and "Key.ChannelDown" in player_host and
        "KeyEventType.KeyDown" in player_host and "KeyEventType.KeyUp" in player_host,
        "Shared PlayerHost owns TV shortcut and full key-cycle behavior")
require("loadToken" not in player and "PlayerLaunchRequest.fromIntent" in player,
        "PlayerActivity delegates typed launch/session state to the shared overlay")
require(all(marker in tv_overlay for marker in ["loadResumeMs(channel)", "saveResumeMs", "DisposableEffect(channel)"]) and
        all(marker in mobile_overlay for marker in ["loadResumeMs(channel)", "saveResumeMs", "DisposableEffect(channel)"]),
        "Shared mobile/TV overlays own resume load, periodic save and final save")
require("handler.removeCallbacksAndMessages(null)" in playback_engine and
        "engine.release()" in tv_overlay and "engine.release()" in mobile_overlay,
        "Shared playback teardown clears engine callbacks and releases both overlays")

launchers = text("app/src/main/java/com/prelude/iptv/ui/route/PlaybackLaunchers.kt")
require("catch (e: kotlinx.coroutines.CancellationException)" in launchers and "throw e" in launchers,
        "Multiview player launch preserves coroutine cancellation")
coordinator = text("app/src/main/java/com/prelude/iptv/ui/coordinator/MainEpgCoordinator.kt")
require("runCatching { EpgSourceDirectory.findForChannels" not in coordinator,
        "EPG directory cancellation cannot publish late empty results")

catalog_loader = text("app/src/main/java/com/prelude/iptv/ui/coordinator/CatalogLoadCoordinator.kt")
require("CatalogLoadCoordinator(" in main_vm and "catalogLoader.section(" in main_vm,
        "MainViewModel delegates provider catalog loading to CatalogLoadCoordinator")
require("private suspend fun fetchChannels" not in main_vm and "providerLoadMutex" not in main_vm,
        "MainViewModel no longer owns provider dispatch or provider mutex")
require("private val providerMutex = Mutex()" in catalog_loader and "withProviderLock" in catalog_loader,
        "CatalogLoadCoordinator owns one serialized provider boundary")
require(main_vm.count("restoreAfterRefreshFailure") >= 2,
        "Progressive refresh failures roll back the prior visible catalog")

source_switch = text("app/src/main/java/com/prelude/iptv/ui/coordinator/SourceSwitchCoordinator.kt")
select_start = main_vm.find("    fun selectPlaylist(i: Int)")
select_end = main_vm.find("    fun saveFontScale", select_start)
select_body = main_vm[select_start:select_end]
require("sourceSwitchCoordinator.switchTo(i)" in select_body and "cancelActiveLoad" not in select_body,
        "MainViewModel delegates source transitions to SourceSwitchCoordinator")
require("private var loadGen" not in main_vm and "private var seriesLoadGen" not in main_vm,
        "MainViewModel no longer owns raw source generation counters")
require("class SourceGenerationGate" in source_switch and "beginSeriesRequest" in source_switch,
        "SourceGenerationGate owns catalog and series freshness tokens")
require(source_switch.find("getOrNull(index) ?: return false") < source_switch.find("persistLastPlaylist(index)"),
        "Invalid source index cannot mutate persisted selection")
require(source_switch.find("generationGate.invalidateAll()") < source_switch.find("callbacks.cancelActiveWork()") < source_switch.find("callbacks.publish(plan)"),
        "Source switch invalidates, cancels and only then publishes")
require(all(marker in source_switch for marker in ["loading = false", "status = \"\"", "selectedGroup = UiState.ALL_GROUP", "seriesLoading = false"]),
        "Source switch clears stale source-bound UI state")

series_loader = text("app/src/main/java/com/prelude/iptv/ui/coordinator/SeriesLoadCoordinator.kt")
series_start = main_vm.find("    fun openSeries(ch: Channel)")
series_end = main_vm.find("    fun closeSeries()", series_start)
series_body = main_vm[series_start:series_end]
require("seriesLoader.load(" in series_body and "XtreamClient.seriesEpisodes" not in series_body,
        "MainViewModel delegates series provider loading to SeriesLoadCoordinator")
require(all(marker in series_loader for marker in [
            "withProviderLock", "Repository.stalkerLoad", "XtreamClient.seriesEpisodes", "SeriesLoadPolicy.resolve"
        ]),
        "SeriesLoadCoordinator owns serialized series expansion and fallback policy")
require("seriesLoader.cancel()" in main_vm and "pendingStalker?.cancelPendingRequests()" in series_loader,
        "Series refresh cancellation reaches its temporary Stalker connection")

require("profilesState" in main_vm, "Profile list is observable instead of a stale remembered snapshot")
require("rememberSaveable" in text("app/src/main/java/com/prelude/iptv/MainActivity.kt"),
        "App-shell route state survives recreation")



# Deep validation gate contracts.
tv_route = text("app/src/main/java/com/prelude/iptv/tvhome/TvHomePlaybackRoutePolicy.kt")
require("pathSegments.size != 1" in tv_route and "UUID.fromString" in tv_route,
        "TV Home routes accept exactly one canonical UUID token")
tv_worker = text("app/src/main/java/com/prelude/iptv/tvhome/TvHomeSyncWorker.kt")
require("catch (cancelled: CancellationException)" in tv_worker and "throw cancelled" in tv_worker,
        "CoroutineWorker preserves structured cancellation")
require("SourceDeletionPolicy.decide" in main_vm and "if (!decision.removedActiveSource)" in main_vm,
        "Deleting an inactive source preserves the active catalog/load")
require("val editingActiveSource = index == before.currentIndex" in main_vm,
        "Editing an inactive source preserves the active catalog/load")
require("testInstrumentationRunner" in text("app/build.gradle.kts") and
        (ROOT / "app/src/androidTest/java/com/prelude/iptv/ui/route/TvDialogFocusInstrumentedTest.kt").exists(),
        "Instrumentation foundation covers TV dialog focus")

playlist_store = text("app/src/main/java/com/prelude/iptv/data/PlaylistStore.kt")
playback_history = text("app/src/main/java/com/prelude/iptv/data/PlaybackHistoryStore.kt")
require("PlaybackHistoryStore(" in playlist_store and "playbackHistory.loadRecents" in playlist_store,
        "PlaylistStore delegates playback history through its stable facade")
require("private fun historyScope" not in playlist_store and "private fun movePosition" not in playlist_store,
        "PlaylistStore no longer owns source-scoped history internals")
require(all(marker in playback_history for marker in [
            "fun migrateLegacyHistory", "fun reconcileHistory", "fun savePosition", "fun clearHistory"
        ]),
        "PlaybackHistoryStore owns migrations, reconciliation and resume persistence")

export_relay = text("app/src/main/java/com/prelude/iptv/ui/coordinator/ExportRelayCoordinator.kt")
require("private val exportRelay" in main_vm and main_vm.count("exportRelay.") >= 4,
        "MainViewModel delegates relay and resolved M3U export")
require("RelayHub." not in main_vm and all(marker in export_relay for marker in [
            "fun startRelay", "fun stopRelay", "fun exportableChannels", "fun buildResolvedM3u"
        ]),
        "ExportRelayCoordinator owns relay lifecycle and export preparation")

category_editor = text("app/src/main/java/com/prelude/iptv/ui/coordinator/CategoryEditorCoordinator.kt")
require("private val categoryEditor" in main_vm and main_vm.count("categoryEditor.") >= 6,
        "MainViewModel delegates category-editor state and actions")
require(all(marker not in main_vm for marker in [
            "_categoryEditor", "_categoryLayoutRevision", "updateCategoryEditorSection"
        ]) and all(marker in category_editor for marker in [
            "fun open()", "fun updateLayout", "fun save()", "private fun updateSection"
        ]),
        "CategoryEditorCoordinator owns loading, draft state and persistence normalization")

catalog_presentation = text("app/src/main/java/com/prelude/iptv/ui/policy/CatalogPresentationPolicy.kt")
require(main_vm.count("CatalogPresentationPolicy.") == 2 and
        "s.channels.filter" not in main_vm and "LinkedHashSet<String>()" not in main_vm,
        "MainViewModel delegates catalog group ordering and visibility")
require(all(marker in catalog_presentation for marker in [
            "fun groups(", "fun visibleChannels(", "lockedGroups", '"year"'
        ]),
        "CatalogPresentationPolicy owns grouping, parental filtering and sorting")

mobile_playback = text("app/src/main/java/com/prelude/iptv/ui/player/MobilePlaybackOverlay.kt")
live_transition = text("app/src/main/java/com/prelude/iptv/ui/player/MobileLiveChannelTransition.kt")
require("MobileLiveChannelTransition(" in mobile_playback and
        "channelFlash" not in mobile_playback and "ChevronRight" not in mobile_playback,
        "Mobile live zapping uses directional refraction without arrow feedback")
require(all(marker in live_transition for marker in [
            "LiveChannelTransitionMotion", "fun edgeFraction", "fun intensity", "Canvas(modifier)"
        ]),
        "Live channel transition keeps deterministic motion separate from playback")

mobile_controls = text("app/src/main/java/com/prelude/iptv/ui/player/MobilePlayerControls.kt")
scrubber_start = mobile_controls.find("private fun Scrubber(")
scrubber_end = mobile_controls.find("private fun QualityBadge", scrubber_start)
scrubber = mobile_controls[scrubber_start:scrubber_end]
require(scrubber.count(".height(2.dp)") == 2 and ".size(8.dp)" in scrubber and
        ".height(26.dp)" in scrubber,
        "Mobile scrubber is visually slim while retaining its touch target")

print("ARCHITECTURE AUDIT")
for item in passes:
    print(f"PASS  {item}")
for item in warnings:
    print(f"WARN  {item}")
for item in failures:
    print(f"FAIL  {item}")
print(f"SUMMARY pass={len(passes)} warn={len(warnings)} fail={len(failures)}")
sys.exit(1 if failures else 0)
