# ADR-0003: Room for Persistence

- **Status:** Accepted
- **Date:** 2026-10-14
- **Deciders:** Patrick Baumgartner

## Context

The app must persist decks, cards, per-card scheduling state, and an immutable review log — fully offline, on-device. Options:

1. **Room** — SQLite ORM with compile-time query checks, `Flow` support, and migration tooling.
2. **Raw SQLite** — full control, maximum verbosity.
3. **SQLDelight** — SQL-first codegen; solid but less integrated with Compose-era Android stacks.
4. **DataStore / files** — not relational; scheduling queries (due cards, aggregates) would be manual.

Queries needed: due cards per deck ordered by overdue, due-count badges per deck, review-log aggregates for stats, all under transactions.

## Decision

Use **Room** as the sole persistence layer with four tables — `Deck`, `Card`, `CardState` (1:1 with Card), `ReviewLog` — as described in the [data model](../arc42/05-building-block-view.md#54-data-model). All access goes through DAOs behind repositories; every multi-row write runs in a transaction.

## Consequences

**Positive**

- Declarative, compile-time-checked SQL for the due-card and aggregate queries that drive the whole app.
- `Flow` return types integrate directly with ViewModel state.
- Built-in migration tooling; schema exports enable review of schema evolution.
- Battle-tested; consistent with the reference project.

**Negative**

- Schema changes require explicit migrations.
- Relational shape forces minor modeling compromises (e.g. tags stored comma-separated today — see [technical debt](../arc42/11-risks-and-technical-debt.md)).
