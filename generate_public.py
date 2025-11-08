import json
import datetime
from datetime import timezone, timedelta
import os
import hashlib
import sys
import re
import shutil

# Template version - increment this when you change title generation
template_version = "v2.1"

def cleanup_orphaned_transaction_files(data):
    """Remove HTML files for transactions that no longer exist in data"""
    if not os.path.exists('transactions'):
        return
    
    # Get all current transaction IDs and parent IDs
    transaction_ids = set()
    parent_ids = set()
    
    for tx in data['transactions']:
        transaction_ids.add(tx['id'])
        if 'parentId' in tx:
            parent_ids.add(tx['parentId'])
    
    # Check each HTML file in transactions directory
    for filename in os.listdir('transactions'):
        if filename.endswith('.html'):
            file_id = filename[:-5]  # Remove .html extension
            
            # Keep file if it's a current transaction or parent group
            if file_id not in transaction_ids and file_id not in parent_ids:
                file_path = os.path.join('transactions', filename)
                os.remove(file_path)
                print(f"🗑️  Removed orphaned transaction file: {filename}")

def cleanup_all_generated_files():
    """Remove all generated HTML files to force complete regeneration"""
    # Remove main dashboard
    if os.path.exists('index.html'):
        os.remove('index.html')
        print("🗑️  Removed index.html")
    
    # Remove transactions directory
    if os.path.exists('transactions'):
        shutil.rmtree('transactions')
        print("🗑️  Removed transactions directory")
    
    # Remove hash file to force regeneration
    if os.path.exists('.data_hash'):
        os.remove('.data_hash')
        print("🗑️  Removed .data_hash")
    
    # Remove template version file
    if os.path.exists('.template_version'):
        os.remove('.template_version')
        print("🗑️  Removed .template_version")

def update_existing_transaction_titles(data):
    """Update titles in existing transaction HTML files"""
    if not os.path.exists('transactions'):
        return
    
    updated_count = 0
    for tx in data['transactions']:
        tx_file = f'transactions/{tx["id"]}.html'
        if os.path.exists(tx_file):
            # Generate new title
            if tx['type'] == 'credit':
                new_title = f"Credited by {tx['whoOrBill']} - Kharcha"
            else:
                new_title = f"Debited for {tx['whoOrBill']} - Kharcha"
            
            # Read file content
            try:
                with open(tx_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Update title if it's the old format
                old_title_pattern = r'<title>[^<]*Transaction [^<]* - Kharcha</title>'
                new_title_tag = f'<title>{new_title}</title>'
                
                if re.search(old_title_pattern, content):
                    content = re.sub(old_title_pattern, new_title_tag, content)
                    with open(tx_file, 'w', encoding='utf-8') as f:
                        f.write(content)
                    updated_count += 1
                    print(f"📝 Updated title for {tx['id']}")
            except Exception as e:
                print(f"⚠️  Could not update title for {tx['id']}: {e}")
    
    if updated_count > 0:
        print(f"✅ Updated titles for {updated_count} transaction files")

def main():
    try:
        # Force complete regeneration if template version changed
        current_template_version = None
        if os.path.exists('.template_version'):
            with open('.template_version', 'r') as f:
                current_template_version = f.read().strip()
        
        if current_template_version != template_version:
            print(f"🔄 Template version changed from {current_template_version} to {template_version}")
            print("🔄 Forcing complete regeneration...")
            cleanup_all_generated_files()
            # Store new template version
            with open('.template_version', 'w') as f:
                f.write(template_version)
        
        # Read the data.json file
        with open('data.json', 'r') as f:
            data = json.load(f)
        
        # Calculate data hash including template version
        data_with_version = {
            'data': data,
            'template_version': template_version
        }
        data_hash = hashlib.md5(json.dumps(data_with_version, sort_keys=True).encode()).hexdigest()
        
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
        
        # Clean up orphaned transaction files before generating new ones
        cleanup_orphaned_transaction_files(data)
        
        # Update titles in existing transaction files
        update_existing_transaction_titles(data)
        
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
        
        # Enhanced HTML template with modern design - FIXED JavaScript escaping
        html_template = '''<!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Kharcha — House Fund Tracker</title>
          <script src="https://cdn.tailwindcss.com"></script>
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
          <script>
            tailwind.config = {{
              theme: {{
                extend: {{
                  colors: {{
                    primary: {{
                      50: '#f0f9ff',
                      100: '#e0f2fe',
                      200: '#bae6fd',
                      300: '#7dd3fc',
                      400: '#38bdf8',
                      500: '#0ea5e9',
                      600: '#0284c7',
                      700: '#0369a1',
                      800: '#075985',
                      900: '#0c4a6e',
                    }}
                  }},
                  animation: {{
                    'fade-in': 'fadeIn 0.5s ease-in-out',
                    'slide-up': 'slideUp 0.3s ease-out',
                  }}
                }}
              }}
            }}
          </script>
          <style>
            @keyframes fadeIn {{
              from {{ opacity: 0; transform: translateY(10px); }}
              to {{ opacity: 1; transform: translateY(0); }}
            }}
            @keyframes slideUp {{
              from {{ 
                opacity: 0;
                transform: translateY(20px);
              }}
              to {{ 
                opacity: 1;
                transform: translateY(0);
              }}
            }}
            
            .transaction-row {{
              transition: all 0.2s ease;
            }}
            
            .transaction-row:hover {{
              background-color: #f8fafc;
              transform: scale(1.01);
            }}
            
            .stat-card {{
              position: relative;
              overflow: hidden;
            }}
            
            .stat-card::before {{
              content: '';
              position: absolute;
              top: 0;
              left: 0;
              right: 0;
              height: 4px;
            }}
            
            .credit-card::before {{
              background: linear-gradient(90deg, #10b981, #34d399);
            }}
            
            .debit-card::before {{
              background: linear-gradient(90deg, #ef4444, #f97316);
            }}
            
            .balance-card::before {{
              background: linear-gradient(90deg, #0ea5e9, #3b82f6);
            }}
            
            .custom-scrollbar::-webkit-scrollbar {{
              width: 6px;
            }}
            
            .custom-scrollbar::-webkit-scrollbar-track {{
              background: #f1f5f9;
              border-radius: 3px;
            }}
            
            .custom-scrollbar::-webkit-scrollbar-thumb {{
              background: #cbd5e1;
              border-radius: 3px;
            }}
            
            .custom-scrollbar::-webkit-scrollbar-thumb:hover {{
              background: #94a3b8;
            }}
          </style>
        </head>
        <body class="bg-gradient-to-br from-gray-50 to-blue-50 text-gray-800 min-h-screen">
          <div class="max-w-7xl mx-auto p-4 lg:p-6">
            <!-- Enhanced Header -->
            <header class="text-center mb-8 lg:mb-12 animate-fade-in">
              <div class="bg-gradient-to-r from-primary-600 via-primary-700 to-primary-800 rounded-2xl p-6 lg:p-8 text-white shadow-xl mb-6 relative overflow-hidden">
                <div class="absolute inset-0 bg-black/10"></div>
                <div class="relative z-10">
                  <div class="flex items-center justify-center mb-4">
                    <div class="bg-white/20 p-3 rounded-2xl backdrop-blur-sm">
                      <i class="fas fa-chart-pie text-2xl text-white"></i>
                    </div>
                  </div>
                  <h1 class="text-2xl lg:text-4xl font-bold mb-3">Kharcha — House Fund Tracker</h1>
                  <p class="text-primary-100 text-sm lg:text-base opacity-90 max-w-2xl mx-auto">
                    Track shared household expenses with transparency and beautiful insights
                  </p>
                  <div class="mt-6">
                    <div class="bg-white/20 text-white px-4 py-2 rounded-xl text-sm font-medium backdrop-blur-sm inline-flex items-center">
                      <i class="fas fa-clock mr-2"></i> Last updated: {last_updated}
                    </div>
                  </div>
                </div>
              </div>
            </header>

            <!-- Enhanced Balance Summary Cards -->
            <div class="grid grid-cols-1 md:grid-cols-3 gap-4 lg:gap-6 mb-8 lg:mb-12">
              <div class="stat-card credit-card bg-white rounded-2xl shadow-lg p-6 text-center animate-slide-up border-t-4 border-green-500">
                <div class="flex items-center justify-center mb-4">
                  <div class="bg-green-100 p-3 rounded-2xl shadow-inner">
                    <i class="fas fa-arrow-down text-green-600 text-xl"></i>
                  </div>
                </div>
                <div class="text-2xl lg:text-3xl font-bold text-green-600 mb-2">{total_credits}</div>
                <div class="text-sm text-gray-600 font-medium">Total Credits</div>
                <div class="text-xs text-gray-400 mt-2 flex items-center justify-center">
                  <i class="fas fa-wallet mr-1"></i> Money received
                </div>
              </div>
              
              <div class="stat-card debit-card bg-white rounded-2xl shadow-lg p-6 text-center animate-slide-up border-t-4 border-red-500" style="animation-delay: 0.1s">
                <div class="flex items-center justify-center mb-4">
                  <div class="bg-red-100 p-3 rounded-2xl shadow-inner">
                    <i class="fas fa-arrow-up text-red-600 text-xl"></i>
                  </div>
                </div>
                <div class="text-2xl lg:text-3xl font-bold text-red-600 mb-2">{total_debits}</div>
                <div class="text-sm text-gray-600 font-medium">Total Debits</div>
                <div class="text-xs text-gray-400 mt-2 flex items-center justify-center">
                  <i class="fas fa-receipt mr-1"></i> Bills paid
                </div>
              </div>
              
              <div class="stat-card balance-card bg-white rounded-2xl shadow-lg p-6 text-center animate-slide-up border-t-4 border-primary-500" style="animation-delay: 0.2s">
                <div class="flex items-center justify-center mb-4">
                  <div class="bg-primary-100 p-3 rounded-2xl shadow-inner">
                    <i class="fas fa-balance-scale text-primary-600 text-xl"></i>
                  </div>
                </div>
                <div class="text-2xl lg:text-3xl font-bold text-primary-600 mb-2">{balance}</div>
                <div class="text-sm text-gray-600 font-medium">Current Balance</div>
                <div class="text-xs text-gray-400 mt-2 flex items-center justify-center">
                  <i class="fas fa-piggy-bank mr-1"></i> Available funds
                </div>
              </div>
            </div>

            <!-- Enhanced Transaction History -->
            <div class="bg-white rounded-2xl shadow-lg p-6 mb-8 animate-fade-in">
              <div class="flex flex-col lg:flex-row justify-between items-start lg:items-center mb-6 gap-4">
                <div>
                  <h2 class="text-xl lg:text-2xl font-bold text-gray-800 flex items-center">
                    <i class="fas fa-history text-primary-600 mr-3 bg-primary-100 p-2 rounded-xl"></i>
                    Transaction History
                  </h2>
                  <p class="text-gray-600 text-sm mt-2">All recorded income and expenses</p>
                </div>
                <div class="bg-primary-50 text-primary-700 px-4 py-2 rounded-xl text-sm font-medium border border-primary-200" id="transactionsSummary">
                  Showing {shown_count} of {total_count} transactions
                </div>
              </div>

              <!-- Enhanced Transactions Table -->
              <div class="border border-gray-200 rounded-xl overflow-hidden shadow-sm">
                <div class="overflow-x-auto custom-scrollbar" style="max-height: 600px; overflow-y: auto">
                  <table class="w-full text-sm">
                    <thead class="text-left bg-gradient-to-r from-gray-50 to-gray-100 sticky top-0">
                      <tr>
                        <th class="p-4 font-semibold text-gray-700 border-b border-gray-200">Date (UTC+6)</th>
                        <th class="p-4 font-semibold text-gray-700 border-b border-gray-200">Type</th>
                        <th class="p-4 font-semibold text-gray-700 border-b border-gray-200">Person / Bill</th>
                        <th class="p-4 font-semibold text-gray-700 border-b border-gray-200">Note</th>
                        <th class="p-4 font-semibold text-gray-700 border-b border-gray-200 text-right">Amount (SOM)</th>
                      </tr>
                    </thead>
                    <tbody id="transactionBody" class="divide-y divide-gray-100">
                      {transactions}
                    </tbody>
                  </table>
                </div>

                <!-- No Transactions State -->
                {no_transactions_message}
              </div>

              <!-- Load More Button -->
              {load_more_button}
            </div>

            <!-- Enhanced Quick Stats -->
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 lg:gap-8 mb-8">
              <!-- People Summary -->
              <div class="bg-white rounded-2xl shadow-lg p-6 animate-fade-in">
                <h2 class="text-xl font-bold text-gray-800 mb-4 flex items-center">
                  <i class="fas fa-users text-primary-600 mr-3 bg-primary-100 p-2 rounded-xl"></i>
                  Contributions Summary
                </h2>
                <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                  {people_summary}
                </div>
              </div>

              <!-- Bill Types Summary -->
              <div class="bg-white rounded-2xl shadow-lg p-6 animate-fade-in">
                <h2 class="text-xl font-bold text-gray-800 mb-4 flex items-center">
                  <i class="fas fa-file-invoice-dollar text-primary-600 mr-3 bg-primary-100 p-2 rounded-xl"></i>
                  Bill Types Summary
                </h2>
                <div class="grid grid-cols-2 md:grid-cols-3 gap-4">
                  {bill_types_summary}
                </div>
              </div>
            </div>

            <!-- Enhanced Footer -->
            <footer class="mt-12 text-center text-sm text-gray-500 py-8 border-t border-gray-200 bg-white/50 rounded-2xl">
              <div class="flex items-center justify-center mb-2">
                <i class="fas fa-heart text-red-500 mr-2"></i>
                <p class="font-medium">Kharcha — House Fund Tracker</p>
              </div>
              <p class="text-gray-400">Transparent household expense management</p>
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
        
        # Enhanced transaction detail page template - FIXED JavaScript escaping
        transaction_template = '''<!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>{title}</title>
          <script src="https://cdn.tailwindcss.com"></script>
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
          <script>
            tailwind.config = {{
              theme: {{
                extend: {{
                  colors: {{
                    primary: {{
                      50: '#f0f9ff',
                      100: '#e0f2fe',
                      200: '#bae6fd',
                      300: '#7dd3fc',
                      400: '#38bdf8',
                      500: '#0ea5e9',
                      600: '#0284c7',
                      700: '#0369a1',
                      800: '#075985',
                      900: '#0c4a6e',
                    }}
                  }}
                }}
              }}
            }}
          </script>
        </head>
        <body class="bg-gradient-to-br from-gray-50 to-blue-50 text-gray-800 min-h-screen">
          <div class="max-w-4xl mx-auto p-4 lg:p-6">
            <!-- Enhanced Header -->
            <header class="text-center mb-8 lg:mb-12">
              <div class="bg-gradient-to-r from-primary-600 to-primary-800 rounded-2xl p-6 lg:p-8 text-white shadow-xl mb-6">
                <h1 class="text-2xl lg:text-3xl font-bold mb-3">Transaction Details</h1>
                <p class="text-primary-100 text-sm lg:text-base opacity-90">Kharcha — House Fund Tracker</p>
                <div class="mt-4">
                  <a href="../index.html" class="bg-white/20 hover:bg-white/30 text-white px-4 py-2 rounded-xl transition-all duration-200 flex items-center justify-center text-sm backdrop-blur-sm max-w-xs mx-auto">
                    <i class="fas fa-arrow-left mr-2"></i> Back to Dashboard
                  </a>
                </div>
              </div>
            </header>

            <!-- Multiple Transaction Group Notice -->
            {multiple_transaction_notice}

            <!-- Individual Transaction Information -->
            <div class="bg-white rounded-2xl shadow-lg p-6 mb-6 animate-fade-in">
              <h2 class="text-xl font-bold text-gray-800 mb-4 flex items-center">
                <i class="fas fa-receipt text-primary-600 mr-3 bg-primary-100 p-2 rounded-xl"></i>
                Transaction Information
              </h2>
              <div class="space-y-4">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div class="bg-gray-50 p-4 rounded-xl border border-gray-200">
                    <div class="text-sm font-medium text-gray-700 mb-1">Transaction ID</div>
                    <div class="text-sm text-gray-900 font-mono">{transaction_id}</div>
                  </div>
                  {parent_id_display}
                </div>
                
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div class="bg-gray-50 p-4 rounded-xl border border-gray-200">
                    <div class="text-sm font-medium text-gray-700 mb-1">Type</div>
                    <div class="font-medium {type_class} flex items-center">{type_icon} {type}</div>
                  </div>
                  <div class="bg-gray-50 p-4 rounded-xl border border-gray-200">
                    <div class="text-sm font-medium text-gray-700 mb-1">Date (UTC+6)</div>
                    <div class="font-medium">{date}</div>
                  </div>
                </div>
                
                <div class="bg-gray-50 p-4 rounded-xl border border-gray-200">
                  <div class="text-sm font-medium text-gray-700 mb-1">{type_label}</div>
                  <div class="font-medium text-lg">{whoOrBill}</div>
                </div>
                
                <div class="bg-gray-50 p-4 rounded-xl border border-gray-200">
                  <div class="text-sm font-medium text-gray-700 mb-1">Note</div>
                  <div class="text-gray-900">{note}</div>
                </div>
                
                <div class="bg-gradient-to-r from-{color_scheme}-50 to-{color_scheme}-100 p-4 rounded-xl border border-{color_scheme}-200">
                  <div class="text-sm font-medium text-gray-700 mb-1">Amount</div>
                  <div class="text-2xl font-bold {type_class}">{amount} SOM</div>
                </div>
                
                {owner_distribution_display}
              </div>
            </div>

            <!-- Multiple Transactions Group (if applicable) -->
            {multiple_transactions_section}

            <!-- Balance Impact -->
            <div class="bg-white rounded-2xl shadow-lg p-6 animate-fade-in">
              <h2 class="text-xl font-bold text-gray-800 mb-4 flex items-center">
                <i class="fas fa-chart-line text-primary-600 mr-3 bg-primary-100 p-2 rounded-xl"></i>
                Balance Impact
              </h2>
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div class="bg-gray-50 p-4 rounded-xl border border-gray-200 text-center">
                  <div class="text-sm text-gray-600 mb-2">Balance Before</div>
                  <div class="text-xl font-bold text-gray-800">{balance_before} SOM</div>
                </div>
                <div class="bg-gray-50 p-4 rounded-xl border border-gray-200 text-center">
                  <div class="text-sm text-gray-600 mb-2">Balance After</div>
                  <div class="text-xl font-bold text-gray-800">{balance_after} SOM</div>
                </div>
                <div class="bg-gradient-to-r from-{color_scheme}-50 to-{color_scheme}-100 p-4 rounded-xl border border-{color_scheme}-200 text-center">
                  <div class="text-sm text-gray-700 mb-2 font-medium">Net Change</div>
                  <div class="text-2xl font-bold {type_class}">{net_change} SOM</div>
                </div>
              </div>
            </div>

            <footer class="mt-12 text-center text-sm text-gray-500 py-8 border-t border-gray-200 bg-white/50 rounded-2xl">
              <p>Kharcha — House Fund Tracker</p>
            </footer>
          </div>
        </body>
        </html>'''
        
        # Enhanced parent transaction group page template - FIXED JavaScript escaping
        parent_transaction_template = '''<!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>{title}</title>
          <script src="https://cdn.tailwindcss.com"></script>
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
          <script>
            tailwind.config = {{
              theme: {{
                extend: {{
                  colors: {{
                    primary: {{
                      50: '#f0f9ff',
                      100: '#e0f2fe',
                      200: '#bae6fd',
                      300: '#7dd3fc',
                      400: '#38bdf8',
                      500: '#0ea5e9',
                      600: '#0284c7',
                      700: '#0369a1',
                      800: '#075985',
                      900: '#0c4a6e',
                    }}
                  }}
                }}
              }}
            }}
          </script>
        </head>
        <body class="bg-gradient-to-br from-gray-50 to-blue-50 text-gray-800 min-h-screen">
          <div class="max-w-6xl mx-auto p-4 lg:p-6">
            <!-- Enhanced Header -->
            <header class="text-center mb-8 lg:mb-12">
              <div class="bg-gradient-to-r from-primary-600 to-primary-800 rounded-2xl p-6 lg:p-8 text-white shadow-xl mb-6">
                <h1 class="text-2xl lg:text-3xl font-bold mb-3">Multiple Transaction Group</h1>
                <p class="text-primary-100 text-sm lg:text-base opacity-90">Kharcha — House Fund Tracker</p>
                <div class="mt-4">
                  <a href="../index.html" class="bg-white/20 hover:bg-white/30 text-white px-4 py-2 rounded-xl transition-all duration-200 flex items-center justify-center text-sm backdrop-blur-sm max-w-xs mx-auto">
                    <i class="fas fa-arrow-left mr-2"></i> Back to Dashboard
                  </a>
                </div>
              </div>
            </header>

            <!-- Group Information -->
            <div class="bg-gradient-to-r from-blue-50 to-cyan-50 border border-blue-200 rounded-2xl p-6 mb-6 animate-fade-in">
              <div class="flex items-center">
                <i class="fas fa-layer-group text-blue-500 mr-4 text-2xl bg-blue-100 p-3 rounded-xl"></i>
                <div>
                  <h2 class="text-xl font-bold text-blue-800">Multiple Transaction Group</h2>
                  <p class="text-sm text-blue-600 mt-1">
                    This group contains {transaction_count} transactions processed together on {date}.
                  </p>
                </div>
              </div>
            </div>

            <!-- Group Transactions -->
            <div class="bg-white rounded-2xl shadow-lg p-6 mb-6 animate-fade-in">
              <h2 class="text-xl font-bold text-gray-800 mb-4 flex items-center">
                <i class="fas fa-list-ul text-primary-600 mr-3 bg-primary-100 p-2 rounded-xl"></i>
                All Transactions in This Group
              </h2>
              <div class="overflow-x-auto custom-scrollbar">
                <table class="w-full text-sm">
                  <thead class="text-left bg-gradient-to-r from-gray-50 to-gray-100">
                    <tr>
                      <th class="p-4 font-semibold text-gray-700 border-b border-gray-200">Transaction ID</th>
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
                      <td class="p-4 font-bold text-gray-700" colspan="4">Total Credits:</td>
                      <td class="p-4 text-right font-bold text-green-600">{total_credits:,.2f}</td>
                    </tr>
                    <tr>
                      <td class="p-4 font-bold text-gray-700" colspan="4">Total Debits:</td>
                      <td class="p-4 text-right font-bold text-red-600">{total_debits:,.2f}</td>
                    </tr>
                    <tr class="border-t border-gray-300">
                      <td class="p-4 font-bold text-gray-800" colspan="4">Net Change:</td>
                      <td class="p-4 text-right font-bold {net_change_class} text-lg">{net_change:+,.2f}</td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>

            <!-- Balance Impact -->
            <div class="bg-white rounded-2xl shadow-lg p-6 animate-fade-in">
              <h2 class="text-xl font-bold text-gray-800 mb-4 flex items-center">
                <i class="fas fa-chart-line text-primary-600 mr-3 bg-primary-100 p-2 rounded-xl"></i>
                Balance Impact
              </h2>
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div class="bg-gray-50 p-4 rounded-xl border border-gray-200 text-center">
                  <div class="text-sm text-gray-600 mb-2">Balance Before Group</div>
                  <div class="text-xl font-bold text-gray-800">{balance_before} SOM</div>
                </div>
                <div class="bg-gray-50 p-4 rounded-xl border border-gray-200 text-center">
                  <div class="text-sm text-gray-600 mb-2">Balance After Group</div>
                  <div class="text-xl font-bold text-gray-800">{balance_after} SOM</div>
                </div>
                <div class="bg-gradient-to-r from-blue-50 to-cyan-50 p-4 rounded-xl border border-blue-200 text-center">
                  <div class="text-sm text-gray-700 mb-2 font-medium">Net Change</div>
                  <div class="text-2xl font-bold {net_change_class}">{net_change:+,.2f} SOM</div>
                </div>
              </div>
            </div>

            <footer class="mt-12 text-center text-sm text-gray-500 py-8 border-t border-gray-200 bg-white/50 rounded-2xl">
              <p>Kharcha — House Fund Tracker</p>
            </footer>
          </div>
        </body>
        </html>'''
        
        # Generate enhanced people summary HTML
        people_summary_html = ''
        for person, amount in data['people'].items():
            people_summary_html += f'''
                <div class="bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl p-4 text-center border border-green-200 shadow-sm hover:shadow-md transition-all duration-200">
                  <div class="font-semibold text-gray-700 mb-2">{person}</div>
                  <div class="text-lg font-bold text-green-600">{amount:,.2f} SOM</div>
                  <div class="text-xs text-gray-500 mt-2">Total contributed</div>
                </div>'''
        
        # Generate enhanced bill types summary HTML
        bill_types_summary_html = ''
        for bill_type, amount in data['billTypes'].items():
            if amount > 0:
                icon = '📦'
                if bill_type == 'Electricity': icon = '⚡'
                elif bill_type == 'Water': icon = '💧'
                elif bill_type == 'Gas': icon = '🔥'
                elif bill_type == 'Garbage': icon = '🗑️'
                elif bill_type == 'Internet': icon = '🌐'
                
                bill_types_summary_html += f'''
                <div class="bg-gradient-to-br from-red-50 to-orange-50 rounded-xl p-4 text-center border border-red-200 shadow-sm hover:shadow-md transition-all duration-200">
                  <div class="font-semibold text-gray-700 mb-2">{icon} {bill_type}</div>
                  <div class="text-lg font-bold text-red-600">{amount:,.2f} SOM</div>
                  <div class="text-xs text-gray-500 mt-2">Total spent</div>
                </div>'''
        
        # Show only first 10 transactions initially
        total_transactions = len(transactions_with_balance)
        shown_count = min(10, total_transactions)
        
        # No transactions message
        no_transactions_message = ''
        if total_transactions == 0:
            no_transactions_message = '''
            <div class="text-center py-12">
              <div class="bg-gradient-to-br from-gray-50 to-blue-50 rounded-2xl p-8 max-w-md mx-auto border border-gray-200">
                <div class="bg-white/80 rounded-2xl p-6 shadow-inner">
                  <i class="fas fa-receipt text-5xl text-gray-300 mb-4"></i>
                  <h3 class="text-lg font-semibold text-gray-700 mb-2">No transactions yet</h3>
                  <p class="text-gray-500 text-sm">Transactions will appear here once added</p>
                </div>
              </div>
            </div>'''
        
        # Generate enhanced transactions HTML with UTC+6 time and links
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
            
            # Create enhanced clickable row
            display_style = 'table-row' if i < shown_count else 'none'
            transactions_html += f'''
                <tr class="transaction-row border-b border-gray-100 hover:bg-gray-50 cursor-pointer" onclick="window.location.href='{link_target}'" style="display: {display_style}">
                  <td class="p-4 font-medium text-gray-700">{formatted_date}</td>
                  <td class="p-4">
                    <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium { 'bg-green-100 text-green-800' if tx['type'] == 'credit' else 'bg-red-100 text-red-800' }">
                      {type_icon}{tx['type']}
                    </span>
                  </td>
                  <td class="p-4 font-semibold text-gray-800">{tx['whoOrBill']}</td>
                  <td class="p-4 text-gray-600">{tx.get('note', '<span class="text-gray-400">-</span>')}</td>
                  <td class="p-4 text-right font-bold {type_class}">{tx['amount']:,.2f}</td>
                </tr>'''
        
        # Generate enhanced load more button if there are more than 10 transactions
        load_more_button = ''
        if total_transactions > 10:
            load_more_button = f'''
            <div id="loadMoreContainer" class="text-center mt-6">
              <button onclick="loadMoreTransactions()" class="bg-primary-600 hover:bg-primary-700 text-white px-8 py-3 rounded-xl transition-all duration-200 flex items-center justify-center mx-auto shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 font-medium">
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
            bill_types_summary=bill_types_summary_html,
            transactions=transactions_html,
            load_more_button=load_more_button,
            no_transactions_message=no_transactions_message,
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
                color_scheme = 'green'
                type_label = 'From Person'
                # Create descriptive title for credit
                title = f"Credited by {tx['whoOrBill']} - Kharcha"
            else:
                balance_after = running_balance - tx['amount']
                net_change = f"-{tx['amount']:,.2f}"
                color_scheme = 'red'
                type_label = 'Bill Type'
                # Create descriptive title for debit
                title = f"Debited for {tx['whoOrBill']} - Kharcha"
            
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
            owner_distribution_display = ''
            
            # Check if this is an owner distribution
            if 'owner' in tx and 'parentId' in tx:
                owner_distribution_display = f'''
                <div class="bg-gradient-to-r from-purple-50 to-pink-50 p-4 rounded-xl border border-purple-200">
                  <div class="flex justify-between items-center">
                    <span class="text-sm font-medium text-gray-700">Distribution Source:</span>
                    <span class="font-medium text-purple-600">Owner distribution of {tx["owner"]:,.2f} SOM</span>
                  </div>
                </div>'''
            
            if 'parentId' in tx:
                parent_id = tx['parentId']
                if parent_id in multi_transaction_groups:
                    group_transactions = multi_transaction_groups[parent_id]
                    
                    # Multiple transaction notice
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
                    
                    # Parent ID display
                    parent_id_display = f'''
                    <div class="bg-blue-50 p-4 rounded-xl border border-blue-200">
                      <div class="text-sm font-medium text-gray-700 mb-1">Group ID</div>
                      <div class="text-sm text-blue-600 font-mono">{parent_id}</div>
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
            
            # Generate enhanced transaction detail page
            tx_html = transaction_template.format(
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
                owner_distribution_display=owner_distribution_display
            )
            
            # Write transaction detail page
            with open(f'transactions/{tx["id"]}.html', 'w') as f:
                f.write(tx_html)

        # Generate enhanced parent transaction group pages
        for parent_id, group_transactions in multi_transaction_groups.items():
            # Create descriptive title for parent transaction group
            parent_title = f"Transaction Group - Kharcha"
            
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
            
            # Generate enhanced transactions HTML for the parent page
            parent_transactions_html = ''
            for group_tx in sorted(group_transactions, key=lambda x: x['id']):
                group_type_class = 'text-green-600' if group_tx['type'] == 'credit' else 'text-red-600'
                group_type_icon = '<i class="fas fa-arrow-down mr-1"></i>' if group_tx['type'] == 'credit' else '<i class="fas fa-arrow-up mr-1"></i>'
                
                parent_transactions_html += f'''
                <tr class="transaction-row border-b border-gray-100 hover:bg-gray-50 cursor-pointer" onclick="window.location.href='{group_tx['id']}.html'">
                  <td class="p-4 font-medium text-blue-600 text-sm">{group_tx['id']}</td>
                  <td class="p-4">
                    <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium { 'bg-green-100 text-green-800' if group_tx['type'] == 'credit' else 'bg-red-100 text-red-800' }">
                      {group_type_icon}{group_tx['type']}
                    </span>
                  </td>
                  <td class="p-4 font-semibold text-gray-800">{group_tx['whoOrBill']}</td>
                  <td class="p-4 text-gray-600">{group_tx.get('note', '<span class="text-gray-400">-</span>')}</td>
                  <td class="p-4 text-right font-bold {group_type_class}">{group_tx['amount']:,.2f}</td>
                </tr>'''
            
            # Generate enhanced parent page
            parent_html = parent_transaction_template.format(
                title=parent_title,
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
        
        print("✅ Enhanced public dashboard and transaction pages generated successfully!")
        
    except Exception as e:
        print(f"❌ Error generating HTML files: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
