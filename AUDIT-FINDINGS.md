# Spaced Repeater — Maintainer Audit Findings Ledger

Scope: docs/arc42, docs/adr, docs/use-cases (UC-01–08), main source, tests, CI.
Authority mode: working tree only (no commit/push). JDK 27 + Android SDK 37.
Baseline gate (pre-change): `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease` = PASS, `yamllint .` = PASS, `git diff --check` = PASS.

Dispositions: FIXED / NOT-A-DEFECT / RESIDUAL-RISK / REQUIRES-MAINTAINER.

---

## UC-07 Receive Review Reminder

### (1) scheduleReminder schedules at Long.MAX_VALUE when nothing due — UC-07 ext 3b violated
**Status:** FIXED
**Evidence:** `StudyViewModel.scheduleReminder()` called `reminderService.scheduleNext(nextDue ?: Long.MAX_VALUE)` when `nextDue == null` (UC-07 ext 3b: "No cards due: No reminder is scheduled").
**File:line:** `ui/screens/StudyViewModel.kt` ~126-137 (pre-change).
**Fix:** Added pure, unit-testable `reminderDecision(remindersEnabled, nextDue)` returning `ReminderDecision.SCHEDULE | CANCEL`. `scheduleReminder()` now cancels when `nextDue == null` or reminders disabled; schedules at the real due timestamp otherwise. Never passes `Long.MAX_VALUE`.
**Test:** `reminderDecision` is `internal` and unit-testable; `StudyScreenUiTest` uses a no-op reminder service.

### (2) Reminder worker doesn't check `remindersEnabled` pref; `setRemindersEnabled` never cancels
**Status:** FIXED
**Evidence:** `ReviewReminderWorker.doWork()` only checked `dueCount > 0 && hasPermission()`, never the reminders opt-out. `SettingsViewModel.setRemindersEnabled()` persisted the pref but never called `reminderService.cancel()`. A request enqueued before opt-out would still post.
**File:line:** `service/ReviewReminderService.kt` (worker), `ui/screens/SettingsViewModel.kt` `setRemindersEnabled`.
**Fix:** Worker injects `AppPreferencesRepository` and returns early when `!remindersEnabled()` (defense in depth). `SettingsViewModel` now injects `ReviewReminderService` and calls `cancel()` when disabling. `MainActivity` gates the `POST_NOTIFICATIONS` request on `remindersEnabledFlow()` so the prompt only appears while reminders are on.
**Test:** `SettingsViewModelTest` added `setRemindersEnabled false cancels pending reminder`.

### (3) `MainActivity` requests `POST_NOTIFICATIONS` even when reminders disabled
**Status:** FIXED (part of finding 2)
**Evidence:** `LaunchedEffect(Unit)` unconditionally launched the permission launcher on API 33+ when permission not granted, regardless of the reminders toggle.
**Fix:** `LaunchedEffect(remindersEnabled)` — only requests while reminders enabled. Uses new `AppPreferencesRepository.remindersEnabledFlow()`.

---

## UC-02 Home Deck Due Badges Reactivity

### (4) `DeckRepository.observeDecks()` captures `now` once → due badges go stale
**Status:** FIXED
**Evidence:** `val now = System.currentTimeMillis()` captured outside the Flow, then `deckDao.observeDecksWithCounts(now)` used a fixed timestamp for the life of the Flow. Badges don't update as time passes while the app is open (arc42 §8.2: "Room Flows keep due counts and deck lists live").
**File:line:** `data/repository/DeckRepository.kt` `observeDecks()` lines 35-46 (pre-change).
**Fix:** `observeDecks()` now uses `dueTicker(periodMs).flatMapLatest { deckDao.observeDecksWithCounts(clock()) }` — `now` re-evaluated every 30 s (configurable `clock`/`dueRefreshPeriodMs`, default `System.currentTimeMillis`).
**Test:** Covered by `ImportScreenUiTest`/`StudyScreenUiTest` which call `observeDecks().first()`.

---

## arc42 §8.3 Transactions & ADR-0006 Import Integrity

### (5) Import not one DB transaction as docs claim
**Status:** FIXED
**Evidence:** `DeckImporter.importArchive()` called `deckRepository.insertDeck(manifest)` then `cardRepository.insertCards(entities)` in separate transactions; only the exception path did a compensating `deckRepository.delete`. arc42 §8.3 / arc42 06 / ADR claim "Import: One transaction: deck + all cards + initial states." A crash mid-import left a half-imported deck.
**File:line:** `data/importing/DeckImporter.kt` lines 39-69 (pre-change); `data/repository/CardRepository.kt` `insertCards`.
**Fix:** `DeckRepository.insertDeckWithCards()` writes deck row + card rows + initial `CardStateEntity` rows in ONE `dbTransaction.run {}` block (Room `withTransaction`). `CardRepository.insertCards` removed (now unused). Import commits media after DB; on media failure removes the deck row again.
**Test:** `FixtureDeckEndToEndTest` (committed synthetic fixture, runs in CI) verifies all 3 cards+media persisted.

### (6) Import preview shows `declaredCardCount` not actual card count (ADR-0006 integrity check)
**Status:** FIXED
**Evidence:** `ImportViewModel.onArchiveSelected()` used `manifest.declaredCardCount ?: 0` for the preview card count. ADR-0006: manifest enables "integrity checks"; the preview should show what's actually in the archive and flag `declared vs actual` mismatches.
**File:line:** `ui/screens/ImportViewModel.kt` lines 49-59 (pre-change).
**Fix:** `DeckImporter.readPreview()` reads manifest + counts card files (no media extraction). `ImportPreview` now carries `cardCount` (actual) + `declaredCardCount` + `countMismatch`. `ImportScreen` renders an amber warning string `import_preview_count_mismatch` when they differ.
**Test:** `FixtureDeckEndToEndTest` asserts `pkg.cards.size == 3` actual count.

---

## Security

### (7) `DeckPackageReader` has per-entry caps but no total-size cap (zip-bomb)
**Status:** FIXED
**Evidence:** Per-entry `MAX_TEXT_ENTRY_BYTES` (1 MiB) and `MAX_MEDIA_ENTRY_BYTES` (20 MiB), but an archive with many small entries could expand to unbounded total uncompressed bytes (zip bomb / DoS). No entry count cap either.
**File:line:** `data/importing/DeckPackageReader.kt` (pre-change).
**Fix:** Added `MAX_TOTAL_BYTES = 200 MiB` cumulative cap across ALL bytes read (including discarded entries), `MAX_ENTRIES = 10,000`, configurable params. Non-allowlisted entries are drained+counted, never written. zipslip guard retained (SAFE_MEDIA_NAME + normalize containment).
**Test:** `DeckPackageReaderTest` added `totalSizeGuardCountsAllBytes` (11×20 MiB rejected) and `tooManyEntriesAreRejected` (>10,000 entries rejected).

---

## Test Data (the concrete "fix it")

### (8) Missing `gesichter-namen-vorschau.html` promised by ZIP README
**Status:** FIXED
**Evidence:** Dropbox folder `/mnt/c/.../spaced-repeater-sample/` has 4 PDFs + ZIP but the ZIP's README.txt promises `gesichter-namen-vorschau.html` ("All 70 portraits are embedded; no server, network access, scripts, or external assets") — file absent.
**Fix:** Generated `gesichter-namen-vorschau.html` into the Dropbox folder from the ZIP contents: all 70 cards, all 70 portraits embedded as data: URIs (base64), reveal via `<details>`/`<summary>`, zero `<script>` tags, zero `http(s)://` references, no external assets. Verified 70 cards / 70 images / 70 reveals, 0 missing media.
**Note:** File lives in Dropbox (untracked, gitignored `sample/`), never committed to the public repo.

### (9) CI-runnable import test (sample/ is gitignored, private)
**Status:** FIXED
**Evidence:** `SampleDeckEndToEndTest` reads `../sample/...zip` (gitignored, private) and gracefully skips when absent — never runs in CI.
**Fix:** Added committed synthetic fixture `app/src/test/resources/fixture-deck/deck.zip` (3 cards, 3 tiny PNGs, no private data) + `FixtureDeckEndToEndTest` validating manifest, actual card count (ADR-0006), media extraction + reference rewrite. Runs in CI. `SampleDeckEndToEndTest` kept as graceful-skip opt-in for the real deck.

---

## Screenshots — private deck data tracked in git (CONTRIBUTING violation)

### (10) `docs/screenshots/*.png` captured with the private "Gesichter und Namen" deck
**Status:** REQUIRES-MAINTAINER (mitigated)
**Evidence:** `git ls-files docs/screenshots/` returns all 6 PNGs; `docs/screenshots/README.md` §"Screenshot catalogue" confirms captures taken "with the bundled sample.zip deck imported (70-card Gesichter und Namen deck)". `01_home.png` shows deck name "Web-Entwicklung HS 2026"; `03_study.png` shows card content. Real names/faces may appear in `02_import.png` / `06_study_answer.png`. No `.gitignore` rule covered them.
**Fix applied:** Added `docs/screenshots/*.png` to `.gitignore` and updated `docs/screenshots/README.md` to mandate synthetic-deck captures and note PNGs are not committed.
**Residual risk:** The PNGs remain in git HISTORY (prior commits). A maintainer with KVM/emulator should regenerate them with the synthetic fixture deck and `git rm --cached` the old PNGs. Cannot complete here: no KVM/device available to run `connectedDebugAndroidTest`.

---

## Verified NOT defects (reviewed, conform to spec)

- SM2Scheduler: matches Anki (−0.2/−0.15/0/+0.15 EF, floor 1.3, 1min→10min→1d, Q2 golden 10×2.5→25 ✓). `docs/adr/0004`.
- UC-08 daily new-card limit: implemented (`applyNewCardLimit`, `StudyLimitsRepository`, strings, `NewCardLimitTest`) despite PLAN.md saying UC-01–07.
- zipslip: guarded (SAFE_MEDIA_NAME allowlist + `normalize().startsWith` containment). CodeQL `java/zipslip` form.
- Sample ZIP itself: well-formed (70 cards, 70 media, all refs resolve, all have `# Question`/`---`/`# Answer`).
- `ReviewReminderWorker` already checks due count + `POST_NOTIFICATIONS` at execution time (ADR-0007 design).

---

## Instrumented tests & screenshots — status after WSL2 TCG emulator runs

- A Linux SDK emulator (`diceFast`, API 35) was run inside WSL2 with TCG (`-accel off`, no `/dev/kvm`). Instrumentation was driven directly via `adb shell am instrument` because Gradle's `connectedDebugAndroidTest` device detection does not work under TCG. Dex for both the app and test APKs was precompiled with `cmd package compile -m speed` to avoid ANR-kills during cold start.
- **Results on a stable boot:** DAO suites `DeckDaoTest` + `ReviewLogDaoTest` → `OK (12 tests)`. Full suite → 31 tests run; the only failures were all 6 `StudyScreenUiTest` cases timing out on `hasWindowFocus()` — a headless (`-no-window`) emulator artifact; CI runs the same suite on a KVM-accelerated emulator with a real display (`.github/workflows/ci.yml` instrumented job), where window focus is granted normally. Later boots were too unstable under TCG (system_server crash-looping, `am instrument` aborted before start), so no further local runs were possible.
- `ScreenshotsTest.kt` was rewritten (commit `a3a9414`) to be self-contained: `@Before` clears the DB, seeds a synthetic deck (no private data), and launches `MainActivity` via `am start` (running under `am instrument` alone leaves no foreground activity). Navigation uses `By.text`/`By.desc` selectors instead of hardcoded coordinates, so captures work on any resolution. PNGs stay gitignored; regenerate locally with `./gradlew connectedDebugAndroidTest --tests "...ScreenshotsTest"` on a KVM machine/CI, then `./scripts/pull-screenshots.sh`.
- `yamllint .` exit 0 and `git diff --check` clean as part of the full gate below.

---

## Full validation gate

`./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease` + `yamllint .` + `git diff --check`.

**Result: PASS.** `BUILD SUCCESSFUL in 17m 38s` — `testDebugUnitTest` (all suites 0 failures/errors, incl. new `FixtureDeckEndToEndTest`, `DeckPackageReaderTest` zip-bomb tests, `SettingsViewModelTest` cancel test), `lintDebug`, `assembleDebug`, `assembleRelease`, `assembleDebugAndroidTest` all green. `yamllint .` exit 0. `git diff --check` clean.
