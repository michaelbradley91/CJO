package com.softwire.it.cjo.resource_control;

import java.util.Collection;
import java.util.HashSet;

/**
 * ****************
 * Date: 11/03/2014
 * @author michael
 * ****************
 * 
 * This class is the abstract representation of a resource. Each resource knows the following:
 * 
 * The resource graph that it is a part of.
 * Its representative in the resource graph.
 * 
 * Equality is reference equality, so each one created is unique.
 *
 */
public class Resource {
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
	
	public boolean equals(Object obj) {
		return this==obj; //reference equality!
	}
}
