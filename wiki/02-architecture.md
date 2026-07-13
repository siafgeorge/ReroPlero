# 02 · Architecture

## Layers

```
┌────────────────────────── UI (activities + Compose) ──────────────────────────┐
│  LoginPage            MainPage                                                  │
│   - LoginFields()      - PaymentForm()                                          │
│   - loginAction()      - (payment save flow)                                    │
└───────────────┬───────────────────────────────┬───────────────────────────────┘
                │ calls                          │ calls
                ▼                                 ▼
┌────────────────────── data layer (package .data) ─────────────────────────────┐
│  SessionRepository ── "who is logged in?"  (SharedPreferences)                 │
│       │  addPay()/getPay()/curMon() are facades that resolve the current       │
│       │  user and delegate to the repository                                   │
│       ▼                                                                        │
│  UserRepository      ── interface: the CONTRACT (what the UI depends on)      │
│  UserRepositoryImpl  ── class: the Room-backed implementation                 │
└───────────────────────────────────┬───────────────────────────────────────────┘
                                    │
┌──────────────── .data.local (Room) ▼ ─────────────────────────────────────────┐
│  AppDao        ── @Dao interface: the SQL. KSP generates the implementation.  │
│  AppDatabase   ── @Database singleton; hands out the AppDao                   │
│  models/User, models/Payment ── the @Entity models (one table each)           │
└────────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
                app-private:  reroplero.db (SQLite)  +  SharedPreferences("session")
```

`.data.remote/` exists but is empty — a placeholder for a future API layer.

## Responsibilities

- **`AppDao`** is the SQL. You write signatures + `@Query` strings; KSP writes
  the implementations and validates the queries against the schema at build time.
- **`UserRepository`** is an **interface** — the contract the UI codes against.
  **`UserRepositoryImpl`** is the class that fulfils it using Room. This is the
  repository pattern: the UI depends on the abstraction, so the storage
  implementation can be swapped (a fake for tests, a remote impl later) without
  touching a single composable.
  ⚠️ You can only *construct* the impl. `UserRepository(context)` is a compile
  error — see [06-current-state.md](06-current-state.md).
- The repository is where logic that **isn't a query** lives: rejecting blank
  credentials, coalescing a null `SUM` to `0.0`. It knows nothing about *who* is
  logged in — you always pass a `username`.
- **`SessionRepository`** owns the "current user" concept via
  `SharedPreferences`. Its `addPay` / `getPay` / `curMon` methods read the
  current username, construct a repository, and delegate. This lets the UI say
  "save this payment for whoever is logged in" without threading a username
  around.
- **Activities** are UI only. They obtain a repository, launch a coroutine, and
  call a `suspend` method.

Because the session facade's signatures never changed, the JSON → Room swap was
invisible to the UI. That's the payoff of this layering.

## Runtime flow: recording a payment

```
User taps + (FAB)  ->  ModalBottomSheet shows PaymentForm
User fills form, taps Save
   -> onSave(category, cost, timeMillis)
      -> build Payment(id=UUID, username, category, cost, timestamp)
         // the owner is stamped HERE, at construction — the repository
         // just inserts whatever Payment it is handed
      -> scope.launch { globalSession.addPay(payment) }   // coroutine (suspend)
            -> SessionRepository.addPay: username = currentUser()
                 -> UserRepositoryImpl.addPayment(payment)
                      -> AppDao.insertPayment  → INSERT INTO payments …
      -> total = globalSession.curMon()                   // re-read, manual
            -> UserRepositoryImpl.currentMoney → AppDao.totalFor
                 → SELECT SUM(cost) FROM payments WHERE username = …
```

Note the total is re-read **by hand** after each save. Switching the DAO's reads
to `Flow` would make Room re-emit automatically and delete that step — see
[06-current-state.md](06-current-state.md).

## Why there's no ViewModel (yet)

This is a small learning app; activities call repositories directly. A ViewModel
+ `StateFlow` would be the natural next step to survive configuration changes,
hold the "current total" as observable state, and retire the `globalSession`
global. Not present today — see [06-current-state.md](06-current-state.md).

## Why an interface + an Impl?

`UserRepository` (interface) and `UserRepositoryImpl` (class) look like
duplication for a two-screen app, and for now they nearly are. What they buy:

- The UI depends on a **contract**, not on Room. Swap the implementation and no
  composable changes.
- Tests can pass a fake `UserRepository` with no database at all.
- The `data/local` vs `data/remote` split anticipates a server-backed impl.

The cost is one extra file and the `override` keyword on every member. Worth it
as a learning exercise; be aware it's more structure than a 2-screen app strictly
needs.
