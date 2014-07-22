package com.softwire.it.cjo.utilities;

import static org.junit.Assert.*;

import mjb.dev.cjo.utilities.ImmutableList;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * ****************<br>
 * Date: 21/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class tests the immutable list. It does not test that it is genuinely thread safe however - only that
 * the list constructs elements correctly. (The implementation implies thread safeness immediately... you'll have to believe
 * me if you can't work out why)
 *
 */
public class ImmutableListTest {
	//The logger for this test class
	private final Logger logger = Logger.getLogger(ImmutableListTest.class);
	
	/**
	 * This test contains some sanity checks that the list is adding and removing items correctly
	 */
	@Test
	public void testAddRemove() {
		//Construct a new empty list!
		ImmutableList<Integer> list = new ImmutableList<Integer>();
		assertTrue(list.isEmpty() && list.size()==0);
		//Add something!
		list = list.add(4);
		assertTrue(!list.isEmpty() && list.size()==1);
		assertTrue(list.contains(4));
		assertTrue(list.head()==4);
		//Try removing it
		list = list.tail();
		assertTrue(list.isEmpty() && list.size()==0);
		assertFalse(list.contains(4));
		//No try adding a few and removing some...
		list = list.add(3);
		list = list.add(2);
		list = list.add(1);
		assertTrue(!list.isEmpty() && list.size()==3);
		assertTrue(list.contains(3));
		assertTrue(list.contains(2));
		assertTrue(list.contains(1));
		assertTrue(list.head()==1);
		assertTrue(list.tail().head()==2);
		assertTrue(list.tail().tail().head()==3);
		assertTrue(list.tail().tail().tail().isEmpty());
		int index = 1;
		for (int element : list) {
			assertTrue(index==element);
			index++;
		}
		//Now remove an element
		list = list.tail();
		//Check it again...
		assertTrue(!list.isEmpty() && list.size()==2);
		assertTrue(list.contains(2));
		assertTrue(list.contains(3));
		assertTrue(list.head()==2);
		assertTrue(list.tail().head()==3);
		assertTrue(list.tail().tail().isEmpty());
		index = 2;
		for (int element : list) {
			assertTrue(index==element);
			index++;
		}
		//Now remove them all
		while (!list.isEmpty()) {
			list = list.tail();
		}
		//Check it is empty
		assertTrue(list.isEmpty() && list.size()==0);
		assertFalse(list.contains(1));
		assertFalse(list.contains(2));
		assertFalse(list.contains(3));
		logger.trace("testAddRemove: complete");
	}
	
	/**
	 * This test checks everything associated to the list being empty (typically a special case)
	 */
	@Test
	public void testEmptyList() {
		//Construct a new empty list!
		ImmutableList<Integer> list = new ImmutableList<Integer>();
		assertTrue(list.isEmpty());
		//Try getting the head...
		try {
			list.head();
			fail("Managed to retrieve the head from an empty list");
		} catch (IllegalStateException e) {	}
		//Try getting the tail
		try {
			list.tail();
			fail("Managed to retrieve the tail from an empty list");
		} catch (IllegalStateException e) {}
		//Try iterating over it
		for (int element : list) {
			fail("Managed to gt element " + element + " from an empty list");
		}
		assertTrue(list.size()==0);
		//Check for containment
		assertFalse(list.contains(0));
		//Check it is equal to another empty list...
		assertTrue(list.equals(new ImmutableList<Integer>()));
		//Check it has the same hashcode
		assertTrue(list.hashCode()==(new ImmutableList<Integer>().hashCode()));
		//Good!
		logger.trace("testEmptyList: complete");
	}
	
	/**
	 * This tests the equality and hash codes of the list
	 */
	@Test
	public void testEquality() {
		ImmutableList<Integer> list1 = new ImmutableList<Integer>();
		ImmutableList<Integer> list2 = new ImmutableList<Integer>();
		//Check empty equality
		assertTrue(list1.equals(list2) && list1.hashCode()==list2.hashCode());
		//Try adding a single null element
		list1 = list1.add(null);
		list2 = list2.add(null);
		assertTrue(list1.equals(list2) && list1.hashCode()==list2.hashCode());
		//Try adding null to one but nothing to the other
		list1 = list1.add(null);
		list2 = list2.add(3);
		assertFalse(list1.equals(list2));
		//Try using different lengths only...
		list2 = list2.tail();
		list2 = list2.add(null);
		list2 = list2.add(17);
		assertFalse(list1.equals(list2));
		//Check for equal objects in the list
		list2 = list2.tail();
		list1 = list1.add(5);
		list2 = list2.add(5);
		assertTrue(list1.equals(list2) && list1.hashCode()==list2.hashCode());
		//Good!
		logger.trace("testEquality: complete");
	}
}
