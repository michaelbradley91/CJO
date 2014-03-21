package com.softwire.it.cjo;

import static org.junit.Assert.*;
import org.junit.Test;

import com.softwire.it.cjo.channels.OneOneChannel;
import com.softwire.it.cjo.operators.AltBuilder;
import com.softwire.it.cjo.operators.AltBuilder.ReadProcess;


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
        Thread.currentThread().getu
    }
}
