# 05 · Conventions & patterns

Patterns this codebase follows. Match them when adding code.

## Threading: all disk I/O off the main thread

Every store method that touches the file or prefs file wraps its work in:

```kotlin
suspend fun something(...) = withContext(Dispatchers.IO) { ... }
```

- `suspend` = callable only from a coroutine; it does **not** by itself change
  threads.
- `withContext(Dispatchers.IO)` = the part that actually moves work to a
  background (IO) thread and returns when done. This is what keeps the UI from
  freezing.
- Private helpers like `readAll()` are **not** suspend; they rely on their
  callers already being inside a `withContext(IO)` block.
- A method that only *calls* other suspend methods (e.g. `currentMoney` calling
  `getPayments`) doesn't need its own `withContext` — just `suspend`.

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

## JSON access

- Use `optJSONArray("key")` / `optJSONObject("key")` when a key may be absent —
  they return `null` instead of throwing. Pattern: `?: JSONArray()` (create) or
  `?: return@withContext emptyList()` (bail).
- Use `getString` / `getDouble` / `getLong` to read **known** fields by key.
  JSON objects are keyed by name — there is no positional `[i][1]` indexing.
- Mutating a `JSONObject` you pulled out of a `JSONArray` mutates it in place;
  write the **whole array** back to persist.

## Kotlin collections

- Empty growable list: `mutableListOf<T>()`; fill with `.add(...)`.
- Empty immutable "nothing to return": `emptyList()`.
- Summing: `list.sumOf { it.cost }` (returns `0.0` on empty — safe). Prefer this
  over `reduce`, which **throws** on an empty list.

## Naming / package layout

- Data/model/persistence → package `com.example.reroplero.data`.
- UI activities + composables → package `com.example.reroplero`.
- Keep the data layer free of UI imports (the model lives in `.data`, so
  `UserStore` doesn't reach into an activity file for `Payment`).
