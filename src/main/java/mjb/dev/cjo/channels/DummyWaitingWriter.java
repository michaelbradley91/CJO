package mjb.dev.cjo.channels;

import mjb.dev.cjo.parallelresources.ResourceManipulator;

/**
 * ****************<br>
 * Date: 18/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class represents writer who just holds a message, and doesn't really care about anything else...<br>
 * It is used by asynchronous channels.<br>
 * 
 * @param <Message> - the types of messages being written by this writer
 *
 */
class DummyWaitingWriter<Message> implements WaitingWriter<Message> {
	private Message message;
	/**
	 * @param message - the message to return (as an implementor, try to get this at the time a writer would normally
	 * expect the message to be read - not too quickly - in case it matters...)
	 */
	public DummyWaitingWriter(Message message) {
		this.message = message;
	}

	@Override
	public void channelClosed(ResourceManipulator manipulator) {}

	@Override
	public void readerArrived(ResourceManipulator manipulator) {}

	@Override
	public Message getMessage() {
		return message;
	}
}
