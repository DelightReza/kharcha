// js/admin-calculations.js
/**
 * Financial calculation functions
 */

const Calculations = {
  // Calculate personal finance breakdown
  calculatePersonalFinance() {
    const data = AppState.getData();
    const personalFinance = {};
    const allPeopleIds = AppState.getAllPeopleIds(); // ALL people including inactive
    
    // Initialize all people
    allPeopleIds.forEach(personId => {
      personalFinance[personId] = {
        credits: 0,
        debits: 0,
        netBalance: 0
      };
    });

    // Calculate credits (direct from credit transactions)
    data.transactions.forEach(tx => {
      if (tx.type === 'credit') {
        const personId = tx.whoOrBill;
        if (!personalFinance[personId]) {
          personalFinance[personId] = { credits: 0, debits: 0, netBalance: 0 };
        }
        personalFinance[personId].credits += tx.amount;
        personalFinance[personId].netBalance += tx.amount;
      }
    });

    // Calculate debits using splitAmong snapshot (V2) or fall back to exemptions (V1)
    data.transactions.forEach(tx => {
      if (tx.type === 'debit') {
        let payingPeople;
        if (tx.splitAmong && tx.splitAmong.length > 0) {
          payingPeople = tx.splitAmong;
        } else {
          const exemptions = tx.exemptions || [];
          payingPeople = allPeopleIds.filter(id => !exemptions.includes(id));
        }
        
        if (payingPeople.length > 0) {
          const amountPerPerson = tx.amount / payingPeople.length;
          
          payingPeople.forEach(personId => {
            if (!personalFinance[personId]) {
              personalFinance[personId] = { credits: 0, debits: 0, netBalance: 0 };
            }
            personalFinance[personId].debits += amountPerPerson;
            personalFinance[personId].netBalance -= amountPerPerson;
          });
        }
      }
    });

    return personalFinance;
  },
  
  // Calculate bill distribution preview for a given splitAmong list
  calculateBillDistribution(amount, splitAmong) {
    if (!splitAmong || splitAmong.length === 0) {
      return { splitAmong: [], amountPerPerson: 0 };
    }
    
    const amountPerPerson = amount / splitAmong.length;
    
    return {
      splitAmong: splitAmong,
      amountPerPerson: amountPerPerson
    };
  },
  
  // Calculate running balance for a transaction
  calculateRunningBalance(transactionId) {
    const data = AppState.getData();
    let runningBalance = 0;
    const sortedTransactions = [...data.transactions].sort((a, b) => 
      new Date(a.date) - new Date(b.date)
    );
    
    let balanceBefore = 0;
    let balanceAfter = 0;
    
    for (const tx of sortedTransactions) {
      if (tx.id === transactionId) {
        balanceBefore = runningBalance;
        if (tx.type === 'credit') {
          balanceAfter = runningBalance + tx.amount;
        } else {
          balanceAfter = runningBalance - tx.amount;
        }
        break;
      }
      
      if (tx.type === 'credit') {
        runningBalance += tx.amount;
      } else {
        runningBalance -= tx.amount;
      }
    }
    
    return { balanceBefore, balanceAfter };
  },
  
  // Calculate totals
  calculateTotals() {
    const data = AppState.getData();
    let totalCredits = 0;
    let totalDebits = 0;
    
    data.transactions.forEach(tx => {
      if (tx.type === 'credit') {
        totalCredits += tx.amount;
      } else {
        totalDebits += tx.amount;
      }
    });
    
    const balance = totalCredits - totalDebits;
    
    return { totalCredits, totalDebits, balance };
  }
};
