/**
 * UI helper functions for forms and interactive elements
 */

// Extend UI object with additional methods
Object.assign(UI, {
  // Initialize form dropdowns using config
  initializeDropdowns() {
    const people = AppState.getPeopleList();
    const peopleOptions = people.map(person => `<option value="${person}">👤 ${person}</option>`).join('');

    // Main forms
    if (DOM.personSelect) DOM.personSelect.innerHTML = peopleOptions;

    // Settlement forms
    if (DOM.settlementPayer) DOM.settlementPayer.innerHTML = peopleOptions;
    if (DOM.settlementReceiver) DOM.settlementReceiver.innerHTML = peopleOptions;

    // Transfer forms
    if (DOM.transferSender) DOM.transferSender.innerHTML = peopleOptions;
    if (DOM.transferRecipient) DOM.transferRecipient.innerHTML = peopleOptions;

    // Debit Type dropdown
    const billTypes = AppState.getBillTypesList();
    if (DOM.debitType) {
      DOM.debitType.innerHTML = billTypes.map(billType =>
        `<option value="${billType}">${Utils.getBillIcon(billType)} ${billType}</option>`
      ).join('');
    }
  },

  // Render Settings Modal Lists
  renderSettingsLists() {
    const config = AppState.config;
    
    // People List
    DOM.settingsPeopleList.innerHTML = config.people.map(person => `
        <div class="flex justify-between items-center bg-white p-2 rounded-lg border border-slate-100">
            <span class="text-sm font-medium text-slate-700">👤 ${person}</span>
            <button onclick="UI.removePerson('${person}')" class="text-red-400 hover:text-red-600 p-1">
                <i class="fas fa-trash-alt"></i>
            </button>
        </div>
    `).join('');

    // Bill Types List
    DOM.settingsBillList.innerHTML = config.billTypes.map(bt => `
        <div class="flex justify-between items-center bg-white p-2 rounded-lg border border-slate-100">
            <span class="text-sm font-medium text-slate-700">${bt.icon} ${bt.name}</span>
            <button onclick="UI.removeBillType('${bt.name}')" class="text-red-400 hover:text-red-600 p-1">
                <i class="fas fa-trash-alt"></i>
            </button>
        </div>
    `).join('');
  },

  // Add Person to Config
  addPerson() {
    const name = DOM.newPersonName.value.trim();
    if (!name) return;
    
    if (AppState.config.people.includes(name)) {
        alert('Person already exists!');
        return;
    }

    AppState.config.people.push(name);
    DOM.newPersonName.value = '';
    this.renderSettingsLists();
  },

  // Remove Person from Config
  removePerson(name) {
    if (confirm(`Remove ${name}? Note: Historical data will still exist but won't be linked correctly if you add them back later with a different name.`)) {
        AppState.config.people = AppState.config.people.filter(p => p !== name);
        this.renderSettingsLists();
    }
  },

  // Add Bill Type to Config
  addBillType() {
    const name = DOM.newBillName.value.trim();
    const icon = DOM.newBillIcon.value.trim() || '🧾';
    
    if (!name) return;

    if (AppState.config.billTypes.some(bt => bt.name === name)) {
        alert('Bill type already exists!');
        return;
    }

    AppState.config.billTypes.push({ name, icon });
    DOM.newBillName.value = '';
    DOM.newBillIcon.value = '';
    this.renderSettingsLists();
  },

  // Remove Bill Type from Config
  removeBillType(name) {
    if (confirm(`Remove ${name} category?`)) {
        AppState.config.billTypes = AppState.config.billTypes.filter(bt => bt.name !== name);
        this.renderSettingsLists();
    }
  },

  // Update Config Commit Status
  updateConfigCommitStatus(message, type) {
    const statusEl = DOM.configCommitStatus;
    statusEl.textContent = message;
    statusEl.className = 'text-xs font-medium ' + (type === 'success' ? 'text-green-600' : (type === 'error' ? 'text-red-600' : 'text-blue-600'));
    
    if (type === 'success') {
        setTimeout(() => statusEl.textContent = '', 5000);
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
    const currency = AppState.config?.currency || 'SOM';
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
        Amount per person: ${amountPerPerson.toFixed(2)} ${currency}
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
    const currency = AppState.config?.currency || 'SOM';
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
        <span class="font-medium text-green-600 text-sm">${amountPerPerson.toFixed(2)} ${currency}</span>
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
