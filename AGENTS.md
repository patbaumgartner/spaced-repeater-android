# Spaced Repeater — Agent Guide

Spaced Repeater is an offline-first Android flashcard app. Start with
[README.md](README.md) for project setup,
[docs/architecture.md](docs/architecture.md) for the implemented architecture,
[docs/README.md](docs/README.md) for the full documentation index, and
[CONTRIBUTING.md](CONTRIBUTING.md) for the contributor workflow.

## Build and validate

Use JDK 27 and Android SDK Platform 37.1. Gradle needs the SDK location from
`ANDROID_HOME` or `sdk.dir` in `local.properties`. Full validation also requires
Python 3 with PyYAML (`python3 -c 'import yaml'`), `jq`, `yamllint`, and
ShellCheck. Current validation scripts do not use Node.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
./gradlew assembleDebugAndroidTest
./gradlew bundleRelease
./scripts/test-dependabot-ruleset.sh
./scripts/test-dependabot-update-type.sh
./scripts/test-release-workflow.sh
yamllint .
find . -name '*.sh' -not -path './*/build/*' -exec shellcheck {} +
git diff --check
```

Run the focused task covering a change while iterating, then run the full gate
above before considering the change complete.

## Project structure and conventions

- The Android application lives in `app/`; JVM tests live under
  `app/src/test/`, instrumented DAO tests under `app/src/androidTest/`
  (run with `connectedDebugAndroidTest` on a device or emulator; CI runs
  them on an emulator).
- Dependencies and plugin versions belong in `gradle/libs.versions.toml`.
- Keep scheduling and other business rules independently testable; avoid
  coupling domain behavior to Android APIs.
- Keep user-facing copy in Android string resources.
- Update the relevant architecture, use-case, or ADR document when a change
  alters documented behavior or a lasting technical decision.
- Follow the official Kotlin style and the surrounding code's conventions.
- Never commit `local.properties`, signing material, credentials, generated
  build output, or private deck data.
