package mjb.dev.cjo.operators;


/**
 * ****************<br>
 * Date: 15/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This is the read end of a channel, and so only supports operations regarding the read end...<br>
 * If you don't need the write end of a channel, it is best to pass in a reader for cleaner separation<br>
 * This exists in the operators package to manage Java visibility rules<br>
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
	 * @return - the underlying channel being read
	 */
	protected Channel<Message> getChannel() {
		return channel;
	}

}
