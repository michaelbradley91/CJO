package com.softwire.it.cjo.channels;

import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.softwire.it.cjo.channels.exceptions.ChannelClosed;
import com.softwire.it.cjo.operators.Channel;
import com.softwire.it.cjo.operators.exceptions.ProcessInterruptedException;
import com.softwire.it.cjo.utilities.Box;

import static com.softwire.it.cjo.operators.Ops.*;
import static org.junit.Assert.*;

/**
 * ****************<br>
 * Date: 19/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class tests the correctness of the BufferManyChannel
 * (many writers - many readers)
 *
 */
public class BufferManyChannelTest {
	//The logger for these tests
	private final Logger logger = Logger.getLogger(BufferManyChannelTest.class);

	/**
	 * Test that we can't make a buffer with negative capacity
	 */
	@Test
	public void testIllegalInstantiation() {
		try {
			new BufferManyChannel<Integer>(-1);
			fail("testIllegalInstantiation: managed to create a buffered channel with negative capacity");
		} catch (IllegalArgumentException e) {}
		try {
			new BufferManyChannel<Integer>(-12);
			fail("testIllegalInstantiation: managed to create a buffered channel with negative capacity");
		} catch (IllegalArgumentException e) {}
		logger.trace("testIllegalInstantiation: complete");
	}
	
	//The first tests all wipe out the zero buffer special case,
	//which should behave in exactly the same way as a many many channel

	/**
	 * Test that readers and writers are forced to wait for each other on the channel
	 */
	@Test
	public void testSyncZeroCapacity() {
		final Channel<Integer> channel = new BufferManyChannel<Integer>(0);
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
			logger.warn("testSyncZeroCapacity: interrupted while waiting");
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
			logger.warn("testSyncZeroCapacity: interrupted while waiting");
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
			logger.warn("testSyncZeroCapacity: interrupted while waiting");
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
		
		//Now test for a couple of writers...
		time2.setItem(0L);
		message2.setItem(0);
		waitSem2.setItem(new Semaphore(0));
		startTime = System.currentTimeMillis();
		t = new Thread(new Runnable() {public void run() {
			//Start reading...
			write(channel,10);
			//Set the time
			time.setItem(System.currentTimeMillis());
			//Release...
			waitSem.getItem().release();
		}});
		t2 = new Thread(new Runnable() {public void run() {
			//Start reading...
			write(channel,6);
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
			logger.warn("testSyncZeroCapacity: interrupted while waiting");
		}
		//Get the messages...
		//Read...
		message.setItem(read(channel));
		message2.setItem(read(channel));
		//Lock on
		waitSem.getItem().acquireUninterruptibly();
		waitSem2.getItem().acquireUninterruptibly();
		//Check it was made to wait
		assertTrue(time.getItem()-startTime>1000);
		assertTrue(time2.getItem()-startTime>1000);
		assertTrue(message.getItem()==6 || message2.getItem()==6);
		assertTrue(message.getItem()==10 || message2.getItem()==10);
		//Good!
		logger.trace("testSyncZeroCapacity: complete");
	}
	
	/**
	 * Tests that the read and write operations applied to channels can be interrupted correctly.
	 * (These operations are considered the fundamental operations - others are not checked like this
	 * and should be checked by each operation - in a channel non-specific way)
	 */
	@Test
	public void testInterruptionsZeroCapacity() {
		final Channel<Integer> channel = new BufferManyChannel<Integer>(0);
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem = new Box<Semaphore>(new Semaphore(0));
		//Check that we can interrupt threads correctly...
		Thread t = new Thread(new Runnable() {public void run() {
			//Start reading...
			try {
				read(channel);
				fail("testInterruptionsZeroCapacity: managed to read nothing from a channel");
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
			logger.warn("testInterruptionsZeroCapacity: interrupted while waiting");
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
				fail("testInterruptionsZeroCapacity: managed to write to no reader in a channel");
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
			logger.warn("testInterruptionsZeroCapacity: interrupted while waiting");
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
				fail("testInterruptionsZeroCapacity: managed to read nothing from a channel");
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
				fail("testInterruptionsZeroCapacity: managed to read nothing from a channel");
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
			logger.warn("testInterruptionsZeroCapacity: interrupted while waiting");
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
			logger.warn("testInterruptionsZeroCapacity: interrupted while waiting");
		}
		waitSem2.getItem().acquireUninterruptibly();
		assertTrue(gotException2.getItem());
		
		//Now test that multiple writers can all be interrupted correctly...
		gotException.setItem(false);
		waitSem2.setItem(new Semaphore(0));
		gotException2.setItem(false);
		t2 = new Thread(new Runnable() {public void run() {
			//Start reading...
			try {
				write(channel,0);
				fail("testInterruptionsZeroCapacity: managed to write to no reader in a channel");
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
				write(channel,-12);
				fail("testInterruptionsZeroCapacity: managed to write to no reader in a channel");
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
			logger.warn("testInterruptionsZeroCapacity: interrupted while waiting");
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
			logger.warn("testInterruptionsZeroCapacity: interrupted while waiting");
		}
		waitSem2.getItem().acquireUninterruptibly();
		assertTrue(gotException2.getItem());
		//Good!
		logger.trace("testInterruptionsZeroCapacity: complete");
	}
	
	/**
	 * Test the channel closed exception is thrown at the right times
	 */
	@Test
	public void testChannelClosedZeroCapacity() {
		//This is quite painful - many many channels should be closed when the write end is closed, or when the whole channel
		//is closed. Otherwise, there should be no effect
		final Box<Channel<Integer>> channel = new Box<Channel<Integer>>(new BufferManyChannel<Integer>(0));
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem = new Box<Semaphore>(new Semaphore(0));
		//Check that we can interrupt threads correctly...
		Thread t;
		for (int i=0;i<1;i++) {
			channel.setItem(new BufferManyChannel<Integer>(0));
			gotException.setItem(false);
			//Firstly, try adding a reader, and catch the exception...
			t = new Thread(new Runnable() {public void run() {
				try {
					read(channel.getItem());
					fail("testChannelClosedZeroCapacity: managed to read from a closed channel");
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
				logger.warn("testChannelClosedZeroCapacity: interrupted while waiting");
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
				fail("testChannelClosedZeroCapacity: managed to read from a closed channel");
			} catch (ChannelClosed c) {	}
			//Repeat for writing...
			channel.setItem(new BufferManyChannel<Integer>(0));
			gotException.setItem(false);
			t = new Thread(new Runnable() {public void run() {
				try {
					write(channel.getItem(),2);
					fail("testChannelClosedZeroCapacity: managed to write to a closed channel");
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
				logger.warn("testChannelClosedZeroCapacity: interrupted while waiting");
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
				write(channel.getItem(),17);
				fail("testChannelClosedZeroCapacity: managed to write to a closed channel");
			} catch (ChannelClosed c) {	}
			//Done!
		}
		waitSem.setItem(new Semaphore(0));
		//Now check that the other closing possibilities have no effect...
		channel.setItem(new BufferManyChannel<Integer>(0));
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
		
		waitSem.setItem(new Semaphore(0));
		//Now check that the other closing possibilities have no effect...
		channel.setItem(new BufferManyChannel<Integer>(0));
		closeWriteEnd(channel.getItem());
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
		logger.trace("testChannelClosedZeroCapacity: complete");
	}
	
	//The number of writers to include...
	private static final int NO_READERS = 10;
	private static final int NO_WRITERS = 10;
	/**
	 * Test that many writing threads and many reading threads can interact properly with each other.
	 */
	@Test
	public void testStressZeroCapacity() {
		final Channel<Integer> channel = new BufferManyChannel<Integer>(0);
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
			logger.warn("testStressZeroCapacity: interrupted while waiting");
		}
		//Close the channel (and hopefully terminate)
		close(channel);
		logger.trace("testStressZeroCapacity: complete");
	}
	
	//With the buffer filled, it should behave as above, but we bother to check...
	/**
	 * Test that readers and writers are forced to wait for each other on the channel
	 */
	@Test
	public void testSyncFilled() {
		final Channel<Integer> channel = new BufferManyChannel<Integer>(3);
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
			logger.warn("testSyncFilled: interrupted while waiting");
		}
		//Now write...
		write(channel,3);
		//Lock on
		waitSem.getItem().acquireUninterruptibly();
		//Check it was made to wait
		assertTrue(time.getItem()-startTime>1000);
		assertTrue(message.getItem()==3);
		
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
			logger.warn("testSyncFilled: interrupted while waiting");
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
		
		//Now test that a writer can be made to wait...
		startTime = System.currentTimeMillis();
		//Fill the buffer
		write(channel,5);
		write(channel,5);
		write(channel,5);
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
			logger.warn("testSyncFilled: interrupted while waiting");
		}
		//Read...
		message.setItem(read(channel));
		//Lock on
		waitSem.getItem().acquireUninterruptibly();
		//Check it was made to wait
		assertTrue(time.getItem()-startTime>1000);
		assertTrue(message.getItem()==5);
		
		//Now test for a couple of writers...
		time2.setItem(0L);
		message2.setItem(0);
		waitSem2.setItem(new Semaphore(0));
		startTime = System.currentTimeMillis();
		t = new Thread(new Runnable() {public void run() {
			//Start reading...
			write(channel,5);
			//Set the time
			time.setItem(System.currentTimeMillis());
			//Release...
			waitSem.getItem().release();
		}});
		t2 = new Thread(new Runnable() {public void run() {
			//Start reading...
			write(channel,5);
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
			logger.warn("testSyncFilled: interrupted while waiting");
		}
		//Get the messages...
		//Read...
		message.setItem(read(channel));
		message2.setItem(read(channel));
		//Lock on
		waitSem.getItem().acquireUninterruptibly();
		waitSem2.getItem().acquireUninterruptibly();
		//Check it was made to wait
		assertTrue(time.getItem()-startTime>1000);
		assertTrue(time2.getItem()-startTime>1000);
		assertTrue(message.getItem()==5 && message2.getItem()==5);
		//Good!
		logger.trace("testSyncFilled: complete");
	}
	
	/**
	 * Tests that the read and write operations applied to channels can be interrupted correctly.
	 * (These operations are considered the fundamental operations - others are not checked like this
	 * and should be checked by each operation - in a channel non-specific way)
	 */
	@Test
	public void testInterruptionsFilled() {
		final Channel<Integer> channel = new BufferManyChannel<Integer>(3);
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem = new Box<Semaphore>(new Semaphore(0));
		//Check that we can interrupt threads correctly...
		Thread t = new Thread(new Runnable() {public void run() {
			//Start reading...
			try {
				read(channel);
				fail("testInterruptionsFilled: managed to read nothing from a channel");
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
			logger.warn("testInterruptionsFilled: interrupted while waiting");
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
				fail("testInterruptionsFilled: managed to read nothing from a channel");
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
				fail("testInterruptionsFilled: managed to read nothing from a channel");
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
			logger.warn("testInterruptionsFilled: interrupted while waiting");
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
			logger.warn("testInterruptionsFilled: interrupted while waiting");
		}
		waitSem2.getItem().acquireUninterruptibly();
		assertTrue(gotException2.getItem());
		
		//Check that writers can be interrupted properly too...
		gotException.setItem(false);
		//Fill it
		write(channel,4);
		write(channel,4);
		write(channel,4);
		t = new Thread(new Runnable() {public void run() {
			//Start writing...
			try {
				write(channel,4);
				fail("testInterruptionsFilled: managed to write to no reader in a channel");
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
			logger.warn("testInterruptionsFilled: interrupted while waiting");
		}
		//Now interrupt the thread...
		t.interrupt();
		//Now check it got the exception
		waitSem.getItem().acquireUninterruptibly();
		assertTrue(gotException.getItem());
		
		//Now test that multiple writers can all be interrupted correctly...
		gotException.setItem(false);
		waitSem2.setItem(new Semaphore(0));
		gotException2.setItem(false);
		t2 = new Thread(new Runnable() {public void run() {
			//Start reading...
			try {
				write(channel,0);
				fail("testInterruptionsFilled: managed to write to no reader in a channel");
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
				write(channel,-12);
				fail("testInterruptionsFilled: managed to write to no reader in a channel");
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
			logger.warn("testInterruptionsFilled: interrupted while waiting");
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
			logger.warn("testInterruptionsFilled: interrupted while waiting");
		}
		waitSem2.getItem().acquireUninterruptibly();
		assertTrue(gotException2.getItem());
		//Good!
		logger.trace("testInterruptionsFilled: complete");
	}
	
	/**
	 * Test the channel closed exception is thrown at the right times
	 */
	@Test
	public void testChannelClosedFilled() {
		//This is quite painful - many many channels should be closed when the write end is closed, or when the whole channel
		//is closed. Otherwise, there should be no effect
		final Box<Channel<Integer>> channel = new Box<Channel<Integer>>(new BufferManyChannel<Integer>(3));
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem = new Box<Semaphore>(new Semaphore(0));
		//Check that we can interrupt threads correctly...
		Thread t;
		for (int i=0;i<1;i++) {
			channel.setItem(new BufferManyChannel<Integer>(3));
			gotException.setItem(false);
			//Firstly, try adding a reader, and catch the exception...
			t = new Thread(new Runnable() {public void run() {
				try {
					read(channel.getItem());
					fail("testChannelClosedFilled: managed to read from a closed channel");
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
				logger.warn("testChannelClosedFilled: interrupted while waiting");
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
				fail("testChannelClosedFilled: managed to read from a closed channel");
			} catch (ChannelClosed c) {	}
			//Repeat for writing...
			channel.setItem(new BufferManyChannel<Integer>(3));
			gotException.setItem(false);
			//Fill the buffer...
			write(channel.getItem(),2);
			write(channel.getItem(),2);
			write(channel.getItem(),2);
			t = new Thread(new Runnable() {public void run() {
				try {
					write(channel.getItem(),2);
					fail("testChannelClosedFilled: managed to write to a closed channel");
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
				logger.warn("testChannelClosedFilled: interrupted while waiting");
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
				write(channel.getItem(),17);
				fail("testChannelClosedFilled: managed to write to a closed channel");
			} catch (ChannelClosed c) {	}
			//Done!
		}
		waitSem.setItem(new Semaphore(0));
		//Now check that the other closing possibilities have no effect...
		channel.setItem(new BufferManyChannel<Integer>(3));
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
		
		waitSem.setItem(new Semaphore(0));
		//Now check that the other closing possibilities have no effect...
		channel.setItem(new BufferManyChannel<Integer>(3));
		closeWriteEnd(channel.getItem());
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
		logger.trace("testChannelClosedFilled: complete");
	}
	
	/**
	 * Test that many writing threads and many reading threads can interact properly with each other.
	 */
	@Test
	public void testStress() {
		final Channel<Integer> channel = new BufferManyChannel<Integer>(3);
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
	
	//Finally, test when the buffer is not full - so it acts as an async many channel
	/**
	 * Test that writing is asynchronous on this channel (readers should still wait for writers)
	 */
	@Test
	public void testAsyncUnfilled() {
		final Channel<Integer> channel = new BufferManyChannel<Integer>(10);
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
			logger.warn("testAsyncUnfilled: interrupted while waiting");
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
			logger.warn("testAsyncUnfilled: interrupted while waiting");
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
		logger.trace("testAsyncUnfilled: complete");
	}
	
	/**
	 * Tests that the read operation can be interrupted correctly. The write operation can't be interrupted since it is
	 * asynchronous! We also test that is the case...
	 */
	@Test
	public void testInterruptionsUnfilled() {
		final Channel<Integer> channel = new BufferManyChannel<Integer>(10);
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem = new Box<Semaphore>(new Semaphore(0));
		//Check that we can interrupt threads correctly...
		Thread t = new Thread(new Runnable() {public void run() {
			//Start reading...
			try {
				read(channel);
				fail("testInterruptionsUnfilled: managed to read nothing from a channel");
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
			logger.warn("testInterruptionsUnfilled: interrupted while waiting");
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
				fail("testInterruptionsUnfilled: managed to read nothing from a channel");
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
				fail("testInterruptionsUnfilled: managed to read nothing from a channel");
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
			logger.warn("testInterruptionsUnfilled: interrupted while waiting");
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
			logger.warn("testInterruptionsUnfilled: interrupted while waiting");
		}
		waitSem2.getItem().acquireUninterruptibly();
		assertTrue(gotException2.getItem());
		//Done!
		logger.trace("testInterruptionsUnfilled: complete");
	}
	
	/**
	 * Test the channel closed exception is thrown at the right times
	 */
	@Test
	public void testChannelClosedUnfilled() {
		//This is quite painful - async one channels should be closed by close.
		//We will check each one in turn...
		final Box<Channel<Integer>> channel = new Box<Channel<Integer>>(new BufferManyChannel<Integer>(10));
		final Box<Boolean> gotException = new Box<Boolean>(false);
		final Box<Semaphore> waitSem = new Box<Semaphore>(new Semaphore(0));
		//Check that we can interrupt threads correctly...
		Thread t;
		for (int i=0;i<1;i++) {
			//i=0, closein, i=1, closeout, i=2, close
			channel.setItem(new BufferManyChannel<Integer>(10));
			gotException.setItem(false);
			//Firstly, try adding a reader, and catch the exception...
			t = new Thread(new Runnable() {public void run() {
				try {
					read(channel.getItem());
					fail("testChannelClosedUnfilled: managed to read from a closed channel");
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
				logger.warn("testChannelClosedUnfilled: interrupted while waiting");
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
				fail("testChannelClosedUnfilled: managed to read from a closed channel");
			} catch (ChannelClosed c) {	}
			//Repeat for writing... (writing can't be closed on while the write is on progress)
			try {
				write(channel.getItem(),17);
				fail("testChannelClosedUnfilled: managed to write to a closed channel");
			} catch (ChannelClosed c) {	}
			//Done!
		}
		//Now check that the other closing possibilities have no effect...
		channel.setItem(new BufferManyChannel<Integer>(10));
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
		
		channel.setItem(new BufferManyChannel<Integer>(10));
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
		logger.trace("testChannelClosedUnfilled: complete");
	}
}
