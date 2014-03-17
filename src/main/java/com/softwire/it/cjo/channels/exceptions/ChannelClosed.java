package com.softwire.it.cjo.channels.exceptions;

/**
 * ****************<br>
 * Date: 17/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This exception is thrown when a channel becomes closed. This is not to be considered as a disaster,
 * as channels may be closed to enforce the termination of an algorithm (as is used in many CSO examples).
 *
 */
public class ChannelClosed extends RuntimeException {
	private static final long serialVersionUID = -7232668694512018534L;
}
