# 11. Risks and Technical Debt

## Risks

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|------------|--------|------------|
| R1 | SM-2 edge cases deviate from Anki (rounding, Easy bonus, ease floor) | Medium | High — breaks user trust in scheduling | Exhaustive JVM test suite mirroring Anki behavior; golden-case tests ([Q2](10-quality-requirements.md)). |
| R2 | Deck format ambiguity (`---` vs. HTML comments, heading variants) causes import failures | Medium | Medium | MarkdownParser has a documented grammar and fixture-based tests; importer reports per-card errors. |
| R3 | Markdown rendering inconsistency (some markdown not supported) | Medium | Low | Constrain the supported markdown subset and document it in the deck format spec. |
| R4 | Reminder work lost after reboot on some OEMs | Low | Low | WorkManager persists across reboots; reminders are best-effort by design. |
| R5 | Schema evolution breaks existing installs | Low | High | Room migrations mandatory for every schema change; export schema for review. |

## Known Technical Debt

| Item | Why it exists | When to address |
|------|---------------|-----------------|
| Single-level undo only | current scope limit ([UC-05](../use-cases/use-cases.md#uc-05-undo-review)) | Later if learners need multi-step undo. |
| Stats screen optional | Study flows take priority | Revisit after the core study workflows ([UC-06](../use-cases/use-cases.md#uc-06-view-progress)) are complete. |
| `INTERNET` permission declared but unused | Reserves future sync | Remove or use in sync epic. |
| Tags as comma-separated string | Simplest current model | Normalize to a join table when filtering ships. |
| No deck export | Out of current scope | Revisit alongside a stable, documented deck package format. |

## Watch Items

- Compose BOM / AGP upgrades may shift theme behavior — verify against the watchdog reference after each bump.
- Large decks (1000+ cards): due-card query must stay indexed (`dueDate`, `deckId`) to keep study start instant.
