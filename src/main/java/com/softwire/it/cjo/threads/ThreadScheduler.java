package com.softwire.it.cjo.threads;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * ****************<br>
 * Date: 21/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This abstract class specifies what the stripped down thread scheduler of CJO should provide.
 * Extenders of this class may assume that only one scheduler will be used.
 *
 */
public abstract class ThreadScheduler {
	/**
	 * The application's thread scheduler
	 */
	public static final ThreadScheduler INSTANCE = SimpleThreadScheduler.INSTANCE;
		
	/**
	 * ****************<br>
     * Date: 21/03/2014<br>
     * Author:  michael<br>
     * ****************<br>
 	 * <br>
 	 * This class represents an object symbolising your thread when it was created. Use this in future interactions
 	 * to execute threads.
	 *
	 */
	public static class Task {};
	
	/**
	 * Schedule the given task. It may begin immediately, but in a separate thread.
	 * @param task - the task to be scheduled
	 * @throws IllegalThreadStateException - if the task was scheduled before
	 */
	public abstract void schedule(Task task);
	
	/**
	 * Deschedule the task. This means it will not run strictly after this call has returned.
	 * The task is not automatically interrupted, so you should do this beforehand if you desire to.
	 * @param task - the task to deschedule
	 * @throws IllegalThreadStateException - if the task was never scheduled
	 */
	public abstract void deschedule(Task task);
	
	/**
	 * Interrupt the given task. This does not deschedule the task, so you should also deschedule the task if you wish
	 * @param task - the task to interrupt
	 * @throws SecurityException - if the thread cannot be interrupted
	 */
	public abstract void interrupt(Task task);
	
	/**
	 * Construct a task from the runnable method. Any exceptions thrown by this task are thrown into any thread
	 * that happened to be running it. You can choose to assign an exception handler if you wish.
	 * By default, the thread inherits the caller's daemon status
	 * @param task - the task to construct
	 * @return - the Task that will be run - not scheduled automatically
	 */
	public abstract Task makeTask(Runnable task);
	
	/**
	 * Construct a task from the runnable method. Any exceptions thrown by this task are thrown into any thread
	 * that happened to be running it. You can choose to assign an exception handler if you wish.
	 * @param task - the task to construct
	 * @param isDaemon - whether or not the thread this task runs in should be a daemon thread.
	 * @return - the Task that will be run - not scheduled automatically
	 * @throws SecurityException - if the daemon status could not be set
	 */
	public abstract Task makeTask(Runnable task, boolean isDaemon);
	
	/**
	 * 
	 * Construct a task from the runnable method.
	 * By default, the thread inherits the caller's daemon status
	 * @param task - the task to construct
	 * @param handler - any uncaught exceptions generated by the task will be thrown to this handler
	 * @return - the Task that will be run - not scheduled automatically
	 */
	public abstract Task makeTask(Runnable task, UncaughtExceptionHandler handler);
	
	/**
	 * 
	 * Construct a task from the runnable method.
	 * @param task - the task to construct
	 * @param handler - any uncaught exceptions generated by the task will be thrown to this handler
	 * @return - the Task that will be run - not scheduled automatically
	 * @throws SecurityException - if the daemon status could not be set
	 */
	public abstract Task makeTask(Runnable task, boolean isDaemon, UncaughtExceptionHandler handler);
}
