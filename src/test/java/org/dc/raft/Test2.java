package org.dc.raft;

import org.dc.penguin.NettyClient;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class Test2  extends TestCase{

	public static void main(String[] args) {
		NettyClient client = new NettyClient("localhost:9001");
		try {
			client.get("");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
