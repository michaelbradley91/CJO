package com.softwire.it.cjo.channels;

import com.softwire.it.cjo.utilities.exceptions.EmptyFIFOQueueException;

/**
 * ****************<br>
 * Date: 17/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class is a specialised FIFO queue (first in, first out), which has O(1) enqueue, dequeue, and
 * more importantly  for this application, removal. The object is "truly" removed, so that the garbage collector
 * could take it.<br>
 * <br>
 * The queue is not thread safe and does not support keys.
 * 
 * @param <T> - the type of elements being added to the queue
 *
 */
final class ChannelFIFOQueue<T> {
	/**
	 * ****************<br>
     * Date: 17/03/2014<br>
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
	public static final class Crate<T> {
		private Crate<T> previous,next;
		private final T heldObj;
		private boolean removed;
		//Which queue this crate is a part of
		private final ChannelFIFOQueue<T> owner;
		/*
		 * Note to self:
		 * 
		 * To avoid the big integer, we can be very clever, and detect when the id is too large. We can
		 * then rescan the queue and re-organise the ids. However, we'd need to mak sure anyone who stored
		 * the id knew it could change... Would rather be simple and correct about this.
		 */
		private Crate(Crate<T> previous, Crate<T> next, T heldObj, ChannelFIFOQueue<T> owner) {
			this.heldObj = heldObj;
			this.previous = previous;
			this.next = next;
			removed = false;
			this.owner = owner;
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
	private final Crate<T> dummyHead;
	private final Crate<T> dummyTail;
	//The number of items held in this queue
	private int size;
	
	/**
	 * Construct a new empty FIFO queue
	 */
	public ChannelFIFOQueue() {
		//Initialise stuff... (the head and tail don't need ids as they can't be reached from the outside)
		dummyHead = new Crate<T>(null,null,null,this);
		dummyTail = new Crate<T>(null,null,null,this);
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
	public Crate<T> enqueue(T object) {
		Crate<T> crate = new Crate<T>(dummyHead,dummyHead.next,object,this);
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
	public T dequeue() {
		if (isEmpty()) {
			throw new EmptyFIFOQueueException();
		}
		//Remove the tail...
		Crate<T> crate = dummyTail.previous;
		dummyTail.previous.previous.next = dummyTail;
		dummyTail.previous = dummyTail.previous.previous;
		crate.removed = true;
		size--;
		return crate.getObject();
	}
	
	/**
	 * @param crate - the crate to remove from the FIFO queue... (if it does not exist in the queue, this has no effect)
	 * @return true iff the crate was removed
	 */
	public boolean remove(Crate<T> crate) {
		if (crate.removed || crate.owner!=this) {
			return false;
		}
		crate.next.previous = crate.previous;
		crate.previous.next = crate.next;
		crate.removed = true;
		size--;
		//That should be it...
		return true;
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
