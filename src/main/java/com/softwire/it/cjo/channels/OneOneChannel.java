package com.softwire.it.cjo.channels;

import java.util.concurrent.Semaphore;

import com.softwire.it.cjo.channels.ChannelFIFOQueue.Crate;
import com.softwire.it.cjo.channels.exceptions.ChannelClosed;
import com.softwire.it.cjo.channels.exceptions.ProcessInterruptedException;
import com.softwire.it.cjo.parallelresources.ResourceGraph;
import com.softwire.it.cjo.parallelresources.ResourceManipulator;
import com.softwire.it.cjo.utilities.Box;

/**
 * ****************<br>
 * Date: 17/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * A one one channel allows for one reader and one writer at a time. If any more than this are detected,
 * it will throw an exception.<br>
 * Reading and writing on a one one channel is synchronised.
 * 
 * @param <Message> - the type of message sent down this channel
 */
public class OneOneChannel<Message> extends Channel<Message> {
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
	 * @throws IllegalStateException - if the channel already has a waiting reader
	 */
	@Override
	public Message read() {
		//Lock the channel...
		ResourceManipulator manipulator = ResourceGraph.INSTANCE.acquireResource(super.getResource());
		//Now see if we're closed if there is already a reader...
		boolean wasClosed = hasClosed;
		boolean hadReader = super.hasReader();
		if (!wasClosed && !hadReader) {
			//We can interact properly!
			if (super.hasWriter()) {
				//Woo! We have a writer already - grab its message...
				WaitingWriter<Message> writer = super.getNextWriter();
				//Get the message
				Message message = writer.getMessage();
				//Now tell the writer a reader arrived...
				writer.readerArrived(manipulator, this);
				//Now leave!
				manipulator.releaseResources();
				return message;
			} else {
				//This is the semaphore we will wait on...
				final Semaphore waitSemaphore = new Semaphore(0); //not fair - we're the only process with it
				final Box<Message> postBox = new Box<Message>(null);
				final Box<Boolean> gotMessage = new Box<Boolean>(false);
				final Box<Boolean> closed = new Box<Boolean>(false);
				//Now we need to wait for a message, which is more complicated...
				Crate<WaitingReader<Message>> reader = super.registerReader(new WaitingReader<Message>() {
					@Override
					public void channelClosed(ResourceManipulator manipulator) {
						closed.setItem(true); //did close
						//Now release the semaphore
						waitSemaphore.release();
					}
					@Override
					public void writerArrived(ResourceManipulator manipulator,
							Message message, Channel<Message> channel) {
						//Got the message...
						postBox.setItem(message); //presumably it is this channel...
						gotMessage.setItem(true);
						waitSemaphore.release();
					}
				});
				//Remember if we were interrupted
				boolean wasInterrupted = false;
				InterruptedException exception = null;
				//Now release the resource, and begin waiting...
				manipulator.releaseResources();
				try {
					waitSemaphore.acquire();
				} catch (InterruptedException e) {
					exception = e;
					wasInterrupted = true;
					Thread.currentThread().interrupt(); //keep it going
				}
				manipulator = ResourceGraph.INSTANCE.acquireResource(super.getResource());
				//See what happened
				if (closed.getItem()) {
					//Throw this exception
					manipulator.releaseResources();
					throw new ChannelClosed();
				} else if (gotMessage.getItem()) {
					//Woo! Return this... (we will have been deregistered already)
					manipulator.releaseResources();
					return postBox.getItem();
				} else {
					//We were interrupted, so re-throw it
					super.deregisterReader(reader);
					throw new ProcessInterruptedException(exception);
				}
			}
		} else {
			manipulator.releaseResources();
			//We need to throw an exception...
			if (wasClosed) {
				throw new ChannelClosed();
			} else {
				throw new IllegalStateException("A second reader arrived on the one one channel");
			}
		}
	}

	/**
	 * @throws IllegalStateException - if the channel already has a waiting writer
	 */
	@Override
	public void write(Message message) {
		
	}

	/**
	 * For a one one channel, this has the effect of completely closing the channel
	 */
	@Override
	public void closeWriteEnd() {
		close();
	}

	/**
	 * For a one one channel, this has the effect of completely closing the channel
	 */
	@Override
	public void closeReadEnd() {
		close();
	}
	
	@Override
	public void close() {
		if (hasClosed) {
			return; //already closed - quick
		}
		//Lock the channel...
		ResourceManipulator manipulator = ResourceGraph.INSTANCE.acquireResource(super.getResource());
		if (hasClosed) {
			manipulator.releaseResources(); //done already...
			return;
		}
		//Now tell anything waiting to leave... (I can only have one of either)
		if (super.hasReader()) {
			//Tell the reader to leave
			super.getNextReader().channelClosed(manipulator);
		}
		if (super.hasWriter()) {
			super.getNextWriter().channelClosed(manipulator);
		}
		hasClosed = true;
		//Remove myself from the graph...
		manipulator.removeResource(getResource());
		manipulator.releaseResources(); //done!
	}

}
