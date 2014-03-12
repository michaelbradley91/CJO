package com.softwire.it.cjo.resource_control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


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
		public Collection<Vertex> neighbours; //adjacency list
		//The mark is used when the algorithm is trying to determine the disconnected sets of vertices.
		boolean mark;
		//false means it has not been visited. true means it has been visited.
		/*
		 * The algorithm:
		 * 
		 * Start from any particular vertex. Mark that vertex with true, and cycle through all neighbours.
		 * For those which aren't true, also add them to the set of vertices to visit.
		 * When you mark a vertex as true, add it to a set of vertices.
		 * 
		 * Repeat until the set of vertices to visit becomes empty.
		 * Then go to the next vertex forming a new disconnected component.
		 * 
		 * At the end, reset all of the marks to false
		 */
		public Vertex(Resource resource) {
			this.resource = resource;
			neighbours = new HashSet<Vertex>();
			mark = false;
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
				//Remove the vertex and all of its edges...
				Vertex v = vertices.get(resource);
				for (Vertex neighbour : v.neighbours) {
					//I must not be a neighbour of myself!!
					neighbour.neighbours.remove(v); //throw it away
				}
				vertices.remove(v);
			} //else do nothing
		}
	}
	
	/**
	 * Add a dependency between resources to the graph. Dependencies are undirected. If the dependency already exists, this has no effect.
	 * If one of the resources does not exist, this has no effect either!!
	 * @param resource1 - the first of the resources to add the edge between
	 * @param resource2 - the second ''
	 * 
	 */
	void addDependency(Resource resource1, Resource resource2) {
		synchronized(vertices) {
			if (vertices.keySet().contains(resource1) && vertices.keySet().contains(resource2)) {
				Vertex u = vertices.get(resource1);
				Vertex v = vertices.get(resource1);
				//Check for the existence of the edge
				if (u.neighbours.contains(v)) {
					return; //done already
				}
				u.neighbours.add(v);
				v.neighbours.add(u);
				//Now merge the two representatives...
				Representative repU = u.resource.getRepresentative();
				Representative repV = v.resource.getRepresentative();
				if (!repU.equals(repV)) {
					//We need to merge the two
					if (repU.rank<repV.rank) {
						repU.parent = repV;
					} else if (repV.rank<repU.rank) {
						repV.parent = repU;
					} else {
						repV.parent = repU;
						repU.rank++;
					}
				}
			} //else do nothing
		}
	}
	
	/**
	 * Remove a dependency between resources in the graph. Dependencies are undirected. If the dependency doesn't exist, this does nothing.
	 * If one of the resources does not exist, this does nothing.
	 * @param resource1 - the first of the resources to add the edge between
	 * @param resource2 - the second ''
	 * THIS WILL NOT update representatives by scanning the graph for disconnected components, since it is far more efficient
	 * to perform this at the end only (in one pass)
	 * TODO: check this is still correct - not updating the reps immediately could invalidate the other methods potentially?
	 * Seems pretty unlikely...
	 */
	void removeDependency(Resource resource1, Resource resource2) {
		synchronized(vertices) {
			if (vertices.keySet().contains(resource1) && vertices.keySet().contains(resource2)) {
				Vertex u = vertices.get(resource1);
				Vertex v = vertices.get(resource1);
				//Check for the existence of the edge
				if (!u.neighbours.contains(v)) {
					return; //done already
				}
				//Remove it!
				u.neighbours.remove(v);
				v.neighbours.remove(u);
				//Think that's it...
			}
		}
	}
	
	/**
	 * This should be called only by the resource manipulator if some edges have been removed.
	 * The resources passed in should be all those who may have been on some side of edge removed.
	 * The manipulator should hold the lock on the vertices still, as most of the calculation will be
	 * performed asynchronously!!
	 * @param resources - the resources who need to be checked for connectivity
	 */
	void updateDisconnectedResources(Set<Resource> resources) {
		if (resources.size()==0) {
			return; //nothing to do...
		}
		//In order to avoid holding the lock for too long, we go through the vertices and put them into a separate set...
		List<Vertex> copiedVertices = new ArrayList<Vertex>();
		synchronized(vertices) {
			for (Resource resource : resources) {
				copiedVertices.add(vertices.get(resource));
			}
		}
		//Now we can go through the vertices and determine which are in different sets...
		int maxSize = -1;
		int maxPos = -1;
		int currentPos = -1;
		//To hold each set of disconnected vertices...
		List<List<Vertex>> disconnectedSets = new ArrayList<List<Vertex>>();
		//The vertices to visit...
		Stack<Vertex> toVisit = new Stack<Vertex>();
		List<Vertex> visited;
		Vertex visiting;
		//Lets go!
		for (Vertex vertex : copiedVertices) {
			if (!vertex.mark) {
				visited = new ArrayList<Vertex>();
				currentPos++;
				disconnectedSets.add(visited);
				visited.add(vertex);
				//Not seen yet so we can use this part of the algorithm!
				vertex.mark = true;
				toVisit.push(vertex);
				while (!toVisit.isEmpty()) {
					//Look at all the neighbours
					visiting = toVisit.pop();
					for (Vertex neighbour : visiting.neighbours) {
						if (!neighbour.mark) {
							//new one to check!
							neighbour.mark = true;
							visited.add(neighbour);
							toVisit.push(neighbour);
						}
					}
				}
				//Done for this connected component! Check to see if this one was the largest
				if (visited.size()>maxSize) {
					//It is best to make use of the old representative in the largest group,
					//as this way we avoid updating unnecessarily...
					maxSize = visited.size();
					maxPos = currentPos;
				}
			}
		}
		Representative newRep;
		//Now we have all of the disconnected sets, give them their new representatives...
		for (int i=0; i<disconnectedSets.size(); i++) {
			if (i!=maxPos) {
				//We need to give these a new representative...
				newRep = new Representative();
				newRep.acquireLock(); //lock it down first!
				for (Vertex v : disconnectedSets.get(i)) {
					v.resource.setRepresentative(newRep);
				}
			}
		}
	}
	
	/*
	 * We may have struck quite a lot of luck here!
	 * I just realised that, via the resource graph, it may be possible to avoid restarts!! And implement fairness at the same time.
	 * The resource graph sees all of the attempts to acquire resources, and if we so desire, we can enforce that the resource manipulators
	 * always report to the resource graph when resources are released.
	 * 
	 * Via this mechanism, we don't really need to handle locks at all... that's quite a big shift actually... hmmm...
	 * 
	 * We could try to maintain a list of resources that have been given up, and force people to wait while their resources
	 * are not available... However, working out how to allocate resources still makes allocating representatives quite a nice
	 * way to go about this... Hmm... How could we manage this...
	 * 
	 * So... we can differentiate between representatives added by manipulators before they have released,
	 * and representatives that have truly been released to the graph. This can be handled simply by a bit of code management
	 * and cooperation between the manipulator and the resource graph (the manipulator remembers all representatives
	 * added because of its actions, and upon release reports all of the representatives to be released)
	 * 
	 * In fact, if our code just holds a list of available representatives, we can be sure of what is free (this implicitly requires some
	 * synchronisation at the acquisition point between all possible threads, but since we're going to query the graph, this is kind
	 * of necessary... isn't it? Well... we had tried to avoid this by asynchronously querying nodes, assuming they must still exist)
	 * 
	 * We can still sort of asynchronously query nodes, but there is an issue in that the set of vertices might be manipulated in parallel...
	 * Of course, the resource has the representative itself - not that vertex! Oops...
	 * 
	 * 
	 * The fairness plan:
	 * 
	 * So, it was eventually decided after a long debate that random restart is the way to go, as it avoid global synchronisation
	 * which is horrible...
	 * 
	 * However, there is a way to enforce fairness even in this situation, with hardly any global synchronisation at all. It works as follows:
	 * 
	 * We keep a count of the number of processes that have complained about waiting.
	 * 
	 * A process will complain about waiting if, during the restarts, it is forced to restart a large number of times.
	 * In terms of the time between restarts, this is fair, because the semaphores in all of the representatives are fair,
	 * and so it will progress eventually.
	 * Once the number of restarts has gone beyond a certain level, it will synchronise on a count of held up processes and add one.
	 * 
	 * A process which enters the resource graph for the first time will asynchronously check if this count is >0. If so, it will lock
	 * on it, and check again. If it is still >0, it will begin waiting on a global semaphore (we can use only one via baton passing - this is
	 * a rare event, so I'm not concerned about the time penalty). If the count was not >0, it will just go on as normal.
	 * 
	 * A busy process will, once finished, lock onto the count and decrement it. If it reaches zero, it will then atomically release all of the
	 * processes that were waiting. It will need to wait for them all to release to avoid a particularly nasty and common form of deadlock. This can
	 * be managed by another counter - this isn't such a big deal because processes waiting like this is kind of inherently expensive anyway.
	 * 
	 * This means that, for an ordinary process, it will perform an asynchronous check against an integer in a comparison with zero, and that's it!!
	 * Very fast.
	 * 
	 * This guarantees fairness because, eventually, a process will register itself as busy, and then no processes can enter until it has finished.
	 * Assuming processes don't stay in the manipulator forever (which is a programming failure anyway), eventually the process must get its resources and be able
	 * to proceed!
	 * 
	 * The threshold for when a process becomes busy will be a constant multiple of the number of resources it is trying to acquire I think
	 * (I imagine 10 should activate fairly quickly if there's a genuine problem).
	 * 
	 * Pretty cool!!! Will implement this tomorrow
	 * 
	 */
	
	/**
	 * Acquire a list of resources from the graph. This will ensure all other resources dependent on what you wish to acquire are also
	 * acquired to prevent any concurrency issues!
	 * @param resources - the set of resources to acquire
	 * @return - a resource manipulator that allows you to modify these resources (and any others implicitly acquired
	 * due to dependencies, although it is best not to rely on these as you are unlikely to know the structure of the graph...)
	 */
	public ResourceManipulator acquireResources(Set<Resource> resources) {
		//TODO: implement!
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
		//TODO: implement!
		return null;
	}
	
	/**
	 * @return - get a manipulator which locks on nothing
	 */
	public ResourceManipulator getManipulator() {
		//TODO: implement!
		return null;
	}
}
