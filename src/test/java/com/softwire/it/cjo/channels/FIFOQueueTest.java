package com.softwire.it.cjo.channels;

import static org.junit.Assert.*;

import org.junit.Test;

import com.softwire.it.cjo.channels.ChannelFIFOQueue.Crate;
import com.softwire.it.cjo.utilities.exceptions.EmptyFIFOQueueException;

/**
 * ****************<br>
 * Date: 16/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * These methods test the correctness of the FIFO queue
 * 
 *
 */
public class FIFOQueueTest {
	
	/**
	 * The queue is a fairly simple object, so we just test almost everything here...
	 */
	@Test
	public void testBasics() {
		ChannelFIFOQueue<Integer> queue = new ChannelFIFOQueue<Integer>();
		//Check start up...
		assertTrue(queue.size()==0 && queue.isEmpty());
		try {
			queue.dequeue(); //should trigger an error
			fail("Able to dequeue from an empty queue");
		} catch (EmptyFIFOQueueException e) {}
		//Add a few items and dequeue them in order
		queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(3); //should be [3,2,1]
		//Now dequeue...
		assertTrue(!queue.isEmpty() && queue.size()==3);
		assertTrue(queue.dequeue()==1);
		assertTrue(!queue.isEmpty() && queue.size()==2);
		assertTrue(queue.dequeue()==2);
		assertTrue(!queue.isEmpty() && queue.size()==1);
		assertTrue(queue.dequeue()==3);
		assertTrue(queue.isEmpty() && queue.size()==0);
		//Now try using the removal...
		Crate<Integer> crate = queue.enqueue(1);
		assertTrue(!queue.isEmpty() && queue.size()==1);
		queue.remove(crate);
		//Check it is now empty...
		assertTrue(queue.isEmpty() && queue.size()==0);
		//Remove it again - should not trigger an error
		queue.remove(crate);
		//Try adding, dequeue and then removing...
		crate = queue.enqueue(1);
		assertTrue(!queue.isEmpty() && queue.size()==1);
		queue.dequeue();
		assertTrue(queue.isEmpty() && queue.size()==0);
		queue.remove(crate);
		//See if it is still empty
		assertTrue(queue.isEmpty() && queue.size()==0);
		//Now try removing an item within queue...
		crate = queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(3);
		Crate<Integer> crate2 = queue.enqueue(4);
		queue.enqueue(5);
		queue.remove(crate);
		assertTrue(queue.dequeue()==2);
		queue.remove(crate2);
		assertTrue(queue.dequeue()==3);
		crate = queue.enqueue(6);
		assertTrue(queue.dequeue()==5);
		queue.remove(crate);
		assertTrue(queue.isEmpty() && queue.size()==0);
		//Should be working!
	}
}
