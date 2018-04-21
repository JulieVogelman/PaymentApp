//////////////////////////////////////////////////////////////////////////////////////////////////
// User: a customer of the payment system, can request payments and fulfill payments
//////////////////////////////////////////////////////////////////////////////////////////////////



package main;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class User {
	private int _accountNumber;
	public int getAccountNumber() {
		return _accountNumber;
	}
	private BigDecimal _balance; 
	public BigDecimal getBalance() {
		return _balance;
	}
	
	// Fulfilled payments, both PaymentSystem.OUTgoing and incoming (2-dim array for PaymentSystem.OUT/IN)
	// order payments by time they were paid for fast access to recent ones, and 
	// also fast access to oldest for the sake of purging
	ArrayList< TreeMap<Date, Payment> > _fulfilledPayments; 
	
	// Unfulfilled payments, both PaymentSystem.OUTgoing and incoming (2-dim array for PaymentSystem.OUT/IN)
	private ArrayList< HashSet<Payment> > _unfulfilledPayments; 
	
	
	// purge payments after they've reached a certain limit
	private static final int MAX_PAYMENTS_MAINTAINED = 10;
	
	// keep track of sums to date so we don't have to recalculate
	private BigDecimal[] _sumUnfulfilled;
	public BigDecimal getSumUnfulfilled(Direction direction) {
		return _sumUnfulfilled[direction._val];
	}
	
	public User(int accountNumber, BigDecimal balance) {
		if (balance == null) {
			throw new RuntimeException("null parameter in User constructor");
		}
		_accountNumber = accountNumber;
		_balance = balance;
		
		_sumUnfulfilled = new BigDecimal[2];
		_sumUnfulfilled[Direction.OUT._val] = new BigDecimal(0).setScale(2, RoundingMode.HALF_UP);
		_sumUnfulfilled[Direction.IN._val] = new BigDecimal(0).setScale(2, RoundingMode.HALF_UP);

		_fulfilledPayments = new ArrayList<TreeMap<Date, Payment>>();
		_fulfilledPayments.add(new TreeMap<Date, Payment>()); // OUT (outgoing payments)
		_fulfilledPayments.add(new TreeMap<Date, Payment>()); // IN (incoming payments)
		
		_unfulfilledPayments = new ArrayList< HashSet<Payment> >();
		_unfulfilledPayments.add(new HashSet<Payment>()); // OUT
		_unfulfilledPayments.add(new HashSet<Payment>()); // IN
	}
	
	protected void addPayment(Payment payment, Direction direction) {
		_unfulfilledPayments.get(direction._val).add(payment);
		_sumUnfulfilled[direction._val] = _sumUnfulfilled[direction._val].add(payment.getAmount());
	}

	private void markPaymentFulfilled(Payment payment, Direction direction) {
		_unfulfilledPayments.get(direction._val).remove(payment);
		_sumUnfulfilled[direction._val] = _sumUnfulfilled[direction._val].subtract(payment.getAmount());
		
		TreeMap<Date, Payment> fulfilledPayments = _fulfilledPayments.get(direction._val);
		fulfilledPayments.put(new Date(), payment);
		// for now just purge beyond a certain amount to save on memory
		if (fulfilledPayments.size() > MAX_PAYMENTS_MAINTAINED) {
			fulfilledPayments.remove(fulfilledPayments.firstKey());
		}
	}
	
	// return false if we can't fulfill this (i.e. balance too low)
	// return true otherwise
	protected boolean fulfillPayment(Payment payment) {
		// check for sufficient balance
		if (payment.getAmount().compareTo(this._balance) > 0) {
			return false;
		}
		
		payment.markFulfilled();
		
		markPaymentFulfilled(payment, Direction.OUT);
		_balance = _balance.subtract(payment.getAmount());
		return true;
	}
	
	protected void markFulfilledIncoming(Payment payment) {
		markPaymentFulfilled(payment, Direction.IN);
		_balance = _balance.add(payment.getAmount());
	}
	
	public Collection<Payment> getRecentPayments(Direction direction) {
		if (direction == null) {
			throw new RuntimeException("null parameter in User.getRecentPayments");
		}
		TreeMap<Date, Payment> fulfilledPayments = _fulfilledPayments.get(direction._val);
		return fulfilledPayments.values();
	}
	
	public Collection<Payment> getRecentPayments(Direction direction, Date startingWith) {
		if (direction == null || startingWith == null) {
			throw new RuntimeException("null parameter in User.getRecentPayments");
		}
		if (_fulfilledPayments.size() == 0) {
			return new LinkedList<Payment>();
		}
		TreeMap<Date, Payment> fulfilledPayments = _fulfilledPayments.get(direction._val);
		return fulfilledPayments.tailMap(startingWith).values();
	}
	
	@Override
    public String toString() {
        return String.format(
        		"account: " + _accountNumber + "\n" +
        		"balance: " + _balance + "\n" +
        		"sum unfulfilled out: " + _sumUnfulfilled[Direction.OUT._val] + "\n" +
        		"sum unfulfilled in: " + _sumUnfulfilled[Direction.IN._val] + "\n");
    
		
    }
}
