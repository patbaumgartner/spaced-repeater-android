# 6. Runtime View

## 6.1 Deck Import

Covers [UC-01](../use-cases/use-cases.md#uc-01-import-deck). Import is transactional: either the deck appears complete (minus explicitly skipped cards) or nothing is persisted.

```mermaid
sequenceDiagram
    actor L as Learner
    participant S as ImportScreen
    participant VM as ImportViewModel
    participant SAF as File Picker (SAF)
    participant Imp as DeckImporter
    participant DB as Room Database
    participant FS as Media Storage

    L->>S: Tap import
    S->>SAF: Launch picker (.zip)
    SAF-->>S: content:// Uri
    S->>VM: onArchiveSelected(uri)
    VM->>Imp: import(uri)
    Imp->>Imp: Stream ZIP → temp dir
    Imp->>Imp: Validate deck.json
    Imp-->>VM: Preview {name, cardCount, validCards}
    VM-->>S: Show preview
    L->>S: Confirm
    Imp->>DB: Transaction: deck + cards + initial states
    Imp->>FS: Copy media/ files
    DB-->>VM: Done (imported N, skipped M)
    VM-->>S: Navigate Home, show result
```

Error handling: validation failures abort before any DB write; per-card parse failures are skipped and reported (`imported N, skipped M`); the source archive is never modified.

## 6.2 Study Session

Covers [UC-03](../use-cases/use-cases.md#uc-03-study-deck) and [UC-04](../use-cases/use-cases.md#uc-04-rate-recall).

```mermaid
sequenceDiagram
    actor L as Learner
    participant SS as StudyScreen
    participant SVM as StudyViewModel
    participant Repo as CardRepository
    participant SM2 as SM2Scheduler
    participant DB as Room Database

    L->>SS: Select deck (Home)
    SS->>SVM: startSession(deckId)
    SVM->>Repo: dueCards(deckId, now)
    Repo-->>SVM: Flow<List<Card>>
    SVM->>SVM: applyNewCardLimit (UC-08: cap NEW cards)
    SVM-->>SS: Show question (front)

    L->>SS: Tap card
    SS-->>L: Reveal answer (back)

    L->>SS: Tap rating (e.g. Good)
    SS->>SVM: rate(cardId, GOOD)
    SVM->>SM2: calculateNextReview(state, GOOD)
    SM2-->>SVM: CardSchedule {interval, EF, dueDate, next}
    SVM->>DB: Transaction {update CardState, insert ReviewLog, snapshot for undo}
    DB-->>SVM: Committed
    SVM-->>SS: Next due card (or session complete)
```

Key properties:

- The rating transaction is atomic — state update and review log commit together ([data durability](10-quality-requirements.md)).
- Undo uses the pre-review snapshot written inside the same transaction ([UC-05](../use-cases/use-cases.md#uc-05-undo-review)).
- Scheduler runs on the JVM with no framework dependency; the same code path is unit tested.

## 6.3 Review Reminder

Covers [UC-07](../use-cases/use-cases.md#uc-07-receive-review-reminder).

```mermaid
sequenceDiagram
    participant SVM as StudyViewModel
    participant Rem as ReviewReminderService
    participant WM as WorkManager
    participant OS as Android OS
    actor L as Learner

    SVM->>Rem: afterSession(nextDueAt, dueCount)
    Rem->>WM: OneTimeWorkRequest(delay = nextDueAt - now)
    Note over WM: OS persists work across reboots
    WM->>Rem: fire at due time
    Rem->>Rem: Query due count
    alt permission granted AND due > 0
        Rem->>OS: Post notification "N cards due"
        OS-->>L: Notification
        L->>OS: Tap
        OS->>L: Open app on Home Screen
    else no permission or none due
        Note over Rem: No-op
    end
```

## 6.4 App Startup

```mermaid
sequenceDiagram
    participant OS as Android OS
    participant App as SpacedRepeaterApp
    participant Hilt as DI Graph
    participant Act as MainActivity
    participant Home as HomeScreen
    participant DB as Room

    OS->>App: Launch
    App->>Hilt: @HiltAndroidApp init
    Hilt->>Act: inject MainActivity
    Act->>Home: set Compose content
    Home->>DB: observe decks + due counts (Flow)
    DB-->>Home: reactive updates
```
