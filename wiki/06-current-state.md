# 06 · Current state, gotchas & TODO

Snapshot of what's done vs. pending. **Update this page as work lands.**

## ⚠️ Build is RED — 9 errors, one root cause

`:app:compileDebugKotlin` fails. Every error is the same mistake in a different
place:

> `Interface 'UserRepository' does not have constructors.`

**You cannot instantiate an interface.** `UserRepository` is a contract;
`UserRepositoryImpl` is the class that fulfils it. Every `UserRepository(context)`
call must become `UserRepositoryImpl(context)`:

| File | Line | Fix |
|------|------|-----|
| `LoginPage.kt` | 87 | `remember { UserRepositoryImpl(context) }` |
| `SessionRepository.kt` | 26, 31, 37 | `UserRepositoryImpl(context)` |

The follow-on errors (`Unresolved reference 'register'` / `'addPayment'` /
`'getPayments'` / `'currentMoney'`) all disappear with them — the compiler
couldn't type the variable, so every method call on it failed too.

Note this is **only** about the `new`-ing. Using `UserRepository` as a *type* is
correct and is the whole point of the interface — `loginAction(…, userRepository:
UserRepository)` in `LoginPage.kt:122` is exactly right. Construct the impl, pass
the interface.

Note also that Kotlin compilation fails **before** KSP validates the DAO's SQL,
so expect a second round of errors from Room's annotation processor once these
clear.

## Not yet done (will bite once the above compile)

- **`AppDao.totalFor` returns `Double`, must be `Double?`** — `SELECT SUM(cost)`
  returns SQL `NULL` for a user with no payments. This crashes on the first
  freshly registered user. `UserRepositoryImpl.currentMoney` then coalesces:
  `?: 0.0`.
- **`AppDao`'s `@Insert`s have no `onConflict`**, so they default to `ABORT` and
  **throw** `SQLiteConstraintException` on a duplicate rather than returning
  `-1`. That makes `UserRepositoryImpl.addPayment`'s `!= -1L` check dead code.
  Add `@Insert(onConflict = OnConflictStrategy.IGNORE)`.
- **`Payment` has no `@ForeignKey` / `@Index`** — see
  [03-data-model.md](03-data-model.md). Deleting a user orphans their payments,
  and a re-registered username inherits the old user's history.
- **`withContext(Dispatchers.IO)` still wraps three `UserRepositoryImpl`
  methods** (`addPayment`, `currentMoney`, `getPayments`). Room's `suspend` DAO
  functions already run off the main thread; the wrapper is a pointless extra
  thread hop. See [05-conventions.md](05-conventions.md).
- **`SessionRepository.kt` still declares `class SessionStore`.** The file was
  renamed but the class wasn't. Legal Kotlin, confusing to read — rename the
  class (and the `MainPage` references) to match the file.
- **`UserRepositoryImpl.getUser` throws `NoSuchElementException`** on a miss,
  discarding the DAO's honest `User?`. Returning `User?` and letting the caller
  decide is friendlier.
- **`@Insert(entity = Payment::class)` on `insertPayment`** is redundant — Room
  infers the entity from the parameter type. Harmless, but noise.

## Works

- Register → login → `setCurrentUser` → `MainPage` opens.
- `MainPage` shows the running total, loaded in a `LaunchedEffect(Unit)`.
- FAB opens a `ModalBottomSheet` with `PaymentForm`; on Save it builds a
  `Payment` — **including the `username`** — and launches a coroutine to
  `addPay`, then re-reads the total.
- `AppDatabase` is correct: `@Database` annotation, `@Volatile` singleton,
  `applicationContext`, double-checked lock.

## Build setup notes (Room + AGP 9)

- Room 2.8.4, KSP `2.2.10-2.0.2` (the prefix must match the Kotlin version).
- `room-compiler` must be added with **`ksp(...)`**, not `implementation(...)`.
- `gradle.properties` carries `android.disallowKotlinSourceSets=false`. AGP 9's
  built-in Kotlin support rejects KSP's `kotlin.sourceSets` registration; this
  flag downgrades that error. Remove it once KSP supports `android.sourceSets`.
- **Do not add `androidx.room3`.** Room 3 is a separate namespace
  (`androidx.room3.*`); mixing it with Room 2 gives you two rival `@Entity` /
  `@PrimaryKey` annotations, and the IDE will happily auto-import the wrong one.
  Every Room import must read `androidx.room`.

## Known gotchas / smells (not blocking)

- **`globalSession`** is a top-level `lateinit var` in `MainPage.kt` — shared
  mutable global state (activity-leak / stale-data risk). A ViewModel is the
  real answer.
- **Plain-text passwords.** `jbcrypt` sits unused in the version catalog. Worth
  doing — but *after* the build is green, not mixed into it.
- **`CAMERA` permission** declared in the manifest but unused.
- `data/remote/` exists but is empty — placeholder for a future API layer.

## Natural next steps

1. **Get the build green** — the interface-instantiation fix, then the DAO
   round, rebuilding between them.
2. Uninstall the app from the emulator (clears the old `users.json`), then
   register a user and confirm a payment survives a restart.
3. **Transactions list** — the original feature request, still unbuilt: a
   horizontal button at the bottom of `MainPage` opening a `ModalBottomSheet`
   listing this account's payments (`globalSession.getPay()` in a `LazyColumn`).
4. Switch the DAO's reads to `Flow<List<Payment>>` / `Flow<Double?>` (drop
   `suspend`) and collect with `collectAsState()`. Room then re-emits on every
   table change, the total updates itself, and the manual
   `total = globalSession.curMon()` refresh after each save disappears.
5. Consider a `ViewModel` so state survives rotation and `globalSession` can go.
6. Add a logout action (`clear()`).
