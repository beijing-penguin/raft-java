package org.dc.raft;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest  extends TestCase{

	private static CountDownLatch downLatch = new CountDownLatch(1);
    public static void main(String[] args) {
    	new Thread(new Runnable() {
			
			public void run() {
				try {
					Thread.sleep(2000);
					downLatch.countDown();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
		}).start();
        	try {
    			downLatch.await(5,TimeUnit.SECONDS);
    			System.out.println(111);
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
	}
}
