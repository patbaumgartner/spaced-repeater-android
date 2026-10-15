# Implementation Plan

## Goal
Implement the spaced-repeater-android app end-to-end to match the documented design in `docs/adr/`, `docs/arc42/`, `docs/use-cases/`, referencing `/home/patbaumgartner/GitHub/patbaumgartner/public/esp32-room-watchdog-android` for style.

## Done
- Project bootstrap: Gradle 9.8.1, AGP 9.4.1, Kotlin 2.4.21 (JVM 26), KSP 2.3.12, Compose BOM 2026.09.00, Room 2.8.5, Hilt 2.60.1, Work 2.12.0, Navigation Compose 2.10.2
- JDK 27 via `org.gradle.java.home`; CI updated to Java 27
- Removed all `v1`/`vX` references in docs (README, ADR, arc42, use-cases)
- Theme/fonts/icons aligned to reference style (Apple-clean feel)
- Domain + SM-2 scheduler: pure, tested
- Data layer: Room entities/DAOs, mappers, repositories (Deck, Card, Stats), DeckMediaStore, DeckPackageReader (streaming, path safety, media rewriting), MarkdownParser
- DI (Hilt) + WorkManager + `Configuration.Provider` + notification
- UI: Home (deck list, due+studied progress), Import (preview, confirm, atomic import), Study (flashcard w/images, reveal, 4 ratings, single-level undo, done), Stats (today/retention/streak), Settings (version + commit), Navigation
- Permission request for `POST_NOTIFICATIONS` on TIRAMISU+
- Tests: 17 unit tests incl. E2E test against real sample zip (`sample/web-entwicklung-hs2026-gesichter-namen.zip`)
- Full gate green: testDebugUnitTest, lintDebug, assembleDebug, assembleRelease, yamllint, git diff --check

## Findings (follow-ups, low-risk)
- Deck delete in UI: `deleteDeck` exists in `HomeViewModel` but no confirmation + menu/action wired on Home screen (technical debt, not blocking UC-02)
- Import preview: could show tags/description from manifest (minor UX polish)
- Instrumented androidTest coverage missing (JVM-only today)
- Tags stored comma-separated (see ADR-0003 tech debt); planned join table later
- Deck export missing (out of scope)

## References
- Use-cases: UC-01–UC-07 implemented per spec
- Sample: `sample/web-entwicklung-hs2026-gesichter-namen.zip` (70 cards w/images) validates end-to-end
