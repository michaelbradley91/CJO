package mjb.dev.cjo.experiments.semaphores;

/**
 * This creates a strictly binary semaphore. This means raising more than once
 * has no additional effects.
 * 
 * The Semaphore is initially up!
 * @author Michael
 *
 */
public class BinarySemaphore {
	
	private java.util.concurrent.Semaphore mutex = new java.util.concurrent.Semaphore(1,true);
	private java.util.concurrent.Semaphore sem = new java.util.concurrent.Semaphore(1,true);
	
	/**
	 * Acquire the semaphore. This will wait if the semaphore is already acquired
	 * @throws InterruptedException 
	 */
	public void acquire() throws InterruptedException {
		sem.acquire();
	}
	
	/**
	 * Acquire the semaphore, ignoring interrupts
	 */
	public void acquireUninterruptibly() {
		sem.acquireUninterruptibly();
	}
	
	/**
	 * Release the semaphore. This has no effect if the semaphore has already been
	 * released
	 */
	public void release() {
		mutex.acquireUninterruptibly();
		//Check the number of permits available
		if (sem.availablePermits()==0) {
			sem.release(); //there aren't any, and this won't change...
			//Hence, release the semaphore!
		}
		mutex.release(); //Let the next person in
	}
}
