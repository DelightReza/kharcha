// js/admin-main.js
/**
 * Main initialization and event listener setup
 */

const App = {
  // Initialize the application
  async init() {
    // Initialize DOM references
    DOM.init();
    
    // Initialize dropdowns immediately (with defaults) so the UI isn't empty
    UI.initializeDropdowns();
    UI.setDefaultDates();
    
    // Load saved PAT if exists
    DataManager.loadSavedPAT();
    
    // Load data
    await DataManager.loadDataFromGitHubPages();
    
    // Re-initialize UI components after data load to reflect dynamic People/BillTypes from JSON
    UI.initializeDropdowns();
    UI.initBillExemptions();
    
    // Setup event listeners
    this.setupEventListeners();
    
    // Initial render
    UI.renderDashboard();
  },
  
  // Setup all event listeners
  setupEventListeners() {
    // Transaction buttons
    DOM.addCreditBtn.addEventListener('click', () => TransactionManager.addTransaction('credit'));
    DOM.addDebitBtn.addEventListener('click', () => TransactionManager.addTransaction('debit'));
    
    // GitHub buttons
    DOM.setPATBtn.addEventListener('click', () => DataManager.setPAT());
    DOM.commitBtn.addEventListener('click', () => DataManager.commitToGitHub());
    
    // Modal buttons
    DOM.closeTransactionModal.addEventListener('click', () => Modals.hideTransactionModal());
    DOM.closeTransactionModalBtn.addEventListener('click', () => Modals.hideTransactionModal());
    DOM.closeEditTransactionModal.addEventListener('click', () => Modals.hideEditModal());
    DOM.cancelEditTransaction.addEventListener('click', () => Modals.hideEditModal());
    DOM.saveEditTransaction.addEventListener('click', () => TransactionManager.saveEditedTransaction());
    
    // Load more button
    DOM.loadMoreBtn.addEventListener('click', () => UI.loadMoreTransactions());
    
    // Distribution
    DOM.distributionAmount.addEventListener('input', () => UI.updateDistributionPreview());
    DOM.addDistributionBtn.addEventListener('click', () => TransactionManager.distributeMoney());
    
    // Date/time toggles
    DOM.creditCustomDate.addEventListener('change', () => {
      DOM.creditDateTimeFields.classList.toggle('hidden', !DOM.creditCustomDate.checked);
    });
    
    DOM.debitCustomDate.addEventListener('change', () => {
      DOM.debitDateTimeFields.classList.toggle('hidden', !DOM.debitCustomDate.checked);
    });
    
    // Modal click outside to close
    DOM.transactionModal.addEventListener('click', (e) => {
      if (e.target === DOM.transactionModal) {
        Modals.hideTransactionModal();
      }
    });
    
    DOM.editTransactionModal.addEventListener('click', (e) => {
      if (e.target === DOM.editTransactionModal) {
        Modals.hideEditModal();
      }
    });
  }
};

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  App.init();
});
