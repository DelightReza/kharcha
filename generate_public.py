#!/usr/bin/env python3
import json
import os
import datetime
from datetime import timezone, timedelta
from jinja2 import Environment, FileSystemLoader

CONFIG_FILE = 'config.json'
DATA_FILE = 'data.json'
INDEX_FILE = 'index.html'
TRANSACTIONS_DIR = 'transactions'

def load_config():
    with open(CONFIG_FILE, 'r') as f:
        return json.load(f)

def load_data(config):
    # If data.json doesn't exist, create it using config
    if not os.path.exists(DATA_FILE):
        people_config = config["people"]
        if people_config and isinstance(people_config[0], dict):
            people_keys = {p["id"]: 0 for p in people_config}
        else:
            people_keys = {p: 0 for p in people_config}
        bill_types_config = config["billTypes"]
        if bill_types_config and isinstance(bill_types_config[0], dict) and "id" in bill_types_config[0]:
            bill_keys = {bt["id"]: 0 for bt in bill_types_config}
        else:
            bill_keys = {bt["name"]: 0 for bt in bill_types_config}
        default_data = {
            "people": people_keys,
            "billTypes": bill_keys,
            "transactions": []
        }
        with open(DATA_FILE, 'w') as f:
            json.dump(default_data, f, indent=2)
        return default_data
    with open(DATA_FILE, 'r') as f:
        return json.load(f)

def format_utc_offset(offset_hours):
    """Format UTC offset string like 'UTC+6' or 'UTC-5.5'."""
    hours = int(offset_hours)
    minutes = int(abs(offset_hours - hours) * 60)
    sign = "+" if offset_hours >= 0 else "-"
    
    if minutes == 0:
        return f"UTC{sign}{abs(hours)}"
    else:
        return f"UTC{sign}{abs(hours)}.{int(minutes/60*10)}"

def format_local_time(iso_date, offset_hours):
    """Convert ISO date to local time string."""
    try:
        utc_time = datetime.datetime.fromisoformat(iso_date.replace('Z', '+00:00'))
        hours = int(offset_hours)
        minutes = int((offset_hours - hours) * 60)
        local_time = utc_time + timedelta(hours=hours, minutes=minutes)
        return local_time.strftime('%Y-%m-%d %H:%M')
    except:
        return iso_date

def calculate_finance(data, config):
    people_config = config["people"]
    # Build ID-to-name maps (supports both V1 string array and V2 object array)
    if people_config and isinstance(people_config[0], dict):
        id_to_name = {p["id"]: p["name"] for p in people_config}
        all_person_ids = [p["id"] for p in people_config]
    else:
        id_to_name = {p: p for p in people_config}
        all_person_ids = list(people_config)

    bill_types_config = config.get("billTypes", [])
    if bill_types_config and isinstance(bill_types_config[0], dict) and "id" in bill_types_config[0]:
        bt_id_to_name = {bt["id"]: bt["name"] for bt in bill_types_config}
    else:
        bt_id_to_name = {bt["name"]: bt["name"] for bt in bill_types_config}

    # finance keyed by display NAME for template rendering
    finance = {id_to_name[p_id]: {'credits': 0, 'debits': 0, 'net_balance': 0} for p_id in all_person_ids}
    total_credits = 0
    total_debits = 0
    enriched_txs = []

    for tx in data['transactions']:
        tx_copy = tx.copy()
        tx_copy['display_date'] = format_local_time(tx['date'], config["timeOffset"])
        # Resolve whoOrBill ID to display name
        if tx['type'] == 'credit':
            tx_copy['whoOrBill'] = id_to_name.get(tx['whoOrBill'], tx['whoOrBill'])
        else:
            tx_copy['whoOrBill'] = bt_id_to_name.get(tx['whoOrBill'], tx['whoOrBill'])
        # Resolve splitAmong IDs to display names for templates
        if tx.get('splitAmong'):
            tx_copy['splitAmong'] = [id_to_name.get(p, p) for p in tx['splitAmong']]
        enriched_txs.append(tx_copy)

        if tx['type'] == 'credit':
            total_credits += tx['amount']
            person_name = id_to_name.get(tx['whoOrBill'], tx['whoOrBill'])
            if person_name in finance:
                finance[person_name]['credits'] += tx['amount']
                finance[person_name]['net_balance'] += tx['amount']
        elif tx['type'] == 'debit':
            total_debits += tx['amount']
            # Use splitAmong snapshot (V2) or fall back to exemptions (V1)
            if tx.get('splitAmong'):
                paying_ids = tx['splitAmong']
            else:
                exemptions = tx.get('exemptions', [])
                paying_ids = [p_id for p_id in all_person_ids if p_id not in exemptions]
            if paying_ids:
                split_amount = tx['amount'] / len(paying_ids)
                for p_id in paying_ids:
                    p_name = id_to_name.get(p_id, p_id)
                    if p_name in finance:
                        finance[p_name]['debits'] += split_amount
                        finance[p_name]['net_balance'] -= split_amount

    enriched_txs.sort(key=lambda x: x['date'], reverse=True)
    return {
        'finance': finance,
        'totals': {
            'credits': total_credits,
            'debits': total_debits,
            'balance': total_credits - total_debits
        },
        'transactions': enriched_txs
    }

def smart_write(filepath, content):
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            existing = f.read()
        if existing == content:
            return False
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    return True

def main():
    config = load_config()
    env = Environment(loader=FileSystemLoader('templates'))
    data = load_data(config)
    results = calculate_finance(data, config)

    # Prepare chart data (use bill types from config, handle V2 ID-based keys)
    bill_types_config = config["billTypes"]
    if bill_types_config and isinstance(bill_types_config[0], dict) and "id" in bill_types_config[0]:
        bill_type_names = [bt["name"] for bt in bill_types_config]
        bill_type_totals = [data.get("billTypes", {}).get(bt["id"], 0) for bt in bill_types_config]
    else:
        bill_type_names = [bt["name"] for bt in bill_types_config]
        bill_type_totals = [data.get("billTypes", {}).get(name, 0) for name in bill_type_names]

    # Time calculations
    utc_now = datetime.datetime.now(timezone.utc)
    offset = config["timeOffset"]
    hours = int(offset)
    minutes = int((offset - hours) * 60)
    local_now = utc_now + timedelta(hours=hours, minutes=minutes)
    
    offset_str = format_utc_offset(offset)
    dashboard_time = local_now.strftime('%Y-%m-%d %H:%M ') + offset_str
    
    site_subtitle = config.get("siteSubtitle", "House Fund")

    # Dashboard
    template = env.get_template('dashboard.html')
    output = template.render(
        site_title=config["siteTitle"],
        site_subtitle=site_subtitle,
        currency=config["currency"],
        last_updated=dashboard_time,
        total_credits=results['totals']['credits'],
        total_debits=results['totals']['debits'],
        balance=results['totals']['balance'],
        personal_finance=results['finance'],
        transactions=results['transactions'],
        chart_labels=bill_type_names,
        chart_data=bill_type_totals,
        bill_types=config["billTypes"]
    )
    with open(INDEX_FILE, 'w', encoding='utf-8') as f:
        f.write(output)

    os.makedirs(TRANSACTIONS_DIR, exist_ok=True)

    # Individual transaction pages
    tx_template = env.get_template('transaction.html')
    grouped = {}
    written = skipped = 0
    for tx in results['transactions']:
        tx_html = tx_template.render(
            tx=tx,
            currency=config["currency"],
            site_title=config["siteTitle"],
            site_subtitle=site_subtitle,
            last_updated=f"{tx['display_date']} {offset_str}"
        )
        path = f"{TRANSACTIONS_DIR}/{tx['id']}.html"
        if smart_write(path, tx_html):
            written += 1
        else:
            skipped += 1
        if 'parentId' in tx:
            grouped.setdefault(tx['parentId'], []).append(tx)

    # Group pages
    group_template = env.get_template('transaction_group.html')
    g_written = g_skipped = 0
    for gid, txs in grouped.items():
        credit = sum(t['amount'] for t in txs if t['type'] == 'credit')
        debit = sum(t['amount'] for t in txs if t['type'] == 'debit')
        html = group_template.render(
            group_id=gid,
            transactions=txs,
            total_credit=credit,
            total_debit=debit,
            currency=config["currency"],
            site_title=config["siteTitle"],
            site_subtitle=site_subtitle,
            last_updated=f"{txs[0]['display_date']} {offset_str}"
        )
        path = f"{TRANSACTIONS_DIR}/{gid}.html"
        if smart_write(path, html):
            g_written += 1
        else:
            g_skipped += 1

    print(f"Dashboard written. Transactions: {written} new, {skipped} unchanged. Groups: {g_written} new, {g_skipped} unchanged.")

if __name__ == "__main__":
    main()
