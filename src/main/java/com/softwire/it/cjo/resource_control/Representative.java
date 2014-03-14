package com.softwire.it.cjo.resource_control;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * ****************<br>
 * Date: 14/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class is for the representative of a group of resources. Every resource will have a representative, and
 * each connected component of the graph will have exactly one representative.<br>
 * Representatives are used internally to control how resources are acquired!<br>
 * <br>
 * Implementation note:<br>
 * <br>
 * Representatives tried to make use of a disjoint set forest data structure, but due to the monotonicity required in
 * resource's representatives, and the fact that representatives can't change their orders (without extra caching - deemed to be less efficient)
 * they only use path compression. Luckily, the worst case running time is still O((m+n)log(n)) where n is the number of merges and m the number
 * of finds - so pretty fast. Typically, these values will be small, so I believe the extra caching will be too much of an overhead.<br>
 * <br>
 * See "Algorithms and Data Structures. The Basic Toolbox" page 224
 *
 */
final class Representative implements Comparable<Representative> {
	//The unique id assigned to this representative
	final BigInteger baseId; //needed by resource graph
	//The lock for this representative
	private final Semaphore lock;
	//These are both visible to the resource graph primarily...
	//The parent representative if applicable
	Representative parent;
	//Heuristic tie breakers
	private final int randomComponent;
	//For deciding ties between representatives
	private final List<Integer> tieBreaker;
	//Synchronise on the tiebreaker
	private final Semaphore tieLock;
	
	
	/**
	 * Construct a new representative for some group of resources. The representative assumes it is in
	 * use when it is made.
	 * The representative does not hold the lock upon creation, so you should lock it if you want control.
	 * 
	 * @param baseId - specifies the id for the representative. It is the base, as ties may need to be resolved between representatives.
	 */
	public Representative(BigInteger baseId) {
		this.baseId=baseId;
		//Create the semaphore!
		lock = new Semaphore(1,true);
		//removed = false; //this representative is in use by default
		parent = this;
		tieBreaker = new ArrayList<Integer>();
		tieLock = new Semaphore(1,true);
		randomComponent = new Random().nextInt();
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
		int baseComp = this.baseId.compareTo(rep.baseId);
		if (baseComp!=0) {
			return baseComp;
		} else if (this.randomComponent<rep.randomComponent) {
			return -1;
		} else if (this.randomComponent>rep.randomComponent) {
			return 1;
		} else {
			//Very unlikely we're forced to tie break...
			int index = 0;
			int thisId;
			int otherId;
			//Need to break the tie...
			while (true) {
				thisId = this.getId(index);
				otherId = rep.getId(index);
				if (thisId<otherId) {
					return -1;
				} else if (thisId>otherId) {
					return 1;
				}
				index++;
			}
		}
	}
	
	//Get the next id for a tie breaking situation
	private int getId(int index) {
		int id;
		tieLock.acquireUninterruptibly();
		if (tieBreaker.size()>index) {
			id = tieBreaker.get(index);
		} else {
			//Construct one! (will be equal to size)
			id = new Random().nextInt(); //we construct a new one to avoid the extremely unlikely risk of two synchronised random number generators
			tieBreaker.add(id);
		}
		tieLock.release();
		return id;
	}
}
