package com.softwire.it.cjo.parallelresources;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import mjb.dev.cjo.parallelresources.Resource;
import mjb.dev.cjo.parallelresources.ResourceGraph;
import mjb.dev.cjo.parallelresources.ResourceManipulator;
import mjb.dev.cjo.parallelresources.exceptions.ResourceNotHeldException;
import mjb.dev.cjo.parallelresources.exceptions.ResourceReleasedException;

import org.apache.log4j.Logger;
import org.junit.Test;


/**
 * ****************<br>
 * Date: 14/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class tests the concurrency mechanism provided by the resource graph<br>
 * Confusingly, this class only tests the methods with a single thread<br>
 * <br>
 * This is a very tricky test, because the object itself holds as little state as possible
 * (so it is hard to query it to check for correctness)
 *
 */
public class ResourceGraphSequentialTest {

	private static final ResourceGraph GRAPH = ResourceGraph.INSTANCE;
	
	/**
	 * Test the ability to just construct resource and put them in the resource graph.
	 * (Very simple test)
	 */
	@Test
	public void testResourceCreation() {
		ResourceManipulator manipulator = GRAPH.getManipulator();
		Resource resource = manipulator.addResource();
		//Now release the resources, and then try to acquire the resource...
		manipulator.releaseResources();
		//Acquire it
		manipulator = GRAPH.acquireResource(resource);
		//Pretend to remove it (should do nothing)
		manipulator.removeResource(resource);
		manipulator.releaseResources();
		//Done!
	}
	
	/**
	 * Test the ability to add a dependency between two resources,
	 * and then make sure we obtain a lock on both when we acquire resources
	 * in the future
	 */
	@Test
	public void testDependencyCreation() {
		ResourceManipulator manipulator = GRAPH.getManipulator();
		Resource resource1 = manipulator.addResource();
		Resource resource2 = manipulator.addResource();
		//Now add an edge between them
		manipulator.addDependency(resource1, resource2);
		//Throw in a self loop too
		manipulator.addDependency(resource1, resource1);
		//Now release...
		manipulator.releaseResources();
		//Now try to acquire only one resource, and check we get the other...
		manipulator = GRAPH.acquireResource(resource1);
		manipulator.removeDependency(resource1, resource2);
		//Should be ok. Now make sure we only get one when we lock on this time
		manipulator.releaseResources();
		manipulator = GRAPH.acquireResource(resource1);
		try {
			manipulator.addDependency(resource1, resource2);
			fail("Succeeded in adding an edge between resources when one was not acquired");
		} catch (ResourceNotHeldException e) {}
		//Good!
		manipulator.releaseResources();
	}
	
	/**
	 * Test the generation of exceptions when the resource graph is abused - when we try to manipulate
	 * resources we have not acquired...
	 */
	@Test
	public void testExceptions() {
		Logger logger = Logger.getLogger(ResourceGraphSequentialTest.class);
		ResourceManipulator manipulator = GRAPH.getManipulator();
		Resource resource1 = manipulator.addResource();
		Resource resource2 = manipulator.addResource();
		//Make two resources...
		manipulator.releaseResources();
		//Now acquire one, and try to use the other...
		manipulator = GRAPH.acquireResource(resource1);
		try {
			manipulator.addDependency(resource1, resource2);
			fail("Succeeded in adding an edge between resources when one was not acquired");
		} catch (ResourceNotHeldException e) {}
		logger.trace("testExceptions: Succeeded in dependency exception");
		//Try adding a resource...
		Resource resource3 = manipulator.addResource();
		manipulator.addDependency(resource1, resource3);
		try {
			manipulator.addDependency(resource2, resource3);
			fail("Succeeded in adding an edge between resources when one was not acquired");
		} catch (ResourceNotHeldException e) {}
		logger.trace("testExceptions: Succeeded in dependency exception v2");
		//Now release
		manipulator.releaseResources();
		//Now try to acquire all resources, but release and then operate
		Set<Resource> toAcquire = new HashSet<Resource>();
		toAcquire.add(resource3);
		toAcquire.add(resource2);
		toAcquire.add(resource1);
		manipulator = GRAPH.acquireResources(toAcquire);
		logger.trace("testExceptions: Managed to acquire all three resources");
		//Release early
		manipulator.releaseResources();
		//Now modify...
		try {
			manipulator.addResource();
			fail("Succeeded in modifying resources after they were released");
		} catch (ResourceReleasedException e) {}
		try {
			manipulator.addDependency(resource1, resource2);
			fail("Succeeded in modifying resources after they were released");
		} catch (ResourceReleasedException e) {}
		//If we made it this far, resources are probably working!
		logger.trace("testExceptions: completed");
	}
	
	/**
	 * Test that we are able to acquire multiple resources with different representatives
	 */
	@Test
	public void testMultipleAcquisition() {
		Logger logger = Logger.getLogger(ResourceGraphSequentialTest.class);
		ResourceManipulator manipulator = GRAPH.getManipulator();
		Resource resource1 = manipulator.addResource();
		Resource resource2 = manipulator.addResource();
		Resource resource3 = manipulator.addResource();
		Resource resource4 = manipulator.addResource();
		Resource resource5 = manipulator.addResource();
		manipulator.releaseResources();
		//Gather some up!
		Set<Resource> toAcquire = new HashSet<Resource>();
		toAcquire.add(resource3);
		toAcquire.add(resource2);
		toAcquire.add(resource1);
		manipulator = GRAPH.acquireResources(toAcquire);
		toAcquire.clear();
		toAcquire.add(resource4);
		toAcquire.add(resource5);
		ResourceManipulator manipulator2 = GRAPH.acquireResources(toAcquire);
		//Both of the above should succeed.
		manipulator.addDependency(resource1, resource2);
		manipulator2.addDependency(resource4, resource5);
		manipulator.releaseResources();
		manipulator2.releaseResources();
		//Now try and acquire some with dependencies
		toAcquire.clear();
		toAcquire.add(resource3);
		toAcquire.add(resource2);
		toAcquire.add(resource4); //should get them all
		manipulator = GRAPH.acquireResources(toAcquire);
		//Now add edges to be sure
		manipulator.addDependency(resource1, resource2);
		manipulator.addDependency(resource2, resource3);
		manipulator.addDependency(resource3, resource4);
		manipulator.addDependency(resource4, resource5);
		manipulator.releaseResources();
		//Now try to acquire a singleton set, and an empty set...
		toAcquire.clear();
		toAcquire.add(resource3);
		manipulator = GRAPH.acquireResources(toAcquire);
		//Check we have everything...
		manipulator.removeDependency(resource1, resource2);
		manipulator.removeDependency(resource2, resource3);
		manipulator.removeDependency(resource3, resource4);
		manipulator.removeDependency(resource4, resource5);
		//Now acquire an empty set...
		toAcquire.clear();
		manipulator2 = GRAPH.acquireResources(toAcquire);
		manipulator2.addResource();
		//Should have all gone through!
		manipulator2.releaseResources();
		manipulator.releaseResources();
		logger.trace("testMultipleAcquisition: completed");
	}
	
	/**
	 * Test how resources are split (and reassigned representatives in code)
	 * when dependencies are removed
	 */
	@Test
	public void testDependencyRemoval() {
		Logger logger = Logger.getLogger(ResourceGraphSequentialTest.class);
		ResourceManipulator manipulator = GRAPH.getManipulator();
		Resource resource1 = manipulator.addResource();
		Resource resource2 = manipulator.addResource();
		Resource resource3 = manipulator.addResource();
		Resource resource4 = manipulator.addResource();
		Resource resource5 = manipulator.addResource();
		//Make resource 3 the centre of four edges, but have resource 1 only connected to resource 2
		manipulator.addDependency(resource1, resource2);
		manipulator.addDependency(resource3, resource2);
		manipulator.addDependency(resource3, resource4);
		manipulator.addDependency(resource3, resource5);
		//Test that we do lock on all resources at once...
		manipulator.releaseResources();
		manipulator = GRAPH.acquireResource(resource1);
		manipulator.addDependency(resource1, resource2);
		manipulator.addDependency(resource3, resource2);
		manipulator.addDependency(resource3, resource4);
		manipulator.addDependency(resource3, resource5);
		manipulator.releaseResources();
		//Done! Now try to remove resource 3...
		manipulator = GRAPH.acquireResource(resource1);
		manipulator.removeResource(resource3);
		//Now release, and re-acquire...
		manipulator.releaseResources();
		manipulator = GRAPH.acquireResource(resource1);
		try {
			manipulator.addDependency(resource1, resource3);
		} catch (ResourceNotHeldException e) {}
		try {
			manipulator.addDependency(resource1, resource4);
		} catch (ResourceNotHeldException e) {}
		try {
			manipulator.addDependency(resource1, resource5);
		} catch (ResourceNotHeldException e) {}
		manipulator.addDependency(resource1, resource2);
		//Now try splitting resources 2 and 1
		manipulator.removeDependency(resource1, resource2);
		manipulator.releaseResources();
		manipulator = GRAPH.acquireResource(resource1);
		try {
			manipulator.addDependency(resource1, resource2);
		} catch (ResourceNotHeldException e) {}
		manipulator.releaseResources();
		logger.trace("testDependencyRemoval: completed");
	}
}
