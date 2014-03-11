package com.softwire.it.cjo.resource_control;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * ****************
 * Date: 11/03/2014
 * @author michael
 * ****************
 * 
 * This class is a resource graph, which is used to store resources and the dependencies between them.
 * Generally, you'll only want a single resource graph. Disconnected subgraphs are intended to behave independently
 * concurrently. Connected components must all be synchronised on to prevent concurrency bugs!
 * 
 * In terms of the addition or removal of edges, it is wise to ensure that most of the graph won't disappear, because this
 * can trigger restarts within the algorithm and remove some certainty about what is going on!
 * 
 * (For CJO, channels are largely persistent)
 *
 */
public class ResourceGraph {
	//A vertex in the graph
	private static class Vertex {
		Resource resource;
		public Collection<Resource> neighbours; //adjacency list
		public Vertex(Resource resource) {
			this.resource = resource;
			neighbours = new HashSet<Resource>();
		}
	}
	//Remember all of the vertices in the graph
	private final Map<Resource,Vertex> vertices;
	
	/**
	 * Construct a new initially empty resource graph
	 */
	public ResourceGraph() {
		vertices = new HashMap<Resource,Vertex>();
	}
	
	/**
	 * Adds a resource to the graph. Currently, the resource is locked by you until the resource manipulator that called this is released
	 * @return - the resource that was just added.
	 */
	Resource addResource() {
		//Construct the resource...
		Resource resource = new Resource(new Representative());
		resource.getRepresentative().acquireLock();
		synchronized(vertices) {
			vertices.put(resource,new Vertex(resource));
		}
		return resource;
	}
	
	/**
	 * Remove a resource from the graph, and implicitly any edges connected to it. If the resource doesn't exist, this has no effect.
	 * @param resource - the resource to remove
	 * @throws ResourceReleasedException - if this manipulator has released its resources already (because you told it to)
	 * @throws ResourceNotHeldException - if the manipulator does not hold this resource, because it was not initially acquired (or added
	 * through the manipulator)
	 */
	void removeResource(Resource resource) {
		synchronized(vertices) {
			if (vertices.keySet().contains(resource)) {
				//We have it!
			} //else do nothing
		}
	}
	
	/**
	 * Add a dependency between resources to the graph. Dependencies are undirected. If the dependency already exists, this has no effect.
	 * @param resource1 - the first of the resources to add the edge between
	 * @param resource2 - the second ''
	 * @throws ResourceReleasedException - if this manipulator has released its resources already (because you told it to)
	 * @throws ResourceNotHeldException - if the manipulator does not hold either of the given resources, because one was not initially acquired (or added
	 * through the manipulator)
	 */
	void addDependency(Resource resource1, Resource resource2) {
		//TODO: implement!
	}
	
	/**
	 * Remove a dependency between resources in the graph. Dependencies are undirected. If the dependency doesn't exist, this does nothing.
	 * @param resource1 - the first of the resources to add the edge between
	 * @param resource2 - the second ''
	 * @throws ResourceReleasedException - if this manipulator has released its resources already (because you told it to)
	 * @throws ResourceNotHeldException - if the manipulator does not hold either of the given resources, because one was not initially acquired (or added
	 * through the manipulator)
	 */
	void removeDependency(Resource resource1, Resource resource2) {
		//TODO: implement!
	}
	
	/**
	 * Acquire a list of resources from the graph. This will ensure all other resources dependent on what you wish to acquire are also
	 * acquired to prevent any concurrency issues!
	 * @param resources - the set of resources to acquire
	 * @return - a resource manipulator that allows you to modify these resources (and any others implicitly acquired
	 * due to dependencies, although it is best not to rely on these as you are unlikely to know the structure of the graph...)
	 */
	public ResourceManipulator acquireResources(Set<Resource> resources) {
		return null;
	}
	
	/**
	 * Acquire a single resource from the graph. This will ensure all other resources dependent on what you wish to acquire are also
	 * acquired to prevent any concurrency issues!
	 * @param resource - the resource to acquire
	 * @return - a resource manipulator that allows you to modify this resource (and any others implicitly acquired
	 * due to dependencies, although it is best not to rely on these as you are unlikely to know the structure of the graph...)
	 */
	public ResourceManipulator acquireResource(Resource resource) {
		return null;
	}
	
	/**
	 * @return - get a manipulator which locks on nothing
	 */
	public ResourceManipulator getManipulator() {
		return null;
	}
}
