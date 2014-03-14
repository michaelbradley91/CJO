package com.softwire.it.cjo.parallelresources;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.softwire.it.cjo.utilities.Box;

/**
 * ****************<br>
 * Date: 14/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class tests the resource graph with multiple threads. These tests are considerably more complex...<br>
 * The passing of these tests does not guarantee a lot due to the scheduler, but if there are concurrency errors,
 * they will hopefully be found!
 *
 */
public class ResourceGraphParallelTest {
	
	private static final ResourceGraph GRAPH = ResourceGraph.INSTANCE;
	
	/**
	 * This class tests that resources cannot be locked on simultaneously in the simplest way - the same
	 * resource is locked on twice.
	 */
	@Test
	public void testLocking() {
		Logger logger = Logger.getLogger(ResourceGraphParallelTest.class);
		ResourceManipulator manipulator = GRAPH.getManipulator();
		final Resource resource = manipulator.addResource();
		//Now release the resources, and then try to acquire the resource...
		manipulator.releaseResources();
		//Lock on...
		long startTime = System.currentTimeMillis();
		manipulator = GRAPH.acquireResource(resource);
		final Box<Long> acquireTime = new Box<Long>(0L);
		final Semaphore completeSemaphore = new Semaphore(0,true);
		//Now spawn a thread to try and lock onto this...
		Thread t = new Thread(new Runnable() {public void run() {
			GRAPH.acquireResource(resource).releaseResources();
			acquireTime.setItem(System.currentTimeMillis());
			completeSemaphore.release();
		}});
		t.start();
		//Wait for a while....
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			logger.warn("testLocking: interrupted while testing");
		} //would ruin the test
		manipulator.releaseResources();
		completeSemaphore.acquireUninterruptibly();
		//Check the other thread had to wait a while...
		assertTrue(acquireTime.getItem()-startTime>1000);
		logger.trace("testLocking: completed");
	}
	
	/**
	 * This is another simple test to check that multiple resources cannot be accessed by any particular
	 * manipulator until released
	 */
	@Test
	public void testMultiLocking() {
		Logger logger = Logger.getLogger(ResourceGraphParallelTest.class);
		ResourceManipulator manipulator = GRAPH.getManipulator();
		final Resource resource1 = manipulator.addResource();
		final Resource resource2 = manipulator.addResource();
		final Resource resource3 = manipulator.addResource();
		//Will use some overlap to make this more complicated...
		manipulator.releaseResources();
		Set<Resource> toAcquire = new HashSet<Resource>();
		//Lock on...
		long startTime = System.currentTimeMillis();
		toAcquire.add(resource1);
		toAcquire.add(resource2);
		manipulator = GRAPH.acquireResources(toAcquire);
		final Box<Long> acquireTime1 = new Box<Long>(0L);
		final Box<Long> acquireTime2 = new Box<Long>(0L);
		final Semaphore completeSemaphore1 = new Semaphore(0,true);
		final Semaphore completeSemaphore2 = new Semaphore(0,true);
		//Now spawn a thread to try and lock onto this...
		Thread t1 = new Thread(new Runnable() {public void run() {
			GRAPH.acquireResource(resource1).releaseResources();
			acquireTime1.setItem(System.currentTimeMillis());
			completeSemaphore1.release();
		}});
		Thread t2 = new Thread(new Runnable() {public void run() {
			Set<Resource> toAcquire = new HashSet<Resource>();
			toAcquire.add(resource2);
			toAcquire.add(resource3);
			GRAPH.acquireResource(resource1).releaseResources();
			acquireTime2.setItem(System.currentTimeMillis());
			completeSemaphore2.release();
		}});
		t1.start();
		t2.start();
		//Wait for a while....
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			logger.warn("testMultiLocking: interrupted while testing");
		} //would ruin the test
		manipulator.releaseResources();
		completeSemaphore1.acquireUninterruptibly();
		completeSemaphore2.acquireUninterruptibly();
		//Check the other threads had to wait a while...
		assertTrue(acquireTime1.getItem()-startTime>1000);
		assertTrue(acquireTime2.getItem()-startTime>1000);
		//Now wait...
		logger.trace("testMultiLocking: completed");
	}
	
	/**
	 * This test makes sure that multiple threads cannot lock onto resources
	 * when they have been modified to be dependent on each other, even after they attempted
	 * to lock onto the resources while they were not dependent
	 */
	@Test
	public void testLockingManipulated() {
		final Logger logger = Logger.getLogger(ResourceGraphParallelTest.class);
		ResourceManipulator manipulator = GRAPH.getManipulator();
		final Resource resource1 = manipulator.addResource();
		final Resource resource2 = manipulator.addResource();
		manipulator.releaseResources();
		Set<Resource> toAcquire = new HashSet<Resource>();
		//Lock on...
		long startTime = System.currentTimeMillis();
		toAcquire.add(resource1);
		toAcquire.add(resource2);
		manipulator = GRAPH.acquireResources(toAcquire);
		final Box<Long> acquireTime1 = new Box<Long>(0L);
		final Box<Long> acquireTime2 = new Box<Long>(0L);
		final Semaphore completeSemaphore1 = new Semaphore(0,true);
		final Semaphore completeSemaphore2 = new Semaphore(0,true);
		//Now spawn a thread to try and lock onto this...
		Thread t1 = new Thread(new Runnable() {public void run() {
			ResourceManipulator manipulator = GRAPH.acquireResource(resource1);
			acquireTime1.setItem(System.currentTimeMillis());
			completeSemaphore1.release();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logger.warn("testLockingManipulated: interrupted while testing (1)");
			} //would ruin the test
			manipulator.releaseResources();
		}});
		Thread t2 = new Thread(new Runnable() {public void run() {
			ResourceManipulator manipulator = GRAPH.acquireResource(resource2);
			acquireTime2.setItem(System.currentTimeMillis());
			completeSemaphore2.release();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logger.warn("testLockingManipulated: interrupted while testing (2)");
			} //would ruin the test
			manipulator.releaseResources();
		}});
		t1.start();
		t2.start();
		//Wait for a while....
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			logger.warn("testMultiLocking: interrupted while testing (3)");
		} //would ruin the test
		manipulator.addDependency(resource1, resource2);
		manipulator.releaseResources();
		completeSemaphore1.acquireUninterruptibly();
		completeSemaphore2.acquireUninterruptibly();
		//Check the other threads had to wait a while...
		assertTrue(acquireTime1.getItem()-startTime>1000);
		assertTrue(acquireTime2.getItem()-startTime>1000);
		//One of them should have waited longer
		assertTrue(acquireTime1.getItem()-startTime>3000 || acquireTime2.getItem()-startTime>3000);
		logger.trace("testLockingManipulated: completed");
	}
	
	/**
	 * This is an attempt to force a locking mechanism to restart in its acquisition
	 * by using other threads to manipulate its resources while its locking.
	 * This is not exactly thorough - the stress test should catch that situtation more directly
	 */
	@Test
	public void testRestart() {
		final Logger logger = Logger.getLogger(ResourceGraphParallelTest.class);
		ResourceManipulator manipulator1 = GRAPH.getManipulator();
		final Resource resource1 = manipulator1.addResource();
		final Resource resource2 = manipulator1.addResource();
		final Resource resource3 = manipulator1.addResource();
		final Resource resource4 = manipulator1.addResource();
		final Resource resource5 = manipulator1.addResource();
		//Add some dependencies...
		manipulator1.addDependency(resource2, resource3);
		manipulator1.addDependency(resource1, resource4);
		manipulator1.releaseResources();
		//Lock on...
		long startTime = System.currentTimeMillis();
		//Now acquire some of the resources in separate manipulators...
		Set<Resource> toAcquire = new HashSet<Resource>();
		toAcquire.add(resource1);
		toAcquire.add(resource5); //gets 1,4 and 5
		manipulator1 = GRAPH.acquireResources(toAcquire);
		ResourceManipulator manipulator2 = GRAPH.acquireResource(resource2); //gets 2 and 3
		//Now start a new manipulator to try and get hold of these...
		final Box<Long> acquireTime1 = new Box<Long>(0L);
		final Semaphore completeSemaphore1 = new Semaphore(0,true);
		//Now spawn a thread to try and lock onto this...
		Thread t1 = new Thread(new Runnable() {public void run() {
			//Get all of the resources...
			Set<Resource> toAcquire = new HashSet<Resource>();
			toAcquire.add(resource5); //reverse order because it might make a difference???
			toAcquire.add(resource4);
			toAcquire.add(resource3);
			toAcquire.add(resource2);
			toAcquire.add(resource1);
			ResourceManipulator manipulator = GRAPH.acquireResources(toAcquire);
			//Check we really got them...
			manipulator.addDependency(resource1, resource2);
			manipulator.addDependency(resource3, resource4);
			manipulator.addDependency(resource4, resource5);
			manipulator.addDependency(resource2, resource3);
			acquireTime1.setItem(System.currentTimeMillis());
			completeSemaphore1.release();
			manipulator.releaseResources();
		}});
		//Start the thread...
		t1.start();
		//Wait for a while....
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			logger.warn("testRestart: interrupted while testing (3)");
		} //would ruin the test
		//Now mess around with the dependencies...
		manipulator1.addDependency(resource4, resource5);
		manipulator1.removeResource(resource4);
		manipulator2.removeDependency(resource3, resource2);
		manipulator2.addDependency(resource3, resource2);
		//Now release and see what happens!
		manipulator1.releaseResources();
		manipulator2.releaseResources();
		completeSemaphore1.acquireUninterruptibly();
		//Check the other threads had to wait a while...
		assertTrue(acquireTime1.getItem()-startTime>1000);
		logger.trace("testRestart: completed");
	}
	
	//Magical constants.. set these to influence what the random threads do...
	private static final int NO_INTERACTIONS=100,
			NO_RESOURCES=500, NO_THREADS=20, NO_METHOD_CALLS=10;
	
	private static class WorkerThread implements Runnable {
		private final Random random;
		private final Resource[] resources;
		private final Semaphore complete;
		WorkerThread(Resource[] resources, Semaphore complete) {
			this.resources = resources;
			random = new Random();
			this.complete = complete;
		}
		
		public void start() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			for (int i=0; i<NO_INTERACTIONS; i++) {
				//Logger.getLogger(getClass()).trace("On interaction " + i);
				//Choose how many resources to interact with... (could be a lot)
				int noResources = random.nextInt(NO_RESOURCES)+1;
				Resource[] currentResources = new Resource[noResources];
				//Now choose the resources...
				for (int j=0; j<noResources; j++) {
					int choice = random.nextInt(NO_RESOURCES);
					currentResources[j] = resources[choice];
				}
				Set<Resource> toAcquire = new HashSet<Resource>();
				toAcquire.addAll(Arrays.asList(currentResources));
				//Acquire the resources...
				ResourceManipulator manipulator = GRAPH.acquireResources(toAcquire);
				//Now for the interactions themselves...
				for (int j=0; j<NO_METHOD_CALLS; j++) {
					//Choose if we will use resources or dependencies...
					if (random.nextInt(2)==0) {
						//Resources!
						Resource resource = currentResources[random.nextInt(noResources)];
						if (random.nextInt(2)==0) {
							//Add!
							manipulator.addResource(); //don't do anything with it...
						} else {
							//Remove!
							manipulator.removeResource(resource);
						}
					} else {
						//Dependencies!
						Resource resource1 = currentResources[random.nextInt(noResources)];
						Resource resource2 = currentResources[random.nextInt(noResources)];
						if (random.nextInt(2)==0) {
							//Add!
							manipulator.addDependency(resource1, resource2);
						} else {
							//Remove!
							manipulator.removeDependency(resource1, resource2);
						}
					}
				}
				manipulator.releaseResources();
			}
			//Done!
			complete.release();
		}
	}
	
	/**
	 * This test is just designed to throw loads of threads doing lots of difficult things with the resources.
	 * It uses 1000 resources, with 100 threads each going to perform 100 interactions with the resource graph.
	 * 
	 * Each thread, when performing an interaction, will randomly choose how many resources to take,
	 * then will randomly choose these resources (choosing possibly less if it picks the same ones by accident)
	 * Once acquired, the thread will, 10 times, randomly flip to decide if to deal with resources or dependencies,
	 * then randomly choose the resources from those selected, and then randomly either add or remove dependency/resource.
	 * 
	 * If the program doesn't lock or crash, it's considered a success!
	 */
	@Test
	public void testStress() {
		final Logger logger = Logger.getLogger(ResourceGraphParallelTest.class);
		//Construct the resources...
		ResourceManipulator manipulator = GRAPH.getManipulator();
		final Resource[] resources = new Resource[NO_RESOURCES];
		for (int i=0; i<NO_RESOURCES; i++) {
			resources[i] = manipulator.addResource();
		}
		manipulator.releaseResources();
		//Now construct the semaphores...
		final Semaphore[] sems = new Semaphore[NO_THREADS];
		for (int i=0; i<NO_THREADS; i++) {
			sems[i] = new Semaphore(0,true);
		}
		//Success! Construct the workers...
		final WorkerThread[] workers = new WorkerThread[NO_THREADS];
		for (int i=0; i<NO_THREADS; i++) {
			workers[i] = new WorkerThread(resources,sems[i]);
			workers[i].start();
		}
		//Wait for them all to finish...
		for (int i=0; i<NO_THREADS; i++) {
			sems[i].acquireUninterruptibly();
		}
		//Done!!
		logger.trace("testStress: completed");
	}
}
