#!/usr/bin/env python3
"""Seed the app's Room database with random payments for testing.

Pulls the SQLite file off the emulator with `run-as`, inserts rows locally,
then pushes it back. The app must not be running while this happens, so the
script force-stops it first.

    python3 tools/seed_payments.py --user stouris --count 40

Run with no arguments to see the users and payment counts already in the DB.
"""

import argparse
import calendar
import random
import shutil
import sqlite3
import subprocess
import sys
import tempfile
import uuid
from datetime import datetime
from pathlib import Path

PACKAGE = "com.example.reroplero"
DB_NAME = "Database"  # from R.string.database
CATEGORIES = ["Food", "Transport", "Rent", "Fun"]

# Rough per-category cost ranges, so the chart has believable shape
# instead of uniform noise.
COST_RANGES = {
    "Food": (5, 60),
    "Transport": (2, 25),
    "Rent": (400, 900),
    "Fun": (10, 120),
}

ADB = shutil.which("adb") or str(Path.home() / "Android/Sdk/platform-tools/adb")
REMOTE_DIR = f"/data/data/{PACKAGE}/databases"
DB_FILES = [DB_NAME, f"{DB_NAME}-wal", f"{DB_NAME}-shm"]


def adb(*args, binary=False):
    """Run an adb command, returning stdout."""
    result = subprocess.run(
        [ADB, *args], capture_output=True, check=False
    )
    if result.returncode != 0:
        stderr = result.stderr.decode(errors="replace").strip()
        sys.exit(f"adb {' '.join(args)} failed: {stderr}")
    return result.stdout if binary else result.stdout.decode(errors="replace")


def check_device():
    lines = [l for l in adb("devices").splitlines()[1:] if l.strip()]
    if not lines:
        sys.exit("No device found. Start your emulator and try again.")
    if len(lines) > 1:
        sys.exit(f"Multiple devices attached; disconnect all but one:\n" + "\n".join(lines))


def pull_db(workdir):
    """Copy the DB and its WAL sidecars off the device.

    exec-out (not shell) keeps the byte stream raw — `adb shell` can mangle
    binary data. The -wal file matters: recent writes live there, not in the
    main file, so pulling only the DB would silently lose them.
    """
    for name in DB_FILES:
        data = adb("exec-out", "run-as", PACKAGE, "cat", f"{REMOTE_DIR}/{name}", binary=True)
        (workdir / name).write_bytes(data)


def push_db(workdir):
    """Push the DB back, replacing the device's copy.

    We checkpointed the WAL into the main file locally, so the device's stale
    -wal and -shm must go — leaving them would let SQLite replay old writes
    over the rows we just added. run-as can't read /sdcard, so we stage in
    /data/local/tmp, which both sides can reach.
    """
    tmp = f"/data/local/tmp/{DB_NAME}"
    adb("push", str(workdir / DB_NAME), tmp)
    adb("shell", "run-as", PACKAGE, "cp", tmp, f"{REMOTE_DIR}/{DB_NAME}")
    for suffix in ("-wal", "-shm"):
        adb("shell", "run-as", PACKAGE, "rm", "-f", f"{REMOTE_DIR}/{DB_NAME}{suffix}")
    adb("shell", "rm", "-f", tmp)


def show_users(db_path):
    conn = sqlite3.connect(db_path)
    rows = conn.execute(
        "SELECT u.username, COUNT(p.id) FROM users u "
        "LEFT JOIN payments p ON p.username = u.username "
        "GROUP BY u.username ORDER BY u.username"
    ).fetchall()
    conn.close()
    print("Users in the database (payments each):")
    for username, count in rows:
        print(f"  {username:<12} {count}")
    print("\nPick one with --user, e.g.:")
    print(f"  python3 tools/seed_payments.py --user {rows[0][0]} --count 40")


def random_payments(username, count, seed=None):
    """Build `count` payments spread over the current month.

    Only the current month: the analytics screen groups by DAY_OF_MONTH, so
    payments from different months would collide onto the same point (the 3rd
    of June would stack onto the 3rd of July).
    """
    rng = random.Random(seed)
    now = datetime.now()
    last_day = min(now.day, calendar.monthrange(now.year, now.month)[1])

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
    args = parser.parse_args()

    check_device()
    print(f"Stopping {PACKAGE} so it releases the database...")
    adb("shell", "am", "force-stop", PACKAGE)

    with tempfile.TemporaryDirectory() as tmpdir:
        workdir = Path(tmpdir)
        print("Pulling database...")
        pull_db(workdir)
        db_path = workdir / DB_NAME

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
