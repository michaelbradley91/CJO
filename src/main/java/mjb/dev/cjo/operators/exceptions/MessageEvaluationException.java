package mjb.dev.cjo.operators.exceptions;

/**
 * ****************<br>
 * Date: 21/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * Thrown when an exception (not a run time exception) occurs within the evaluation of a message inside an alt.
 * This wraps around the exception, and should be caught if relevant.
 *
 */
public class MessageEvaluationException extends RuntimeException {
	private static final long serialVersionUID = 7357628282688587661L;
	private final Exception exception;
	
	/**
	 * Construct a new message evaluation exception
	 * @param exception - the exception that was thrown
	 */
	public MessageEvaluationException(Exception exception) {
		this.exception = exception;
		super.setStackTrace(exception.getStackTrace());
	}

	/**
	 * @return - the exception that was originally thrown
	 */
	public Exception getException() {
		return exception;
	}
}
