package org.dc.penguin.core.pojo;

public interface MsgType {
	public static int SUCCESS = 1;
	public static int GET_DATA = 100;
	public static int SET_DATA = 200;
	
	public static int PING = 400;
	public static int LEADER_PING = 401;
	public static int VOTE = 402;
}
