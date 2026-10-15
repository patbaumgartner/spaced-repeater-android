# 4. Solution Strategy

The architecture follows one theme: **a thin, reactive UI over a local, testable core**. Every structural decision serves the quality goals privacy, offline capability, and scheduling correctness.

## Approach per Quality Goal

| Quality Goal | Strategy |
|--------------|----------|
| Privacy | No network code in the current release; Room + app-private storage only; no third-party analytics SDKs. |
| Offline capability | All data local from first launch; import works from SAF without permissions on modern Android. |
| Scheduling correctness | `SM2Scheduler` is a pure Kotlin function `(CardState, Rating) → CardSchedule` with exhaustive unit tests; no Android framework dependencies in the scheduler or parser. |
| Responsiveness | Compose declarative UI, Room `Flow`-driven state, coroutines on `Dispatchers.IO` for import; no blocking on the main thread. |
| Data durability | Every rating is a transaction: card-state update + review-log insert commit together. |

## Tactical Decisions (summary)

| Topic | Decision | Rationale | ADR |
|-------|----------|-----------|-----|
| Architecture pattern | MVVM + Repository, single Activity | Clear separation; Compose-native; matches watchdog reference | [ADR-0001](../adr/0001-mvvm-with-repository-pattern.md) |
| DI framework | Hilt | Compile-time DI, first-class Compose/ViewModel support | [ADR-0002](../adr/0002-hilt-for-dependency-injection.md) |
| Persistence | Room | Type-safe SQL, Flow support, migration tooling | [ADR-0003](../adr/0003-room-for-persistence.md) |
| Scheduling algorithm | SM-2 (Anki variant) | Proven, simple, deterministic; matches the Anki-like promise | [ADR-0004](../adr/0004-sm2-scheduling-algorithm.md) |
| UI toolkit | Jetpack Compose + Material 3 | Declarative, themeable, single-source UI | [ADR-0005](../adr/0005-compose-and-material3.md) |
| Deck format | ZIP of markdown cards | Human-authorable, tool-agnostic, diff-friendly | [ADR-0006](../adr/0006-deck-package-format.md) |
| Background work | WorkManager | Deferred work with OS-level constraints, battery-friendly | [ADR-0007](../adr/0007-workmanager-for-reminders.md) |

## Build Blocks at a Glance

- **UI layer** (`ui/`) — Compose screens + ViewModels; no business logic.
- **Domain layer** (`domain/`) — models and the pure SM-2 scheduler; framework-free.
- **Data layer** (`data/`) — Room DAOs, repositories, importer/parser.
- **Service layer** (`service/`) — WorkManager-based reminders.
