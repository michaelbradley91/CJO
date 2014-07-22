package mjb.dev.cjo.channels;

import mjb.dev.cjo.channels.ChannelFIFOQueue.Crate;
import mjb.dev.cjo.channels.exceptions.ChannelClosed;
import mjb.dev.cjo.channels.exceptions.RegistrationException;
import mjb.dev.cjo.parallelresources.ResourceManipulator;
/**
 * ****************<br>
 * Date: 18/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * A one one channel allows for one reader and one writer at a time. If any more than this are detected,
 * it will throw an exception.<br>
 * Reading and writing on a one one channel is synchronised.<br>
 * Fairness guaranteed!
 * 
 * @param <Message> - the type of message sent down this channel
 */
public class OneOneChannel<Message> extends AbstractChannel<Message> {
	//Store how much the channel has been closed
	private boolean hasClosed;
	
	/**
	 * Construct a new one one channel
	 */
	public OneOneChannel() {
		super();
		hasClosed = false;
	}
	
	/**
	 * For a one one channel, this throws a registration exception if there is already a writer
	 * waiting on the channel
	 */
	@Override
	protected Crate<WaitingWriter<Message>> registerWriter(WaitingWriter<Message> writer) {
		if (hasClosed) {
			throw new ChannelClosed(this);
		}
		if (super.hasWriter()) {
			throw new RegistrationException("A one one channel cannot have more than one waiting writer at once");
		}
		return super.registerWriter(writer);
	}
	
	/**
	 * For a one one channel, this throws a registration exception if there is already a reader
	 * waiting on the channel
	 */
	@Override
	protected Crate<WaitingReader<Message>> registerReader(WaitingReader<Message> reader) {
		if (hasClosed) {
			throw new ChannelClosed(this);
		}
		if (super.hasReader()) {
			throw new RegistrationException("A one one channel cannot have more than one waiting reader at once");
		}
		return super.registerReader(reader);
	}

	/**
	 * Completely closes a one one channel
	 */
	@Override
	protected void closeWriteEndProtected() {
		closeProtected();
	}

	/**
	 * Completely closes a one one channel
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
	protected void update(ResourceManipulator manipulator) {
		//Now see what we should do with readers or writers...
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
