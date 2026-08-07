"""Shared adb/SQLite plumbing for the tools/ scripts.

Pulling and pushing the app's Room database off an emulator/device via
`run-as`, plus the Windows-safe adb wrappers underneath it. See
seed_payments.py's module docstring for the story on *why* each piece works
the way it does (WAL handling, /data/local/tmp staging, text-mode corruption
avoidance) — this module just holds the code, seed_payments.py holds the
explanation.
"""

import os
import shutil
import subprocess
import sys
from pathlib import Path

PACKAGE = "com.example.reroplero"
DB_NAME = "Database"  # from R.string.database
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

    The caller is expected to have checkpointed its WAL into the main file
    locally, so the device's stale -wal and -shm must go — leaving them would
    let SQLite replay old writes over whatever the caller just changed. run-as
    can't read /sdcard, so we stage in /data/local/tmp, which both sides can
    reach.
    """
    tmp = f"/data/local/tmp/{DB_NAME}"
    adb("push", str(workdir / DB_NAME), tmp)
    adb("shell", "run-as", PACKAGE, "cp", tmp, f"{REMOTE_DIR}/{DB_NAME}")
    for suffix in ("-wal", "-shm"):
        adb("shell", "run-as", PACKAGE, "rm", "-f", f"{REMOTE_DIR}/{DB_NAME}{suffix}")
    adb("shell", "rm", "-f", tmp)
