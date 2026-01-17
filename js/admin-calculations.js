// js/admin-calculations.js
/**
 * Financial calculation functions
 */

const Calculations = {
  // Calculate personal finance breakdown
  calculatePersonalFinance() {
    const data = AppState.getData();
    const personalFinance = {};
    const allPeople = AppState.getPeopleList();
    
    // Initialize all people
    allPeople.forEach(person => {
      personalFinance[person] = {
        credits: 0,
        debits: 0,
        netBalance: 0
      };
    });

    // Calculate credits (direct from credit transactions)
    data.transactions.forEach(tx => {
      if (tx.type === 'credit') {
        // Ensure person exists in our list (handle legacy data gracefully)
        if (!personalFinance[tx.whoOrBill]) {
             personalFinance[tx.whoOrBill] = { credits: 0, debits: 0, netBalance: 0 };
        }
        
        if (personalFinance[tx.whoOrBill]) {
          personalFinance[tx.whoOrBill].credits += tx.amount;
          personalFinance[tx.whoOrBill].netBalance += tx.amount;
        }
      }
    });

    // Calculate debits (considering transaction-level exemptions)
    data.transactions.forEach(tx => {
      if (tx.type === 'debit') {
        const exemptions = tx.exemptions || [];
        const payingPeople = allPeople.filter(person => !exemptions.includes(person));
        
        if (payingPeople.length > 0) {
          const amountPerPerson = tx.amount / payingPeople.length;
          
          payingPeople.forEach(person => {
            if (personalFinance[person]) {
              personalFinance[person].debits += amountPerPerson;
              personalFinance[person].netBalance -= amountPerPerson;
            }
          });
        }
      }
    });

    return personalFinance;
  },
  
  // Calculate bill distribution with exemptions
  calculateBillDistribution(amount, exemptions) {
    const allPeople = AppState.getPeopleList();
    const payingPeople = allPeople.filter(person => !exemptions.includes(person));
    
    if (payingPeople.length === 0) {
      return { 
        exemptPeople: exemptions, 
        payingPeople: [], 
        amountPerPerson: 0 
      };
    }
    
    const amountPerPerson = amount / payingPeople.length;
    
    return {
      exemptPeople: exemptions,
      payingPeople: payingPeople,
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
