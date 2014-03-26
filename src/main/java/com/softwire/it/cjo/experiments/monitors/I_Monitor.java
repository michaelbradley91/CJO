package com.softwire.it.cjo.experiments.monitors;

/**
 * The interface for a monitor!
 * @author Michael
 *
 */
public interface I_Monitor {
	/**
	 * Obtain the lock for the monitor. This should be called before the method in
	 * the relevant object begins
	 */
	public void Lock();
	
	/**
	 * Release the lock on the semaphore. This should be called when the method in
	 * the relevant object terminates.
	 */
	public void Unlock();
	
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
  public void Notify();
  
  /**
   * Notify all currently waiting processes.
   * This is guaranteed to wake only processes which waited strictly before
	 * the notify was called.
   */
  public void NotifyAll();
  
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
  public void Wait() throws InterruptedException;
  
  /**
   * Wait until a notification is received.
   * This cannot be interrupted
   */
  public void WaitUninterruptibly();
}
