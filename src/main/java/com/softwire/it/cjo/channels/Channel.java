package com.softwire.it.cjo.channels;

/**
 * ****************<br>
 * Date: 15/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This is the most general interface for a channel! A channel has a read end and a write end, and you can cast to the appropriate type.
 * 
 * @param <Message> - the type of messages being sent down this channel
 * 
 */
public interface Channel<Message> extends ChannelReader<Message>,ChannelWriter<Message> {
	
	/**
	 * Close the channel. This closes the channel with certainty, meaning no further processes
	 * can interact with it (and any waiting will be kicked or dropped in the case of asynchronous communication)
	 */
	public void close();
}
