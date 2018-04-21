//////////////////////////////////////////////////////////////////////////////////////////////////
// TestUser: keeping track of information pertaining to a single User in the PaymentSystem
//				Used to assess the validity of the system after the test
//////////////////////////////////////////////////////////////////////////////////////////////////


package test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;

import main.Direction;
import main.Payment;
import main.PaymentSystem;
import main.User;

public class TestUser {
	User _user;
	
	BigDecimal _startingBalance;
	BigDecimal _balance;
	BigDecimal[] _oweAmount;
	int[] _numPaymentsUnfulfilled; // not currently doing any assessment with this: keep it?
	int[] _numPaymentsFulfilled;
	
	public TestUser(User user) {
		_user = user;
		_startingBalance = _user.getBalance();
		_balance = _startingBalance;
		_oweAmount = new BigDecimal[2];
		_oweAmount[Direction.OUT._val] = new BigDecimal(0);
		_oweAmount[Direction.IN._val] = new BigDecimal(0);
		_numPaymentsFulfilled = new int[2];
		_numPaymentsFulfilled[Direction.OUT._val] = 0;
		_numPaymentsFulfilled[Direction.IN._val] = 0;
		_numPaymentsUnfulfilled = new int[2];
		_numPaymentsUnfulfilled[Direction.OUT._val] = 0;
		_numPaymentsUnfulfilled[Direction.IN._val] = 0;
	}
	
	synchronized void addUnfulfilled(Payment payment, Direction direction) {
		_oweAmount[direction._val] = _oweAmount[direction._val].add(payment.getAmount());
		_numPaymentsUnfulfilled[direction._val] += 1;
	}
	
	synchronized void setFulfilled(Payment payment, Direction direction) {
		int dir = direction._val;
		_oweAmount[dir] = _oweAmount[dir].subtract(payment.getAmount());
		_numPaymentsUnfulfilled[dir] -= 1;
		_numPaymentsFulfilled[dir] += 1;
	}
	
	synchronized void setFulfilledOutgoing(Payment payment) {
		_balance = _balance.subtract(payment.getAmount());
		setFulfilled(payment, Direction.OUT);
	}
	
	synchronized void setFulfilledIncoming(Payment payment) {
		_balance = _balance.add(payment.getAmount());
		setFulfilled(payment, Direction.IN);
	}
	
	// make sure all the data agrees with the underlying user's data
	synchronized void check(BufferedWriter errLog) throws IOException {
		if (_balance.compareTo(_user.getBalance()) != 0) {
			writeToErrorLog(errLog, "ERR: (balance) " + _balance + " != " + _user.getBalance());
		}
		for (Direction direction : Direction.values()) {
			if (_oweAmount[direction._val].compareTo(_user.getSumUnfulfilled(direction)) != 0) {
				writeToErrorLog(errLog, "ERR: (owe amount, direction = " + direction + ") " + 
						_oweAmount[direction._val] + " != " + _user.getSumUnfulfilled(direction));
			}
			
		}
	}
	

	private void writeToErrorLog(BufferedWriter errorLog, String str) throws IOException {
		try {
			errorLog.write(str);
			errorLog.newLine();
		} catch (IOException e) {
			System.out.println("failed to write to error log");
			throw e;
		}
	}
}
