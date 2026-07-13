# 04 · File reference

Source root: `app/src/main/java/com/example/reroplero/`

```
com/example/reroplero/
├── LoginPage.kt              launcher activity
├── MainPage.kt               second activity
├── data/
│   ├── UserRepository.kt     interface — the contract
│   ├── UserRepositoryImpl.kt class — the Room-backed implementation
│   ├── SessionRepository.kt  who is logged in (SharedPreferences)
│   ├── local/
│   │   ├── AppDao.kt         @Dao — the SQL
│   │   ├── AppDatabase.kt    @Database — the singleton
│   │   └── models/
│   │       ├── User.kt       @Entity → users table
│   │       └── Payment.kt    @Entity → payments table
│   └── remote/               (empty — placeholder for a future API layer)
└── ui/theme/                 Color.kt, Theme.kt, Type.kt (generated)
```

Build status: see [06-current-state.md](06-current-state.md).

## `data/UserRepository.kt` — the interface
The **contract**: 11 `suspend` functions, no implementation. Callers depend on
this type, never on the concrete class.

This is what lets `loginAction(…, userRepository: UserRepository)` be written
against an abstraction — you could swap in a fake for testing, or a
network-backed impl later, without touching the UI.

⚠️ **You cannot construct an interface.** `UserRepository(context)` is an error;
construct `UserRepositoryImpl(context)` and pass it around as a `UserRepository`.

## `data/UserRepositoryImpl.kt` — the implementation
`class UserRepositoryImpl(private val context: Context) : UserRepository`.
Holds `private val dao = AppDatabase.getInstance(context).dao()`. Every member
carries `override` (mandatory in Kotlin, unlike Java's optional `@Override`).

| Member | Purpose |
|--------|---------|
| `register(username, password)` | Rejects blanks, then inserts. False if the username is taken. |
| `checkCredentials(username, password)` | Delegates to `dao.checkCreds`. |
| `addPayment(payment)` | Inserts a **fully-populated** `Payment` — the caller sets `username`. |
| `currentMoney(username)` | `dao.totalFor(username)` — the **database** sums, not Kotlin. |
| `getPayments(username)` | Delegates to the DAO. |
| `userExists` / `getUser` / `deleteUser` / `deletePayment*` | Straight delegation. |

The repository is where logic that *isn't* a query belongs: rejecting blank
credentials, coalescing a null `SUM`. Queries belong in the DAO.

## `data/local/AppDao.kt`
The `@Dao` interface. You declare signatures + SQL; KSP writes the
implementations at build time and validates every query against the schema.

| Member | Kind | Purpose |
|--------|------|---------|
| `userExists(username)` | `@Query` | `SELECT EXISTS(…)` → `Boolean`. |
| `getUser(username)` | `@Query` | `User?` — nullable, a lookup can miss. |
| `insertUser(user)` | `@Insert` | Returns `Long` rowid; `-1` **only** under `onConflict = IGNORE`. |
| `checkCreds(username, password)` | `@Query` | `SELECT EXISTS(…)` → `Boolean`. |
| `insertPayment(payment)` | `@Insert` | Same contract as `insertUser`. |
| `getPayments(username)` | `@Query` | `List<Payment>` for one user. |
| `totalFor(username)` | `@Query` | `SELECT SUM(cost)` → must be **`Double?`** (NULL when no payments). |
| `delUser(user)` / `deletePayment(payment)` | `@Delete` | Match on **primary key only**; other fields ignored. Return `Int` (rows affected) or `Unit` — never `Long`. |
| `deletePaymentById(id)` / `deleteAllPaymentsForUser(username)` | `@Query` | Explicit `DELETE` statements. Safer than `@Delete` — they say exactly what they match. |

Every function is `suspend`, which is what lets Room enforce off-main-thread
execution itself.

## `data/local/AppDatabase.kt`
The Room database: entity list, schema version, and the singleton.

| Member | Kind | Purpose |
|--------|------|---------|
| `@Database(entities = [User, Payment], version = 1)` | annotation | Registers the tables. Without it KSP generates nothing. |
| `dao()` | abstract, **no body** | KSP generates `AppDatabase_Impl`, which returns a real `AppDao`. This is where the DAO *interface* becomes a callable object. |
| `getInstance(context)` | companion object | Double-checked-locked singleton (`@Volatile`). Uses `context.applicationContext` (never the Activity — that would leak it) and the file `reroplero.db`. |

Opening a database is expensive, so it must be opened **once per process**.
`SessionRepository` constructs a repository on every call; the singleton is what
makes that cheap.

## `data/local/models/User.kt`
The `User` entity: `username` (`@PrimaryKey`) + `password`. See
[03-data-model.md](03-data-model.md).

## `data/local/models/Payment.kt`
The `Payment` entity + `plus` operator. Carries a `username` column — the owner.
See [03-data-model.md](03-data-model.md).

## `data/SessionRepository.kt`
Owns the "current user" via `SharedPreferences("session")`. **Unchanged by the
Room migration** — it stores one small value, which is what prefs are for.

⚠️ The file is named `SessionRepository.kt` but still declares
`class SessionStore`. Legal, confusing; rename pending (see 06).

| Member | Kind | Purpose |
|--------|------|---------|
| `setCurrentUser(username)` | plain | Persist the logged-in username. |
| `currentUser()` | plain | `String?` — current username or null. |
| `clear()` | plain | Logout: remove the stored username. |
| `addPay(payment)` | suspend | Facade: resolves `currentUser()`, delegates to the repository. |
| `getPay()` | suspend | Facade: same, for the payment list. |
| `curMon()` | suspend | Facade: same, for the total. |

These facades are why the storage swap stayed invisible to the UI: the
signatures never moved.

## `LoginPage.kt` — launcher activity
- `LoginFields()` composable: username / password fields, "Register user" text,
  Login button. Uses `LocalContext.current` and `rememberCoroutineScope()`.
- `loginAction(context, username, password, userRepository)`: checks credentials;
  on success stores the current user, then
  `startActivity(Intent(context, MainPage::class))`. Note it takes the
  **interface** as its parameter type — textbook use of the abstraction.

## `MainPage.kt` — second activity
- Assigns the top-level `globalSession` **inside `onCreate`** (not a field
  initializer — the Context isn't attached yet; see
  [05-conventions.md](05-conventions.md)).
- Reads `currentUser()`; bails with `?: return` if nobody is logged in.
- Loads the total in `LaunchedEffect(Unit) { total = globalSession.curMon() }`.
- FAB opens a `ModalBottomSheet` containing `PaymentForm`; on save it launches a
  coroutine, calls `addPay`, and re-reads the total.
- `PaymentForm(onSave)`: category chips, cost field, date + time pickers; builds
  a `Payment` with a fresh `UUID` **and the current username**, then invokes
  `onSave`.

## `ui/theme/` — `Color.kt`, `Theme.kt`, `Type.kt`
Standard Compose Material 3 theme scaffolding (generated). Rarely edited.

## Non-source
- `AndroidManifest.xml` — declares `LoginPage` (LAUNCHER) and `MainPage`; requests `CAMERA` permission (unused so far).
- `gradle/libs.versions.toml` — version catalog. Room + KSP live here.
- `gradle.properties` — carries the AGP-9/KSP compatibility flag (see 06).
- `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts` — Gradle build.
