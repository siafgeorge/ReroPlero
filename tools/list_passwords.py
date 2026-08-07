#!/usr/bin/env python3
"""List every user's username and password straight from the app's database.

The app stores passwords in plain text (no hashing wired up yet — see
wiki/06-current-state.md's "Plain-text passwords" note), so this is a direct
read of whatever's on disk. Local dev/debugging use only: pulls the SQLite
file off the emulator with `run-as`, same as seed_payments.py, and never
writes anything back.

    python3 tools/list_passwords.py

On Windows, use `python` instead of `python3`. Needs Python 3.10+.
"""

import sqlite3
import tempfile
from pathlib import Path

from reroplero_db import DB_NAME, PACKAGE, adb, check_device, pull_db


def main():
    check_device()
    print(f"Stopping {PACKAGE} so it releases the database...")
    adb("shell", "am", "force-stop", PACKAGE)

    with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmpdir:
        workdir = Path(tmpdir)
        print("Pulling database...")
        pull_db(workdir)

        conn = sqlite3.connect(workdir / DB_NAME)
        rows = conn.execute(
            "SELECT username, password FROM users ORDER BY username"
        ).fetchall()
        conn.close()

    if not rows:
        print("No users in the database yet — register one in the app first.")
        return

    width = max(len(username) for username, _ in rows)
    print(f"{'username':<{width}}  password")
    print(f"{'-' * width}  {'-' * 8}")
    for username, password in rows:
        print(f"{username:<{width}}  {password}")


if __name__ == "__main__":
    main()
