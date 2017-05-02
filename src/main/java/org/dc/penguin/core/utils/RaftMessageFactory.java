package org.dc.penguin.core.utils;

import org.dc.penguin.core.ConfigInfo;
import org.dc.penguin.core.entity.Message;
import org.dc.penguin.core.entity.MsgType;
import org.dc.penguin.core.raft.LocalStateMachine;

import com.alibaba.fastjson.JSON;

public class RaftMessageFactory {
	/*public static String createSendPollMsg(){
		Message msg = new Message();
		msg.setReqType(MsgType.POLL_ME);
		return msg.toJSONString();
	}
	public static Message createGetLeaderMsg(){
		Message msg = new Message();
		msg.setReqType(MsgType.GET_LEADER);
		return msg;
	}
	public static Message createMachineInfo(LocalStateMachine leaderMachine) throws Exception{
		String hostiPData = "";
		for (int i = 0; i < InitSystemHandle.getInstance().getConnVector().size(); i++) {
			LocalStateMachine machine = InitSystemHandle.getInstance().getConnVector().get(i);
			hostiPData += machine.getHost()+":"+machine.getPort()+";";
		}
		hostiPData += "&"+ JSON.toJSONString(leaderMachine.getData());
		Message msg = new Message();
		msg.setReqType(MsgType.SYNC_DATA);
		msg.setBody(hostiPData.getBytes());
		return msg;
	}
	public static Message createjoinLeaderMsg(LocalStateMachine localStateMachine) {
		Message msg = new Message();
		msg.setReqType(MsgType.JOIN_LEADER_REQ);
		msg.setBody((localStateMachine.getHost()+":"+localStateMachine.getPort()).getBytes());
		return msg;
	}
	public static Message createSyncAllClusterInfoMsg(LocalStateMachine localStateMachine) {
		Message msg = new Message();
		msg.setReqType(MsgType.SYNC_DATA);
		msg.setBody((localStateMachine.getHost()+":"+localStateMachine.getPort()).getBytes());
		return msg;
	}
	public static Message createPingMsg() {
		Message msg = new Message();
		msg.setReqType(MsgType.RAFT_PING);
		return msg;
	}*/
}
