#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

command -v adb >/dev/null 2>&1 || {
  echo "ERROR: adb is not available. Install Android platform-tools." >&2
  exit 2
}

mapfile -t DEVICES < <(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')
if (( ${#DEVICES[@]} == 0 )); then
  echo "ERROR: no ready Android device/emulator is visible to adb." >&2
  adb devices -l >&2 || true
  exit 3
fi

if (( ${#DEVICES[@]} > 1 )) && [[ -z "${ANDROID_SERIAL:-}" ]]; then
  echo "ERROR: multiple devices are attached; set ANDROID_SERIAL explicitly." >&2
  adb devices -l >&2
  exit 4
fi

python3 scripts/compatibility_contracts.py
python3 scripts/architecture_audit.py
python3 scripts/deep_validation_audit.py
python3 scripts/risk_inventory.py

RUN_STAMP="$(date +%Y%m%d-%H%M%S)"
SAFE_SERIAL="$(printf '%s' "${ANDROID_SERIAL:-${DEVICES[0]}}" | tr -c '[:alnum:]_.-' '_')"
ARTIFACT_DIR="validation/device-runs/${RUN_STAMP}-${SAFE_SERIAL}-current"
mkdir -p "$ARTIFACT_DIR"

adb -s "${ANDROID_SERIAL:-${DEVICES[0]}}" shell getprop > "$ARTIFACT_DIR/getprop.txt"
adb -s "${ANDROID_SERIAL:-${DEVICES[0]}}" logcat -c

./gradlew \
  :app:connectedDebugAndroidTest \
  --stacktrace \
  --no-daemon \
  2>&1 | tee "$ARTIFACT_DIR/instrumentation.txt"

SERIAL="${ANDROID_SERIAL:-${DEVICES[0]}}"
adb -s "$SERIAL" shell am force-stop com.prelude.iptv
adb -s "$SERIAL" shell am start -W -n com.prelude.iptv/.StartupActivity > "$ARTIFACT_DIR/launch.txt"
sleep 4
adb -s "$SERIAL" logcat -d -v threadtime > "$ARTIFACT_DIR/logcat.txt"
adb -s "$SERIAL" shell dumpsys meminfo com.prelude.iptv > "$ARTIFACT_DIR/meminfo.txt"
adb -s "$SERIAL" exec-out screencap -p > "$ARTIFACT_DIR/launch.png"

if grep -E 'FATAL EXCEPTION|ANR in com\.prelude\.iptv|Force finishing activity com\.prelude\.iptv' "$ARTIFACT_DIR/logcat.txt"; then
  echo "ERROR: crash/ANR signature detected; see $ARTIFACT_DIR/logcat.txt" >&2
  exit 5
fi

echo "DEVICE QA PASS. Artifacts: $ARTIFACT_DIR"
