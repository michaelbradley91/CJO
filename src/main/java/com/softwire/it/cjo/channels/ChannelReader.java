package com.softwire.it.cjo.channels;

/**
 * ****************<br>
 * Date: 15/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This is the read end of a channel, and so only supports operations regarding the read end...<br>
 * If you don't need the write end of a channel, it is best to pass in a reader for cleaner separation<br>
 * 
 * @param <Message> - the type of messages being sent down this channel
 * 
 */
public class ChannelReader<Message> {
	//This is used to access the underlying channel's methods
	private final Channel<Message> channel;
	
	/**
	 * Construct a channel reader from this channel
	 * @param channel - the channel to be read
	 */
	ChannelReader(Channel<Message> channel) {
		this.channel = channel;
	}
	/**
	 * @return - a message read from this channel
	 * @throws - TODO (channel closed exception) - don't forget interrupts too, but these are channel specific potentially...
	 */
	public Message read() {
		return channel.read();
	}
	
	/**
	 * Close the read end of the channel (if this was already closed, this does nothing).
	 * Whether or not this has any effect depends on the specific channel.
	 */
	public void closeReadEnd() {
		channel.closeReadEnd();
	}
	
	/**
	 * @return - the underlying channel being read
	 */
	protected Channel<Message> getChannel() {
		return channel;
	}

}
