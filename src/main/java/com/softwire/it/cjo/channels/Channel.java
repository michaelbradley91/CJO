package com.softwire.it.cjo.channels;

import com.softwire.it.cjo.parallelresources.Resource;
import com.softwire.it.cjo.parallelresources.ResourceGraph;
import com.softwire.it.cjo.utilities.FIFOQueue;

/**
 * ****************<br>
 * Date: 16/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This is the most general interface for a channel! A channel has a read end and a write end, and you can cast to the appropriate type.<br>
 * <br>
 * Channels operate according to quite a tricky set of rules, so implementing any variants yourselves will be hard. The outline
 * is as follows:<br>
 * <br>
 * Each channel is a resource - a vertex of the resource graph. When anyone shows interest in the channel, the resource
 * should be acquired, and when the transaction is completed the resource must be released (this can't easily be managed
 * automatically, so you're expected to be careful yourself - due to the need for clean up after the resource is released,
 * and possibly waiting on something...)<br>
 * When a writer arrives, it will register itself as a "WaitingWriter", which may be constructed when a write method
 * is called, or by an operator itself (this is protected - if you want to build more operators - get the original code!)<br>
 * The channel checks if a reader exists, and if so tells the reader a writer arrived, and then immediately tells the writer
 * a reader arrived. The reader is removed from an internal FIFO queue, the resources are released and presumably the reader
 * stops waiting on something like a semaphore.<br>
 * If the reader is not ready, the writer will just be added to a FIFO queue of writers waiting for listeners.
 * When a reader finally arrives, the writer will be told, and the reader etc...<br>
 * <br>
 * For more complex operators which may wait on multiple channels at once, they're expected to remove
 * themselves from the list of readers and writers appropriately when they leave. They are also expected
 * to ensure all channels on which they were reading and writing (AKA will modify the state of) were locked,
 * by connecting them together via dependencies in the resource graph.<br>
 * If a channel closes for some reason while you're waiting, you'll be told, and you're response will probably
 * be to throw an exception (channel closed exception), but it is up to you.
 * <br>
 * If that all makes sense, you could try building your own!
 * 
 * @param <Message> - the type of messages being sent down this channel
 * 
 */
public abstract class Channel<Message> {
	//The list of writers
	private final FIFOQueue<Channel<Message>> writers;
	//The list of readers
	private final FIFOQueue<Channel<Message>> readers;
	//My resource
	private final Resource resource; //me!! (uniquely so)
	//The read and write ends
	private final ChannelReader<Message> reader;
	private final ChannelWriter<Message> writer;
	
	/**
	 * Construct a new channel with no readers or writers waiting
	 */
	public Channel() {
		writers = new FIFOQueue<Channel<Message>>();
		readers = new FIFOQueue<Channel<Message>>();
		resource = ResourceGraph.INSTANCE.getManipulator().addResource();
		reader = new ChannelReader<Message>(this);
		writer = new ChannelWriter<Message>(this);
	}
	
	/**
	 * @return - a message read from this channel
	 * @throws - TODO (channel closed exception) - don't forget interrupts too, but these are channel specific potentially...
	 */
	public abstract Message read();
	
	/**
	 * @param message - a message o send into the channel
	 * @throws - TODO (channel closed exception) - don't forget interrupts too, but these are channel specific potentially...
	 */
	public abstract void write(Message message);
	
	/**
	 * Close the write end of the channel (if this was already closed, this does nothing).
	 * Whether or not this has any effect depends on the specific channel.
	 */
	public abstract void closeWriteEnd();
	
	/**
	 * Close the read end of the channel (if this was already closed, this does nothing).
	 * Whether or not this has any effect depends on the specific channel.
	 */
	public abstract void closeReadEnd();
	
	/**
	 * Close the channel. This closes the channel with certainty, meaning no further processes
	 * can interact with it (and any waiting will be kicked or dropped in the case of asynchronous communication)
	 */
	public abstract void close();
	
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
}
