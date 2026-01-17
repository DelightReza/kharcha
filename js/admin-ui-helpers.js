// js/admin-ui-helpers.js
/**
 * UI helper functions for forms and interactive elements
 */

// Extend UI object with additional methods
Object.assign(UI, {
  // Initialize form dropdowns
  initializeDropdowns() {
    const people = AppState.getPeopleList();
    
    // Check if element exists before setting innerHTML
    if (DOM.personSelect) {
        DOM.personSelect.innerHTML = people.map(person => 
          `<option value="${person}">👤 ${person}</option>`
        ).join('');
    }
    
    // Also update Debit Type dropdown dynamically
    const billTypes = AppState.getBillTypesList();
    if (DOM.debitType) {
        DOM.debitType.innerHTML = billTypes.map(billType => 
            `<option value="${billType}">${Utils.getBillIcon(billType)} ${billType}</option>`
        ).join('');
    }
  },
  
  // Set default dates
  setDefaultDates() {
    const today = Utils.getTodayDate();
    DOM.creditDate.value = today;
    DOM.debitDate.value = today;
  },
  
  // Update whoOrBill dropdown based on type (Used primarily in Edit Modal logic)
  updateWhoOrBillDropdown(selectElement, type) {
    selectElement.innerHTML = '';
    
    if (type === 'credit') {
      const people = AppState.getPeopleList();
      people.forEach(person => {
        const option = document.createElement('option');
        option.value = person;
        option.textContent = `👤 ${person}`;
        selectElement.appendChild(option);
      });
    } else {
      const billTypes = AppState.getBillTypesList();
      billTypes.forEach(billType => {
        const option = document.createElement('option');
        option.value = billType;
        option.textContent = `${Utils.getBillIcon(billType)} ${billType}`;
        selectElement.appendChild(option);
      });
    }
  },
  
  // Initialize bill exemptions
  initBillExemptions() {
    const people = AppState.getPeopleList();
    DOM.exemptionCheckboxes.innerHTML = people.map(person => `
      <label class="flex items-center space-x-2 text-sm">
        <input type="checkbox" value="${person}" class="rounded text-red-600 focus:ring-red-500 exemption-checkbox">
        <span>${person}</span>
      </label>
    `).join('');

    DOM.debitType.addEventListener('change', () => this.updateExemptionPreview());
    
    document.querySelectorAll('.exemption-checkbox').forEach(checkbox => {
      checkbox.addEventListener('change', () => this.updateExemptionPreview());
    });
    
    // Remove existing listener to avoid duplicates if re-initialized
    const newEnableExemptions = DOM.enableExemptions.cloneNode(true);
    DOM.enableExemptions.parentNode.replaceChild(newEnableExemptions, DOM.enableExemptions);
    DOM.enableExemptions = newEnableExemptions;

    DOM.enableExemptions.addEventListener('change', function() {
      DOM.exemptionFields.classList.toggle('hidden', !this.checked);
      if (this.checked) {
        UI.updateExemptionPreview();
      } else {
        DOM.exemptionPreview.classList.add('hidden');
      }
    });

    DOM.debitAmount.addEventListener('input', () => this.updateExemptionPreview());
  },
  
  // Update exemption preview
  updateExemptionPreview() {
    const amount = parseFloat(DOM.debitAmount.value);
    const exemptPeople = Array.from(document.querySelectorAll('.exemption-checkbox:checked'))
      .map(cb => cb.value);
    
    if (!amount || amount <= 0 || isNaN(amount)) {
      DOM.exemptionPreview.classList.add('hidden');
      return;
    }
    
    const allPeople = AppState.getPeopleList();
    const payingPeople = allPeople.filter(person => !exemptPeople.includes(person));
    
    if (payingPeople.length === 0) {
      DOM.exemptionDetails.innerHTML = 
        '<span class="text-red-600">⚠️ No one is paying this bill!</span>';
      DOM.exemptionPreview.classList.remove('hidden');
      return;
    }
    
    const amountPerPerson = amount / payingPeople.length;
    
    DOM.exemptionDetails.innerHTML = `
      <div class="mb-2">
        <span class="font-medium">Exempt:</span> ${exemptPeople.length > 0 ? exemptPeople.join(', ') : 'None'}
      </div>
      <div class="mb-2">
        <span class="font-medium">Paying (${payingPeople.length} people):</span> ${payingPeople.join(', ')}
      </div>
      <div class="font-medium text-red-600">
        Amount per person: ${amountPerPerson.toFixed(2)} SOM
      </div>
      <div class="text-xs text-red-500 mt-2 flex items-center">
        <i class="fas fa-info-circle mr-1"></i>
        These exemptions apply only to this transaction
      </div>
    `;
    
    DOM.exemptionPreview.classList.remove('hidden');
  },
  
  // Update distribution preview
  updateDistributionPreview() {
    const amount = parseFloat(DOM.distributionAmount.value);
    
    if (!amount || amount <= 0 || isNaN(amount)) {
      DOM.distributionPreview.classList.add('hidden');
      return;
    }
    
    const allPeople = AppState.getPeopleList();
    const amountPerPerson = amount / allPeople.length;
    
    DOM.distributionDetails.innerHTML = allPeople.map(person => `
      <div class="flex justify-between items-center bg-green-50 p-2 rounded-lg">
        <span class="text-gray-600 text-xs">${person}:</span>
        <span class="font-medium text-green-600 text-sm">${amountPerPerson.toFixed(2)}</span>
      </div>
    `).join('');
    
    DOM.distributionPreview.classList.remove('hidden');
  },
  
  // Load more transactions
  loadMoreTransactions() {
    AppState.incrementPage();
    this.renderTransactions();
  }
});
