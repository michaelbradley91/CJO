package com.softwire.it.cjo.channels;

import static com.softwire.it.cjo.operators.Ops.*;
import static org.junit.Assert.*;

import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.softwire.it.cjo.channels.exceptions.ChannelClosed;
import com.softwire.it.cjo.channels.exceptions.RegistrationException;
import com.softwire.it.cjo.operators.Channel;
import com.softwire.it.cjo.operators.exceptions.ProcessInterruptedException;
import com.softwire.it.cjo.utilities.Box;

/**
 * ****************<br>
 * Date: 19/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class tests the correctness of the AsyncOneChannel.<br>
 * Being asynchronous, this is actually a bit easier to test in a lot of ways...
 *
 */
public class AsyncOneChannelTest {
	//The logger for these tests
	private final Logger logger = Logger.getLogger(AsyncOneChannelTest.class);

	/**
	 * Test that writing is asynchronous on this channel (readers should still wait for writers)
	 */
	@Test
	public void testAsync() {
		final Channel<Integer> channel = new AsyncOneChannel<Integer>();
		//Firstly, check that the reader must wait:
		final Box<Semaphore> waitSem = new Box<Semaphore>(new Semaphore(0));
		final Box<Long> time = new Box<Long>(0L);
		final Box<Integer> message = new Box<Integer>(0);
		long startTime = System.currentTimeMillis();
		Thread t = new Thread(new Runnable() {public void run() {
			//Start reading...
			message.setItem(read(channel));
			//Set the time
			time.setItem(System.currentTimeMillis());
			//Release...
			waitSem.getItem().release();
		}});
		//Start the thread...
		t.start();
		//Wait for a while.
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			//Problem
			logger.warn("testAsync: interrupted while waiting");
		}
		//Now write...
		write(channel,3);
		//Lock on
		waitSem.getItem().acquireUninterruptibly();
		//Check it was made to wait
		assertTrue(time.getItem()-startTime>1000);
		assertTrue(message.getItem()==3);
		//Now test that a writer can write asynchronously
		write(channel,5);
		assertTrue(read(channel)==5);
		//And test multiple writers
		write(channel,17);
		write(channel,6);
		//Fairness is guaranteed at the moment, but we don't require it
		int first = read(channel);
		int second = read(channel);
		//Check that these are what we expected
		assertTrue(first==17 || second==17);
		assertTrue(first==6 || second==6);
		logger.trace("testAsync: complete");
	}
	
	/**
	 * Tests that the read operation can be interrupted correctly. The write operation can't be interrupted since it is
	 * asynchronous! We also test that is the case...
	 */
	@Test
	public void testInterruptions() {
		final Channel<Integer> channel = new AsyncOneChannel<Integer>();
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem = new Box<Semaphore>(new Semaphore(0));
		//Check that we can interrupt threads correctly...
		Thread t = new Thread(new Runnable() {public void run() {
			//Start reading...
			try {
				read(channel);
				fail("testInterruptions: managed to read nothing from a channel");
			} catch (ProcessInterruptedException e) {
				//Check that the thread is still interrupted
				assertTrue(Thread.currentThread().isInterrupted());
				gotException.setItem(true);
				waitSem.getItem().release();
			}
		}});
		t.start();
		//Wait a while
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			logger.warn("testInterruptions: interrupted while waiting");
		}
		//Now interrupt the thread...
		t.interrupt();
		//Now check it got the exception
		waitSem.getItem().acquireUninterruptibly();
		final Semaphore waitSem2 = new Semaphore(0);
		assertTrue(gotException.getItem());
		//Check that writers cannot be interrupted...
		t = new Thread(new Runnable() {public void run() {
			//Start reading...
			try {
				waitSem.getItem().acquireUninterruptibly();
				write(channel,0);
				waitSem2.release();
			} catch (ProcessInterruptedException e) {
				//Check that the thread is still interrupted
				fail("Asynchronous writer was interrupted!");
			}
		}});
		t.start();
		t.interrupt();
		//Now wake it up
		waitSem.getItem().release();
		//Now wait myself...
		waitSem2.acquireUninterruptibly();
		//Done!
		logger.trace("testInterruptions: complete");
	}
	/**
	 * Test the channel closed exception is thrown at the right times
	 */
	@Test
	public void testChannelClosed() {
		//This is quite painful - async one channels should be closed by closein, or close.
		//We will check each one in turn...
		final Box<Channel<Integer>> channel = new Box<Channel<Integer>>(new AsyncOneChannel<Integer>());
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem = new Box<Semaphore>(new Semaphore(0));
		//Check that we can interrupt threads correctly...
		Thread t;
		for (int i=0;i<2;i++) {
			//i=0, closein, i=1, closeout, i=2, close
			channel.setItem(new AsyncOneChannel<Integer>());
			gotException.setItem(false);
			//Firstly, try adding a reader, and catch the exception...
			t = new Thread(new Runnable() {public void run() {
				try {
					read(channel.getItem());
					fail("testChannelClosed: managed to read from a closed channel");
				} catch (ChannelClosed c) {
					gotException.setItem(true);
					waitSem.getItem().release();
				}
			}});
			//Start the thread
			t.start();
			//Wait a bit...
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				logger.warn("testChannelClosed: interrupted while waiting");
			}
			//Now close it!
			if (i==0) {
				closeReadEnd(channel.getItem());
			} else {
				close(channel.getItem());
			}
			//Wait for the exception
			waitSem.getItem().acquireUninterruptibly();
			//Now check
			assertTrue(gotException.getItem());
			//Now try reading when it is already closed
			try {
				read(channel.getItem());
				fail("testChannelClosed: managed to read from a closed channel");
			} catch (ChannelClosed c) {	}
			//Repeat for writing... (writing can't be closed on while the write is on progress)
			try {
				write(channel.getItem(),17);
				fail("testChannelClosed: managed to write to a closed channel");
			} catch (ChannelClosed c) {	}
			//Done!
		}
		//Now check that the other closing possibilities have no effect...
		channel.setItem(new ManyOneChannel<Integer>());
		closeWriteEnd(channel.getItem());
		final Box<Integer> message = new Box<Integer>(0);
		t = new Thread(new Runnable() {public void run() {
			//Just try to read from the channel
			message.setItem(read(channel.getItem()));
			//Good!
			waitSem.getItem().release();
		}});
		t.start();
		//Now write to it...
		write(channel.getItem(),-2);
		waitSem.getItem().acquireUninterruptibly();
		//Check the item is as expected
		assertTrue(message.getItem()==-2);
		logger.trace("testChannelClosed: complete");
	}
	
	/**
	 * Test that only one reader can use the channel at once...
	 */
	@Test
	public void testAsyncOne() {
		final Channel<Integer> channel = new ManyOneChannel<Integer>();
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem1 = new Box<Semaphore>(new Semaphore(0));
		final Box<Semaphore> waitSem2 = new Box<Semaphore>(new Semaphore(0));
		final Box<Integer> message = new Box<Integer>(0);
		final Box<Integer> sendMessage = new Box<Integer>(901);
		//Try adding multiple readers...
		Thread t = new Thread(new Runnable() {public void run() {
			//Try reading...
			try {
				message.setItem(read(channel));
				waitSem1.getItem().release();
				waitSem2.getItem().acquireUninterruptibly();
			} catch (RegistrationException e) {
				//Got it
				gotException.setItem(true);
				//Write!
				write(channel,sendMessage.getItem());
				waitSem2.getItem().release();
				waitSem1.getItem().acquireUninterruptibly();
			}
		}});
		//Start the thread
		t.start();
		//Try reading myself
		try {
			message.setItem(read(channel));
			waitSem1.getItem().release();
			waitSem2.getItem().acquireUninterruptibly();
		} catch (RegistrationException e) {
			//Got it
			gotException.setItem(true);
			//Write!
			write(channel,sendMessage.getItem());
			waitSem2.getItem().release();
			waitSem1.getItem().acquireUninterruptibly();
		}
		//Now we should both be out. Check we got the message, and the exception
		assertTrue(gotException.getItem());
		assertTrue(message.getItem().equals(sendMessage.getItem()));
		logger.trace("testAsyncOne: complete");
	}
	//The number of writers to include...
	private static final int NO_WRITERS = 10;
	
	/**
	 * Test that many writing threads and a reading thread can interact properly with each other.
	 */
	@Test
	public void testStress() {
		final Channel<Integer> channel = new ManyOneChannel<Integer>();
		Thread[] threads = new Thread[NO_WRITERS];
		for (int i=0; i<NO_WRITERS; i++) {
			threads[i] = new Thread(new Runnable() {public void run() {
				while(true) {
					try {
						write(channel,8);
					} catch (ChannelClosed c) {
						break;
					} //finish
				}
			}});
			threads[i].start();
		}
		//Get a reader going
		Thread t = new Thread(new Runnable() {public void run() {
			while(true) {
				try {
					read(channel);
				} catch (ChannelClosed c) {
					break;
				} //finish
			}
		}});
		t.start();
		//Wait for a bit...
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			logger.warn("testStress: interrupted while waiting");
		}
		//Close the channel (and hopefully terminate)
		close(channel);
		logger.trace("testStress: complete");
	}
}
