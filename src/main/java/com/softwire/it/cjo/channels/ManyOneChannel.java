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
	 * Construct a new one one channel
	 */
	public ManyOneChannel() {
		super();
		hasClosed = false;
	}
	
	@Override
	protected Crate<WaitingWriter<Message>> registerWriter(WaitingWriter<Message> writer) {
		if (hasClosed) {
			throw new ChannelClosed();
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
			throw new ChannelClosed();
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
	protected void closeWriteEnd() {}

	/**
	 * Completely closes a many one channel
	 */
	@Override
	protected void closeReadEnd() {
		close();
	}

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
			//There shouldn't be any further readers - so no more interactions
		}
		if (hasClosed && super.hasReader()) {
			//Get the reader out
			super.getNextReader().channelClosed();
		}
		if (hasClosed && super.hasWriter()) {
			//Get the writers out
			while (super.hasWriter()) {
				super.getNextWriter().channelClosed();
			}
		}
	}
}