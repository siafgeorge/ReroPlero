#!/usr/bin/env python3
"""Seed the app's Room database with random payments for testing.

Pulls the SQLite file off the emulator with `run-as`, inserts rows locally,
then pushes it back. The app must not be running while this happens, so the
script force-stops it first.

    python3 tools/seed_payments.py --user stouris --count 40

Run with no arguments to see the users and payment counts already in the DB.
Delete a user (and all their payments) with:

    python3 tools/seed_payments.py --remove-user stouris

On Windows, use `python` instead of `python3`. Needs Python 3.10+.
"""

import argparse
import calendar
import random
import sqlite3
import sys
import tempfile
import uuid
from datetime import datetime
from pathlib import Path

from reroplero_db import PACKAGE, DB_NAME, adb, check_device, pull_db, push_db

CATEGORIES = ["Food", "Transport", "Rent", "Fun"]

# Rough per-category cost ranges, so the chart has believable shape
# instead of uniform noise.
COST_RANGES = {
    "Food": (5, 60),
    "Transport": (2, 25),
    "Rent": (400, 900),
    "Fun": (10, 120),
}


def show_users(db_path):
    conn = sqlite3.connect(db_path)
    rows = conn.execute(
        "SELECT u.username, COUNT(p.id) FROM users u "
        "LEFT JOIN payments p ON p.username = u.username "
        "GROUP BY u.username ORDER BY u.username"
    ).fetchall()
    conn.close()
    if not rows:
        print("No users in the database yet — register one in the app first.")
        return
    print("Users in the database (payments each):")
    for username, count in rows:
        print(f"  {username:<12} {count}")
    print("\nPick one with --user, e.g.:")
    print(f"  python3 tools/seed_payments.py --user {rows[0][0]} --count 40")


def remove_user(conn, username):
    """Delete a user and every payment attached to them.

    Payments reference the username, so they must go first — otherwise
    they'd be orphaned rows pointing at a user that no longer exists.
    Returns how many payments were removed.
    """
    payments = conn.execute(
        "DELETE FROM payments WHERE username = ?", (username,)
    ).rowcount
    conn.execute("DELETE FROM users WHERE username = ?", (username,))
    return payments


def random_payments(username, count, seed=None):
    """Build `count` payments spread over the current month.

    Only the current month: the analytics screen groups by DAY_OF_MONTH, so
    payments from different months would collide onto the same point (the 3rd
    of June would stack onto the 3rd of July).
    """
    rng = random.Random(seed)
    now = datetime.now()
    last_day = max(now.day, calendar.monthrange(now.year, now.month)[1])

    rows = []
    for _ in range(count):
        day = rng.randint(1, last_day)
        when = now.replace(
            day=day,
            hour=rng.randint(8, 21),
            minute=rng.randint(0, 59),
            second=rng.randint(0, 59),
            microsecond=0,
        )
        category = rng.choice(CATEGORIES)
        low, high = COST_RANGES[category]
        rows.append(
            (
                str(uuid.uuid4()),
                username,
                category,
                round(rng.uniform(low, high), 2),
                int(when.timestamp() * 1000),
            )
        )
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--user", help="username to attach the payments to")
    parser.add_argument("--count", type=int, default=40, help="how many payments (default: 40)")
    parser.add_argument("--seed", type=int, help="RNG seed, for repeatable data")
    parser.add_argument(
        "--wipe",
        action="store_true",
        help="delete this user's existing payments first",
    )
    parser.add_argument(
        "--remove-user",
        metavar="USERNAME",
        help="delete a user and all their payments, then exit",
    )
    args = parser.parse_args()

    check_device()
    print(f"Stopping {PACKAGE} so it releases the database...")
    adb("shell", "am", "force-stop", PACKAGE)

    # ignore_cleanup_errors: Windows won't delete a directory that still holds
    # an open file, so a mid-flight exception would otherwise be masked by a
    # PermissionError from the cleanup.
    with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmpdir:
        workdir = Path(tmpdir)
        print("Pulling database...")
        pull_db(workdir)
        db_path = workdir / DB_NAME

        if args.remove_user:
            conn = sqlite3.connect(db_path)
            exists = conn.execute(
                "SELECT 1 FROM users WHERE username = ?", (args.remove_user,)
            ).fetchone()
            if not exists:
                conn.close()
                sys.exit(
                    f"No user named {args.remove_user!r}. Run with no arguments to list them."
                )
            count = conn.execute(
                "SELECT COUNT(*) FROM payments WHERE username = ?", (args.remove_user,)
            ).fetchone()[0]
            answer = input(
                f"Delete user {args.remove_user!r} and their {count} payments? [y/N] "
            )
            if answer.strip().lower() not in ("y", "yes"):
                conn.close()
                print("Aborted; database left unchanged.")
                return
            removed = remove_user(conn, args.remove_user)
            conn.commit()
            # Fold the WAL into the main file so pushing that one file is enough.
            conn.execute("PRAGMA wal_checkpoint(TRUNCATE)")
            conn.close()
            print(f"Removed {args.remove_user} and {removed} payments.")
            print("Pushing database back...")
            push_db(workdir)
            return

        if not args.user:
            show_users(db_path)
            return

        conn = sqlite3.connect(db_path)
        exists = conn.execute(
            "SELECT 1 FROM users WHERE username = ?", (args.user,)
        ).fetchone()
        if not exists:
            conn.close()
            sys.exit(
                f"No user named {args.user!r}. Run without --user to list them."
            )

        if args.wipe:
            deleted = conn.execute(
                "DELETE FROM payments WHERE username = ?", (args.user,)
            ).rowcount
            print(f"Deleted {deleted} existing payments for {args.user}.")

        rows = random_payments(args.user, args.count, args.seed)
        conn.executemany(
            "INSERT INTO payments (id, username, category, cost, timestamp) "
            "VALUES (?, ?, ?, ?, ?)",
            rows,
        )
        conn.commit()
        # Fold the WAL into the main file so pushing that one file is enough.
        conn.execute("PRAGMA wal_checkpoint(TRUNCATE)")
        total = conn.execute(
            "SELECT COUNT(*), SUM(cost) FROM payments WHERE username = ?", (args.user,)
        ).fetchone()
        conn.close()

        print(f"Inserted {len(rows)} payments for {args.user}.")
        print("Pushing database back...")
        push_db(workdir)

    print(f"\nDone. {args.user} now has {total[0]} payments totalling {total[1]:.2f}.")
    print("Launch the app and open the analytics tab.")


if __name__ == "__main__":
    main()
