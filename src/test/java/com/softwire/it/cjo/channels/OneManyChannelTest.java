package com.softwire.it.cjo.channels;

import java.util.concurrent.Semaphore;

import mjb.dev.cjo.channels.OneManyChannel;
import mjb.dev.cjo.channels.exceptions.ChannelClosed;
import mjb.dev.cjo.channels.exceptions.RegistrationException;
import mjb.dev.cjo.operators.Channel;
import mjb.dev.cjo.operators.exceptions.ProcessInterruptedException;
import mjb.dev.cjo.utilities.Box;

import org.apache.log4j.Logger;
import org.junit.Test;


import static mjb.dev.cjo.operators.Ops.*;
import static org.junit.Assert.*;

/**
 * ****************<br>
 * Date: 19/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class tests the correctness of the OneManyChannel
 * (one writer - many readers)
 *
 */
public class OneManyChannelTest {
	//The logger for these tests
	private final Logger logger = Logger.getLogger(OneManyChannelTest.class);

	/**
	 * Test that readers and writers are forced to wait for each other on the channel
	 */
	@Test
	public void testSync() {
		final Channel<Integer> channel = new OneManyChannel<Integer>();
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
			logger.warn("testSync: interrupted while waiting");
		}
		//Now write...
		write(channel,3);
		//Lock on
		waitSem.getItem().acquireUninterruptibly();
		//Check it was made to wait
		assertTrue(time.getItem()-startTime>1000);
		assertTrue(message.getItem()==3);
		
		//Now test that a writer can be made to wait...
		startTime = System.currentTimeMillis();
		t = new Thread(new Runnable() {public void run() {
			//Start reading...
			write(channel,5);
			//Set the time
			time.setItem(System.currentTimeMillis());
			//Release...
			waitSem.getItem().release();
		}});
		t.start();
		//Wait for a while.
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			//Problem
			logger.warn("testSync: interrupted while waiting");
		}
		//Read...
		message.setItem(read(channel));
		//Lock on
		waitSem.getItem().acquireUninterruptibly();
		//Check it was made to wait
		assertTrue(time.getItem()-startTime>1000);
		assertTrue(message.getItem()==5);
		
		//Now test for a couple of readers being added at once...
		final Box<Long> time2 = new Box<Long>(0L);
		final Box<Integer> message2 = new Box<Integer>(0);
		final Box<Semaphore> waitSem2 = new Box<Semaphore>(new Semaphore(0));
		startTime = System.currentTimeMillis();
		t = new Thread(new Runnable() {public void run() {
			//Start reading...
			message.setItem(read(channel));
			//Set the time
			time.setItem(System.currentTimeMillis());
			//Release...
			waitSem.getItem().release();
		}});
		Thread t2 = new Thread(new Runnable() {public void run() {
			//Start reading...
			message2.setItem(read(channel));
			//Set the time
			time2.setItem(System.currentTimeMillis());
			//Release...
			waitSem2.getItem().release();
		}});
		t.start();
		t2.start();
		//Now wait for a while
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			//Problem
			logger.warn("testSync: interrupted while waiting");
		}
		//Get the messages...
		//Read...
		write(channel,5);
		write(channel,4);
		//Lock on
		waitSem.getItem().acquireUninterruptibly();
		waitSem2.getItem().acquireUninterruptibly();
		//Check it was made to wait
		assertTrue(time.getItem()-startTime>1000);
		assertTrue(time2.getItem()-startTime>1000);
		assertTrue(message.getItem()==5 || message2.getItem()==5);
		assertTrue(message.getItem()==4 || message2.getItem()==4);
		//Good!
		logger.trace("testSync: complete");
	}
	
	/**
	 * Tests that the read and write operations applied to channels can be interrupted correctly.
	 * (These operations are considered the fundamental operations - others are not checked like this
	 * and should be checked by each operation - in a channel non-specific way)
	 */
	@Test
	public void testInterruptions() {
		final Channel<Integer> channel = new OneManyChannel<Integer>();
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
		assertTrue(gotException.getItem());
		//Check that writers can be interrupted properly too...
		gotException.setItem(false);
		t = new Thread(new Runnable() {public void run() {
			//Start writing...
			try {
				write(channel,4);
				fail("testInterruptions: managed to write to no reader in a channel");
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
		assertTrue(gotException.getItem());
		
		//Now test that multiple readers can all be interrupted correctly...
		gotException.setItem(false);
		final Box<Semaphore> waitSem2 = new Box<Semaphore>(new Semaphore(0));
		final Box<Boolean> gotException2 = new Box<Boolean>(false);
		Thread t2 = new Thread(new Runnable() {public void run() {
			//Start reading...
			try {
				read(channel);
				fail("testInterruptions: managed to read nothing from a channel");
			} catch (ProcessInterruptedException e) {
				//Check that the thread is still interrupted
				assertTrue(Thread.currentThread().isInterrupted());
				gotException2.setItem(true);
				waitSem2.getItem().release();
			}
		}});
		t = new Thread(new Runnable() {public void run() {
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
		t2.start();
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
		//Check the other hasn't been interrupted yet
		assertTrue(gotException.getItem());
		assertTrue(!gotException2.getItem());
		t2.interrupt();
		//Wait...
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			logger.warn("testInterruptions: interrupted while waiting");
		}
		waitSem2.getItem().acquireUninterruptibly();
		assertTrue(gotException2.getItem());
		//Good!
		logger.trace("testInterruptions: complete");
	}
	/**
	 * Test the channel closed exception is thrown at the right times
	 */
	@Test
	public void testChannelClosed() {
		//This is quite painful - one many channels should be closed when the write end is closed, or when the whole channel
		//is closed. Otherwise, there should be no effect
		final Box<Channel<Integer>> channel = new Box<Channel<Integer>>(new OneManyChannel<Integer>());
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem = new Box<Semaphore>(new Semaphore(0));
		//Check that we can interrupt threads correctly...
		Thread t;
		for (int i=0;i<2;i++) {
			channel.setItem(new OneManyChannel<Integer>());
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
				closeWriteEnd(channel.getItem());
			} else {
				close(channel.getItem());
			}
			assertTrue(channel.getItem().isClosed());
			//Wait for the exception
			waitSem.getItem().acquireUninterruptibly();
			//Now check
			assertTrue(gotException.getItem());
			//Now try reading when it is already closed
			try {
				read(channel.getItem());
				fail("testChannelClosed: managed to read from a closed channel");
			} catch (ChannelClosed c) {	}
			//Repeat for writing...
			channel.setItem(new OneManyChannel<Integer>());
			gotException.setItem(false);
			t = new Thread(new Runnable() {public void run() {
				try {
					write(channel.getItem(),2);
					fail("testChannelClosed: managed to write to a closed channel");
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
				closeWriteEnd(channel.getItem());
			} else {
				close(channel.getItem());
			}
			assertTrue(channel.getItem().isClosed());
			//Wait for the exception
			waitSem.getItem().acquireUninterruptibly();
			//Now check
			assertTrue(gotException.getItem());
			//Now try reading when it is already closed
			try {
				write(channel.getItem(),17);
				fail("testChannelClosed: managed to write to a closed channel");
			} catch (ChannelClosed c) {	}
			//Done!
		}
		waitSem.setItem(new Semaphore(0));
		//Now check that the other closing possibilities have no effect...
		channel.setItem(new OneManyChannel<Integer>());
		closeReadEnd(channel.getItem());
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
	 * Test that only one writer can use the channel at once...
	 * (multiple writers has been tested previously)
	 */
	@Test
	public void testOneMany() {
		final Channel<Integer> channel = new OneManyChannel<Integer>();
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem1 = new Box<Semaphore>(new Semaphore(0));
		final Box<Semaphore> waitSem2 = new Box<Semaphore>(new Semaphore(0));
		final Box<Integer> message = new Box<Integer>(0);
		final Box<Integer> sendMessage = new Box<Integer>(901);
		//Now repeat for writers...
		sendMessage.setItem(78);
		message.setItem(0);
		gotException.setItem(false);
		Thread t = new Thread(new Runnable() {public void run() {
			//Try writing...
			try {
				write(channel,sendMessage.getItem());
				waitSem1.getItem().release();
				waitSem2.getItem().acquireUninterruptibly();
			} catch (RegistrationException e) {
				//Got it
				gotException.setItem(true);
				//Write!
				message.setItem(read(channel));
				waitSem2.getItem().release();
				waitSem1.getItem().acquireUninterruptibly();
			}
		}});
		//Start the thread
		t.start();
		//Try writing myself
		try {
			write(channel,sendMessage.getItem());
			waitSem1.getItem().release();
			waitSem2.getItem().acquireUninterruptibly();
		} catch (RegistrationException e) {
			//Got it
			gotException.setItem(true);
			//Write!
			message.setItem(read(channel));
			waitSem2.getItem().release();
			waitSem1.getItem().acquireUninterruptibly();
		}
		//Now we should both be out. Check we got the message, and the exception
		assertTrue(gotException.getItem());
		assertTrue(message.getItem().equals(sendMessage.getItem()));
		logger.trace("testOneMany: complete");
	}
	//The number of writers to include...
	private static final int NO_READERS = 10;
	
	/**
	 * Test that many writing threads and a reading thread can interact properly with each other.
	 */
	@Test
	public void testStress() {
		final Channel<Integer> channel = new OneManyChannel<Integer>();
		Thread[] threads = new Thread[NO_READERS];
		for (int i=0; i<NO_READERS; i++) {
			threads[i] = new Thread(new Runnable() {public void run() {
				while(true) {
					try {
						read(channel);
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
					write(channel,8);
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
