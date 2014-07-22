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
 * A many many channel allows any number of readers and writers.<br>
 * Reading and writing on a one many channel is synchronised.<br>
 * Each message written to the channel is only read once.<br>
 * Fairness guaranteed!
 * 
 * TODO: remove code duplication
 * 
 * @param <Message> - the type of message sent down this channel
 */
public class ManyManyChannel<Message> extends AbstractChannel<Message> {
	//Store how much the channel has been closed
	private boolean hasClosed;
	
	/**
	 * Construct a new many many channel
	 */
	public ManyManyChannel() {
		super();
		hasClosed = false;
	}
	
	/**
	 * For a one many channel, this throws a registration exception if there is already a writer
	 * waiting on the channel
	 */
	@Override
	protected Crate<WaitingWriter<Message>> registerWriter(WaitingWriter<Message> writer) {
		if (hasClosed) {
			throw new ChannelClosed(this);
		}
		return super.registerWriter(writer);
	}
	
	@Override
	protected Crate<WaitingReader<Message>> registerReader(WaitingReader<Message> reader) {
		if (hasClosed) {
			throw new ChannelClosed(this);
		}
		return super.registerReader(reader);
	}

	/**
	 * Has no effect on a many many channel
	 */
	@Override
	protected void closeWriteEndProtected() {}

	/**
	 * Has no effect on a many many channel
	 */
	@Override
	protected void closeReadEndProtected() {}

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
