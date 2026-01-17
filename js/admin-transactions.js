// js/admin-transactions.js
/**
 * Transaction management functions (add, edit, delete)
 */

const TransactionManager = {
  // Add a single transaction
  addTransaction(type) {
    let amount, whoOrBill, note, transactionDate;
    let button, processingMessage, successMessage;
    let exemptions = [];

    if (type === 'credit') {
      amount = parseFloat(DOM.creditAmount.value);
      whoOrBill = DOM.personSelect.value;
      note = DOM.creditNote.value;
      button = DOM.addCreditBtn;
      processingMessage = "🔄 Adding credit transaction...";
      successMessage = "✅ Credit added successfully!";
      
      if (!amount || amount <= 0 || isNaN(amount)) {
        UI.showTransactionStatus('❌ Please enter a valid amount', 'error');
        setTimeout(() => UI.hideTransactionStatus(), 3000);
        return;
      }
      
      // Get date (custom or current)
      if (DOM.creditCustomDate.checked) {
        if (!DOM.creditDate.value) {
          UI.showTransactionStatus('❌ Please select a date', 'error');
          setTimeout(() => UI.hideTransactionStatus(), 3000);
          return;
        }
        transactionDate = Utils.localToUTC(DOM.creditDate.value, DOM.creditTime.value);
      } else {
        transactionDate = new Date().toISOString();
      }
    } else {
      amount = parseFloat(DOM.debitAmount.value);
      whoOrBill = DOM.debitType.value;
      note = DOM.debitNote.value;
      button = DOM.addDebitBtn;
      processingMessage = "🔄 Adding debit transaction...";
      successMessage = "✅ Debit added successfully!";
      
      if (!amount || amount <= 0 || isNaN(amount)) {
        UI.showTransactionStatus('❌ Please enter a valid amount', 'error');
        setTimeout(() => UI.hideTransactionStatus(), 3000);
        return;
      }
      
      // Get exemptions for this transaction only
      if (DOM.enableExemptions.checked) {
        exemptions = Array.from(document.querySelectorAll('.exemption-checkbox:checked'))
          .map(cb => cb.value);
      }
      
      // Get date (custom or current)
      if (DOM.debitCustomDate.checked) {
        if (!DOM.debitDate.value) {
          UI.showTransactionStatus('❌ Please select a date', 'error');
          setTimeout(() => UI.hideTransactionStatus(), 3000);
          return;
        }
        transactionDate = Utils.localToUTC(DOM.debitDate.value, DOM.debitTime.value);
      } else {
        transactionDate = new Date().toISOString();
      }
    }

    // Show processing state
    UI.showTransactionStatus(processingMessage, 'processing');
    button.disabled = true;
    button.innerHTML = '<div class="loading-spinner mr-2"></div> Adding...';

    // Use delay to show loading animation
    setTimeout(() => {
      try {
        // Create transaction
        const transaction = {
          id: Utils.generateTransactionId(),
          type,
          whoOrBill,
          note: note || '',
          amount,
          date: transactionDate
        };

        // Add exemptions to debit transactions
        if (type === 'debit' && exemptions && exemptions.length > 0) {
          transaction.exemptions = exemptions;
        }

        // Update data
        const data = AppState.getData();
        data.transactions.unshift(transaction);
        
        if (type === 'credit') {
          data.people[whoOrBill] = (data.people[whoOrBill] || 0) + amount;
        } else {
          data.billTypes[whoOrBill] = (data.billTypes[whoOrBill] || 0) + amount;
        }

        // Save and render
        DataManager.saveData();
        UI.renderDashboard();
        
        // Show success message
        UI.showTransactionStatus(successMessage, 'success');
        
        // Reset button state
        button.disabled = false;
        button.innerHTML = type === 'credit'
          ? '<i class="fas fa-plus-circle mr-2"></i> Add Credit'
          : '<i class="fas fa-minus-circle mr-2"></i> Add Debit';

        // Clear form fields
        this.clearTransactionForm(type);

        // Hide success message after 3 seconds
        setTimeout(() => UI.hideTransactionStatus(), 3000);
      } catch (error) {
        console.error('Error adding transaction:', error);
        UI.showTransactionStatus('❌ Error adding transaction: ' + error.message, 'error');
        
        // Reset button state
        button.disabled = false;
        button.innerHTML = type === 'credit'
          ? '<i class="fas fa-plus-circle mr-2"></i> Add Credit'
          : '<i class="fas fa-minus-circle mr-2"></i> Add Debit';
          
        setTimeout(() => UI.hideTransactionStatus(), 5000);
      }
    }, 500);
  },
  
  // Clear transaction form
  clearTransactionForm(type) {
    if (type === 'credit') {
      DOM.creditAmount.value = '';
      DOM.creditNote.value = '';
      DOM.creditCustomDate.checked = false;
      DOM.creditDateTimeFields.classList.add('hidden');
    } else {
      DOM.debitAmount.value = '';
      DOM.debitNote.value = '';
      DOM.debitCustomDate.checked = false;
      DOM.debitDateTimeFields.classList.add('hidden');
      DOM.enableExemptions.checked = false;
      DOM.exemptionFields.classList.add('hidden');
      DOM.exemptionPreview.classList.add('hidden');
      document.querySelectorAll('.exemption-checkbox').forEach(checkbox => {
        checkbox.checked = false;
      });
    }
  },
  
  // Delete transaction
  deleteTransaction(transactionId, parentId = null) {
    if (!confirm('Are you sure you want to PERMANENTLY delete this transaction? This action cannot be undone.')) {
      return;
    }

    UI.showTransactionStatus('🔄 Deleting transaction...', 'processing');

    setTimeout(() => {
      try {
        const data = AppState.getData();
        
        if (parentId) {
          // Delete entire group
          const transactionsToDelete = data.transactions.filter(tx => tx.parentId === parentId);
            
          transactionsToDelete.forEach(tx => {
            if (tx.type === 'credit') {
              data.people[tx.whoOrBill] = (data.people[tx.whoOrBill] || 0) - tx.amount;
            } else {
              data.billTypes[tx.whoOrBill] = (data.billTypes[tx.whoOrBill] || 0) - tx.amount;
            }
          });
          
          data.transactions = data.transactions.filter(tx => tx.parentId !== parentId);
        } else {
          // Delete single transaction
          const transaction = data.transactions.find(tx => tx.id === transactionId);
          if (transaction) {
            if (transaction.type === 'credit') {
              data.people[transaction.whoOrBill] = (data.people[transaction.whoOrBill] || 0) - transaction.amount;
            } else {
              data.billTypes[transaction.whoOrBill] = (data.billTypes[transaction.whoOrBill] || 0) - transaction.amount;
            }
            
            data.transactions = data.transactions.filter(tx => tx.id !== transactionId);
          }
        }

        DataManager.saveData();
        UI.renderDashboard();
        
        UI.showTransactionStatus('✅ Transaction permanently deleted!', 'success');
        setTimeout(() => UI.hideTransactionStatus(), 3000);
      } catch (error) {
        UI.showTransactionStatus('❌ Error deleting transaction', 'error');
        console.error('Delete error:', error);
      }
    }, 800);
  },
  
  // Distribute money
  distributeMoney() {
    const amount = parseFloat(DOM.distributionAmount.value);
    const note = DOM.distributionNote.value.trim();
    
    if (!amount || amount <= 0 || isNaN(amount)) {
      UI.showTransactionStatus('❌ Please enter a valid amount', 'error');
      setTimeout(() => UI.hideTransactionStatus(), 3000);
      return;
    }
    
    const allPeople = AppState.getPeopleList();
    const amountPerPerson = amount / allPeople.length;
    const transactionDate = new Date().toISOString();
    const baseTransactionId = Utils.generateTransactionId('tx_dist');
    
    UI.showTransactionStatus('🔄 Distributing money...', 'processing');
    DOM.addDistributionBtn.disabled = true;
    DOM.addDistributionBtn.innerHTML = '<div class="loading-spinner mr-2"></div> Distributing...';
    
    setTimeout(() => {
      try {
        const data = AppState.getData();
        
        allPeople.forEach((person, index) => {
          const transaction = {
            id: `${baseTransactionId}_${index}`,
            type: 'credit',
            whoOrBill: person,
            note: note || 'From distribution',
            amount: amountPerPerson,
            date: transactionDate,
            parentId: baseTransactionId,
            distributionTotal: amount // Renamed from 'owner'
          };
          
          data.transactions.unshift(transaction);
          data.people[person] = (data.people[person] || 0) + amountPerPerson;
        });
        
        DataManager.saveData();
        UI.renderDashboard();
        
        UI.showTransactionStatus(`✅ ${amount.toFixed(2)} SOM distributed equally among ${allPeople.length} people (${amountPerPerson.toFixed(2)} each)`, 'success');
        
        DOM.distributionAmount.value = '';
        DOM.distributionNote.value = '';
        DOM.distributionPreview.classList.add('hidden');
        
      } catch (error) {
        UI.showTransactionStatus('❌ Error distributing money', 'error');
        console.error('Distribution error:', error);
      }
      
      DOM.addDistributionBtn.disabled = false;
      DOM.addDistributionBtn.innerHTML = '<i class="fas fa-crown mr-2"></i> Distribute';
      
      setTimeout(() => UI.hideTransactionStatus(), 5000);
    }, 800);
  },

  // Save edited transaction
  saveEditedTransaction() {
    const transactionId = document.getElementById('editTransactionId').value;
    const transactionType = document.getElementById('editTransactionType').value;
    const whoOrBill = document.getElementById('editWhoOrBill').value;
    const amount = parseFloat(document.getElementById('editAmount').value);
    const note = document.getElementById('editNote').value.trim();
    const customDateChecked = document.getElementById('editCustomDate').checked;
    const date = customDateChecked ? document.getElementById('editDate').value : '';
    const time = customDateChecked ? document.getElementById('editTime').value : '';
    
    if (!amount || amount <= 0 || isNaN(amount)) {
      UI.showTransactionStatus('❌ Please enter a valid amount', 'error');
      setTimeout(() => UI.hideTransactionStatus(), 3000);
      return;
    }
    
    if (!whoOrBill) {
      UI.showTransactionStatus(`❌ Please select a ${transactionType === 'credit' ? 'person' : 'bill type'}`, 'error');
      setTimeout(() => UI.hideTransactionStatus(), 3000);
      return;
    }
    
    // Find the transaction
    const data = AppState.getData();
    const transactionIndex = data.transactions.findIndex(tx => tx.id === transactionId);
    if (transactionIndex === -1) {
      UI.showTransactionStatus('❌ Transaction not found', 'error');
      setTimeout(() => UI.hideTransactionStatus(), 3000);
      return;
    }
    
    const oldTransaction = data.transactions[transactionIndex];
    
    // Show processing state
    UI.showTransactionStatus('🔄 Updating transaction...', 'processing');
    DOM.saveEditTransaction.disabled = true;
    DOM.saveEditTransaction.innerHTML = '<div class="loading-spinner mr-2"></div> Saving...';
    
    setTimeout(() => {
      try {
        // Calculate date
        let transactionDate;
        if (customDateChecked && date) {
          transactionDate = Utils.localToUTC(date, time);
        } else {
          transactionDate = oldTransaction.date; // Keep original date
        }
        
        // Get exemptions for debit transactions
        let exemptions = [];
        if (transactionType === 'debit') {
          const enableExemptions = document.getElementById('editEnableExemptions');
          if (enableExemptions && enableExemptions.checked) {
            exemptions = Array.from(document.querySelectorAll('.edit-exemption-checkbox:checked'))
              .map(cb => cb.value);
          }
        }
        
        // Update the transaction
        const updatedTransaction = {
          ...oldTransaction,
          whoOrBill: whoOrBill,
          amount: amount,
          note: note,
          date: transactionDate
        };
        
        // Add exemptions if it's a debit
        if (transactionType === 'debit') {
          if (exemptions.length > 0) {
            updatedTransaction.exemptions = exemptions;
          } else {
            delete updatedTransaction.exemptions;
          }
        }
        
        // Update people and bill types totals
        if (oldTransaction.type === 'credit') {
          // Subtract old amount from old person
          data.people[oldTransaction.whoOrBill] = (data.people[oldTransaction.whoOrBill] || 0) - oldTransaction.amount;
          // Add new amount to new person
          data.people[whoOrBill] = (data.people[whoOrBill] || 0) + amount;
        } else {
          // Subtract old amount from old bill type
          data.billTypes[oldTransaction.whoOrBill] = (data.billTypes[oldTransaction.whoOrBill] || 0) - oldTransaction.amount;
          // Add new amount to new bill type
          data.billTypes[whoOrBill] = (data.billTypes[whoOrBill] || 0) + amount;
        }
        
        // Update transaction in array
        data.transactions[transactionIndex] = updatedTransaction;
        
        // Save and render
        DataManager.saveData();
        UI.renderDashboard();
        
        // Show success message
        UI.showTransactionStatus('✅ Transaction updated successfully!', 'success');
        
        // Hide modal
        Modals.hideEditModal();
        
        // Reset button state
        DOM.saveEditTransaction.disabled = false;
        DOM.saveEditTransaction.innerHTML = '<i class="fas fa-save mr-2"></i> Save Changes';
        
        // Hide success message after 3 seconds
        setTimeout(() => UI.hideTransactionStatus(), 3000);
        
      } catch (error) {
        console.error('Error updating transaction:', error);
        UI.showTransactionStatus('❌ Error updating transaction', 'error');
        
        // Reset button state
        DOM.saveEditTransaction.disabled = false;
        DOM.saveEditTransaction.innerHTML = '<i class="fas fa-save mr-2"></i> Save Changes';
        
        // Hide error message after 5 seconds
        setTimeout(() => UI.hideTransactionStatus(), 5000);
      }
    }, 500);
  }
};
