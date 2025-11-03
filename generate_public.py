import json
import datetime
from datetime import timezone, timedelta
import os
import hashlib
import sys

def main():
    try:
        # Read the data.json file
        with open('data.json', 'r') as f:
            data = json.load(f)
        
        # Calculate data hash to detect changes
        data_hash = hashlib.md5(json.dumps(data, sort_keys=True).encode()).hexdigest()
        
        # Read existing hash if exists
        hash_file = '.data_hash'
        existing_hash = None
        if os.path.exists(hash_file):
            with open(hash_file, 'r') as f:
                existing_hash = f.read().strip()
        
        # If data hasn't changed, exit early
        if existing_hash == data_hash:
            print("Data unchanged. No regeneration needed.")
            return
        
        print("Data changed. Generating HTML files...")
        
        # Store new hash
        with open(hash_file, 'w') as f:
            f.write(data_hash)
        
        # Calculate totals and running balances
        total_credits = 0
        total_debits = 0
        running_balance = 0
        transactions_with_balance = []
        
        # Sort transactions by date (oldest first for balance calculation)
        sorted_transactions = sorted(data['transactions'], key=lambda x: x['date'])
        
        for tx in sorted_transactions:
            if tx['type'] == 'credit':
                total_credits += tx['amount']
                running_balance += tx['amount']
            else:
                total_debits += tx['amount']
                running_balance -= tx['amount']
            
            # Add running balance to transaction
            tx_copy = tx.copy()
            tx_copy['running_balance'] = running_balance
            transactions_with_balance.append(tx_copy)
        
        # Reverse for display (newest first)
        transactions_with_balance.reverse()
        balance = total_credits - total_debits
        
        # Generate HTML for public dashboard
        html_template = '''<!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Kharcha — House Fund Tracker</title>
          <script src="https://cdn.tailwindcss.com"></script>
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        </head>
        <body class="bg-gray-50 text-gray-800 min-h-screen">
          <div class="max-w-6xl mx-auto p-4">
            <header class="text-center mb-8">
              <h1 class="text-3xl font-bold text-indigo-700 mb-2">Kharcha — House Fund Tracker</h1>
              <p class="text-gray-600">Track shared household expenses with transparency</p>
              <p class="text-sm text-gray-500 mt-2">Last updated: {last_updated}</p>
            </header>

            <!-- Balance Summary Cards -->
            <div class="grid md:grid-cols-3 gap-6 mb-8">
              <div class="bg-white rounded-lg shadow-md p-5 text-center">
                <div class="text-2xl font-bold text-green-600">{total_credits}</div>
                <div class="text-sm text-gray-600">Total Credits</div>
              </div>
              <div class="bg-white rounded-lg shadow-md p-5 text-center">
                <div class="text-2xl font-bold text-red-600">{total_debits}</div>
                <div class="text-sm text-gray-600">Total Debits</div>
              </div>
              <div class="bg-white rounded-lg shadow-md p-5 text-center">
                <div class="text-2xl font-bold text-indigo-600">{balance}</div>
                <div class="text-sm text-gray-600">Current Balance</div>
              </div>
            </div>

            <!-- Transaction History -->
            <div class="bg-white rounded-lg shadow-md p-6 mb-8">
              <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-4 gap-4">
                <h2 class="text-xl font-semibold">Transaction History</h2>
                <div class="text-sm text-gray-600" id="transactionsSummary">Showing {shown_count} of {total_count} transactions</div>
              </div>

              <div class="overflow-x-auto" style="max-height: 1000px; overflow-y: auto">
                <table class="w-full text-sm">
                  <thead class="text-left bg-gray-100 sticky top-0">
                    <tr>
                      <th class="p-3 font-medium">Date (UTC+6)</th>
                      <th class="p-3 font-medium">Type</th>
                      <th class="p-3 font-medium">Person / Bill</th>
                      <th class="p-3 font-medium">Note</th>
                      <th class="p-3 font-medium text-right">Amount (SOM)</th>
                    </tr>
                  </thead>
                  <tbody id="transactionBody">
                    {transactions}
                  </tbody>
                </table>
              </div>

              <!-- Load More Button -->
              {load_more_button}
            </div>

            <!-- Amount per Person -->
            <div class="bg-white rounded-lg shadow-md p-6 mb-8">
              <h2 class="text-xl font-semibold mb-4">Contributions Summary</h2>
              <div class="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
                {people_summary}
              </div>
            </div>

            <footer class="mt-8 text-center text-sm text-gray-500">
              <p>Kharcha — House Fund Tracker</p>
            </footer>
          </div>

          <script>
            let currentPage = 1;
            const transactionsPerPage = 10;
            const totalTransactions = {total_count};

            function loadMoreTransactions() {{
              currentPage++;
              const endIndex = currentPage * transactionsPerPage;
              const shownCount = Math.min(endIndex, totalTransactions);
              
              // Update the showing count
              document.getElementById('transactionsSummary').textContent = 
                'Showing ' + shownCount + ' of ' + totalTransactions + ' transactions';
              
              // Show all transactions up to the current page
              const allRows = document.querySelectorAll('#transactionBody tr');
              allRows.forEach((row, index) => {{
                if (index < shownCount) {{
                  row.style.display = 'table-row';
                }} else {{
                  row.style.display = 'none';
                }}
              }});
              
              // Hide load more button if no more transactions
              if (shownCount >= totalTransactions) {{
                document.getElementById('loadMoreContainer').classList.add('hidden');
              }}
            }}
          </script>
        </body>
        </html>'''
        
        # Generate transaction detail page template
        transaction_template = '''<!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Transaction {transaction_id} - Kharcha</title>
          <script src="https://cdn.tailwindcss.com"></script>
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        </head>
        <body class="bg-gray-50 text-gray-800 min-h-screen">
          <div class="max-w-4xl mx-auto p-4">
            <header class="text-center mb-8">
              <h1 class="text-3xl font-bold text-indigo-700 mb-2">Transaction Details</h1>
              <p class="text-gray-600">Kharcha — House Fund Tracker</p>
              <p class="text-sm text-gray-500 mt-2"><a href="../index.html" class="text-indigo-600 hover:underline">← Back to Dashboard</a></p>
            </header>

            <!-- Multiple Transaction Group Notice -->
            {multiple_transaction_notice}

            <!-- Individual Transaction Information -->
            <div class="bg-white rounded-lg shadow-md p-6 mb-6">
              <h2 class="text-xl font-semibold mb-4">Transaction Information</h2>
              <div class="space-y-4">
                <div class="flex justify-between">
                  <span class="text-gray-600">Transaction ID:</span>
                  <span class="font-medium">{transaction_id}</span>
                </div>
                {parent_id_display}
                <div class="flex justify-between">
                  <span class="text-gray-600">Type:</span>
                  <span class="font-medium {type_class}">{type_icon} {type}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-gray-600">Person / Bill:</span>
                  <span class="font-medium">{whoOrBill}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-gray-600">Note:</span>
                  <span class="font-medium">{note}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-gray-600">Amount:</span>
                  <span class="font-medium {type_class}">{amount} SOM</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-gray-600">Date (UTC+6):</span>
                  <span class="font-medium">{date}</span>
                </div>
              </div>
            </div>

            <!-- Multiple Transactions Group (if applicable) -->
            {multiple_transactions_section}

            <!-- Balance Impact -->
            <div class="bg-white rounded-lg shadow-md p-6">
              <h2 class="text-xl font-semibold mb-4">Balance Impact</h2>
              <div class="space-y-4">
                <div class="flex justify-between">
                  <span class="text-gray-600">Balance Before Transaction:</span>
                  <span class="font-medium">{balance_before} SOM</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-gray-600">Balance After Transaction:</span>
                  <span class="font-medium">{balance_after} SOM</span>
                </div>
                <div class="flex justify-between border-t pt-4">
                  <span class="text-gray-600 font-semibold">Net Change:</span>
                  <span class="font-medium {type_class}">{net_change} SOM</span>
                </div>
              </div>
            </div>

            <footer class="mt-8 text-center text-sm text-gray-500">
              <p>Kharcha — House Fund Tracker</p>
            </footer>
          </div>
        </body>
        </html>'''
        
        # Generate parent transaction group page template
        parent_transaction_template = '''<!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Transaction Group {parent_id} - Kharcha</title>
          <script src="https://cdn.tailwindcss.com"></script>
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        </head>
        <body class="bg-gray-50 text-gray-800 min-h-screen">
          <div class="max-w-4xl mx-auto p-4">
            <header class="text-center mb-8">
              <h1 class="text-3xl font-bold text-indigo-700 mb-2">Multiple Transaction Group</h1>
              <p class="text-gray-600">Kharcha — House Fund Tracker</p>
              <p class="text-sm text-gray-500 mt-2"><a href="../index.html" class="text-indigo-600 hover:underline">← Back to Dashboard</a></p>
            </header>

            <!-- Group Information -->
            <div class="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-6">
              <div class="flex items-center">
                <i class="fas fa-layer-group text-blue-500 mr-3 text-xl"></i>
                <div>
                  <h2 class="text-xl font-semibold text-blue-800">Multiple Transaction Group</h2>
                  <p class="text-sm text-blue-600 mt-1">
                    This group contains {transaction_count} transactions processed together on {date}.
                  </p>
                </div>
              </div>
            </div>

            <!-- Group Transactions -->
            <div class="bg-white rounded-lg shadow-md p-6 mb-6">
              <h2 class="text-xl font-semibold mb-4">All Transactions in This Group</h2>
              <div class="overflow-x-auto">
                <table class="w-full text-sm">
                  <thead class="text-left bg-gray-100">
                    <tr>
                      <th class="p-3 font-medium">Transaction ID</th>
                      <th class="p-3 font-medium">Type</th>
                      <th class="p-3 font-medium">Person / Bill</th>
                      <th class="p-3 font-medium">Note</th>
                      <th class="p-3 font-medium text-right">Amount (SOM)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions_html}
                  </tbody>
                  <tfoot class="bg-gray-50 border-t">
                    <tr>
                      <td class="p-3 font-medium" colspan="4">Total Credits:</td>
                      <td class="p-3 text-right font-medium text-green-600">{total_credits:,.2f}</td>
                    </tr>
                    <tr>
                      <td class="p-3 font-medium" colspan="4">Total Debits:</td>
                      <td class="p-3 text-right font-medium text-red-600">{total_debits:,.2f}</td>
                    </tr>
                    <tr class="border-t">
                      <td class="p-3 font-bold" colspan="4">Net Change:</td>
                      <td class="p-3 text-right font-bold {net_change_class}">{net_change:+,.2f}</td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>

            <!-- Balance Impact -->
            <div class="bg-white rounded-lg shadow-md p-6">
              <h2 class="text-xl font-semibold mb-4">Balance Impact</h2>
              <div class="space-y-4">
                <div class="flex justify-between">
                  <span class="text-gray-600">Balance Before Group:</span>
                  <span class="font-medium">{balance_before} SOM</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-gray-600">Balance After Group:</span>
                  <span class="font-medium">{balance_after} SOM</span>
                </div>
                <div class="flex justify-between border-t pt-4">
                  <span class="text-gray-600 font-semibold">Net Change:</span>
                  <span class="font-medium {net_change_class}">{net_change:+,.2f} SOM</span>
                </div>
              </div>
            </div>

            <footer class="mt-8 text-center text-sm text-gray-500">
              <p>Kharcha — House Fund Tracker</p>
            </footer>
          </div>
        </body>
        </html>'''
        
        # Generate people summary HTML
        people_summary_html = ''
        for person, amount in data['people'].items():
            people_summary_html += f'''
                <div class="bg-gray-50 rounded-lg p-4 text-center">
                  <div class="font-medium text-gray-800">{person}</div>
                  <div class="text-lg font-semibold text-green-600 mt-1">{amount:,.2f} SOM</div>
                </div>'''
        
        # Show only first 10 transactions initially
        total_transactions = len(transactions_with_balance)
        shown_count = min(10, total_transactions)
        
        # Generate transactions HTML with UTC+6 time and links
        transactions_html = ''
        for i, tx in enumerate(transactions_with_balance):
            # Convert to UTC+6
            utc_time = datetime.datetime.fromisoformat(tx['date'].replace('Z', '+00:00'))
            utc6_time = utc_time + timedelta(hours=6)
            formatted_date = utc6_time.strftime('%Y-%m-%d %H:%M')
            
            type_class = 'text-green-600' if tx['type'] == 'credit' else 'text-red-600'
            type_icon = '<i class="fas fa-arrow-down mr-1"></i>' if tx['type'] == 'credit' else '<i class="fas fa-arrow-up mr-1"></i>'
            
            # Determine which page to link to
            if 'parentId' in tx:
                # Link to parent page for multiple transactions
                link_target = f'transactions/{tx["parentId"]}.html'
            else:
                # Link to individual transaction page
                link_target = f'transactions/{tx["id"]}.html'
            
            # Create clickable row
            display_style = 'table-row' if i < shown_count else 'none'
            transactions_html += f'''
                <tr class="border-b border-gray-100 hover:bg-gray-50 cursor-pointer" onclick="window.location.href='{link_target}'" style="display: {display_style}">
                  <td class="p-3">{formatted_date}</td>
                  <td class="p-3 {type_class}">{type_icon}{tx['type']}</td>
                  <td class="p-3 font-medium">{tx['whoOrBill']}</td>
                  <td class="p-3 text-gray-600">{tx.get('note', '-')}</td>
                  <td class="p-3 text-right font-medium {type_class}">{tx['amount']:,.2f}</td>
                </tr>'''
        
        # Generate load more button if there are more than 10 transactions
        load_more_button = ''
        if total_transactions > 10:
            load_more_button = f'''
            <div id="loadMoreContainer" class="text-center mt-6">
              <button onclick="loadMoreTransactions()" class="bg-indigo-600 text-white px-6 py-2 rounded-md hover:bg-indigo-700 transition duration-200 flex items-center justify-center mx-auto">
                <i class="fas fa-chevron-down mr-2"></i> Load Older Transactions
              </button>
            </div>'''
        
        # Format numbers with commas and two decimal places
        total_credits_str = f"{total_credits:,.2f}"
        total_debits_str = f"{total_debits:,.2f}"
        balance_str = f"{balance:,.2f}"
        
        # Get current time in UTC+6 for last updated
        utc_now = datetime.datetime.now(timezone.utc)
        utc6_now = utc_now + timedelta(hours=6)
        last_updated = utc6_now.strftime('%Y-%m-%d %H:%M:%S UTC+6')
        
        # Final HTML generation for main dashboard
        final_html = html_template.format(
            last_updated=last_updated,
            total_credits=total_credits_str,
            total_debits=total_debits_str,
            balance=balance_str,
            people_summary=people_summary_html,
            transactions=transactions_html,
            load_more_button=load_more_button,
            shown_count=shown_count,
            total_count=total_transactions
        )
        
        # Write main dashboard
        with open('index.html', 'w') as f:
            f.write(final_html)
        
        # Create transactions directory
        os.makedirs('transactions', exist_ok=True)
        
        # Generate individual transaction pages
        running_balance = 0
        sorted_tx = sorted(data['transactions'], key=lambda x: x['date'])  # Sort by date for balance calculation

        # Group transactions by parentId for multiple transactions
        multi_transaction_groups = {}
        for tx in sorted_tx:
            if 'parentId' in tx:
                parent_id = tx['parentId']
                if parent_id not in multi_transaction_groups:
                    multi_transaction_groups[parent_id] = []
                multi_transaction_groups[parent_id].append(tx)

        for i, tx in enumerate(sorted_tx):
            # Calculate balance before and after this transaction
            balance_before = running_balance
            
            if tx['type'] == 'credit':
                balance_after = running_balance + tx['amount']
                net_change = f"+{tx['amount']:,.2f}"
            else:
                balance_after = running_balance - tx['amount']
                net_change = f"-{tx['amount']:,.2f}"
            
            running_balance = balance_after
            
            # Convert to UTC+6
            utc_time = datetime.datetime.fromisoformat(tx['date'].replace('Z', '+00:00'))
            utc6_time = utc_time + timedelta(hours=6)
            formatted_date = utc6_time.strftime('%Y-%m-%d %H:%M:%S')
            
            type_class = 'text-green-600' if tx['type'] == 'credit' else 'text-red-600'
            type_icon = '<i class="fas fa-arrow-down mr-1"></i>' if tx['type'] == 'credit' else '<i class="fas fa-arrow-up mr-1"></i>'
            
            # Check if this is part of a multiple transaction group
            multiple_transaction_notice = ''
            multiple_transactions_section = ''
            parent_id_display = ''
            
            if 'parentId' in tx:
                parent_id = tx['parentId']
                if parent_id in multi_transaction_groups:
                    group_transactions = multi_transaction_groups[parent_id]
                    
                    # Multiple transaction notice
                    multiple_transaction_notice = f'''
                    <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
                      <div class="flex items-center">
                        <i class="fas fa-layer-group text-blue-500 mr-3 text-xl"></i>
                        <div>
                          <h3 class="font-semibold text-blue-800">Multiple Transaction Group</h3>
                          <p class="text-sm text-blue-600 mt-1">
                            This transaction is part of a group of {len(group_transactions)} transactions processed together.
                            <a href="{parent_id}.html" class="text-blue-700 hover:text-blue-900 underline ml-1">View full group</a>
                          </p>
                        </div>
                      </div>
                    </div>'''
                    
                    # Parent ID display
                    parent_id_display = f'''
                    <div class="flex justify-between">
                      <span class="text-gray-600">Group ID:</span>
                      <span class="font-medium text-blue-600">{parent_id}</span>
                    </div>'''
                    
                    # Multiple transactions section
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
                        
                        transactions_html += f'''
                        <tr class="border-b border-gray-100 {'bg-blue-50' if group_tx['id'] == tx['id'] else ''}">
                          <td class="p-3 {group_type_class}">{group_type_icon}{group_tx['type']}</td>
                          <td class="p-3 font-medium">{group_tx['whoOrBill']}</td>
                          <td class="p-3 text-gray-600">{group_tx.get('note', '-')}</td>
                          <td class="p-3 text-right font-medium {group_type_class}">{group_tx['amount']:,.2f}</td>
                        </tr>'''
                    
                    multiple_transactions_section = f'''
                    <div class="bg-white rounded-lg shadow-md p-6 mb-6">
                      <h2 class="text-xl font-semibold mb-4">All Transactions in This Group</h2>
                      <div class="overflow-x-auto">
                        <table class="w-full text-sm">
                          <thead class="text-left bg-gray-100">
                            <tr>
                              <th class="p-3 font-medium">Type</th>
                              <th class="p-3 font-medium">Person / Bill</th>
                              <th class="p-3 font-medium">Note</th>
                              <th class="p-3 font-medium text-right">Amount (SOM)</th>
                            </tr>
                          </thead>
                          <tbody>
                            {transactions_html}
                          </tbody>
                          <tfoot class="bg-gray-50 border-t">
                            <tr>
                              <td class="p-3 font-medium" colspan="3">Total Credits:</td>
                              <td class="p-3 text-right font-medium text-green-600">{total_credits:,.2f}</td>
                            </tr>
                            <tr>
                              <td class="p-3 font-medium" colspan="3">Total Debits:</td>
                              <td class="p-3 text-right font-medium text-red-600">{total_debits:,.2f}</td>
                            </tr>
                            <tr class="border-t">
                              <td class="p-3 font-bold" colspan="3">Net Change:</td>
                              <td class="p-3 text-right font-bold {type_class}">{total_credits - total_debits:+,.2f}</td>
                            </tr>
                          </tfoot>
                        </table>
                      </div>
                      <p class="text-sm text-gray-500 mt-3">
                        <i class="fas fa-info-circle mr-1"></i>
                        Highlighted row shows the current transaction you're viewing
                      </p>
                    </div>'''
            
            # Generate transaction detail page
            tx_html = transaction_template.format(
                transaction_id=tx['id'],
                type=tx['type'],
                type_class=type_class,
                type_icon=type_icon,
                whoOrBill=tx['whoOrBill'],
                note=tx.get('note', 'No note provided'),
                amount=f"{tx['amount']:,.2f}",
                date=formatted_date,
                balance_before=f"{balance_before:,.2f}",
                balance_after=f"{balance_after:,.2f}",
                net_change=net_change,
                multiple_transaction_notice=multiple_transaction_notice,
                multiple_transactions_section=multiple_transactions_section,
                parent_id_display=parent_id_display
            )
            
            # Write transaction detail page
            with open(f'transactions/{tx["id"]}.html', 'w') as f:
                f.write(tx_html)

        # Generate parent transaction group pages
        for parent_id, group_transactions in multi_transaction_groups.items():
            # Calculate totals for the group
            total_credits = sum(tx['amount'] for tx in group_transactions if tx['type'] == 'credit')
            total_debits = sum(tx['amount'] for tx in group_transactions if tx['type'] == 'debit')
            net_change = total_credits - total_debits
            net_change_class = 'text-green-600' if net_change >= 0 else 'text-red-600'
            
            # Get the date of the first transaction in the group
            group_date_utc = datetime.datetime.fromisoformat(group_transactions[0]['date'].replace('Z', '+00:00'))
            group_date_utc6 = group_date_utc + timedelta(hours=6)
            formatted_group_date = group_date_utc6.strftime('%Y-%m-%d %H:%M:%S')
            
            # Calculate balance before and after the group
            balance_before_group = 0
            balance_after_group = 0
            
            # Find the balance impact by processing transactions until we reach this group
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
            
            # Generate transactions HTML for the parent page
            parent_transactions_html = ''
            for group_tx in sorted(group_transactions, key=lambda x: x['id']):
                group_type_class = 'text-green-600' if group_tx['type'] == 'credit' else 'text-red-600'
                group_type_icon = '<i class="fas fa-arrow-down mr-1"></i>' if group_tx['type'] == 'credit' else '<i class="fas fa-arrow-up mr-1"></i>'
                
                parent_transactions_html += f'''
                <tr class="border-b border-gray-100 hover:bg-gray-50 cursor-pointer" onclick="window.location.href='{group_tx['id']}.html'">
                  <td class="p-3 font-medium text-blue-600">{group_tx['id']}</td>
                  <td class="p-3 {group_type_class}">{group_type_icon}{group_tx['type']}</td>
                  <td class="p-3 font-medium">{group_tx['whoOrBill']}</td>
                  <td class="p-3 text-gray-600">{group_tx.get('note', '-')}</td>
                  <td class="p-3 text-right font-medium {group_type_class}">{group_tx['amount']:,.2f}</td>
                </tr>'''
            
            # Generate parent page
            parent_html = parent_transaction_template.format(
                parent_id=parent_id,
                transaction_count=len(group_transactions),
                date=formatted_group_date,
                transactions_html=parent_transactions_html,
                total_credits=total_credits,
                total_debits=total_debits,
                net_change=net_change,
                net_change_class=net_change_class,
                balance_before=f"{balance_before_group:,.2f}",
                balance_after=f"{balance_after_group:,.2f}"
            )
            
            # Write parent transaction page
            with open(f'transactions/{parent_id}.html', 'w') as f:
                f.write(parent_html)
        
        print("✅ Public dashboard and transaction pages generated successfully!")
        
    except Exception as e:
        print(f"❌ Error generating HTML files: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
