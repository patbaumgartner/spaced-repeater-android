# Changelog

All notable changes are documented here. The project follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

No public release has been published yet.

### Added

- Deck import copies bundled `media/` images to app-private storage, and the
  study screen renders Markdown image references (`![alt](media/file.png)`) on
  the front and back of cards. Media is removed together with its deck.

### Fixed

- Deck import read `deck.json` and `cards/` from the same consumed stream and
  bound card states to card id `0`; packages are now read in a single pass and
  states use the generated card ids.
