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
    const activePeople = this.getActivePeopleObjects();
    const billTypes = this.config.billTypes;
    this.data = {
      people: activePeople.reduce((acc, p) => {
        acc[typeof p === 'object' ? p.id : p] = 0;
        return acc;
      }, {}),
      billTypes: billTypes.reduce((acc, bt) => {
        acc[typeof bt === 'object' && bt.id ? bt.id : bt.name] = 0;
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

  // Return all people objects from config (V2 [{id,name,active}] or V1 [string])
  getActivePeopleObjects() {
    if (!this.config) return [];
    const people = this.config.people;
    if (!people || !people.length) return [];
    if (typeof people[0] === 'object') {
      return people.filter(p => p.active !== false);
    }
    return people.map(p => ({ id: p, name: p, active: true }));
  },

  // Returns only active person IDs (for new-transaction dropdowns)
  getPeopleList() {
    return this.getActivePeopleObjects().map(p => p.id);
  },

  // Returns ALL person IDs including inactive (for historical calculations)
  getAllPeopleIds() {
    if (!this.config) return Object.keys(this.data.people);
    const people = this.config.people;
    if (!people || !people.length) return Object.keys(this.data.people);
    if (typeof people[0] === 'object') {
      return people.map(p => p.id);
    }
    return people;
  },

  // Resolve a person ID to their display name
  getPersonName(id) {
    if (!this.config) return id;
    const people = this.config.people;
    if (people && people.length && typeof people[0] === 'object') {
      const person = people.find(p => p.id === id);
      return person ? person.name : id;
    }
    return id; // V1: ID is the name
  },

  getBillTypesList() {
    if (this.config) {
      const bts = this.config.billTypes;
      if (bts && bts.length && typeof bts[0] === 'object' && bts[0].id) {
        return bts.map(bt => bt.id);
      }
      return bts.map(bt => bt.name);
    }
    return Object.keys(this.data.billTypes).length ? Object.keys(this.data.billTypes) : [];
  },

  // Resolve a bill type ID to its display name
  getBillTypeName(id) {
    if (!this.config) return id;
    const bts = this.config.billTypes;
    if (bts && bts.length && typeof bts[0] === 'object' && bts[0].id) {
      const bt = bts.find(b => b.id === id);
      return bt ? bt.name : id;
    }
    return id;
  },

  getBillIcon(billId) {
    if (!this.config) return '🧾';
    const bt = this.config.billTypes.find(b => (b.id || b.name) === billId || b.name === billId);
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
