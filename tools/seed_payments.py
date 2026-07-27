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
import os
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

REMOTE_DIR = f"/data/data/{PACKAGE}/databases"
DB_FILES = [DB_NAME, f"{DB_NAME}-wal", f"{DB_NAME}-shm"]


def find_adb():
    """Locate adb: PATH first, then the usual SDK location per platform."""
    found = shutil.which("adb")
    if found:
        return found

    exe = "adb.exe" if os.name == "nt" else "adb"
    roots = [
        Path(os.environ[var])
        for var in ("ANDROID_SDK_ROOT", "ANDROID_HOME")
        if os.environ.get(var)
    ]
    if os.name == "nt":
        if os.environ.get("LOCALAPPDATA"):
            roots.append(Path(os.environ["LOCALAPPDATA"]) / "Android" / "Sdk")
    else:
        roots.append(Path.home() / "Android" / "Sdk")
        roots.append(Path.home() / "Library" / "Android" / "sdk")  # macOS

    for root in roots:
        candidate = root / "platform-tools" / exe
        if candidate.is_file():
            return str(candidate)
    return None


ADB = find_adb()


def _run(args):
    if ADB is None:
        sys.exit(
            "Couldn't find adb. Add platform-tools to your PATH, or set "
            "ANDROID_SDK_ROOT to your SDK folder."
        )
    return subprocess.run([ADB, *args], capture_output=True, check=False)


def adb(*args):
    """Run an adb command, returning stdout."""
    result = _run(args)
    if result.returncode != 0:
        stderr = result.stderr.decode(errors="replace").strip()
        sys.exit(f"adb {' '.join(args)} failed: {stderr}")
    return result.stdout.decode(errors="replace")


def adb_ok(*args):
    """Run an adb command, returning whether it succeeded. For probes."""
    return _run(args).returncode == 0


def check_device():
    """Count attached devices.

    Only lines shaped like "<serial>\\t<state>" count — on Windows the first
    adb call often prints "* daemon not running; starting now ..." to stdout,
    and a naive splitlines()[1:] would count that as an extra device.
    """
    lines = [
        l
        for l in adb("devices").splitlines()[1:]
        if "\t" in l and l.split("\t")[1].strip() == "device"
    ]
    if not lines:
        sys.exit("No device found. Start your emulator and try again.")
    if len(lines) > 1:
        sys.exit("Multiple devices attached; disconnect all but one:\n" + "\n".join(lines))


def pull_db(workdir):
    """Copy the DB and its WAL sidecars off the device.

    Staged through /data/local/tmp rather than streamed with `exec-out cat`:
    on Windows adb's stdout goes through the C runtime's text mode, which can
    expand 0x0A bytes to 0x0D 0x0A and quietly corrupt a SQLite file. `adb
    pull` is a framed file transfer, so it stays byte-exact everywhere.

    The `>` runs on the *shell* side on purpose. /data/local/tmp is 0771
    root:shell, so the app UID that run-as drops to can't create files there;
    the device shell opens the file, and run-as just inherits the fd. It has
    to be one argument, or Windows quoting and adb's arg-joining mangle it.

    The -wal file matters: recent writes live there, not in the main file. But
    it can legitimately be absent if SQLite checkpointed on shutdown, so a
    missing sidecar is skipped instead of being treated as an error.
    """
    for name in DB_FILES:
        remote = f"{REMOTE_DIR}/{name}"
        if not adb_ok("shell", "run-as", PACKAGE, "test", "-f", remote):
            continue
        staged = f"/data/local/tmp/{name}"
        adb("shell", f"run-as {PACKAGE} cat {remote} > {staged}")
        adb("pull", staged, str(workdir / name))
        adb("shell", "rm", "-f", staged)


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
