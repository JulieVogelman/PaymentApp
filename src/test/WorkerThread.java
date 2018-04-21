//////////////////////////////////////////////////////////////////////////////////////////////////
// WorkerThread: one of the threads responsible for making requests of the PaymentSystem
//////////////////////////////////////////////////////////////////////////////////////////////////


package test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

import main.Direction;
import main.Payment;
import main.PaymentSystem;

public class WorkerThread extends Thread {
	private int _requestsPerSec;
	private double _timeInterval;
	private TestUser[] _users;
	private LinkedList<Payment> _paymentRequests; // future enhancement: consider having this be shared between threads
	private PaymentSystem _paymentSystem;
	private boolean _timeToStop; // for the parent thread to tell this thread to stop
	private volatile int _requestCount;
	
	public WorkerThread(PaymentSystem paymentSystem, int requestsPerSec, TestUser[] users) {
		_paymentSystem = paymentSystem;
		_requestsPerSec = requestsPerSec;
		_timeInterval = 1.0/requestsPerSec;
		System.out.println("time interval:" + _timeInterval);
		_users = users;
		
		// this is where we'll maintain the payments that are still unfulfilled
		_paymentRequests = new LinkedList<Payment>();
		_timeToStop = false;
		_requestCount = 0;
	}
	
	public void stopThread() {
		_timeToStop = true;
	}
	
	public int requestCount() {
		return _requestCount;
	}
	
	public void run() {
		
        
        Random rand = new Random();
        
        Date startTime = new Date();
        
        while (true) {
        	try {
                // randomly generate a test case
        		int testCase = rand.nextInt(5);
        		// randomly generate a couple of users
        		TestUser user1 = _users[rand.nextInt(_users.length)];
        		TestUser user2 = _users[rand.nextInt(_users.length)];
        		Payment payment;
        		
        		switch (testCase) {
        		case 0: // Request payment
        			BigDecimal amount = new BigDecimal(rand.nextDouble() * 100).setScale(2, RoundingMode.HALF_UP);
        			try {
        				payment = _paymentSystem.requestPayment(amount, user1._user, user2._user);
        			} catch (Exception e) {
        				break;
        			}
        			_paymentRequests.add(payment);
        			user1.addUnfulfilled(payment, Direction.OUT);
        			user2.addUnfulfilled(payment, Direction.IN);
        			break;
        		
        		
        		case 1: // Fulfill payment
        			if (_paymentRequests.isEmpty()) {
        				break;
        			}
        			payment = _paymentRequests.removeFirst();
        			
        			//todo: may want to check that fulfill returns expected value
        			if (_paymentSystem.fulfill(payment)) {
	        			TestUser from = _users[payment.from().getAccountNumber()];
	        			TestUser to = _users[payment.to().getAccountNumber()];
	        			from.setFulfilledOutgoing(payment);
	        			to.setFulfilledIncoming(payment);
        			} 
        			
        			break;
        		
        		
        		case 2: // Check balance
        			user1._user.getBalance();
        			break;
        		case 3: // Check amount owed
        			user1._user.getSumUnfulfilled(Direction.IN);
        			break;
        		case 4: // Check amount owing
        			user1._user.getSumUnfulfilled(Direction.OUT);
        			break;
        		case 5: // view recent payments
        			user1._user.getRecentPayments(Direction.OUT);
        			break;
        		}
        		
        		_requestCount++;
        		
        		// pause if we're ahead of schedule
        		if (_requestCount % 100 == 0) {	// only do this periodically
        			sleep(startTime, _requestCount);
        		}
        		if (_timeToStop) {
                	System.out.println(this.getName() + ": complete");
        			return;
        		}
        		
            } catch (InterruptedException e) {
                // We've been interrupted: no more messages.
            	System.out.println(this.getName() + ": returning after interrupt exception");
                return;
            }
        }
    }
	
	// attempt to stay on predetermined schedule
	private void sleep(Date startTime, int msgCountSoFar) throws InterruptedException {
		double secPassedSoFar = (new Date().getTime() - startTime.getTime())/1000.0;
				
		// based on the current number of requests we've generated, how long should that have taken?
		double waitUntil = msgCountSoFar * _timeInterval;
		
		if (waitUntil > secPassedSoFar) {
			double sleepFor = waitUntil - secPassedSoFar;
			//System.out.println("sleeping for " + sleepFor + " seconds");
			Thread.sleep((int)(sleepFor)*1000);
		}
	}
}
