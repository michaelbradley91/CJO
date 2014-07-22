package mjb.dev.cjo.experiments.monitors;

import java.math.BigInteger;

import mjb.dev.cjo.experiments.semaphores.BinarySemaphore;
import mjb.dev.cjo.experiments.semaphores.ToggleSemaphore;



/**
 * A monitor implemented using semaphores!!! Strictly, these are entirely
 * binary semaphores! Note that because of this, this version of a Monitor does
 * not suffer from spurious wake-ups
 * 
 * All methods are capitalised to avoid conflicting with the methods in Object
 * @author Michael
 *
 */
public class Monitor implements I_Monitor {
	//All of the semaphores and things that I need!!!
	private BigInteger waiting;
	private ToggleSemaphore wait; //The important one. The bidirectional invention
	private BinarySemaphore lock;
	private BinarySemaphore sync; //Two synchronising semaphores
	private BinarySemaphore sync2;
	private BinarySemaphore waitMutex;
	private boolean notifying;

	/**
	 * Construct a new monitor. Use this only within one object
	 */
	public Monitor() {
		sync = new BinarySemaphore();
		sync2 = new BinarySemaphore();
		//Acquire this stuff
		sync.acquireUninterruptibly();
		sync2.acquireUninterruptibly();
		waitMutex = new BinarySemaphore();
		wait = new ToggleSemaphore(); //AWESOME
		wait.acquireUninterruptibly();
		waiting = BigInteger.ZERO;
		notifying = false;
	}
	
	/**
	 * Obtain the lock for the monitor. This should be called before the method in
	 * the relevant object begins
	 */
	public void Lock() {
		lock.acquireUninterruptibly();
	}
	
	/**
	 * Release the lock on the semaphore. This should be called when the method in
	 * the relevant object terminates.
	 */
	public void Unlock() {
		lock.release();
	}
	
	/**
	 * Notify a process already waiting to wake up.
	 * This is guaranteed to wake a process which waited strictly before
	 * the notify was called.
	 * Multiple calls to notify in the same thread will cause multiple
	 * waiting threads to be woken. If there are x waiting threads
	 * and none suffer from an interruption, x calls to notify
	 * will wake all x threads. (If an interruption occurs, it will not reduce the count
	 * in any way)
	 */
  public void Notify() {
  	waitMutex.acquireUninterruptibly();
  	if (!waiting.equals(BigInteger.ZERO)) { //someone is waiting
  		notifying = true;
  		waitMutex.release();
  		wait.releaseUninterruptibly(); //If nobody waited, this will require them to arrive...
  		notifying = false;
  		//Let the waiting process decrement the count
  		sync2.release();
  		sync.acquireUninterruptibly();
  	}
  	//I keep the lock
  }
  
  /**
   * Notify all currently waiting processes.
   * This is guaranteed to wake only processes which waited strictly before
	 * the notify was called.
   */
  public void NotifyAll() {
  	while (!waiting.equals(BigInteger.ZERO)) notify();
  }
  
  /**
   * Wait until a notification is received.
   * @throws InterruptedException you promise not to just re-throw this!
   * When it is thrown, you will still be given the lock back. Hence you must still
   * release the lock. You could re-throw like this:
   * <br><br>
   * try {<br>
   *   &nbsp&nbsp&nbsp&nbspmonitor.wait();<br>
   * } catch (InterruptedException e) {<br>
   *   &nbsp&nbsp&nbsp&nbspThread.currentThread.interrupt();<br>
   *   &nbsp&nbsp&nbsp&nbspmonitor.unlock();<br>
   *   &nbsp&nbsp&nbsp&nbspthrow e;<br>
   * }
   */
  public void Wait() throws InterruptedException {
  	//Only one thread could arrive here at once!
  	//Add to the number of processes waiting...
  	waiting = waiting.add(BigInteger.ONE);
  	//Release the lock...
  	lock.release();
  	//Now wait for a notification
  	/*
  	 * PROBLEM: what should we do if an interrupt exception is thrown while
  	 * the notify was trying to synchronise with us? We need to make sure the notify
  	 * does not become stuck!!
  	 */
  	try {
  		wait.acquire();
  	} catch (InterruptedException e) {
  		waitMutex.acquireUninterruptibly();
  		//Deduct from the number waiting still
  		waiting = waiting.subtract(BigInteger.ONE);
  		//Check to see if the notify method could have become stuck...
  		if (waiting.equals(BigInteger.ZERO) && notifying) {
  			/*
  			 * Is it possible for a waiting process to just wake up from the wait
  			 * bidirectional semaphore?
  			 * 
  			 * In other words, can the bidirectional semaphore be waking up the notifying
  			 * process?
  			 * 
  			 * Suppose one had woken up, and I woke up separately. Clearly there are two
  			 * waiting processes at least, so my deduction did not set it to zero here.
  			 * 
  			 * If the other process went ahead of me, it could be trying to acquire the wait
  			 * mutex now. But then it will not have subtracted itself, so the count
  			 * would still be one here, and notify will hear from it.
  			 * 
  			 * Otherwise, if it obtained the mutex ahead of me and decremented the count,
  			 * then it must have synchronised with the notifier, and so notifying should be false.
  			 * 
  			 * Another notifier could have entered, but then the semaphore would be back down.
  			 * Since I have the mutex, it cannot notify me anyway, and will see my decrement
  			 */
  			wait.acquireUninterruptibly();
  			sync.release(); //allow synchronisation
  	  	sync2.acquireUninterruptibly();
  	  	waitMutex.release();
  	  	//Re-throw the exception
  		}
			if (!Thread.interrupted()) {Thread.currentThread().interrupt();}
			lock.acquireUninterruptibly();
  		throw e;
  	}
  	waitMutex.acquireUninterruptibly();
  	//State that I'm leaving...
  	waiting = waiting.subtract(BigInteger.ONE);
  	//Synchronise on leaving
  	sync.release();
  	sync2.acquireUninterruptibly();
  	//Re-acquire the lock (at some point)
  	waitMutex.release();
  	lock.acquireUninterruptibly(); //same as if entering a synchronised method
  }
  
  /**
   * Wait until a notification is received.
   * This cannot be interrupted
   */
  public void WaitUninterruptibly() {
  	//Only one thread could arrive here at once!
  	//Add to the number of processes waiting...
  	waiting = waiting.add(BigInteger.ONE);
  	//Release the lock...
  	lock.release();
  	//Now wait for a notification
    wait.acquireUninterruptibly();
  	waitMutex.acquireUninterruptibly();
  	//State that I'm leaving...
  	waiting = waiting.subtract(BigInteger.ONE);
  	//Synchronise on leaving
  	sync.release();
  	sync2.acquireUninterruptibly();
  	//Re-acquire the lock (at some point)
  	waitMutex.release();
  	lock.acquireUninterruptibly(); //same as if entering a synchronised method
  }

}
