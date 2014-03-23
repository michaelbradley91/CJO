package com.softwire.it.cjo.operators;

import java.util.Random;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.softwire.it.cjo.channels.OneOneChannel;
import com.softwire.it.cjo.operators.AltBuilder.ReadProcess;
import com.softwire.it.cjo.operators.AltBuilder.WriteProcess;
import com.softwire.it.cjo.operators.exceptions.ProcessInterruptedException;
import com.softwire.it.cjo.threads.ThreadScheduler;
import com.softwire.it.cjo.threads.ThreadScheduler.Task;
import com.softwire.it.cjo.utilities.Box;

import static org.junit.Assert.*;
import static com.softwire.it.cjo.operators.Ops.*;

/**
 * ****************<br>
 * Date: 23/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class tests the legendary power of the alt.
 * This test is not to be considered in any way extensive, as that would require tests
 * on all possible channels types in a variety of orders (way too many combinations)!<br>
 * <br>
 * The tests here are just designed to check some basic behaviour. Good luck!<br>
 * <br>
 * 23rd March - day of the alt
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
	
	/**
	 * This test is designed just to check that alts can interact with each other remotely correctly.
	 * Specifically, it will test that separate branches can individually interact, and that alts are capable
	 * of interacting with themselves
	 */
	@Test
	public void testSimpleInteractions() {
		final Semaphore finishedSemaphore = new Semaphore(0);
		OneOneChannel<Integer> alice = new OneOneChannel<Integer>();
		OneOneChannel<Integer> bob = new OneOneChannel<Integer>();
		AltBuilder builder = new AltBuilder();
		final Box<Integer> aliceMessageBox = new Box<Integer>(0);
		final Box<Integer> bobMessageBox = new Box<Integer>(0);
		final Box<Boolean> aliceGotMessage = new Box<Boolean>(false);
		final Box<Boolean> bobGotMessage = new Box<Boolean>(false);
		builder = builder.addReadBranch(alice, new ReadProcess<Integer>() {
			@Override
			public void run(Integer message) {
				aliceMessageBox.setItem(message);
				aliceGotMessage.setItem(true);
				finishedSemaphore.release();
			}
		});
		builder = builder.addReadBranch(bob, new ReadProcess<Integer>() {
			public void run(Integer message) {
				bobMessageBox.setItem(message);
				bobGotMessage.setItem(true);
				finishedSemaphore.release();
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
		assertTrue(aliceGotMessage.getItem());
		assertFalse(bobGotMessage.getItem());
		assertTrue(aliceMessageBox.getItem().equals(6));
		//Now reset alice
		aliceGotMessage.setItem(false);
		task = scheduler.makeTask(new Runnable() {public void run() {
			alt(builderBox.getItem());
		}});
		scheduler.schedule(task);
		//Write to bob!
		write(bob,3);
		finishedSemaphore.acquireUninterruptibly();
		assertTrue(bobGotMessage.getItem());
		assertFalse(aliceGotMessage.getItem());
		assertTrue(bobMessageBox.getItem().equals(3));
		//Now reset bob
		bobGotMessage.setItem(false);
		//Next, we need to test that multiple alts can interact together...
		OneOneChannel<Integer> charlie = new OneOneChannel<Integer>();
		final Box<Integer> charlieMessageBox = new Box<Integer>(0);
		final Box<Boolean> charlieGotMessage = new Box<Boolean>(false);
		//Construct a new alt for charlie
		AltBuilder builder2 = new AltBuilder();
		final Box<AltBuilder> builderBox2 = new Box<AltBuilder>(null);
		final Box<Boolean> bobHasWritten = new Box<Boolean>(false);
		final Semaphore finishedSemaphore2 = new Semaphore(0);
		//We will allow bob to write in the second alt, and will have charlie reading.
		builder2 = builder2.addReadBranch(charlie, new ReadProcess<Integer>() {
			public void run(Integer message) {
				charlieMessageBox.setItem(message);
				charlieGotMessage.setItem(true);
				finishedSemaphore.release();
			}});
		final Box<Integer> bobsMessage = new Box<Integer>(9);
		builder2 = builder2.addWriteBranch(bob, new WriteProcess<Integer>() {
			@Override
			public Integer getMessage() throws Exception {
				//Confusingly, bob writes to bob...
				return bobsMessage.getItem();
			}
			@Override
			public void run() {
				//Nothing important to do?
				bobHasWritten.setItem(true);
				finishedSemaphore2.release();
			}});
		builderBox2.setItem(builder2);
		//Schedule these two!
		task = scheduler.makeTask(new Runnable() {public void run() {
			alt(builderBox.getItem());
		}});
		scheduler.schedule(task);
		task = scheduler.makeTask(new Runnable() {public void run() {
			alt(builderBox2.getItem());
		}});
		scheduler.schedule(task);
		//Wait for them
		finishedSemaphore.acquireUninterruptibly();
		finishedSemaphore2.acquireUninterruptibly();
		//Now perform lots of checks...
		assertTrue(bobHasWritten.getItem());
		assertTrue(bobGotMessage.getItem());
		assertTrue(bobMessageBox.getItem().equals(bobsMessage.getItem()));
		assertFalse(aliceGotMessage.getItem());
		assertFalse(charlieGotMessage.getItem());
		final Box<Integer> alicesMessage = new Box<Integer>(21);
		final Box<Boolean> aliceHasWritten = new Box<Boolean>(false);
		//Finally, check a slightly more complicated scenario, where there are two branches both able
		//to activate. Check this one repeatedly...
		builder2 = builder2.addWriteBranch(alice, new WriteProcess<Integer>() {
			@Override
			public Integer getMessage() throws Exception {
				//Confusingly, bob writes to bob...
				return alicesMessage.getItem();
			}
			@Override
			public void run() {
				//Nothing important to do?
				aliceHasWritten.setItem(true);
				finishedSemaphore2.release();
			}});
		for (int i=0; i<100; i++) {
			//Reset flags
			bobGotMessage.setItem(false);
			aliceGotMessage.setItem(false);
			bobHasWritten.setItem(false);
			aliceHasWritten.setItem(false);
			//Change the messages
			alicesMessage.setItem(new Random().nextInt());
			bobsMessage.setItem(new Random().nextInt());
			//Now run the alts
			task = scheduler.makeTask(new Runnable() {public void run() {
				alt(builderBox.getItem());
			}});
			scheduler.schedule(task);
			task = scheduler.makeTask(new Runnable() {public void run() {
				alt(builderBox2.getItem());
			}});
			scheduler.schedule(task);
			//Wait for finish
			finishedSemaphore.acquireUninterruptibly();
			finishedSemaphore2.acquireUninterruptibly();
			//Check what happened
			assertTrue(aliceGotMessage.getItem() || bobGotMessage.getItem());
			assertTrue(aliceHasWritten.getItem() || bobHasWritten.getItem());
			assertTrue(!aliceGotMessage.getItem() || !bobGotMessage.getItem());
			assertTrue(!aliceHasWritten.getItem() || !bobHasWritten.getItem());
			//Only one interacted
			if (aliceGotMessage.getItem()) {
				assertTrue(aliceMessageBox.getItem().equals(alicesMessage.getItem()));
			} else {
				assertTrue(bobMessageBox.getItem().equals(bobsMessage.getItem()));
			}
		}
		//Woo?
		logger.trace("testSimpleInteractions: complete");
	}
	
	/**
	 * This test is designed to make sure that the or else branch of an alt will
	 * activate at appropriate times
	 */
	@Test
	public void testOrElse() {
		final Semaphore finishedSemaphore = new Semaphore(0);
		AltBuilder builder = new AltBuilder();
		final Box<AltBuilder> builderBox = new Box<AltBuilder>(null);
		//Test that it activates on an empty alt first
		builder = builder.addOrElseBranch(new Runnable() {public void run() {
			finishedSemaphore.release();
		}});
		builderBox.setItem(builder);
		//Now run the alt
		Task task = scheduler.makeTask(new Runnable() {public void run() {
			alt(builderBox.getItem());
		}});
		scheduler.schedule(task);
		//Wait for a response
		finishedSemaphore.acquireUninterruptibly();
		//Good! Now see if an alt with an alternative branch not ready immediately will acitvate...
		final OneOneChannel<Integer> alice = new OneOneChannel<Integer>();
		builder = builder.addReadBranch(alice, new ReadProcess<Integer>() {
			@Override
			public void run(Integer message) {
				fail("testOrElse: Alice managed to read from an alt without a writer...");
			}});
		//Run it...
		builderBox.setItem(builder);
		task = scheduler.makeTask(new Runnable() {public void run() {
			alt(builderBox.getItem());
		}});
		scheduler.schedule(task);
		//Wait for a response
		finishedSemaphore.acquireUninterruptibly();
		//Good, but check Alice can't still respond...
		task = scheduler.makeTask(new Runnable() {public void run() {
			try {write(alice,9);
				fail("testOrElse: managed to write to Alice");
			} catch (ProcessInterruptedException interrupted) {
				finishedSemaphore.release();
			}
		}});
		scheduler.schedule(task);
		//Wait a bit...
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			logger.warn("testOrElse: interrupted while waiting");
		}
		//Now interrupt it, and check that we didn't write
		scheduler.interrupt(task);
		finishedSemaphore.acquireUninterruptibly();
		//Good!
		
		//Next, we want to check that it won't activate when it is not wanted...
		//This will involve timing tests for simplicity...
		builder = new AltBuilder();
		builder = builder.addOrElseBranch(new Runnable() {public void run() {
			fail("testOrElse: or else activated when Alice should have been waiting");
		}});
		final Box<Integer> aliceMessageBox = new Box<Integer>(0);
		final Box<Boolean> aliceGotMessage = new Box<Boolean>(false);
		builder = builder.addReadBranch(alice, new ReadProcess<Integer>() {
			@Override
			public void run(Integer message) {
				aliceMessageBox.setItem(message);
				aliceGotMessage.setItem(true);
				finishedSemaphore.release();
			}
		});
		final int message = 98;
		//Start another thread trying to write...
		task = scheduler.makeTask(new Runnable() {public void run() {
			finishedSemaphore.release();
			write(alice,message);
		}});
		//Wait a bit...
		scheduler.schedule(task);
		finishedSemaphore.acquireUninterruptibly();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			logger.warn("testOrElse: interrupted while waiting");
		}
		//Run it...
		builderBox.setItem(builder);
		task = scheduler.makeTask(new Runnable() {public void run() {
			alt(builderBox.getItem());
		}});
		scheduler.schedule(task);
		//Make sure alice wrote!
		finishedSemaphore.acquireUninterruptibly();
		assertTrue(aliceGotMessage.getItem());
		assertTrue(aliceMessageBox.getItem().equals(message));
		//Good work!
		logger.trace("testOrElse: complete");
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
