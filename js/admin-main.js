/**
 * Main initialization and event listener setup
 */

const App = {
  // Initialize the application
  async init() {
    // 1. Load config
    try {
      const resp = await fetch('config.json');
      if (!resp.ok) throw new Error('Config not found');
      AppState.config = await resp.json();
    } catch (e) {
      console.error('Failed to load config, using fallback defaults', e);
      // Fallback – should not happen in production
      AppState.config = {
        siteTitle: 'Fund',
        siteSubtitle: 'Expense Tracker',
        people: ['Raza'],
        billTypes: [
          { name: 'Other', icon: '📦' }
        ],
        currency: 'INR',
        transactionsPerPage: 14
      };
    }

    // Initialize DOM references
    DOM.init();

    // 2. Set Dynamic Site Title and Header
    if (AppState.config.siteTitle) {
      document.title = `${AppState.config.siteTitle} — Admin Panel`;
      const headerTitle = document.getElementById('adminHeaderTitle');
      if (headerTitle) headerTitle.textContent = `${AppState.config.siteTitle} Admin`;
      const footerBrand = document.getElementById('footerBrand');
      if (footerBrand) footerBrand.textContent = `${AppState.config.siteTitle} Admin`;
    }
    
    // Set Dynamic Subtitle
    if (AppState.config.siteSubtitle) {
      const headerSubtitle = document.getElementById('adminHeaderSubtitle');
      if (headerSubtitle) headerSubtitle.textContent = AppState.config.siteSubtitle;
    }

    // 3. Update Currency Placeholders/Labels in the UI
    const currencyElements = document.querySelectorAll('.currency-label');
    currencyElements.forEach(el => {
      el.textContent = AppState.config.currency;
    });

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

    // Settings Modal
    DOM.openSettingsBtn.addEventListener('click', () => {
        UI.renderSettingsLists();
        DOM.settingsModal.classList.remove('hidden');
    });
    DOM.closeSettingsModal.addEventListener('click', () => DOM.settingsModal.classList.add('hidden'));
    
    // Settings Actions
    DOM.addPersonBtn.addEventListener('click', () => UI.addPerson());
    DOM.addBillTypeBtn.addEventListener('click', () => UI.addBillType());
    DOM.saveConfigBtn.addEventListener('click', () => DataManager.commitConfigToGitHub());

    // Modal buttons (Details/Edit)
    DOM.closeTransactionModal.addEventListener('click', () => Modals.hideTransactionModal());
    DOM.closeTransactionModalBtn.addEventListener('click', () => Modals.hideTransactionModal());
    DOM.closeEditTransactionModal.addEventListener('click', () => Modals.hideEditModal());
    DOM.cancelEditTransaction.addEventListener('click', () => Modals.hideEditModal());
    DOM.saveEditTransaction.addEventListener('click', () => TransactionManager.saveEditedTransaction());

    // Person profile modal buttons
    DOM.closePersonProfileModal.addEventListener('click', () => Modals.hidePersonProfileModal());
    DOM.closePersonProfileModalBtn.addEventListener('click', () => Modals.hidePersonProfileModal());

    // Load more button
    DOM.loadMoreBtn.addEventListener('click', () => UI.loadMoreTransactions());

    // Distribution
    DOM.distributionAmount.addEventListener('input', () => UI.updateDistributionPreview());
    DOM.addDistributionBtn.addEventListener('click', () => TransactionManager.distributeMoney());

    // Settlement (Offline)
    DOM.addSettlementBtn.addEventListener('click', () => TransactionManager.addSettlement());

    // Transfer (Balance)
    DOM.addTransferBtn.addEventListener('click', () => TransactionManager.addTransfer());

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
    
    DOM.settingsModal.addEventListener('click', (e) => {
        if (e.target === DOM.settingsModal) {
            DOM.settingsModal.classList.add('hidden');
        }
    });

    DOM.personProfileModal.addEventListener('click', (e) => {
      if (e.target === DOM.personProfileModal) {
        Modals.hidePersonProfileModal();
      }
    });
  }
};

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  App.init();
});
