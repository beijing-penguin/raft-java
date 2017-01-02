package org.dc.penguin.core.entity;

public interface MsgType {

	//发送raft心跳
	public static int RAFT_PING = 100;
	//获取leader请求
	public static int GET_LEADER = 101;
	//没有leader类型的消息，可能服务器全部宕机，或者正在选举leader进行中....
	public static int NO_LEADER = 102;
	//告诉客户端，本次返回的是leader 的数据
	//public static int YES_LEADER = 103;
	//获取数据请求
	public static int GET_DATA = 104;
	//投我请求
	public static int POLL_ME = 105;
	//同步数据
	public static int SYNC_DATA = 106;
	//加入leader请求
	public static int JOIN_LEADER_REQ = 107;
	//业务数据消息状态类型,以及各种成功状态码
	public static int SUCCESS = 1;
	public static int NO = -1;
}
