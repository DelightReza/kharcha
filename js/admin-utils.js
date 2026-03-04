/**
 * Utility functions for formatting and data manipulation
 */

const Utils = {
  // Format currency
  formatCurrency(amount) {
    const curr = AppState.config?.currency || 'SOM';
    return parseFloat(amount).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ",") + ' ' + curr;
  },

  // Format a stored date string for display
  // Input: "2025-01-20T10:00:00"
  // Output: "2025-01-20 10:00"
  formatDate(dateString) {
    if (!dateString) return '';
    return dateString.replace('T', ' ').substring(0, 16);
  },

  // Name kept for compatibility, but logic CHANGED to be Local
  // Returns: "YYYY-MM-DDTHH:MM:00" (Local Device Time)
  localToUTC(localDate, localTime) {
    const timePart = localTime || '12:00';
    return `${localDate}T${timePart}:00`;
  },

  // Generate unique transaction ID
  generateTransactionId(prefix = 'tx') {
    return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`;
  },

  // Get current local datetime as "YYYY-MM-DDTHH:MM:00"
  getLocalNow() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}:00`;
  },

  // Get today's date in YYYY-MM-DD format (local time)
  getTodayDate() {
    return this.getLocalNow().split('T')[0];
  },

  // Save/Load helpers
  saveToLocalStorage(key, data) {
    try {
      localStorage.setItem(key, JSON.stringify(data));
      return true;
    } catch (error) {
      console.error('Error saving to localStorage:', error);
      return false;
    }
  },

  loadFromLocalStorage(key) {
    try {
      const data = localStorage.getItem(key);
      return data ? JSON.parse(data) : null;
    } catch (error) {
      console.error('Error loading from localStorage:', error);
      return null;
    }
  },

  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  },

  getBillIcon(billName) {
    return AppState.getBillIcon(billName);
  }
};
