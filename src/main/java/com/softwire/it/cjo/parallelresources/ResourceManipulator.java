package com.softwire.it.cjo.parallelresources;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.softwire.it.cjo.parallelresources.exceptions.ResourceNotHeldException;
import com.softwire.it.cjo.parallelresources.exceptions.ResourceReleasedException;

/**
 * ****************<br>
 * Date: 14/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class controls how you manipulate the resource graph. This is the idea:<br>
 * <br>
 * When you want to manipulate the resource graph in any way, you call "acquireResources"
 * on the graph, and it will pass you a resource manipulator. The resource manipulator is aware
 * that you have all of the resources that you specified you wanted to acquire (as well as any connected
 * to at least one of those resources in the graph potentially)<br>
 * <br>
 * Through the manipulator, you can then modify the shape of the graph so that it only affects these resources. That includes
 * adding and removing edges or other vertices in the graph.<br>
 * <br>
 * Once you're done, you release the resources via the manipulator, after which you cannot modify them through this manipulator any more.<br>
 * <br>
 * The resource manipulator itself is NOT thread safe - it is designed to be used by one thread (but of course multiple manipulators
 * can be handled in parallel)<br>
 * <br>
 * In total, n operations in the resource manipulator has worst case running time of O(n*log(n)) I believe
 * 
 */
public class ResourceManipulator {
	//Remember all of the representatives that we have locked
	final Collection<Representative> representatives; //visibile agan for speed (wouldn't normally prefer this...)
	//Remember the resource graph you are a part of...
	private final ResourceGraph graph;
	//Remember if the manipulator has released its locks!!
	private boolean hasReleasedResources;
	//Remember if I was marked as "old" when the resources were allocated (this matters to the resource manipulator - not to me - but it is really
	//a LOT faster if I store it myself
	final boolean isOld; //visible for speed
	//Remember all of the resources that may have been affected by the removal of edges (and so may need to be scanned at the very end)
	private final Set<Resource> splitResources;
	//Remember the max rep id for creating new representatives respecting the required ordering.
	BigInteger maxRepId; //visible for speed, and controlled by the resource graph
	
	/**
	 * Construct a new resource manipulator
	 * @param graph - the graph that is being manipulated
	 * @param representatives - the representatives that we have locked
	 */
	public ResourceManipulator(ResourceGraph graph,Collection<Representative> representatives, boolean isOld, BigInteger maxRepId) {
		this.graph = graph;
		this.representatives = representatives;
		this.isOld = isOld;
		this.splitResources = new HashSet<Resource>();
		this.maxRepId = maxRepId;
	}
	
	/**
	 * Adds a resource to the graph. Currently, the resource is locked by you until the resource manipulator is released
	 * @return - the resource that was just added.
	 * @throws ResourceReleasedException - if this manipulator has released its resources already (because you told it to)
	 */
	public Resource addResource() {
		if (hasReleasedResources) {
			throw new ResourceReleasedException();
		}
		return graph.addResource(this);
	}
	
	/**
	 * Remove a resource from the graph, and implicitly any edges connected to it. If the resource doesn't exist, this has no effect.
	 * @param resource - the resource to remove
	 * @throws ResourceReleasedException - if this manipulator has released its resources already (because you told it to)
	 * @throws ResourceNotHeldException - if the manipulator does not hold this resource, because it was not initially acquired (or added
	 * through the manipulator)
	 */
	public void removeResource(Resource resource) {
		if (hasReleasedResources) {
			throw new ResourceReleasedException();
		}
		//Check we control this resource
		if (representatives.contains(resource.getRepresentative())) {
			splitResources.add(resource);
			splitResources.addAll(resource.neighbours); //a lot of people may be affected...
			graph.removeResource(resource);
		} else {
			throw new ResourceNotHeldException();
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
	public void addDependency(Resource resource1, Resource resource2) {
		if (hasReleasedResources) {
			throw new ResourceReleasedException();
		}
		if (representatives.contains(resource1.getRepresentative()) && representatives.contains(resource2.getRepresentative())) {
			graph.addDependency(this,resource1, resource2);
		} else {
			throw new ResourceNotHeldException();
		}
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
		if (hasReleasedResources) {
			throw new ResourceReleasedException();
		}
		if (representatives.contains(resource1.getRepresentative()) && representatives.contains(resource2.getRepresentative())) {
			splitResources.add(resource1);
			splitResources.add(resource2);
			graph.removeDependency(resource1, resource2);
		} else {
			throw new ResourceNotHeldException();
		}
	}
	
	/**
	 * Release all of the resources held by this manipulator.
	 * Once executed, you cannot use the manipulator again
	 */
	public void releaseResources() {
		if (hasReleasedResources) {
			return; //can't do this twice
		}
		//Firstly, update all of the split resources
		graph.updateDisconnectedResources(this, splitResources);
		graph.releaseResources(this);
	}

}
