package com.softwire.it.cjo.channels;

import com.softwire.it.cjo.channels.ChannelFIFOQueue.Crate;
import com.softwire.it.cjo.channels.exceptions.ChannelClosed;
/**
 * ****************<br>
 * Date: 18/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * A buffer many channel allows for many readers and many writers at a time.<br>
 * Writing to this channel will be asynchronous until the buffer has been filled (which is specified as a number
 * of messages). Then readers must consume some of the messages before writing can continue to be asynchronous.<br>
 * This is a bit safer than a fully asynchronous channel, as it will not allow an infinite number of messages
 * to be stored, unless there are an infinite number of processes.<br>
 * Fairness guaranteed!
 * 
 * @param <Message> - the type of message sent down this channel
 */
public class BufferManyChannel<Message> extends AbstractChannel<Message> {
	/*
	 * Notes to self:
	 * 
	 * We will use the super classes list of writers as the actual buffer.
	 * We will register the writers here first, and add dummy writers
	 * as writers are pushed into the buffer.
	 * 
	 * We have to be a bit careful when updating to cope with readers sucking out writers
	 * as they arrive, but it shouldn't be impossible!
	 */
	//Store how much the channel has been closed
	private boolean hasClosed;
	//Store the writers that need to be told to leave, since it is asynchronous...
	private ChannelFIFOQueue<WaitingWriter<Message>> waitingWriters;
	//The capacity of this buffer
	private final int capacity;
	
	/**
	 * Construct a new buffer many channel
	 * 
	 * @param capacity - the number of messages that can be asynchronously written to this buffer
	 * @throws IllegalArgumentException - if the capacity is negative
	 */
	public BufferManyChannel(int capacity) {
		super();
		if (capacity<0) {
			throw new IllegalArgumentException("Cannot construct a buffered channel with a capacity: " + capacity + " (less than zero)");
		}
		this.capacity = capacity;
		hasClosed = false;
		waitingWriters = new ChannelFIFOQueue<WaitingWriter<Message>>();
	}
	
	@Override
	protected Crate<WaitingWriter<Message>> registerWriter(WaitingWriter<Message> writer) {
		if (hasClosed) {
			throw new ChannelClosed(this);
		}
		//Remember to release this writer.
		return waitingWriters.enqueue(writer);
	}
	
	@Override
	protected void deregisterWriter(Crate<WaitingWriter<Message>> writer) {
		//Remove it from the asynchronous queue too.
		waitingWriters.remove(writer);
		//Impossible for a real writer to have entered this...
	}
	
	@Override
	protected Crate<WaitingReader<Message>> registerReader(WaitingReader<Message> reader) {
		if (hasClosed) {
			throw new ChannelClosed(this);
		}
		return super.registerReader(reader);
	}

	/**
	 * Has no effect on a buffer many channel
	 */
	@Override
	protected void closeWriteEndProtected() {}

	/**
	 * Has no effect on a buffer many channel
	 */
	@Override
	protected void closeReadEndProtected() {}

	@Override
	protected void closeProtected() {
		hasClosed = true;
	}

	@Override
	protected void update() {
		//Firstly, flush out the readers as much as possible...
		super.completeWriterReaderInteractions();
		//Either there are no readers left, or no writers left in the buffer... Perform our interactions with fresh writers
		while (super.hasReader() && !waitingWriters.isEmpty()) {
			//Interact
			WaitingReader<Message> reader = super.getNextReader();
			WaitingWriter<Message> writer = waitingWriters.dequeue();
			//Now awake them
			reader.writerArrived(writer.getMessage(), this);
			writer.readerArrived(this);
		}
		//Now, either the buffer is empty and the fresh writers queue is empty and there are still readers,
		//or there are no more readers...
		//Thus, fill the buffer with writers if there are any
		while (super.getNumberOfWriters()<capacity && !waitingWriters.isEmpty()) {
			WaitingWriter<Message> writer = waitingWriters.dequeue();
			super.registerWriter(new DummyWaitingWriter<Message>(writer.getMessage()));
			writer.readerArrived(this); //a bit of a lie...
		}
		//Nothing may have happened above, but that's OK!
		if (hasClosed) {
			super.clearOutWaitingReadersAndWriters();
			//Clear out our own buffered writers..
			while (!waitingWriters.isEmpty()) {
				waitingWriters.dequeue().channelClosed(); //go!!
			}
		}
	}
	
	@Override
	public boolean isClosed() {
		return hasClosed;
	}
}
