package com.softwire.it.cjo.channels;

import org.junit.Test;

import com.softwire.it.cjo.channels.OneOneChannel;
import com.softwire.it.cjo.operators.Channel;

import static com.softwire.it.cjo.operators.Ops.*;
import static org.junit.Assert.*;

/**
 * ****************<br>
 * Date: 16/03/2014<br>
 * Author:  michael<br>
 * ****************<br>
 * <br>
 * This class tests the correctness of the OneOneChannel
 *
 */
public class OneOneChannelTest {

	/**
	 * Some preliminary very basic tests to make sure the channel is working even remotely..
	 */
	@Test
	public void basicTest() {
		final Channel<Integer> channel = new OneOneChannel<Integer>();
		//Construct a thread to write to the channel, and another to read...
		Thread t = new Thread(new Runnable() {public void run() {
			write(channel,3);
		}});
		t.start();
		//Now read the message...
		assertTrue(read(channel)==3);
	}

}
