package mjb.dev.cjo.channels;

import mjb.dev.cjo.parallelresources.ResourceManipulator;

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
	 * @param manipulator - the manipulator that holds the lock on the channel calling this method
	 */
	public void channelClosed(ResourceManipulator manipulator);
	
	/**
	 * Specifies that a reader has read the written message
	 * @param manipulator - the manipulator that holds the lock on the channel calling this method
	 */
	public void readerArrived(ResourceManipulator manipulator);
	
	/**
	 * @return - the message written by this writer. This should be a field - nothing complicated
	 * in its implementation (no side effects)
	 */
	public Message getMessage();
}
