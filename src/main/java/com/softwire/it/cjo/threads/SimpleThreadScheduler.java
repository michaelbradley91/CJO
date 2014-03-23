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
		private final Semaphore syncSemaphore;
		private boolean wasStarted;
		/**
		 * Construct a new simple task keeping the current thread
		 * @param thread - the thread the task is running in.
		 */
		private MyTask(Thread thread, Semaphore finishedSemaphore) {
			this.thread = thread;
			this.finishedSemaphore = finishedSemaphore;
			syncSemaphore = new Semaphore(1,true);
			wasStarted = false;
		}
	}
	
	@Override
	public void schedule(Task task) {
		if (task==null) {
			throw new IllegalArgumentException();
		}
		//Run it!!
		MyTask myTask = (MyTask)task;
		myTask.syncSemaphore.acquireUninterruptibly();
		if (myTask.wasStarted) {
			IllegalThreadStateException exception = new IllegalThreadStateException("Tried to start thread (name:" +
					myTask.thread.getName() + ",id:" + myTask.thread.getId() + ") twice.");
			myTask.syncSemaphore.release();
			throw exception;
		} else {
			myTask.wasStarted = true;
			myTask.thread.start();
			myTask.syncSemaphore.release();
		}
	}

	@Override
	public void deschedule(Task task) {
		if (task==null) {
			throw new IllegalArgumentException();
		}
		MyTask myTask = (MyTask)task;
		myTask.syncSemaphore.acquireUninterruptibly();
		if (!myTask.wasStarted) {
			IllegalThreadStateException exception = new IllegalThreadStateException("Tried to stop a thread no started: (name:" +
					myTask.thread.getName() + ",id:" + myTask.thread.getId() + ").");
			myTask.syncSemaphore.release();
			throw exception;
		} else {
			myTask.syncSemaphore.release();
			myTask.finishedSemaphore.acquireUninterruptibly();
			myTask.finishedSemaphore.release();
		}
	}

	@Override
	public void interrupt(Task task) {
		if (task==null) {
			throw new IllegalArgumentException();
		}
		((MyTask)task).thread.interrupt();
	}

	@Override
	public Task makeTask(final Runnable task) {
		if (task==null) {
			throw new IllegalArgumentException();
		}
		//Construct the task...
		final Semaphore finishedSemaphore = new Semaphore(0);
		Thread thread = buildThread(task,finishedSemaphore);
		return (Task)new MyTask(thread,finishedSemaphore);
	}

	@Override
	public Task makeTask(final Runnable task, final UncaughtExceptionHandler handler) {
		if (task==null || handler==null) {
			throw new IllegalArgumentException();
		}
		//Construct the task...
		final Semaphore finishedSemaphore = new Semaphore(0);
		Thread thread = buildThread(task,finishedSemaphore);
		thread.setUncaughtExceptionHandler(handler);
		return (Task)new MyTask(thread,finishedSemaphore);
	}

	@Override
	public Task makeTask(final Runnable task, boolean isDaemon) {
		if (task==null) {
			throw new IllegalArgumentException();
		}
		//Construct the task...
		final Semaphore finishedSemaphore = new Semaphore(0);
		Thread thread = buildThread(task,finishedSemaphore);
		thread.setDaemon(isDaemon);
		return (Task)new MyTask(thread,finishedSemaphore);
	}

	@Override
	public Task makeTask(final Runnable task, boolean isDaemon,
			UncaughtExceptionHandler handler) {
		if (task==null || handler==null) {
			throw new IllegalArgumentException();
		}
		//Construct the task...
		final Semaphore finishedSemaphore = new Semaphore(0);
		Thread thread = buildThread(task,finishedSemaphore);
		thread.setUncaughtExceptionHandler(handler);
		thread.setDaemon(isDaemon);
		return (Task)new MyTask(thread,finishedSemaphore);
	}
	
	//To prevent code duplication - builds the standard thread
	private Thread buildThread(final Runnable task, final Semaphore finishedSemaphore) {
		return new Thread(new Runnable() {public void run() {
			try {
				task.run();
				finishedSemaphore.release();
			} catch (RuntimeException e) {
				finishedSemaphore.release();
				throw e;
			}
		}});
	}
}
