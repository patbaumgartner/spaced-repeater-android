# 7. Deployment View

## 7.1 Deployment Context

A single artifact ships to a single node: the learner's Android device. There are no servers.

```mermaid
flowchart LR
    subgraph Store[Distribution]
        PS[Direct APK / sideload]
    end
    subgraph Device[Android Device — API 26+]
        subgraph AppPkg[com.patbaumgartner.android.spacedrepeater]
            APK[App process]
            DB[(App-private DB<br/>databases/)]
            Media[(App-private media<br/>files/media/)]
        end
        WM[WorkManager]
    end
    PS --> APK
    APK --- DB
    APK --- Media
    WM -.->|runs reminders| APK
```

## 7.2 Infrastructure Mapped to Building Blocks

| Node / Storage | Building Blocks | Notes |
|----------------|-----------------|-------|
| App process (foreground) | MainActivity, ViewModels, screens, components | Compose UI; single task. |
| App process (background) | ReviewReminderService via WorkManager | Work requests survive process death and reboots. |
| `databases/` (app-private) | Room database | Only writable location for structured data. |
| `files/media/` (app-private) | Imported images | Referenced by cards; removed with deck deletion. |
| Cache dir (temp) | Import extraction staging | Cleared after import; safe to lose. |

## 7.3 Build Variants

| Variant | minifyEnabled | shrinkResources | Purpose |
|---------|---------------|-----------------|---------|
| `debug` | false | false | Development; verbose logging. |
| `release` | true | true | Distribution; ProGuard/R8, signed. |

## 7.4 Permissions Mapping

| Permission | Purpose | Scope |
|------------|---------|-------|
| `POST_NOTIFICATIONS` (runtime, API 33+) | Review reminders | Optional; app fully functional without it. |
| `RECEIVE_BOOT_COMPLETED` | Required by WorkManager's `RescheduleReceiver`: re-enqueues pending work (the review reminder) after a reboot | Declared in the app manifest; WorkManager runtime-enables its receiver only while unfinished work exists. |
| `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE` | Declared by the WorkManager library (transitive) | Not granted-claiming features of the app itself. |
| `INTERNET` | Not declared; stripped via `tools:node="remove"` so no transitive dependency can re-add it | No network usage. |
