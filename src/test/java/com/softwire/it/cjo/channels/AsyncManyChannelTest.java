package com.softwire.it.cjo.channels;

import static com.softwire.it.cjo.operators.Ops.*;
import static org.junit.Assert.*;

import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.softwire.it.cjo.channels.exceptions.ChannelClosed;
import com.softwire.it.cjo.operators.Channel;
import com.softwire.it.cjo.operators.exceptions.ProcessInterruptedException;
import com.softwire.it.cjo.utilities.Box;

/**
 * ****************<br>
 * Date: 19/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class tests the correctness of the AsyncManyChannel.<br>
 * Being asynchronous, this is actually a bit easier to test in a lot of ways...
 *
 */
public class AsyncManyChannelTest {
	//The logger for these tests
	private final Logger logger = Logger.getLogger(AsyncManyChannelTest.class);

	/**
	 * Test that writing is asynchronous on this channel (readers should still wait for writers)
	 */
	@Test
	public void testAsync() {
		final Channel<Integer> channel = new AsyncManyChannel<Integer>();
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
			logger.warn("testAsync: interrupted while waiting");
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
		logger.trace("testAsync: complete");
	}
	
	/**
	 * Tests that the read operation can be interrupted correctly. The write operation can't be interrupted since it is
	 * asynchronous! We also test that is the case...
	 */
	@Test
	public void testInterruptions() {
		final Channel<Integer> channel = new AsyncManyChannel<Integer>();
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
		final Box<Semaphore> waitSem2 = new Box<Semaphore>(new Semaphore(0));
		assertTrue(gotException.getItem());
		//Check that writers cannot be interrupted...
		t = new Thread(new Runnable() {public void run() {
			//Start reading...
			try {
				waitSem.getItem().acquireUninterruptibly();
				write(channel,0);
				waitSem2.getItem().release();
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
		waitSem2.getItem().acquireUninterruptibly();
		//Read the message to get rid of it
		read(channel);
		
		//Now test that multiple readers can all be interrupted correctly...
		gotException.setItem(false);
		waitSem2.setItem(new Semaphore(0));
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
		//Done!
		logger.trace("testInterruptions: complete");
	}
	
	/**
	 * Test the channel closed exception is thrown at the right times
	 */
	@Test
	public void testChannelClosed() {
		//This is quite painful - async one channels should be closed by close.
		//We will check each one in turn...
		final Box<Channel<Integer>> channel = new Box<Channel<Integer>>(new AsyncManyChannel<Integer>());
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem = new Box<Semaphore>(new Semaphore(0));
		//Check that we can interrupt threads correctly...
		Thread t;
		for (int i=0;i<1;i++) {
			//i=0, closein, i=1, closeout, i=2, close
			channel.setItem(new AsyncManyChannel<Integer>());
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
		channel.setItem(new AsyncManyChannel<Integer>());
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
		
		channel.setItem(new AsyncManyChannel<Integer>());
		closeReadEnd(channel.getItem());
		message.setItem(0);
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
	
	//The number of writers to include...
	private static final int NO_READERS = 10;
	private static final int NO_WRITERS = 10;
	/**
	 * Test that many writing threads and many reading threads can interact properly with each other.
	 */
	@Test
	public void testStress() {
		final Channel<Integer> channel = new AsyncManyChannel<Integer>();
		Thread[] readers = new Thread[NO_READERS];
		for (int i=0; i<NO_READERS; i++) {
			readers[i] = new Thread(new Runnable() {public void run() {
				while(true) {
					try {
						read(channel);
					} catch (ChannelClosed c) {
						break;
					} //finish
				}
			}});
			readers[i].start();
		}
		Thread[] writers = new Thread[NO_WRITERS];
		for (int i=0; i<NO_WRITERS; i++) {
			writers[i] = new Thread(new Runnable() {public void run() {
				while(true) {
					try {
						write(channel,8);
					} catch (ChannelClosed c) {
						break;
					} //finish
				}
			}});
			writers[i].start();
		}
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
