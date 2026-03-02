/**
 * UI rendering and display functions
 */

const UI = {
  // Render the entire dashboard
  renderDashboard() {
    const totals = Calculations.calculateTotals();
    
    // Update summary cards
    DOM.totalCreditsEl.textContent = Utils.formatCurrency(totals.totalCredits);
    DOM.totalDebitsEl.textContent = Utils.formatCurrency(totals.totalDebits);
    DOM.balanceEl.textContent = Utils.formatCurrency(totals.balance);
    
    // Update personal finance summary
    this.renderPersonalFinance();
    
    // Update bill types summary
    this.renderBillTypes();
    
    // Render transactions
    this.renderTransactions();
    
    // Update data preview
    this.updateDataPreview();
  },
  
  // Render personal finance summary
  renderPersonalFinance() {
    const personalFinance = Calculations.calculatePersonalFinance();
    const allPeopleIds = AppState.getAllPeopleIds();
    
    DOM.personalFinanceBody.innerHTML = '';
    DOM.personalBreakdown.innerHTML = '';

    // Render table rows
    allPeopleIds.forEach(personId => {
      const finance = personalFinance[personId];
      const personName = AppState.getPersonName(personId);
      // Safety check in case calculation missed a person
      if (!finance) return;

      const statusClass = finance.netBalance >= 0 ? 'text-green-600' : 'text-red-600';
      const statusIcon = finance.netBalance >= 0 ? 'fa-smile' : 'fa-frown';
      const statusText = finance.netBalance >= 0 ? 'In Credit' : 'In Debit';
      
      const row = document.createElement('tr');
      row.className = 'border-b border-gray-100 hover:bg-gray-50 cursor-pointer';
      row.onclick = () => Modals.showPersonProfile(personId);
      row.innerHTML = `
        <td class="p-4 font-semibold text-gray-800">${personName}</td>
        <td class="p-4 text-right font-medium text-green-600">${Utils.formatCurrency(finance.credits)}</td>
        <td class="p-4 text-right font-medium text-red-600">${Utils.formatCurrency(finance.debits)}</td>
        <td class="p-4 text-right font-bold ${statusClass}">${Utils.formatCurrency(finance.netBalance)}</td>
        <td class="p-4 text-center">
          <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${finance.netBalance >= 0 ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}">
            <i class="fas ${statusIcon} mr-1"></i> ${statusText}
          </span>
        </td>
      `;
      DOM.personalFinanceBody.appendChild(row);
    });

    // Render detailed breakdown cards
    allPeopleIds.forEach(personId => {
      const finance = personalFinance[personId];
      const personName = AppState.getPersonName(personId);
      if (!finance) return;

      const statusClass = finance.netBalance >= 0 ? 'from-green-50 to-emerald-50 border-green-200' : 'from-red-50 to-orange-50 border-red-200';
      const textClass = finance.netBalance >= 0 ? 'text-green-600' : 'text-red-600';
      
      const card = document.createElement('div');
      card.className = `bg-gradient-to-br ${statusClass} rounded-xl p-4 text-center border shadow-sm hover:shadow-md transition-all duration-200`;
      card.innerHTML = `
        <div class="font-bold text-gray-800 mb-3 text-lg">${personName}</div>
        <div class="space-y-2 text-sm">
          <div class="flex justify-between items-center">
            <span class="text-gray-600">Credits:</span>
            <span class="font-medium text-green-600">${Utils.formatCurrency(finance.credits)}</span>
          </div>
          <div class="flex justify-between items-center">
            <span class="text-gray-600">Debits:</span>
            <span class="font-medium text-red-600">${Utils.formatCurrency(finance.debits)}</span>
          </div>
          <div class="border-t border-gray-200 pt-2 mt-2">
            <div class="flex justify-between items-center font-bold">
              <span class="text-gray-700">Net Balance:</span>
              <span class="${textClass}">${Utils.formatCurrency(finance.netBalance)}</span>
            </div>
          </div>
        </div>
      `;
      DOM.personalBreakdown.appendChild(card);
    });
  },
  
  // Render bill types summary
  renderBillTypes() {
    const data = AppState.getData();
    DOM.billTypesSummary.innerHTML = '';
    
    // Use the dynamic bill types from state if available, or just iterate the object keys
    const billTypes = AppState.getBillTypesList();
    
    billTypes.forEach(billType => {
      const amount = data.billTypes[billType] || 0;
      if (amount > 0) {
        const icon = Utils.getBillIcon(billType);
        
        const billCard = document.createElement('div');
        billCard.className = 'bg-gradient-to-br from-red-50 to-orange-50 rounded-xl p-4 text-center border border-red-200 shadow-sm hover:shadow-md transition-all duration-200';
        billCard.innerHTML = `
          <div class="font-semibold text-gray-700 mb-2">${icon} ${billType}</div>
          <div class="text-lg font-bold text-red-600">${Utils.formatCurrency(amount)}</div>
          <div class="text-xs text-gray-500 mt-2">Total spent</div>
        `;
        DOM.billTypesSummary.appendChild(billCard);
      }
    });
  },
  
  // Render transactions with pagination
  renderTransactions() {
    const data = AppState.getData();
    const totalTransactions = data.transactions.length;
    const config = AppState.config;  // get config
    
    if (totalTransactions === 0) {
      DOM.transactionBody.innerHTML = '';
      DOM.noTransactions.classList.remove('hidden');
      DOM.loadMoreContainer.classList.add('hidden');
      DOM.transactionsSummary.textContent = '';
      return;
    } else {
      DOM.noTransactions.classList.add('hidden');
    }

    const currentPage = AppState.getCurrentPage();
    const perPage = config?.transactionsPerPage || 15;  // fallback to 15 if config missing
    const transactionsToShow = data.transactions.slice(0, currentPage * perPage);
    const hasMore = totalTransactions > transactionsToShow.length;

    DOM.transactionsSummary.textContent = `Showing ${transactionsToShow.length} of ${totalTransactions} transactions`;

    let transactionsHTML = '';

    transactionsToShow.forEach(tx => {
      const formattedDate = Utils.formatDate(tx.date);
      const typeClass = tx.type === 'credit' ? 'text-green-600' : 'text-red-600';
      const typeIcon = tx.type === 'credit' ? 
        '<i class="fas fa-arrow-down mr-1"></i>' : 
        '<i class="fas fa-arrow-up mr-1"></i>';
      
      // Resolve whoOrBill ID to display name
      const displayWhoOrBill = tx.type === 'credit'
        ? AppState.getPersonName(tx.whoOrBill)
        : AppState.getBillTypeName(tx.whoOrBill);

      const hasSplitAmong = tx.type === 'debit' && tx.splitAmong && tx.splitAmong.length > 0;
      const splitIcon = hasSplitAmong ? '<i class="fas fa-users ml-1 text-xs" title="Has split snapshot"></i>' : '';
      
      transactionsHTML += `
        <tr class="transaction-row border-b border-gray-100 hover:bg-gray-50 cursor-pointer" onclick="Modals.showTransactionDetail(${JSON.stringify(tx).replace(/"/g, '&quot;')})">
          <td class="p-4 font-medium text-gray-700">${formattedDate}</td>
          <td class="p-4">
            <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${tx.type === 'credit' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}">
              ${typeIcon}${tx.type}${splitIcon}
            </span>
          </td>
          <td class="p-4 font-semibold text-gray-800">${displayWhoOrBill}</td>
          <td class="p-4 text-gray-600">${tx.note || '<span class="text-gray-400">-</span>'}</td>
          <td class="p-4 text-right font-bold ${typeClass}">${Utils.formatCurrency(tx.amount)}</td>
          <td class="p-4 text-center">
            <button onclick="event.stopPropagation(); Modals.showEditTransactionModal(${JSON.stringify(tx).replace(/"/g, '&quot;')})" 
                    class="text-blue-500 hover:text-blue-700 hover:bg-blue-50 p-2 rounded-lg transition-colors duration-200 mr-2" 
                    title="Edit Transaction">
              <i class="fas fa-edit"></i>
            </button>
            <button onclick="event.stopPropagation(); TransactionManager.deleteTransaction('${tx.id}', ${tx.parentId ? `'${tx.parentId}'` : 'null'})" 
                    class="text-red-500 hover:text-red-700 hover:bg-red-50 p-2 rounded-lg transition-colors duration-200" 
                    title="Permanently Delete">
              <i class="fas fa-trash"></i>
            </button>
          </td>
        </tr>
      `;
    });

    DOM.transactionBody.innerHTML = transactionsHTML;

    if (hasMore) {
      DOM.loadMoreContainer.classList.remove('hidden');
    } else {
      DOM.loadMoreContainer.classList.add('hidden');
    }
  },
  
  // Update data preview
  updateDataPreview() {
    const data = AppState.getData();
    DOM.dataPreview.textContent = JSON.stringify(data, null, 2);
  },
  
  // Show/hide transaction status
  showTransactionStatus(message, type = 'info') {
    DOM.transactionStatus.textContent = message;
    DOM.transactionStatus.className = 'mb-6 p-4 rounded-xl text-center text-sm font-medium border animate-slide-up ';
    
    if (type === 'processing') {
      DOM.transactionStatus.classList.add('bg-blue-100', 'text-blue-800', 'border-blue-200');
    } else if (type === 'success') {
      DOM.transactionStatus.classList.add('bg-green-100', 'text-green-800', 'border-green-200');
    } else if (type === 'error') {
      DOM.transactionStatus.classList.add('bg-red-100', 'text-red-800', 'border-red-200');
    }
    
    DOM.transactionStatus.classList.remove('hidden');
  },
  
  hideTransactionStatus() {
    DOM.transactionStatus.classList.add('hidden');
  },
  
  // Update commit status
  updateCommitStatus(message) {
    DOM.commitStatus.textContent = message;
    setTimeout(() => {
      if (!DOM.commitStatus.textContent.includes('PAT') && 
          !DOM.commitStatus.textContent.includes('security') && 
          !DOM.commitStatus.textContent.includes('auto-clears')) {
        DOM.commitStatus.textContent = '';
      }
    }, 5000);
  }
};
