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
 * A one many channel allows for many readers but only one writer at a time. If any more than this are detected,
 * it will throw an exception.<br>
 * Reading and writing on a one many channel is synchronised.<br>
 * Each message written to the channel is only read once.<br>
 * Fairness guaranteed!
 * 
 * @param <Message> - the type of message sent down this channel
 */
public class OneManyChannel<Message> extends AbstractChannel<Message> {
	//Store how much the channel has been closed
	private boolean hasClosed;
	
	/**
	 * Construct a new one many channel
	 */
	public OneManyChannel() {
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
		if (super.hasWriter()) {
			throw new RegistrationException("A one many channel cannot have more than one waiting writer at once");
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
	 * Completely closes a one many channel
	 */
	@Override
	protected void closeWriteEndProtected() {
		closeProtected();
	}

	/**
	 * Has no effect on a one many channel
	 */
	@Override
	protected void closeReadEndProtected() {}

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
	
	@Override
	public boolean isClosed() {
		return hasClosed;
	}
}
