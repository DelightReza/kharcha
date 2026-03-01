#!/usr/bin/env python3
"""
migrate_to_v2.py

Converts Ramzan Fund data from V1 (name-based) to V2 (ID-based with snapshots).

V1 format
---------
config.json:
  "people": ["Raza", "Gulam", ...]
  "billTypes": [{"name": "Other", "icon": "📦"}, ...]

data.json:
  "people":    {"Raza": 2000.0, ...}
  "billTypes": {"Other": 9539.0, ...}
  transactions (credit): {"whoOrBill": "Raza", ...}
  transactions (debit):  {"whoOrBill": "Other", ...}  # no splitAmong

V2 format
---------
config.json:
  "people": [{"id": "raza", "name": "Raza", "active": true}, ...]
  "billTypes": [{"id": "other", "name": "Other", "icon": "📦"}, ...]

data.json:
  "people":    {"raza": 2000.0, ...}
  "billTypes": {"other": 9539.0, ...}
  transactions (credit): {"whoOrBill": "raza", ...}
  transactions (debit):  {"whoOrBill": "other", "splitAmong": ["raza", "gulam", ...], ...}

Usage
-----
  python migrate_to_v2.py                        # uses config.json + data.json in cwd
  python migrate_to_v2.py --config path/config.json --data path/data.json
  python migrate_to_v2.py --dry-run              # preview changes without writing
  python migrate_to_v2.py --no-backup            # skip creating .bak files
"""

import argparse
import json
import os
import re
import shutil
from datetime import datetime


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_id(name: str) -> str:
    """Turn a display name into a stable lowercase ID (e.g. 'Nizamuddin' → 'nizamuddin')."""
    return re.sub(r"[^a-z0-9_]+", "_", name.strip().lower()).strip("_")


def _is_v2_people(people) -> bool:
    """Return True when people is already the V2 object-array format."""
    return bool(people) and isinstance(people[0], dict) and "id" in people[0]


def _is_v2_bill_types(bill_types) -> bool:
    """Return True when billTypes is already the V2 format (has 'id' field)."""
    return bool(bill_types) and isinstance(bill_types[0], dict) and "id" in bill_types[0]


def _backup(path: str) -> str:
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_path = f"{path}.{ts}.bak"
    shutil.copy2(path, backup_path)
    return backup_path


# ---------------------------------------------------------------------------
# Config migration
# ---------------------------------------------------------------------------

def migrate_config(config: dict) -> tuple[dict, dict, dict]:
    """
    Upgrade config to V2 in-place.

    Returns
    -------
    (updated_config, person_name_to_id, bill_name_to_id)
    """
    people = config.get("people", [])
    bill_types = config.get("billTypes", [])

    # ── people ──────────────────────────────────────────────────────────────
    person_name_to_id: dict[str, str] = {}

    if _is_v2_people(people):
        # Already V2 – just build the lookup map
        for p in people:
            person_name_to_id[p["name"]] = p["id"]
            person_name_to_id[p["id"]] = p["id"]   # id maps to itself
        print("  config.people  : already V2 – no change needed")
    else:
        # V1 string array → convert to object array
        new_people = []
        for name in people:
            pid = _make_id(name)
            person_name_to_id[name] = pid
            person_name_to_id[pid] = pid
            new_people.append({"id": pid, "name": name, "active": True})
        config["people"] = new_people
        print(f"  config.people  : migrated {len(new_people)} members to V2 object format")

    # ── billTypes ────────────────────────────────────────────────────────────
    bill_name_to_id: dict[str, str] = {}

    if _is_v2_bill_types(bill_types):
        for bt in bill_types:
            bill_name_to_id[bt["name"]] = bt["id"]
            bill_name_to_id[bt["id"]] = bt["id"]
        print("  config.billTypes: already V2 – no change needed")
    else:
        new_bill_types = []
        for bt in bill_types:
            name = bt["name"]
            bid = _make_id(name)
            bill_name_to_id[name] = bid
            bill_name_to_id[bid] = bid
            new_bill_types.append({"id": bid, "name": name, "icon": bt.get("icon", "🧾")})
        config["billTypes"] = new_bill_types
        print(f"  config.billTypes: migrated {len(new_bill_types)} bill types to V2 format")

    return config, person_name_to_id, bill_name_to_id


# ---------------------------------------------------------------------------
# Data migration
# ---------------------------------------------------------------------------

def migrate_data(data: dict,
                 person_name_to_id: dict,
                 bill_name_to_id: dict) -> dict:
    """
    Upgrade data.json to V2 in-place.

    - Converts `people` and `billTypes` aggregate keys to IDs.
    - Converts every transaction's `whoOrBill` to an ID.
    - Adds a `splitAmong` snapshot to every debit transaction that lacks one.
    - Removes the legacy `exemptions` field when `splitAmong` is added.

    Returns the migrated data dict.
    """
    all_person_ids = list(person_name_to_id.values())
    # De-duplicate while preserving order (dict values from name→id may repeat ids)
    seen: set[str] = set()
    ordered_ids: list[str] = []
    for pid in all_person_ids:
        if pid not in seen:
            seen.add(pid)
            ordered_ids.append(pid)

    # ── people aggregate dict ─────────────────────────────────────────────
    old_people = data.get("people", {})
    new_people: dict[str, float] = {}
    people_changed = 0

    for key, value in old_people.items():
        new_key = person_name_to_id.get(key, key)
        if new_key != key:
            people_changed += 1
        new_people[new_key] = value
    data["people"] = new_people

    if people_changed:
        print(f"  data.people    : renamed {people_changed} keys to IDs")
    else:
        print("  data.people    : keys already use IDs – no change")

    # ── billTypes aggregate dict ─────────────────────────────────────────────
    old_bill_types = data.get("billTypes", {})
    new_bill_types: dict[str, float] = {}
    bt_changed = 0

    for key, value in old_bill_types.items():
        new_key = bill_name_to_id.get(key, key)
        if new_key != key:
            bt_changed += 1
        new_bill_types[new_key] = value
    data["billTypes"] = new_bill_types

    if bt_changed:
        print(f"  data.billTypes : renamed {bt_changed} keys to IDs")
    else:
        print("  data.billTypes : keys already use IDs – no change")

    # ── transactions ─────────────────────────────────────────────────────────
    credit_updated = 0
    debit_snapshot_added = 0
    debit_snapshot_kept = 0
    debit_exemptions_removed = 0

    for tx in data.get("transactions", []):
        tx_type = tx.get("type")
        old_who = tx.get("whoOrBill", "")

        if tx_type == "credit":
            new_who = person_name_to_id.get(old_who, old_who)
            if new_who != old_who:
                tx["whoOrBill"] = new_who
                credit_updated += 1

        elif tx_type == "debit":
            new_who = bill_name_to_id.get(old_who, old_who)
            if new_who != old_who:
                tx["whoOrBill"] = new_who

            if "splitAmong" in tx and tx["splitAmong"]:
                # Already has a snapshot – keep it, just normalise IDs inside it
                tx["splitAmong"] = [
                    person_name_to_id.get(p, p) for p in tx["splitAmong"]
                ]
                debit_snapshot_kept += 1
            else:
                # Build snapshot: all people minus anyone in legacy exemptions
                exemptions = tx.pop("exemptions", [])
                if exemptions:
                    debit_exemptions_removed += 1
                exempt_ids = {person_name_to_id.get(e, e) for e in exemptions}
                tx["splitAmong"] = [p for p in ordered_ids if p not in exempt_ids]
                debit_snapshot_added += 1

    print(f"  transactions   : "
          f"{credit_updated} credit whoOrBill updated, "
          f"{debit_snapshot_added} debit snapshots added, "
          f"{debit_snapshot_kept} debit snapshots kept"
          + (f", {debit_exemptions_removed} legacy exemptions removed"
             if debit_exemptions_removed else ""))

    return data


# ---------------------------------------------------------------------------
# Custom JSON serialization helpers
# ---------------------------------------------------------------------------

def _dump_config(config: dict) -> str:
    """Serialize config.json with compact single-line objects for billTypes and people entries."""
    raw = json.dumps(config, indent=2, ensure_ascii=False)
    # Compact flat objects (no nested braces/brackets) to a single line.
    # Matches each billTypes/people entry like {"id": "raza", "name": "Raza", "active": true}.
    def _compact_obj(m: re.Match) -> str:
        text = m.group(0)
        text = re.sub(r'\{\s+', '{', text)
        text = re.sub(r',\s+', ', ', text)
        text = re.sub(r'\s+\}', '}', text)
        return text
    return re.sub(r'\{[^{}\[\]]+\}', _compact_obj, raw, flags=re.DOTALL)


def _dump_data(data: dict) -> str:
    """Serialize data.json with compact single-line splitAmong arrays."""
    raw = json.dumps(data, indent=2, ensure_ascii=False)
    # Compact multi-line splitAmong arrays to a single line.
    def _compact_split_among(m: re.Match) -> str:
        items = re.findall(r'"([^"]+)"', m.group(1))
        return '"splitAmong": [' + ', '.join(f'"{i}"' for i in items) + ']'
    return re.sub(r'"splitAmong": \[([^\]]+)\]', _compact_split_among, raw, flags=re.DOTALL)


# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------

def validate(config: dict, data: dict) -> list[str]:
    """
    Return a list of warning strings for any inconsistencies found.
    """
    warnings = []
    people = config.get("people", [])
    bill_types = config.get("billTypes", [])

    if _is_v2_people(people):
        valid_ids = {p["id"] for p in people}
    else:
        valid_ids = set(people)

    if _is_v2_bill_types(bill_types):
        valid_bt_ids = {bt["id"] for bt in bill_types}
    else:
        valid_bt_ids = {bt["name"] for bt in bill_types}

    for i, tx in enumerate(data.get("transactions", [])):
        who = tx.get("whoOrBill", "")
        if tx.get("type") == "credit" and who not in valid_ids:
            warnings.append(f"  Transaction #{i} (credit): whoOrBill={who!r} not in config.people")
        if tx.get("type") == "debit":
            if who not in valid_bt_ids:
                warnings.append(f"  Transaction #{i} (debit): whoOrBill={who!r} not in config.billTypes")
            for pid in tx.get("splitAmong", []):
                if pid not in valid_ids:
                    warnings.append(
                        f"  Transaction #{i} (debit): splitAmong contains {pid!r} not in config.people"
                    )

    return warnings


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Migrate Ramzan Fund data from V1 (name-based) to V2 (ID-based + snapshots)."
    )
    parser.add_argument(
        "--config", default="config.json",
        help="Path to config.json (default: config.json)"
    )
    parser.add_argument(
        "--data", default="data.json",
        help="Path to data.json (default: data.json)"
    )
    parser.add_argument(
        "--dry-run", action="store_true",
        help="Print what would change without writing any files"
    )
    parser.add_argument(
        "--no-backup", action="store_true",
        help="Skip creating .bak backup files"
    )
    args = parser.parse_args()

    config_path = args.config
    data_path = args.data

    # ── load ─────────────────────────────────────────────────────────────────
    if not os.path.exists(config_path):
        print(f"ERROR: config file not found: {config_path}")
        raise SystemExit(1)
    if not os.path.exists(data_path):
        print(f"ERROR: data file not found: {data_path}")
        raise SystemExit(1)

    with open(config_path, encoding="utf-8") as f:
        config = json.load(f)
    with open(data_path, encoding="utf-8") as f:
        data = json.load(f)

    print("=" * 60)
    print("  Ramzan Fund – V1 → V2 Migration")
    print("=" * 60)
    print(f"  config : {os.path.abspath(config_path)}")
    print(f"  data   : {os.path.abspath(data_path)}")
    if args.dry_run:
        print("  mode   : DRY RUN (no files will be written)")
    print()

    # ── migrate ───────────────────────────────────────────────────────────────
    print("[ config.json ]")
    config, person_name_to_id, bill_name_to_id = migrate_config(config)

    print()
    print("[ data.json ]")
    data = migrate_data(data, person_name_to_id, bill_name_to_id)

    # ── validate ──────────────────────────────────────────────────────────────
    print()
    warnings = validate(config, data)
    if warnings:
        print("[ WARNINGS ]")
        for w in warnings:
            print(w)
    else:
        print("[ Validation ] ✅ No issues found")

    # ── write ─────────────────────────────────────────────────────────────────
    if args.dry_run:
        print()
        print("Dry-run complete. Re-run without --dry-run to apply changes.")
        return

    print()
    if not args.no_backup:
        cfg_bak = _backup(config_path)
        dat_bak = _backup(data_path)
        print(f"  Backup created : {cfg_bak}")
        print(f"  Backup created : {dat_bak}")

    with open(config_path, "w", encoding="utf-8") as f:
        f.write(_dump_config(config))
        f.write("\n")
    print(f"  Written        : {config_path}")

    with open(data_path, "w", encoding="utf-8") as f:
        f.write(_dump_data(data))
        f.write("\n")
    print(f"  Written        : {data_path}")

    print()
    print("Migration complete. Run generate_public.py to rebuild the public site.")


if __name__ == "__main__":
    main()
