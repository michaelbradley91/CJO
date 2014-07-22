package mjb.dev.cjo.channels.exceptions;

/**
 * ****************<br>
 * Date: 18/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This exception is thrown when an attempt is made to register with a channel, but it goes wrong
 * for some reason specific to the channel. The exact reason can be identified by the internal message
 * (or subclass if applicable).<br>
 * Anything registering with channels directly should handle this elegantly to avoid concurrency issues.
 * However, the user need not handle this intentionally - they are not meant to be thrown if channels
 * are used correctly.
 * 
 */
public class RegistrationException extends IllegalStateException {
	
	/**
	 * Construct a new registration exception
	 * @param explanation - why this exception was thrown
	 */
	public RegistrationException(String explanation) {
		super(explanation);
	}

	private static final long serialVersionUID = -3303114129102780910L;
}
