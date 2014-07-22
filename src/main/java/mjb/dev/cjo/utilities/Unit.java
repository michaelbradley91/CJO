package mjb.dev.cjo.utilities;

/**
 * Date: 07/10/2013
 * 
 * This class is a simple Unit, which represents "nothing" when passed to functions
 * @author michael
 *
 */
public class Unit {
	/**
	 * The Unit to be used everywhere (to avoid duplicate instances)
	 */
	public static final Unit INSTANCE = new Unit();
	
	//The string representation
	private final static String representation = "()";
	
	public boolean equals(Object obj) {
		if (obj==this) {
			return true;
		}
		if (obj==null) {
			return false;
		}
		return (obj instanceof Unit);
	}
	
	public int hashCode() {
		return 0;
	}
	
	public String toString() {
		return representation;
	}
}
