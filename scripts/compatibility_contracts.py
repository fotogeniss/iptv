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
tv_playback_overlay = (ROOT / "app/src/main/java/com/prelude/iptv/ui/player/TvPlaybackOverlay.kt").read_text(encoding="utf-8")
tv_live_transition = (ROOT / "app/src/main/java/com/prelude/iptv/ui/player/TvLiveChannelTransition.kt").read_text(encoding="utf-8")
subtitle_search_content = (ROOT / "app/src/main/java/com/prelude/iptv/ui/player/PlayerSubtitleSearchContent.kt").read_text(encoding="utf-8")
subtitle_wiring = (ROOT / "app/src/main/java/com/prelude/iptv/ui/player/SubtitleWiring.kt").read_text(encoding="utf-8")
playback_engine = (ROOT / "app/src/main/java/com/prelude/iptv/player/PlaybackEngine.kt").read_text(encoding="utf-8")
proguard_rules = (ROOT / "app/proguard-rules.pro").read_text(encoding="utf-8")
android_manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
diagnostics_manager = (ROOT / "app/src/main/java/com/prelude/iptv/diagnostics/DiagnosticsManager.kt").read_text(encoding="utf-8")
diagnostics_store = (ROOT / "app/src/main/java/com/prelude/iptv/diagnostics/LocalDiagnosticStore.kt").read_text(encoding="utf-8")
diagnostics_set_enabled = diagnostics_manager[
    diagnostics_manager.find("fun setCollectionEnabled"):diagnostics_manager.find("fun refreshPendingState")
]
add_playlist = (ROOT / "app/src/main/java/com/prelude/iptv/AddPlaylistScreen.kt").read_text(encoding="utf-8")
tv_add_playlist = (ROOT / "app/src/main/java/com/prelude/iptv/ui/tv/sources/TvAddPlaylistScreen.kt").read_text(encoding="utf-8")
tv_add_playlist_components = (ROOT / "app/src/main/java/com/prelude/iptv/ui/tv/sources/TvAddPlaylistComponents.kt").read_text(encoding="utf-8")
mobile_add_playlist = (ROOT / "app/src/main/java/com/prelude/iptv/ui/mobile/sources/MobileAddPlaylistScreen.kt").read_text(encoding="utf-8")
tv_onboarding_steps = (ROOT / "app/src/main/java/com/prelude/iptv/ui/tv/sources/TvSourceOnboardingSteps.kt").read_text(encoding="utf-8")
tv_source_details = (ROOT / "app/src/main/java/com/prelude/iptv/ui/tv/sources/TvSourceDetailsStep.kt").read_text(encoding="utf-8")
mobile_source_details = (ROOT / "app/src/main/java/com/prelude/iptv/ui/mobile/sources/MobileSourceDetailsStep.kt").read_text(encoding="utf-8")
playlist_source_submission = (ROOT / "app/src/main/java/com/prelude/iptv/ui/sources/PlaylistSourceSubmission.kt").read_text(encoding="utf-8")
playlist_source_draft = (ROOT / "app/src/main/java/com/prelude/iptv/ui/sources/PlaylistSourceDraft.kt").read_text(encoding="utf-8")
playlist_connection_tester = (ROOT / "app/src/main/java/com/prelude/iptv/ui/sources/PlaylistConnectionTester.kt").read_text(encoding="utf-8")
shared_m3u_importer = (ROOT / "app/src/main/java/com/prelude/iptv/ui/sources/M3uFileImporter.kt").read_text(encoding="utf-8")
playlist_store = (ROOT / "app/src/main/java/com/prelude/iptv/data/PlaylistStore.kt").read_text(encoding="utf-8")
main_activity = (ROOT / "app/src/main/java/com/prelude/iptv/MainActivity.kt").read_text(encoding="utf-8")
profile_settings = (ROOT / "app/src/main/java/com/prelude/iptv/ui/coordinator/ProfileSettingsCoordinator.kt").read_text(encoding="utf-8")
backup = (ROOT / "app/src/main/java/com/prelude/iptv/data/Backup.kt").read_text(encoding="utf-8")
backup_crypto = (ROOT / "app/src/main/java/com/prelude/iptv/data/PortableBackupCrypto.kt").read_text(encoding="utf-8")
settings_account_dialogs = (ROOT / "app/src/main/java/com/prelude/iptv/ui/route/SettingsAccountDialogs.kt").read_text(encoding="utf-8")
delete_profile_body = profile_settings[
    profile_settings.find("fun deleteProfile"):profile_settings.find("fun setActiveProfile")
]
delete_profile_order = [
    delete_profile_body.find("store.saveProfiles(remaining)"),
    delete_profile_body.find("store.wipeProfile(id)"),
    delete_profile_body.find("if (store.activeProfile == id) store.activeProfile = 0"),
    delete_profile_body.find("TvHomeSyncScheduler.schedule(app)"),
]
playlist_sources = (ROOT / "app/src/main/java/com/prelude/iptv/ui/route/PlaylistSourcesScreen.kt").read_text(encoding="utf-8")
main_vm = TARGETS["MainViewModel"].read_text(encoding="utf-8")
select_start = main_vm.find("    fun selectPlaylist(i: Int)")
select_end = main_vm.find("    fun saveFontScale", select_start)
select_body = main_vm[select_start:select_end]

source_contracts = {
    "Profile switch keeps persistence before TV Home synchronization": (
        profile_settings.find("store.activeProfile = id")
        < profile_settings.find("TvHomeSyncScheduler.schedule(app)", profile_settings.find("fun setActiveProfile"))
    ),
    "Profile deletion keeps scoped wipe and active fallback before TV Home synchronization": (
        all(index >= 0 for index in delete_profile_order)
        and delete_profile_order == sorted(delete_profile_order)
    ),
    "Parental unlock TTL remains thirty minutes": "private val unlockTtlMs = 30 * 60 * 1000L" in profile_settings,
    "Portable backup envelope and crypto parameters remain compatible": all(
        marker in backup + backup_crypto
        for marker in (
            'private const val VERSION = 2',
            'private const val MAGIC = "UltimateIPTV-Backup"',
            'private const val ITERATIONS = 210_000',
            'private const val KEY_BITS = 256',
            'private const val SALT_BYTES = 16',
            'private const val IV_BYTES = 12',
            'private const val TAG_BITS = 128',
        )
    ),
    "Profile and backup dialogs retain explicit TV focus boundaries": (
        settings_account_dialogs.count("rememberInitialFocus()") >= 4
        and "Modifier.focusRequester(fP)" in settings_account_dialogs
        and settings_account_dialogs.count(".tvFocus(") >= 10
    ),
    "External PlayerActivity keeps its manifest launch class": "class PlayerActivity : AppCompatActivity()" in player,
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
    "TV live zapping keeps direction and transition outside focus handling": (
        "directionalChannelStep" in tv_playback_overlay
        and "TvLiveChannelTransition(" in tv_playback_overlay
        and "videoOverlay = if (isLive)" in tv_playback_overlay
        and all(marker not in tv_live_transition for marker in (
            ".focusable(", ".onKeyEvent", ".onPreviewKeyEvent", ".clickable("
        ))
    ),
    "TV live zapping keeps one surface and waits for first frame": (
        "frameCapture = frameCapture" in player_host
        and "frameCapture?.attach(this)" in player_video_surface
        and "PixelCopy.request" in (
            ROOT / "app/src/main/java/com/prelude/iptv/ui/player/PlayerVideoFrameCapture.kt"
        ).read_text(encoding="utf-8")
        and "DisposableEffect(engine)" in tv_playback_overlay
    ),
    "TV scrubber stays slim without shrinking its DPAD target": (
        ".height(48.dp)" in player_controls
        and "if (scrubFocused) 3.dp else 2.dp" in player_controls
        and "onSeekBy(-SCRUB_STEP_MS)" in player_controls
        and "onSeekBy(SCRUB_STEP_MS)" in player_controls
    ),
    "TV player exposes one combined subtitles and audio entry": (
        "label = stringResource(R.string.player_subtitles_audio)" in player_controls
        and "label = stringResource(R.string.player_audio)" not in player_controls
    ),
    "Downloaded subtitles switch without restarting playback": (
        "fun setExternalSubtitle(cues: List<Cue>, label: String)" in playback_engine
        and "publishExternalCue(p.currentPosition" in playback_engine
        and "p.setMediaItem(buildMediaItem(currentUrl))" not in playback_engine
    ),
    "Editable manual subtitle queries search automatically and cancel stale work": (
        "normalizedQuery," in subtitle_search_content
        and "SubtitleAutoSearchPolicy.DEBOUNCE_MS" in subtitle_search_content
        and "currentLoad(normalizedQuery)" in subtitle_search_content
        and "queryFocused" in subtitle_search_content
        and "currentCoroutineContext().ensureActive()" in subtitle_wiring
        and "ProviderCancellation.rethrow" in subtitle_wiring
    ),
    "Crashlytics optional profiling has narrow R8 rules": (
        "-dontwarn android.os.ProfilingTrigger$Builder" in proguard_rules
        and "-dontwarn android.os.ProfilingTrigger" in proguard_rules
        and "-dontwarn android.os.**" not in proguard_rules
    ),
    "Diagnostics Firebase startup remains explicit and consent gated": (
        'android:name="com.google.firebase.provider.FirebaseInitProvider"' in android_manifest
        and 'tools:node="remove"' in android_manifest
        and 'android:name="firebase_crashlytics_collection_enabled"' in android_manifest
        and 'android:value="false"' in android_manifest
        and "if (enabled)" in diagnostics_manager
        and "firebaseReporter.initializeIfConfigured()" in diagnostics_manager
    ),
    "Disabling diagnostics still removes unsent Firebase reports": (
        "if (!enabled) firebase?.deleteUnsentReports()" in diagnostics_set_enabled
        and "firebaseHasUnsentReport = if (enabled)" in diagnostics_set_enabled
    ),
    "Local diagnostics keeps one storage-compatible pending report": all(
        marker in diagnostics_store
        for marker in (
            'const val PREFERENCES = "diagnostics_privacy"',
            'const val KEY_COLLECTION_ENABLED = "crash_reporting_enabled"',
            'const val KEY_CAPTURED_AT = "pending_captured_at"',
            'const val KEY_EXCEPTION_TYPE = "pending_exception_type"',
            'const val KEY_SUMMARY = "pending_summary"',
            'const val KEY_STACK = "pending_stack"',
        )
    ),
    "TV add-playlist route uses the dedicated screen": (
        "TvAddPlaylistScreen(" in add_playlist
        and "existing == null && isTvDevice()" in add_playlist
    ),
    "TV add-playlist has deterministic initial and directional focus": (
        "requestFocusWithRetry()" in tv_add_playlist
        and "focusProperties" in tv_onboarding_steps
        and "right = methodFocus.getValue" in tv_onboarding_steps
        and "focusProperties" in tv_source_details
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
    "Mobile and TV add-playlist forms atomically test before building": (
        "submitPlaylistSource(" in mobile_add_playlist
        and "defaultLocalName else defaultPlaylistName" in mobile_add_playlist
        and "submitPlaylistSource(" in tv_add_playlist
        and "defaultLocalName else defaultPlaylistName" in tv_add_playlist
        and "tester(snapshot)" in playlist_source_submission
        and "PlaylistSourceDraftPolicy.build(snapshot, fallbackName)" in playlist_source_submission
        and "R.string.sources_check_and_add" in mobile_source_details
        and "R.string.sources_check_and_add" in tv_source_details
    ),
    "Connection testing covers every supported source method": all(marker in playlist_connection_tester for marker in [
        "PlaylistSourceMethod.URL -> Repository.testM3u",
        "PlaylistSourceMethod.XTREAM -> XtreamClient.test",
        "PlaylistSourceMethod.MAC -> StalkerClient",
        "PlaylistSourceMethod.FILE -> Repository.testM3u",
    ]),
    "TV verified-submit focus is explicit between exit and submit": (
        "right = submitFocus" in tv_source_details
        and "left = exitFocus" in tv_source_details
        and "up = advancedFocus" in tv_source_details
    ),
    "TV source details return left from each first input to method change": (
        tv_source_details.count("left = changeFocus") >= 4
        and "right = firstInput" in tv_source_details
    ),
    "Source onboarding never imports Compose's internal weight symbol": all(
        "import androidx.compose.foundation.layout.weight" not in source
        for source in (
            mobile_source_details,
            tv_add_playlist,
            tv_source_details,
            tv_onboarding_steps,
        )
    ),
    "TV onboarding helper models have distinct package-level names": (
        "data class TvMethodCardContent" in tv_add_playlist_components
        and "data class TvMethodFormContent" in tv_source_details
        and "data class TvMethodContent" not in tv_add_playlist_components
        and "data class TvMethodContent" not in tv_source_details
    ),
    "Successful onboarding skips the redundant content chooser for Live TV": main_activity.count('vm.setContentType("live")') >= 2,
    "Source onboarding detects credentials and owns field-level validation centrally": (
        "fun detect(raw: String)" in playlist_source_draft
        and "PlaylistSourceField.PASSWORD" in playlist_source_draft
        and "PlaylistSourceField.MAC_ADDRESS" in playlist_source_draft
        and "PlaylistSourceDraftPolicy.detect(smartInput)" in mobile_add_playlist
    ),
    "Source credentials stay transient until encrypted playlist persistence": (
        "rememberSaveable" not in mobile_add_playlist
        and "rememberSaveable" not in tv_add_playlist
        and 'secure.putString("playlists", arr.toString())' in playlist_store
    ),
    "Active add-source screens contain no dead account-login promise": (
        "Σύνδεση λογαριασμού" not in mobile_add_playlist
        and "Σύνδεση λογαριασμού" not in tv_add_playlist
        and "Ίσως αργότερα" not in mobile_source_details
        and "Ίσως αργότερα" not in tv_source_details
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
