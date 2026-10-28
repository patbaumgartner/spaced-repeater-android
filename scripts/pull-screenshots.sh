#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# pull-screenshots.sh
# ─────────────────────────────────────────────────────────────────────────────
# Pulls screenshots captured by ScreenshotsTest from a connected Android device
# or emulator into docs/screenshots/.
#
# Prerequisites:
#   1. Build + install:   ./gradlew assembleDebug installDebug
#   2. Run screenshots:  ./gradlew connectedDebugAndroidTest \
#                            --tests "com.patbaumgartner.android.spacedrepeater.ui.ScreenshotsTest"
#   3. Then run this script.
#
# For a single screen:
#   adb shell am instrument -w .../androidx.test.runner.AndroidJUnitRunner \
#     -e class com.patbaumgartner.android.spacedrepeater.ui.ScreenshotsTest#homeScreen
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SRC="/sdcard/Android/data/com.patbaumgartner.android.spacedrepeater/files/Pictures/SpacedRepeater"
DEST="$(cd "$(dirname "$0")/.." && pwd)/docs/screenshots"

mkdir -p "$DEST"
echo "Pulling screenshots from $SRC → $DEST"
adb pull "$SRC/" "$DEST/"
echo "Done. $(ls "$DEST"/*.png 2>/dev/null | wc -l) PNG file(s) in $DEST"
echo ""
echo "Files:"
ls -lh "$DEST"/*.png 2>/dev/null || echo "(no files found — is the device connected and tests run?)"