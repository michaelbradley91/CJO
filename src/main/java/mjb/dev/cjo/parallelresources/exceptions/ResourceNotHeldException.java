package mjb.dev.cjo.parallelresources.exceptions;

/**
 * ****************<br>
 * Date: 14/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This exception is thrown when an attempt is made to manipulate a resource in the resource graph
 * that is not held by the resource manipulator.
 *
 */
public class ResourceNotHeldException extends RuntimeException {
	private static final long serialVersionUID = 7854509356128153576L;
}
