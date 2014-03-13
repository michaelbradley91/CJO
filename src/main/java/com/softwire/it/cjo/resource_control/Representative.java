package com.softwire.it.cjo.resource_control;

import java.math.BigInteger;
import java.util.concurrent.Semaphore;

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
	//TODO: avoid global synchronisation
	/*
	 * This is the last location where global synchronisation takes place.
	 * It can be avoided via randomly assigned ints, and then, during a comparison if equality is detected,
	 * a second newly generated randomised int is constructed to break the tie (and again if necessary - could "theoretically" go horribly wrong
	 * but so so very unlikely)
	 * 
	 * We have a guarantee that comparisons will not be made in parallel?? No... Hm...
	 */
	
	//The Big Integer used as the id of this representative. New representatives are guaranteed to obtain ids larger than before through this...
	private static BigInteger universalCount;
	//Used for concurrency control on the universal count..
	private final static Semaphore universalCountSemaphore = new Semaphore(1, true); //fairness is important!!
	
	//The unique id assigned to this representative
	private final BigInteger id;
	//The lock for this representative
	private final Semaphore lock;
	//These are both visible to the resource graph primarily...
	//The parent representative if applicable
	Representative parent;
	//The rank of the representative, as in a disjoint set forest!
	int rank;
	
	/**
	 * Construct a new representative for some group of resources. The representative assumes it is in
	 * use when it is made.
	 * The representative does not hold the lock upon creation, so you should lock it if you want control.
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
		//removed = false; //this representative is in use by default
		parent = this;
		rank = 0;
	}
	
	/**
	 * @return - the representative this object really represents - it is very important
	 * that any resources holding representatives call this first as otherwise exceptions will be thrown...
	 */
	public Representative getTrueRepresentative() {
		if (parent!=this) {
			parent = parent.getTrueRepresentative(); //update reference - heuristic
		}
		return parent;
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
