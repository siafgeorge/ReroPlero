# 07 · Glossary

Concepts used in this project, explained in ReroPlero's terms.

## Coroutines & threading

- **`suspend`** — marks a function that can pause/resume; callable only from a
  coroutine or another suspend function. Does **not** change threads by itself.
- **Coroutine** — a cancellable unit of async work. Started in the UI with
  `rememberCoroutineScope()` + `scope.launch { }`.
- **Dispatcher** — decides which thread a coroutine runs on:
  - `Dispatchers.Main` — the UI thread.
  - `Dispatchers.IO` — background pool for **blocking I/O** (files, network). Used
    for all `users.json` access.
  - `Dispatchers.Default` — background pool for **CPU-heavy** work. Not used yet.
  - `Dispatchers.Unconfined` — advanced/rare.
- **`withContext(dispatcher) { }`** — runs the block on that dispatcher's thread
  and switches back when done. The thing that actually moves work off the main
  thread.

## Kotlin language

- **`data class`** — auto-generates `equals`, `hashCode`, `toString`, `copy`.
  `Payment` is one.
- **Operator overloading** — `operator fun plus(other)` makes `a + b` work.
  Return the same type to allow chaining; `Payment.plus` returns `Double`, so it
  doesn't chain. (Term is *overloading*, not *overriding*.)
- **`by lazy { }`** — property initialized on first access, not at construction.
  Useful to defer building a Context-dependent object until `onCreate`.
- **`?:` (elvis)** — `a ?: b` = "a, or b if a is null". No-op on non-nullable
  types (dead code / warning).
- **`sumOf { }`** — sums a numeric selector over a collection; `0.0` on empty.
- **`mutableListOf<T>()` vs `emptyList<T>()`** — growable vs immutable-empty.

## Android

- **Activity** — a screen. `LoginPage`, `MainPage`. An Activity **is** a
  `Context`.
- **Context** — handle to app resources/storage. In a composable use
  `LocalContext.current`; in an Activity use `this`. Not available in a field
  initializer (constructor runs before it's attached).
- **`SharedPreferences`** — tiny key-value store for small data (the session's
  current username). `apply()` writes async and is main-thread safe.
- **`filesDir`** — the app's private files directory; home of `users.json`.

## Jetpack Compose

- **`@Composable`** — a UI-building function; re-runs ("recomposes") when its
  state changes.
- **`remember { }` / `mutableStateOf(...)`** — hold state across recompositions;
  writing to the state triggers recomposition.
- **`rememberCoroutineScope()`** — a coroutine scope tied to the composition, for
  launching suspend work from event handlers.
- **`ModalBottomSheet`** — the sheet that hosts `PaymentForm`.
- **`LaunchedEffect(key)`** — run suspend work when a composable enters the
  composition (e.g. to load the current total). Not used yet; see
  [06-current-state.md](06-current-state.md).
