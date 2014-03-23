package com.softwire.it.cjo.operators;

import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.softwire.it.cjo.channels.OneOneChannel;
import com.softwire.it.cjo.operators.AltBuilder.ReadProcess;
import com.softwire.it.cjo.threads.ThreadScheduler;
import com.softwire.it.cjo.threads.ThreadScheduler.Task;
import com.softwire.it.cjo.utilities.Box;

import static org.junit.Assert.*;
import static com.softwire.it.cjo.operators.Ops.*;

/**
 * ****************<br>
 * Date: 22/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class tests the legendary power of the alt.
 * This test is not to be considered in any way extensive, as that would require tests
 * on all possible channels types in a variety of orders (way too many combinations)!<br>
 * <br>
 * The tests here are just designed to check some basic behaviour. Good luck!
 *
 */
public class AltTest {
	//Our copy (lower case for convenience
	private static final ThreadScheduler scheduler = ThreadScheduler.INSTANCE;
	//Our logger
	private static final Logger logger = Logger.getLogger(AltTest.class);
	
	/**
	 * Some fun tests for me! Very nervous
	 */
	@Test
	public void testBasics() {
		//Build an alt!
		final Semaphore finishedSemaphore = new Semaphore(0);
		OneOneChannel<Integer> alice = new OneOneChannel<Integer>();
		OneOneChannel<Integer> bob = new OneOneChannel<Integer>();
		AltBuilder builder = new AltBuilder();
		final Box<Integer> messageBox = new Box<Integer>(0);
		builder = builder.addReadBranch(alice, new ReadProcess<Integer>() {
			@Override
			public void run(Integer message) {
				messageBox.setItem(message);
				finishedSemaphore.release();
			}
		});
		builder = builder.addReadBranch(bob, new ReadProcess<Integer>() {
			public void run(Integer message) {
				fail("Managed to receive a message from bob: " + message);
			}
		});
		final Box<AltBuilder> builderBox = new Box<AltBuilder>(builder);
		//Run the alt in a separate thread
		Task task = scheduler.makeTask(new Runnable() {public void run() {
			alt(builderBox.getItem());
		}});
		scheduler.schedule(task);
		//Now see what happens!!
		write(alice,6);
		finishedSemaphore.acquireUninterruptibly();
		assertTrue(messageBox.getItem().equals(6));
		//Woo?
		logger.trace("testBasics: complete");
	}
}
