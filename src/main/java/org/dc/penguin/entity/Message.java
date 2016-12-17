package org.dc.penguin.entity;

public class Message {
	private int reqType;//100，认证
	private String body;
	
	public int getReqType() {
		return reqType;
	}
	public void setReqType(int reqType) {
		this.reqType = reqType;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
}
