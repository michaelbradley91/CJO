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
	 * Construct a new one one channel
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
			throw new ChannelClosed();
		}
		if (super.hasWriter()) {
			throw new RegistrationException("A one one channel cannot have more than one waiting writer at once");
		}
		return super.registerWriter(writer);
	}
	
	@Override
	protected Crate<WaitingReader<Message>> registerReader(WaitingReader<Message> reader) {
		if (hasClosed) {
			throw new ChannelClosed();
		}
		return super.registerReader(reader);
	}

	/**
	 * Completely closes a one many channel
	 */
	@Override
	protected void closeWriteEnd() {
		close();
	}

	/**
	 * Has no effect on a one many channel
	 */
	@Override
	protected void closeReadEnd() {}

	@Override
	protected void close() {
		hasClosed = true;
	}

	@Override
	protected void update() {
		//Now see what we should do with readers or writers...
		if (super.hasReader() && super.hasWriter()) {
			//Communicate!
			WaitingReader<Message> reader = super.getNextReader();
			WaitingWriter<Message> writer = super.getNextWriter();
			//Now awake them
			reader.writerArrived(writer.getMessage(), this);
			writer.readerArrived(this);
			//There will be no more writers
		}
		if (hasClosed && super.hasReader()) {
			//Get the readers out
			while (super.hasReader()) {
				super.getNextReader().channelClosed();
			}
		}
		if (hasClosed && super.hasWriter()) {
			//Get the writer out (only one)
			super.getNextWriter().channelClosed();
		}
	}
}