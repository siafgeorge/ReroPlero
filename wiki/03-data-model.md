# 03 · Data model & storage

> **Migrated from `users.json` to Room (SQLite) in July 2026.** The old JSON
> file is gone; there is no importer. Uninstall the app to clear stale data.

## Where data lives

| Data | Mechanism | Location |
|------|-----------|----------|
| Users + their payments | **Room database** `reroplero.db` | app-private `databases/` dir |
| Current logged-in user | `SharedPreferences` named `"session"`, key `current_username` | app-private prefs |

Both are app-private: only ReroPlero can read them, and they're wiped on
uninstall. `SharedPreferences` stays for the session — it holds one small value
and Room would be overkill for it.

You never create `reroplero.db` yourself. Room creates it on first access.

## Schema: two tables, not one nested blob

The JSON version nested each user's payments *inside* the user object. SQL has
no nesting, so the payments moved to their own table, linked back by a
`username` foreign key.

```
users                       payments
─────                       ────────
username  (PK, TEXT)  ◄──┐  id        (PK, TEXT — UUID)
password      (TEXT)     └──username  (FK → users.username, CASCADE)
                            category  (TEXT)
                            cost      (REAL)
                            timestamp (INTEGER — epoch millis)
```

- `username` is the primary key of `users`, so **SQLite itself** rejects a
  duplicate registration — no check-then-insert race.
- The foreign key is declared `onDelete = CASCADE`: deleting a user deletes
  their payments. Without it, payments are orphaned and a re-registered
  username inherits the old user's history. (See 06 — this is not wired yet.)
- `payments.username` needs an `@Index`; every query filters on it.
- `password` is still **plain text** — known limitation, unchanged by the
  migration. `jbcrypt` is already in the version catalog, unused.

## The entities

Both live in `data/local/models/`.

`User.kt`:

```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val password: String
)
```

`Payment.kt` — the pre-existing data class, now also an entity:

```kotlin
@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey val id: String,   // UUID string, generated in MainPage
    val username: String,         // the owner — replaces JSON nesting
    val category: String,         // one of: Food, Transport, Rent, Fun
    val cost: Double,
    val timestamp: Long           // epoch millis (date + time combined)
) {
    operator fun plus(other: Payment): Double = this.cost + other.cost
}
```

`username` has **no default value**, which is deliberate: it forces whoever
builds a `Payment` to say who it belongs to. `MainPage` stamps it at
construction, so the repository can simply insert what it's given.

Room maps Kotlin types to SQLite automatically: `String` → TEXT, `Double` →
REAL, `Long` → INTEGER. **Only constructor properties become columns** —
anything declared in the class body (like `plus`) is ignored.

`plus` returns a `Double`, so it does **not** chain (`a + b + c` won't compile).
It's also now redundant for totalling: the database sums with `SUM(cost)`.

## DAO return-type contract

Worth memorising, because the two are easy to mix up:

| Annotation | Returns | Meaning |
|------------|---------|---------|
| `@Insert` | `Long` | The new row's rowid, or **`-1` if the insert was ignored** |
| `@Delete` / `@Update` | `Int` | **Number of rows affected** (0 = nothing matched) |

`-1` only appears under `onConflict = OnConflictStrategy.IGNORE`. The default
strategy is `ABORT`, which **throws** `SQLiteConstraintException` on a
conflict rather than returning anything.

`@Delete` matches on the **primary key alone** — every other field of the object
passed in is ignored. `deleteUser(User("john", "wrong-password"))` still deletes
John.

Two SQL nullability traps:

- `SELECT SUM(cost) …` returns SQL **`NULL`**, not `0`, when a user has no
  payments. The DAO must declare it `Double?` and the caller coalesces
  (`?: 0.0`). A non-null `Double` crashes on the first newly registered user.
- Lookups that can miss (`getUser`) return a nullable `User?`.

## Conversion boundary

There isn't one any more. Room reads and writes `Payment` / `User` objects
directly — no JSON, no manual field-by-field mapping. `UserRepositoryImpl` is a
thin repository over the DAO rather than a parser.
