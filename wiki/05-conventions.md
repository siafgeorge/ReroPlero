# 05 · Conventions & patterns

Patterns this codebase follows. Match them when adding code.

## Threading: Room handles it — don't wrap DAO calls

**Do not put `withContext(Dispatchers.IO)` around a DAO call.** Room's `suspend`
DAO functions already run off the main thread; wrapping them adds a pointless
second thread hop and forces the clumsy `return@withContext` label.

```kotlin
// yes
override suspend fun currentMoney(username: String): Double =
    dao.totalFor(username) ?: 0.0

// no — redundant wrapper
override suspend fun currentMoney(username: String): Double = withContext(Dispatchers.IO) {
    return@withContext dao.totalFor(username) ?: 0.0
}
```

- `suspend` = callable only from a coroutine; by itself it does **not** change
  threads. Room's generated code is what moves the work.
- `withContext(Dispatchers.IO)` is still the right tool for *non-Room* blocking
  I/O (raw files, network). There just isn't any left in this app.
- The pre-Room code wrapped everything, because reading `users.json` was a
  plain blocking call. That's history now — see
  [06-current-state.md](06-current-state.md) for wrappers still to remove.

## Let the database do the work

Prefer a query over loading rows and computing in Kotlin.

```kotlin
// yes — SQLite sums; one number crosses the boundary
@Query("SELECT SUM(cost) FROM payments WHERE username = :username")
suspend fun totalFor(username: String): Double?

// no — loads every Payment into memory just to add them up
suspend fun currentMoney(u: String) = getPayments(u).sumOf { it.cost }
```

Remember `SUM` returns SQL `NULL` (not `0`) on no rows — hence `Double?` and
`?: 0.0` at the call site.

## Calling suspend code from the UI

Compose/Activities are on the main thread. To call a suspend store method:

```kotlin
val scope = rememberCoroutineScope()      // in a @Composable
...
scope.launch { store.suspendMethod(...) } // launch = the coroutine builder
```

## Getting a `Context`

| Where you are | Use |
|---------------|-----|
| Inside a `@Composable` | `LocalContext.current` |
| Inside an `Activity` method (e.g. `onCreate`) | `this` |

⚠️ **Do not** build a Context-using object in an Activity **field initializer**
(`private val x = Foo(this)`). Field initializers run in the constructor, before
Android attaches the base Context, so `getSharedPreferences` / file access there
crashes with an NPE. Construct such objects **inside `onCreate`** (or use
`by lazy { Foo(this) }`).

## Room access

- Every Room import reads **`androidx.room`**. If the IDE offers you
  `androidx.room3.*`, that's Room 3 — a different library. Decline it.
- DAO functions are `suspend` and named for what they do; the SQL lives in the
  `@Query` string next to them.
- Insert returns a rowid (`Long`, `-1` when ignored); delete/update return a row
  count (`Int`). See [03-data-model.md](03-data-model.md) for the full contract.
- Nullable returns are meaningful: `User?` = the lookup can miss, `Double?` =
  `SUM` over zero rows. Coalesce at the repository boundary so the UI gets
  non-null values.
- Getting a DAO: `AppDatabase.getInstance(context).dao()`. Never construct the
  database directly — the singleton exists so the app opens it once.
- Room stays behind the repository. Nothing above `data/` should import
  `androidx.room` or touch an `AppDao`.

## Kotlin collections

- Empty growable list: `mutableListOf<T>()`; fill with `.add(...)`.
- Empty immutable "nothing to return": `emptyList()`.
- Summing in Kotlin: `list.sumOf { it.cost }` (returns `0.0` on empty — safe;
  prefer it over `reduce`, which **throws** on an empty list). But for data that
  lives in the database, prefer a `SUM` query — see above.

## Naming / package layout

```
data/            repositories — the contract + the impl (what callers use)
data/local/      Room: AppDao, AppDatabase
data/local/models/   the @Entity classes
data/remote/     (empty — future API layer)
(root)           UI activities + composables
```

- Keep the data layer free of UI imports.
- **Depend on the interface, construct the impl.** Pass `UserRepository` around
  as a type; only ever call `UserRepositoryImpl(context)` at the point where an
  object is actually created. Constructing the interface is a compile error.
- `override` is **mandatory** in Kotlin on every member that implements an
  interface function — unlike Java, where `@Override` is optional.
- Don't write `public`. It's the default in Kotlin.
