# html_builders.py
"""Functions to build HTML components."""

import datetime
from datetime import timedelta


def format_date_utc6(date_string):
    """Convert ISO date string to UTC+6 formatted string."""
    utc_time = datetime.datetime.fromisoformat(date_string.replace('Z', '+00:00'))
    utc6_time = utc_time + timedelta(hours=6)
    return utc6_time.strftime('%Y-%m-%d %H:%M')


def build_personal_finance_table(personal_finance, all_people):
    """Build HTML table rows for personal finance summary."""
    html = ''
    for person in all_people:
        finance = personal_finance[person]
        status_class = 'text-green-600' if finance['net_balance'] >= 0 else 'text-red-600'
        status_icon = 'fa-smile' if finance['net_balance'] >= 0 else 'fa-frown'
        status_text = 'In Credit' if finance['net_balance'] >= 0 else 'In Debit'
        status_bg = 'bg-green-100 text-green-800' if finance['net_balance'] >= 0 else 'bg-red-100 text-red-800'
        
        html += f'''
            <tr class="border-b border-gray-100 hover:bg-gray-50">
              <td class="p-4 font-semibold text-gray-800">{person}</td>
              <td class="p-4 text-right font-medium text-green-600">{finance['credits']:,.2f}</td>
              <td class="p-4 text-right font-medium text-red-600">{finance['debits']:,.2f}</td>
              <td class="p-4 text-right font-bold {status_class}">{finance['net_balance']:,.2f}</td>
              <td class="p-4 text-center">
                <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium {status_bg}">
                  <i class="fas {status_icon} mr-1"></i> {status_text}
                </span>
              </td>
            </tr>'''
    
    return html


def build_personal_finance_cards(personal_finance, all_people):
    """Build HTML cards for personal finance breakdown."""
    html = ''
    for person in all_people:
        finance = personal_finance[person]
        status_class = 'from-green-50 to-emerald-50 border-green-200' if finance['net_balance'] >= 0 else 'from-red-50 to-orange-50 border-red-200'
        text_class = 'text-green-600' if finance['net_balance'] >= 0 else 'text-red-600'
        
        html += f'''
            <div class="bg-gradient-to-br {status_class} rounded-xl p-4 text-center border shadow-sm hover:shadow-md transition-all duration-200">
              <div class="font-bold text-gray-800 mb-3 text-lg">{person}</div>
              <div class="space-y-2 text-sm">
                <div class="flex justify-between items-center">
                  <span class="text-gray-600">Credits:</span>
                  <span class="font-medium text-green-600">{finance['credits']:,.2f}</span>
                </div>
                <div class="flex justify-between items-center">
                  <span class="text-gray-600">Debits:</span>
                  <span class="font-medium text-red-600">{finance['debits']:,.2f}</span>
                </div>
                <div class="border-t border-gray-200 pt-2 mt-2">
                  <div class="flex justify-between items-center font-bold">
                    <span class="text-gray-700">Net Balance:</span>
                    <span class="{text_class}">{finance['net_balance']:,.2f}</span>
                  </div>
                </div>
              </div>
            </div>'''
    
    return html


def build_bill_types_summary(bill_types):
    """Build HTML for bill types summary cards."""
    html = ''
    icon_map = {
        'Electricity': '⚡',
        'Water': '💧',
        'Gas': '🔥',
        'Garbage': '🗑️',
        'Internet': '🌐'
    }
    
    for bill_type, amount in bill_types.items():
        if amount > 0:
            icon = icon_map.get(bill_type, '📦')
            
            html += f'''
            <div class="bg-gradient-to-br from-red-50 to-orange-50 rounded-xl p-4 text-center border border-red-200 shadow-sm hover:shadow-md transition-all duration-200">
              <div class="font-semibold text-gray-700 mb-2">{icon} {bill_type}</div>
              <div class="text-lg font-bold text-red-600">{amount:,.2f} SOM</div>
              <div class="text-xs text-gray-500 mt-2">Total spent</div>
            </div>'''
    
    return html


def build_transactions_html(transactions, shown_count):
    """Build HTML for transaction table rows."""
    html = ''
    
    for i, tx in enumerate(transactions):
        formatted_date = format_date_utc6(tx['date'])
        type_class = 'text-green-600' if tx['type'] == 'credit' else 'text-red-600'
        type_icon = '<i class="fas fa-arrow-down mr-1"></i>' if tx['type'] == 'credit' else '<i class="fas fa-arrow-up mr-1"></i>'
        type_bg = 'bg-green-100 text-green-800' if tx['type'] == 'credit' else 'bg-red-100 text-red-800'
        
        # Determine link target
        if 'parentId' in tx:
            link_target = f'transactions/{tx["parentId"]}.html'
        else:
            link_target = f'transactions/{tx["id"]}.html'
        
        # Check for exemptions
        has_exemptions = tx.get('type') == 'debit' and 'exemptions' in tx and tx['exemptions']
        exemption_icon = '<i class="fas fa-user-slash ml-1 text-xs text-orange-500" title="Has exemptions"></i>' if has_exemptions else ''
        
        display_style = 'table-row' if i < shown_count else 'none'
        
        html += f'''
            <tr class="transaction-row border-b border-gray-100 hover:bg-gray-50 cursor-pointer" onclick="window.location.href='{link_target}'" style="display: {display_style}">
              <td class="p-4 font-medium text-gray-700">{formatted_date}</td>
              <td class="p-4">
                <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium {type_bg}">
                  {type_icon}{tx['type']}{exemption_icon}
                </span>
              </td>
              <td class="p-4 font-semibold text-gray-800">{tx['whoOrBill']}</td>
              <td class="p-4 text-gray-600">{tx.get('note', '<span class="text-gray-400">-</span>')}</td>
              <td class="p-4 text-right font-bold {type_class}">{tx['amount']:,.2f}</td>
            </tr>'''
    
    return html


def build_no_transactions_message():
    """Build HTML for no transactions state."""
    return '''
    <div class="text-center py-12">
      <div class="bg-gradient-to-br from-gray-50 to-blue-50 rounded-2xl p-8 max-w-md mx-auto border border-gray-200">
        <div class="bg-white/80 rounded-2xl p-6 shadow-inner">
          <i class="fas fa-receipt text-5xl text-gray-300 mb-4"></i>
          <h3 class="text-lg font-semibold text-gray-700 mb-2">No transactions yet</h3>
          <p class="text-gray-500 text-sm">Transactions will appear here once added</p>
        </div>
      </div>
    </div>'''


def build_load_more_button():
    """Build HTML for load more button."""
    return '''
    <div id="loadMoreContainer" class="text-center mt-6">
      <button onclick="loadMoreTransactions()" class="bg-primary-600 hover:bg-primary-700 text-white px-8 py-3 rounded-xl transition-all duration-200 flex items-center justify-center mx-auto shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 font-medium">
        <i class="fas fa-chevron-down mr-2"></i> Load Older Transactions
      </button>
    </div>'''
