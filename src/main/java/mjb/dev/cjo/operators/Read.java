package mjb.dev.cjo.operators;

import java.util.concurrent.Semaphore;

import mjb.dev.cjo.channels.WaitingReader;
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
 * This class allows you to read from a channel! That's it.<br>
 * It is not thread safe, in the sense you can't use the same read object to read from multiple channels at once.
 * 
 * @param <Message> - the types of message being read
 *
 */
public class Read<Message> implements WaitingReader<Message> {
	//Remember if the channel closed
	private boolean closed;
	//Remember if a message was recieved
	private Message message;
	private boolean gotMessage;
	//Remember if we were interrupted
	private InterruptedException exception;
	//A semaphore to wait on
	private final Semaphore waitSemaphore;
	
	/**
	 * Construct a new object for reading
	 */
	public Read() {
		waitSemaphore = new Semaphore(0);
	}
	
	/**
	 * Read a message from a channel. Note that illegal state exceptions may be thrown if the underlying channel is improperly
	 * used.
	 * @param channel - the channel to read from
	 * @return - a message from the channel
	 * @throws ProcessInterruptedException - if the process is interrupted before it receives a message
	 * @throws ChannelClosed - if the channel you were reading from closed before you received a message
	 */
	public Message read(Channel<Message> channel) {
		//Reset the variables...
		closed = false;
		gotMessage = false;
		waitSemaphore.drainPermits();
		//Acquire the channel...
		Crate<WaitingReader<Message>> myId = null;
		ResourceManipulator manipulator = ResourceGraph.INSTANCE.acquireResource(channel.getResource());
		try {
			myId = channel.registerReader(this);
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
		if (closed || gotMessage) {
			manipulator.releaseResources();
			//We received a response already
			if (closed) {
				throw new ChannelClosed(channel);
			} else {
				return message;
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
			} else if (gotMessage) {
				manipulator.releaseResources();
				return message;
			} else {
				//We were interrupted... I need to remove myself from this channel
				channel.deregisterReader(myId);
				channel.update(manipulator);
				manipulator.releaseResources();
				throw new ProcessInterruptedException(exception);
			}
		}
	}
	
	/**
	 * Read a message from a channel. Note that illegal state exceptions may be thrown if the underlying channel is improperly
	 * used.
	 * @param channel - the channel to read from
	 * @return - a message from the channel
	 * @throws ProcessInterruptedException - if the process is interrupted before it receives a message
	 * @throws ChannelClosed - if the channel you were reading from closed before you received a message
	 */
	public Message read(ChannelReader<Message> channel) {
		return read(channel.getChannel());
	}

	@Override
	public void channelClosed(ResourceManipulator manipulator) {
		closed = true;
		waitSemaphore.release();
	}

	@Override
	public void writerArrived(Message message, ResourceManipulator manipulator) {
		gotMessage = true;
		this.message = message;
		waitSemaphore.release();
	}
}
