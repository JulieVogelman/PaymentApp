//////////////////////////////////////////////////////////////////////////////////////////////////
// Payment: A payment that has been requested and may or may not have been fulfilled 
//////////////////////////////////////////////////////////////////////////////////////////////////


package main;

import java.math.BigDecimal;
import java.util.Date;

public class Payment {
	private User _from;	// payer
	public User from() {
		return _from;
	}
	private User _to;	// payee/requester
	public User to() {
		return _to;
	}
	private BigDecimal _amount;
	public BigDecimal getAmount() {
		return _amount;
	}
	private boolean _fulfilled; 
	public boolean isFulfilled() {
		return _fulfilled;
	}
	private Date _timeRequested;
	private Date _timeFulfilled;
	
	
	Payment(BigDecimal amount, User from, User to) {
		_from = from;
		_to = to;
		_amount = amount;
		_fulfilled = false;
		_timeRequested = new Date(); // automatically set to right now
		_timeFulfilled = null; // assumption is that at construction this is unfulfilled
	}
	
	protected void markFulfilled() {
		_fulfilled = true;
		_timeFulfilled = new Date(); // automatically set to right now
	}
	
}
