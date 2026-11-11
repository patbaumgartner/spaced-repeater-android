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

## Sign your commits

Every commit that reaches `main` must be signed and verified (see
[ADR-0008](docs/adr/0008-signed-commits.md)). CI rejects unsigned commits, and
the `main` branch ruleset requires signed commits, so configure signing once:

```bash
git config --global user.signingkey <your-key-id>   # e.g. GPG or SSH signing key
git config --global commit.gpgsign true
git config --global gpg.format openpgp               # or "ssh" for SSH signing
```

Add the matching **public** key to your GitHub account (Settings → SSH and GPG
keys) so commits show the **Verified** badge. The maintainer key is pinned in
the repo at `.github/keys/` and is what CI verifies against. SSH signing
(`gpg.format=ssh`) is also fine — add the key as a *signing* key on your
account and make sure your identity is listed in `.github/allowed_signers`,
which CI uses to verify SSH signatures. Never commit private keys or other
secret signing material. The only exempt identity is the `dependabot[bot]`
platform bot, which GitHub does not sign.

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
