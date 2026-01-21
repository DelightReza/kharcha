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
PEOPLE_DIR = 'people'

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
    # Dynamic People List (Single Source of Truth) - Sorted for consistency
    all_people = sorted(list(data.get('people', {}).keys()))
    
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

def smart_write(filepath, content):
    """Only write the file if content has changed to preserve timestamps/git history."""
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            existing_content = f.read()
        if existing_content == content:
            return False # Skipped
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    return True # Written

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
    
    # Current time for the Dashboard only
    utc_now = datetime.datetime.now(timezone.utc) + timedelta(hours=6)
    dashboard_time = utc_now.strftime('%Y-%m-%d %H:%M UTC+6')

    # --- 1. Generate Dashboard (Always update this) ---
    print(f"Generating Dashboard ({dashboard_time})...")
    template = env.get_template('dashboard.html')
    
    output = template.render(
        last_updated=dashboard_time,
        total_credits=results['totals']['credits'],
        total_debits=results['totals']['debits'],
        balance=results['totals']['balance'],
        personal_finance=results['finance'],
        transactions=results['transactions'],
        chart_labels=chart_labels,
        chart_data=chart_data
    )
    
    with open(INDEX_FILE, 'w', encoding='utf-8') as f:
        f.write(output)

    # Make sure directory exists
    os.makedirs(TRANSACTIONS_DIR, exist_ok=True)

    # --- 2. Generate Individual Transaction Pages ---
    print("Generating Individual Transaction Pages...")
    tx_template = env.get_template('transaction.html')
    
    grouped_transactions = {}
    skipped_count = 0
    written_count = 0

    for tx in results['transactions']:
        # KEY CHANGE: Use the transaction's own date as the "Last Updated" in the header
        # This ensures the HTML content is identical unless the transaction data actually changes.
        tx_header_time = f"{tx['display_date']} UTC+6"

        tx_html = tx_template.render(tx=tx, last_updated=tx_header_time)
        file_path = f"{TRANSACTIONS_DIR}/{tx['id']}.html"
        
        if smart_write(file_path, tx_html):
            written_count += 1
        else:
            skipped_count += 1
        
        # Collect for groups
        if 'parentId' in tx:
            pid = tx['parentId']
            if pid not in grouped_transactions:
                grouped_transactions[pid] = []
            grouped_transactions[pid].append(tx)

    print(f"   - Transactions: {written_count} updated, {skipped_count} skipped.")

    # --- 3. Generate Group Transaction Pages ---
    print("Generating Group Pages...")
    group_template = env.get_template('transaction_group.html')
    
    g_written = 0
    g_skipped = 0

    for group_id, txs in grouped_transactions.items():
        # Calculate group totals
        g_credit = sum(t['amount'] for t in txs if t['type'] == 'credit')
        g_debit = sum(t['amount'] for t in txs if t['type'] == 'debit')
        
        # Use the first transaction's date for the group header time
        group_header_time = f"{txs[0]['display_date']} UTC+6"

        group_html = group_template.render(
            group_id=group_id,
            transactions=txs,
            total_credit=g_credit,
            total_debit=g_debit,
            last_updated=group_header_time
        )
        
        file_path = f"{TRANSACTIONS_DIR}/{group_id}.html"
        if smart_write(file_path, group_html):
            g_written += 1
        else:
            g_skipped += 1

    print(f"   - Groups: {g_written} updated, {g_skipped} skipped.")
    
    # --- 4. Generate Person Profile Pages ---
    print("Generating Person Profile Pages...")
    os.makedirs(PEOPLE_DIR, exist_ok=True)
    
    person_template = env.get_template('person_profile.html')
    all_people = sorted(list(data.get('people', {}).keys()))
    
    p_written = 0
    p_skipped = 0
    
    for person_name in all_people:
        # Get person's finance data
        person_finance = results['finance'].get(person_name, {'credits': 0, 'debits': 0, 'net_balance': 0})
        
        # Filter transactions for this person
        person_transactions = []
        
        for tx in results['transactions']:
            # Add credit transactions where person gave money
            if tx['type'] == 'credit' and tx['whoOrBill'] == person_name:
                person_transactions.append({
                    **tx,
                    'person_role': 'credit',
                    'person_amount': tx['amount']
                })
            
            # Add debit transactions where person shares the cost
            elif tx['type'] == 'debit':
                exemptions = tx.get('exemptions', [])
                contributors = [p for p in all_people if p not in exemptions]
                
                if person_name in contributors:
                    split_amount = tx['amount'] / len(contributors)
                    person_transactions.append({
                        **tx,
                        'person_role': 'debit',
                        'person_amount': split_amount
                    })
        
        # Use the current dashboard time for person pages
        person_html = person_template.render(
            person_name=person_name,
            person_finance=person_finance,
            person_transactions=person_transactions,
            last_updated=dashboard_time
        )
        
        file_path = f"{PEOPLE_DIR}/{person_name}.html"
        if smart_write(file_path, person_html):
            p_written += 1
        else:
            p_skipped += 1
    
    print(f"   - Person Pages: {p_written} updated, {p_skipped} skipped.")
    print("✅ Build Complete.")

if __name__ == "__main__":
    main()
