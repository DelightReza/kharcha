/**
 * Application state management
 */

const AppState = {
  data: {
    people: {},
    billTypes: {},
    transactions: []
  },

  config: null,                // will hold the loaded config

  githubPAT: "",
  currentPage: 1,
  patTimeout: null,

  // Initialize default data structure using config
  initializeDefaultData() {
    if (!this.config) return;
    this.data = {
      people: this.config.people.reduce((acc, p) => {
        acc[p] = 0;
        return acc;
      }, {}),
      billTypes: this.config.billTypes.reduce((acc, bt) => {
        acc[bt.name] = 0;
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

  // Dynamic getters using config if available, otherwise from data
  getPeopleList() {
    if (this.config) return this.config.people;
    return Object.keys(this.data.people).length ? Object.keys(this.data.people) : [];
  },

  getBillTypesList() {
    if (this.config) return this.config.billTypes.map(bt => bt.name);
    return Object.keys(this.data.billTypes).length ? Object.keys(this.data.billTypes) : [];
  },

  getBillIcon(billName) {
    if (!this.config) return '🧾';
    const bt = this.config.billTypes.find(b => b.name === billName);
    return bt ? bt.icon : '🧾';
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
