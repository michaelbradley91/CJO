package com.softwire.it.cjo.channels;

import com.softwire.it.cjo.channels.ChannelFIFOQueue.Crate;
import com.softwire.it.cjo.channels.exceptions.ChannelClosed;
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
 * @param <Message> - the type of message sent down this channel
 */
public class ManyManyChannel<Message> extends AbstractChannel<Message> {
	//Store how much the channel has been closed
	private boolean hasClosed;
	
	/**
	 * Construct a new one one channel
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
			throw new ChannelClosed();
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
	 * Has no effect on a many many channel
	 */
	@Override
	protected void closeWriteEnd() {}

	/**
	 * Has no effect on a many many channel
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
		while (super.hasReader() && super.hasWriter()) {
			//Communicate!
			WaitingReader<Message> reader = super.getNextReader();
			WaitingWriter<Message> writer = super.getNextWriter();
			//Now awake them
			reader.writerArrived(writer.getMessage(), this);
			writer.readerArrived(this);
			//There will be no more writers
		}
		if (hasClosed) {
			//Get the readers out
			while (super.hasReader()) {
				super.getNextReader().channelClosed();
			}
			//Get the readers out
			while (super.hasWriter()) {
				super.getNextWriter().channelClosed();
			}
		}
	}
}
