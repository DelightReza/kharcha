# calculations.py
"""Financial calculation functions."""


def calculate_bill_distribution_with_exemptions(amount, exemptions, all_people):
    """Calculate bill distribution with transaction-level exemptions."""
    paying_people = [person for person in all_people if person not in exemptions]
    
    if not paying_people:
        return {
            'exempt_people': exemptions,
            'paying_people': [],
            'amount_per_person': 0,
            'total_amount': amount
        }
    
    amount_per_person = amount / len(paying_people)
    
    return {
        'exempt_people': exemptions,
        'paying_people': paying_people,
        'amount_per_person': amount_per_person,
        'total_amount': amount
    }


def calculate_personal_finance(data, all_people):
    """Calculate personal finance breakdown for each person."""
    personal_finance = {}
    
    # Initialize all people
    for person in all_people:
        personal_finance[person] = {
            'credits': 0,
            'debits': 0,
            'net_balance': 0
        }

    # Calculate credits (direct from credit transactions)
    for tx in data['transactions']:
        if tx['type'] == 'credit':
            if tx['whoOrBill'] in personal_finance:
                personal_finance[tx['whoOrBill']]['credits'] += tx['amount']
                personal_finance[tx['whoOrBill']]['net_balance'] += tx['amount']

    # Calculate debits (considering transaction-level exemptions)
    for tx in data['transactions']:
        if tx['type'] == 'debit':
            exemptions = tx.get('exemptions', [])
            paying_people = [person for person in all_people if person not in exemptions]
            
            if paying_people:
                amount_per_person = tx['amount'] / len(paying_people)
                
                for person in paying_people:
                    if person in personal_finance:
                        personal_finance[person]['debits'] += amount_per_person
                        personal_finance[person]['net_balance'] -= amount_per_person

    return personal_finance


def calculate_totals_and_balances(data):
    """Calculate totals and running balances for all transactions."""
    total_credits = 0
    total_debits = 0
    running_balance = 0
    transactions_with_balance = []
    
    # Sort transactions by date (oldest first for balance calculation)
    sorted_transactions = sorted(data['transactions'], key=lambda x: x['date'])
    
    for tx in sorted_transactions:
        if tx['type'] == 'credit':
            total_credits += tx['amount']
            running_balance += tx['amount']
        else:
            total_debits += tx['amount']
            running_balance -= tx['amount']
        
        tx_copy = tx.copy()
        tx_copy['running_balance'] = running_balance
        transactions_with_balance.append(tx_copy)
    
    # Reverse for display (newest first)
    transactions_with_balance.reverse()
    balance = total_credits - total_debits
    
    return {
        'total_credits': total_credits,
        'total_debits': total_debits,
        'balance': balance,
        'transactions_with_balance': transactions_with_balance
    }
