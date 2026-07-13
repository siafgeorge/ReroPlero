# 04 · File reference

Source root: `app/src/main/java/com/example/reroplero/`

## `data/UserStore.kt`
Owns `users.json`. Constructed with a `Context` (`File(context.filesDir, "users.json")`).

| Member | Kind | Purpose |
|--------|------|---------|
| `readAll()` | private, **not** suspend | Read file → `JSONArray` (empty if file missing). Must be called *inside* an IO block — callers are already on `Dispatchers.IO`. |
| `userExists(username)` | suspend | True if a user with that name exists. |
| `register(username, password)` | suspend | Appends a new user. False if blank or already exists. |
| `checkCredentials(username, password)` | suspend | True if a stored user matches. |
| `addPayment(username, payment)` | suspend | Finds the user, appends the payment to their `payments` array, **writes the whole array back**. Returns false if user not found. |
| `getPayments(username)` | suspend | Returns `List<Payment>` for the user (empty list if none / not found). |
| `currentMoney(username)` | suspend | `getPayments(username).sumOf { it.cost }`. |

⚠️ In `addPayment`, the final write must be `file.writeText(users.toString())`
(the whole array) — writing the single `user` object corrupts the file. This
was a real bug during development.

## `data/SessionStore.kt`
Owns the "current user" via `SharedPreferences("session")`. Constructed with a `Context`.

| Member | Kind | Purpose |
|--------|------|---------|
| `setCurrentUser(username)` | plain | Persist the logged-in username. |
| `currentUser()` | plain | `String?` — current username or null. |
| `clear()` | plain | Logout: remove the stored username. |
| `addPay(payment)` | suspend | Facade: resolves `currentUser()`, delegates to `UserStore.addPayment`. |
| `getPay()` | suspend | Facade: resolves `currentUser()`, delegates to `UserStore.getPayments`. |

Session prefs reads/writes are cheap and main-thread-safe (no coroutine needed);
`addPay`/`getPay` are suspend only because the `UserStore` methods they call are.

## `data/Payment.kt`
The `Payment` data class + `plus` operator. See [03-data-model.md](03-data-model.md).

## `LoginPage.kt` — launcher activity
- `LoginFields()` composable: username / password fields, "Register user" text,
  Login button. Uses `LocalContext.current` for a Context and
  `rememberCoroutineScope()` to call suspend store methods.
- `loginAction(context, username, password, userStore)`: checks credentials; on
  success calls `SessionStore(context).setCurrentUser(username)` then
  `startActivity(Intent(context, MainPage::class))`.

## `MainPage.kt` — second activity
- Builds `SessionStore(this)` **inside `onCreate`** (not a field initializer —
  the Context isn't attached yet during construction; see
  [05-conventions.md](05-conventions.md)).
- Reads `currentUser()`; bails with `?: return` if nobody is logged in.
- FAB opens a `ModalBottomSheet` containing `PaymentForm`.
- `PaymentForm(onSave)` composable: category chips, cost field, date + time
  pickers; on Save builds a `Payment` and invokes `onSave`.

## `ui/theme/` — `Color.kt`, `Theme.kt`, `Type.kt`
Standard Compose Material 3 theme scaffolding (generated). Rarely edited.

## Non-source
- `AndroidManifest.xml` — declares `LoginPage` (LAUNCHER) and `MainPage`; requests `CAMERA` permission (unused so far).
- `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts` — Gradle build.
