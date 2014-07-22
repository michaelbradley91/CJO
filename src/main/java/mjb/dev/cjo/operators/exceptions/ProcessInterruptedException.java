package mjb.dev.cjo.operators.exceptions;

/**
 * ****************<br>
 * Date: 17/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This exception is thrown whenever a process is interrupted.<br>
 * TODO: move this to a more appropriate location
 *
 */
public class ProcessInterruptedException extends RuntimeException {
	private static final long serialVersionUID = 318015913333249625L;
	//Remember the interrupted exception
	private InterruptedException exception;
	/**
	 * Construct a new process interrupted exception
	 * @param exception - the original interruption
	 */
	public ProcessInterruptedException(InterruptedException exception) {
		this.exception = exception;
		super.setStackTrace(exception.getStackTrace());
	}
	
	/**
	 * @return - the original interrupted exception
	 */
	public InterruptedException getInterruptedException() {
		return exception;
	}
}
