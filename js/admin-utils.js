/**
 * Utility functions for formatting and data manipulation
 */

const Utils = {
  // Format currency with commas, two decimals, and currency symbol from config
  formatCurrency(amount) {
    const curr = AppState.config?.currency || 'SOM';
    return parseFloat(amount).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ",") + ' ' + curr;
  },

  // Format a stored date string for display
  formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString('en-GB', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
      timeZone: 'UTC'
    }).replace(',', '');
  },

  // Build a UTC ISO string from date/time inputs — name kept for compatibility
  localToUTC(localDate, localTime) {
    const timePart = localTime || '12:00';
    return `${localDate}T${timePart}:00.000Z`;
  },

  // Generate unique transaction ID
  generateTransactionId(prefix = 'tx') {
    return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`;
  },

  // Get today's date in YYYY-MM-DD format
  getTodayDate() {
    return new Date().toISOString().split('T')[0];
  },

  // Save data to localStorage
  saveToLocalStorage(key, data) {
    try {
      localStorage.setItem(key, JSON.stringify(data));
      return true;
    } catch (error) {
      console.error('Error saving to localStorage:', error);
      return false;
    }
  },

  // Load data from localStorage
  loadFromLocalStorage(key) {
    try {
      const data = localStorage.getItem(key);
      return data ? JSON.parse(data) : null;
    } catch (error) {
      console.error('Error loading from localStorage:', error);
      return null;
    }
  },

  // Escape HTML to prevent XSS
  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  },

  // Get bill icon – delegate to AppState
  getBillIcon(billName) {
    return AppState.getBillIcon(billName);
  }
};
