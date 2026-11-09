# Screenshots

Screenshots are taken with the real app on a device or emulator so the docs
always show authentic UI. The PNG files are **not** committed — they are
gitignored (`docs/screenshots/*.png`) and regenerated locally on each machine
with a synthetic deck, so no private names or faces can enter the repository.

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

Captures are taken from a 1080×1920 @420dpi API 35 emulator running the debug
build of this repository. `ScreenshotsTest` self-seeds a synthetic deck (no
private data) directly into the app database before each capture.

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
- **Never commit screenshots captured with the private sample deck** — real names
  and faces must not enter the public repo (see CONTRIBUTING). Capture with a
  synthetic deck (e.g. `app/src/test/resources/fixture-deck/deck.zip`) instead.
- Commit the updated PNG files alongside the code change.
- Screenshot PNGs are gitignored (`docs/screenshots/*.png`) so they are not
  accidentally committed; they are regenerated locally on each machine.
- Navigation in `ScreenshotsTest` uses UiSelector (text / content-description),
  so it works on any display resolution. Captures were taken on a 1080×1920
  @420dpi API 35 emulator.
- Screenshots include the status bar and nav bar to reflect the real learner
  experience.
- On hosts without KVM (for example WSL 2 without nested virtualization), the
  emulator needs `-accel off` (TCG). Android's Watchdog then kills
  `system_server` mid-boot; setting
  `adb shell setprop ro.hw_timeout_multiplier 20` before `system_server`
  starts (then `setprop ctl.restart zygote`) works around it. Boot takes
  roughly 10 minutes and app first frames several minutes more.
- The same slowness affects instrumented UI tests locally: JIT verification
  of Compose code can freeze the app for minutes and the window may not have
  input focus when the first interactions run. Compose `waitUntil` timeouts in
  the UI tests are therefore set to 60 s (not the 1–10 s defaults), and
  `StudyScreenUiTest.setContentAndWait` additionally waits for window focus
  before the first click. On CI (KVM) these waits return almost immediately.
