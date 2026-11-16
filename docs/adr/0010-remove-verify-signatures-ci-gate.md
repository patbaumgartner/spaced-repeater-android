# ADR-0010: Drop the verify-signatures CI workflow; keep the platform ruleset gate

- **Status:** Accepted
- **Date:** 2026-11-16
- **Deciders:** Patrick Baumgartner
- **Supersedes:** [ADR-0008](0008-signed-commits.md)

## Context

ADR-0008 enforced signed commits with two layers: a branch ruleset with
"Require signed commits" (`required_signatures`) plus a required CI status
check (`.github/workflows/verify-signatures.yml`) that re-verified every
commit in the pushed/PR range against a pinned trust store
(`.github/keys/*.asc`, `.github/allowed_signers`, GitHub's web-flow key).

The CI layer caused operational problems without adding real security:

- Dependabot pushes to `main` failed the job, because GitHub does not sign
  bot commits and the exemption matched an exact author email while git
  records `49699333+dependabot[bot]@users.noreply.github.com`.
- Force-pushes made the job scan the entire history and fail on those same
  unsigned bot commits.
- The ruleset already rejects unsigned commits at the platform level, so the
  workflow only re-checked what the platform had already enforced.

## Decision

Remove `.github/workflows/verify-signatures.yml` and its pinned trust store
(`.github/keys/`, `.github/allowed_signers`). Signed commits remain mandatory:

- The `main` branch ruleset keeps `required_signatures`, so unsigned commits
  cannot be pushed or merged (platform bot excepted, via its bypass entry).
- The ruleset keeps requiring the remaining status checks (CI, Security, and
  release checks); the "Verify commit signatures" context is dropped from the
  required list because the workflow no longer exists.
- The **Verified** badge on github.com comes from keys uploaded to the
  GitHub account, not from the repo-pinned copies, so nothing user-visible
  changes.
- Docs (`CONTRIBUTING.md`, PR template) point at the ruleset gate instead of
  the removed CI check.

## Consequences

- Dependabot pushes and force-pushes no longer produce a red signature job.
- There is no per-range cryptographic re-verification in CI anymore; signing
  is enforced by the platform ruleset and surfaced via the Verified badge.
- Contributors still configure `commit.gpgsign=true`; unsigned PRs are
  blocked at merge time by the ruleset with a clear error.
