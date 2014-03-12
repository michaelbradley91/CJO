package com.softwire.it.cjo.resource_control;

import java.util.Set;

/**
 * ****************
 * Date: 11/03/2014
 * @author michael
 * ****************
 * 
 * This class controls how you manipulate the resource graph. This is the idea:
 * 
 * When you want to manipulate the resource graph in any way, you call "acquireResources"
 * on the graph, and it will pass you a resource manipulator. The resource manipulator is aware
 * that you have all of the resources that you specified you wanted to acquire (as well as any connected
 * to at least one of those resources in the graph potentially)
 * 
 * Through the manipulator, you can then modify the shape of the graph so that it only affects these resources. That includes
 * adding and removing edges or other vertices in the graph.
 * 
 * Once you're done, you release the resources via the manipulator, after which you cannot modify them through this manipulator any more.
 */
public class ResourceManipulator {
	//Remember all of the resources that we have locked
	private final Set<Resource> resources;
	//Remember the resource graph you are a part of...
	private final ResourceGraph graph;
	//Remember if the manipulator has released its locks!!
	private boolean hasReleasedResources;
	
	/**
	 * Construct a new resource manipulator
	 * @param graph - the graph that is being manipulated
	 * @param resources - the resources themselves
	 */
	public ResourceManipulator(ResourceGraph graph,Set<Resource> resources) {
		this.graph = graph;
		this.resources = resources;
	}
	
	/**
	 * Adds a resource to the graph. Currently, the resource is locked by you until the resource manipulator is released
	 * @return - the resource that was just added.
	 * @throws ResourceReleasedException - if this manipulator has released its resources already (because you told it to)
	 */
	public Resource addResource() {
		//TODO: implement!
		return null;
	}
	
	/**
	 * Remove a resource from the graph, and implicitly any edges connected to it. If the resource doesn't exist, this has no effect.
	 * @param resource - the resource to remove
	 * @throws ResourceReleasedException - if this manipulator has released its resources already (because you told it to)
	 * @throws ResourceNotHeldException - if the manipulator does not hold this resource, because it was not initially acquired (or added
	 * through the manipulator)
	 */
	public void removeResource(Resource resource) {
		//TODO: implement!
	}
	
	/**
	 * Add a dependency between resources to the graph. Dependencies are undirected. If the dependency already exists, this has no effect.
	 * @param resource1 - the first of the resources to add the edge between
	 * @param resource2 - the second ''
	 * @throws ResourceReleasedException - if this manipulator has released its resources already (because you told it to)
	 * @throws ResourceNotHeldException - if the manipulator does not hold either of the given resources, because one was not initially acquired (or added
	 * through the manipulator)
	 */
	public void addDependency(Resource resource1, Resource resource2) {
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
	public void removeDependency(Resource resource1, Resource resource2) {
		//TODO: implement!
	}
	
	/**
	 * Release all of the resources held by this manipulator.
	 * Once executed, you cannot use the manipulator again
	 */
	public void releaseResources() {
		//TODO: implement!
	}

}
