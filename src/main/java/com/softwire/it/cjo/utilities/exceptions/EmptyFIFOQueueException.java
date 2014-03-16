package com.softwire.it.cjo.utilities.exceptions;

/**
 * ****************<br>
 * Date: 16/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This exception is thrown if you attempt to dequeue an item from an empty FIFO queue
 *
 */
public class EmptyFIFOQueueException extends IllegalStateException {
	private static final long serialVersionUID = -4950435982832108864L;
}
