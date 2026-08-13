#!/usr/bin/env python3
"""Keep release metadata and generated reports from drifting out of date."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


gradle = read("app/build.gradle.kts")
readme = read("README.md")
changelog = read("CHANGELOG.md")
agents = read("AGENTS.md")
maintenance = read("docs/MAINTENANCE.md")
handoff = read("docs/NEXT_CHAT_HANDOFF.md")

name_match = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)
code_match = re.search(r"versionCode\s*=\s*(\d+)", gradle)
if not name_match or not code_match:
    print("FAIL: app version could not be read from app/build.gradle.kts")
    sys.exit(1)

version_name = name_match.group(1)
version_code = code_match.group(1)
failures: list[str] = []

authorization_markers = {
    "AGENTS.md": (agents, "no exceptions", "There is no \"too small to preview\" exception"),
    "docs/MAINTENANCE.md": (maintenance, "Every visual change", "owner's explicit statement that it is OK"),
    "docs/NEXT_CHAT_HANDOFF.md": (handoff, "Separate explicit owner order", "wait for an explicit \"OK\""),
}
for path, (body, first, second) in authorization_markers.items():
    if first not in body or second not in body:
        failures.append(f"mandatory exact-scope authorization rule is missing from {path}")

if f"Current app version: **{version_name}** (`versionCode {version_code}`)" not in readme:
    failures.append("README current-version line does not match app/build.gradle.kts")

release_heading = rf"(?m)^##\s+{re.escape(version_name)}\s+(?:-|\u2014)\s+versionCode\s+{re.escape(version_code)}\s*$"
if not re.search(release_heading, changelog):
    failures.append("CHANGELOG has no heading for the current app version/versionCode")

for path in ROOT.iterdir():
    if not path.is_file():
        continue
    name = path.name
    if re.search(
        r"(?i)^(CHANGELOG_.+|.*VALIDATION.*|.*_REPORT|BUILD_FIX_.+|"
        r"gradle_(?:build|compile|test)_attempt.+|.*SEMANTIC.*|.*WIRING_AUDIT.*)"
        r"\.(?:md|txt|exitcode)$",
        name,
    ):
        failures.append(f"generated/historical artifact belongs under docs/archive, not root: {name}")

if failures:
    for failure in failures:
        print(f"FAIL: {failure}")
    sys.exit(1)

print(f"PASS: documentation matches app {version_name} ({version_code}) and root is clean")
