# Spaced Repeater

[![CI](https://github.com/patbaumgartner/spaced-repeater-android/actions/workflows/ci.yml/badge.svg)](https://github.com/patbaumgartner/spaced-repeater-android/actions/workflows/ci.yml)
[![Security](https://github.com/patbaumgartner/spaced-repeater-android/actions/workflows/security.yml/badge.svg)](https://github.com/patbaumgartner/spaced-repeater-android/actions/workflows/security.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

Spaced Repeater is an Android flashcard app for studying offline with spaced
repetition. Learners import ZIP deck packages containing Markdown question and
answer cards, review cards when they are due, and rate how well they recalled
each answer. The scheduler uses an Anki-style SM-2 algorithm.

The app is in active development. The architecture documents describe the
intended design; they are not a guarantee that every described feature is
already available in a release.

## Current scope

- Import ZIP decks containing `deck.json`, Markdown cards, and optional media.
- Study due cards in a question, reveal, and rate session.
- Schedule reviews locally using the SM-2 algorithm.
- Store decks, cards, and review state on the device.
- Schedule optional reminders for cards that are due.

Cloud sync, FSRS, audio cards, cloze deletion, in-app card editing, and
AnkiWeb integration are outside the current scope.

## Download

Signed builds are published on the
[Releases](https://github.com/patbaumgartner/spaced-repeater-android/releases)
page:

- **Releases** (`vX.Y.Z` tags): signed universal APK and AAB, plus `SHA256SUMS`
  and the signer certificate fingerprint in the release notes.
- **Snapshot** (prerelease, rebuilt on every push to `main`): the latest signed
  universal APK and `SHA256SUMS` for testing.

Both use the same release key. A newer Snapshot installs over a stable release,
but Android blocks direct Snapshot → Stable installation as a downgrade.
Switching channels with myApps is manual-only and shows a data-loss warning; it
does not automatically uninstall the app. If you choose to uninstall and
reinstall during a manual switch, local decks, settings, and review history can
be lost.

Verify a download against `SHA256SUMS` before installing. Sideloading on
Android 8+ requires allowing "Install unknown apps" for the app you open the
APK with.

## Requirements

- Android 8.0 (API 26) or newer
- JDK 27
- Android SDK Platform 37.1

Full validation additionally requires Python 3 with PyYAML
(`python3 -c 'import yaml'`), `jq`, `yamllint`, and ShellCheck. None of the
current validation scripts uses Node.

Gradle needs the Android SDK path from `ANDROID_HOME` or an `sdk.dir` entry in
`local.properties`. The first build downloads the Gradle distribution and
Android dependencies.

## Build and validate

```bash
git clone https://github.com/patbaumgartner/spaced-repeater-android.git
cd spaced-repeater-android
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew :app:assembleDebug
```

Run the full local build, package, and preflight gates:

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

A release APK built from a plain source checkout is unsigned. To build a
signed release locally, provide the signing credentials via the
`SR_KEYSTORE_FILE`, `SR_KEYSTORE_PASSWORD`, `SR_KEY_ALIAS`, and
`SR_KEY_PASSWORD` environment variables or a local (git-ignored)
`keystore.properties` file — see [CONTRIBUTING.md](CONTRIBUTING.md#releases)
and [ADR-0009](docs/adr/0009-release-signing-and-distribution.md).

## Repository structure

```text
app/src/main/       Android application and resources
app/src/test/       JVM unit tests
docs/adr/           Architecture decision records
docs/arc42/         System architecture documentation
gradle/             Wrapper and dependency version catalog
```

See [docs/architecture.md](docs/architecture.md) for the implemented
architecture, [docs/README.md](docs/README.md) for the documentation index,
[CONTRIBUTING.md](CONTRIBUTING.md) for the development workflow, and
[SECURITY.md](SECURITY.md) for vulnerability reporting.

## About

Settings shows the app version, open-source notices, and author information.

© 2026 [Patrick Baumgartner](https://patbaumgartner.com/) - Crafted with ❤️

## License

Spaced Repeater is licensed under the [Apache License 2.0](LICENSE). The bundled
Manrope typeface is licensed under the SIL Open Font License 1.1; see the
license notice in Settings. Project and dependency attribution is recorded in
[NOTICE](NOTICE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
