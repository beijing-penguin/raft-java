package org.dc.penguin.core.pojo;

import com.alibaba.fastjson.JSON;

public class Message {
	private String reqId;
	private int reqCode;
	private int rtnCode;
	
	private String key;
	private byte[] value;
	
	public int getReqCode() {
		return reqCode;
	}
	public void setReqCode(int reqCode) {
		this.reqCode = reqCode;
	}
	public int getRtnCode() {
		return rtnCode;
	}
	public void setRtnCode(int rtnCode) {
		this.rtnCode = rtnCode;
	}
	public String toJSONString(){
		return JSON.toJSONString(this)+"\n";
	}
	public String getReqId() {
		return reqId;
	}
	public void setReqId(String reqId) {
		this.reqId = reqId;
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
