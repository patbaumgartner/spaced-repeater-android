# Contributing

Thanks for helping improve Spaced Repeater. Focused changes with tests for
changed behavior are easiest to review.

## Set up a development environment

Install JDK 27 and Android SDK 37, then clone the repository. Android Studio can
open the root directory directly. Command-line builds use the checked-in Gradle
wrapper and need `ANDROID_HOME` set or an `sdk.dir` entry in
`local.properties`.

```bash
git clone https://github.com/patbaumgartner/spaced-repeater-android.git
cd spaced-repeater-android
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew :app:assembleDebug
```

## Make a change

1. Create a branch from `main`.
2. Keep the change scoped to one concern.
3. Add or update tests for changed behavior.
4. Keep user-facing text in Android string resources.
5. Never commit local configuration, credentials, signing material, generated
   build output, or private deck data.

Use Conventional Commit subjects, for example `fix: preserve review state` or
`docs: clarify deck package format`.

## Validate

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
yamllint .
git diff --check
```

If you changed DAOs, entities, or Room queries, also run the instrumented
DAO tests on a device or emulator (`./gradlew connectedDebugAndroidTest`); CI
runs them automatically.

## Pull requests

Explain the user-visible effect, why the change is needed, and exactly how it
was tested. Include screenshots for visual changes when they make review
materially easier. Do not include private decks or learner data in screenshots,
logs, fixtures, or issue reports.

Report security vulnerabilities privately as described in
[SECURITY.md](SECURITY.md), not in a public issue.
