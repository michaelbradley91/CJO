package com.softwire.it.cjo.channels;

import com.softwire.it.cjo.channels.ChannelFIFOQueue.Crate;
import com.softwire.it.cjo.channels.exceptions.ChannelClosed;
import com.softwire.it.cjo.channels.exceptions.RegistrationException;
/**
 * ****************<br>
 * Date: 18/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * An asynchronous one channel allows for one reader but many writers at a time. If any more than this are detected,
 * it will throw an exception.<br>
 * Reading and writing on an asynchronous one channel is asynchronous - that is, writers will never have to wait on the channel.<br>
 * Beware of filling a channel, as it will eat all of your memory...<br>
 * Fairness guaranteed!
 * 
 * @param <Message> - the type of message sent down this channel
 */
public class AsyncOneChannel<Message> extends AbstractChannel<Message> {
	//Store how much the channel has been closed
	private boolean hasClosed;
	//Store the writers that need to be told to leave, since it is asynchronous...
	private ChannelFIFOQueue<WaitingWriter<Message>> waitingWriters;
	
	/**
	 * Construct a new asynchronous one channel
	 */
	public AsyncOneChannel() {
		super();
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
		//It is impossible for a real writer to have entered the super classes queue.
	}
	
	/**
	 * For an asynchronous one channel, this throws a registration exception if there is already a reader
	 * waiting on the channel
	 */
	@Override
	protected Crate<WaitingReader<Message>> registerReader(WaitingReader<Message> reader) {
		if (hasClosed) {
			throw new ChannelClosed(this);
		}
		if (super.hasReader()) {
			throw new RegistrationException("An asynchronous one channel cannot have more than one waiting reader at once");
		}
		return super.registerReader(reader);
	}

	/**
	 * Has no effect on an asynchronous one channel
	 */
	@Override
	protected void closeWriteEndProtected() {}

	/**
	 * Completely closes an asynchronous one channel
	 */
	@Override
	protected void closeReadEndProtected() {
		closeProtected();
	}

	@Override
	protected void closeProtected() {
		hasClosed = true;
	}

	@Override
	protected void update() {
		//Firstly, flush the asynchronous pool...
		while (waitingWriters.size()>0) {
			WaitingWriter<Message> writer = waitingWriters.dequeue();
			super.registerWriter(new DummyWaitingWriter<Message>(writer.getMessage()));
			writer.readerArrived(this); //a bit of a lie...
		}
		//Complete the interactions...
		super.completeWriterReaderInteractions();
		if (hasClosed) {
			super.clearOutWaitingReadersAndWriters();
		}
	}
}
