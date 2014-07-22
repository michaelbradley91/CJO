package mjb.dev.cjo.parallelresources;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Semaphore;

/**
 * ****************<br>
 * Date: 14/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class is a resource graph, which is used to store resources and the dependencies between them.<br>
 * Generally, you'll only want a single resource graph. Disconnected subgraphs are intended to behave independently
 * concurrently. Connected components must all be synchronised on to prevent concurrency bugs!<br>
 * <br>
 * In terms of the addition or removal of edges, it is wise to ensure that most of the graph won't disappear, because this
 * can trigger restarts within the algorithm and remove some certainty about what is going on!<br>
 * <br>
 * (For CJO, channels are largely persistent)
 *
 */
public final class ResourceGraph {
	
	/**
	 * The resource graph instance. There should only ever be one in use for an application
	 * (as you should never mix resources between graphs, and if they are cleanly separated, there won't be any overhead
	 * in the computation anyway)
	 */
	public static final ResourceGraph INSTANCE = new ResourceGraph();
	
	/**
	 * Construct a new initially empty resource graph
	 */
	private ResourceGraph() {}
	
	/**
	 * Adds a resource to the graph. Currently, the resource is locked by you until the resource manipulator that called this is released
	 * @param manipulator - the resource manipulator requesting this operation - this is important because the new resource will have
	 * a new representative, and it is more convenient for the graph to update the manipulator's list.
	 * @return - the resource that was just added
	 */
	Resource addResource(ResourceManipulator manipulator) {
		//Construct the resource...
		BigInteger newId = manipulator.maxRepId.add(BigInteger.ONE); //the new id must be strictly larger than any previous resource
		manipulator.maxRepId = newId;
		Representative newRep = new Representative(newId); //make sure it is bigger.
		newRep.acquireLock();
		Resource resource = new Resource(newRep);
		manipulator.representatives.add(newRep); //update the representatives
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
		//Remove all of its edges...
		for (Resource neighbour : resource.neighbours) {
			//I must not be a neighbour of myself!!
			neighbour.neighbours.remove(resource); //throw it away
		}
		resource.neighbours.clear(); //remove all of the edges...
	}
	
	/**
	 * Add a dependency between resources to the graph. Dependencies are undirected. If the dependency already exists, this has no effect.
	 * If one of the resources does not exist, this has no effect either!!
	 * 
	 * @param manipulator - important for assigning a new id to the new representative
	 * @param resource1 - the first of the resources to add the edge between
	 * @param resource2 - the second ''
	 * 
	 */
	void addDependency(ResourceManipulator manipulator, Resource resource1, Resource resource2) {
		if (resource1.equals(resource2)) {
			return; //don' add self loops...
		}
		//Check for the existence of the edge
		if (resource1.neighbours.contains(resource2)) {
			return; //done already
		}
		resource1.neighbours.add(resource2);
		resource2.neighbours.add(resource1);
		//Now merge the two representatives...
		Representative rep1 = resource1.getRepresentative();
		Representative rep2 = resource2.getRepresentative();
		//Note:
		if (!rep1.equals(rep2)) {
			//We need to merge the two
			//Add the larget rep on top (so that the rep's for resources only increase)
			if (rep1.compareTo(rep2)>0) {
				//rep1 bigger
				rep2.parent = rep1;
			} else {
				//rep2 bigger
				rep1.parent = rep2;
			}
		}
	}
	
	/**
	 * Remove a dependency between resources in the graph. Dependencies are undirected. If the dependency doesn't exist, this does nothing.
	 * If one of the resources does not exist, this does nothing.
	 * @param resource1 - the first of the resources to add the edge between
	 * @param resource2 - the second ''
	 * THIS WILL NOT update representatives by scanning the graph for disconnected components, since it is far more efficient
	 * to perform this at the end only (in one pass)
	 */
	void removeDependency(Resource resource1, Resource resource2) {
		//Check for the existence of the edge
		if (!resource1.neighbours.contains(resource2)) {
			return; //done already
		}
		//Remove it!
		resource1.neighbours.remove(resource2);
		resource2.neighbours.remove(resource1);
		//Think that's it...
	}
	
	/**
	 * This should be called only by the resource manipulator if some edges have been removed.
	 * The resources passed in should be all those who may have been on some side of edge removed.
	 * The manipulator should hold the lock on the vertices still, as most of the calculation will be
	 * performed asynchronously!!
	 * @param manipulator - the resource manipulator requesting this - it will have its representatives updated
	 * @param resources - the resources who need to be checked for connectivity
	 */
	void updateDisconnectedResources(ResourceManipulator manipulator, Set<Resource> resources) {
		if (resources.size()==0) {
			return; //nothing to do...
		}
		//Now we can go through the vertices and determine which are in different sets...
		int maxSize = -1;
		int maxPos = -1;
		int currentPos = -1;
		//To hold each set of disconnected vertices...
		List<List<Resource>> disconnectedSets = new ArrayList<List<Resource>>();
		//The vertices to visit...
		Stack<Resource> toVisit = new Stack<Resource>();
		List<Resource> visited;
		Resource visiting;
		//Lets go!
		for (Resource resource : resources) {
			if (!resource.mark) {
				visited = new ArrayList<Resource>();
				currentPos++;
				disconnectedSets.add(visited);
				visited.add(resource);
				//Not seen yet so we can use this part of the algorithm!
				resource.mark = true;
				toVisit.push(resource);
				while (!toVisit.isEmpty()) {
					//Look at all the neighbours
					visiting = toVisit.pop();
					for (Resource neighbour : visiting.neighbours) {
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
		/*
		 * No attempt is made to throw away old representatives that are no longer in use for three reasons:
		 * 
		 * 1. It is actually not so clear that everything will keep working!! The resources passed by the manipulator
		 * are only those involved in removal operations - not all of them.
		 * 2. They are going to be released once, and if they are useless will then play as much a role after this anyway,
		 * so the remove operation may actually be more expensive!!
		 * 3. Other manipulators may be waiting on the old reps, so it is important to unlock them. Granted,
		 * some time might be saved if they were unlocked slightly earlier (so they noticed that they needed to restart), but
		 * this method is the last one called by a manipulator before release anyway.
		 */
		//Now we have all of the disconnected sets, give them their new representatives...
		BigInteger newId;
		for (int i=0; i<disconnectedSets.size(); i++) {
			if (i!=maxPos) {
				newId = manipulator.maxRepId.add(BigInteger.ONE); //the new id must be strictly larger than any previous resource
				manipulator.maxRepId = newId;
				//We need to give these a new representative...
				newRep = new Representative(newId);
				newRep.acquireLock(); //lock it down first!
				for (Resource resource : disconnectedSets.get(i)) {
					resource.setRepresentative(newRep);
				}
				manipulator.representatives.add(newRep); //store the new one!
			}
			for (Resource resource : disconnectedSets.get(i)) {
				resource.mark = false; //reset the marks
			}
		}
	}
	
	/*
	 * Note:
	 * 
	 * Global synchronisation is currently still a problem - we're globally synchronising on the vertices, but this really shouldn't be necessary!!
	 * 
	 * The issue is that the graph is being stored in a single set (hashmap...) of Vertices. When elements need to be added or removed then it is important
	 * to synchronise on this set, as otherwise we might get a concurrency error...
	 * 
	 * However, the whole point of a lot of this work is to avoid global synchronisation. In particular, when resources are being manipulated,
	 * we'd like not to have to synchronise on them because the manipulator has already locked those vertices (and all edges between
	 * such vertices) - hence, it shouldn't have to lock again...
	 * 
	 * So, one particularly troubling issue is - how do we add vertices atomically??
	 * 
	 * The hashmap for starters is complicating things - I'd like the resources themselves to be the vertices.
	 * The resources do hold the representatives themselves, and effectively maintain the graph state all by themselves. So, I don't think
	 * the resource graph needs to hold the graph at all!! (In theory...)
	 * 
	 * This wipes out the issue of non-existent resources - as if they can be written in they obviously exist (although
	 * we'd like to make sure they're a part of the same resource graph, so they can be made to remember that I guess)
	 * 
	 * This way, any requests regarding resources automatically provides all of the necessary graph stuff...! Seems like the solution.
	 * (Very weird not actually having the graph... nothing can go wrong right?)
	 * 
	 * Note that removing a resource is now a funny operation - it can have its edges removed (and better had for the sake of the graph),
	 * but otherwise still exists - we could set a flag inside it to throw an error if it is used, but maybe it is better to just let the user throw
	 * away the reference...?
	 * 
	 */
	
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
	
	//Fairness bits and bobs:
	
	//Remember how many old processes there are:
	private int noOldProcesses = 0;
	//Synchronise on the above
	private final Semaphore oldProcessesSemaphore = new Semaphore(1,true);
	//Wait if there are busy processes on this semaphore:
	private final Semaphore waitSemaphore = new Semaphore(1,true);
	//The official threshold for when a resource is declared old
	private final static int OLD_THRESHOLD = 10;
	
	
	/**
	 * Acquire a list of resources from the graph. This will ensure all other resources dependent on what you wish to acquire are also
	 * acquired to prevent any concurrency issues! This will destroy the set passed to it.
	 * @param resources - the set of resources to acquire
	 * @return - a resource manipulator that allows you to modify these resources (and any others implicitly acquired
	 * due to dependencies, although it is best not to rely on these as you are unlikely to know the structure of the graph...)
	 */
	public ResourceManipulator acquireResources(Set<Resource> resources) {
		int noRestarts = 0;
		int threshold = OLD_THRESHOLD*(resources.size()); //If I go above this in the number of restarts, then i will be old!
		boolean isOld = false;
		if (noOldProcesses>0) {
			//Wait!
			waitSemaphore.acquireUninterruptibly();
			waitSemaphore.release();
		}
		//Doing stuff...
		/*
		 * The plan:
		 * 
		 * Asynchronously scan through the representatives of the resources, and add them to a list.
		 * Sort them!
		 * Then, from the smallest first, try to acquire the lock.
		 * Once acquired, get the list of representatives again and sort.
		 * If the representative is not equal to the one in the first position, restart! (Holding onto previous locks)
		 * Otherwise, remove all of the resources who belonged to that representative from the set we began with...
		 * 
		 */
		Representative rep, minRep, tempMinRep;
		Set<Representative> uniqueReps = new HashSet<Representative>();
		Map<Representative,List<Resource>> resourceMap = new HashMap<Representative,List<Resource>>();
		for (Resource resource : resources) {
			rep = resource.getRepresentative();
			uniqueReps.add(rep);
		}
		minRep = uniqueReps.isEmpty() ? null : uniqueReps.iterator().next();
		for (Representative representative : uniqueReps) {
			if (representative.compareTo(minRep)<0) {
				minRep = representative;
			}
		}
		//Remember the representatives we've successfully got...
		Set<Representative> acquired = new HashSet<Representative>(); //containment will be frequently checked by the manipulator
		while (!resources.isEmpty()) {
			//Try to get the first one...
			minRep.acquireLock();
			//See if a resource uses this representative...
			resourceMap.clear();
			for (Resource resource : resources) {
				//Get the updated representatives...
				rep = resource.getRepresentative();
				if (!resourceMap.containsKey(rep)) {
					resourceMap.put(rep, new ArrayList<Resource>());
				}
				resourceMap.get(rep).add(resource);
			}
			uniqueReps = resourceMap.keySet();
			tempMinRep = uniqueReps.iterator().next();
			for (Representative representative : uniqueReps) {
				if (representative.compareTo(tempMinRep)<0) {
					tempMinRep = representative;
				}
			}
			//The one we see must be the first, as we must lock in the right order.
			if (tempMinRep.equals(minRep)) {
				//Got a useful one! To the next index...
				acquired.add(minRep);
				for (Resource resource : resourceMap.get(minRep)) {
					resources.remove(resource); //for efficiency - don't need to consider these again
				}
				//Get the next smallest...
				uniqueReps.remove(minRep);
				if (!uniqueReps.isEmpty()) {
					minRep = uniqueReps.iterator().next();
					for (Representative representative : uniqueReps) {
						if (representative.compareTo(minRep)<0) {
							minRep = representative;
						}
					}
				} //else we're done!
			} else {
				//The representative is now useless... Better throw it away and start again...
				minRep.releaseLock();
				noRestarts++;
				minRep = tempMinRep; //need to start from the new representative too
				if (!isOld && noRestarts>threshold) {
					//Too many!!
					addOldProcess();
					isOld = true; //only do this once
				}
			}
		}
		//Find the largest rep...
		Representative maxRep = acquired.isEmpty() ? null : acquired.iterator().next();
		for (Representative representative : acquired) {
			if (maxRep.compareTo(representative)<0) {
				maxRep = representative;
			}
		}
		BigInteger baseid = maxRep==null ? BigInteger.ZERO : maxRep.baseId;
		return new ResourceManipulator(this, acquired, isOld, baseid);
	}
	
	/**
	 * Acquire a single resource from the graph. This will ensure all other resources dependent on what you wish to acquire are also
	 * acquired to prevent any concurrency issues!
	 * @param resource - the resource to acquire
	 * @return - a resource manipulator that allows you to modify this resource (and any others implicitly acquired
	 * due to dependencies, although it is best not to rely on these as you are unlikely to know the structure of the graph...)
	 */
	public ResourceManipulator acquireResource(Resource resource) {
		//Implemented separately because it is a little more efficient and a common case
		int noRestarts = 0;
		int threshold = OLD_THRESHOLD;
		boolean isOld = false;
		if (noOldProcesses>0) {
			//Wait!
			waitSemaphore.acquireUninterruptibly();
			waitSemaphore.release();
		}
		Representative rep;
		while (true) {
			rep = resource.getRepresentative();
			rep.acquireLock();
			if (resource.getRepresentative().equals(rep)) {
				//Done!
				break;
			} else {
				rep.releaseLock();
				noRestarts++;
				if (!isOld && noRestarts>threshold) {
					//Too many!!
					addOldProcess();
					isOld = true; //only do this once
				}
			}
		}
		Set<Representative> acquired = new HashSet<Representative>();
		acquired.add(rep);
		return new ResourceManipulator(this, acquired, isOld, rep.baseId);
	}
	
	/**
	 * @return - get a manipulator which locks on nothing
	 */
	public ResourceManipulator getManipulator() {
		//Again - more efficient and a common case...
		return new ResourceManipulator(this, new HashSet<Representative>(), false, BigInteger.ZERO);
	}
	
	/**
	 * To be called by a resource manipulator only, this releases all of the resources it had locked up!
	 * @param manipulator - the manipulator whose resources should be released.
	 */
	void releaseResources(ResourceManipulator manipulator) {
		//Unlock everything...
		for (Representative representative : manipulator.representatives) {
			representative.releaseLock(); //should include all "true" representatives as well as the old ones
			//which may be waited on
		}
		//When done:
		if (manipulator.isOld) {
			//I need to potentially release the waiting processes...
			oldProcessesSemaphore.acquireUninterruptibly();
			noOldProcesses--;
			if (noOldProcesses==0) {
				//Release them!
				waitSemaphore.release();
			}
			oldProcessesSemaphore.release();
		} //else just leave...
	}
	
	/**
	 * To remove code duplication, this registers the fact that a particular process claims to be old,
	 * and so new processes should wait for the resource graph to become quiet again.
	 * Should only be called once by a process!!
	 */
	private void addOldProcess() {
		oldProcessesSemaphore.acquireUninterruptibly();
		if (noOldProcesses==0) {
			//Stop everyone!
			waitSemaphore.acquireUninterruptibly();
		}
		noOldProcesses++;
		oldProcessesSemaphore.release();
	}
}
