# 1. Introduction and Goals

## 1.1 Requirements Overview

**Spaced Repeater** is an Android flashcard app that works like Anki: learners import deck packages (ZIP archives of markdown Q&A cards), study due cards in focused sessions, and rate their recall. The SM-2 algorithm schedules each card's next review.

Core capabilities:

- **Deck import** — load `.zip` packages containing `deck.json`, markdown cards, and media files.
- **Study sessions** — question → reveal → rate (Again / Hard / Good / Easy) loop.
- **Daily new-card limit** — caps first-time card introductions per day (default 20, 0 = unlimited).
- **SM-2 scheduling** — per-card intervals, ease factors, learning/review/relearning phases.
- **Progress visibility** — due counts, studied/total progress, streak and retention stats.
- **Review reminders** — optional notifications derived from real due dates.

Explicit non-goals (current scope): cloud sync, audio cards, cloze deletion, in-app card editing, AnkiWeb integration, FSRS.

## 1.2 Quality Goals

| # | Quality | Motivation |
|---|---------|------------|
| 1 | **Privacy** | All data stays on-device; no network calls, no analytics, no tracking. |
| 2 | **Offline capability** | Every feature works without connectivity; the app is functional from first launch. |
| 3 | **Scheduling correctness** | SM-2 intervals and ease factors match Anki semantics exactly — the scheduler is the trust core of the app. |
| 4 | **Responsiveness** | Card transitions and rating taps feel instant (< 100 ms perceived latency). |
| 5 | **Data durability** | Reviews and card states survive process death and device restarts without loss. |

## 1.3 Stakeholders

| Stakeholder | Role / Expectation |
|-------------|--------------------|
| Learner | Imports decks, studies reliably, keeps data private. |
| Deck Author | Produces deck packages outside the app; needs a stable, documented deck format. |
| Developer (patbaumgartner) | Maintains a small, testable codebase with CI; values conventions from esp32-room-watchdog-android. |
| Open-source users | Transparent, self-contained code they can audit and extend. |
