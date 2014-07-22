package mjb.dev.cjo.operators;

import mjb.dev.cjo.parallelresources.ResourceGraph;
import mjb.dev.cjo.parallelresources.ResourceManipulator;

/**
 * ****************<br>
 * Date: 18/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class allows you to close parts of channels<br>
 * It is not thread safe, in the sense you can't use the same read object to read from multiple channels at once.
 * 
 * @param <Message> - the types of message being read
 *
 */
public class Close<Message> {
	/**
	 * Create a new close object
	 */
	public Close() {}
	
	/**
	 * Close the read end of a channel. This does nothing if that channel is already closed.
	 * @param channel - the one to close
	 */
	public void closeReadEnd(Channel<Message> channel) {
		ResourceManipulator manipulator = ResourceGraph.INSTANCE.acquireResource(channel.getResource());
		//Close the channel...
		channel.closeReadEndProtected();
		channel.update(manipulator);
		manipulator.releaseResources();
	}
	
	/**
	 * Close the read end of a channel. This does nothing if that channel is already closed.
	 * @param channel - the one to close
	 */
	public void closeReadEnd(ChannelReader<Message> channel) {
		closeReadEnd(channel.getChannel());
	}
	
	/**
	 * Close the write end of a channel. This does nothing if that channel is already closed.
	 * @param channel - the one to close
	 */
	public void closeWriteEnd(Channel<Message> channel) {
		ResourceManipulator manipulator = ResourceGraph.INSTANCE.acquireResource(channel.getResource());
		//Close the channel...
		channel.closeWriteEndProtected();
		channel.update(manipulator);
		manipulator.releaseResources();
	}
	
	/**
	 * Close the write end of a channel. This does nothing if that channel is already closed.
	 * @param channel - the one to close
	 */
	public void closeWriteEnd(ChannelWriter<Message> channel) {
		closeWriteEnd(channel.getChannel());
	}
	
	/**
	 * Close the channel. This does nothing if that channel is already closed.
	 * @param channel - the one to close
	 */
	public void close(Channel<Message> channel) {
		ResourceManipulator manipulator = ResourceGraph.INSTANCE.acquireResource(channel.getResource());
		//Close the channel...
		channel.closeProtected();
		channel.update(manipulator);
		manipulator.releaseResources();
	}
}
