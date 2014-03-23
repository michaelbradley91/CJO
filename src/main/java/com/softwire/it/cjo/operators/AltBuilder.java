package com.softwire.it.cjo.operators;

import java.util.concurrent.Callable;

import com.softwire.it.cjo.utilities.ImmutableList;

/**
 * ****************<br>
 * Date: 20/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class forms part of the builder pattern. Alts have a large number of possible arguments,
 * and putting them all into the same constructor is infeasible (or at least very confusing). Instead,
 * you set all of the various components of the Alt inside a builder, and then get the item from the builder
 * when you're done.<br>
 * <br>
 * An important implementation note: guards for branches on alts are evaluated immediately before
 * any locks are required and the channels waited on. They are evaluated sequentially in reverse order
 * in which they were added. If parallelism is required or efficient, you can arrange
 * to do this computation in the first guard - it is not the concern of the operator.<br>
 * In general, guards should be quick to evaluate. Make sure you cannot trigger deadlock according to the above rule.
 * <br>
 * Given that thread safeness is quite a big deal in an application like this, I've decided to make this 
 * thread safe by ensuring the builder returns immutable objects. That is, the builder itself is immutable,
 * and each method returns another builder with the new values set.<br>
 * <br>
 * <b>Design restrictions:</b><br>
 * <br>
 * Alts in CJO are more restrictive than in CSO in the following ways:<br>
 * <br>
 * 1. A read branch must read its message immediately when it becomes active. In CSO, you can hold the channel
 * you are reading from up, with the branch still activated, just so long as you read from it at some point in the branch.<br>
 * 2. A write branch must provide its message before it can wait for a reader. In CSO, you can hold the channel
 * you are writing to up, with the branch still activated, just so long as you write to it at some point in the branch.<br>
 * 3. The same channel cannot appear in more than one branch of the same alt.<br>
 * <br>
 * The ability not to complete the interaction with a channel on a branch in CJO has been removed for the following reasons:<br>
 * <br>
 * 1. While you are holding onto the channel, you must promise to interact with it eventually on this branch. This is risky,
 * as it may be the case you throw an exception or your computer dies. In code, this can be handled, but if CJO is built upon,
 * knowing what to do could become increasingly complicated.<br>
 * 2. While holding a channel in a branch (before you've interacted), you necessarily are holding onto its lock.
 * If you interact with a different channel before you interact with the held one on the branch, you could trigger deadlock very
 * easily. A lock on a channel locks all channels related to it through "alts", as these are also interested in that channel.
 * Practically, it is difficult for a programmer to know how many channels are really locked when one channel
 * is locked, so telling the programmer to avoid this seemed unrealistic.<br>
 * 3. It's true that we could tell the programmer not to interact with any other channels before replying on this branch,
 * I believe that would be disobeyed by casual users, and since it "might" not cause a problem, could causes
 * errors late in the application's development.<br>
 * <br>
 * So, I don't like the power CSO gives you in this area - I think it is too dangerous. Just to be clear, CSO requires
 * the promises regarding locking onto other channels in the same way as above, and I'm not certain of how it handles exceptions
 * gracefully.<br>
 * It is possible to implement the same power in CJO (just hold onto the resource lock while you calculate the message,
 * or execute the read branch, and pass a function instead), but it just seems too dangerous. I think there are fairly simple
 * workarounds for where this additional power may have been useful.<br>
 * That's the final say in it - our alts are going to behave clearly and safely.<bR>
 * <br>
 * Finally, for (3.) we don't allow the same channel to appear in one alt due to code complications. While the framework would
 * support it, the problem generates considerable overhead...<br>
 * It only makes sense if you would like an alt to both read and write from a branch, but since an alt
 * cannot interact with itself, this ability is a heavy burden on the channels to know which readers and writers
 * can actually interact.<br>
 * <br>
 * TODO: adding branches is quadratic due to the check of existence of a channel in a branch (and the fact that everything
 * is immutable). This isn't ideal, but generally alts will be small, so it probably isn't an issue...?
 * I don't want to remove the check and say that you'll get mysterious side effects if you disobey the rule, as that includes
 * deadlock!
 * 
 */
public class AltBuilder {
	//A null guard is implicitly "true"
	//orelse and after branches can have guards too!
	private final ImmutableList<Callable<Boolean>> guards;
	//A list of processes to apply when a branch is activated... (maintained by the code
	//to ensure they are in fact processes of the right type - a bit ugly)
	private final ImmutableList<BranchProcess<Object>> processes;
	//A list of channels on which the processes are being applied
	private final ImmutableList<Channel<Object>> channels;
	//Whether or not there is an orelse branch
	private final Runnable orElse;
	private final Callable<Boolean> orElseGuard;
	//Whether or not there is an after branch
	private final Runnable after;
	private final Callable<Boolean> afterGuard;
	//The length of time to wait
	private final long milliseconds;
	private final int nanoseconds;
	
	//Accessor methods
	
	/**
	 * @return - the guards for all branches except the after and or else branches. A null value for a guard
	 * indicates one didn't exist. Note that corresponding branches, guards and processes are stored at the same indices within the immutable lists
	 */
	ImmutableList<Callable<Boolean>> getGuards() {
		return guards;
	}
	
	/**
	 * @return - the channels for all branches except the after and or else branches. Note that corresponding branches,
	 * guards and processes are stored at the same indices within the immutable lists
	 */
	ImmutableList<Channel<Object>> getChannels() {
		return channels;
	}
	
	/**
	 * @return - the branch processes for all branches except the after and or else branches. Note that corresponding branches,
	 * guards and processes are stored at the same indices within the immutable lists
	 */
	ImmutableList<BranchProcess<Object>> getBranchProcesses() {
		return processes;
	}
	
	/**
	 * @return - the or else branch
	 * @throws IllegalStateException - if the or else branch does not exist
	 */
	Runnable getOrElseBranch() {
		if (!hasOrElseBranch()) {
			throw new IllegalStateException();
		}
		return orElse;
	}
	
	/**
	 * @return - the guard for the or else branch - null if it doesn't have one
	 * @throws IllegalStateException - if the or else branch does not exist
	 */
	Callable<Boolean> getOrElseGuard() {
		if (!hasOrElseBranch()) {
			throw new IllegalStateException();
		}
		return orElseGuard;
	}
	
	/**
	 * @return - the after branch
	 * @throws IllegalStateException - if the after branch does not exist
	 */
	Runnable getAfterBranch() {
		if (!hasAfterBranch()) {
			throw new IllegalStateException();
		}
		return after;
	}
	
	/**
	 * @return - the number of milliseconds the after branch should wait for
	 * @throws IllegalStateException - if the after branch does not exist
	 */
	long getAfterMilliseconds() {
		if (!hasAfterBranch()) {
			throw new IllegalStateException();
		}
		return milliseconds;
	}
	
	/**
	 * @return - the number of nanoseconds the after branch should wait for
	 * @throws IllegalStateException - if the after branch does not exist
	 */
	int getAfterNanoseconds() {
		if (!hasAfterBranch()) {
			throw new IllegalStateException();
		}
		return nanoseconds;
	}
	
	/**
	 * @return - the guard for the after branch - null if it doesn't have one
	 * @throws IllegalStateException - if the after branch does not exist
	 */
	Callable<Boolean> getAfterGuard() {
		if (!hasAfterBranch()) {
			throw new IllegalStateException();
		}
		return afterGuard;
	}
	
	/**
	 * @return - true iff this alt builder has an or else branch
	 */
	boolean hasOrElseBranch() {
		return orElse!=null;
	}
	
	/**
	 * @return - true iff this alt builder has an after branch
	 */
	boolean hasAfterBranch() {
		return after!=null;
	}
	
	
	//TODO This is a bit inefficient - a lot of empty alt builders may be created - could keep the empty
	//one as a special one... might do this.
	
	/**
	 * Construct a new empty alt builder
	 */
	public AltBuilder() {
		guards = new ImmutableList<Callable<Boolean>>();
		processes = new ImmutableList<BranchProcess<Object>>();
		channels = new ImmutableList<Channel<Object>>();
		orElse = null;
		after = null;
		orElseGuard = null;
		afterGuard = null;
		milliseconds = 0;
		nanoseconds = 0;
	}
	
	/**
	 * Construct a new immutable Alt Builder
	 * @param guards - the guards for the alt builder
	 * @param processes - the processes for the alt builder
	 * @param channels - the channels for the alt builder
	 */
	private AltBuilder(ImmutableList<Callable<Boolean>> guards, ImmutableList<BranchProcess<Object>> processes,
			ImmutableList<Channel<Object>> channels,Runnable orElse,Runnable after,long milliseconds,int nanoseconds,
			Callable<Boolean> orElseGuard,Callable<Boolean> afterGuard) {
		this.guards = guards;
		this.processes = processes;
		this.channels = channels;
		this.after = after;
		this.orElse = orElse;
		this.milliseconds = milliseconds;
		this.nanoseconds = nanoseconds;
		this.orElseGuard = orElseGuard;
		this.afterGuard = afterGuard;
	}
	/**
	 * Add a new read branch to the alt builder
	 * @param channel - the channel to read from; the branch will activate as soon as this channel receives a message
	 * @param guard - if evaluated to false before the branch begins, the branch will be ignored (if true there is no effect).
	 * A null guard is interpreted as "true"
	 * @param process - the read process to execute when the branch activates
	 * @return - an alt builder with this branch added
	 * @throws IllegalArgumentException - if channel or process is null, or if the channel has been added to a branch before
	 */
	@SuppressWarnings("unchecked")
	public <Message> AltBuilder addReadBranch(Channel<Message> channel, Callable<Boolean> guard, ReadProcess<Message> process) {
		if (channel==null) {
			throw new IllegalArgumentException("Cannot add a null channel to a branch");
		}
		if (process==null) {
			throw new IllegalArgumentException("Cannot add a null process to a branch");
		}
		if (channels.contains((Channel<Object>)channel)) {
			throw new IllegalArgumentException("Cannot include the same channel in two different branches");
		}
		return new AltBuilder(guards.add(guard),processes.add((ReadProcess<Object>)process),channels.add((Channel<Object>)channel),
				orElse,after,milliseconds,nanoseconds,orElseGuard,afterGuard);
	}
	/**
	 * Add a new read branch to the alt builder
	 * @param channel - the channel to read from; the branch will activate as soon as this channel receives a message
	 * @param process - the read process to execute when the branch activates
	 * @return - an alt builder with this branch added
	 * @throws IllegalArgumentException - if channel or process is null, or if the channel has been added to a branch before
	 */
	public <Message> AltBuilder addReadBranch(Channel<Message> channel, ReadProcess<Message> process) {
		return addReadBranch(channel,null,process);
	}
	/**
	 * Add a new read branch to the alt builder
	 * @param channel - the channel to read from; the branch will activate as soon as this channel receives a message
	 * @param process - the read process to execute when the branch activates
	 * @return - an alt builder with this branch added
	 * @throws IllegalArgumentException - if channel or process is null, or if the channel has been added to a branch before
	 */
	public <Message> AltBuilder addReadBranch(ChannelReader<Message> channel, ReadProcess<Message> process) {
		return addReadBranch(channel.getChannel(),process);
	}
	/**
	 * Add a new read branch to the alt builder
	 * @param channel - the channel to read from; the branch will activate as soon as this channel receives a message
	 * @param guard - if evaluated to false before the branch begins, the branch will be ignored (if true there is no effect).
	 * A null guard is interpreted as "true"
	 * @param process - the read process to execute when the branch activates
	 * @return - an alt builder with this branch added
	 * @throws IllegalArgumentException - if channel or process is null, or if the channel has been added to a branch before
	 */
	public <Message> AltBuilder addReadBranch(ChannelReader<Message> channel, Callable<Boolean> guard, ReadProcess<Message> process) {
		return addReadBranch(channel.getChannel(),guard,process);
	}
	
	/**
	 * Add a new write branch to the alt builder
	 * @param channel - the channel to read from; the branch will activate as soon as this channel receives a reader for your message
	 * @param guard - if evaluated to false before the branch begins, the branch will be ignored (if true there is no effect).
	 * A null guard is interpreted as "true"
	 * @param process - the write process to execute when the branch activates (the message will be requested before
	 * a wait on the branch begins)
	 * @return - an alt builder with this branch added
	 * @throws IllegalArgumentException - if channel or process is null, or if the channel has been added to a branch before
	 */
	@SuppressWarnings("unchecked")
	public <Message> AltBuilder addWriteBranch(Channel<Message> channel, Callable<Boolean> guard, WriteProcess<Message> process) {
		if (channel==null) {
			throw new IllegalArgumentException("Cannot add a null channel to a branch");
		}
		if (process==null) {
			throw new IllegalArgumentException("Cannot add a null process to a branch");
		}
		if (channels.contains((Channel<Object>)channel)) {
			throw new IllegalArgumentException("Cannot include the same channel in two different branches");
		}
		return new AltBuilder(guards.add(guard),processes.add((WriteProcess<Object>)process),channels.add((Channel<Object>)channel),
				orElse,after,milliseconds,nanoseconds,orElseGuard,afterGuard);
	}
	/**
	 * Add a new write branch to the alt builder
	 * @param channel - the channel to read from; the branch will activate as soon as this channel receives a reader for your message
	 * @param process - the write process to execute when the branch activates (the message will be requested before
	 * a wait on the branch begins)
	 * @return - an alt builder with this branch added
	 * @throws IllegalArgumentException - if channel or process is null, or if the channel has been added to a branch before
	 */
	public <Message> AltBuilder addWriteBranch(Channel<Message> channel, WriteProcess<Message> process) {
		return addWriteBranch(channel,null,process);
	}
	/**
	 * Add a new write branch to the alt builder
	 * @param channel - the channel to read from; the branch will activate as soon as this channel receives a reader for your message
	 * @param process - the write process to execute when the branch activates (the message will be requested before
	 * a wait on the branch begins)
	 * @return - an alt builder with this branch added
	 * @throws IllegalArgumentException - if channel or process is null, or if the channel has been added to a branch before
	 */
	public <Message> AltBuilder addWriteBranch(ChannelWriter<Message> channel, WriteProcess<Message> process) {
		return addWriteBranch(channel.getChannel(),process);
	}
	/**
	 * Add a new write branch to the alt builder
	 * @param channel - the channel to read from; the branch will activate as soon as this channel receives a reader for your message
	 * @param guard - if evaluated to false before the branch begins, the branch will be ignored (if true there is no effect).
	 * A null guard is interpreted as "true"
	 * @param process - the write process to execute when the branch activates (the message will be requested before
	 * a wait on the branch begins)
	 * @return - an alt builder with this branch added
	 * @throws IllegalArgumentException - if channel or process is null, or if the channel has been added to a branch before
	 */
	public <Message> AltBuilder addWriteBranch(ChannelWriter<Message> channel, Callable<Boolean> guard, WriteProcess<Message> process) {
		return addWriteBranch(channel.getChannel(),guard,process);
	}
	
	/**
	 * Add an orelse branch to the alt builder. An orElse branch is activated when, after the initial scan all of the guards
	 * are false or the channels are not ready to interact yet.
	 * @param guard - if evaluated to false, the orelse branch will be ignored. null is interpreted as true
	 * @param orElse - a runnable object to execute if no branches are ready
	 * @return - a new alt builder with the branch added
	 * @throws IllegalStateException - if an orelse branch has already been added to this alt
	 * @throws IllegalArgumentException - if a null orelse branch is passed
	 */
	public AltBuilder addOrElseBranch(Callable<Boolean> guard, Runnable orElse) {
		if (hasOrElseBranch()) {
			throw new IllegalStateException("Cannot add two or else branches to an alt builder");
		} else if (orElse==null) {
			throw new IllegalArgumentException("Cannot use a null or else branch in an alt builder");
		} else {
			return new AltBuilder(guards,processes,channels,
					orElse,after,milliseconds,nanoseconds,guard,afterGuard);
		}
	}
	
	/**
	 * Add an orelse branch to the alt builder. An orElse branch is activated when, after the initial scan all of the guards
	 * are false or the channels are not ready to interact yet.
	 * @param orElse - a runnable object to execute if no branches are ready
	 * @return - a new alt builder with the branch added
	 * @throws IllegalStateException - if an orelse branch has already been added to this alt
	 * @throws IllegalArgumentException - if a null orelse branch is passed
	 */
	public AltBuilder addOrElseBranch(Runnable orElse) {
		return addOrElseBranch(null,orElse);
	}
	
	/**
	 * Add a branch to the alt builder, which will be executed if no other branch is ready within the specified amount
	 * of time.
	 * @param guard - if evaluated to false, the after branch will be ignored. null is interpreted as true
	 * @param milliseconds - the number of milliseconds to wait
	 * @param nanoseconds - the number of nanoseconds to wait, on top of the number of milliseconds
	 * @param after - the process to execute once the time has expired if no other branch is ready
	 * @return - an alt builder with the specified after branch
	 * @throws IllegalArgumentException - if the value of millis is negative or the value of nanos is not in the range 0-999999,
	 * or if the after branch is null
	 * @throws IllegalStateException - if an after branch has been added already.
	 */
	public AltBuilder addAfterBranch(Callable<Boolean> guard, long milliseconds, int nanoseconds, Runnable after) {
		if (hasAfterBranch()) {
			throw new IllegalStateException("Cannot add a second after branch to an alt builder");
		} else if (after==null) {
			throw new IllegalArgumentException("Cannot use a null after branch inside an alt builder");
		} else if (milliseconds<0) {
			throw new IllegalArgumentException("Cannot wait for a negative number of milliseconds");
		} else if (!(0<=nanoseconds && nanoseconds<=999999)) {
			throw new IllegalArgumentException("The number of nanoseconds to wait for must be between 0 and 999999");
		} else {
			return new AltBuilder(guards,processes,channels,
					orElse,after,milliseconds,nanoseconds,orElseGuard,guard);
		}
	}
	
	/**
	 * Add a branch to the alt builder, which will be executed if no other branch is ready within the specified amount
	 * of time.
	 * @param guard - if evaluated to false, the after branch will be ignored. null is interpreted as true
	 * @param milliseconds - the number of milliseconds to wait
	 * @param after - the process to execute once the time has expired if no other branch is ready
	 * @return - an alt builder with the specified after branch
	 * @throws IllegalArgumentException - if the value of millis is negative or if the after branch is null
	 * @throws IllegalStateException - if an after branch has been added already.
	 */
	public AltBuilder addAfterBranch(Callable<Boolean> guard, long milliseconds, Runnable after) {
		return addAfterBranch(guard,milliseconds,after);
	}
	
	/**
	 * Add a branch to the alt builder, which will be executed if no other branch is ready within the specified amount
	 * of time.
	 * @param milliseconds - the number of milliseconds to wait
	 * @param nanoseconds - the number of nanoseconds to wait, on top of the number of milliseconds
	 * @param after - the process to execute once the time has expired if no other branch is ready
	 * @return - an alt builder with the specified after branch
	 * @throws IllegalArgumentException - if the value of millis is negative or the value of nanos is not in the range 0-999999,
	 * or if the after branch is null
	 * @throws IllegalStateException - if an after branch has been added already.
	 */
	public AltBuilder addAfterBranch(long milliseconds, int nanoseconds, Runnable after) {
		return addAfterBranch(null,milliseconds,nanoseconds,after);
	}
	
	/**
	 * Add a branch to the alt builder, which will be executed if no other branch is ready within the specified amount
	 * of time.
	 * @param milliseconds - the number of milliseconds to wait
	 * @param nanoseconds - the number of nanoseconds to wait, on top of the number of milliseconds
	 * @param after - the process to execute once the time has expired if no other branch is ready
	 * @return - an alt builder with the specified after branch
	 * @throws IllegalArgumentException - if the value of millis is negative or if the after branch is null
	 * @throws IllegalStateException - if an after branch has been added already.
	 */
	public AltBuilder addAfterBranch(long milliseconds, Runnable after) {
		return addAfterBranch(null,milliseconds,0,after);
	}
	
	/**
	 * ****************<br>
	 * Date: 20/03/2014<br>
	 * Author:  michael<br>
	 * ****************<br>
	 * <br>
	 * For unifying the two types of branch
	 *
	 * @param <Message> - the type of message involved
	 */
	protected static abstract class BranchProcess<Message> {
		//Get the write process associated
		protected abstract WriteProcess<Message> getWriteProcess();
		//Get the read process associated
		protected abstract ReadProcess<Message> getReadProcess();
		//Check for the above
		protected abstract boolean isWriteProcess();
		protected abstract boolean isReadProcess();
	}
	
	/**
	 * ****************<br>
	 * Date: 20/03/2014<br>
	 * Author:  michael<br>
	 * ****************<br>
	 * <br>
	 * This represents a process waiting to write to a channel within an alt branch.<br>
	 * The message will be requested immediately after all guards have been evaluated, and sequentially
	 * in the order they were added.<br>
	 * No locks will be held when the message is evaluated. Typically - this should be simple.<br>
	 * This is a restriction over the flexibility CSO provides - this is to prevent certain very nasty bugs
	 * arising in complex programs. Please see the class description "Design restrictions" for details.
	 * 
	 * @param <Message> - the type of message being written
	 */
	public static abstract class WriteProcess<Message> extends BranchProcess<Message> {
		/**
		 * Called if the corresponding guard is "true" and immediately before any locks on channels
		 * are acquired.
		 * @return - the message you wish to write to this channel
		 */
		public abstract Message getMessage() throws Exception;
		/**
		 * Execute this branch
		 */
		public abstract void run();
		@Override
		protected final WriteProcess<Message> getWriteProcess() {
			return this;
		}
		@Override
		protected final ReadProcess<Message> getReadProcess() {
			throw new IllegalStateException("Tried to return a read process from a write process");
		}
		@Override
		protected final boolean isWriteProcess() {
			return true;
		}
		@Override
		protected final boolean isReadProcess() {
			return false;
		}
	}
	/**
	 * ****************<br>
	 * Date: 20/03/2014<br>
	 * Author:  michael<br>
	 * ****************<br>
	 * <br>
	 * This represents a process waiting to read from a channel within an alt branch.<br>
	 * The message will be passed directly from the channel which wrote to you (which may have proceeded
	 * in its execution). This a restriction over the flexibility CSO provides - this is to prevent certain very nasty bugs
	 * arising in complex programs. Please see the class description "Design restrictions" for details.
	 * 
	 * @param <Message> - the type of message being read
	 */
	public static abstract class ReadProcess<Message> extends BranchProcess<Message> {
		/**
		 * @param message - execute this branch with the given message read
		 */
		public abstract void run(Message message);
		@Override
		protected final WriteProcess<Message> getWriteProcess() {
			throw new IllegalStateException("Tried to return a write process from a read process");
		}
		@Override
		protected final ReadProcess<Message> getReadProcess() {
			return this;
		}
		@Override
		protected final boolean isReadProcess() {
			return true;
		}
		@Override
		protected final boolean isWriteProcess() {
			return false;
		}
	}
}
