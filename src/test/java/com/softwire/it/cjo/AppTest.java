package com.softwire.it.cjo;

import static org.junit.Assert.*;
import mjb.dev.cjo.channels.OneOneChannel;
import mjb.dev.cjo.operators.AltBuilder;
import mjb.dev.cjo.operators.AltBuilder.ReadProcess;

import org.junit.Test;



/**
 * Unit test for simple App.
 */
public class AppTest {
    
    @Test
    public void testApp() {
    	AltBuilder alt = new AltBuilder();
    	OneOneChannel<Integer> channel = new OneOneChannel<Integer>();
    	alt.addReadBranch(channel, new ReadProcess<Integer>() {

			@Override
			public void run(Integer message) {
				System.out.println("Got the message " + message);
			}});
    	//Rigorous test
        assertTrue(true);
    }
}
