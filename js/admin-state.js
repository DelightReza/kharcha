// js/admin-state.js
/**
 * Application state management
 */

const AppState = {
  data: {
    people: {},
    billTypes: {},
    transactions: []
  },
  
  githubPAT: "",
  currentPage: 1,
  patTimeout: null,
  
  // Initialize default data structure
  initializeDefaultData() {
    this.data = {
      people: PEOPLE.DEFAULT.reduce((acc, person) => {
        acc[person] = 0;
        return acc;
      }, {}),
      billTypes: BILL_TYPES.reduce((acc, billType) => {
        acc[billType] = 0;
        return acc;
      }, {}),
      transactions: []
    };
  },
  
  // Get current data
  getData() {
    return this.data;
  },
  
  // Set data
  setData(newData) {
    this.data = {
      ...newData,
      billTypes: newData.billTypes || {}
    };
  },
  
  // PAT management
  setPAT(token) {
    this.githubPAT = token;
  },
  
  getPAT() {
    return this.githubPAT;
  },
  
  clearPAT() {
    this.githubPAT = "";
    localStorage.removeItem('kharcha_pat');
    localStorage.removeItem('kharcha_pat_time');
    if (this.patTimeout) {
      clearTimeout(this.patTimeout);
      this.patTimeout = null;
    }
  },
  
  setPATTimeout(timeout) {
    this.patTimeout = timeout;
  },
  
  // Page management
  incrementPage() {
    this.currentPage++;
  },
  
  resetPage() {
    this.currentPage = 1;
  },
  
  getCurrentPage() {
    return this.currentPage;
  }
};
