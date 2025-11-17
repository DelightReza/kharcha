#!/usr/bin/env python3
# generate_public.py
"""Main script to generate public dashboard and transaction pages."""

import json
import sys
import datetime
from datetime import timezone, timedelta

from config import (
    TEMPLATE_VERSION, ALL_PEOPLE, DATA_FILE, INDEX_FILE
)
from utils import (
    cleanup_orphaned_transaction_files,
    cleanup_all_generated_files,
    calculate_data_hash,
    read_existing_hash,
    save_hash,
    read_template_version,
    save_template_version
)
from calculations import (
    calculate_personal_finance,
    calculate_totals_and_balances
)
from templates import DASHBOARD_TEMPLATE
from html_builders import (
    build_personal_finance_table,
    build_personal_finance_cards,
    build_bill_types_summary,
    build_transactions_html,
    build_no_transactions_message,
    build_load_more_button
)
from transaction_generators import generate_transaction_pages


def main():
    """Main function to generate all HTML files."""
    try:
        # Check template version
        current_template_version = read_template_version()
        
        if current_template_version != TEMPLATE_VERSION:
            print(f"🔄 Template version changed from {current_template_version} to {TEMPLATE_VERSION}")
            print("🔄 Forcing complete regeneration...")
            cleanup_all_generated_files()
            save_template_version(TEMPLATE_VERSION)
        
        # Read data
        with open(DATA_FILE, 'r') as f:
            data = json.load(f)
        
        # Calculate data hash
        data_hash = calculate_data_hash(data, TEMPLATE_VERSION)
        existing_hash = read_existing_hash()
        
        # Check if regeneration needed
        if existing_hash == data_hash:
            print("Data unchanged. No regeneration needed.")
            return
        
        print("Data changed. Generating HTML files...")
        
        # Save new hash
        save_hash(data_hash)
        
        # Cleanup orphaned files
        cleanup_orphaned_transaction_files(data)
        
        # Calculate all financial data
        personal_finance = calculate_personal_finance(data, ALL_PEOPLE)
        totals = calculate_totals_and_balances(data)
        
        # Build HTML components
        personal_finance_table = build_personal_finance_table(personal_finance, ALL_PEOPLE)
        personal_finance_cards = build_personal_finance_cards(personal_finance, ALL_PEOPLE)
        bill_types_summary = build_bill_types_summary(data.get('billTypes', {}))
        
        # Transaction display settings
        total_count = len(totals['transactions_with_balance'])
        shown_count = min(10, total_count)
        
        # Build transaction components
        if total_count == 0:
            transactions_html = ''
            no_transactions_message = build_no_transactions_message()
            load_more_button = ''
        else:
            transactions_html = build_transactions_html(
                totals['transactions_with_balance'],
                shown_count
            )
            no_transactions_message = ''
            load_more_button = build_load_more_button() if total_count > 10 else ''
        
        # Get current time
        utc_now = datetime.datetime.now(timezone.utc)
        utc6_now = utc_now + timedelta(hours=6)
        last_updated = utc6_now.strftime('%Y-%m-%d %H:%M:%S UTC+6')
        
        # Generate main dashboard
        final_html = DASHBOARD_TEMPLATE.format(
            last_updated=last_updated,
            total_credits=f"{totals['total_credits']:,.2f}",
            total_debits=f"{totals['total_debits']:,.2f}",
            balance=f"{totals['balance']:,.2f}",
            personal_finance_table=personal_finance_table,
            personal_finance_cards=personal_finance_cards,
            bill_types_summary=bill_types_summary,
            transactions=transactions_html,
            load_more_button=load_more_button,
            no_transactions_message=no_transactions_message,
            shown_count=shown_count,
            total_count=total_count
        )
        
        # Write main dashboard
        with open(INDEX_FILE, 'w') as f:
            f.write(final_html)
        
        # Generate transaction pages
        generate_transaction_pages(data)
        
        print("✅ Enhanced public dashboard and transaction pages generated successfully!")
        
    except Exception as e:
        print(f"❌ Error generating HTML files: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
