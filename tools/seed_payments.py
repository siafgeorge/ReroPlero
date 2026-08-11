#!/usr/bin/env python3
"""Seed the app's Room database with payments for testing.

Pulls the SQLite file off the emulator with `run-as`, inserts rows locally,
then pushes it back. The app must not be running while this happens, so the
script force-stops it first.

Two generators:

    # scattered random payments across the current month
    python3 tools/seed_payments.py --user stouris --count 40

    # a steady year-to-date history, for checking the analytics projection
    python3 tools/seed_payments.py --user stouris --consistent --wipe

`--consistent` spends a different amount every day — averaging --daily, swung by
--jitter, split randomly across Food/Transport/Fun — plus one Rent payment per
month. Pass `--jitter 0` to make every day identical instead, which turns the
projection into a round number you can check in your head. Either way the
script prints the exact figures the app should show; see PROJECTION NOTES.

Run with no arguments to see the users and payment counts already in the DB.
Delete a user (and all their payments) with:

    python3 tools/seed_payments.py --remove-user stouris

On Windows, use `python` instead of `python3`. Needs Python 3.10+.


PROJECTION NOTES
----------------
MainPageViewModel.analyticsFrom() computes, for a given month:

    daysElapsed    = current month ? today.dayOfMonth : month.lengthOfMonth()
    fixed          = sum of payments whose category is in FIXED_CATEGORIES
    variable       = spent - fixed
    variablePerDay = variable / daysElapsed
    projectedTotal = fixed + variablePerDay * month.lengthOfMonth()
                     (null when daysElapsed < MIN_DAYS_FOR_PROJECTION)

For a *past* month daysElapsed equals lengthOfMonth, so the projection always
reduces to the actual total — it can't really be wrong. The current month is
the only meaningful test: there daysElapsed is smaller than the month, so the
projection extrapolates.

With --jitter 0 every day is identical, variablePerDay is the same on every
prefix of the month, and the current month's projection is exactly
`rent + daily * daysInMonth` — checkable in your head.

With jitter on, variablePerDay is the *average of the days elapsed so far*, so
the projection no longer equals what the month will eventually total: it says
"if the rest of the month keeps up today's average, we land here." That is the
projection behaving correctly, not a bug. To verify it, compare the app against
the table this script prints, which recomputes the same formula over the rows
it actually generated.
"""

import argparse
import calendar
import random
import sqlite3
import sys
import tempfile
import uuid
from collections import defaultdict
from datetime import date, datetime, timedelta
from pathlib import Path

from reroplero_db import PACKAGE, DB_NAME, adb, check_device, pull_db, push_db

CATEGORIES = ["Food", "Transport", "Rent", "Fun"]

# Must match FIXED_CATEGORIES in MainPageViewModel — these are excluded from
# the per-day average and added back to the projection as a flat monthly cost.
FIXED_CATEGORIES = {"Rent"}

# Everything the daily budget gets split across. Weighted so Food dominates,
# the way a real month looks.
VARIABLE_CATEGORIES = ["Food", "Transport", "Fun"]
VARIABLE_WEIGHTS = [0.6, 0.25, 0.15]

# Rough per-category cost ranges, so the chart has believable shape
# instead of uniform noise. Used by the random generator only.
COST_RANGES = {
    "Food": (5, 60),
    "Transport": (2, 25),
    "Rent": (400, 900),
    "Fun": (10, 120),
}

# Don't emit change-sized payments when splitting a day's budget.
MIN_PAYMENT_CENTS = 150

# Mirrors MIN_DAYS_FOR_PROJECTION in MainPageViewModel.
MIN_DAYS_FOR_PROJECTION = 4


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
    print(f"  python3 tools/seed_payments.py --user {rows[0][0]} --consistent --wipe")


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


def make_row(username, category, cents, day, rng, hour=None):
    """One payment row, at a random daytime hour on `day`.

    Hours stay well inside 08:00-21:00 so that a timezone mismatch between this
    machine and the emulator can't push a payment across midnight into the
    neighbouring day, which would skew the per-day average.
    """
    when = datetime(
        day.year,
        day.month,
        day.day,
        rng.randint(8, 21) if hour is None else hour,
        rng.randint(0, 59),
        rng.randint(0, 59),
    )
    return (
        str(uuid.uuid4()),
        username,
        category,
        round(cents / 100, 2),
        int(when.timestamp() * 1000),
    )


def split_day(daily_cents, rng):
    """Split one day's budget into 2-3 payments summing to *exactly* the budget.

    Working in integer cents is what makes the total exact — splitting floats
    and rounding each piece would drift a cent or two per day, which is enough
    to make the projection check ambiguous over a year.

    Each payment gets MIN_PAYMENT_CENTS as a floor, then the leftover is cut at
    random points (stars and bars), so the split varies day to day while the
    daily sum never moves.
    """
    if daily_cents <= MIN_PAYMENT_CENTS:
        return [(rng.choices(VARIABLE_CATEGORIES, VARIABLE_WEIGHTS)[0], daily_cents)]

    count = rng.choice((2, 3))
    while count > 1 and daily_cents < count * MIN_PAYMENT_CENTS:
        count -= 1

    remainder = daily_cents - count * MIN_PAYMENT_CENTS
    cuts = sorted(rng.randint(0, remainder) for _ in range(count - 1))
    parts = [b - a for a, b in zip([0, *cuts], [*cuts, remainder])]
    categories = rng.choices(VARIABLE_CATEGORIES, VARIABLE_WEIGHTS, k=count)
    return [(cat, part + MIN_PAYMENT_CENTS) for cat, part in zip(categories, parts)]


def daily_budget_cents(base_cents, jitter, rng, used):
    """One day's variable budget: `base_cents` swung by up to ±`jitter`.

    No two days in a run get the same total. Over a wide band collisions are
    unlikely anyway, but "every day is a different number" is easier to eyeball
    on the chart when it's guaranteed rather than merely probable.

    jitter=0 pins every day to base_cents, which makes variablePerDay identical
    on every prefix of the month and the projection a round, hand-checkable
    number. See PROJECTION NOTES.
    """
    if jitter <= 0:
        return base_cents

    low = max(MIN_PAYMENT_CENTS, round(base_cents * (1 - jitter)))
    high = max(low, round(base_cents * (1 + jitter)))
    for _ in range(100):
        value = rng.randint(low, high)
        if value not in used:
            used.add(value)
            return value
    return rng.randint(low, high)  # band exhausted; a repeat is harmless


def consistent_payments(username, start, end, daily, rent, jitter=0.0, seed=None):
    """Daily spending from `start` to `end` inclusive, plus monthly rent.

    Every day gets a variable budget averaging `daily`, split across 2-3
    payments. Rent lands on the 1st of each month (and on `start` itself, so a
    mid-month start still carries its fixed cost).
    """
    rng = random.Random(seed)
    base_cents = round(daily * 100)
    rent_cents = round(rent * 100)
    used = set()

    rows = []
    day = start
    while day <= end:
        if rent_cents > 0 and (day.day == 1 or day == start):
            rows.append(make_row(username, "Rent", rent_cents, day, rng, hour=9))
        budget = daily_budget_cents(base_cents, jitter, rng, used)
        for category, cents in split_day(budget, rng):
            rows.append(make_row(username, category, cents, day, rng))
        day += timedelta(days=1)
    return rows


def random_payments(username, count, seed=None):
    """Build `count` payments scattered over the current month.

    Deliberately lumpy: useful for eyeballing the chart, useless for checking
    the projection, since the per-day average swings with whatever days the RNG
    happened to pick. Use --consistent for that.
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


def projection_report(rows, today):
    """Recompute analyticsFrom() over the generated rows and print the result.

    This deliberately mirrors the Kotlin rather than reusing the parameters the
    rows were built from — if the generator and the formula ever disagree, the
    numbers printed here are the ones the app will show.
    """
    by_month = defaultdict(list)
    for _, _, category, cost, timestamp in rows:
        when = datetime.fromtimestamp(timestamp / 1000)
        by_month[(when.year, when.month)].append((category, cost))

    print(f"\nExpected analytics values (today = {today.isoformat()}):\n")
    print(f"  {'month':<9}{'days':>6}{'spent':>11}{'fixed':>10}{'per day':>10}{'projected':>12}")
    print(f"  {'-' * 56}")

    for (year, month), items in sorted(by_month.items()):
        length = calendar.monthrange(year, month)[1]
        is_current = (year, month) == (today.year, today.month)
        days_elapsed = today.day if is_current else length

        spent = sum(cost for _, cost in items)
        fixed = sum(cost for category, cost in items if category in FIXED_CATEGORIES)
        per_day = (spent - fixed) / days_elapsed if days_elapsed else 0.0
        projected = (
            None
            if days_elapsed < MIN_DAYS_FOR_PROJECTION
            else fixed + per_day * length
        )

        label = f"{year}-{month:02d}"
        shown = "—" if projected is None else f"{projected:.2f}"
        marker = "  <- current month" if is_current else ""
        print(
            f"  {label:<9}{days_elapsed:>6}{spent:>11.2f}{fixed:>10.2f}"
            f"{per_day:>10.2f}{shown:>12}{marker}"
        )

    print(
        "\n  Past months always project to their actual total (days elapsed == month\n"
        "  length), so they only confirm the arithmetic. The current month is the\n"
        "  real check: its projection extrapolates the average of the days elapsed\n"
        "  so far across the whole month, so it is not meant to equal what the\n"
        "  month will finally total."
    )


def parse_date(value):
    try:
        return datetime.strptime(value, "%Y-%m-%d").date()
    except ValueError:
        raise argparse.ArgumentTypeError(f"expected YYYY-MM-DD, got {value!r}") from None


def main():
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
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
    parser.add_argument(
        "--consistent",
        action="store_true",
        help="steady daily spend over a date range instead of random one-offs (ignores --count)",
    )
    parser.add_argument(
        "--start",
        type=parse_date,
        default=date(2026, 1, 1),
        help="first day for --consistent (default: 2026-01-01)",
    )
    parser.add_argument(
        "--end",
        type=parse_date,
        help="last day for --consistent (default: today)",
    )
    parser.add_argument(
        "--daily",
        type=float,
        default=30.0,
        help="variable spend per day for --consistent (default: 30.00)",
    )
    parser.add_argument(
        "--rent",
        type=float,
        default=800.0,
        help="monthly Rent payment for --consistent, 0 to skip (default: 800.00)",
    )
    parser.add_argument(
        "--jitter",
        type=float,
        default=0.35,
        help="how far each day's spend swings from --daily, 0-1 "
        "(default: 0.35; use 0 for an identical amount every day)",
    )
    args = parser.parse_args()

    if not 0 <= args.jitter < 1:
        sys.exit(f"--jitter must be between 0 and 1, got {args.jitter}.")

    today = date.today()
    end = args.end or today
    if args.consistent and args.start > end:
        sys.exit(f"--start ({args.start}) is after --end ({end}).")
    if args.consistent and end > today:
        # Future payments would inflate `spent` while daysElapsed stays at
        # today's date, so the projection would read far too high.
        print(f"Warning: --end {end} is in the future; the projection will look wrong.")

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

        if args.consistent:
            rows = consistent_payments(
                args.user, args.start, end, args.daily, args.rent, args.jitter, args.seed
            )
            spread = (
                "identical every day"
                if args.jitter <= 0
                else f"±{args.jitter:.0%}, a different amount every day"
            )
            print(
                f"Generating {args.start} to {end}: "
                f"{args.daily:.2f}/day variable ({spread}) + {args.rent:.2f}/month rent."
            )
            if not args.wipe:
                print(
                    "Note: existing payments were kept (--wipe removes them). "
                    "Leftover rows will throw the projection off."
                )
        else:
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

    if args.consistent:
        projection_report(rows, today)

    print("\nLaunch the app and open the analytics tab.")


if __name__ == "__main__":
    main()
