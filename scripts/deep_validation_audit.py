#!/usr/bin/env python3
"""Deep release-gate contracts for source routing, TV focus and test coverage."""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java"
TEST = ROOT / "app/src/test/java"
ANDROID_TEST = ROOT / "app/src/androidTest/java"
passes: list[str] = []
failures: list[str] = []
warnings: list[str] = []


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def require(ok: bool, label: str) -> None:
    (passes if ok else failures).append(label)


gradle = read("app/build.gradle.kts")
main_vm = read("app/src/main/java/com/prelude/iptv/ui/MainViewModel.kt")
route_policy = read("app/src/main/java/com/prelude/iptv/tvhome/TvHomePlaybackRoutePolicy.kt")
tv_playback = read("app/src/main/java/com/prelude/iptv/tvhome/TvHomePlaybackActivity.kt")
tv_worker = read("app/src/main/java/com/prelude/iptv/tvhome/TvHomeSyncWorker.kt")
tv_disabled_receiver = read("app/src/main/java/com/prelude/iptv/tvhome/TvHomeBrowsableDisabledReceiver.kt")
tv_ownership_policy = read("app/src/main/java/com/prelude/iptv/tvhome/TvHomeProviderOwnershipPolicy.kt")
interaction = read("app/src/main/java/com/prelude/iptv/ui/TvInteraction.kt")
source_switch = read("app/src/main/java/com/prelude/iptv/ui/coordinator/SourceSwitchCoordinator.kt")

version_code = re.search(r"versionCode\s*=\s*(\d+)", gradle)
version_name = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)
require(version_code is not None and version_name is not None,
        "versionCode and versionName are declared in app/build.gradle.kts")
require('testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"' in gradle,
        "AndroidJUnitRunner is configured")
require('androidTestImplementation("androidx.compose.ui:ui-test-junit4")' in gradle,
        "Compose instrumentation dependency is configured")
require('animationsDisabled = true' in gradle,
        "instrumentation animations are disabled for deterministic focus tests")
require(any(ANDROID_TEST.rglob("*.kt")), "androidTest suite exists")
require((ANDROID_TEST / "com/prelude/iptv/ui/route/TvDialogFocusInstrumentedTest.kt").exists(),
        "TV dialog focus instrumentation suite exists")
require((ANDROID_TEST / "com/prelude/iptv/ui/player/PlayerTracksPanelInstrumentedTest.kt").exists(),
        "mobile player tracks panel instrumentation suite exists")
require((TEST / "com/prelude/iptv/ui/player/PlayerTracksPanelLayoutPolicyTest.kt").exists(),
        "responsive player tracks panel policy tests exist")
require((ROOT / "scripts/verify-device.ps1").exists() and
        (ROOT / "scripts/verify-device.sh").exists(),
        "cross-platform physical-device QA runners exist")
require((ROOT / "docs/DEVICE_QA_MATRIX.md").exists(),
        "mandatory device QA matrix exists")

billing_policy = read("app/src/main/java/com/prelude/iptv/billing/PremiumPolicy.kt")
billing_repository = read("app/src/main/java/com/prelude/iptv/billing/PlayBillingRepository.kt")
require('implementation("com.android.billingclient:billing:9.1.0")' in gradle,
        "current Google Play Billing dependency is pinned")
require('val defaultTier: PremiumTier = PremiumTier.FREE' in billing_policy and
        'PremiumTier.FULL' not in billing_policy,
        "premium defaults to free without a local full-tier bypass")
require('var premiumTier:' not in read("app/src/main/java/com/prelude/iptv/data/PlaylistStore.kt"),
        "premium ownership is not writable through PlaylistStore")
require('Purchase.PurchaseState.PENDING -> PurchaseState.PENDING' in billing_repository and
        'state == PurchaseState.PURCHASED' in read("app/src/main/java/com/prelude/iptv/billing/BillingModels.kt"),
        "pending purchases cannot grant premium")
require('queryPurchasesAsync' in billing_repository and 'acknowledgePurchase' in billing_repository,
        "owned purchases are restored and acknowledged")
require('PurchaseVerifier' in billing_repository and
        (ROOT / "docs/PLAY_BILLING_SETUP.md").exists(),
        "billing has a documented server-verification boundary")
require((TEST / "com/prelude/iptv/billing/BillingEntitlementPolicyTest.kt").exists() and
        (TEST / "com/prelude/iptv/billing/DevicePurchaseVerifierTest.kt").exists(),
        "billing entitlement and verifier policies have focused unit tests")
premium_state = read("app/src/main/java/com/prelude/iptv/billing/PremiumState.kt")
require('create("qa")' in gradle and 'applicationIdSuffix = ".qa"' in gradle and
        'versionNameSuffix = "-qa"' in gradle,
        "owner QA build is a separately installable variant")
require('buildConfigField("boolean", "PREMIUM_QA_OVERRIDE", "false")' in gradle and
        gradle.count('buildConfigField("boolean", "PREMIUM_QA_OVERRIDE", "true")') == 2 and
        'BuildConfig.PREMIUM_QA_OVERRIDE' in premium_state,
        "QA premium override is compile-time and public release is explicitly disabled")
require((ROOT / "app/src/qa/res/values/strings.xml").exists() and
        (ROOT / "docs/OWNER_QA_BUILD.md").exists(),
        "owner QA artifact is visibly labelled and documented")

require('pathSegments.size != 1' in route_policy and 'UUID.fromString' in route_policy,
        "TV Home deep links require one canonical UUID segment")
require('TvHomePlaybackRoutePolicy.parse' in tv_playback,
        "TV Home exported activity delegates to strict route policy")
require('catch (cancelled: CancellationException)' in tv_playback and 'throw cancelled' in tv_playback,
        "TV Home playback preserves cancellation")
require('catch (cancelled: CancellationException)' in tv_worker and 'throw cancelled' in tv_worker,
        "CoroutineWorker preserves cancellation")
require('UUID.fromString' in tv_ownership_policy and 'ownedToken' in tv_ownership_policy,
        "TV Provider mutations require an owned canonical token")
require('?: return' in tv_disabled_receiver and
        tv_disabled_receiver.find('val entry = store.resolve(token) ?: return') < tv_disabled_receiver.find('contentResolver.delete'),
        "TV Provider rows are deleted only after app ownership and store resolution")

require('getOrNull(index) ?: return false' in source_switch and
        source_switch.find('getOrNull(index) ?: return false') < source_switch.find('persistLastPlaylist(index)'),
        "invalid playlist index cannot be persisted or routed")
delete_start = main_vm.find('fun deletePlaylist')
delete_body = main_vm[delete_start:main_vm.find('fun refreshFavorites', delete_start)]
reset_source_start = main_vm.find('private fun resetSourceSession()')
reset_source_body = main_vm[reset_source_start:main_vm.find('/** Invalidates only one source.', reset_source_start)]
require('if (decision.removedActiveSource) {' in delete_body and
        'resetSourceSession()' in delete_body and
        'cancelActiveLoad()' in reset_source_body and
        'if (!decision.removedActiveSource)' in delete_body and
        delete_body.find('if (decision.removedActiveSource) {') < delete_body.find('resetSourceSession()') < delete_body.find('if (!decision.removedActiveSource)'),
        "deleting a background playlist preserves the active load while active deletion cancels first")
require('SourceDeletionPolicy.isLastReference' in main_vm and 'clearPersistedSourceState' in main_vm,
        "duplicate source references protect shared history/favorites/files")
require('val editingActiveSource = index == before.currentIndex' in main_vm and
        'if (editingActiveSource)' in main_vm,
        "editing a background playlist does not restart the active source")
require('forgetRememberedChoices(sourceId)' in main_vm,
        "final source cleanup clears in-memory group selections")
require('sourceSwitchCoordinator.switchTo(i)' in main_vm and 'private var loadGen' not in main_vm,
        "playlist switches and generation ownership are delegated")
require(source_switch.find('getOrNull(index) ?: return false') < source_switch.find('persistLastPlaylist(index)'),
        "invalid source switch cannot persist an out-of-range index")
require(source_switch.find('generationGate.invalidateAll()') < source_switch.find('callbacks.cancelActiveWork()') < source_switch.find('callbacks.publish(plan)'),
        "source switch invalidates stale callbacks before cancellation and publication")
require(all(marker in source_switch for marker in ['loading = false', 'status = ""', 'search = ""', 'openSeriesTitle = null']),
        "source switch clears stale source-bound loading/search/series state")

require('fun TvDialogTextButton' in interaction,
        "dialogs have a reusable visible TV focus action")
focus_files = [
    "app/src/main/java/com/prelude/iptv/MainActivity.kt",
    "app/src/main/java/com/prelude/iptv/ui/AdaptiveSettingsScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/TextEntryDialog.kt",
    "app/src/main/java/com/prelude/iptv/ui/route/BrowseStateComponents.kt",
    "app/src/main/java/com/prelude/iptv/ui/route/BrowseRoute.kt",
    "app/src/main/java/com/prelude/iptv/ui/route/SettingsPlaybackDialogs.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/settings/TvPremiumSettingsScreen.kt",
]
for rel in focus_files:
    require('TvDialogTextButton' in read(rel), f"visible dialog focus action: {Path(rel).name}")

require((TEST / "com/prelude/iptv/ui/SourceDeletionPolicyTest.kt").exists(),
        "source deletion policy tests exist")
require('all valid deletion combinations preserve a bounded index' in read(
    "app/src/test/java/com/prelude/iptv/ui/SourceDeletionPolicyTest.kt"),
    "source deletion exhaustive matrix exists")
require((TEST / "com/prelude/iptv/data/CatalogNormalizerLargeDatasetTest.kt").exists(),
        "large-catalog stress tests exist")
require((TEST / "com/prelude/iptv/tvhome/TvHomePlaybackRoutePolicyTest.kt").exists(),
        "strict TV Home route tests exist")
require((TEST / "com/prelude/iptv/tvhome/TvHomeProviderOwnershipPolicyTest.kt").exists(),
        "TV Provider ownership tests exist")
require((ROOT / "docs/archive/validation/BROAD_POLICY_TESTS_V1_40_43.txt").exists(),
        "broad policy regression suite result exists")


# Production crash-risk and CI contracts.
production_sources = "\n".join(path.read_text(encoding="utf-8") for path in SRC.rglob("*.kt"))
require("!!" not in production_sources, "production Kotlin contains no non-null assertions")
require("activeUa!!" not in production_sources and "fun activeHeaders()" in read(
    "app/src/main/java/com/prelude/iptv/source/StalkerClient.kt"),
    "Stalker session races fail explicitly instead of throwing NPE")
require((ROOT / "app/src/main/java/com/prelude/iptv/net/ProviderCancellation.kt").exists() and
        (TEST / "com/prelude/iptv/net/ProviderCancellationTest.kt").exists(),
        "provider cancellation behavior is centralized and tested")
for rel in [
    "app/src/main/java/com/prelude/iptv/data/TmdbClient.kt",
    "app/src/main/java/com/prelude/iptv/data/SubtitleClient.kt",
    "app/src/main/java/com/prelude/iptv/data/Repository.kt",
    "app/src/main/java/com/prelude/iptv/source/XtreamClient.kt",
]:
    require("ProviderCancellation.rethrow" in read(rel), f"network fallback preserves cancellation: {Path(rel).name}")
settings_route = read("app/src/main/java/com/prelude/iptv/ui/route/SettingsRoute.kt")
require("openInputStream(uri)!!" not in settings_route and "openOutputStream(uri)!!" not in settings_route,
        "SAF import/export handles providers that return null streams")
ci = read(".github/workflows/android-ci.yml")
require("actions/checkout@v6" in ci and "actions/setup-java@v5" in ci and
        "gradle/actions/setup-gradle@v6" in ci,
        "CI uses valid pinned major action versions")
require("python3 scripts/localization_contracts.py" in ci and
        "python3 scripts/deep_validation_audit.py" in ci and
        "python3 scripts/risk_inventory.py" in ci and
        ci.find("python3 scripts/deep_validation_audit.py") < ci.find(":app:testDebugUnitTest"),
        "static release contracts run before Gradle build jobs")
focus_test = read("app/src/androidTest/java/com/prelude/iptv/ui/route/TvDialogFocusInstrumentedTest.kt")
require(focus_test.count("@Test") >= 5, "TV focus instrumentation suite covers at least five routes/transitions")
broad_result = read("docs/archive/validation/BROAD_POLICY_TESTS_V1_40_43.txt")
require("SUMMARY pass=179 fail=0" in broad_result, "inherited broad pure-policy regression suite passes 179 assertions")
chrome_result = read("docs/archive/validation/PLAYER_CHROME_FOCUSED_TESTS_V1_40_44.txt")
require("SUMMARY pass=9 fail=0" in chrome_result,
        "PlayerChromeController focused regression suite passes 9 assertions")
require((TEST / "com/prelude/iptv/player/PlayerChromeControllerTest.kt").exists(),
        "Player chrome behavior has focused unit tests")
source_switch_test = read("app/src/test/java/com/prelude/iptv/ui/coordinator/SourceSwitchCoordinatorTest.kt")
require(source_switch_test.count("@Test") >= 10,
        "SourceSwitchCoordinator has at least ten focused generation/state tests")
require((ROOT / "docs/archive/validation/SOURCE_SWITCH_COORDINATOR_FOCUSED_TESTS_V1_40_46.txt").exists(),
        "SourceSwitchCoordinator focused runtime result exists")

# Security compatibility decision: arbitrary IPTV sources legitimately include HTTP.
manifest = read("app/src/main/AndroidManifest.xml")
if 'android:usesCleartextTraffic="true"' in manifest:
    warnings.append("global cleartext remains enabled for arbitrary user-provided IPTV HTTP endpoints; documented compatibility exception")
else:
    passes.append("global cleartext is disabled")

# Parse every XML resource and manifest.
xml_files = list((ROOT / "app/src/main").rglob("*.xml"))
xml_errors: list[str] = []
for path in xml_files:
    try:
        ET.parse(path)
    except Exception as error:  # pragma: no cover - release script
        xml_errors.append(f"{path.relative_to(ROOT)}: {error}")
require(not xml_errors, f"XML parse ({len(xml_files)} files)" + (f": {'; '.join(xml_errors)}" if xml_errors else ""))

# No credential-bearing values should be logged.
log_risk = re.compile(r"Log\.[vdiew]\([^\n]*(?:password|username|mac|portal|server|streamUrl|epgUrl)", re.I)
log_violations = []
for path in SRC.rglob("*.kt"):
    body = path.read_text(encoding="utf-8")
    if log_risk.search(body):
        log_violations.append(str(path.relative_to(ROOT)))
require(not log_violations, "no obvious credential-bearing values are logged" +
        (f" ({', '.join(log_violations)})" if log_violations else ""))

print("DEEP VALIDATION AUDIT")
for item in passes:
    print(f"PASS  {item}")
for item in warnings:
    print(f"WARN  {item}")
for item in failures:
    print(f"FAIL  {item}")
print(f"SUMMARY pass={len(passes)} warn={len(warnings)} fail={len(failures)}")
sys.exit(1 if failures else 0)
