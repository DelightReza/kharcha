// js/admin-config.js
/**
 * Configuration constants for the Kharcha admin panel
 */

const CONFIG = {
  REPO_OWNER: 'DelightReza',
  REPO_NAME: 'kharcha',
  DATA_FILE: 'data.json',
  TRANSACTIONS_PER_PAGE: 10
};

const PEOPLE = {
  DEFAULT: ['Raza', 'Salman', 'Mujeeb', 'Gulam', 'Rana', 'Naved', 'Musawwar', 'Nizamuddin'],
  ALL: ['Raza', 'Salman', 'Mujeeb', 'Gulam', 'Rana', 'Naved', 'Musawwar', 'Nizamuddin']
};

const BILL_TYPES = ['Electricity', 'Water', 'Gas', 'Garbage', 'Internet', 'Other'];

const BILL_ICONS = {
  'Electricity': '⚡',
  'Water': '💧',
  'Gas': '🔥',
  'Garbage': '🗑️',
  'Internet': '🌐',
  'Other': '📦'
};

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { CONFIG, PEOPLE, BILL_TYPES, BILL_ICONS };
}
