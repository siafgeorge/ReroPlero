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
| Async | Kotlin coroutines (`suspend`; Room does its own threading) |
| Storage | **Room** (SQLite) `reroplero.db` + `SharedPreferences` for the session |
| Build | Gradle (Kotlin DSL) + **KSP** (generates Room's code) |
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

The two activities are pure UI on top of a small **repository** layer.
`UserRepository` is an interface; `UserRepositoryImpl` implements it over a Room
DAO. `SessionRepository` remembers who's logged in. There is no ViewModel layer
yet — activities call the repositories directly inside coroutines. See
[02-architecture.md](02-architecture.md).

> **Storage was migrated from a hand-rolled `users.json` to Room in July 2026.**
> The migration is **not finished — the build is currently red.** Start at
> [06-current-state.md](06-current-state.md).
