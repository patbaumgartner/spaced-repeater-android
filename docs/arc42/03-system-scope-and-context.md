# 3. System Scope and Context

## 3.1 Business Context

Spaced Repeater is a self-contained mobile application. It communicates with **no external systems at runtime**. Its only boundary is the deck package format, produced by external tooling.

```mermaid
flowchart LR
    DA[Deck Author<br/><i>external tooling</i>] -->|deck.zip<br/>deck.json + cards/*.md + media/| App
    subgraph App [Spaced Repeater Android App]
        FS[(App-private storage)]
    end
    L[Learner] -->|import, study, rate| App
    App -->|review reminder| L
    OS[Android OS] -->|time, notifications, SAF| App
```

| Neighbor | Input | Output |
|----------|-------|--------|
| Learner (person) | Deck archives, taps, ratings | Decks list, study UI, progress, notifications |
| Deck package producer (external) | `deck.zip` per deck format spec | — |
| Android OS | File URIs (SAF), clock, notification channel | Permission grants, notification delivery |

## 3.2 Technical Context

The app is offline by design; there is no runtime network boundary. The only integration points are platform channels:

```mermaid
flowchart LR
    subgraph App
        UI[Compose UI]
        Room[(Room DB)]
        Media[(Media dir)]
        Sched[SM-2 Scheduler]
    end
    SAF[Storage Access Framework] -->|Uri| UI
    Sched --> Room
    UI --> Room
    UI -->|image decode| Media
    WM[WorkManager] -->|run at due time| Rem[ReviewReminderWorker]
    Rem -->|post when enabled and due| NC[NotificationManager]
    Rem --> Room
```

- **Storage Access Framework (SAF)** — deck archive selection through a system-provided `content://` URI; no broad storage permission is needed on any supported API level.
- **NotificationManager / WorkManager** — reminder scheduling and delivery.
- **File system (app-private)** — extracted media images.

No network permission is declared: the app performs no network I/O, and the manifest strips any transitive `INTERNET` request via `tools:node="remove"`.
