package com.softwire.it.cjo.threads;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Semaphore;

/**
 * ****************<br>
 * Date: 21/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * For testing purposes, this thread scheduler just always constructs new threads.
 *
 */
class SimpleThreadScheduler extends ThreadScheduler {
	/**
	 * An instance of this scheduler - not to be accessed externally.
	 */
	final static SimpleThreadScheduler INSTANCE = new SimpleThreadScheduler();
	
	//Hide the constructor
	private SimpleThreadScheduler() {};
	
	/**
	 * ****************<br>
	 * Date: 21/03/2014<br>
	 * Author:  michael<br>
	 * ****************<br>
	 * <br>
	 * Just stores the thread running itself
	 *
	 */
	static class MyTask extends Task {
		private final Thread thread;
		private final Semaphore finishedSemaphore;
		/**
		 * Construct a new simple task keeping the current thread
		 * @param thread - the thread the task is running in.
		 */
		private MyTask(Thread thread, Semaphore finishedSemaphore) {
			this.thread = thread;
			this.finishedSemaphore = finishedSemaphore;
		}
	}
	
	@Override
	public void schedule(Task task) {
		//Run it!!
		((MyTask)task).thread.start();
	}

	@Override
	public void deschedule(Task task) {
		((MyTask)task).finishedSemaphore.acquireUninterruptibly();
		((MyTask)task).finishedSemaphore.release();
	}

	@Override
	public void interrupt(Task task) {
		((MyTask)task).thread.interrupt();
	}

	@Override
	public Task makeTask(final Runnable task) {
		return makeTask(task,Thread.currentThread().isDaemon());
	}

	@Override
	public Task makeTask(final Runnable task, final UncaughtExceptionHandler handler) {
		return makeTask(task,Thread.currentThread().isDaemon(),handler);
	}

	@Override
	public Task makeTask(final Runnable task, boolean isDaemon) {
		//Construct the task...
		final Semaphore finishedSemaphore = new Semaphore(0);
		Thread thread = new Thread(new Runnable() {public void run() {
			try {
				task.run();
				finishedSemaphore.release();
			} catch (RuntimeException e) {
				finishedSemaphore.release();
				throw e;
			}
		}});
		thread.setDaemon(isDaemon);
		return (Task)new MyTask(thread,finishedSemaphore);
	}

	@Override
	public Task makeTask(final Runnable task, boolean isDaemon,
			UncaughtExceptionHandler handler) {
		//Construct the task...
		final Semaphore finishedSemaphore = new Semaphore(0);
		Thread thread = new Thread(new Runnable() {public void run() {
			try {
				task.run();
				finishedSemaphore.release();
			} catch (RuntimeException e) {
				finishedSemaphore.release();
				throw e;
			}
		}});
		thread.setUncaughtExceptionHandler(handler);
		thread.setDaemon(isDaemon);
		return (Task)new MyTask(thread,finishedSemaphore);
	}
}
