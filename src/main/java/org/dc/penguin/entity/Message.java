package org.dc.penguin.entity;

import com.alibaba.fastjson.JSON;

public class Message {
	private int reqType;
	private Object body;
	
	public int getReqType() {
		return reqType;
	}
	public void setReqType(int reqType) {
		this.reqType = reqType;
	}
	public Object getBody() {
		return body;
	}
	public void setBody(Object body) {
		this.body = body;
	}
	
	public String toJSONString(){
		return JSON.toJSONString(this)+"\n";
	}
}
