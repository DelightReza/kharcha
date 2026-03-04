// js/admin-modals.js
/**
 * Modal management for transaction details and editing
 */

const Modals = {
  // Show transaction detail modal
  showTransactionDetail(transaction) {
    const { balanceBefore, balanceAfter } = Calculations.calculateRunningBalance(transaction.id);
    const currency = AppState.config?.currency || 'SOM';
    
    const formattedDate = Utils.formatDate(transaction.date);
    const typeClass = transaction.type === 'credit' ? 'text-green-600' : 'text-red-600';
    const typeIcon = transaction.type === 'credit' ? 
      '<i class="fas fa-arrow-down mr-1"></i>' : 
      '<i class="fas fa-arrow-up mr-1"></i>';
    const netChange = transaction.type === 'credit' ? 
      `+${Utils.formatCurrency(transaction.amount)}` : 
      `-${Utils.formatCurrency(transaction.amount)}`;

    // Resolve whoOrBill ID to display name
    const displayWhoOrBill = transaction.type === 'credit'
      ? AppState.getPersonName(transaction.whoOrBill)
      : AppState.getBillTypeName(transaction.whoOrBill);
    
    // Check for new key 'distributionTotal' or legacy key 'owner'
    const distTotal = transaction.distributionTotal || transaction.owner;
    const distributionDisplay = distTotal ? 
      `<div class="flex justify-between items-center bg-purple-50 p-3 rounded-lg border border-purple-200">
        <span class="text-gray-600 text-sm">Distribution Source:</span>
        <span class="font-medium text-purple-600">Distribution of ${Utils.formatCurrency(distTotal)}</span>
      </div>` : '';
    
    let splitDisplay = '';
    if (transaction.type === 'debit' && transaction.splitAmong && transaction.splitAmong.length > 0) {
      const splitNames = transaction.splitAmong.map(id => AppState.getPersonName(id));
      const amountPerPerson = transaction.amount / transaction.splitAmong.length;
      
      splitDisplay = `
        <div class="bg-gradient-to-r from-blue-50 to-indigo-50 p-4 rounded-xl border border-blue-200">
          <h4 class="font-medium text-blue-700 mb-2 flex items-center">
            <i class="fas fa-users mr-2 text-blue-600"></i>Split Among (Transaction Snapshot)
          </h4>
          <div class="text-sm text-gray-700 space-y-1">
            <div><span class="font-medium">Split among:</span> ${splitNames.join(', ')}</div>
            <div class="font-medium text-blue-600">Amount per person: ${amountPerPerson.toFixed(2)} ${currency}</div>
          </div>
        </div>`;
    } else if (transaction.type === 'debit' && transaction.exemptions && transaction.exemptions.length > 0) {
      const distribution = Calculations.calculateBillDistribution(transaction.amount, 
        AppState.getAllPeopleIds().filter(id => !transaction.exemptions.includes(id)));
      
      splitDisplay = `
        <div class="bg-gradient-to-r from-orange-50 to-amber-50 p-4 rounded-xl border border-orange-200">
          <h4 class="font-medium text-orange-700 mb-2 flex items-center">
            <i class="fas fa-user-slash mr-2 text-orange-600"></i>Bill Exemptions (This Transaction Only)
          </h4>
          <div class="text-sm text-gray-700 space-y-1">
            <div><span class="font-medium">Exempt people:</span> ${transaction.exemptions.join(', ')}</div>
            <div><span class="font-medium">Paying people:</span> ${distribution.splitAmong.map(id => AppState.getPersonName(id)).join(', ')}</div>
            <div class="font-medium text-orange-600">Amount per person: ${distribution.amountPerPerson.toFixed(2)} ${currency}</div>
          </div>
        </div>`;
    }
    
    DOM.transactionDetails.innerHTML = `
      <div class="bg-gray-50 p-4 rounded-xl border border-gray-200">
        <label class="block text-sm font-medium text-gray-700 mb-2">Transaction ID</label>
        <div class="text-sm text-gray-900 font-mono">${transaction.id}</div>
      </div>
      
      ${transaction.parentId ? `
      <div class="flex justify-between items-center bg-blue-50 p-3 rounded-lg border border-blue-200">
        <span class="text-gray-600 text-sm">Group ID:</span>
        <span class="font-medium text-blue-600 text-sm">${transaction.parentId}</span>
      </div>` : ''}
      
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="bg-white p-4 rounded-xl border border-gray-200">
          <label class="block text-sm font-medium text-gray-700 mb-2">Type</label>
          <div class="text-sm ${typeClass} font-medium flex items-center">${typeIcon} ${transaction.type}</div>
        </div>
        <div class="bg-white p-4 rounded-xl border border-gray-200">
          <label class="block text-sm font-medium text-gray-700 mb-2">Date</label>
          <div class="text-sm text-gray-900">${formattedDate}</div>
        </div>
      </div>
      
      <div class="bg-white p-4 rounded-xl border border-gray-200">
        <label class="block text-sm font-medium text-gray-700 mb-2">${transaction.type === 'credit' ? 'From Person' : 'Bill Type'}</label>
        <div class="text-sm text-gray-900 font-medium">${displayWhoOrBill}</div>
      </div>
      
      <div class="bg-white p-4 rounded-xl border border-gray-200">
        <label class="block text-sm font-medium text-gray-700 mb-2">Note</label>
        <div class="text-sm text-gray-900">${transaction.note || '<span class="text-gray-400">No note provided</span>'}</div>
      </div>
      
      <div class="bg-white p-4 rounded-xl border border-gray-200">
        <label class="block text-sm font-medium text-gray-700 mb-2">Amount</label>
        <div class="text-2xl font-bold ${typeClass}">${Utils.formatCurrency(transaction.amount)}</div>
      </div>
      
      ${distributionDisplay}
      ${splitDisplay}
      
      <div class="border-t border-gray-200 pt-6 mt-4">
        <h4 class="font-bold text-gray-800 mb-4 flex items-center">
          <i class="fas fa-chart-line text-primary-600 mr-2"></i>Balance Impact
        </h4>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div class="bg-gray-50 p-4 rounded-xl border border-gray-200">
            <div class="text-gray-600 mb-1">Balance Before</div>
            <div class="font-bold text-lg">${Utils.formatCurrency(balanceBefore)}</div>
          </div>
          <div class="bg-gray-50 p-4 rounded-xl border border-gray-200">
            <div class="text-gray-600 mb-1">Balance After</div>
            <div class="font-bold text-lg">${Utils.formatCurrency(balanceAfter)}</div>
          </div>
          <div class="md:col-span-2 bg-gradient-to-r from-primary-50 to-blue-50 p-4 rounded-xl border border-primary-200 mt-2">
            <div class="text-gray-700 font-semibold mb-2">Net Change</div>
            <div class="font-bold ${typeClass} text-2xl">${netChange}</div>
          </div>
        </div>
      </div>
      
      <div class="border-t border-red-200 pt-6 mt-6">
        <h4 class="font-bold text-red-700 mb-4 flex items-center">
          <i class="fas fa-exclamation-triangle mr-2"></i>Danger Zone
        </h4>
        <div class="bg-red-50 border border-red-200 rounded-xl p-4">
          <p class="text-sm text-red-800 mb-4 flex items-start">
            <i class="fas fa-exclamation-circle mr-2 mt-0.5"></i>
            PERMANENTLY deleting this transaction will remove it from all calculations and reports.
            ${transaction.parentId ? 'This is part of a transaction group - deleting will remove the entire group.' : ''}
          </p>
          <button onclick="TransactionManager.deleteTransaction('${transaction.id}', ${transaction.parentId ? `'${transaction.parentId}'` : 'null'}); Modals.hideTransactionModal();" 
            class="w-full bg-red-600 hover:bg-red-700 text-white py-3 px-4 rounded-xl transition-all duration-200 flex items-center justify-center shadow-lg hover:shadow-xl font-medium">
            <i class="fas fa-trash mr-2"></i> Permanently Delete${transaction.parentId ? ' Group' : ''}
          </button>
        </div>
      </div>
    `;
    
    DOM.transactionModal.classList.remove('hidden');
  },
  
  // Hide transaction modal
  hideTransactionModal() {
    DOM.transactionModal.classList.add('hidden');
  },
  
  // Show edit transaction modal
  showEditTransactionModal(transaction) {
    const currency = AppState.config?.currency || 'SOM';
    
    const typeClass = transaction.type === 'credit' ? 'from-green-50 to-emerald-50 border-green-200' : 'from-red-50 to-orange-50 border-red-200';
    const typeIcon = transaction.type === 'credit' ? 'fa-plus-circle text-green-600' : 'fa-minus-circle text-red-600';
    
    // Parse the stored local date string directly for editing
    const formattedDate = transaction.date.split('T')[0];
    const formattedTime = transaction.date.split('T')[1]?.substring(0, 5) || '12:00';
    
    let whoOrBillOptions = '';
    let splitAmongSection = '';
    
    if (transaction.type === 'credit') {
      const activePeople = AppState.getActivePeopleObjects();
      activePeople.forEach(p => {
        const selected = p.id === transaction.whoOrBill ? 'selected' : '';
        whoOrBillOptions += `<option value="${p.id}" ${selected}>👤 ${p.name}</option>`;
      });
    } else {
      const billTypeIds = AppState.getBillTypesList();
      billTypeIds.forEach(btId => {
        const selected = btId === transaction.whoOrBill ? 'selected' : '';
        const icon = Utils.getBillIcon(btId);
        whoOrBillOptions += `<option value="${btId}" ${selected}>${icon} ${AppState.getBillTypeName(btId)}</option>`;
      });
      
      // Compute current exemptions from splitAmong (inverted)
      const activePeople = AppState.getActivePeopleObjects();
      const currentSplitAmong = transaction.splitAmong || [];
      // An active person is "exempt" if they're NOT in splitAmong
      const exemptIds = currentSplitAmong.length > 0
        ? activePeople.filter(p => !currentSplitAmong.includes(p.id)).map(p => p.id)
        : (transaction.exemptions || []);
      
      const exemptionCheckboxes = activePeople.map(p => `
        <label class="flex items-center space-x-2 text-sm">
          <input type="checkbox" value="${p.id}" class="rounded text-red-600 focus:ring-red-500 edit-exemption-checkbox" ${exemptIds.includes(p.id) ? 'checked' : ''}>
          <span>${p.name}</span>
        </label>
      `).join('');
      
      splitAmongSection = `
        <div class="border-t border-red-200 pt-4 mt-4">
          <div class="flex items-center mb-3">
            <input type="checkbox" id="editEnableExemptions" class="mr-3 w-4 h-4 text-red-600 focus:ring-red-500 rounded" ${exemptIds.length > 0 ? 'checked' : ''}>
            <label for="editEnableExemptions" class="text-sm font-medium text-gray-700 flex items-center">
              <i class="fas fa-users mr-2 text-red-600"></i>Customize who splits this bill
            </label>
          </div>
          
          <div id="editExemptionFields" class="${exemptIds.length > 0 ? '' : 'hidden'} space-y-3 bg-white/50 p-4 rounded-xl border border-red-100">
            <div class="mb-3">
              <p class="text-sm font-medium text-gray-700 mb-2">Select people to <strong>exclude</strong> from this bill:</p>
              <div class="grid grid-cols-2 gap-2" id="editExemptionCheckboxes">
                ${exemptionCheckboxes}
              </div>
            </div>
            
            <div class="text-xs text-gray-500 flex items-center bg-red-50 p-2 rounded-lg">
              <i class="fas fa-info-circle mr-2 text-red-500"></i>
              Unchecked people will be in the splitAmong snapshot for this transaction.
            </div>
            
            <div id="editExemptionPreview" class="bg-white border border-red-100 rounded-xl p-3 shadow-inner">
              <h4 class="font-medium text-red-700 mb-2 flex items-center text-sm">
                <i class="fas fa-calculator mr-2 text-red-600"></i>Cost Distribution Preview
              </h4>
              <div class="text-xs text-gray-600" id="editExemptionDetails">
                ${this.updateEditExemptionPreview(transaction.amount, exemptIds)}
              </div>
            </div>
          </div>
        </div>
      `;
    }
    
    DOM.editTransactionForm.innerHTML = `
      <input type="hidden" id="editTransactionId" value="${transaction.id}">
      <input type="hidden" id="editTransactionType" value="${transaction.type}">
      
      <div class="bg-gradient-to-br ${typeClass} border rounded-2xl p-6 shadow-lg">
        <h4 class="font-bold text-lg mb-4 ${transaction.type === 'credit' ? 'text-green-800' : 'text-red-800'} flex items-center">
          <i class="fas ${typeIcon} mr-3 bg-white p-2 rounded-xl"></i>
          Editing ${transaction.type === 'credit' ? 'Credit' : 'Debit'} Transaction
        </h4>
        
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2 flex items-center">
              <i class="fas ${transaction.type === 'credit' ? 'fa-user text-green-600' : 'fa-file-invoice text-red-600'} mr-2"></i>
              ${transaction.type === 'credit' ? 'Person' : 'Bill Type'}
            </label>
            <select id="editWhoOrBill" class="w-full border border-gray-300 rounded-xl p-3 focus:ring-2 focus:ring-${transaction.type === 'credit' ? 'green' : 'red'}-500 focus:border-${transaction.type === 'credit' ? 'green' : 'red'}-500 bg-white shadow-sm transition-all duration-200">
              ${whoOrBillOptions}
            </select>
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2 flex items-center">
              <i class="fas fa-money-bill-wave mr-2 ${transaction.type === 'credit' ? 'text-green-600' : 'text-red-600'}"></i>Amount (${currency})
            </label>
            <input id="editAmount" type="number" step="0.01" value="${transaction.amount}" class="w-full border border-gray-300 rounded-xl p-3 focus:ring-2 focus:ring-${transaction.type === 'credit' ? 'green' : 'red'}-500 focus:border-${transaction.type === 'credit' ? 'green' : 'red'}-500 bg-white shadow-sm transition-all duration-200">
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2 flex items-center">
              <i class="fas fa-sticky-note mr-2 ${transaction.type === 'credit' ? 'text-green-600' : 'text-red-600'}"></i>Note
            </label>
            <textarea id="editNote" class="w-full border border-gray-300 rounded-xl p-3 focus:ring-2 focus:ring-${transaction.type === 'credit' ? 'green' : 'red'}-500 focus:border-${transaction.type === 'credit' ? 'green' : 'red'}-500 bg-white shadow-sm transition-all duration-200" rows="2">${transaction.note || ''}</textarea>
          </div>
          
          <div class="border-t border-${transaction.type === 'credit' ? 'green' : 'red'}-200 pt-4 mt-4">
            <div class="flex items-center mb-3">
              <input type="checkbox" id="editCustomDate" class="mr-3 w-4 h-4 text-${transaction.type === 'credit' ? 'green' : 'red'}-600 focus:ring-${transaction.type === 'credit' ? 'green' : 'red'}-500 rounded" checked>
              <label for="editCustomDate" class="text-sm font-medium text-gray-700 flex items-center">
                <i class="fas fa-calendar-alt mr-2 text-${transaction.type === 'credit' ? 'green' : 'red'}-600"></i>Edit date & time
              </label>
            </div>
            
            <div id="editDateTimeFields" class="space-y-3 bg-white/50 p-4 rounded-xl border border-${transaction.type === 'credit' ? 'green' : 'red'}-100">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                  <label class="block text-xs font-medium text-gray-600 mb-1">Date</label>
                  <input type="date" id="editDate" value="${formattedDate}" class="w-full border border-gray-300 rounded-lg p-2 text-sm focus:ring-1 focus:ring-${transaction.type === 'credit' ? 'green' : 'red'}-500">
                </div>
                <div>
                  <label class="block text-xs font-medium text-gray-600 mb-1">Time</label>
                  <input type="time" id="editTime" value="${formattedTime}" class="w-full border border-gray-300 rounded-lg p-2 text-sm focus:ring-1 focus:ring-${transaction.type === 'credit' ? 'green' : 'red'}-500">
                </div>
              </div>
              <div class="text-xs text-gray-500 flex items-center bg-${transaction.type === 'credit' ? 'green' : 'red'}-50 p-2 rounded-lg">
                <i class="fas fa-info-circle mr-2 text-${transaction.type === 'credit' ? 'green' : 'red'}-500"></i>Leave time empty for 12:00
              </div>
            </div>
          </div>
          
          ${transaction.type === 'debit' ? splitAmongSection : ''}
        </div>
      </div>
    `;
    
    // Add event listeners for edit modal
    this.setupEditModalEventListeners(transaction);
    this.showEditModal();
  },
  
  // Setup event listeners for edit modal
  setupEditModalEventListeners(transaction) {
    // Exemption toggle for debit transactions
    if (transaction.type === 'debit') {
      const editEnableExemptions = document.getElementById('editEnableExemptions');
      const editExemptionFields = document.getElementById('editExemptionFields');
      
      editEnableExemptions.addEventListener('change', function() {
        editExemptionFields.classList.toggle('hidden', !this.checked);
        if (this.checked) {
          Modals.updateEditExemptionPreview();
        }
      });
      
      // Update exemption preview when amount or checkboxes change
      document.getElementById('editAmount').addEventListener('input', () => this.updateEditExemptionPreview());
      document.querySelectorAll('.edit-exemption-checkbox').forEach(checkbox => {
        checkbox.addEventListener('change', () => this.updateEditExemptionPreview());
      });
    }
    
    // Date toggle
    const editCustomDate = document.getElementById('editCustomDate');
    const editDateTimeFields = document.getElementById('editDateTimeFields');
    
    editCustomDate.addEventListener('change', () => {
      editDateTimeFields.classList.toggle('hidden', !editCustomDate.checked);
    });
  },
  
  // Update exemption preview in edit modal
  updateEditExemptionPreview(amount = null, exemptIds = null) {
    const currency = AppState.config?.currency || 'SOM';
    if (!amount) {
      amount = parseFloat(document.getElementById('editAmount').value);
    }
    
    if (!exemptIds) {
      exemptIds = Array.from(document.querySelectorAll('.edit-exemption-checkbox:checked'))
        .map(cb => cb.value);
    }
    
    if (!amount || amount <= 0 || isNaN(amount)) {
      return '<span class="text-gray-400">Enter amount to see distribution</span>';
    }
    
    const activePeople = AppState.getActivePeopleObjects();
    const payingPeople = activePeople.filter(p => !exemptIds.includes(p.id));
    const exemptNames = activePeople.filter(p => exemptIds.includes(p.id)).map(p => p.name);
    
    if (payingPeople.length === 0) {
      return '<span class="text-red-600">⚠️ No one is paying this bill!</span>';
    }
    
    const amountPerPerson = amount / payingPeople.length;
    
    return `
      <div class="mb-2">
        <span class="font-medium">Excluded:</span> ${exemptNames.length > 0 ? exemptNames.join(', ') : 'None'}
      </div>
      <div class="mb-2">
        <span class="font-medium">Split among (${payingPeople.length} people):</span> ${payingPeople.map(p => p.name).join(', ')}
      </div>
      <div class="font-medium text-red-600">
        Amount per person: ${amountPerPerson.toFixed(2)} ${currency}
      </div>
      <div class="text-xs text-red-500 mt-2 flex items-center">
        <i class="fas fa-info-circle mr-1"></i>
        This snapshot is frozen at transaction time
      </div>
    `;
  },
  
  // Show edit modal
  showEditModal() {
    DOM.editTransactionModal.classList.remove('hidden');
  },
  
  // Hide edit modal
  hideEditModal() {
    DOM.editTransactionModal.classList.add('hidden');
  },
  
  // Show person profile modal (accepts person ID)
  showPersonProfile(personId) {
    const data = AppState.getData();
    const personalFinance = Calculations.calculatePersonalFinance();
    const finance = personalFinance[personId];
    const personName = AppState.getPersonName(personId);
    
    if (!finance) {
      console.error('No finance data for person:', personId);
      return;
    }
    
    // Set person name and avatar
    DOM.personProfileName.textContent = personName;
    DOM.personProfileAvatar.textContent = personName.charAt(0).toUpperCase();
    
    // Set color based on balance
    const isPositive = finance.netBalance >= 0;
    DOM.personProfileAvatar.className = `w-12 h-12 rounded-full flex items-center justify-center text-lg font-bold ${
      isPositive ? 'bg-emerald-500 text-white' : 'bg-red-500 text-white'
    }`;
    
    // Set summary
    DOM.personProfileSummary.textContent = `Net Balance: ${Utils.formatCurrency(finance.netBalance)}`;
    
    // Filter transactions for this person
    const personTransactions = [];
    
    data.transactions.forEach(tx => {
      // Add credit transactions where person gave money (match by ID)
      if (tx.type === 'credit' && tx.whoOrBill === personId) {
        personTransactions.push({
          ...tx,
          personRole: 'credit',
          personAmount: tx.amount,
          formattedDate: Utils.formatDate(tx.date)
        });
      }
      
      // Add debit transactions where person shares the cost
      if (tx.type === 'debit') {
        // V2: use splitAmong snapshot; V1: fall back to exemptions
        let payingIds;
        if (tx.splitAmong && tx.splitAmong.length > 0) {
          payingIds = tx.splitAmong;
        } else {
          const exemptions = tx.exemptions || [];
          payingIds = AppState.getAllPeopleIds().filter(id => !exemptions.includes(id));
        }
        
        if (payingIds.includes(personId)) {
          const amountPerPerson = tx.amount / payingIds.length;
          const payingNames = payingIds.map(id => AppState.getPersonName(id));
          const displayBillType = AppState.getBillTypeName(tx.whoOrBill);
          personTransactions.push({
            ...tx,
            whoOrBill: displayBillType,
            personRole: 'debit',
            personAmount: amountPerPerson,
            payingPeople: payingNames,
            formattedDate: Utils.formatDate(tx.date)
          });
        }
      }
    });
    
    // Sort by date (most recent first)
    personTransactions.sort((a, b) => new Date(b.date) - new Date(a.date));
    
    // Build the content
    let contentHTML = `
      <div class="space-y-6">
        <!-- Summary Cards -->
        <div class="grid grid-cols-3 gap-4">
          <div class="bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl p-4 border border-green-200">
            <div class="text-xs text-gray-600 mb-1">Credits</div>
            <div class="text-lg font-bold text-green-600">${Utils.formatCurrency(finance.credits)}</div>
          </div>
          <div class="bg-gradient-to-br from-red-50 to-orange-50 rounded-xl p-4 border border-red-200">
            <div class="text-xs text-gray-600 mb-1">Debits</div>
            <div class="text-lg font-bold text-red-600">${Utils.formatCurrency(finance.debits)}</div>
          </div>
          <div class="bg-gradient-to-br ${isPositive ? 'from-emerald-50 to-teal-50 border-emerald-200' : 'from-red-50 to-rose-50 border-red-200'} rounded-xl p-4 border">
            <div class="text-xs text-gray-600 mb-1">Net</div>
            <div class="text-lg font-bold ${isPositive ? 'text-emerald-600' : 'text-red-600'}">${Utils.formatCurrency(finance.netBalance)}</div>
          </div>
        </div>
        
        <!-- Transaction History -->
        <div>
          <h4 class="font-bold text-gray-800 mb-4 flex items-center">
            <i class="fas fa-history text-blue-600 mr-2"></i>Transaction History (${personTransactions.length} transaction${personTransactions.length === 1 ? '' : 's'})
          </h4>
          
          ${personTransactions.length === 0 ? `
            <div class="text-center py-8 text-gray-400">
              <i class="fas fa-inbox text-4xl mb-2"></i>
              <p>No transactions found</p>
            </div>
          ` : `
            <div class="space-y-3">
              ${personTransactions.map(tx => {
                const isCredit = tx.personRole === 'credit';
                const typeClass = isCredit ? 'from-green-50 to-emerald-50 border-green-200' : 'from-red-50 to-orange-50 border-red-200';
                const amountClass = isCredit ? 'text-green-600' : 'text-red-600';
                const icon = isCredit ? 'fa-arrow-down' : 'fa-arrow-up';
                
                const txId = Utils.escapeHtml(tx.id);
                const noteHtml = tx.note ? Utils.escapeHtml(tx.note) : '';
                const whoOrBillHtml = Utils.escapeHtml(tx.whoOrBill);
                
                return `
                  <div class="bg-gradient-to-r ${typeClass} rounded-xl p-4 border hover:shadow-md transition-all cursor-pointer" data-tx-id="${txId}" onclick="Modals.showTransactionById('${txId}')">
                    <div class="flex items-start justify-between">
                      <div class="flex-1">
                        <div class="flex items-center gap-2 mb-2">
                          <i class="fas ${icon} ${amountClass}"></i>
                          <span class="font-semibold ${amountClass}">${isCredit ? 'Credit' : 'Debit Share'}</span>
                          ${!isCredit ? `<span class="text-xs text-gray-500">(${whoOrBillHtml})</span>` : ''}
                        </div>
                        <div class="text-sm text-gray-600 mb-1">
                          <i class="fas fa-calendar text-gray-400 mr-1"></i>${tx.formattedDate}
                        </div>
                        ${tx.note ? `
                          <div class="text-sm text-gray-600 italic">
                            <i class="fas fa-sticky-note text-gray-400 mr-1"></i>${noteHtml}
                          </div>
                        ` : ''}
                        ${!isCredit && tx.payingPeople ? `
                          <div class="text-xs text-gray-500 mt-2">
                            <i class="fas fa-users text-gray-400 mr-1"></i>Split among: ${tx.payingPeople.map(p => Utils.escapeHtml(p)).join(', ')}
                          </div>
                        ` : ''}
                      </div>
                      <div class="text-right ml-4">
                        <div class="text-xl font-bold ${amountClass}">
                          ${isCredit ? '+' : '-'}${Utils.formatCurrency(tx.personAmount)}
                        </div>
                        <div class="text-xs text-gray-500">
                          ${!isCredit ? `of ${Utils.formatCurrency(tx.amount)}` : 'total'}
                        </div>
                      </div>
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          `}
        </div>
      </div>
    `;
    
    DOM.personProfileContent.innerHTML = contentHTML;
    DOM.personProfileModal.classList.remove('hidden');
  },
  
  // Hide person profile modal
  hidePersonProfileModal() {
    DOM.personProfileModal.classList.add('hidden');
  },
  
  // Show transaction by ID (helper for person profile)
  showTransactionById(txId) {
    const data = AppState.getData();
    const transaction = data.transactions.find(tx => tx.id === txId);
    if (transaction) {
      this.showTransactionDetail(transaction);
    }
  }
};
