# 12. Glossary

| Term | Definition |
|------|------------|
| **Card** | A single Q&A unit: markdown front (question), markdown back (answer), optional tags. |
| **Card State** | Scheduling data for one card: `repetitions`, `easeFactor`, `interval`, `dueDate`, and phase (NEW / LEARNING / REVIEW / RELARNING). |
| **Deck** | A named collection of cards imported from one deck package. |
| **Deck package** | A `.zip` archive containing `deck.json`, `cards/*.md`, and `media/` — the interchange format ([ADR-0006](../adr/0006-deck-package-format.md)). |
| **Due card** | A card whose `dueDate` is at or before now, in the LEARNING or REVIEW phase. |
| **Ease factor (EF)** | Multiplier applied to the previous interval on each successful review; default 2.5, floor 1.3. |
| **Graduate** | Leave the learning phase and enter the review queue with a 1-day interval. |
| **Interval** | Days until a card's next review. |
| **Lapse** | An "Again" rating on a review-phase card: repetitions reset, card enters RELARNING. |
| **Learning phase** | Short stepping-stone reviews (1 min → 10 min) before graduation to review. |
| **Rating** | Learner's recall assessment: Again (0), Hard (1), Good (2), Easy (3). |
| **Review log** | Immutable record of one rating with timestamp, resulting interval and ease factor, and time taken. |
| **Review session** | One focused pass over a deck's due cards ([UC-03](../use-cases/use-cases.md#uc-03-study-deck)). |
| **SM-2** | SuperMemo-2 scheduling algorithm, Anki variant ([ADR-0004](../adr/0004-sm2-scheduling-algorithm.md)). |
| **Streak** | Consecutive days with at least one review. |
| **Retention rate** | Share of answers not rated "Again" over a period. |
| **SAF** | Storage Access Framework — Android's file picker without broad storage permissions. |
| **UDF** | Unidirectional data flow: state flows down, events flow up. |
