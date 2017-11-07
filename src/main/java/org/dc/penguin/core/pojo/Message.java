package org.dc.penguin.core.pojo;

import com.alibaba.fastjson.JSON;

public class Message {
	private int msgCode;
	private String key;
	private byte[] value;
	private String leaderKey;
	
	
	public String getLeaderKey() {
		return leaderKey;
	}
	public void setLeaderKey(String leaderKey) {
		this.leaderKey = leaderKey;
	}
	public int getMsgCode() {
		return msgCode;
	}
	public void setMsgCode(int msgCode) {
		this.msgCode = msgCode;
	}
	public String toJSONString(){
		return JSON.toJSONString(this)+"\n";
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public byte[] getValue() {
		return value;
	}
	public void setValue(byte[] value) {
		this.value = value;
	}
}
