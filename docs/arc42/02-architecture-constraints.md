# 2. Architecture Constraints

## Technical Constraints

| Constraint | Consequence |
|------------|-------------|
| Kotlin 2.4.21, AGP 9.4.1, Compose BOM 2026.09.00, Material 3 | Modern Android stack; Jetpack Compose is the only UI toolkit. |
| minSdk 26 (Android 8.0), target/compileSdk 37 | Java 8+ APIs available (incl. `java.time`); notification runtime permission must be handled. |
| Single Activity, Compose Navigation | No fragment management; screen state lives in ViewModels. |
| Room as sole persistence layer | All scheduling state must fit a relational model; migrations required for schema changes. |
| Package `com.patbaumgartner.android.spacedrepeater` | Fixed namespace, aligned with the watchdog reference project. |
| SM-2 only (no FSRS) | Scheduler is a pure function over card state + rating; algorithm swap deferred to a later release. |

## Organizational Constraints

| Constraint | Consequence |
|------------|-------------|
| Single developer, open-source project | Bias toward boring, well-documented choices; heavy automated tests compensate for limited review bandwidth. |
| Design language inherited from esp32-room-watchdog-android | Theme (colors, Manrope typography, corner radii) is ported, not reinvented; consistent version catalog and CI workflows. |
| Match watchdog repository conventions (Gradle version catalog, documentation index, and CI workflows) | Repository setup is consistent while keeping app-specific structure and documentation. |

## Development Conventions

- Kotlin code style: official Kotlin coding conventions.
- CI runs lint, unit tests, and a debug build on every push.
- Docs live in `docs/`; decisions in `docs/adr/` (one numbered file per decision).
