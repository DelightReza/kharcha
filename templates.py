# templates.py
"""HTML template strings."""

# Main dashboard template
DASHBOARD_TEMPLATE = '''<!DOCTYPE html>
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

    <!-- Transaction History -->
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
        {no_transactions_message}
      </div>
      {load_more_button}
    </div>

    <!-- Personal Finance Summary -->
    <div class="bg-white rounded-2xl shadow-lg p-6 mb-8 animate-fade-in">
      <div class="flex flex-col lg:flex-row justify-between items-start lg:items-center mb-6 gap-4">
        <div>
          <h2 class="text-xl lg:text-2xl font-bold text-gray-800 flex items-center">
            <i class="fas fa-user-circle text-primary-600 mr-3 bg-primary-100 p-2 rounded-xl"></i>
            Personal Finance Summary
          </h2>
          <p class="text-gray-600 text-sm mt-2">Detailed breakdown for each person</p>
        </div>
      </div>

      <div class="overflow-x-auto custom-scrollbar">
        <table class="w-full text-sm">
          <thead class="text-left bg-gradient-to-r from-gray-50 to-gray-100 sticky top-0">
            <tr>
              <th class="p-4 font-semibold text-gray-700 border-b border-gray-200">Person</th>
              <th class="p-4 font-semibold text-gray-700 border-b border-gray-200 text-right">Total Credits</th>
              <th class="p-4 font-semibold text-gray-700 border-b border-gray-200 text-right">Total Debits</th>
              <th class="p-4 font-semibold text-gray-700 border-b border-gray-200 text-right">Net Balance</th>
              <th class="p-4 font-semibold text-gray-700 border-b border-gray-200 text-center">Status</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            {personal_finance_table}
          </tbody>
        </table>
      </div>

      <div class="mt-8 border-t border-gray-200 pt-6">
        <h3 class="text-lg font-bold text-gray-800 mb-4 flex items-center">
          <i class="fas fa-chart-bar text-primary-600 mr-3"></i>
          Detailed Per-Person Breakdown
        </h3>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {personal_finance_cards}
        </div>
      </div>
    </div>

    <!-- Bill Types Summary -->
    <div class="bg-white rounded-2xl shadow-lg p-6 mb-8 animate-fade-in">
      <h2 class="text-xl font-bold text-gray-800 mb-4 flex items-center">
        <i class="fas fa-file-invoice-dollar text-primary-600 mr-3 bg-primary-100 p-2 rounded-xl"></i>
        Bill Types Summary
      </h2>
      <div class="grid grid-cols-2 md:grid-cols-3 gap-4">
        {bill_types_summary}
      </div>
    </div>

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
      
      document.getElementById('transactionsSummary').textContent = 
        'Showing ' + shownCount + ' of ' + totalTransactions + ' transactions';
      
      const allRows = document.querySelectorAll('#transactionBody tr');
      allRows.forEach((row, index) => {{
        if (index < shownCount) {{
          row.style.display = 'table-row';
        }} else {{
          row.style.display = 'none';
        }}
      }});
      
      if (shownCount >= totalTransactions) {{
        document.getElementById('loadMoreContainer').classList.add('hidden');
      }}
    }}
  </script>
</body>
</html>'''


# Transaction detail template will be in templates_transaction.py
# to keep file sizes manageable
