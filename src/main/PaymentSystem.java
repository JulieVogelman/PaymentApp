//////////////////////////////////////////////////////////////////////////////////////////////////
// PaymentSystem: front end to the payment application
//////////////////////////////////////////////////////////////////////////////////////////////////


package main;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;



public class PaymentSystem {
	
	
	public User addUser(int accountNumber, BigDecimal balance) {
		// future enhancements: maintain a structure with all users, verify uniqueness of account number
		return new User(accountNumber, balance);
	}
	
	public Payment requestPayment(BigDecimal amount, User from, User to) {
		if (amount == null || from == null || to == null) {
			throw new RuntimeException("null input to requestPayment() method");
		}
		
		int fromAcct = from.getAccountNumber();
		int toAcct = to.getAccountNumber();
		if (fromAcct == toAcct) { 
			throw new RuntimeException("cannot request payment for oneself: acct id = " + from.getAccountNumber()); 
		}
		Payment payment = new Payment(amount, from, to);
		
		// lock Users in specific order to avoid deadlock
		User[] userOrder = getLockOrder(payment.from(), payment.to());
		
		synchronized (userOrder[0]) {
			synchronized (userOrder[1]) {
				from.addPayment(payment, Direction.OUT);
				to.addPayment(payment, Direction.IN);
			}
		}
		return payment;
	}
	
	public boolean fulfill(Payment payment) {
		if (payment == null) {
			throw new RuntimeException("invalid parameter, payment=null");
		}
		if (payment.isFulfilled()) {
			throw new RuntimeException("payment already fulfilled ");
		}
		
		// lock Users in specific order to avoid deadlock
		User[] userOrder = getLockOrder(payment.from(), payment.to());
		
		synchronized (userOrder[0]) {
			synchronized (userOrder[1]) {
				if (!payment.from().fulfillPayment(payment)) {
					return false;
				}
				payment.to().markFulfilledIncoming(payment);
			}
		}

		return true;
	}
	
	public BigDecimal getUnfulfilledAmt(User user, Direction direction) {
		if (user == null || direction == null) {
			throw new RuntimeException("invalid null input");
		}
		synchronized (user) {
			return user.getSumUnfulfilled(direction);
		}
	}
	
	
	Collection<Payment> getRecentPayments(User user, Direction direction) {
		if (user == null || direction == null) {
			throw new RuntimeException("invalid null input");
		}
		synchronized (user) {
			return user.getRecentPayments(direction);
		}
	}
	
	Collection<Payment> getRecentPayments(User user, Direction direction, Date startingWith) {
		if (user == null || direction == null || startingWith == null) {
			throw new RuntimeException("invalid null input");
		}
		synchronized (user) {
			return user.getRecentPayments(direction, startingWith);
		}
	}
	
	
	// need to lock Users in specific order to avoid deadlock
	// return the ordered Users
	private User[] getLockOrder(User user1, User user2) {
		int fromAcct = user1.getAccountNumber();
		int toAcct = user2.getAccountNumber();
		if (fromAcct == toAcct) {
			throw new RuntimeException("accounts are the same");
		}
		// lock Users in specific order to avoid deadlock
		User[] users = new User[2];
		if (fromAcct < toAcct) {
			users[0] = user1;
			users[1] = user2;
		} else {
			users[0] = user2;
			users[1] = user1;
		}
		return users;
	}
	
	// for testing purposes...
	public static void main(String[] args) {
		PaymentSystem ps = new PaymentSystem();
		User user1 = ps.addUser(0, toBigDecimal(999.98));
		User user2 = ps.addUser(1, toBigDecimal(500));
		Payment payment1 = ps.requestPayment(toBigDecimal(400), user1, user2);
		ps.fulfill(payment1);
		Payment payment2 = ps.requestPayment(toBigDecimal(700), user1, user2);
		ps.fulfill(payment2);
		System.out.println("user1: " + user1.toString());
		System.out.println("user2: " + user2.toString());
	}
	
	private static BigDecimal toBigDecimal(double val) {
		return new BigDecimal(val).setScale(2, RoundingMode.HALF_UP);
	}
	
}
