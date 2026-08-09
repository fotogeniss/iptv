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

catalog_mapping = read(
    "app/src/main/java/com/prelude/iptv/ui/localization/CatalogLocalizationResources.kt"
)
for synthetic_group in ("UiState.ALL_GROUP", "UiState.FAV_GROUP"):
    if synthetic_group not in catalog_mapping:
        failures.append(f"catalog synthetic-group resource mapping missing: {synthetic_group}")

migrated_catalog_ui = [
    "app/src/main/java/com/prelude/iptv/ui/route/BrowseFilterComponents.kt",
    "app/src/main/java/com/prelude/iptv/ui/route/BrowseStateComponents.kt",
    "app/src/main/java/com/prelude/iptv/ui/route/BrowseTopBar.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/browse/TvCategoryBrowseScreen.kt",
]
for ui_path in migrated_catalog_ui:
    literals = greek_string_literals(read(ui_path))
    if literals:
        failures.append(f"hardcoded Greek display copy in migrated catalog UI: {ui_path}")

tv_catalog = read(
    "app/src/main/java/com/prelude/iptv/ui/tv/browse/TvCategoryBrowseScreen.kt"
)
if "fun allGroupLabel" in tv_catalog:
    failures.append("TV catalog still owns localized synthetic-group display copy")
if 'state.contentType == "vod"' not in browse_route or 'state.contentType == "series"' not in browse_route:
    failures.append("Movies/Series still expose legacy catalog status transport copy")
loading_progress = browse_route[browse_route.index("private fun CatalogLoadingProgress"):]
if "progress.stage" in loading_progress or greek_string_literals(loading_progress):
    failures.append("catalog loading UI bypasses the localized progress boundary")
for legacy_copy in ("Τι θέλεις να δεις;", "Η φωνητική αναζήτηση δεν είναι διαθέσιμη"):
    if f'"{legacy_copy}"' in uncomment_kotlin(browse_route):
        failures.append("voice-search catalog copy bypasses resources")

search_policy = read("app/src/main/java/com/prelude/iptv/ui/SearchUiPolicy.kt")
search_mapping = read(
    "app/src/main/java/com/prelude/iptv/ui/localization/SearchLocalizationResources.kt"
)
if "enum class PremiumSearchFilter(val label:" in search_policy:
    failures.append("Android-free Search filter model still owns display copy")
for typed_boundary in ("sealed interface SearchHeading", "sealed interface SearchCategory"):
    if typed_boundary not in search_policy:
        failures.append(f"typed Search resource boundary missing: {typed_boundary}")
for filter_name in ("ALL", "MOVIES", "SERIES", "LIVE", "SPORTS", "DOCUMENTARIES"):
    if f"PremiumSearchFilter.{filter_name} -> R.string.search_filter_" not in search_mapping:
        failures.append(f"Search filter resource mapping missing: {filter_name}")
if "fun description(channel: Channel, meta: TmdbClient.Meta?): String?" not in search_policy:
    failures.append("Search fallback description is still formatted outside resources")

migrated_search_ui = [
    "app/src/main/java/com/prelude/iptv/ui/components/search/SearchFoundation.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/search/MobilePremiumSearchScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/search/MobileSearchComponents.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/search/TvPremiumSearchScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/search/TvSearchControls.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/search/TvSearchResults.kt",
]
for ui_path in migrated_search_ui:
    literals = greek_string_literals(read(ui_path))
    if literals:
        failures.append(f"hardcoded Greek display copy in migrated Search UI: {ui_path}")

search_keyboard = read("app/src/main/java/com/prelude/iptv/ui/SearchKeyboardPolicy.kt")
if 'initialMode(language: String)' not in search_keyboard:
    failures.append("TV Search keyboard no longer follows the active app language")

detail_presentation = read(
    "app/src/main/java/com/prelude/iptv/ui/components/details/DetailPresentation.kt"
)
detail_mapping = read(
    "app/src/main/java/com/prelude/iptv/ui/localization/DetailsLocalizationResources.kt"
)
watch_progress = read("app/src/main/java/com/prelude/iptv/ui/WatchProgressPolicy.kt")
if "val showTmdbNotice: Boolean" not in detail_presentation or "val notice: String" in detail_presentation:
    failures.append("Detail presentation still transports localized TMDB notice copy")
for section in ("Episodes", "About", "Cast", "Similar"):
    if f"DetailSection.{section} -> R.string.details_" not in detail_mapping:
        failures.append(f"Detail section resource mapping missing: {section}")
if "data class WatchRemaining" not in watch_progress or "remainingLabel" in watch_progress:
    failures.append("watch progress still formats localized remaining-time copy in pure policy")
if greek_string_literals(watch_progress):
    failures.append("pure watch-progress policy owns Greek display copy")

migrated_detail_ui = [
    "app/src/main/java/com/prelude/iptv/ui/DetailScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/components/details/DetailFoundation.kt",
    "app/src/main/java/com/prelude/iptv/ui/components/details/DetailPresentation.kt",
    "app/src/main/java/com/prelude/iptv/ui/components/details/DetailCastAndRelated.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/details/MobilePremiumDetailScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/details/MobileDetailHero.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/details/MobileDetailCards.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/details/MobileSeasonHeader.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/details/TvPremiumDetailScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/details/TvDetailHero.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/details/TvDetailCards.kt",
]
for ui_path in migrated_detail_ui:
    literals = greek_string_literals(read(ui_path))
    if literals:
        failures.append(f"hardcoded Greek display copy in migrated Details UI: {ui_path}")

detail_route = uncomment_kotlin(
    read("app/src/main/java/com/prelude/iptv/ui/route/DetailRouteHost.kt")
)
for legacy_copy in (
    "Για βαθμολογίες, ελληνική υπόθεση και φωτογραφίες ηθοποιών",
    'Intent.createChooser(share, "Κοινοποίηση")',
):
    if legacy_copy in detail_route:
        failures.append("Detail route bypasses typed/resource-owned app copy")

player_mapping = read(
    "app/src/main/java/com/prelude/iptv/ui/localization/PlayerLocalizationResources.kt"
)
track_policy = read("app/src/main/java/com/prelude/iptv/player/TrackLabelPolicy.kt")
subtitle_wiring = read(
    "app/src/main/java/com/prelude/iptv/ui/player/SubtitleWiring.kt"
)
for mode in ("FIT", "FILL", "FORCE_4_3", "FORCE_16_9"):
    if f"AspectMode.{mode} -> R.string.player_aspect_" not in player_mapping:
        failures.append(f"Player aspect resource mapping missing: {mode}")
if "displayLocale: Locale" not in track_policy or greek_string_literals(track_policy):
    failures.append("playback track policy still owns Greek display copy")
if "fallbackLabel: String" not in track_policy:
    failures.append("playback track fallback bypasses the Android resource boundary")
if greek_string_literals(subtitle_wiring):
    failures.append("subtitle network wiring still returns Greek display messages")

migrated_player_ui = [
    "app/src/main/java/com/prelude/iptv/PlayerActivity.kt",
    "app/src/main/java/com/prelude/iptv/ui/route/BrowsePlaybackLayer.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/MobileMiniPlayer.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/MobileNextEpisodeOffer.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/MobilePlayerContextContent.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/MobilePlayerControls.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/PlayerControlMenus.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/PlayerControlPrimitives.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/PlayerControls.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/PlayerEpgDialog.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/PlayerHost.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/PlayerMenuHost.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/PlayerNextEpisodeCard.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/PlayerSubtitleSearchContent.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/PlayerTracksPanel.kt",
]
for ui_path in migrated_player_ui:
    literals = greek_string_literals(read(ui_path))
    if literals:
        failures.append(f"hardcoded Greek display copy in migrated Player UI: {ui_path}")

for overlay_path in (
    "app/src/main/java/com/prelude/iptv/ui/player/MobilePlaybackOverlay.kt",
    "app/src/main/java/com/prelude/iptv/ui/player/TvPlaybackOverlay.kt",
):
    literals = greek_string_literals(read(overlay_path))
    unexpected = [
        literal for literal in literals
        if not literal.startswith('"Η επίλυση διεύθυνσης απέτυχε')
        and not literal.startswith('"Κενή διεύθυνση για')
    ]
    if unexpected:
        failures.append(f"hardcoded Greek display copy in migrated Player overlay: {overlay_path}")

source_draft = read("app/src/main/java/com/prelude/iptv/ui/sources/PlaylistSourceDraft.kt")
source_connection = read(
    "app/src/main/java/com/prelude/iptv/ui/sources/PlaylistConnectionMessagePolicy.kt"
)
source_submission = read(
    "app/src/main/java/com/prelude/iptv/ui/sources/PlaylistSourceSubmission.kt"
)
source_importer = read("app/src/main/java/com/prelude/iptv/ui/sources/M3uFileImporter.kt")
source_mapping = read(
    "app/src/main/java/com/prelude/iptv/ui/localization/SourceLocalizationResources.kt"
)
if "val reason: PlaylistSourceValidationReason" not in source_draft:
    failures.append("source validation still transports localized display copy")
if "val kind: PlaylistSourceDetectionKind" not in source_draft:
    failures.append("source detection still transports localized display copy")
if "enum class PlaylistConnectionFailure" not in source_connection:
    failures.append("source connection failures bypass the typed localization boundary")
if "sealed interface PlaylistSourceSubmissionFailure" not in source_submission:
    failures.append("source submission failures bypass the typed localization boundary")
if "enum class M3uImportFailure" not in source_importer:
    failures.append("M3U import failures bypass the typed localization boundary")
for mapping in (
    "PlaylistSourceValidationReason.messageRes",
    "PlaylistSourceDetectionKind.labelRes",
    "PlaylistConnectionFailure.messageRes",
    "PlaylistSourceSubmissionFailure.messageRes",
    "M3uImportFailure.messageRes",
    "SettingsSourceStatus.labelRes",
):
    if mapping not in source_mapping:
        failures.append(f"source resource mapping missing: {mapping}")

# These Greek aliases classify provider/paste input; they are not display copy.
source_policy_greek = {
    literal for literal in re.findall(r'"(?:\\.|[^"\\])*"', source_draft)
    if re.search(r"[\u0370-\u03ff\u1f00-\u1fff]", literal)
}
if len(source_policy_greek) != 2 or any(
    "όνομα\\\\s+χρήστη" not in literal and "κωδικός" not in literal
    for literal in source_policy_greek
):
    failures.append("source draft policy contains unexpected Greek display copy")
connection_classifier_greek = set(greek_string_literals(source_connection))
if connection_classifier_greek != {'"δεν μοιάζει με m3u"', '"λάθος στοιχεία"'}:
    failures.append("source connection classifier contains unexpected Greek display copy")
for pure_source in (source_submission, source_importer):
    if greek_string_literals(pure_source):
        failures.append("source policy/import boundary owns Greek display copy")

migrated_source_ui = [
    "app/src/main/java/com/prelude/iptv/AddPlaylistScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/settings/MobileEditPlaylistScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/settings/MobilePlaylistManagerScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/settings/MobileSettingsComponents.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/sources/MobileAddPlaylistComponents.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/sources/MobileAddPlaylistScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/sources/MobileSourceDetailsStep.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/sources/MobileSourceOnboardingSteps.kt",
    "app/src/main/java/com/prelude/iptv/ui/route/PlaylistSourcesScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/settings/TvSettingsComponents.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/sources/TvAddPlaylistComponents.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/sources/TvAddPlaylistScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/sources/TvSourceDetailsStep.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/sources/TvSourceOnboardingSteps.kt",
]
for ui_path in migrated_source_ui:
    literals = greek_string_literals(read(ui_path))
    if literals:
        failures.append(f"hardcoded Greek display copy in migrated Source UI: {ui_path}")

epg_presentation = read(
    "app/src/main/java/com/prelude/iptv/ui/epg/EpgPresentationState.kt"
)
epg_mapping = read(
    "app/src/main/java/com/prelude/iptv/ui/localization/EpgLocalizationResources.kt"
)
epg_coordinator = read(
    "app/src/main/java/com/prelude/iptv/ui/coordinator/MainEpgCoordinator.kt"
)
epg_directory = read("app/src/main/java/com/prelude/iptv/data/EpgSourceDirectory.kt")
epg_manager = read("app/src/main/java/com/prelude/iptv/data/EpgManager.kt")
epg_foundation = read(
    "app/src/main/java/com/prelude/iptv/ui/components/epg/EpgFoundation.kt"
)
main_ui_state = read("app/src/main/java/com/prelude/iptv/ui/MainUiState.kt")
for typed_boundary in (
    "sealed interface EpgStatus",
    "enum class EpgFilter",
    "enum class EpgSourceKind",
    "data class EpgSourceOption",
):
    if typed_boundary not in epg_presentation:
        failures.append(f"typed EPG presentation boundary missing: {typed_boundary}")
if "val epgSources: List<EpgSourceOption>" not in main_ui_state:
    failures.append("EPG sources still transport preformatted display labels")
if "val epgStatus: EpgStatus" not in main_ui_state:
    failures.append("EPG status still transports localized display copy")
if greek_string_literals(epg_coordinator):
    failures.append("EPG coordinator still owns Greek display copy")
if "EpgStatus." not in epg_coordinator or "EpgSourceOption(" not in epg_coordinator:
    failures.append("EPG coordinator bypasses typed presentation state")
if "val label:" in epg_directory or "Locale.ROOT" not in epg_directory:
    failures.append("EPG source directory mixes display copy or locale-sensitive protocol casing")
if "enum class EpgLoadFailure" not in epg_manager or "EpgLoadException" not in epg_manager:
    failures.append("EPG load failures bypass the typed data boundary")
if greek_string_literals(epg_manager):
    failures.append("EPG manager still throws or owns Greek display copy")
if "DateFormat.getTimeFormat(LocalContext.current)" not in epg_foundation:
    failures.append("EPG time formatting does not follow the active Android locale")
if "localizedEpgRuntime(minutes)" not in epg_foundation:
    failures.append("EPG runtime is still formatted outside the resource boundary")
for mapping in (
    "EpgFilter.labelRes",
    "EpgStatus.localizedText",
    "EpgSourceOption.localizedLabel",
    "localizedEpgRuntime",
):
    if mapping not in epg_mapping:
        failures.append(f"EPG resource mapping missing: {mapping}")

migrated_epg_ui = [
    "app/src/main/java/com/prelude/iptv/ui/mobile/epg/MobileEpgGuide.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/epg/MobileEpgHero.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/epg/MobileEpgProgramCard.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/epg/MobileEpgScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/mobile/settings/MobileEpgSettingsScreen.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/epg/TvEpgDock.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/epg/TvEpgHero.kt",
    "app/src/main/java/com/prelude/iptv/ui/tv/epg/TvEpgProgramCell.kt",
]
for ui_path in migrated_epg_ui:
    literals = greek_string_literals(read(ui_path))
    if ui_path.endswith("MobileEpgGuide.kt"):
        # Provider programme text classifiers, not app-owned display copy.
        literals = [literal for literal in literals if literal not in {'"ταιν"', '"αθλη"'}]
    if literals:
        failures.append(f"hardcoded Greek display copy in migrated EPG UI: {ui_path}")

adaptive_settings = read("app/src/main/java/com/prelude/iptv/ui/AdaptiveSettingsScreen.kt")
mobile_settings = read(
    "app/src/main/java/com/prelude/iptv/ui/mobile/settings/MobilePremiumSettingsScreen.kt"
)
tv_settings = read(
    "app/src/main/java/com/prelude/iptv/ui/tv/settings/TvPremiumSettingsScreen.kt"
)
if "epgStatus: EpgStatus" not in adaptive_settings or "epgSources: List<EpgSourceOption>" not in adaptive_settings:
    failures.append("adaptive Settings route bypasses typed EPG presentation state")
if "epgStatus: EpgStatus" not in mobile_settings or "epgSources: List<EpgSourceOption>" not in mobile_settings:
    failures.append("mobile Settings route bypasses typed EPG presentation state")
for key in ("epg_settings_programme_guide", "epg_settings_xmltv_matching"):
    if f"R.string.{key}" not in tv_settings:
        failures.append(f"TV Settings EPG row resource missing: {key}")

if failures:
    for failure in failures:
        print(f"FAIL: {failure}")
    sys.exit(1)

print(
    "PASS: staged English/Greek resource parity, release baseline, rollout gate, "
    "activity hosts and UI/model boundaries are intact"
)
