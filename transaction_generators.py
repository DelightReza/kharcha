# transaction_generators.py
"""Functions to generate individual transaction pages."""

import os
import datetime
from datetime import timedelta
from config import TRANSACTIONS_DIR, ALL_PEOPLE
from templates_transaction import TRANSACTION_TEMPLATE, PARENT_TRANSACTION_TEMPLATE
from calculations import calculate_bill_distribution_with_exemptions


def generate_transaction_pages(data):
    """Generate all transaction detail pages."""
    os.makedirs(TRANSACTIONS_DIR, exist_ok=True)
    
    # Sort transactions by date for balance calculation
    sorted_tx = sorted(data['transactions'], key=lambda x: x['date'])
    
    # Group transactions by parentId
    multi_transaction_groups = {}
    for tx in sorted_tx:
        if 'parentId' in tx:
            parent_id = tx['parentId']
            if parent_id not in multi_transaction_groups:
                multi_transaction_groups[parent_id] = []
            multi_transaction_groups[parent_id].append(tx)
    
    # Generate individual transaction pages
    running_balance = 0
    for tx in sorted_tx:
        balance_before = running_balance
        
        if tx['type'] == 'credit':
            balance_after = running_balance + tx['amount']
            net_change = f"+{tx['amount']:,.2f}"
            color_scheme = 'green'
            type_label = 'From Person'
            title = f"Credited by {tx['whoOrBill']} - Kharcha"
        else:
            balance_after = running_balance - tx['amount']
            net_change = f"-{tx['amount']:,.2f}"
            color_scheme = 'red'
            type_label = 'Bill Type'
            title = f"Debited for {tx['whoOrBill']} - Kharcha"
        
        running_balance = balance_after
        
        # Generate transaction HTML
        tx_html = generate_single_transaction_html(
            tx, balance_before, balance_after, net_change,
            color_scheme, type_label, title,
            multi_transaction_groups
        )
        
        # Write file
        with open(f'{TRANSACTIONS_DIR}/{tx["id"]}.html', 'w') as f:
            f.write(tx_html)
    
    # Generate parent transaction group pages
    for parent_id, group_transactions in multi_transaction_groups.items():
        parent_html = generate_parent_transaction_html(
            parent_id, group_transactions, sorted_tx
        )
        
        with open(f'{TRANSACTIONS_DIR}/{parent_id}.html', 'w') as f:
            f.write(parent_html)


def generate_single_transaction_html(tx, balance_before, balance_after, net_change,
                                     color_scheme, type_label, title, multi_transaction_groups):
    """Generate HTML for a single transaction page."""
    # Format date
    utc_time = datetime.datetime.fromisoformat(tx['date'].replace('Z', '+00:00'))
    utc6_time = utc_time + timedelta(hours=6)
    formatted_date = utc6_time.strftime('%Y-%m-%d %H:%M:%S')
    
    type_class = 'text-green-600' if tx['type'] == 'credit' else 'text-red-600'
    type_icon = '<i class="fas fa-arrow-down mr-1"></i>' if tx['type'] == 'credit' else '<i class="fas fa-arrow-up mr-1"></i>'
    
    # Build optional sections
    multiple_transaction_notice = ''
    multiple_transactions_section = ''
    parent_id_display = ''
    owner_distribution_display = ''
    bill_exemption_display = ''
    
    # Owner distribution
    if 'owner' in tx and 'parentId' in tx:
        owner_distribution_display = f'''
        <div class="bg-gradient-to-r from-purple-50 to-pink-50 p-4 rounded-xl border border-purple-200">
          <div class="flex justify-between items-center">
            <span class="text-sm font-medium text-gray-700">Distribution Source:</span>
            <span class="font-medium text-purple-600">Owner distribution of {tx["owner"]:,.2f} SOM</span>
          </div>
        </div>'''
    
    # Bill exemptions
    if tx['type'] == 'debit' and 'exemptions' in tx and tx['exemptions']:
        distribution = calculate_bill_distribution_with_exemptions(
            tx['amount'], tx['exemptions'], ALL_PEOPLE
        )
        
        bill_exemption_display = f'''
        <div class="bg-gradient-to-r from-orange-50 to-amber-50 p-4 rounded-xl border border-orange-200">
          <h4 class="font-medium text-orange-700 mb-2 flex items-center">
            <i class="fas fa-user-slash mr-2 text-orange-600"></i>Bill Exemptions (This Transaction Only)
          </h4>
          <div class="text-sm text-gray-700 space-y-1">
            <div><span class="font-medium">Exempt people:</span> {', '.join(distribution['exempt_people'])}</div>
            <div><span class="font-medium">Paying people:</span> {', '.join(distribution['paying_people'])}</div>
            <div class="font-medium text-orange-600">Amount per person: {distribution['amount_per_person']:,.2f} SOM</div>
            <div class="text-xs text-orange-600 mt-2 flex items-center">
              <i class="fas fa-info-circle mr-1"></i>
              These exemptions apply only to this transaction
            </div>
          </div>
        </div>'''
    
    # Multiple transaction group
    if 'parentId' in tx:
        parent_id = tx['parentId']
        if parent_id in multi_transaction_groups:
            group_transactions = multi_transaction_groups[parent_id]
            
            multiple_transaction_notice = f'''
            <div class="bg-gradient-to-r from-blue-50 to-cyan-50 border border-blue-200 rounded-2xl p-6 mb-6 animate-fade-in">
              <div class="flex items-center">
                <i class="fas fa-layer-group text-blue-500 mr-4 text-2xl bg-blue-100 p-3 rounded-xl"></i>
                <div>
                  <h3 class="font-bold text-blue-800">Multiple Transaction Group</h3>
                  <p class="text-sm text-blue-600 mt-1">
                    This transaction is part of a group of {len(group_transactions)} transactions processed together.
                    <a href="{parent_id}.html" class="text-blue-700 hover:text-blue-900 underline ml-1 font-medium">View full group</a>
                  </p>
                </div>
              </div>
            </div>'''
            
            parent_id_display = f'''
            <div class="bg-blue-50 p-4 rounded-xl border border-blue-200">
              <div class="text-sm font-medium text-gray-700 mb-1">Group ID</div>
              <div class="text-sm text-blue-600 font-mono">{parent_id}</div>
            </div>'''
            
            # Build transactions table
            transactions_html = ''
            total_credits = 0
            total_debits = 0
            
            for group_tx in sorted(group_transactions, key=lambda x: x['id']):
                group_type_class = 'text-green-600' if group_tx['type'] == 'credit' else 'text-red-600'
                group_type_icon = '<i class="fas fa-arrow-down mr-1"></i>' if group_tx['type'] == 'credit' else '<i class="fas fa-arrow-up mr-1"></i>'
                
                if group_tx['type'] == 'credit':
                    total_credits += group_tx['amount']
                else:
                    total_debits += group_tx['amount']
                
                current_tx_class = 'bg-blue-50 border border-blue-200' if group_tx['id'] == tx['id'] else ''
                
                transactions_html += f'''
                <tr class="border-b border-gray-100 hover:bg-gray-50 cursor-pointer {current_tx_class}" onclick="window.location.href='{group_tx['id']}.html'">
                  <td class="p-4 {group_type_class} font-medium">{group_type_icon}{group_tx['type']}</td>
                  <td class="p-4 font-medium">{group_tx['whoOrBill']}</td>
                  <td class="p-4 text-gray-600">{group_tx.get('note', '-')}</td>
                  <td class="p-4 text-right font-bold {group_type_class}">{group_tx['amount']:,.2f}</td>
                </tr>'''
            
            multiple_transactions_section = f'''
            <div class="bg-white rounded-2xl shadow-lg p-6 mb-6 animate-fade-in">
              <h2 class="text-xl font-bold text-gray-800 mb-4 flex items-center">
                <i class="fas fa-list-ul text-primary-600 mr-3 bg-primary-100 p-2 rounded-xl"></i>
                All Transactions in This Group
              </h2>
              <div class="overflow-x-auto custom-scrollbar">
                <table class="w-full text-sm">
                  <thead class="text-left bg-gradient-to-r from-gray-50 to-gray-100">
                    <tr>
                      <th class="p-4 font-semibold text-gray-700 border-b border-gray-200">Type</th>
                      <th class="p-4 font-semibold text-gray-700 border-b border-gray-200">Person / Bill</th>
                      <th class="p-4 font-semibold text-gray-700 border-b border-gray-200">Note</th>
                      <th class="p-4 font-semibold text-gray-700 border-b border-gray-200 text-right">Amount (SOM)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions_html}
                  </tbody>
                  <tfoot class="bg-gray-50 border-t border-gray-200">
                    <tr>
                      <td class="p-4 font-bold text-gray-700" colspan="3">Total Credits:</td>
                      <td class="p-4 text-right font-bold text-green-600">{total_credits:,.2f}</td>
                    </tr>
                    <tr>
                      <td class="p-4 font-bold text-gray-700" colspan="3">Total Debits:</td>
                      <td class="p-4 text-right font-bold text-red-600">{total_debits:,.2f}</td>
                    </tr>
                    <tr class="border-t border-gray-300">
                      <td class="p-4 font-bold text-gray-800" colspan="3">Net Change:</td>
                      <td class="p-4 text-right font-bold {type_class} text-lg">{total_credits - total_debits:+,.2f}</td>
                    </tr>
                  </tfoot>
                </table>
              </div>
              <p class="text-sm text-gray-500 mt-3 flex items-center">
                <i class="fas fa-info-circle mr-2 text-blue-500"></i>
                Highlighted row shows the current transaction you're viewing
              </p>
            </div>'''
    
    return TRANSACTION_TEMPLATE.format(
        title=title,
        transaction_id=tx['id'],
        type=tx['type'],
        type_class=type_class,
        type_icon=type_icon,
        type_label=type_label,
        whoOrBill=tx['whoOrBill'],
        note=tx.get('note', '<span class="text-gray-400">No note provided</span>'),
        amount=f"{tx['amount']:,.2f}",
        date=formatted_date,
        balance_before=f"{balance_before:,.2f}",
        balance_after=f"{balance_after:,.2f}",
        net_change=net_change,
        color_scheme=color_scheme,
        multiple_transaction_notice=multiple_transaction_notice,
        multiple_transactions_section=multiple_transactions_section,
        parent_id_display=parent_id_display,
        owner_distribution_display=owner_distribution_display,
        bill_exemption_display=bill_exemption_display
    )


def generate_parent_transaction_html(parent_id, group_transactions, sorted_tx):
    """Generate HTML for parent transaction group page."""
    title = "Transaction Group - Kharcha"
    
    # Calculate totals
    total_credits = sum(tx['amount'] for tx in group_transactions if tx['type'] == 'credit')
    total_debits = sum(tx['amount'] for tx in group_transactions if tx['type'] == 'debit')
    net_change = total_credits - total_debits
    net_change_class = 'text-green-600' if net_change >= 0 else 'text-red-600'
    
    # Get date
    group_date_utc = datetime.datetime.fromisoformat(group_transactions[0]['date'].replace('Z', '+00:00'))
    group_date_utc6 = group_date_utc + timedelta(hours=6)
    formatted_group_date = group_date_utc6.strftime('%Y-%m-%d %H:%M:%S')
    
    # Calculate balance impact
    balance_before_group = 0
    temp_balance = 0
    for temp_tx in sorted_tx:
        if temp_tx['id'] == group_transactions[0]['id']:
            balance_before_group = temp_balance
            break
        
        if temp_tx['type'] == 'credit':
            temp_balance += temp_tx['amount']
        else:
            temp_balance -= temp_tx['amount']
    
    balance_after_group = balance_before_group + net_change
    
    # Build transactions HTML
    transactions_html = ''
    for group_tx in sorted(group_transactions, key=lambda x: x['id']):
        group_type_class = 'text-green-600' if group_tx['type'] == 'credit' else 'text-red-600'
        group_type_icon = '<i class="fas fa-arrow-down mr-1"></i>' if group_tx['type'] == 'credit' else '<i class="fas fa-arrow-up mr-1"></i>'
        type_bg = 'bg-green-100 text-green-800' if group_tx['type'] == 'credit' else 'bg-red-100 text-red-800'
        
        transactions_html += f'''
        <tr class="transaction-row border-b border-gray-100 hover:bg-gray-50 cursor-pointer" onclick="window.location.href='{group_tx['id']}.html'">
          <td class="p-4 font-medium text-blue-600 text-sm">{group_tx['id']}</td>
          <td class="p-4">
            <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium {type_bg}">
              {group_type_icon}{group_tx['type']}
            </span>
          </td>
          <td class="p-4 font-semibold text-gray-800">{group_tx['whoOrBill']}</td>
          <td class="p-4 text-gray-600">{group_tx.get('note', '<span class="text-gray-400">-</span>')}</td>
          <td class="p-4 text-right font-bold {group_type_class}">{group_tx['amount']:,.2f}</td>
        </tr>'''
    
    return PARENT_TRANSACTION_TEMPLATE.format(
        title=title,
        parent_id=parent_id,
        transaction_count=len(group_transactions),
        date=formatted_group_date,
        transactions_html=transactions_html,
        total_credits=total_credits,
        total_debits=total_debits,
        net_change=net_change,
        net_change_class=net_change_class,
        balance_before=f"{balance_before_group:,.2f}",
        balance_after=f"{balance_after_group:,.2f}"
    )
