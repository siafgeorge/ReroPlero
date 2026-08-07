#!/usr/bin/env python3
"""Set a user's password directly in the app's database.

Pulls the SQLite file off the emulator with `run-as`, updates the row
locally, then pushes it back — same dance as seed_payments.py. The app must
not be running while this happens, so the script force-stops it first.

    python3 tools/set_password.py --user asd --password newpass123

Omit --password to be prompted for it (hidden input, so it doesn't end up in
your shell history):

    python3 tools/set_password.py --user asd

On Windows, use `python` instead of `python3`. Needs Python 3.10+.
"""

import argparse
import getpass
import sqlite3
import sys
import tempfile
from pathlib import Path

from reroplero_db import DB_NAME, PACKAGE, adb, check_device, pull_db, push_db


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--user", required=True, help="username whose password to change")
    parser.add_argument("--password", help="new password (prompted securely if omitted)")
    args = parser.parse_args()

    new_password = args.password or getpass.getpass("New password: ")
    if not new_password:
        sys.exit("Password can't be empty.")

    check_device()
    print(f"Stopping {PACKAGE} so it releases the database...")
    adb("shell", "am", "force-stop", PACKAGE)

    with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmpdir:
        workdir = Path(tmpdir)
        print("Pulling database...")
        pull_db(workdir)
        db_path = workdir / DB_NAME

        conn = sqlite3.connect(db_path)
        exists = conn.execute(
            "SELECT 1 FROM users WHERE username = ?", (args.user,)
        ).fetchone()
        if not exists:
            conn.close()
            sys.exit(
                f"No user named {args.user!r}. "
                f"Run tools/list_passwords.py to see who's there."
            )

        conn.execute(
            "UPDATE users SET password = ? WHERE username = ?",
            (new_password, args.user),
        )
        conn.commit()
        # Fold the WAL into the main file so pushing that one file is enough.
        conn.execute("PRAGMA wal_checkpoint(TRUNCATE)")
        conn.close()

        print("Pushing database back...")
        push_db(workdir)

    print(f"Password for {args.user!r} updated.")


if __name__ == "__main__":
    main()
