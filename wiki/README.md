# ReroPlero Wiki

Reference documentation for the ReroPlero Android app. This folder is the
single place to understand *how the app is built* without re-reading every
source file. Keep it in sync when the architecture or data model changes.

## Index

| Page | What's in it |
|------|--------------|
| [01-overview.md](01-overview.md) | What the app is, tech stack, entry point |
| [02-architecture.md](02-architecture.md) | Layers (UI + repository + Room), the runtime flow, why interface+Impl |
| [03-data-model.md](03-data-model.md) | Room schema (`users` + `payments`), the entities, DAO contracts |
| [04-file-reference.md](04-file-reference.md) | Every `.kt` file, its responsibility, and key functions |
| [05-conventions.md](05-conventions.md) | Coroutine / threading / Compose / Room patterns used here |
| [06-current-state.md](06-current-state.md) | What works, what's pending, known gotchas & TODOs |
| [07-glossary.md](07-glossary.md) | Kotlin / Android / Compose / Room concepts, in this app's terms |

## Quick facts

- **Package:** `com.example.reroplero`
- **Language / UI:** Kotlin + Jetpack Compose (Material 3)
- **Launcher activity:** `LoginPage`
- **Persistence:** Room / SQLite (`reroplero.db`) + `SharedPreferences` (session)
- **Data layer:** repository pattern — `UserRepository` (interface) +
  `UserRepositoryImpl` (Room-backed class)
- **No network, no ViewModel, no external DI** — deliberately simple (learning project)

> ⚠️ **The Room migration is in progress and the build is currently red.**
> See [06-current-state.md](06-current-state.md) for the exact error list.

## How to keep this current

When you change the data model, add a repository method, or wire a new screen,
update **03**, **04**, and **06** in the same change. The rest move rarely.
