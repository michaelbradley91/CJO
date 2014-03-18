package com.softwire.it.cjo.channels;

import com.softwire.it.cjo.channels.ChannelFIFOQueue.Crate;
import com.softwire.it.cjo.operators.Channel;

/**
 * ****************<br>
 * Date: 17/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This abstract class provides many default methods for handing readers and writers in a channel.
 * It was built to remove code duplication.
 *
 * @param <Message> - the type of message sent down this channel
 */
public abstract class AbstractChannel<Message> extends Channel<Message> {
	//The list of writers
	private final ChannelFIFOQueue<WaitingWriter<Message>> writers;
	//The list of readers
	private final ChannelFIFOQueue<WaitingReader<Message>> readers;
	
	/**
	 * Construct a new channel with no readers or writers waiting
	 * The abstract channel provides basic support for registering and deregistering readers
	 * or writers using FIFO queues (O(1) enqueue, removal and dequeue)
	 */
	public AbstractChannel() {
		writers = new ChannelFIFOQueue<WaitingWriter<Message>>();
		readers = new ChannelFIFOQueue<WaitingReader<Message>>();
	}
	
	@Override
	protected Crate<WaitingReader<Message>> registerReader(WaitingReader<Message> reader) {
		return readers.enqueue(reader);
	}
	
	@Override
	protected void deregisterReader(Crate<WaitingReader<Message>> reader) {
		readers.remove(reader);
	}
	
	/**
	 * @return - the reader to read the next message from the channel, which has been deregistered automatically
	 */
	protected WaitingReader<Message> getNextReader() {
		return readers.dequeue();
	}
	
	/**
	 * @return - the number of processes waiting to read on this channel
	 */
	protected int getNumberOfReaders() {
		return readers.size();
	}
	
	/**
	 * @return - true iff there is at least one waiting reader held by the channel
	 */
	protected boolean hasReader() {
		return readers.size()==0;
	}
	
	@Override
	protected Crate<WaitingWriter<Message>> registerWriter(WaitingWriter<Message> writer) {
		return writers.enqueue(writer);
	}
	
	@Override
	protected void deregisterWriter(Crate<WaitingWriter<Message>> writer) {
		writers.remove(writer);
	}
	
	/**
	 * @return - the writer to pass the next message down the channel, which has been deregistered automatically
	 */
	protected WaitingWriter<Message> getNextWriter() {
		return writers.dequeue();
	}
	
	/**
	 * @return - the number of processes waiting to read on this channel (according to these internal methods - assumes
	 * they are actually used!)
	 */
	protected int getNumberOfWriters() {
		return writers.size();
	}
	
	/**
	 * @return - true iff there is at least one waiting reader held by the channel
	 */
	protected boolean hasWriter() {
		return writers.size()==0;
	}
	
	/**
	 * Tell all waiting readers and writers that they are to leave because the channel
	 * has closed. This empties the readers and writers queue
	 */
	protected void clearOutWaitingReadersAndWriters() {
		while (hasReader()) {
			getNextReader().channelClosed();
		}
		while (hasWriter()) {
			getNextWriter().channelClosed();
		}
	}
	
	/**
	 * Automatically pairs of readers and writers from the queues until one list becomes empty
	 */
	protected void completeWriterReaderInteractions() {
		while (hasReader() && hasWriter()) {
			//Communicate!
			WaitingReader<Message> reader = getNextReader();
			WaitingWriter<Message> writer = getNextWriter();
			//Now awake them
			reader.writerArrived(writer.getMessage(), this);
			writer.readerArrived(this);
		}
	}
}
