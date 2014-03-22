package com.softwire.it.cjo.threads;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

import com.softwire.it.cjo.threads.ThreadScheduler.Task;

/**
 * ****************<br>
 * Date: 22/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This tests the ability of the scheduler of to schedule threads correctly!
 *
 */
public class ThreadSchedulerTest {
	//Our copy (lower case for convenience
	private static final ThreadScheduler scheduler = ThreadScheduler.INSTANCE;
	//Our logger
	private static final Logger logger = Logger.getLogger(ThreadSchedulerTest.class);
	
	/**
	 * This test spawns threads which must run in parallel to complete
	 */
	@Test
	public void testParallel() {
		//Force tw threads to run in parallel
		for (int i=0; i<1000; i++) {
			final Semaphore finishSemaphore1 = new Semaphore(0);
			final Semaphore finishSemaphore2 = new Semaphore(0);
			final Semaphore syncSemaphore1 = new Semaphore(0);
			final Semaphore syncSemaphore2 = new Semaphore(0);
			//Spawn!!
			Task task1 = scheduler.makeTask(new Runnable() {public void run() {
				syncSemaphore1.release();
				syncSemaphore2.acquireUninterruptibly();
				finishSemaphore1.release();
			}});
			Task task2 = scheduler.makeTask(new Runnable() {public void run() {
				syncSemaphore2.release();
				syncSemaphore1.acquireUninterruptibly();
				finishSemaphore2.release();
			}});
			//Schedule them!
			scheduler.schedule(task1);
			scheduler.schedule(task2);
			//Now wait for completion
			finishSemaphore1.acquireUninterruptibly();
			finishSemaphore2.acquireUninterruptibly();
		}
		//Now generate loads of threads that must run at the same time!!
		int NO_THREADS = 1000;
		final Semaphore syncSemaphore = new Semaphore(-NO_THREADS);
		for (int i=0; i<NO_THREADS; i++) {
			Task task = scheduler.makeTask(new Runnable() {public void run() {
				syncSemaphore.release();
				syncSemaphore.acquireUninterruptibly();
				syncSemaphore.release();
			}});
			scheduler.schedule(task);
		}
		//Now wait
		syncSemaphore.release();
		syncSemaphore.acquireUninterruptibly();
		//done!
		syncSemaphore.release();
		logger.trace("testParallel: complete");
	}
	
	/**
	 * This test makes sure that tasks can be interrupted correctly.
	 */
	@Test
	public void testInterruption() {
		final Semaphore finishedSemaphore = new Semaphore(0);
		Task task = scheduler.makeTask(new Runnable() {public void run() {
			try {
				Thread.sleep(3000);
				fail("testInterruption: Was not interrupted");
			} catch (InterruptedException e) {
				finishedSemaphore.release();
			}
		}});
		scheduler.schedule(task);
		scheduler.interrupt(task);
		finishedSemaphore.acquireUninterruptibly();
		//There shouldn't be an issue interrupting an unscheduled task
		task = scheduler.makeTask(new Runnable() {public void run() {}});
		scheduler.interrupt(task); //good
		logger.trace("testInterruption: complete");
	}
	
	/**
	 * Test exceptions are caught when the handler is set
	 */
	@Test
	public void testExceptionHandler() {
		final Semaphore finishedSemaphore = new Semaphore(0);
		Task task = scheduler.makeTask(new Runnable() {public void run() {
			throw new Error("woah!");
		}},new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				assertTrue(e instanceof Error);
				finishedSemaphore.release();
			}});
		scheduler.schedule(task);
		finishedSemaphore.acquireUninterruptibly();
		logger.trace("testExceptionHandler: complete");
	}
	
	/**
	 * Test that the thread's daemon status can be set successfully
	 */
	@Test
	public void testIsDaemon() {
		final Semaphore finishedSemaphore = new Semaphore(0);
		Task task = scheduler.makeTask(new Runnable() {public void run() {
			assertTrue(Thread.currentThread().isDaemon());
			finishedSemaphore.release();
		}},true);
		scheduler.schedule(task);
		finishedSemaphore.acquireUninterruptibly();
		task = scheduler.makeTask(new Runnable() {public void run() {
			assertFalse(Thread.currentThread().isDaemon());
			finishedSemaphore.release();
		}},false);
		scheduler.schedule(task);
		finishedSemaphore.acquireUninterruptibly();
		logger.trace("testIsDaemon: complete");
	}
	
	/**
	 * Test that the various exceptions are thrown at the correct times. (This test is not comprehensive
	 * regarding concurrency)
	 */
	@Test
	public void testExceptions() {
		Task task = scheduler.makeTask(new Runnable() {public void run() {}});
		scheduler.schedule(task);
		try {
			scheduler.schedule(task);
			fail("Managed to schedule a thread twice");
		} catch (IllegalThreadStateException exception) {}
		task = scheduler.makeTask(new Runnable() {public void run() {}});
		try {
			scheduler.deschedule(task);
			fail("Managed to deschedule a thread before it was scheduled");
		} catch (IllegalThreadStateException exception) {}
		//That should be it!
		logger.trace("testExceptions: complete");
	}
	
	/**
	 * This tests the scheduler's ability to deschedule threads
	 */
	@Test
	public void testDeschedule() {
		long startTime = System.currentTimeMillis();
		Task task = scheduler.makeTask(new Runnable() {public void run() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.warn("testDeschedule: interrupted while waiting");
			}
		}});
		scheduler.schedule(task);
		//Should be made to wait for it
		scheduler.deschedule(task);
		long endTime = System.currentTimeMillis();
		assertTrue(endTime-startTime>500);
		//Should not be an issue descheduling again
		scheduler.deschedule(task);
		endTime = System.currentTimeMillis();
		assertTrue(endTime-startTime<1500);
	}
}
