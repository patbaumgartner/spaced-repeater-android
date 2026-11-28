# Contributing

Thanks for helping improve Spaced Repeater. Focused changes with tests for
changed behavior are easiest to review.

## Set up a development environment

Install JDK 27 and Android SDK Platform 37.1, then clone the repository.
Android Studio can open the root directory directly. Command-line builds use the
checked-in Gradle wrapper and need `ANDROID_HOME` set or an `sdk.dir` entry in
`local.properties`.

Full validation also requires Python 3 with PyYAML (verify with
`python3 -c 'import yaml'`), `jq`, `yamllint`, and ShellCheck. The workflow
controls use Python/PyYAML and `jq`; none of the current scripts requires Node.

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

Every commit that reaches `main` must be signed (see
[ADR-0010](docs/adr/0010-remove-verify-signatures-ci-gate.md), which supersedes
[ADR-0008](docs/adr/0008-signed-commits.md)). The `main` branch ruleset has
"Require signed commits" enabled, so unsigned commits are rejected at the
platform level. Configure signing once:

```bash
git config --global user.signingkey <your-key-id>   # e.g. GPG or SSH signing key
git config --global commit.gpgsign true
git config --global gpg.format openpgp               # or "ssh" for SSH signing
```

Add the matching **public** key to your GitHub account (Settings → SSH and GPG
keys) so commits show the **Verified** badge. Never commit private keys or
other secret signing material. The only exempt identity is the
`dependabot[bot]` platform bot, which GitHub does not sign; it is in the
ruleset bypass list for that reason.

## Releases

Downloadable builds are published from `.github/workflows/release.yml` (see
[ADR-0009](docs/adr/0009-release-signing-and-distribution.md)):

- Every push to `main` rebuilds the **Snapshot** prerelease with a signed
  universal APK and `SHA256SUMS`.
- Pushing a signed tag `vX.Y.Z` publishes a **release**: signed universal APK,
  signed AAB, and `SHA256SUMS`. The tag sets the version (`v0.1.0` → versionName `0.1.0`,
  versionCode `100`):

  ```bash
  git tag -s v0.1.0 -m "v0.1.0"
  git push origin main v0.1.0
  ```

Both jobs sign with the release key held in the repository secrets
`SR_KEYSTORE_BASE64`, `SR_KEYSTORE_PASSWORD`, `SR_KEY_ALIAS`, and
`SR_KEY_PASSWORD`; the workflow fails if they are missing. To set or rotate
the key, generate a PKCS12 keystore with `keytool` and upload it with
`gh secret set` — never commit keystore material. Keep one backup of the
keystore outside the repository (password manager / HSM); losing it means
updates can no longer be signed with the same key.

For local signed builds, either export the four `SR_*` environment variables
or create a git-ignored `keystore.properties` with `storeFile`,
`storePassword`, `keyAlias`, `keyPassword`, then run
`./gradlew :app:assembleRelease`.

## Validate

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

If you changed DAOs, entities, or Room queries, also run the instrumented
DAO tests on a device or emulator (`./gradlew connectedDebugAndroidTest`); CI
runs them automatically.

## Hosted merge protection

The active `protect-main` ruleset targets `refs/heads/main` and requires
`Test, lint, and package`, `Instrumented tests (emulator)`, and `CodeQL`, with
strict up-to-date enforcement. The first context is CI's aggregate `verify`
job: it fails unless both `Build and unit tests` and the instrumented job
succeed. The separately required instrumented context remains intentional.

Dependabot auto-merge accepts only explicit semver-patch and semver-minor
metadata; empty, unknown, and major update types are rejected before protection
checks. Patch/minor auto-merge then makes only a metadata-readable, paginated
request to `repos/$GITHUB_REPOSITORY/rules/branches/main?per_page=100`. It fails
closed on API errors and requires a `required_status_checks` rule containing the
exact `Test, lint, and package` context. It does not call classic branch
protection or ruleset-detail endpoints and never checks out or executes
pull-request code under `pull_request_target`.

### Dependabot auto-merge token

Only the final `gh pr merge` step uses `secrets.AUTO_MERGE_TOKEN`; Dependabot
metadata fetching and the branch-rules preflight keep using `GITHUB_TOKEN`. The
merge step has a nonempty-token guard that fails without logging the value.
Configure a fine-grained personal access token restricted to this repository
with:

- **Contents: Read and write** to create the merged commit on `main`.
- **Pull requests: Read and write** to enable auto-merge on eligible PRs.
- **Workflows: Read and write** to merge dependency PRs that also update
  `.github/workflows/**`.

Using the dedicated token for the merge allows the resulting `main` push to
trigger post-merge CI and Snapshot publication. Keep the token out of source,
logs, and local validation fixtures, and rotate/revoke it through GitHub when
needed. No token is needed to run the local controls.

Run `scripts/test-dependabot-ruleset.sh` for paginated branch-rule fixtures,
`scripts/test-dependabot-update-type.sh` for eligibility controls, and
`scripts/test-release-workflow.sh` for release SDK coverage. CI runs all three
controls in its validation step after installing PyYAML/Yamllint in an isolated
Python environment and checking `jq`.

**Hosted state verified 2026-11-27:** `main` had zero hosted check runs. The
ruleset requirements above describe merge policy, not passing-check evidence
for the current main commit; use an actual PR or workflow run for build health.

## Pull requests

Explain the user-visible effect, why the change is needed, and exactly how it
was tested. Include screenshots for visual changes when they make review
materially easier. Do not include private decks or learner data in screenshots,
logs, fixtures, or issue reports.

Report security vulnerabilities privately as described in
[SECURITY.md](SECURITY.md), not in a public issue.
