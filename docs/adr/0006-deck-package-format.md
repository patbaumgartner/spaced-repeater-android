# ADR-0006: ZIP-based Markdown Deck Package Format

- **Status:** Accepted
- **Date:** 2026-10-14
- **Deciders:** Patrick Baumgartner

## Context

Decks must be exchangeable, authored outside the app, and importable offline. Options:

1. **ZIP of markdown cards + `deck.json`** (the planned `.apkg`-style package).
2. **Anki `.apkg` (SQLite inside ZIP)** — binary, tool-locked, complex to parse.
3. **Single JSON file** — machine-friendly, poor for authoring prose and images.
4. **Plain folder** — multi-file selection is clunky on Android.

Deck authors want human-readable cards; the app wants a manifest and bundled media.

## Decision

Define a **ZIP-based markdown deck package**:

```
deck.zip
├── deck.json           # name, description, version, cardCount, createdAt, tags
├── cards/
│   ├── card-001.md     # front / back sections
│   └── ...
└── media/
    └── *.png|jpg       # images referenced from cards
```

- Card markdown splits front and back via a `---` separator; `# Question` / `# Answer` headings structure the sections.
- Images live in `media/` and are copied to app-private storage at import. Cards reference them with `![alt](media/<file>)`; file names must be flat (`[A-Za-z0-9._-]`) with a `png`, `jpg`, `jpeg`, `webp`, or `gif` extension, other entries are ignored.
- `deck.json` is validated before any database write.

## Consequences

**Positive**

- Human-authorable: decks can be written in any editor, versioned in git, and reviewed as text.
- No vendor lock-in; trivially produced by scripts or other tools later.
- Manifest enables a preview-before-import UX and integrity checks.

**Negative**

- Markdown subset must be constrained and documented to render consistently ([risk R3](../arc42/11-risks-and-technical-debt.md)).
- Not directly compatible with Anki `.apkg` — a converter would be a separate tool.
- Media deduplication is out of scope; large decks duplicate shared images per package.
