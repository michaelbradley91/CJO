package com.softwire.it.cjo;

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
        //System.out.println( "Hello World!" );
    }
}
