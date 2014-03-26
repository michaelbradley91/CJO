package com.softwire.it.cjo.experiments.semaphores;

/**
 * Implements a strictly bidirectional semaphore!
 * That means, if the semaphore is up and we release it, we will be forced
 * to wait until it is pulled down. Similar for down
 * @author Michael
 *
 */
public class ToggleSemaphore {
	private BinarySemaphore upSem;
	private BinarySemaphore downSem;
	/**
	 * Create a new bidirectional semaphore. It is initially up
	 */
	public ToggleSemaphore() {
		upSem = new BinarySemaphore();
		downSem = new BinarySemaphore();
		//Initialise to the right way round
		upSem.acquireUninterruptibly();
	}
	/**
	 * Acquire the semaphore
	 * @throws InterruptedException
	 */
	public void acquire() throws InterruptedException {
		downSem.acquire();
		upSem.release();
	}
	
	/**
	 * Release the semaphore
	 * @throws InterruptedException
	 */
	public void release() throws InterruptedException {
		upSem.acquire();
		downSem.release();
	}
	
	/**
	 * Acquire the semaphore uninterruptibly
	 */
	public void acquireUninterruptibly() {
		downSem.acquireUninterruptibly();
		upSem.release();
	}
	
	/**
	 * Acquire the semaphore uninterruptibly
	 */
	public void releaseUninterruptibly() {
		downSem.acquireUninterruptibly();
		upSem.release();
	}
	
}