#!/usr/bin/env python3
"""Static release gates for Prelude+ staged localization."""
from __future__ import annotations

import sys
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

if failures:
    for failure in failures:
        print(f"FAIL: {failure}")
    sys.exit(1)

print(
    "PASS: staged English/Greek resource parity, release baseline, rollout gate, "
    "activity hosts and UI/model boundaries are intact"
)
