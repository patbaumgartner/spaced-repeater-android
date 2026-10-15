# 9. Architecture Decisions

All decisions are recorded as ADRs in [docs/adr/](../adr/README.md) using the Michael Nygard template (context → decision → consequences).

| ID | Decision | Status |
|----|----------|--------|
| [ADR-0001](../adr/0001-mvvm-with-repository-pattern.md) | MVVM with Repository pattern | Accepted |
| [ADR-0002](../adr/0002-hilt-for-dependency-injection.md) | Hilt for dependency injection | Accepted |
| [ADR-0003](../adr/0003-room-for-persistence.md) | Room for persistence | Accepted |
| [ADR-0004](../adr/0004-sm2-scheduling-algorithm.md) | SM-2 scheduling algorithm (Anki variant) | Accepted |
| [ADR-0005](../adr/0005-compose-and-material3.md) | Jetpack Compose with Material 3 | Accepted |
| [ADR-0006](../adr/0006-deck-package-format.md) | ZIP-based markdown deck package format | Accepted |
| [ADR-0007](../adr/0007-workmanager-for-reminders.md) | WorkManager for review reminders | Accepted |

Decision drivers recap: privacy and offline capability push everything local; scheduling correctness pushes the algorithm into a pure, framework-free core; the watchdog reference project pre-decides style, stack, and repository conventions.
