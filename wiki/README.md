# ReroPlero Wiki

Reference documentation for the ReroPlero Android app. This folder is the
single place to understand *how the app is built* without re-reading every
source file. Keep it in sync when the architecture or data model changes.

## Index

| Page | What's in it |
|------|--------------|
| [01-overview.md](01-overview.md) | What the app is, tech stack, entry point |
| [02-architecture.md](02-architecture.md) | Layers (UI activities + data layer) and the runtime flow |
| [03-data-model.md](03-data-model.md) | `users.json` schema, the `Payment` model, storage design |
| [04-file-reference.md](04-file-reference.md) | Every `.kt` file, its responsibility, and key functions |
| [05-conventions.md](05-conventions.md) | Coroutine / threading / Compose / JSON patterns used here |
| [06-current-state.md](06-current-state.md) | What works, what's pending, known gotchas & TODOs |
| [07-glossary.md](07-glossary.md) | Kotlin / Android / Compose concepts, explained in this app's terms |

## Quick facts

- **Package:** `com.example.reroplero`
- **Language / UI:** Kotlin + Jetpack Compose (Material 3)
- **Launcher activity:** `LoginPage`
- **Persistence:** plain JSON file (`users.json`) + `SharedPreferences` (session)
- **No database, no network, no external DI** — deliberately simple (learning project)

## How to keep this current

When you change the data model, add a store method, or wire a new screen,
update **03**, **04**, and **06** in the same change. The rest move rarely.
