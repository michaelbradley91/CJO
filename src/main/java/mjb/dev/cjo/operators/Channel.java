package mjb.dev.cjo.operators;

import mjb.dev.cjo.channels.WaitingReader;
import mjb.dev.cjo.channels.WaitingWriter;
import mjb.dev.cjo.channels.ChannelFIFOQueue.Crate;
import mjb.dev.cjo.parallelresources.Resource;
import mjb.dev.cjo.parallelresources.ResourceGraph;
import mjb.dev.cjo.parallelresources.ResourceManipulator;

/**
 * ****************<br>
 * Date: 17/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This is the most general interface for a channel! A channel has a read end and a write end, and you can cast to the appropriate type.<br>
 * <br>
 * Channels operate according to quite a tricky set of rules, so implementing any variants yourselves will be hard. The outline
 * is as follows:<br>
 * This exists in the operators package to manage Java visibility rules<br>
 * TODO: write this - keeps changing
 * 
 * @param <Message> - the type of messages being sent down this channel
 * 
 */
public abstract class Channel<Message> {
	//My resource
	private final Resource resource; //me!! (uniquely so)
	//The read and write ends
	private final ChannelReader<Message> reader;
	private final ChannelWriter<Message> writer;
	
	/**
	 * Construct a new channel with no readers or writers waiting
	 */
	public Channel() {
		ResourceManipulator manipulator = ResourceGraph.INSTANCE.getManipulator();
		resource = manipulator.addResource();
		manipulator.releaseResources();
		reader = new ChannelReader<Message>(this);
		writer = new ChannelWriter<Message>(this);
	}
	
	/**
	 * Close the write end of the channel (if this was already closed, this does nothing).
	 * Whether or not this has any effect depends on the specific channel.
	 */
	protected abstract void closeWriteEndProtected();
	
	/**
	 * Close the read end of the channel (if this was already closed, this does nothing).
	 * Whether or not this has any effect depends on the specific channel.
	 */
	protected abstract void closeReadEndProtected();
	
	/**
	 * Close the channel. This closes the channel with certainty, meaning no further processes
	 * can interact with it (and any waiting will be kicked or dropped in the case of asynchronous communication)
	 */
	protected abstract void closeProtected();
	
	/**
	 * @return - the read end of the channel (restricted to methods related to reading for separation)
	 */
	public final ChannelReader<Message> getReader() {
		return reader;
	}
	
	/**
	 * @return - the write end of the channel (restricted to methods related to writing for separation)
	 */
	public final ChannelWriter<Message> getWriter() {
		return writer;
	}
	
	/**
	 * Perform an asynchronous check to see if this channel is closed. Note that channels cannot re-open,
	 * so if this returns true, then the channel is certainly and will remain closed. It will be true if you wait
	 * until strictly after a channel closed exception.
	 * @return - true if the channel is closed (false technically means nothing useful)
	 */
	public abstract boolean isClosed();
	
	/*
	 * Any sub classes are expected to make use of the following methods to register and deregister
	 * the interest of readers and writers for this channel. This is necessary for most operators
	 * that rely on the use of these methods - its a bit of a fragile base class issue I think..?
	 * 
	 * If you know a better way to do this, please tell me!!
	 */
	
	/**
	 * Add a reader to the internal list of readers for this channel. (Does not mean they are doing more than reading...
	 * @param reader - the reader who is waiting to read
	 * @return - the item to pass back to the channel if you desire to remove this reader.
	 * @throws RegistrationException - if for any reason the reader could not be registered. Operators should
	 * catch this to allow the program to crash without deadlocking
	 * @throws ChannelClosed - if the channel has already been closed. This should be caught by operators again
	 */
	protected abstract Crate<WaitingReader<Message>> registerReader(WaitingReader<Message> reader);
	
	/**
	 * @param reader - the reader to be removed from the internal list of readers
	 */
	protected abstract void deregisterReader(Crate<WaitingReader<Message>> reader);
	
	/**
	 * Add a writer to the internal list of writers for this channel.
	 * @param writer - the writer who is waiting to write to this channel
	 * @return - the item to pass back to the channel if you desire to remove this writer.
	 * @throws RegistrationException - if for any reason the writer could not be registered. Operators should
	 * catch this to allow the program to crash without deadlocking
	 * @throws ChannelClosed - if the channel has already been closed. This should be caught by operators again
	 */
	protected abstract Crate<WaitingWriter<Message>> registerWriter(WaitingWriter<Message> writer);
	
	/**
	 * @param writer - the writer to be removed from the internal list of writers
	 */
	protected abstract void deregisterWriter(Crate<WaitingWriter<Message>> writer);
	
	/**
	 * You are expected to acquire the resource of the channel before any interactions on it
	 * (through the resource graph). Once you have finished your interactions, you should release
	 * the resources (via your resource manipulator)
	 * @return - the resource that this channel is represented by
	 */
	protected final Resource getResource() {
		return resource;
	}
	
	/**
	 * This method is to be called once you are ready for the channel to update the interactions
	 * between readers or writers (or to respond to its closed status). It is expected that the channel's resource
	 * is still acquired. This is when your waiting readers or writers might receive responses.
	 * @param manipulator - the manipulator holding the lock on this channel
	 */
	protected abstract void update(ResourceManipulator manipulator);
}
