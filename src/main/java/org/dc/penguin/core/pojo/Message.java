package org.dc.penguin.core.pojo;

import org.dc.jdbc.core.pojo.Table;

import com.alibaba.fastjson.JSON;

@Table(name="RAFT_TABLE")
public class Message {
	private Long id;
	private int msgCode;
	private String key;
	private byte[] value;
	private Long dataIndex;
	private Integer term;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getDataIndex() {
		return dataIndex;
	}
	public void setDataIndex(Long dataIndex) {
		this.dataIndex = dataIndex;
	}
	public Integer getTerm() {
		return term;
	}
	public void setTerm(Integer term) {
		this.term = term;
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
