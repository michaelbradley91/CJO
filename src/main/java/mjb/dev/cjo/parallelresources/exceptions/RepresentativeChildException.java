package mjb.dev.cjo.parallelresources.exceptions;

/**
 * ****************<br>
 * Date: 14/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This exception is thrown when an attempt is made to use a representative which is now a child in
 * the hierarchy. You should always use the top level representative.<br>
 * (Call getTrueRepresentative and update your reference first)
 *
 */
public class RepresentativeChildException extends RuntimeException {
	private static final long serialVersionUID = -7232668694512018534L;
}
