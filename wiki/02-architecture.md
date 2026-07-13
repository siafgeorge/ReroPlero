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
┌────────────────────────── data layer (package .data) ─────────────────────────┐
│  SessionStore  ── "who is logged in?"  (SharedPreferences)                     │
│       │  addPay()/getPay() are convenience facades that resolve the current    │
│       │  user and delegate to UserStore                                        │
│       ▼                                                                        │
│  UserStore     ── reads/writes users.json  (the source of truth)              │
│  Payment       ── the data model (data class + `plus` operator)               │
└────────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
                       app-private files:  users.json  +  SharedPreferences("session")
```

## Responsibilities

- **`UserStore`** owns `users.json`. Every read/write of users and their
  payments goes through here. It knows nothing about *who* is logged in — you
  always pass a `username`.
- **`SessionStore`** owns the "current user" concept via `SharedPreferences`.
  Its `addPay` / `getPay` methods are a convenience layer: they read the current
  username, construct a `UserStore`, and delegate. This lets the UI say
  "save this payment for whoever is logged in" without threading a username
  around.
- **Activities** are UI only. They obtain a store, launch a coroutine, and call
  a `suspend` method.

## Runtime flow: recording a payment

```
User taps + (FAB)  ->  ModalBottomSheet shows PaymentForm
User fills form, taps Save
   -> onSave(category, cost, timeMillis)
      -> build Payment(id=UUID, category, cost, timestamp)
      -> scope.launch { session.addPay(payment) }        // coroutine (suspend)
            -> SessionStore.addPay: username = currentUser()
                 -> UserStore.addPayment(username, payment)
                      -> withContext(IO) { read users.json, append, write back }
```

## Why there's no ViewModel (yet)

This is a small learning app; activities call stores directly. A ViewModel +
`StateFlow` would be the natural next step to survive configuration changes and
hold the "current total" as observable state. Not present today — see
[06-current-state.md](06-current-state.md).
