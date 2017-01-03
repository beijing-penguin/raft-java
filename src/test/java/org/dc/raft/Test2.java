package org.dc.raft;


import java.util.concurrent.Callable;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class Test2  extends TestCase{

	public static void main(String[] args) {
		new Runnable() {
			
			public void run() {
			}
		};
		new Callable<String>() {

			public String call() throws Exception {
				System.out.println(111);
				return null;
			}
		};
	}
}
