# Screenshots

Screenshots are taken with the real app on a device or emulator so the docs
always show authentic UI. The PNG files are committed alongside this catalogue
so published docs render without a capture step.

## Capturing screenshots

### One-time setup (once per machine)

```bash
# Build + install the debug APK on the connected device/emulator
./gradlew assembleDebug installDebug
```

### Capture all screens

```bash
# Run the screenshot instrumented test — captures 6 screens
./gradlew connectedDebugAndroidTest \
  --tests "com.patbaumgartner.android.spacedrepeater.ui.ScreenshotsTest"

# Pull them into docs/screenshots/
./scripts/pull-screenshots.sh
```

### Capture a single screen

```bash
PACKAGE="com.patbaumgartner.android.spacedrepeater"
adb shell am instrument -w \
  "$PACKAGE.test/androidx.test.runner.AndroidJUnitRunner" \
  -e class "$PACKAGE.ui.ScreenshotsTest#homeScreen"
```

## Adding screenshots to docs

Once `pull-screenshots.sh` has written PNG files here, reference them with
standard markdown:

```markdown
![Home screen](screenshots/01_home.png)
```

## Screenshot catalogue

| File | Screen | Notes |
|------|--------|-------|
| `01_home.png` | Home — deck list | Shows FAB, due badge, progress |
| `02_import.png` | Import — SAF picker | Pre-confirm preview |
| `03_study.png` | Study — question front | Card front rendered |
| `04_stats.png` | Stats — progress dashboard | Studied today, retention, streak |
| `05_settings.png` | Settings | Version + commit info |
| `06_study_answer.png` | Study — answer revealed | Rating buttons visible |

## Notes for contributors

- Always re-run the screenshot test after UI changes and update the files.
- Commit the updated PNG files alongside the code change.
- Tap coordinates in `ScreenshotsTest` are tuned for a 1080×2280 Pixel-5
  display. Adjust `input tap X Y` values for other resolutions.
- Screenshots include the status bar and nav bar to reflect the real learner
  experience.
