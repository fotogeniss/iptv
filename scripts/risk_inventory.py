#!/usr/bin/env python3
"""Deterministic production risk inventory for the current app version."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java"
GRADLE = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
VERSION = re.search(r'versionName\s*=\s*"([^"]+)"', GRADLE)

CRITICAL_PATTERNS = {
    "non_null_assertions": re.compile(r"!!"),
    "global_scope": re.compile(r"\bGlobalScope\b"),
    "blocking_run": re.compile(r"\brunBlocking\s*\("),
    "thread_sleep": re.compile(r"\bThread\.sleep\s*\("),
    "process_exit": re.compile(r"(?:Runtime\.getRuntime\(\)\.exit|System\.exit)\s*\("),
    "unsafe_saf_stream_assertion": re.compile(r"open(?:Input|Output)Stream\([^\n]*\)!!"),
    "stalker_session_assertion": re.compile(r"\bactiveUa!!"),
}
INFORMATIONAL_PATTERNS = {
    "catch_exception": re.compile(r"catch\s*\([^)]*:\s*Exception\b"),
    "catch_throwable": re.compile(r"catch\s*\([^)]*:\s*Throwable\b"),
    "post_delayed": re.compile(r"\bpostDelayed\s*\("),
    "todo_fixme": re.compile(r"\b(?:TODO|FIXME)\b"),
}


def matches(pattern: re.Pattern[str]) -> list[str]:
    found: list[str] = []
    for path in sorted(SRC.rglob("*.kt")):
        text = path.read_text(encoding="utf-8")
        for lineno, line in enumerate(text.splitlines(), start=1):
            if pattern.search(line):
                found.append(f"{path.relative_to(ROOT)}:{lineno}: {line.strip()}")
    return found


critical = {name: matches(pattern) for name, pattern in CRITICAL_PATTERNS.items()}
info = {name: matches(pattern) for name, pattern in INFORMATIONAL_PATTERNS.items()}

print(f"PRODUCTION RISK INVENTORY v{VERSION.group(1) if VERSION else 'unknown'}")
for name, findings in critical.items():
    status = "PASS" if not findings else "FAIL"
    print(f"{status} {name}={len(findings)}")
    for finding in findings[:20]:
        print(f"  {finding}")
    if len(findings) > 20:
        print(f"  ... {len(findings) - 20} more")

for name, findings in info.items():
    print(f"INFO {name}={len(findings)}")

# Large files are debt, not release failures while compatibility contracts exist.
large_files = []
for path in sorted(SRC.rglob("*.kt")):
    lines = len(path.read_text(encoding="utf-8").splitlines())
    if lines > 1500:
        large_files.append((path.relative_to(ROOT), lines))
print(f"INFO files_over_1500_lines={len(large_files)}")
for path, lines in large_files:
    print(f"  {path}: {lines}")

failure_count = sum(bool(items) for items in critical.values())
print(f"SUMMARY critical_categories_failed={failure_count} informational_categories={len(info)}")
sys.exit(1 if failure_count else 0)
