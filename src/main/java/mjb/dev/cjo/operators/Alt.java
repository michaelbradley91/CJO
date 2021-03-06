package mjb.dev.cjo.operators;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import mjb.dev.cjo.channels.WaitingReader;
import mjb.dev.cjo.channels.WaitingWriter;
import mjb.dev.cjo.channels.ChannelFIFOQueue.Crate;
import mjb.dev.cjo.channels.exceptions.ChannelClosed;
import mjb.dev.cjo.channels.exceptions.RegistrationException;
import mjb.dev.cjo.operators.AltBuilder.BranchProcess;
import mjb.dev.cjo.operators.exceptions.GuardEvaluationException;
import mjb.dev.cjo.operators.exceptions.MessageEvaluationException;
import mjb.dev.cjo.operators.exceptions.NoBranchesException;
import mjb.dev.cjo.operators.exceptions.ProcessInterruptedException;
import mjb.dev.cjo.parallelresources.Resource;
import mjb.dev.cjo.parallelresources.ResourceGraph;
import mjb.dev.cjo.parallelresources.ResourceManipulator;
import mjb.dev.cjo.threads.ThreadScheduler;
import mjb.dev.cjo.threads.ThreadScheduler.Task;
import mjb.dev.cjo.utilities.Box;

import org.apache.log4j.Logger;


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
 * 1. The guards are evaluated first sequentially, in reverse order of the branches as added to the alt builder,
 * with the after branch always second to last, and the or else branch always last. No channels are locked at this point.<br>
 * 2. The branches with false guards are effectively removed. Then, for all the branches remaining, and branches with write
 * operations have their messages evaluated sequentially, in reverse order in which the remaining branches were added to the alt builder.
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
 * Oh! If you have an or else branch and an after branch, the or else branch will activate and the after branch
 * will be ignored. Got it!<br>
 * <br>
 * This Alt object itself is not thread safe, but the builder is. Don't execute multiple copies of this class at once,
 * but do feel free to execute the rest.<br>
 * <br>
 * TODO: make the alt thread safe with a semaphore (handle exceptions carefully!)
 * If you think it is a good idea, it could help greatly with efficiency, as building this object over and over
 * in a while loop is painful...
 * <br>
 * Actually, we reconstruct the arrays each time anyway, so I'm not sure how much it would save you.<br>
 * However, making it truly thread safe is much harder than it might first appear due to exceptions
 * being thrown at unfortunate times. In particular, correct termination
 * of an after branch is a bit scary. (The after branch relies on it being this instance
 * of an alt run - won't like it if it has come back in the middle of another alt run)<br>
 * <br>
 * It might not be a big deal in reality, but I'm not very confident so I'd leave this until later.<br>
 * 
 * @see AltBuilder
 *
 */
public class Alt {
	private final Logger logger = Logger.getLogger(Alt.class);
	//The alt builder for this alt...
	private final AltBuilder alt;
	//The lists of channels and branches that actually matter on any given run
	private Channel<Object>[] channels;
	private boolean[] operations;
	private AltBuilder.BranchProcess<Object>[] processes;
	private Object[] messages;
	private boolean hasOrElse;
	private boolean hasAfter;
	private Set<Resource> resourceSet;
	//The semaphore that this will be release when a response is seen from a channel
	private final Semaphore waitSemaphore;
	//The branch that became active. It is -1 for the after branch
	private int activeBranch;
	//Remember the waiting readers and writers
	private AltWaitingWriter<Object>[] writers;
	private AltWaitingReader<Object>[] readers;
	//The after task if it is running
	private Task afterTask;
	private Box<Boolean> afterTerminateSignal;
	
	//Our resource
	private final Resource resource;
	
	//For our benefit...
	private static final boolean READ = false, WRITE = true;
	private static final int AFTER_BRANCH = -1, NO_BRANCH = -2;
	
	/**
	 * Construct a new Alt object!
	 * @param altBuilder - the builder for this alt
	 */
	protected Alt(AltBuilder altBuilder) {
		this.alt = altBuilder;
		ResourceManipulator manipulator = ResourceGraph.INSTANCE.getManipulator();
		resource = manipulator.addResource();
		manipulator.releaseResources();
		waitSemaphore = new Semaphore(0,true);
	}
	
	/*
	 * Evaluates all guards and messages, and so filters out irrelevant branches.
	 * Fills the operations, channels, and messages arrays, and sets hasOrElse and hasAfter.
	 */
	@SuppressWarnings("unchecked")
	private void constructRelevantBranches(int startIndex, boolean chooseRandom) {
		//Firstly, evaluate all of the guards in order...
		boolean[] guardValues = new boolean[alt.getChannels().size()];
		int index = 0;
		int noBranches = 0;
		for (Callable<Boolean> guard : alt.getGuards()) {
			if (guard==null) {
				guardValues[index] = true;
				noBranches++;
			} else {
				try {
					guardValues[index] = guard.call();
					if (guardValues[index]) {
						noBranches++;
					}
				} catch (Exception e) {
					//Wrap it up and throw it
					throw new GuardEvaluationException(e);
				}
			}
			index++;
		}
		//Now work out the after branch situation
		try {
			hasAfter = alt.hasAfterBranch() && (alt.getAfterGuard()==null ? true : alt.getAfterGuard().call());
		} catch (Exception e) {
			throw new GuardEvaluationException(e);
		}
		//Finally, the or else branch
		try {
			hasOrElse = alt.hasOrElseBranch() && (alt.getOrElseGuard()==null ? true : alt.getOrElseGuard().call());
		} catch (Exception e) {
			throw new GuardEvaluationException(e);
		}
		if (noBranches==0 && !hasAfter && !hasOrElse) {
			//Nothing!!
			throw new NoBranchesException();
		}
		//Evaluate the messages
		messages = new Object[noBranches];
		operations = new boolean[noBranches];
		int activeIndex = 0;
		index = 0;
		for (BranchProcess<?> process : alt.getBranchProcesses()) {
			if (!guardValues[activeIndex]) {
				activeIndex++;
				continue;
			}
			if (process.isWriteProcess()) {
				//We need to evaluate the message...
				try {
					messages[index] = process.getWriteProcess().getMessage();
				} catch (Exception e) {
					throw new MessageEvaluationException(e);
				}
			} else {
				messages[index] = null;
			}
			//Add the operation
			if (process.isReadProcess()) {
				operations[index] = READ;
			} else {
				operations[index] = WRITE;
			}
			index++;
			activeIndex++;
		}
		channels = (Channel<Object>[]) new Channel<?>[noBranches];
		//Fill in the channels
		index = 0;
		activeIndex = 0;
		for (Channel<?> channel : alt.getChannels()) {
			if (guardValues[activeIndex]) {
				channels[index] = (Channel<Object>)channel;
				index++;
			}
			activeIndex++;
		}
		processes = (BranchProcess<Object>[]) new AltBuilder.BranchProcess<?>[noBranches];
		index = 0;
		activeIndex = 0;
		for (BranchProcess<Object> process : alt.getBranchProcesses()) {
			if (guardValues[activeIndex]) {
				processes[index] = process;
				index++;
			}
			activeIndex++;
		}
		//Work out the resource set
		resourceSet = new HashSet<Resource>();
		for (int i=0; i<noBranches; i++) {
			resourceSet.add(channels[i].getResource());
		}
		resourceSet.add(resource);
		//Drain the semaphore
		waitSemaphore.drainPermits();
		//Active branch removed
		activeBranch = NO_BRANCH;
		afterTask = null;
		afterTerminateSignal = new Box<Boolean>(false);
		performFirstPass(startIndex, chooseRandom);
	}
	
	/*
	 * Acquires all of the resources (does not release at the end), and goes through the branches in the specified
	 * order. For now, this order is just from top down, but normally we'd allow this to be randomised or for a specific
	 * position to be set to ensure fairness in serves
	 * 
	 * TODO: enable specific order from start index
	 */
	@SuppressWarnings("unchecked")
	private void performFirstPass(int startIndex, boolean chooseRandom) {
		//We want to acquire the resources first...
		ResourceManipulator manipulator = ResourceGraph.INSTANCE.acquireResources(resourceSet);
		int noBranches = channels.length;
		writers = (AltWaitingWriter<Object>[]) new AltWaitingWriter<?>[noBranches];
		readers = (AltWaitingReader<Object>[]) new AltWaitingReader<?>[noBranches];
		//Now, try registering an interest...
		if (chooseRandom && noBranches>0) {
			startIndex = new Random().nextInt(noBranches);
		}
		for (int i=0; i<noBranches; i++) {
			int index = (i+startIndex) % noBranches;
			//Try registering..
			if (operations[index]==READ) {
				writers[index] = null;
				readers[index] = new AltWaitingReader<Object>(index,channels[index]);
				readers[index].registerSelf(manipulator);
				//See if something happened...
				if (activeBranch!=NO_BRANCH) {
					//Something happened!!
					handleInteraction(manipulator);
					return; //quit
				}
			} else {
				readers[index] = null;
				writers[index] = new AltWaitingWriter<Object>(index,messages[index],channels[index]);
				writers[index].registerSelf(manipulator);
				//See if something happened
				if (activeBranch!=NO_BRANCH) {
					//Something happened!!
					handleInteraction(manipulator);
					return; //quit
				}
			}
		}
		//No one interacted immediately (or suffered any registration errors)
		//See if there is an or else branch to active...
		if (hasOrElse) {
			//Use it!
			deregisterAll();
			manipulator.removeResource(resource);
			manipulator.releaseResources();
			alt.getOrElseBranch().run();
			return; //quit
		}
		//Now see if we have an after branch
		if (hasAfter) {
			//We need to spawn this in a separate thread..
			if (alt.getAfterMilliseconds()==0 && alt.getAfterNanoseconds()==0) {
				//Use it like an or else branch
				deregisterAll();
				manipulator.removeResource(resource);
				manipulator.releaseResources();
				alt.getAfterBranch().run();
				return; //quit
			}
			//Otherwise, we will actually need to wait for this. We need to spawn a separate
			//thread to do the waiting...
			final Alt me = this;
			afterTask = ThreadScheduler.INSTANCE.makeTask(new Runnable(){public void run() {
				me.runAfterTask(me.alt.getAfterMilliseconds(),me.alt.getAfterNanoseconds(), me.afterTerminateSignal);
			}});
			ThreadScheduler.INSTANCE.schedule(afterTask);
			//Good!!
		}
		performWait(manipulator);
	}
	
	/**
	 * Only to be called when no branches have given an immediate response, this begins waiting,
	 * but assumes the resource manipulator is still held.
	 */
	private void performWait(ResourceManipulator manipulator) {
		boolean wasInterrupted = false;
		ProcessInterruptedException exception = null;
		//Release the resources, and wait
		manipulator.releaseResources();
		try {
			waitSemaphore.acquire();
		} catch (InterruptedException e) {
			wasInterrupted = true;
			exception = new ProcessInterruptedException(e);
			Thread.currentThread().interrupt();
		}
		//The resource should be acquired by now... (note that a different thread would have
		//had the resource before, so this won't deadlock with a waiting reader or writer)
		manipulator = ResourceGraph.INSTANCE.acquireResource(resource); //only need the one resource this time - if no interaction
		//took place, we will have all the dependencies. Otherwise, we don't require the other resources
		if (hasAfter && activeBranch!=AFTER_BRANCH) {
			afterTerminateSignal.setItem(true); //kill the after task if it wasn't killed already
			ThreadScheduler.INSTANCE.interrupt(afterTask);
		}
		//Now we can check who responded...
		if (activeBranch==NO_BRANCH) {
			assert(wasInterrupted);
			//No one!! We were just interrupted.
			deregisterAll();
			manipulator.removeResource(resource);
			manipulator.releaseResources();
			if (hasAfter && activeBranch!=AFTER_BRANCH) {
				//Wait for termination
				ThreadScheduler.INSTANCE.deschedule(afterTask);
			}
			throw exception;
		} else if (activeBranch==AFTER_BRANCH) {
			//Run the after branch - already removed from the resource graph
			manipulator.releaseResources();
			if (hasAfter && activeBranch!=AFTER_BRANCH) {
				//Wait for termination
				ThreadScheduler.INSTANCE.deschedule(afterTask);
			}
			alt.getAfterBranch().run();
			return;
		} else {
			//An ordinary branch responded!
			handleInteraction(manipulator);
			if (hasAfter && activeBranch!=AFTER_BRANCH) {
				//Wait for termination
				ThreadScheduler.INSTANCE.deschedule(afterTask);
			}
			return; //done!!
		}
	}
	
	/**
	 * Run by the after task. Will wait for the required amount of time, and then
	 * will try to execute the after branch
	 */
	private void runAfterTask(final long milliseconds, final int nanoseconds, final Box<Boolean> wasTerminated) {
		//Firstly, wait for the required amount of time..
		try {
			Thread.sleep(milliseconds,nanoseconds);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			//We swallow this exception
		}
		//See what happened...
		ResourceManipulator manipulator = ResourceGraph.INSTANCE.acquireResource(resource);
		//Any necessary dependencies will still exist if I was meant to have one
		if (wasTerminated.getItem() || activeBranch!=NO_BRANCH) {
			//Done!
			manipulator.releaseResources();
			return;
		} else {
			//We're still with the original alt that called this, and we need to run this branch
			//We run this in the original thread...
			deregisterAll();
			manipulator.removeResource(resource);
			activeBranch = AFTER_BRANCH;
			manipulator.releaseResources();
			waitSemaphore.release();
			//Done!
		}
	}
	
	/**
	 * This should terminate the alt
	 * @param manipulator - the manipulator holding the lock on the resources
	 */
	private void handleInteraction(ResourceManipulator manipulator) {
		manipulator.releaseResources(); //don't need the lock anymore - should have been handled by the waiters
		//Get the active branch
		if (operations[activeBranch]==READ) { //read operation
			AltWaitingReader<Object> reader = readers[activeBranch];
			if (reader.wasRegisteredBadly) {
				//Could not register. Re-throw the exception
				throw reader.registrationException;
			} else if (reader.wasClosed) {
				//Again, re-throw
				throw reader.closedException;
			} else { //got a writer!
				assert(reader.gotWriter);
				//Take the message, and apply the operation
				processes[activeBranch].getReadProcess().run(reader.messageReceived);
			}
		} else { //write operation
			AltWaitingWriter<Object> writer = writers[activeBranch];
			if (writer.wasRegisteredBadly) {
				//Could not register. Re-throw the exception
				throw writer.registrationException;
			} else if (writer.wasClosed) {
				//Again, re-throw
				throw writer.closedException;
			} else { //got a writer!
				assert(writer.gotReader);
				//Take the message, and apply the operation
				processes[activeBranch].getWriteProcess().run();
			}
		}
	}
	
	/**
	 * ****************<br>
	 * Date: 20/03/2014<br>
	 * Author:  michael<br>
	 * ****************<br>
	 * <br>
	 * A waiting writer for the alt object. This sets the various flags and releases the alt whenever applicable.
	 *
	 * @param <T> - the type of message this is writing
	 */
	private class AltWaitingWriter<T> implements WaitingWriter<T> {
		private final int branchNo;
		private final T message;
		private final Channel<T> channel;
		//Set to true if this becomes closed
		private boolean wasClosed;
		private ChannelClosed closedException;
		//If there was a registration exception
		private boolean wasRegisteredBadly;
		private RegistrationException registrationException;
		//Set to true if a reader arrived
		private boolean gotReader;
		//Remember my crate
		private Crate<WaitingWriter<T>> crate;
		/**
		 * Construct a new waiting writer for the given branch. 
		 * @param branchNo - the branch this is waiting on
		 * @param message - the message for this writer
		 * @param channel - the channel this writer is writing to
		 */
		public AltWaitingWriter(int branchNo, T message, Channel<T> channel) {
			this.branchNo = branchNo;
			wasClosed = false;
			gotReader = false;
			wasRegisteredBadly = false;
			closedException = null;
			registrationException = null;
			this.message = message;
			this.channel = channel;
			crate = null;
		}
		
		@Override
		public void channelClosed(ResourceManipulator manipulator) {
			//We were closed!!
			wasClosed = true;
			closedException = new ChannelClosed(channel);
			activeBranch = branchNo;
			deregisterAll();
			//Now remove the alt from the resource graph
			manipulator.removeResource(resource);
			waitSemaphore.release(); //release him!
		}

		@Override
		public void readerArrived(ResourceManipulator manipulator) {
			//Woo!!
			gotReader = true;
			activeBranch = branchNo;
			deregisterAll();
			//Now remove the alt from the resource graph
			manipulator.removeResource(resource);
			waitSemaphore.release();
		}

		@Override
		public T getMessage() {
			return message;
		}
		
		/**
		 * Register yourself to the channel. This automatically adds the dependency and updates the channel.
		 * It will automatically deregister all interest and remove the resource in the event of an immediate interaction
		 */
		private void registerSelf(ResourceManipulator manipulator) {
			try {
				manipulator.addDependency(channel.getResource(), resource);
				crate = channel.registerWriter(this);
				channel.update(manipulator); //update for immediate interactions
				//Handle the exceptions appropriately...
			} catch (ChannelClosed closed) {
				wasClosed = true;
				closedException = new ChannelClosed(channel);
				activeBranch = branchNo;
				deregisterAll();
				manipulator.removeResource(resource);
				waitSemaphore.release(); //release him!
			} catch (RegistrationException exception) {
				wasRegisteredBadly = true;
				registrationException = exception;
				activeBranch = branchNo;
				deregisterAll();
				manipulator.removeResource(resource);
				waitSemaphore.release(); //release him!
			}
		}
		
		/**
		 * Deregister yourself from the channel if you were registered.
		 * Does not remove the dependency automatically
		 */
		private void deRegisterSelf() {
			if (crate!=null) {
				channel.deregisterWriter(crate);
			}
		}
	}
	
	/**
	 * ****************<br>
	 * Date: 20/03/2014<br>
	 * Author:  michael<br>
	 * ****************<br>
	 * <br>
	 * A waiting reader for the alt object. This sets the various flags and releases the alt whenever applicable.
	 *
	 * @param <T> - the type of message being read
	 */
	private class AltWaitingReader<T> implements WaitingReader<T> {
		private final int branchNo;
		private final Channel<T> channel;
		//Set to true if this becomes closed
		private boolean wasClosed;
		private ChannelClosed closedException;
		//If there was a registration exception
		private boolean wasRegisteredBadly;
		private RegistrationException registrationException;
		//Set to true if a reader arrived
		private boolean gotWriter;
		//Remember my crate
		private Crate<WaitingReader<T>> crate;
		//Remember the message I received if I'm active
		private T messageReceived;
		/**
		 * Construct a new waiting writer for the given branch. 
		 * @param branchNo - the branch this is waiting on
		 * @param channel - the channel this writer is writing to
		 */
		public AltWaitingReader(int branchNo, Channel<T> channel) {
			this.branchNo = branchNo;
			wasClosed = false;
			gotWriter = false;
			wasRegisteredBadly = false;
			this.messageReceived = null;
			registrationException = null;
			closedException = null;
			crate = null;
			this.channel = channel;
		}
		@Override
		public void channelClosed(ResourceManipulator manipulator) {
			//We were closed!!
			wasClosed = true;
			closedException = new ChannelClosed(channel);
			activeBranch = branchNo;
			deregisterAll();
			//Now remove the alt from the resource graph
			manipulator.removeResource(resource);
			waitSemaphore.release(); //release him!
		}
		@Override
		public void writerArrived(T message, ResourceManipulator manipulator) {
			//Woo!!
			gotWriter = true;
			activeBranch = branchNo;
			this.messageReceived = message;
			deregisterAll();
			//Now remove the alt from the resource graph
			manipulator.removeResource(resource);
			waitSemaphore.release();
		}
		

		/**
		 * Register yourself to the channel. This automatically adds the dependency and updates the channel.
		 * It will automatically deregister all interest and remove the resource in the event of an immediate interaction
		 */
		private void registerSelf(ResourceManipulator manipulator) {
			try {
				manipulator.addDependency(channel.getResource(), resource);
				crate = channel.registerReader(this);
				channel.update(manipulator); //update for immediate interactions
				//Handle the exceptions appropriately...
			} catch (ChannelClosed closed) {
				wasClosed = true;
				closedException = new ChannelClosed(channel);
				activeBranch = branchNo;
				deregisterAll();
				manipulator.removeResource(resource);
				waitSemaphore.release(); //release him!
			} catch (RegistrationException exception) {
				wasRegisteredBadly = true;
				registrationException = exception;
				activeBranch = branchNo;
				deregisterAll();
				manipulator.removeResource(resource);
				waitSemaphore.release(); //release him!
			}
		}
		
		/**
		 * Deregister yourself from the channel if you were registered.
		 * Does not remove the dependency automatically
		 */
		private void deRegisterSelf() {
			if (crate!=null) {
				channel.deregisterReader(crate);
			}
		}
	}
	
	/**
	 * Deregister all the existing channels
	 */
	private void deregisterAll() {
		for (int i=0; i<channels.length; i++) {
			//Can remove from this
			if (operations[i]==READ && readers[i]!=null) {
				readers[i].deRegisterSelf();
			} else if (operations[i]==WRITE && writers[i]!=null) {
				writers[i].deRegisterSelf();
			}
		}
	}
	
	/**
	 * Execute this alt! When the alt starts, it will first check if any of its branches are ready immediately.
	 * It will check its branches sequentially in order of the given startBranch index (0 being the first branch)
	 * and will continue in a round robin manner. However, once all branches have been checked for an immediate response,
	 * if none responded it becomes first come first serve
	 * @param startBranch - the branch to check for an interaction with first
	 * @param chooseRandom - if true, overrides start branch and chooses the branch at random from those with true gaurds
	 */
	protected void run(int startBranch, boolean chooseRandom) {
		constructRelevantBranches(startBranch, chooseRandom);
	}
}
