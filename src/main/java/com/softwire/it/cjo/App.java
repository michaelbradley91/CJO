package com.softwire.it.cjo;

import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

/**
 * Hello world!
 *
 */
public class App {
    public static void main( String[] args ) {
    	//Try logging something!!
    	Logger logger = Logger.getLogger(App.class);
    	logger.warn("This is a test log message");
    	final Semaphore finished = new Semaphore(0);
        //System.out.println( "Hello World!" );
    	Thread t = new Thread(new Runnable() {public void run() {
    		Thread t = new Thread(new Runnable() {public void run() {
    			finished.release();
    		}});
    		t.setDaemon(false);
    		System.out.println("Inner is daemon? " + t.isDaemon());
    		t.start();
    	}});
    	t.setDaemon(true);
    	System.out.println("Inner is daemon? " + t.isDaemon());
    	t.start();
    	try {
			finished.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
