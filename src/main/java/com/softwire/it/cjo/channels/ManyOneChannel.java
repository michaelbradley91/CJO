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
 * A many one channel allows for one reader and many writers at a time. If any more than this are detected,
 * it will throw an exception.<br>
 * Reading and writing on a many one channel is synchronised.<br>
 * Fairness guaranteed!
 * 
 * @param <Message> - the type of message sent down this channel
 */
public class ManyOneChannel<Message> extends AbstractChannel<Message> {
	//Store how much the channel has been closed
	private boolean hasClosed;
	
	/**
	 * Construct a new many one channel
	 */
	public ManyOneChannel() {
		super();
		hasClosed = false;
	}
	
	@Override
	protected Crate<WaitingWriter<Message>> registerWriter(WaitingWriter<Message> writer) {
		if (hasClosed) {
			throw new ChannelClosed(this);
		}
		return super.registerWriter(writer);
	}
	
	/**
	 * For a many one channel, this throws a registration exception if there is already a reader
	 * waiting on the channel
	 */
	@Override
	protected Crate<WaitingReader<Message>> registerReader(WaitingReader<Message> reader) {
		if (hasClosed) {
			throw new ChannelClosed(this);
		}
		if (super.hasReader()) {
			throw new RegistrationException("A many one channel cannot have more than one waiting reader at once");
		}
		return super.registerReader(reader);
	}

	/**
	 * Has no effect on a many one channel
	 */
	@Override
	protected void closeWriteEndProtected() {}

	/**
	 * Completely closes a many one channel
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
		//Now see what we should do with readers or writers...
		super.completeWriterReaderInteractions();
		if (hasClosed) {
			super.clearOutWaitingReadersAndWriters();
		}
	}
}
