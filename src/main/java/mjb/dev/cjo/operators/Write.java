package mjb.dev.cjo.operators;

import java.util.concurrent.Semaphore;

import mjb.dev.cjo.channels.WaitingWriter;
import mjb.dev.cjo.channels.ChannelFIFOQueue.Crate;
import mjb.dev.cjo.channels.exceptions.ChannelClosed;
import mjb.dev.cjo.channels.exceptions.RegistrationException;
import mjb.dev.cjo.operators.exceptions.ProcessInterruptedException;
import mjb.dev.cjo.parallelresources.ResourceGraph;
import mjb.dev.cjo.parallelresources.ResourceManipulator;


/**
 * ****************<br>
 * Date: 18/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class allows you to write to a channel! That's it.<br>
 * It is not thread safe, in the sense that you should not use the same object to write to multiple channels at the same time.
 * 
 * @param <Message> - the types of message being written
 *
 */
public class Write<Message> implements WaitingWriter<Message> {
	//Remember if the channel closed
	private boolean closed;
	//Remember if a reader arrived
	private boolean wasRead;
	//Remember if we were interrupted
	private InterruptedException exception;
	//The message to write
	private Message message;
	//A semaphore to wait on
	private final Semaphore waitSemaphore;
	
	/**
	 * Construct a new object for reading
	 */
	public Write() {
		waitSemaphore = new Semaphore(0);
	}
	
	/**
	 * Write a message into a channel. Note that illegal state exceptions may be thrown if the underlying channel is improperly
	 * used.
	 * @param channel - the channel to write into
	 * @param message - the message to write
	 * @return - a message from the channel
	 * @throws ProcessInterruptedException - if the process is interrupted before it can send a message
	 * @throws ChannelClosed - if the channel you were writing to closed before you sent a message
	 */
	public void write(Channel<Message> channel, Message message) {
		//Reset the variables...
		closed = false;
		wasRead = false;
		waitSemaphore.drainPermits();
		this.message = message;
		//Acquire the channel...
		Crate<WaitingWriter<Message>> myId = null;
		ResourceManipulator manipulator = ResourceGraph.INSTANCE.acquireResource(channel.getResource());
		try {
			myId = channel.registerWriter(this);
			//Handle errors
		} catch (RegistrationException exception) {
			manipulator.releaseResources();
			throw exception;
		} catch (ChannelClosed exception) {
			manipulator.releaseResources();
			throw exception;
		}
		//Now update the channel, and check for a response..
		channel.update(manipulator);
		if (closed || wasRead) {
			manipulator.releaseResources();
			//We received a response already
			if (closed) {
				throw new ChannelClosed(channel);
			} else {
				return; //success!
			}
		} else {
			//We need to wait for a writer properly...
			manipulator.releaseResources();
			try {
				waitSemaphore.acquire();
			} catch (InterruptedException e) {
				exception = e;
				Thread.currentThread().interrupt(); //keep the interrupt going
			}
			manipulator = ResourceGraph.INSTANCE.acquireResource(channel.getResource());
			//Check what happened
			if (closed) {
				manipulator.releaseResources();
				throw new ChannelClosed(channel);
			} else if (wasRead) {
				manipulator.releaseResources();
				return; //success!
			} else {
				//We were interrupted... I need to remove myself from this channel
				channel.deregisterWriter(myId);
				channel.update(manipulator);
				manipulator.releaseResources();
				throw new ProcessInterruptedException(exception);
			}
		}
	}
	
	/**
	 * Write a message into a channel. Note that illegal state exceptions may be thrown if the underlying channel is improperly
	 * used.
	 * @param channel - the channel to write into
	 * @param message - the message to write
	 * @return - a message from the channel
	 * @throws ProcessInterruptedException - if the process is interrupted before it can send a message
	 * @throws ChannelClosed - if the channel you were writing to closed before you sent a message
	 */
	public void write(ChannelWriter<Message> channel, Message message) {
		write(channel.getChannel(),message);
	}

	@Override
	public void channelClosed(ResourceManipulator manipulator) {
		closed = true;
		waitSemaphore.release();
	}

	@Override
	public void readerArrived(ResourceManipulator manipulator) {
		wasRead = true;
		waitSemaphore.release();
	}

	@Override
	public Message getMessage() {
		return message;
	}
}
