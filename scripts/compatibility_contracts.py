#!/usr/bin/env python3
"""Compatibility contracts for the current player and MainViewModel boundaries."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTRACTS = ROOT / "contracts"
TARGETS = {
    "MainViewModel": ROOT / "app/src/main/java/com/prelude/iptv/ui/MainViewModel.kt",
}


def public_members(path: Path) -> set[str]:
    members: set[str] = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        # Class members in these legacy classes use exactly one indentation level.
        if not line.startswith("    ") or line.startswith("        "):
            continue
        source = line.strip()
        if source.startswith("private "):
            continue
        fn = re.match(
            r"(?:(?:public|protected|internal)\s+)?"
            r"(?:(?:override|open|final|abstract)\s+)*"
            r"(?:suspend\s+)?fun\s+([A-Za-z0-9_]+)\s*\(",
            source,
        )
        if fn:
            members.add(f"fun {fn.group(1)}")
            continue
        prop = re.match(
            r"(?:(?:public|protected|internal)\s+)?"
            r"(?:(?:override|open|final|abstract)\s+)*"
            r"(?:lateinit\s+)?(?:val|var)\s+([A-Za-z0-9_]+)\b",
            source,
        )
        if prop:
            members.add(f"property {prop.group(1)}")
    return members


failures: list[str] = []
for name, source in TARGETS.items():
    baseline_path = CONTRACTS / f"{name}.v1_40_41.api"
    baseline = {
        line.strip()
        for line in baseline_path.read_text(encoding="utf-8").splitlines()
        if line.strip()
    }
    current = public_members(source)
    missing = sorted(baseline - current)
    if missing:
        failures.append(f"{name} missing public members: {', '.join(missing)}")
    else:
        print(f"PASS {name}: preserved {len(baseline)} v1.40.41 public members")

player = (ROOT / "app/src/main/java/com/prelude/iptv/PlayerActivity.kt").read_text(encoding="utf-8")
controller = (ROOT / "app/src/main/java/com/prelude/iptv/player/PlayerSessionController.kt").read_text(encoding="utf-8")
chrome = (ROOT / "app/src/main/java/com/prelude/iptv/player/PlayerChromeController.kt").read_text(encoding="utf-8")
playback_layer = (ROOT / "app/src/main/java/com/prelude/iptv/ui/route/BrowsePlaybackLayer.kt").read_text(encoding="utf-8")
catalog_loader = (ROOT / "app/src/main/java/com/prelude/iptv/ui/coordinator/CatalogLoadCoordinator.kt").read_text(encoding="utf-8")
series_loader = (ROOT / "app/src/main/java/com/prelude/iptv/ui/coordinator/SeriesLoadCoordinator.kt").read_text(encoding="utf-8")
source_switch = (ROOT / "app/src/main/java/com/prelude/iptv/ui/coordinator/SourceSwitchCoordinator.kt").read_text(encoding="utf-8")
player_video_surface = (ROOT / "app/src/main/java/com/prelude/iptv/ui/player/PlayerVideoSurface.kt").read_text(encoding="utf-8")
player_host = (ROOT / "app/src/main/java/com/prelude/iptv/ui/player/PlayerHost.kt").read_text(encoding="utf-8")
player_controls = (ROOT / "app/src/main/java/com/prelude/iptv/ui/player/PlayerControls.kt").read_text(encoding="utf-8")
playback_engine = (ROOT / "app/src/main/java/com/prelude/iptv/player/PlaybackEngine.kt").read_text(encoding="utf-8")
proguard_rules = (ROOT / "app/proguard-rules.pro").read_text(encoding="utf-8")
add_playlist = (ROOT / "app/src/main/java/com/prelude/iptv/AddPlaylistScreen.kt").read_text(encoding="utf-8")
tv_add_playlist = (ROOT / "app/src/main/java/com/prelude/iptv/ui/tv/sources/TvAddPlaylistScreen.kt").read_text(encoding="utf-8")
mobile_add_playlist = (ROOT / "app/src/main/java/com/prelude/iptv/ui/mobile/sources/MobileAddPlaylistScreen.kt").read_text(encoding="utf-8")
playlist_connection_tester = (ROOT / "app/src/main/java/com/prelude/iptv/ui/sources/PlaylistConnectionTester.kt").read_text(encoding="utf-8")
shared_m3u_importer = (ROOT / "app/src/main/java/com/prelude/iptv/ui/sources/M3uFileImporter.kt").read_text(encoding="utf-8")
main_activity = (ROOT / "app/src/main/java/com/prelude/iptv/MainActivity.kt").read_text(encoding="utf-8")
playlist_sources = (ROOT / "app/src/main/java/com/prelude/iptv/ui/route/PlaylistSourcesScreen.kt").read_text(encoding="utf-8")
main_vm = TARGETS["MainViewModel"].read_text(encoding="utf-8")
select_start = main_vm.find("    fun selectPlaylist(i: Int)")
select_end = main_vm.find("    fun saveFontScale", select_start)
select_body = main_vm[select_start:select_end]

source_contracts = {
    "External PlayerActivity keeps its manifest launch class": "class PlayerActivity : ComponentActivity()" in player,
    "External PlayerActivity rejects malformed launch intents": "if (request == null)" in player and "finish()" in player,
    "External PlayerActivity delegates to the shared playback overlay": "TvPlaybackOverlay(" in player,
    "External PlayerActivity persists resume through the shared overlay": "loadResumeMs =" in player and "saveResumeMs =" in player,
    "External PlayerActivity shares subtitle wiring": all(marker in player for marker in [
        "SubtitleWiring.autoFetch", "SubtitleWiring.search", "SubtitleWiring.apply"
    ]),
    "PlayerActivity has no legacy load token": "loadToken" not in player,
    "PlayerActivity has no mutable duplicated playback URL": not re.search(r"private\s+var\s+streamUrl\b", player),
    "Controller separates committed and pending state": "committedState" in controller and "pendingTargetIndex" in controller,
    "Controller rejects stale async commits": "isCurrent(generation)" in controller,
    "Controller restores committed queue index": "queue.index = committedQueueIndex" in controller,
    "Browse playback resolves provider URLs through MainViewModel": "vm.resolvePlayableUrl(channel)" in playback_layer,
    "Browse playback delegates mobile and TV to shared overlays": "MobilePlaybackOverlay(" in playback_layer and "TvPlaybackOverlay(" in playback_layer,
    "Browse playback shares subtitle wiring": playback_layer.count("SubtitleWiring.autoFetch") >= 2 and playback_layer.count("SubtitleWiring.apply") >= 2,
    "Chrome controller owns auto-hide": "autoHideTask" in chrome and "fun armHide" in chrome,
    "Chrome controller owns bounded TV focus retry": "scheduleFocusAttempt" in chrome and "maxFocusRetries" in chrome,
    "Activity has no legacy chrome runnables": "restoreOverlayFocus" not in player and "private val hideOverlay = Runnable" not in player,
    "MainViewModel delegates catalog provider loading": "CatalogLoadCoordinator(" in main_vm and "catalogLoader.section(" in main_vm,
    "MainViewModel has no legacy catalog fetch function": "private suspend fun fetchChannels" not in main_vm,
    "Catalog coordinator owns provider serialization": "private val providerMutex = Mutex()" in catalog_loader and "withProviderLock" in catalog_loader,
    "Catalog coordinator normalizes progressive and final snapshots": "PartialCatalog" in catalog_loader and catalog_loader.count("CatalogNormalizer.normalize") >= 2,
    "Progressive refresh failure restores visible catalog": main_vm.count("restoreAfterRefreshFailure") >= 2,
    "Series requests share the catalog provider boundary": (
        "SeriesLoadCoordinator(catalogLoader)" in main_vm
        and "catalogLoader.withProviderLock" in series_loader
    ),
    "MainViewModel delegates playlist switching": "sourceSwitchCoordinator.switchTo(i)" in select_body and "Repository.invalidate" not in select_body,
    "MainViewModel has no mutable raw generation counters": "private var loadGen" not in main_vm and "private var seriesLoadGen" not in main_vm,
    "Source generation ownership is centralized": "class SourceGenerationGate" in source_switch and "private var loadGeneration" in source_switch and "private var seriesGeneration" in source_switch,
    "Source switch rejects invalid index before side effects": source_switch.find("getOrNull(index) ?: return false") < source_switch.find("persistLastPlaylist(index)"),
    "Source switch invalidates generations before cancellation": source_switch.find("generationGate.invalidateAll()") < source_switch.find("callbacks.cancelActiveWork()"),
    "Source switch publishes before optional auto-load": source_switch.find("callbacks.publish(plan)") < source_switch.find("callbacks.autoLoad()"),
    "Source-bound UI is reset transactionally": all(marker in source_switch for marker in ["loading = false", 'status = ""', "selectedGroup = UiState.ALL_GROUP", "openSeriesTitle = null"]),
    "Catalog and series callbacks use generation gate": main_vm.count("sourceGeneration.isCurrentLoad") >= 10 and main_vm.count("sourceGeneration.isCurrent(request)") >= 2,
    "TextureView has no unsupported background drawable": not re.search(
        r"TextureView\(ctx\)\.also\s*\{\s*it\.setBackgroundColor",
        player_video_surface,
        re.DOTALL,
    ),
    "Player surfaces detach with their AndroidView lifecycle": (
        player_video_surface.count("engine::detachSurface")
        + player_video_surface.count("engine.detachSurface(") >= 2
    ),
    "TV VOD seeks only while the progress bar owns focus": (
        "focusTarget = PlayerFocusTarget.PROGRESS" in player_host
        and "progressFocus.requestFocusWithRetry()" in player_host
        and "engine.seekBy(-SEEK_STEP_MS)" not in player_host
        and "engine.seekBy(SEEK_STEP_MS)" not in player_host
        and "onSeekBy(-SCRUB_STEP_MS)" in player_controls
        and "onSeekBy(SCRUB_STEP_MS)" in player_controls
    ),
    "TV player controls have an explicit D-pad focus graph": (
        "focusProperties { down = playFocus }" in player_controls
        and "right = tracksFocus" in player_controls
        and "right = aspectFocus" in player_controls
        and "right = afterAspect" in player_controls
    ),
    "TV player exposes one combined subtitles and audio entry": (
        'label = "Υπότιτλοι & ήχος"' in player_controls
        and 'label = "Ήχος"' not in player_controls
    ),
    "Downloaded subtitles switch without restarting playback": (
        "fun setExternalSubtitle(cues: List<Cue>, label: String)" in playback_engine
        and "publishExternalCue(p.currentPosition" in playback_engine
        and "p.setMediaItem(buildMediaItem(currentUrl))" not in playback_engine
    ),
    "Crashlytics optional profiling has narrow R8 rules": (
        "-dontwarn android.os.ProfilingTrigger$Builder" in proguard_rules
        and "-dontwarn android.os.ProfilingTrigger" in proguard_rules
        and "-dontwarn android.os.**" not in proguard_rules
    ),
    "TV add-playlist route uses the dedicated screen": (
        "TvAddPlaylistScreen(" in add_playlist
        and "existing == null && isTvDevice()" in add_playlist
    ),
    "TV add-playlist has deterministic initial and directional focus": (
        "requestFocusWithRetry()" in tv_add_playlist
        and "focusProperties" in tv_add_playlist
        and "right = firstFieldFocus" in tv_add_playlist
    ),
    "TV add-playlist restores focus after overlays": (
        "restoreInputFocus(editingInput)" in tv_add_playlist
        and "helpFocus.requestFocusWithRetry()" in tv_add_playlist
        and "TvPlaylistInput.FILE).requestFocusWithRetry()" in tv_add_playlist
    ),
    "Mobile and TV share bounded-memory M3U import": (
        "source.copyTo(destination)" in shared_m3u_importer
        and "ByteArray(4096)" in shared_m3u_importer
        and "#EXTM3U" in shared_m3u_importer
    ),
    "Mobile and TV add-playlist forms expose a real connection test": (
        "testPlaylistConnection(snapshot)" in mobile_add_playlist
        and "testPlaylistConnection(snapshot)" in tv_add_playlist
        and "Δοκιμή σύνδεσης" in mobile_add_playlist
        and "Δοκιμή σύνδεσης" in tv_add_playlist
    ),
    "Connection testing covers every supported source method": all(marker in playlist_connection_tester for marker in [
        "MobilePlaylistMethod.URL -> Repository.testM3u",
        "MobilePlaylistMethod.XTREAM -> XtreamClient.test",
        "MobilePlaylistMethod.MAC -> StalkerClient",
        "MobilePlaylistMethod.FILE -> Repository.testM3u",
    ]),
    "TV connection-test focus is explicit between dismiss and save": (
        "right = testFocus" in tv_add_playlist
        and "left = laterFocus" in tv_add_playlist
        and "right = saveFocus" in tv_add_playlist
        and "left = testFocus" in tv_add_playlist
    ),
    "Empty mobile and TV installs route directly to add playlist": (
        "if (state.playlists.isEmpty())" in main_activity
        and "AddPlaylistScreen(" in main_activity
        and "openFirstPlaylistAfterAdd" in main_activity
    ),
    "Legacy source onboarding is absent from active routing": (
        "SourceOnboarding(" not in playlist_sources
        and "WelcomeLandingScreen(" not in main_activity
        and "Όλο το περιεχόμενό σου, σε μία καθαρή streaming εμπειρία." not in playlist_sources
    ),
}
for label, ok in source_contracts.items():
    if ok:
        print(f"PASS {label}")
    else:
        failures.append(label)

if failures:
    for failure in failures:
        print(f"FAIL {failure}")
    print(f"SUMMARY pass=0 fail={len(failures)}")
    sys.exit(1)

print(f"SUMMARY pass={len(TARGETS) + len(source_contracts)} fail=0")
