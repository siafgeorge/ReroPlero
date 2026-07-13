# 06 · Current state, gotchas & TODO

Snapshot of what's done vs. pending. **Update this page as work lands.**

## Works

- Register a user → appended to `users.json`.
- Login → validates credentials, stores current user in `SessionStore`, opens
  `MainPage`.
- Data layer for payments is complete and correct:
  - `UserStore.addPayment` (writes the whole array back — fixed),
  - `UserStore.getPayments` → `List<Payment>`,
  - `UserStore.currentMoney` → `sumOf { it.cost }`,
  - `SessionStore.addPay` / `getPay` facades.
- `Payment` data class + `plus` operator compile and work.

## Pending / not yet wired

1. **Payment save is not connected in the UI.** `MainPage`'s `onSave` still
   calls `createPayment(payment)`, which only `println`s — the payment is
   discarded. Fix: in `setContent`, add `val scope = rememberCoroutineScope()`
   and change `onSave` to `scope.launch { session.addPay(payment) }`, then
   delete `createPayment`. Requires imports `rememberCoroutineScope` and
   `kotlinx.coroutines.launch`.
2. **No UI shows the data.** `MainPage` shows a placeholder `Text("Main page")`.
   Nothing displays `currentMoney(username)` or the payment list yet. Load it in
   a `LaunchedEffect` and render.

## Known gotchas / smells (not blocking)

- **`globalSession`** is a top-level `lateinit var` in `MainPage.kt`. It works
  but is shared mutable global state (activity-leak / stale-data risk). Prefer a
  local `session` (or a ViewModel) over a global.
- **Redundant elvis in `currentMoney`** (if still present):
  `getPayments(...) ?: return@withContext -1.0` is dead code — `getPayments`
  returns a non-nullable `List`. Harmless yellow warning; can be removed.
- **Plain-text passwords** in `users.json`. Fine for learning; real fix = hash
  (e.g. salted hash) and never store the raw password.
- **`CAMERA` permission** is declared in the manifest but unused.

## Natural next steps

1. Wire the save (item 1 above) and run the app to confirm a payment lands in
   `users.json`.
2. Display the running total + payment list on `MainPage`.
3. Consider a `ViewModel` + `StateFlow` so the total is observable and survives
   rotation.
4. Add a logout action (`SessionStore.clear()`).
