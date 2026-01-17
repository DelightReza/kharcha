// js/admin-dom.js
/**
 * DOM element references
 */

const DOM = {
  // Form elements
  personSelect: null,
  creditAmount: null,
  creditNote: null,
  addCreditBtn: null,
  debitType: null,
  debitAmount: null,
  debitNote: null,
  addDebitBtn: null,
  
  // GitHub elements
  setPATBtn: null,
  commitBtn: null,
  commitStatus: null,
  
  // Display elements
  totalCreditsEl: null,
  totalDebitsEl: null,
  balanceEl: null,
  billTypesSummary: null,
  transactionBody: null,
  dataPreview: null,
  
  // Modal elements
  transactionModal: null,
  closeTransactionModal: null,
  closeTransactionModalBtn: null,
  transactionDetails: null,
  editTransactionModal: null,
  closeEditTransactionModal: null,
  cancelEditTransaction: null,
  saveEditTransaction: null,
  editTransactionForm: null,
  
  // Status elements
  transactionStatus: null,
  loadMoreContainer: null,
  loadMoreBtn: null,
  noTransactions: null,
  transactionsSummary: null,
  
  // Distribution elements
  distributionAmount: null,
  distributionNote: null,
  distributionPreview: null,
  distributionDetails: null,
  addDistributionBtn: null,
  
  // Settlement (Offline) elements
  settlementPayer: null,
  settlementReceiver: null,
  settlementAmount: null,
  settlementNote: null,
  addSettlementBtn: null,

  // Transfer (Balance) elements
  transferSender: null,
  transferRecipient: null,
  transferAmount: null,
  transferNote: null,
  addTransferBtn: null,
  
  // Personal finance
  personalFinanceBody: null,
  personalBreakdown: null,
  
  // Date/Time elements
  creditCustomDate: null,
  creditDateTimeFields: null,
  creditDate: null,
  creditTime: null,
  debitCustomDate: null,
  debitDateTimeFields: null,
  debitDate: null,
  debitTime: null,
  
  // Exemption elements
  enableExemptions: null,
  exemptionFields: null,
  exemptionCheckboxes: null,
  exemptionPreview: null,
  exemptionDetails: null,
  
  // Initialize all DOM references
  init() {
    // Form elements
    this.personSelect = document.getElementById('personSelect');
    this.creditAmount = document.getElementById('creditAmount');
    this.creditNote = document.getElementById('creditNote');
    this.addCreditBtn = document.getElementById('addCreditBtn');
    this.debitType = document.getElementById('debitType');
    this.debitAmount = document.getElementById('debitAmount');
    this.debitNote = document.getElementById('debitNote');
    this.addDebitBtn = document.getElementById('addDebitBtn');
    
    // GitHub elements
    this.setPATBtn = document.getElementById('setPATBtn');
    this.commitBtn = document.getElementById('commitBtn');
    this.commitStatus = document.getElementById('commitStatus');
    
    // Display elements
    this.totalCreditsEl = document.getElementById('totalCredits');
    this.totalDebitsEl = document.getElementById('totalDebits');
    this.balanceEl = document.getElementById('balance');
    this.billTypesSummary = document.getElementById('billTypesSummary');
    this.transactionBody = document.getElementById('transactionBody');
    this.dataPreview = document.getElementById('dataPreview');
    
    // Modal elements
    this.transactionModal = document.getElementById('transactionModal');
    this.closeTransactionModal = document.getElementById('closeTransactionModal');
    this.closeTransactionModalBtn = document.getElementById('closeTransactionModalBtn');
    this.transactionDetails = document.getElementById('transactionDetails');
    this.editTransactionModal = document.getElementById('editTransactionModal');
    this.closeEditTransactionModal = document.getElementById('closeEditTransactionModal');
    this.cancelEditTransaction = document.getElementById('cancelEditTransaction');
    this.saveEditTransaction = document.getElementById('saveEditTransaction');
    this.editTransactionForm = document.getElementById('editTransactionForm');
    
    // Status elements
    this.transactionStatus = document.getElementById('transactionStatus');
    this.loadMoreContainer = document.getElementById('loadMoreContainer');
    this.loadMoreBtn = document.getElementById('loadMoreBtn');
    this.noTransactions = document.getElementById('noTransactions');
    this.transactionsSummary = document.getElementById('transactionsSummary');
    
    // Distribution elements
    this.distributionAmount = document.getElementById('distributionAmount');
    this.distributionNote = document.getElementById('distributionNote');
    this.distributionPreview = document.getElementById('distributionPreview');
    this.distributionDetails = document.getElementById('distributionDetails');
    this.addDistributionBtn = document.getElementById('addDistributionBtn');
    
    // Settlement elements
    this.settlementPayer = document.getElementById('settlementPayer');
    this.settlementReceiver = document.getElementById('settlementReceiver');
    this.settlementAmount = document.getElementById('settlementAmount');
    this.settlementNote = document.getElementById('settlementNote');
    this.addSettlementBtn = document.getElementById('addSettlementBtn');

    // Transfer elements
    this.transferSender = document.getElementById('transferSender');
    this.transferRecipient = document.getElementById('transferRecipient');
    this.transferAmount = document.getElementById('transferAmount');
    this.transferNote = document.getElementById('transferNote');
    this.addTransferBtn = document.getElementById('addTransferBtn');
    
    // Personal finance
    this.personalFinanceBody = document.getElementById('personalFinanceBody');
    this.personalBreakdown = document.getElementById('personalBreakdown');
    
    // Date/Time elements
    this.creditCustomDate = document.getElementById('creditCustomDate');
    this.creditDateTimeFields = document.getElementById('creditDateTimeFields');
    this.creditDate = document.getElementById('creditDate');
    this.creditTime = document.getElementById('creditTime');
    this.debitCustomDate = document.getElementById('debitCustomDate');
    this.debitDateTimeFields = document.getElementById('debitDateTimeFields');
    this.debitDate = document.getElementById('debitDate');
    this.debitTime = document.getElementById('debitTime');
    
    // Exemption elements
    this.enableExemptions = document.getElementById('enableExemptions');
    this.exemptionFields = document.getElementById('exemptionFields');
    this.exemptionCheckboxes = document.getElementById('exemptionCheckboxes');
    this.exemptionPreview = document.getElementById('exemptionPreview');
    this.exemptionDetails = document.getElementById('exemptionDetails');
  }
};
