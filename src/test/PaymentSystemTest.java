//////////////////////////////////////////////////////////////////////////////////////////////////
// PaymentSystemTest: contains the Main() method for load testing the PaymentApp main module 
//////////////////////////////////////////////////////////////////////////////////////////////////


package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import main.PaymentSystem;
import main.User;


public class PaymentSystemTest {
	
	static final int REQUESTS_PER_SEC = 5000;
	static final int NUM_USERS = 500;
	static final int NUM_THREADS = Runtime.getRuntime().availableProcessors(); 
	static final int MAX_STARTING_BALANCE = 1000;
	
	private TestUser[] _users;
	private String _errLogPath;
	private BufferedWriter _errLogWriter;
	private WorkerThread[] _threads;
	private int _runTimeSec;
	private PaymentSystem _ps;
	
	PaymentSystemTest(int runTimeSec, int numThreads) {
		_users = new TestUser[NUM_USERS];
		Random rand = new Random();
		for (int i = 0; i < NUM_USERS; i++) {
			_users[i] = new TestUser(
				new User(i, new BigDecimal(rand.nextDouble() * MAX_STARTING_BALANCE).setScale(2, RoundingMode.HALF_UP)));
		}

		
		_errLogPath = "err.log";
		try {
			_errLogWriter = openErrLog(_errLogPath);
			_errLogWriter.write("starting test...");
			_errLogWriter.newLine();
		} catch (IOException e) {
			System.out.println("failed to open " + _errLogPath);
			return;
		}
		
		_runTimeSec = runTimeSec;
		_ps = new PaymentSystem();

		int requestsPerSecPerThread = REQUESTS_PER_SEC/NUM_THREADS;
		_threads = new WorkerThread[numThreads];
		for (int thread = 0; thread < numThreads; thread++) {
			_threads[thread] = new WorkerThread(_ps, requestsPerSecPerThread, _users);
		}
	}
	
	void closeErrorLog() {
		try {
			_errLogWriter.write("Test complete, cleaning up...");
			_errLogWriter.flush();
			_errLogWriter.close();
		} catch (IOException e) {
			System.out.println("failed to close error log");
		}
	}
	
	public void run() {
		
		Date startTime = new Date();
		
		for (int thread = 0; thread < _threads.length; thread++) {
			_threads[thread].start();
		}
		
		// report request count periodically
		Timer progressTimer = new Timer(); 
		progressTimer.schedule(new ReportProgressTask(startTime), 1000, 1000);
		
		// wait until end of run time to terminate the threads
		try {
			Thread.sleep(_runTimeSec * 1000L);
			
			progressTimer.cancel();
			
			// interrupt our threads
			for (int thread = 0; thread < _threads.length; thread++) {
				_threads[thread].stopThread();
			}
			// join our threads
			for (int thread = 0; thread < _threads.length; thread++) {
				_threads[thread].join();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	int getRequestCounts() {
		int countSum = 0;
		for (int thread = 0; thread < _threads.length; thread++) {
			countSum += _threads[thread].requestCount();
		}
		return countSum;
	}
	
	public void assess() throws IOException {
		// go through all of our users and verify that their balance, fulfilled payments, and unfulfilled payments 
		// are valid
		System.out.println("cleaned up threads, performing assessment...");
		for (int i = 0; i < NUM_USERS; i++) {
			_errLogWriter.write("validating user " + i);
			_errLogWriter.newLine();
			_users[i].check(_errLogWriter);
		}
		System.out.println("assessment complete, check " + _errLogPath + " for details");
	}
	
	private BufferedWriter openErrLog(String path) throws IOException {
		File file = new File(path);
		FileWriter fw;

		if (!file.exists()) {
			file.createNewFile();
	  	}

		fw = new FileWriter(file);
		return new BufferedWriter(fw);
	}

	public static void main(String[] args) {
		try {
			if (args.length < 1) {
				throw new RuntimeException("PaymentSystemTest requires argument run time (secs)");
			}
			int runTimeSec = Integer.parseInt(args[0]);
			
			int numThreads = NUM_THREADS;
			if (args.length > 1) {
				numThreads = Integer.parseInt(args[1]);
			}
			
			// run the system for the indicated time
			PaymentSystemTest pst = new PaymentSystemTest(runTimeSec, numThreads);
			pst.run();
			
			// only after the run assess it (this way we don't have to worry about concurrency during the 
			// assessment, or impacting performance)
			pst.assess();
			
			pst.closeErrorLog();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	
	private class ReportProgressTask extends TimerTask  {
		private Date _time;
		int _requestCounts;
		
		public ReportProgressTask(Date startTime){
			_time = startTime;
			_requestCounts = 0;
		}
		

		public void run() {
			try {
				Date currentTime = new Date();
				float elapsedTime = (float) ((currentTime.getTime() - _time.getTime())/1000.0);
				int newRequestCounts = getRequestCounts() - _requestCounts;
				int countsPerSec = (int) (newRequestCounts/elapsedTime);
				_time = currentTime;
				_requestCounts += newRequestCounts;

				System.out.println("sent " + countsPerSec + " requests in the last second");
	        } catch (Exception ex) {

	        	System.out.println("error running thread " + ex.getMessage());
	        }
		}
	}
}
