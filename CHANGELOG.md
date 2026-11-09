# Changelog

All notable changes are documented here. The project follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

No public release has been published yet.

### Added

- Deck import copies bundled `media/` images to app-private storage, and the
  study screen renders Markdown image references (`![alt](media/file.png)`) on
  the front and back of cards. Media is removed together with its deck.
- Import preview now shows the **actual** number of card files found in the
  archive; when it differs from the manifest-declared `cardCount`, an integrity
  warning is shown (ADR-0006).
- ZIP packages are hardened against zip bombs: per-entry caps plus a cumulative
  uncompressed-byte cap and a maximum-entry count (`DeckPackageReader`).

### Changed

- The dark-theme preference now applies immediately: MainActivity collects the
  preference as a reactive flow, so toggling dark mode in Settings no longer
  requires an app restart.
- Deck due badges on Home are recomputed on a refresh ticker instead of a
  captured `now`, so they stay correct while the app is open (UC-02).
- Review reminders are gated on the Settings toggle: `POST_NOTIFICATIONS` is
  only requested while reminders are enabled, and switching the toggle off
  cancels any pending reminder immediately (UC-07).

### Fixed

- Deck import read `deck.json` and `cards/` from the same consumed stream and
  bound card states to card id `0`; packages are now read in a single pass and
  states use the generated card ids.
- Deck import now persists the deck row, all card rows and their initial NEW
  states in **one** database transaction (arc42 §8.3); a crash mid-import can
  never leave a half-imported deck behind.
- `StudyViewModel.scheduleReminder()` no longer schedules a reminder at
  `Long.MAX_VALUE` when no card is due later: per UC-07 extension 3b, no
  reminder is scheduled (and any pending one is cancelled) when there is
  nothing due.
- The review-reminder worker re-checks the reminders-enabled preference before
  posting, so an opt-out is honored even if a request was already enqueued.
- `MainActivity` no longer requests `POST_NOTIFICATIONS` when reminders are
  disabled.
