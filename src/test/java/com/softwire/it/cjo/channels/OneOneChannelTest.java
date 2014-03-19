package com.softwire.it.cjo.channels;

import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.softwire.it.cjo.channels.OneOneChannel;
import com.softwire.it.cjo.channels.exceptions.ChannelClosed;
import com.softwire.it.cjo.channels.exceptions.RegistrationException;
import com.softwire.it.cjo.operators.Channel;
import com.softwire.it.cjo.operators.exceptions.ProcessInterruptedException;
import com.softwire.it.cjo.utilities.Box;

import static com.softwire.it.cjo.operators.Ops.*;
import static org.junit.Assert.*;

/**
 * ****************<br>
 * Date: 16/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class tests the correctness of the OneOneChannel
 *
 */
public class OneOneChannelTest {
	//The logger for these tests
	private final Logger logger = Logger.getLogger(OneOneChannelTest.class);

	/**
	 * Test that readers and writers are forced to wait for each other on the channel
	 */
	@Test
	public void testSync() {
		final Channel<Integer> channel = new OneOneChannel<Integer>();
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
		final Channel<Integer> channel = new OneOneChannel<Integer>();
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
		logger.trace("testInterruptions: complete");
	}
	
	/**
	 * Test the channel closed exception is thrown at the right times
	 */
	@Test
	public void testChannelClosed() {
		//This is quite painful - one one channels should be closed by closein, closeout, or close.
		//We will check each one in turn...
		final Box<Channel<Integer>> channel = new Box<Channel<Integer>>(new OneOneChannel<Integer>());
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem = new Box<Semaphore>(new Semaphore(0));
		//Check that we can interrupt threads correctly...
		Thread t;
		for (int i=0;i<3;i++) {
			//i=0, closein, i=1, closeout, i=2, close
			channel.setItem(new OneOneChannel<Integer>());
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
			} else if (i==1) {
				closeWriteEnd(channel.getItem());
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
			//Repeat for writing...
			channel.setItem(new OneOneChannel<Integer>());
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
				closeReadEnd(channel.getItem());
			} else if (i==1) {
				closeWriteEnd(channel.getItem());
			} else {
				close(channel.getItem());
			}
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
		logger.trace("testChannelClosed: complete");
	}
	
	/**
	 * Test that only one reader and one writer can use the channel at once...
	 */
	@Test
	public void testOneOne() {
		final Channel<Integer> channel = new OneOneChannel<Integer>();
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
		
		//Now repeat for writers...
		sendMessage.setItem(78);
		message.setItem(0);
		gotException.setItem(false);
		t = new Thread(new Runnable() {public void run() {
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
		logger.trace("testOneOne: complete");
	}
	
	/**
	 * This reads and writers between two threads very frequently for a fixed amount of time
	 * to see if any rare concurrency bugs are caught...
	 */
	@Test
	public void testStress() {
		final Channel<Integer> channel = new OneOneChannel<Integer>();
		Thread t1 = new Thread(new Runnable() {public void run() {
			while (true) {
				try {
					read(channel);
					write(channel,2);
				} catch (ChannelClosed c) {
					break;
				}
			}
		}});
		Thread t2 = new Thread(new Runnable() {public void run() {
			while (true) {
				try {
					write(channel,8);
					read(channel);
				} catch (ChannelClosed c) {
					break;
				}
			}
		}});
		//Now execute the threads for a while...
		t1.start();
		t2.start();
		//Wait for a bit...
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			logger.warn("testChannelClosed: interrupted while waiting");
		}
		//Close the channel (and hopefully terminate)
		close(channel);
		logger.trace("testStress: complete");
	}
}
