package mjb.dev.cjo.parallelresources;

import java.util.Collection;
import java.util.HashSet;

/**
 * ****************<br>
 * Date: 14/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class is the abstract representation of a resource. Each resource knows the following:<br>
 * <br>
 * The resource graph that it is a part of.<br>
 * Its representative in the resource graph.<br>
 * <br>
 * Equality is reference equality, so each one created is unique.<br>
 *
 */
public final class Resource {
	//Remember the representative for this resource
	private Representative representative;
	//The mark for this resource
	boolean mark; //visible for speed...
	//The collection of vertices I am connected to (resources "are" the graph)
	Collection<Resource> neighbours; //adjacency list - visible for speed
	
	/**
	 * Construct a new resource
	 * @param representative - the representative assigned to this resource
	 */
	Resource(Representative representative) {
		this.representative = representative;
		mark = false; //not marked by default
		neighbours = new HashSet<Resource>();
	}
	
	/**
	 * @return - the representative of this resource (updates itself in the hierarchy if necessary)
	 */
	Representative getRepresentative() {
		representative = representative.getTrueRepresentative();
		return representative;
	}
	
	/**
	 * Set the representative for this resource. This is managed carefully by other objects...
	 * @param representative - the new one!
	 */
	void setRepresentative(Representative representative) {
		this.representative = representative;
	}
}
