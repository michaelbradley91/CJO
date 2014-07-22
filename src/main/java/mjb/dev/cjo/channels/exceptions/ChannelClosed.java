package mjb.dev.cjo.channels.exceptions;

import mjb.dev.cjo.operators.Channel;

/**
 * ****************<br>
 * Date: 17/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This exception is thrown when a channel becomes closed. This is not to be considered as a disaster,
 * as channels may be closed to enforce the termination of an algorithm (as is used in many CSO examples).
 *
 */
public class ChannelClosed extends RuntimeException {
	private static final long serialVersionUID = -7232668694512018534L;
	private final Channel<?> channel;
	/**
	 * Construct a new channel closed exception
	 * @param channel - the channel that became closed
	 */
	public ChannelClosed(Channel<?> channel) {
		super();
		this.channel = channel;
	}
	
	/**
	 * @return - the channel which was closed
	 */
	public Channel<?> getChannel() {
		return channel;
	}
}
