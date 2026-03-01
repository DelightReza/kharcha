/**
 * UI helper functions for forms and interactive elements
 */

Object.assign(UI, {
  // Initialize form dropdowns using config (only active people)
  initializeDropdowns() {
    const activePeople = AppState.getActivePeopleObjects();
    const peopleOptions = activePeople.map(p => {
      const id = p.id;
      const name = p.name;
      return `<option value="${id}">👤 ${name}</option>`;
    }).join('');

    // Main forms
    if (DOM.personSelect) DOM.personSelect.innerHTML = peopleOptions;

    // Settlement forms
    if (DOM.settlementPayer) DOM.settlementPayer.innerHTML = peopleOptions;
    if (DOM.settlementReceiver) DOM.settlementReceiver.innerHTML = peopleOptions;

    // Transfer forms
    if (DOM.transferSender) DOM.transferSender.innerHTML = peopleOptions;
    if (DOM.transferRecipient) DOM.transferRecipient.innerHTML = peopleOptions;

    // Debit Type dropdown (bill types)
    const billTypeIds = AppState.getBillTypesList();
    if (DOM.debitType) {
      DOM.debitType.innerHTML = billTypeIds.map(btId =>
        `<option value="${btId}">${Utils.getBillIcon(btId)} ${AppState.getBillTypeName(btId)}</option>`
      ).join('');
    }
  },

  // Render Settings Modal Lists
  renderSettingsLists() {
    const config = AppState.config;
    const people = config.people;
    
    // People List (V2: objects with id/name/active; V1: strings)
    if (people.length && typeof people[0] === 'object') {
      DOM.settingsPeopleList.innerHTML = people.map(person => `
        <div class="flex justify-between items-center bg-white p-2 rounded-lg border border-slate-100">
            <span class="text-sm font-medium text-slate-700">
              👤 ${person.name}
              <span class="ml-2 text-xs px-2 py-0.5 rounded-full font-medium ${person.active ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}">
                ${person.active ? 'Active' : 'Inactive'}
              </span>
            </span>
            <div class="flex gap-2">
              <button onclick="UI.togglePersonActive('${person.id}')" class="text-xs px-2 py-1 rounded ${person.active ? 'bg-red-50 text-red-600 hover:bg-red-100' : 'bg-green-50 text-green-600 hover:bg-green-100'}">
                ${person.active ? 'Deactivate' : 'Activate'}
              </button>
              <button onclick="UI.removePerson('${person.id}')" class="text-red-400 hover:text-red-600 p-1">
                <i class="fas fa-trash-alt"></i>
              </button>
            </div>
        </div>
      `).join('');
    } else {
      DOM.settingsPeopleList.innerHTML = people.map(person => `
        <div class="flex justify-between items-center bg-white p-2 rounded-lg border border-slate-100">
            <span class="text-sm font-medium text-slate-700">👤 ${person}</span>
            <button onclick="UI.removePerson('${person}')" class="text-red-400 hover:text-red-600 p-1">
                <i class="fas fa-trash-alt"></i>
            </button>
        </div>
      `).join('');
    }

    // Bill Types List
    DOM.settingsBillList.innerHTML = config.billTypes.map(bt => `
        <div class="flex justify-between items-center bg-white p-2 rounded-lg border border-slate-100">
            <span class="text-sm font-medium text-slate-700">${bt.icon} ${bt.name}</span>
            <button onclick="UI.removeBillType('${bt.id || bt.name}')" class="text-red-400 hover:text-red-600 p-1">
                <i class="fas fa-trash-alt"></i>
            </button>
        </div>
    `).join('');
  },

  // Add Person to Config
  addPerson() {
    const name = DOM.newPersonName.value.trim();
    if (!name) return;
    
    const people = AppState.config.people;
    if (people.length && typeof people[0] === 'object') {
      const id = name.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z0-9_]/g, '');
      if (people.some(p => p.id === id || p.name.toLowerCase() === name.toLowerCase())) {
        alert('Person already exists!');
        return;
      }
      AppState.config.people.push({ id, name, active: true });
    } else {
      if (people.includes(name)) {
        alert('Person already exists!');
        return;
      }
      AppState.config.people.push(name);
    }
    DOM.newPersonName.value = '';
    this.renderSettingsLists();
  },

  // Toggle person active status
  togglePersonActive(id) {
    const people = AppState.config.people;
    if (people.length && typeof people[0] === 'object') {
      const person = people.find(p => p.id === id);
      if (person) {
        person.active = !person.active;
        this.renderSettingsLists();
        this.initializeDropdowns(); // refresh dropdowns
        this.initBillExemptions();
      }
    }
  },

  // Remove Person from Config
  removePerson(id) {
    const people = AppState.config.people;
    const displayName = people.length && typeof people[0] === 'object'
      ? (people.find(p => p.id === id) || {}).name || id
      : id;
    if (confirm(`Remove ${displayName}? Note: Historical data will still exist but won't be linked if re-added with a different ID.`)) {
      if (people.length && typeof people[0] === 'object') {
        AppState.config.people = people.filter(p => p.id !== id);
      } else {
        AppState.config.people = people.filter(p => p !== id);
      }
      this.renderSettingsLists();
    }
  },

  // Add Bill Type to Config
  addBillType() {
    const name = DOM.newBillName.value.trim();
    const icon = DOM.newBillIcon.value.trim() || '🧾';
    
    if (!name) return;

    const bts = AppState.config.billTypes;
    if (bts.length && typeof bts[0] === 'object' && bts[0].id !== undefined) {
      const id = name.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z0-9_]/g, '');
      if (bts.some(bt => bt.id === id || bt.name.toLowerCase() === name.toLowerCase())) {
        alert('Bill type already exists!');
        return;
      }
      AppState.config.billTypes.push({ id, name, icon });
    } else {
      if (bts.some(bt => bt.name === name)) {
        alert('Bill type already exists!');
        return;
      }
      AppState.config.billTypes.push({ name, icon });
    }
    DOM.newBillName.value = '';
    DOM.newBillIcon.value = '';
    this.renderSettingsLists();
  },

  // Remove Bill Type from Config
  removeBillType(id) {
    if (confirm(`Remove this bill category?`)) {
      const bts = AppState.config.billTypes;
      if (bts.length && typeof bts[0] === 'object' && bts[0].id !== undefined) {
        AppState.config.billTypes = bts.filter(bt => bt.id !== id);
      } else {
        AppState.config.billTypes = bts.filter(bt => bt.name !== id);
      }
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
      const activePeople = AppState.getActivePeopleObjects();
      activePeople.forEach(p => {
        const option = document.createElement('option');
        option.value = p.id;
        option.textContent = `👤 ${p.name}`;
        selectElement.appendChild(option);
      });
    } else {
      const billTypeIds = AppState.getBillTypesList();
      billTypeIds.forEach(btId => {
        const option = document.createElement('option');
        option.value = btId;
        option.textContent = `${Utils.getBillIcon(btId)} ${AppState.getBillTypeName(btId)}`;
        selectElement.appendChild(option);
      });
    }
  },

  // Initialize bill exemptions (who to exclude from splitAmong)
  initBillExemptions() {
    const activePeople = AppState.getActivePeopleObjects();
    DOM.exemptionCheckboxes.innerHTML = activePeople.map(p => `
      <label class="flex items-center space-x-2 text-sm">
        <input type="checkbox" value="${p.id}" class="rounded text-red-600 focus:ring-red-500 exemption-checkbox">
        <span>${p.name}</span>
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
    const exemptIds = Array.from(document.querySelectorAll('.exemption-checkbox:checked'))
      .map(cb => cb.value);

    if (!amount || amount <= 0 || isNaN(amount)) {
      DOM.exemptionPreview.classList.add('hidden');
      return;
    }

    const activePeople = AppState.getActivePeopleObjects();
    const payingPeople = activePeople.filter(p => !exemptIds.includes(p.id));

    if (payingPeople.length === 0) {
      DOM.exemptionDetails.innerHTML =
        '<span class="text-red-600">⚠️ No one is paying this bill!</span>';
      DOM.exemptionPreview.classList.remove('hidden');
      return;
    }

    const amountPerPerson = amount / payingPeople.length;
    const exemptNames = activePeople.filter(p => exemptIds.includes(p.id)).map(p => p.name);

    DOM.exemptionDetails.innerHTML = `
      <div class="mb-2">
        <span class="font-medium">Exempt:</span> ${exemptNames.length > 0 ? exemptNames.join(', ') : 'None'}
      </div>
      <div class="mb-2">
        <span class="font-medium">Paying (${payingPeople.length} people):</span> ${payingPeople.map(p => p.name).join(', ')}
      </div>
      <div class="font-medium text-red-600">
        Amount per person: ${amountPerPerson.toFixed(2)} ${currency}
      </div>
      <div class="text-xs text-red-500 mt-2 flex items-center">
        <i class="fas fa-info-circle mr-1"></i>
        Split is frozen at transaction time (snapshot)
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

    const activePeople = AppState.getActivePeopleObjects();
    const amountPerPerson = amount / activePeople.length;

    DOM.distributionDetails.innerHTML = activePeople.map(p => `
      <div class="flex justify-between items-center bg-green-50 p-2 rounded-lg">
        <span class="text-gray-600 text-xs">${p.name}:</span>
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
