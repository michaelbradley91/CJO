package com.softwire.it.cjo.resource_control;

import java.math.BigInteger;
import java.util.concurrent.Semaphore;

import com.softwire.it.cjo.resource_control.exceptions.RepresentativeChildException;

/**
 * ****************
 * Date: 11/03/2014
 * @author michael
 * ****************
 * 
 * This class is for the representative of a group of resources. Every resource will have a representative, and
 * each connected component of the graph will have exactly one representative.
 * Representatives are used internally to control how resources are acquired!
 *
 */
final class Representative implements Comparable<Representative> {
	//TODO: implement some official fairness by storing a queue of listeners here!!
	
	//The Big Integer used as the id of this representative. New representatives are guaranteed to obtain ids larger than before through this...
	private static BigInteger universalCount;
	//Used for concurrency control on the universal count..
	private final static Semaphore universalCountSemaphore = new Semaphore(1, true); //fairness is important!!
	
	//The unique id assigned to this representative
	private final BigInteger id;
	//The lock for this representative
	private final Semaphore lock;
	//Remember if this representative is still the official representative being used in the graph,
	//or if it has been removed
	private boolean removed;
	//The parent representative if applicable
	private Representative parent;
	
	/**
	 * Construct a new representative for some group of resources. The representative assumes it is in
	 * use when it is made.
	 * The representative keeps control of the lock (so sort of in control of the creator). Hence, the lock
	 * should be released after construction when you are done with it!
	 */
	public Representative() {
		//Get a unique id....
		universalCountSemaphore.acquireUninterruptibly();
		id = universalCount;
		//Update the count
		universalCount = universalCount.add(BigInteger.ONE);
		universalCountSemaphore.release();
		//Create the semaphore!
		lock = new Semaphore(1,true);
		removed = false; //this representative is in use by default
		lock.acquireUninterruptibly(); //got it!!
	}
	
	/**
	 * @return - the representative this object really represents - it is very important
	 * that any resources holding representatives call this first as otherwise exceptions will be thrown...
	 */
	public Representative getTrueRepresentative() {
		if (parent==null) {
			return this;
		}
		return parent.getTrueRepresentative();
	}
	
	/**
	 * Set the parent representative for this representative
	 * @param parent - the parent to refer to
	 * @throws RepresentativeChildException - if this representative already had a parent.
	 * (Call getTrueRepresentative for the top level one - the one you actually need...)
	 */
	public void setParentRepresentative(Representative parent) {
		if (parent!=null) {
			throw new RepresentativeChildException();
		}
		this.parent = parent;
	}
	
	/**
	 * Acquire a lock on this representative. This will only terminate when the lock is acquired...
	 */
	public void acquireLock() {
		lock.acquireUninterruptibly(); //got it!!
	}
	
	/**
	 * Release this representative for future use
	 */
	public void releaseLock() {
		lock.release();
	}
	
	/**
	 * Set whether or not this representative has been removed. You should hold the lock
	 * before making this call.
	 * @param removed - true if you want this representative to believe it has been removed. False otherwise
	 */
	public void setRemoved(boolean removed) {
		this.removed = removed;
	}
	
	/**
	 * You should not call this if you have not acquired the lock
	 * @return - true if this representative believes it is not in control of a group in the resource graph
	 */
	public boolean isRemoved() {
		return removed;
	}
	
	@Override
	public int compareTo(Representative rep) {
		//Enable an ordering according to the ids. This is important for non-deadlocking acquisition of resources.
		return this.id.compareTo(rep.id);
	}
	
	//Only possible to be equal by reference equality
	public boolean equals(Object obj) {
		return obj==this;
	}
	
	//Base it on the id
	public int hashCode() {
		return id.hashCode();
	}
	
	//Print the id!
	public String toString() {
		return "Representative id: " + id.toString();
	}
}
