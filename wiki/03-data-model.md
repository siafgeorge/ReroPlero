# 03 · Data model & storage

## Where data lives

| Data | Mechanism | Location |
|------|-----------|----------|
| Users + their payments | JSON file `users.json` | `context.filesDir` (app-private) |
| Current logged-in user | `SharedPreferences` named `"session"`, key `current_username` | app-private prefs |

Both are app-private: only ReroPlero can read them, and they're wiped on
uninstall.

## `users.json` schema

A single top-level **array of user objects**. Each user's payments are nested
**inside** that user's object under a `payments` array:

```json
[
  {
    "username": "george",
    "password": "1234",
    "payments": [
      { "id": "a1b2-uuid", "category": "Food", "cost": 12.5, "timestamp": 1720000000000 }
    ]
  }
]
```

Notes:
- `payments` is **absent** until the user records their first payment. Readers
  must use `optJSONArray("payments")` (returns `null` if missing), *not*
  `getJSONArray` (throws). See [05-conventions.md](05-conventions.md).
- `timestamp` is epoch **milliseconds** (`Long`), combining the chosen date +
  time. Formatted for display with `SimpleDateFormat`.
- `cost` is a JSON number read back as `Double`.
- `password` is stored in **plain text** — a known limitation, fine for
  learning, first thing to fix for real use.

## The `Payment` model

`data/Payment.kt`:

```kotlin
data class Payment(
    val id: String,        // UUID string, generated at creation
    val category: String,  // one of: Food, Transport, Rent, Fun
    val cost: Double,
    val timestamp: Long    // epoch millis
) {
    operator fun plus(other: Payment): Double = this.cost + other.cost
}
```

- It's a `data class` → free `copy()`, `equals()`, `toString()`.
- The `plus` **operator** returns a `Double` (sum of the two costs). Because it
  returns `Double`, it does **not** chain (`a + b + c` won't compile). To total
  a *list*, use `list.sumOf { it.cost }`, not `plus`.

## Conversion boundary

JSON ⇄ objects happens only in `UserStore`:
- **Write:** `addPayment` builds a `JSONObject` from a `Payment`.
- **Read:** `getPayments` builds `Payment` objects from JSON and returns a typed
  `List<Payment>`. Everything above the data layer works with `Payment`, never
  raw JSON.
