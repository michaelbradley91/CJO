package com.softwire.it.cjo.utilities;

import com.softwire.it.cjo.utilities.exceptions.EmptyFIFOQueueException;

/**
 * ****************<br>
 * Date: 16/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class is a FIFO queue (first in, first out), which has O(1) enqueue, dequeue, and
 * more importantly  for this application, removal. The object is "truly" removed, so that the garbage collector
 * could take it.<br>
 * <br>
 * The queue is not thread safe and does not support keys
 * @param <T> - the type of elements being added to the queue
 *
 */
public final class FIFOQueue<T> {
	/**
	 * ****************<br>
     * Date: 16/03/2014<br>
     * Author:  michael<br>
     * ****************<br>
     * <br>
     * These are crates that can be added to or removed from the FIFO queue.
     * (We need the extra structure to manage O(1) removal)<br>
     * Externally, you can only pass these around or view the object held inside
     * <br>
     * The crate uses reference equality.
	 *
	 */
	public final class Crate {
		private Crate previous,next;
		private final T heldObj;
		private boolean removed;
		private Crate(Crate previous, Crate next, T heldObj) {
			this.heldObj = heldObj;
			this.previous = previous;
			this.next = next;
			removed = false;
		}
		
		/**
		 * @return - get the object held within this crate
		 */
		public T getObject() {
			return heldObj;
		}
		
		public String toString() {
			return "Crate:[" + heldObj.toString() + "]";
		}
	}
	
	//A dummy head for the front of the queue
	private final Crate dummyHead;
	private final Crate dummyTail;
	//The number of items held in this queue
	private int size;
	
	/**
	 * Construct a new empty FIFO queue
	 */
	public FIFOQueue() {
		//Initialise stuff...
		dummyHead = new Crate(null,null,null);
		dummyTail = new Crate(null,null,null);
		dummyHead.previous = dummyHead;
		dummyTail.next = dummyTail;
		dummyHead.next = dummyTail;
		dummyTail.previous = dummyHead;
		size = 0;
	}
	
	/**
	 * @param object - the object to add to the back of this FIFO queue
	 * @return - the crate the object has been put in.
	 */
	public Crate enqueue(T object) {
		Crate crate = new Crate(dummyHead,dummyHead.next,object);
		//Correct the head...
		dummyHead.next.previous = crate;
		dummyHead.next = crate;
		size++;
		return crate;
	}
	
	/**
	 * @return - the item at the front of the FIFO queue
	 * @throws EmptyFIFOQueueException - if the queue was empty
	 */
	public Crate dequeue() {
		if (isEmpty()) {
			throw new EmptyFIFOQueueException();
		}
		//Remove the tail...
		Crate crate = dummyTail.previous;
		dummyTail.previous.previous.next = dummyTail;
		dummyTail.previous = dummyTail.previous.previous;
		crate.removed = true;
		size--;
		return crate;
	}
	
	/**
	 * @param crate - the crate to remove from the FIFO queue... (if it was removed in the past, this has no effect
	 */
	public void remove(Crate crate) {
		if (crate.removed) {
			return;
		}
		crate.next.previous = crate.previous;
		crate.previous.next = crate.next;
		crate.removed = true;
		size--;
		//That should be it...
	}
	
	/**
	 * @return - the number of crates in this FIFO queue
	 */
	public int size() {
		return size;
	}
	
	/**
	 * @return - true iff this queue contains no crates
	 */
	public boolean isEmpty() {
		return size==0;
	}

}
