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
        default_data = {
            "people": {p: 0 for p in config["people"]},
            "billTypes": {b["name"]: 0 for b in config["billTypes"]},
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
    all_people = config["people"]
    finance = {p: {'credits': 0, 'debits': 0, 'net_balance': 0} for p in all_people}
    total_credits = 0
    total_debits = 0
    enriched_txs = []

    for tx in data['transactions']:
        tx['display_date'] = format_local_time(tx['date'], config["timeOffset"])
        enriched_txs.append(tx)

        if tx['type'] == 'credit':
            total_credits += tx['amount']
            if tx['whoOrBill'] in finance:
                finance[tx['whoOrBill']]['credits'] += tx['amount']
                finance[tx['whoOrBill']]['net_balance'] += tx['amount']
        elif tx['type'] == 'debit':
            total_debits += tx['amount']
            exemptions = tx.get('exemptions', [])
            contributors = [p for p in all_people if p not in exemptions]
            if contributors:
                split_amount = tx['amount'] / len(contributors)
                for p in contributors:
                    finance[p]['debits'] += split_amount
                    finance[p]['net_balance'] -= split_amount

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

    # Prepare chart data (use bill types from config)
    bill_type_names = [bt["name"] for bt in config["billTypes"]]
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
