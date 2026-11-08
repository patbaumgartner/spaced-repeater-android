# Changelog

All notable changes are documented here. The project follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

No public release has been published yet.

### Added

- Deck import copies bundled `media/` images to app-private storage, and the
  study screen renders Markdown image references (`![alt](media/file.png)`) on
  the front and back of cards. Media is removed together with its deck.

### Changed

- The dark-theme preference now applies immediately: MainActivity collects the
  preference as a reactive flow, so toggling dark mode in Settings no longer
  requires an app restart.
- Removed the "Open-source licenses" row from Settings; it was a non-clickable
  stub. A licenses screen will return with proper attribution once wired to a
  generated license report.
- Removed the unused `okhttp` dependency; the app is offline-only and never
  referenced it. The manifest still strips any transitive `INTERNET` request.

### Fixed

- Deck import read `deck.json` and `cards/` from the same consumed stream and
  bound card states to card id `0`; packages are now read in a single pass and
  states use the generated card ids.
