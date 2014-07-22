package mjb.dev.cjo.operators.exceptions;

/**
 * ****************<br>
 * Date: 20/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * Thrown when an all the branches in an alt have false guards, and so it really has no branches at all.
 * Note that this is also thrown when there are no branches at all...
 *
 */
public class NoBranchesException extends RuntimeException {
	private static final long serialVersionUID = 7357628282688587661L;
	
	/**
	 * Construct a new all guards false exception
	 */
	public NoBranchesException() {}
}
