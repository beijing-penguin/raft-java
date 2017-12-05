package org.dc.penguin.core.pojo;

public interface MsgType {
	public static int SUCCESS = 1;
	public static int FAIL = -1;
	public static int NO_LEADER = -2;
	
	public static int GET_DATA = 100;
	public static int LEADER_SET_DATA = 101;
	public static int CLIENT_SET_DATA = 102;
	
	public static int PING = 400;
	public static int LEADER_PING = 401;
	public static int VOTE = 402;
}
