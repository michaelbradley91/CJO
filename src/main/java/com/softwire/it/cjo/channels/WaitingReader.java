package com.softwire.it.cjo.channels;

import com.softwire.it.cjo.parallelresources.ResourceManipulator;

/**
 * ****************<br>
 * Date: 15/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * Any user of CJO isn't expected to use this interface (and no public methods accept it). Its public
 * only for the wishful sense of a package hierarchy...<br>
 * This interface is for any reader who is about to wait for a writer to arrive on a channel. The channel
 * can alert the reader of changes through this interface.
 * <br><br>
 * All of the methods in this interface will only be called while a channel has its resources locked,
 * and so they shouldn't take long nor risk deadlocks (so avoid interactions with other channels)
 * 
 * @param <Message> - the types of messages being written by this writer
 * 
 */ 
public interface WaitingReader<Message> {
	/**
	 * This states that the channel was closed so no interaction on the channel will take place.
	 * @param manipulator - the resource manipulator being used to control this channel
	 */
	public void channelClosed(ResourceManipulator manipulator);
	
	/**
	 * Specifies that a writer has written to the this reader with the given message.
	 * @param manipulator - the resource manipulator being used to control this channel
	 * @param message - the message from the writer itself
	 * @param channel - the channel this writer came on
	 */
	public void writerArrived(ResourceManipulator manipulator, Message message, Channel<Message> channel);
}
