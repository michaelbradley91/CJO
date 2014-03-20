package com.softwire.it.cjo.operators;

import java.util.concurrent.Callable;

/**
 * ****************<br>
 * Date: 20/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * The basic alt, which allows you to wait on multiple channels at once.<br>
 * An alt works in the following way:<br>
 * <br>
 * An alt has a list of branches. An ordinary branch has a guard, a channel, and a flag saying
 * if you want to read or write from that channel.<br>
 * Given a list of branches, an alt will wait for some channel to be ready to respond to its operation,
 * and as soon as one is ready, that branch will execute. It promises that only one branch can execute.<br>
 * Before an alt begins waiting it will evaluate its guards as true or false. A branch with a false guard is entirely ignored.<br>
 * <br>
 * There are two special kinds of branch:<br>
 * 1. or else branch - this will execute if no other branches are ready when the alt begins (regardless of whether or not they
 * could become ready)<br>
 * 2. after branch - coupled with an amount of time, this branch will execute if no other branch becomes ready after a specific
 * amount of time.<br>
 * <br>
 * There are some important details regarding guards and write operations on a branch. You pass callable methods
 * into an alt so it can be evaluated on the fly (so that you can use the same alt builder in multiple locations - particularly
 * relevant to server...). When they are evaluated is obviously important in avoiding deadlock, so here's the law:<br>
 * 1. The guards are evaluated first sequentially, in the order of the branches as added to the alt builder,
 * with the after branch always second to last, and the or else branch always last. No channels are locked at this point.<br>
 * 2. The branches with false guards are effectively removed. Then, for all the branches remaining, and branches with write
 * operations have their messages evaluated sequentially, in the order in which the remaining branches were added to the alt builder.
 * No channels are locked at this point either.<br>
 * <br>
 * The alt handles exceptions in the following way:<br>
 * 1. While evaluating guards or messages to write, the first exception encountered is the one thrown.<br>
 * 2. When an alt attempts to wait on a channel, if there are any issues in this (such as registration exceptions on a one one channel 
 * - too many readers or writers) then again the first exception encountered is thrown. However, no order is guaranteed
 * in which the attempts to register are applied.<br>
 * 3. If a channel becomes closed while waiting on an alt or during the attempts to wait (somewhat countering what I said in 2.)
 * then the first channel closed exception encountered will be thrown.<br>
 * <br>
 * That's everything I think! Have fun!<br>
 * <br>
 * This Alt object itself is not thread safe, but the builder is. Don't execute multiple copies of this class at once,
 * but do feel free to execute the rest.
 * 
 * @see AltBuilder
 *
 */
public class Alt implements Runnable {
	//The alt builder for this alt...
	private final AltBuilder alt;
	//The lists of channels and branches that actually matter on any given run
	private Runnable[] branches;
	private Channel<?>[] channels;
	private boolean[] operations;
	private Object[] messages;
	private boolean hasOrElse;
	private boolean hasAfter;
	
	//For our benefit...
	private static final boolean READ = false, WRITE = true;
	
	/**
	 * Construct a new Alt object!
	 * @param altBuilder - the builder for this alt
	 */
	public Alt(AltBuilder altBuilder) {
		this.alt = altBuilder;
	}
	
	//Filters out all of the false guards and throws any exceptions occurring. It
	//fills all of the relevant arrays...
	private void constructRelevantBranches() {
		//Firstly, evaluate all of the guards in order...
		boolean[] guardValues = new boolean[alt.getChannels().size()];
		int index = 0;
		for (Callable<Boolean> guard : alt.getGuards()) {
			if (guard==null) {
				guardValues[index] = true;
			} else {
				try {
					guardValues[index] = guard.call();
				} catch (Exception e) {
					
				}
			}
			index++;
		}
	}
	
	@Override
	public void run() {
		
	}
}
