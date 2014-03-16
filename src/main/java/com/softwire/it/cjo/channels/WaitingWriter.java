package com.softwire.it.cjo.channels;

/**
 * ****************<br>
 * Date: 15/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * Any user of CJO isn't expected to use this interface (and no public methods accept it). Its public
 * only for the wishful sense of a package hierarchy...<br>
 * This interface is for any writer who is about to wait for a reader to arrive on a channel. The channel
 * can alert the writer of changes through this interface
 * 
 * @param <Message> - the types of messages being written by this writer
 * 
 */ 
public interface WaitingWriter<Message> {
	
	/**
	 * This states that the channel was closed so no interaction on the channel will take place.
	 */
	public void channelClosed();
	
	/**
	 * Specifies that a reader has read the written message
	 * @param reader - the reader itself
	 */
	public void readerArrived(WaitingWriter<Message> reader);
	
	/**
	 * @return - the message written by this writer. This should be a field - nothing complicated
	 * in its implementation (no side effects)
	 */
	public Message getMessage();
}
