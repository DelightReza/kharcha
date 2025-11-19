#!/usr/bin/env python3
import json
import os
import datetime
from datetime import timezone, timedelta
from jinja2 import Environment, FileSystemLoader

# Configuration
DATA_FILE = 'data.json'
INDEX_FILE = 'index.html'
TRANSACTIONS_DIR = 'transactions'

def load_data():
    with open(DATA_FILE, 'r') as f:
        return json.load(f)

def format_utc6(iso_date):
    """Helper to format date string to readable UTC+6"""
    try:
        utc_time = datetime.datetime.fromisoformat(iso_date.replace('Z', '+00:00'))
        utc6_time = utc_time + timedelta(hours=6)
        return utc6_time.strftime('%Y-%m-%d %H:%M')
    except:
        return iso_date

def calculate_finance(data):
    # Dynamic People List (Single Source of Truth)
    all_people = set(data.get('people', {}).keys())
    
    # Initialize logic
    finance = {p: {'credits': 0, 'debits': 0, 'net_balance': 0} for p in all_people}
    total_credits = 0
    total_debits = 0
    
    enriched_txs = []

    for tx in data['transactions']:
        tx['display_date'] = format_utc6(tx['date'])
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
                    if p in finance:
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

def main():
    # Setup Jinja2
    env = Environment(loader=FileSystemLoader('templates'))
    
    # Load Data
    data = load_data()
    results = calculate_finance(data)
    
    # Prepare Chart Data
    bill_types = data.get('billTypes', {})
    chart_labels = list(bill_types.keys())
    chart_data = list(bill_types.values())
    
    # Generate Dashboard
    print("Generating Dashboard...")
    template = env.get_template('dashboard.html')
    
    utc_now = datetime.datetime.now(timezone.utc) + timedelta(hours=6)
    last_updated = utc_now.strftime('%Y-%m-%d %H:%M UTC+6')

    output = template.render(
        last_updated=last_updated,
        total_credits=results['totals']['credits'],
        total_debits=results['totals']['debits'],
        balance=results['totals']['balance'],
        personal_finance=results['finance'],
        transactions=results['transactions'],
        chart_labels=chart_labels,
        chart_data=chart_data
    )
    
    with open(INDEX_FILE, 'w') as f:
        f.write(output)

    # Generate Transaction Pages
    print("Generating Transaction Pages...")
    os.makedirs(TRANSACTIONS_DIR, exist_ok=True)
    tx_template = env.get_template('transaction.html')
    
    for tx in results['transactions']:
        tx_html = tx_template.render(tx=tx, last_updated=last_updated)
        with open(f"{TRANSACTIONS_DIR}/{tx['id']}.html", 'w') as f:
            f.write(tx_html)

    print("✅ Build Complete.")

if __name__ == "__main__":
    main()
