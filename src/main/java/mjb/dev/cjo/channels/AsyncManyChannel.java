package mjb.dev.cjo.channels;

import mjb.dev.cjo.channels.ChannelFIFOQueue.Crate;
import mjb.dev.cjo.channels.exceptions.ChannelClosed;
import mjb.dev.cjo.parallelresources.ResourceManipulator;
/**
 * ****************<br>
 * Date: 18/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * An asynchronous many channel allows for any number of readers and many writers at a time.<br>
 * Reading and writing on an asynchronous many channel is asynchronous - that is, writers will never have to wait on the channel.<br>
 * Each message written will only be read by one reader once.<br>
 * Beware of filling a channel, as it will eat all of your memory...<br>
 * Fairness guaranteed!
 * 
 * @param <Message> - the type of message sent down this channel
 */
public class AsyncManyChannel<Message> extends AbstractChannel<Message> {
	//Store how much the channel has been closed
	private boolean hasClosed;
	//Store the writers that need to be told to leave, since it is asynchronous...
	private ChannelFIFOQueue<WaitingWriter<Message>> waitingWriters;
	
	/**
	 * Construct a new asynchronous one channel
	 */
	public AsyncManyChannel() {
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
	
	@Override
	protected Crate<WaitingReader<Message>> registerReader(WaitingReader<Message> reader) {
		if (hasClosed) {
			throw new ChannelClosed(this);
		}
		return super.registerReader(reader);
	}

	/**
	 * Has no effect on an asynchronous many channel
	 */
	@Override
	protected void closeWriteEndProtected() {}

	/**
	 * Has no effect on an asynchronous many channel
	 */
	@Override
	protected void closeReadEndProtected() {}

	@Override
	protected void closeProtected() {
		hasClosed = true;
	}

	@Override
	protected void update(ResourceManipulator manipulator) {
		//Firstly, flush the asynchronous pool...
		while (waitingWriters.size()>0) {
			WaitingWriter<Message> writer = waitingWriters.dequeue();
			super.registerWriter(new DummyWaitingWriter<Message>(writer.getMessage()));
			writer.readerArrived(manipulator); //a bit of a lie...
		}
		//Complete the interactions...
		super.completeWriterReaderInteractions(manipulator);
		if (hasClosed) {
			super.clearOutWaitingReadersAndWriters(manipulator);
		}
	}

	@Override
	public boolean isClosed() {
		return hasClosed;
	}
}
