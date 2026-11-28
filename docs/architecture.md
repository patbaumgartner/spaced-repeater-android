# Architecture at a glance

Spaced Repeater is a single-activity, offline-first Android app. Decks, review
state, statistics, media, and preferences stay on the device; the merged app
manifest removes `INTERNET`. There is no backend, sync service, analytics SDK,
or crash-reporting SDK.

## Implementation map

| Area | Implementation |
|------|----------------|
| UI and navigation | `ui/MainActivity.kt`, Compose screens and ViewModels in `ui/` and `ui/screens/` |
| Domain models | `domain/model/` (`Card`, `CardState`, `ReviewRating`, `Deck`) |
| SM-2 | Framework-free `data/scheduler/SM2Scheduler.kt`, invoked by `CardRepository` |
| Import | `data/importing/`: streaming ZIP reader, manifest parser, Markdown parser, media store, importer |
| Persistence | Room DAOs/entities in `data/local/`, repositories in `data/repository/` |
| Dependency injection | Hilt modules in `di/AppModule.kt` |
| Reminders | `service/ReviewReminderService.kt` with WorkManager and `ReviewReminderWorker` |
| Visual design | `ui/theme/`: cyan accents, warm-neutral light surfaces, dark graphite surfaces, Manrope, Material 3 |

The UI collects repository `Flow`s through ViewModel `StateFlow`s. Rating writes
update the card state and append its review-log row in one Room transaction.
Undo restores the pre-review snapshot and removes that log in a second
transaction. The scheduler itself remains pure Kotlin and is tested on the JVM.

## Offline deck import

`DeckPackageReader` makes one streaming pass over `deck.json`, `cards/*.md`,
and `media/`. Text entries are limited to 1 MiB each, media to 20 MiB each,
the whole expanded archive to 200 MiB, and the archive to 10,000 entries
(including directories). Media names must be flat and match an image-extension
allowlist. Media is staged under app-private `files/media/`; only image paths
inside that root can be rendered.

The importer validates the manifest before database writes, skips individual
unparseable cards, and rejects a deck with no valid cards. Deck, cards, and
initial states commit in one transaction. Media moves into its deck directory
afterward; a media-commit failure removes the newly inserted deck again.

## Persistence and upgrades

`SpacedRepeaterDatabase` is at schema version 2 with exported schemas in
`app/schemas/`. `MIGRATION_1_2` adds `review_log.previousPhase` while retaining
existing decks, cards, states, and logs. Production does not enable destructive
migration fallback. Every future schema change needs an explicit migration,
schema export, and `MigrationTestHelper` coverage on an emulator.

Schema v1 did not record each review's previous phase, so the v1→v2 migration is
intentionally lossy. Each card's earliest row is marked `NEW`; every later row
uses `REVIEW` only as a **non-NEW sentinel** for the daily new-card budget. The
sentinel does not reconstruct exact `LEARNING`/`RELEARNING` history and is never
used as scheduling or replay state. Existing card states and all other review
fields remain unchanged.

## Review reminders

Finishing a study session schedules one uniquely named WorkManager request for
the next real due timestamp, or cancels pending work when reminders are off or
nothing is due later. The worker re-checks the preference, current due cards,
and notification permission before posting. API 33+ requires
`POST_NOTIFICATIONS`; earlier Android versions need no runtime notification
permission. WorkManager timing and reboot/OEM delivery remain best-effort.

## Build, checks, and distribution

The project builds with JDK 27 and Java/Kotlin bytecode 17, Gradle 9.9.2,
AGP 9.5.2, Kotlin 2.4.21, compileSdk 37.1, minSdk 26, and targetSdk 37.
Dependency versions live in `gradle/libs.versions.toml`.

CI runs JVM tests, lint, debug/release assembly, YAML validation, ShellCheck,
and emulator instrumentation. `Test, lint, and package` is an aggregate that
fails unless both `Build and unit tests` and `Instrumented tests (emulator)`
succeed. The separately required CodeQL job analyzes Java/Kotlin.

No ABI splits are configured: published APKs are universal. Main-branch
snapshots publish a signed universal APK and `SHA256SUMS`; `vX.Y.Z` releases
publish a signed universal APK, signed AAB, and `SHA256SUMS`. Local release
assembly without signing credentials remains unsigned.

## Detailed documents

- [Use cases](use-cases/use-cases.md) define learner-visible behavior.
- [arc42](arc42/README.md) covers context, runtime views, and quality goals.
- [ADRs](adr/README.md) preserve architectural decisions and their history.
- [Contributing](../CONTRIBUTING.md) documents validation and hosted branch
  protection.
