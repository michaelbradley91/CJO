package com.softwire.it.cjo.channels;

import com.softwire.it.cjo.channels.ChannelFIFOQueue.Crate;
import com.softwire.it.cjo.parallelresources.Resource;
import com.softwire.it.cjo.parallelresources.ResourceGraph;

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
 * be to throw an exception (channel closed exception), but it is up to you.<br>
 * <br>
 * If that all makes sense, you could try building your own!
 * 
 * @param <Message> - the type of messages being sent down this channel
 * 
 */
public abstract class Channel<Message> {
	//The list of writers
	private final ChannelFIFOQueue<WaitingWriter<Message>> writers;
	//The list of readers
	private final ChannelFIFOQueue<WaitingReader<Message>> readers;
	//My resource
	private final Resource resource; //me!! (uniquely so)
	//The read and write ends
	private final ChannelReader<Message> reader;
	private final ChannelWriter<Message> writer;
	
	/**
	 * Construct a new channel with no readers or writers waiting
	 */
	public Channel() {
		writers = new ChannelFIFOQueue<WaitingWriter<Message>>();
		readers = new ChannelFIFOQueue<WaitingReader<Message>>();
		resource = ResourceGraph.INSTANCE.getManipulator().addResource();
		reader = new ChannelReader<Message>(this);
		writer = new ChannelWriter<Message>(this);
	}
	
	/**
	 * @return - a message read from this channel
	 * @throws ChannelClosed - if the channel was closed before this method was called, or otherwise closed
	 * before a writer passes you a message. It is recommended that you catch this!! It is only a RuntimeException
	 * to avoid code obfuscation.
	 * @throws InterruptedException - if the process was interrupted before it could read a message
	 */
	public abstract Message read();
	
	/**
	 * @param message - a message o send into the channel
	 * @throws ChannelClosed - if the channel was closed before this method was called, or otherwise closed
	 * before a reader receives your message. It is recommended that you catch this!! It is only a RuntimeException
	 * to avoid code obfuscation.
	 * @throws InterruptedException - if the process was interrupted before it could write a message
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
	 */
	protected Crate<WaitingReader<Message>> registerReader(WaitingReader<Message> reader) {
		return readers.enqueue(reader);
	}
	
	/**
	 * @param reader - the reader to be removed from the internal list of readers
	 */
	protected void deregisterReader(Crate<WaitingReader<Message>> reader) {
		readers.remove(reader);
	}
	
	/**
	 * @return - the reader to read the next message from the channel, which has been deregistered automatically
	 */
	protected WaitingReader<Message> getNextReader() {
		return readers.dequeue();
	}
	
	/**
	 * @return - the number of processes waiting to read on this channel (according to these internal methods - assumes
	 * they are actually used!)
	 */
	protected int getNumberOfReaders() {
		return readers.size();
	}
	
	/**
	 * @return - true iff there is at least one waiting reader held by the channel
	 */
	protected boolean hasReader() {
		return readers.size()==0;
	}
	
	/**
	 * Add a writer to the internal list of writers for this channel.
	 * @param writer - the writer who is waiting to write to this channel
	 * @return - the item to pass back to the channel if you desire to remove this writer.
	 */
	protected Crate<WaitingWriter<Message>> registerWriter(WaitingWriter<Message> writer) {
		return writers.enqueue(writer);
	}
	
	/**
	 * @param writer - the writer to be removed from the internal list of writers
	 */
	protected final void deregisterWriter(Crate<WaitingWriter<Message>> writer) {
		writers.remove(writer);
	}
	
	/**
	 * @return - the writer to pass the next message down the channel, which has been deregistered automatically
	 */
	protected final WaitingWriter<Message> getNextWriter() {
		return writers.dequeue();
	}
	
	/**
	 * @return - the number of processes waiting to read on this channel (according to these internal methods - assumes
	 * they are actually used!)
	 */
	protected final int getNumberOfWriters() {
		return writers.size();
	}
	
	/**
	 * @return - true iff there is at least one waiting reader held by the channel
	 */
	protected final boolean hasWriter() {
		return writers.size()==0;
	}
	
	/**
	 * You are expected to acquire the resource of the channel before any interactions on it
	 * (through the resource graph). Once you have finished your interactions, you should release
	 * the resources (via your resource manipulator)
	 * @return - the resource that this channel is represented by
	 */
	protected final Resource getResource() {
		return resource;
	}
}
