// js/admin-utils.js
/**
 * Utility functions for formatting and data manipulation
 */

const Utils = {
  // Format currency with commas and two decimals
  formatCurrency(amount) {
    return parseFloat(amount).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  },
  
  // Convert to UTC+6 time
  toUTC6(dateString) {
    const date = new Date(dateString);
    const utc6Date = new Date(date.getTime() + (6 * 60 * 60 * 1000));
    return utc6Date.toLocaleString('en-GB', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    }).replace(',', '');
  },
  
  // Convert local date/time to UTC ISO string
  localToUTC(localDate, localTime) {
    const timePart = localTime || '12:00';
    const localDateTimeString = `${localDate}T${timePart}:00`;
    const localDateObj = new Date(localDateTimeString);
    const utcDateObj = new Date(localDateObj.getTime() - (localDateObj.getTimezoneOffset() * 60000));
    return utcDateObj.toISOString();
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
  
  // Get bill icon
  getBillIcon(billType) {
    // Return specific icon or generic box if unknown
    return BILL_ICONS[billType] || '🧾';
  }
};
