package com.softwire.it.cjo.resource_control.exceptions;

/**
 * ****************
 * Date: 11/03/2014
 * @author michael
 * ****************
 * 
 * This exception is thrown when an attempt is made to manipulate a resource in the resource graph
 * that is not held by the resource manipulator.
 *
 */
public class ResourceNotHeldException extends RuntimeException {
	private static final long serialVersionUID = 7854509356128153576L;
}
