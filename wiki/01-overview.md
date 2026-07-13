# 01 · Overview

## What the app does

ReroPlero is a simple personal expense tracker. A user registers / logs in,
then records **payments** (an expense: category, cost, date-time). Payments are
stored per-user and can be totalled ("current money").

## Tech stack

| Concern | Choice |
|---------|--------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Async | Kotlin coroutines (`suspend` + `Dispatchers.IO`) |
| Storage | JSON file in app-private storage + `SharedPreferences` |
| Build | Gradle (Kotlin DSL): `build.gradle.kts`, `app/build.gradle.kts` |
| Min entry | `LoginPage` activity (declared `LAUNCHER` in the manifest) |

## Entry point & navigation

- `AndroidManifest.xml` marks **`LoginPage`** as the launcher activity.
- `MainPage` is a second activity, started via an explicit `Intent` after a
  successful login.
- Navigation is **activity-to-activity** (`startActivity`), *not* Compose
  Navigation. There are only two screens.

```
LoginPage  --(login OK: startActivity)-->  MainPage
```

## Mental model

Two thin "store" classes wrap all persistence, and the two activities are pure
UI on top of them. There is no ViewModel layer yet — activities call the stores
directly inside coroutines. See [02-architecture.md](02-architecture.md).
