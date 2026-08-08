#!/usr/bin/env python3
"""Static release gates for Prelude+ staged localization."""
from __future__ import annotations

import sys
import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
QA_RES = ROOT / "app/src/localizationQa/res"
failures: list[str] = []


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def resource_keys(root: Path, folder: str) -> set[str]:
    keys: set[str] = set()
    for path in sorted((root / folder).glob("strings*.xml")):
        tree = ET.parse(path)
        for node in tree.getroot():
            name = node.attrib.get("name")
            if not name or node.attrib.get("translatable") == "false":
                continue
            if name in keys:
                failures.append(f"duplicate {root.name}/{folder} resource key: {name}")
            keys.add(name)
    return keys


greek = resource_keys(RES, "values")
english = resource_keys(QA_RES, "values-en")
missing_english = sorted(greek - english)
extra_english = sorted(english - greek)
if missing_english:
    failures.append("missing QA English keys: " + ", ".join(missing_english))
if extra_english:
    failures.append("QA English-only keys: " + ", ".join(extra_english))
if any((RES / "values-en").glob("strings*.xml")) or any((RES / "values-el").glob("strings*.xml")):
    failures.append("partial public locale folders bypass the release-safe staging gate")

gradle = read("app/build.gradle.kts")
if 'buildConfigField("boolean", "LOCALIZATION_PARITY_COMPLETE", "false")' not in gradle:
    failures.append("public localization parity gate is not explicitly closed")
if "generateLocaleConfig" in gradle:
    failures.append("generated locale config must remain disabled until final parity")
if 'resourceConfigurations += listOf("en", "el")' not in gradle:
    failures.append("packaged locale allow-list is not restricted to English/Greek")
if gradle.count('res.srcDir("src/localizationQa/res")') != 2:
    failures.append("shared English resources are not limited to debug and QA source sets")

if read("app/src/main/res/resources.properties").strip() != "unqualifiedResLocale=el":
    failures.append("Greek is not preserved as the public migration baseline")

activity_paths = [
    "app/src/main/java/com/prelude/iptv/MainActivity.kt",
    "app/src/main/java/com/prelude/iptv/PlayerActivity.kt",
    "app/src/main/java/com/prelude/iptv/MultiviewActivity.kt",
    "app/src/main/java/com/prelude/iptv/StartupActivity.kt",
    "app/src/main/java/com/prelude/iptv/tvhome/TvHomePlaybackActivity.kt",
]
for activity_path in activity_paths:
    if ": AppCompatActivity()" not in read(activity_path):
        failures.append(f"locale-aware activity host missing: {activity_path}")

navigation = read(
    "app/src/main/java/com/prelude/iptv/ui/navigation/PrimaryContentDestination.kt"
)
settings = read(
    "app/src/main/java/com/prelude/iptv/ui/components/settings/SettingsFoundation.kt"
)
if "val label:" in navigation or "val label:" in settings:
    failures.append("Android-free navigation/settings models still own display copy")

home_layout_path = "app/src/main/java/com/prelude/iptv/ui/home/HomeLayoutPolicy.kt"
home_layout = read(home_layout_path)
home_mapping = read(
    "app/src/main/java/com/prelude/iptv/ui/localization/HomeLocalizationResources.kt"
)
catalog_policy = read("app/src/main/java/com/prelude/iptv/ui/CatalogPolicy.kt")
premium_policy = read("app/src/main/java/com/prelude/iptv/billing/PremiumPolicy.kt")
if re.search(r"data class HomeSection\s*\([^)]*\btitle\s*:", home_layout, re.DOTALL):
    failures.append("Android-free HomeSection model still owns display copy")
if "labels: CatalogRailLabels" not in catalog_policy:
    failures.append("catalog policy no longer receives localized app-owned labels")
if "fun label(" in premium_policy:
    failures.append("Android-free premium policy still owns display copy")

home_section_constants = (
    "HEADER", "HERO", "SUGGESTIONS", "CONTINUE", "RECENT_LIVE", "NEW_LIVE",
    "NEW_MOVIES", "NEW_EPISODES", "LIVE", "MOVIES", "SERIES",
)
for constant in home_section_constants:
    if f"HomeLayoutPolicy.{constant} -> R.string.home_section_" not in home_mapping:
        failures.append(f"Home section resource mapping missing: {constant}")


def uncomment_kotlin(source: str) -> str:
    source = re.sub(r"/\*.*?\*/", "", source, flags=re.DOTALL)
    return re.sub(r"//.*", "", source)


def greek_string_literals(source: str) -> list[str]:
    literals = re.findall(r'"(?:\\.|[^"\\])*"', uncomment_kotlin(source))
    return [literal for literal in literals if re.search(r"[\u0370-\u03ff\u1f00-\u1fff]", literal)]


migrated_home_ui = [
    "app/src/main/java/com/prelude/iptv/ui/AdaptiveCatalogHome.kt",
    "app/src/main/java/com/prelude/iptv/ui/components/home/PremiumHomeShared.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobileCategoryExplorer.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobileEditHomeScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobileHomeHeader.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobileHomeNavigation.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobileHomeRail.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobileHomeRailResolver.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobileHomeSupportingSections.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobileHomeTiles.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobilePremiumHomeHero.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobilePremiumHomeRail.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobilePremiumHomeScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobilePremiumSectionScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/home/MobileScrollChrome.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/home/TvHomeRailPolicy.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/home/TvPremiumHomeHero.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/home/TvPremiumHomeRail.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/home/TvPremiumHomeScreen.kt",
]
for ui_path in migrated_home_ui:
    literals = greek_string_literals(read(ui_path))
    if literals:
        failures.append(f"hardcoded Greek display copy in migrated Home UI: {ui_path}")

live_foundation = read(
    "app/src/main/java/com/prelude/iptv/ui/components/live/LiveFoundation.kt"
)
tv_live_policy = read(
    "app/src/main/java/com/prelude/iptv/ui/tv/browse/TvLiveBrowsePolicy.kt"
)
browse_route = read("app/src/main/java/com/prelude/iptv/ui/route/BrowseRoute.kt")
mobile_live_channels = read(
    "app/src/main/java/com/prelude/iptv/ui/mobile/live/MobileLiveChannelsScreen.kt"
)
playback_launchers = read(
    "app/src/main/java/com/prelude/iptv/ui/route/PlaybackLaunchers.kt"
)
if "val providerLabel: String?" not in live_foundation:
    failures.append("Live filter model no longer separates provider data from app copy")
if "fun liveRemaining(programme: EpgManager.Prog?, nowMs: Long): LiveRemaining?" not in live_foundation:
    failures.append("Live remaining time is preformatted outside the resource boundary")
live_foundation_greek = set(greek_string_literals(live_foundation))
if live_foundation_greek != {'"αθλη"', '"ποδόσφ"'}:
    failures.append("shared Live foundation contains unexpected Greek display copy")
if greek_string_literals(tv_live_policy):
    failures.append("Android-free TV Live policy still owns Greek display copy")
if 'state.status.startsWith("Σφάλμα")' in browse_route:
    failures.append("Browse UI bypasses the typed legacy catalog-status boundary")
if "OTHER_LIVE_GROUP_ID" not in mobile_live_channels:
    failures.append("localized Live fallback category is being used as state identity")
if "MultiviewLaunchFailure" not in playback_launchers or greek_string_literals(playback_launchers):
    failures.append("Multiview launch failures bypass the typed localization boundary")

migrated_live_ui = [
    "app/src/main/java/com/prelude/iptv/ui/PremiumLiveTvScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/live/MobileLiveCategorySections.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/live/MobileLiveChannelContent.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/live/MobileLiveChannelsScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/live/MobileLiveControls.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/live/MobileLiveHero.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/live/MobileLiveSections.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/live/MobilePremiumLiveScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/browse/TvLiveBrowseScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/browse/TvLiveBrowsePolicy.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/live/TvLiveHero.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/live/TvLiveRail.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/live/TvPremiumLiveScreen.kt",
]
for ui_path in migrated_live_ui:
    literals = greek_string_literals(read(ui_path))
    if literals:
        failures.append(f"hardcoded Greek display copy in migrated Live UI: {ui_path}")

if failures:
    for failure in failures:
        print(f"FAIL: {failure}")
    sys.exit(1)

print(
    "PASS: staged English/Greek resource parity, release baseline, rollout gate, "
    "activity hosts and UI/model boundaries are intact"
)
