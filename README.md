# Spaced Repeater

[![CI](https://github.com/patbaumgartner/spaced-repeater-android/actions/workflows/ci.yml/badge.svg)](https://github.com/patbaumgartner/spaced-repeater-android/actions/workflows/ci.yml)
[![Security](https://github.com/patbaumgartner/spaced-repeater-android/actions/workflows/security.yml/badge.svg)](https://github.com/patbaumgartner/spaced-repeater-android/actions/workflows/security.yml)


Spaced Repeater is an Android flashcard app for studying offline with spaced
repetition. Learners import ZIP deck packages containing Markdown question and
answer cards, review cards when they are due, and rate how well they recalled
each answer. The scheduler uses an Anki-style SM-2 algorithm.

The app is in active development. The architecture and use-case documents
describe the intended design; they are not a guarantee that every described
feature is already available in a release.

## Current scope

- Import ZIP decks containing `deck.json`, Markdown cards, and optional media.
- Study due cards in a question, reveal, and rate session.
- Schedule reviews locally using the SM-2 algorithm.
- Store decks, cards, and review state on the device.
- Schedule optional reminders for cards that are due.

Cloud sync, FSRS, audio cards, cloze deletion, in-app card editing, and
AnkiWeb integration are outside the current scope.

## Requirements

- Android 8.0 (API 26) or newer
- JDK 27
- Android SDK 37

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

Run the same build and test gates as CI:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
yamllint .
git diff --check
```

The release APK built from a source checkout is unsigned.

## Repository structure

```text
app/src/main/       Android application and resources
app/src/test/       JVM unit tests
docs/adr/           Architecture decision records
docs/arc42/         System architecture documentation
docs/use-cases/     User workflows and scenarios
gradle/             Wrapper and dependency version catalog
```

See [docs/README.md](docs/README.md) for the documentation index,
[CONTRIBUTING.md](CONTRIBUTING.md) for the development workflow, and
[SECURITY.md](SECURITY.md) for vulnerability reporting.
