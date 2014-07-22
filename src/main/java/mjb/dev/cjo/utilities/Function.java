package mjb.dev.cjo.utilities;

/**
 * Date: 07/10/2013
 * 
 * This class defines the type of a function, which can be passed around
 * and be applied to specific types and be expected to return a specific type
 * 
 * @author michael
 *
 * @param <Input> - the type of input given to this function
 * @param <Return> - the type of output from the  function
 * <br><br>
 * Note: these are not necessarily true Mathematical functions - they may have side effects.
 */
public interface Function<Input, Return> {
	/**
	 * Evaluate the function with the given argument
	 * @param input - the input argument
	 * @return - the result of calling the function on the given input
	 */
	public Return eval(Input input);
}
