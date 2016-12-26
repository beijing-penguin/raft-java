package org.dc.penguin.core.entity;

import com.alibaba.fastjson.JSON;

public class Message {
	private int reqType;
	private byte[] body;
	
	public int getReqType() {
		return reqType;
	}
	public void setReqType(int reqType) {
		this.reqType = reqType;
	}
	
	public byte[] getBody() {
		return body;
	}
	public void setBody(byte[] body) {
		this.body = body;
	}
	public String toJSONString(){
		return JSON.toJSONString(this)+"\n";
	}
}
