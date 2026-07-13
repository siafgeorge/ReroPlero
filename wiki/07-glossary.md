# 07 · Glossary

Concepts used in this project, explained in ReroPlero's terms.

## Coroutines & threading

- **`suspend`** — marks a function that can pause/resume; callable only from a
  coroutine or another suspend function. Does **not** change threads by itself.
- **Coroutine** — a cancellable unit of async work. Started in the UI with
  `rememberCoroutineScope()` + `scope.launch { }`.
- **Dispatcher** — decides which thread a coroutine runs on:
  - `Dispatchers.Main` — the UI thread.
  - `Dispatchers.IO` — background pool for **blocking I/O** (files, network).
    Room's `suspend` DAO functions handle this themselves — **do not** wrap them
    in `withContext(IO)`.
  - `Dispatchers.Default` — background pool for **CPU-heavy** work. Not used yet.
  - `Dispatchers.Unconfined` — advanced/rare.
- **`withContext(dispatcher) { }`** — runs the block on that dispatcher's thread
  and switches back when done. The thing that actually moves work off the main
  thread.

## Room (the database layer)

- **Room** — Google's ORM over SQLite. You declare tables and queries with
  annotations; **KSP generates the implementation code at build time**, and
  validates your SQL against your schema while compiling.
- **KSP** (Kotlin Symbol Processing) — the annotation processor that does that
  generation. `room-compiler` must be wired with `ksp(...)`, not
  `implementation(...)`, or nothing is generated.
- **`@Entity`** — "this data class is a table." Only **constructor** properties
  become columns; anything in the class body is ignored.
- **`@PrimaryKey`** — the unique identifier for a row. Every entity needs one.
  It's a real database-level guarantee: SQLite rejects a duplicate, so you don't
  have to check first.
- **`@Dao`** (Data Access Object) — an **interface** listing your queries. It's a
  contract, not an object — you can't call its functions until `AppDatabase`
  hands you an instance.
- **`@Query` / `@Insert` / `@Delete`** — how each DAO function is implemented.
  `@Insert` returns a rowid (`Long`); `@Delete` returns a row count (`Int`) and
  matches on the **primary key alone**.
- **`@ForeignKey` / CASCADE** — links `payments.username` back to `users`.
  `onDelete = CASCADE` deletes a user's payments along with the user. This is
  what replaces JSON's nesting.
- **`OnConflictStrategy`** — what an insert does when the primary key already
  exists. `ABORT` (the default) **throws**; `IGNORE` skips silently and returns
  `-1`. Pick deliberately.
- **`Flow<T>`** — a stream of values over time. A DAO function returning
  `Flow<List<Payment>>` (no `suspend`) re-emits **automatically** whenever the
  table changes. Not used yet; see [06-current-state.md](06-current-state.md).

## Architecture

- **Repository pattern** — the UI talks to a `UserRepository` **interface** and
  never knows Room exists. `UserRepositoryImpl` is the class behind it. Swap the
  impl (a fake for tests, a network-backed one later) and no composable changes.
- **Interface vs. class** — an interface is a **contract**: signatures, no
  bodies, **no constructor**. `UserRepository(context)` is a compile error;
  `UserRepositoryImpl(context)` is how you get an object. Depend on the
  interface, construct the impl.
- **`override`** — **mandatory** in Kotlin on every member implementing an
  interface function (Java's `@Override` is optional; Kotlin's isn't). Omit it
  and you get *"needs 'override' modifier"*.

## Kotlin language

- **`data class`** — auto-generates `equals`, `hashCode`, `toString`, `copy`.
  `Payment` is one.
- **`companion object`** — Kotlin's replacement for `static`. Things inside it
  belong to the **class**, not to an instance, so you call them on the type name:
  `AppDatabase.getInstance(...)`. That's essential here — you need the function
  *before* an instance exists. Its `INSTANCE` field is the single shared
  database for the whole process.
- **`@Volatile`** — forces every thread to read the latest write of a field
  instead of a cached copy. Guards the singleton's double-checked lock.
- **`synchronized(this) { }`** — only one thread at a time inside the block.
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
  current username). `apply()` writes async and is main-thread safe. Still the
  right tool for the session — Room would be overkill for one string.
- **`applicationContext`** — the process-lifetime Context. Hand this (never an
  Activity) to anything long-lived like the database, or the Activity can't be
  garbage collected.
- **`filesDir`** — the app's private files directory. Former home of
  `users.json`; the Room database lives next door in `databases/`.

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
