# ADR-0008: Signed and Verified Commits

- **Status:** Accepted
- **Date:** 2026-11-11
- **Deciders:** Patrick Baumgartner

## Context

Commits on `main` are the provenance anchor for everything the app ships. Some
recent commits (Dependabot bumps, a few direct commits) landed unsigned, and
nothing in the pipeline checked for signatures — the gap only became visible
when inspecting history locally (`git log --format='%G?'` showed `N`).

Options:

1. **No enforcement** — rely on contributor discipline. Rejected: unsigned
   commits were already landing.
2. **CI-only verification** — a workflow checks signatures of the pushed /
   PR range. Good, but a red pipeline can still be merged if the check is not
   required.
3. **Branch ruleset with "Require signed commits" + CI verification** —
   unsigned pushes and unsigned PRs are rejected at the platform level, and
   a required status check re-verifies every commit cryptographically against
   pinned keys. Chosen.

## Decision

All commits that reach `main` must be signed and verified:

- **Local signing:** `commit.gpgsign=true` with the maintainer's GPG key
  (EdDSA, fingerprint `8959B9D5 222D14C0 1CBE9E15 C1D8F804 E42201B3`); the
  public key is pinned in the repo at `.github/keys/patbaumgartner.asc` and is
  on the GitHub account, so github.com shows the green **Verified** badge.
  SSH signing (`gpg.format=ssh`) is equally accepted: allowed principals are
  pinned in `.github/allowed_signers`, and the public key must be added to the
  GitHub account as a **signing** key.
- **CI gate:** `.github/workflows/verify-signatures.yml` verifies every commit
  in the affected range (`before..after` on push, `base...head` on PRs) with
  `git log --format='%G?'` after importing the pinned keys plus GitHub's
  web-flow key (`https://github.com/web-flow.gpg`, which signs web-UI merge
  commits). Anything that is not a good (`G`) signature fails the job.
- **Platform gate:** a branch ruleset on `main` enables `required_signatures`
  and requires the CI status checks, so unsigned commits cannot be pushed or
  merged by anyone except the allowlisted platform bot.
- **Bot exemption:** `dependabot[bot]` commits are exempt (matched by exact
  author identity, reported as warnings) because GitHub does not sign
  Dependabot commits; the bot is in the ruleset bypass list for the same
  reason.

## Consequences

- Contributors must configure signing before pushing (`CONTRIBUTING.md`);
  unsigned PRs are blocked at merge time with a clear error.
- Historical unsigned commits on `main` are not rewritten (that would change
  public SHAs); the gate applies to new commits only.
- The Dependabot exemption is the single documented exception; it is scoped to
  one exact identity, not to "anything that looks like a bot".
