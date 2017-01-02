package org.dc.penguin.core.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MachineInfo {
	private int role = Role.FOLLOWER;
	private Map<String,byte[]> data = new ConcurrentHashMap<String,byte[]>();
	private String host;
	private int port;
	
	public int getRole() {
		return role;
	}
	public void setRole(int role) {
		this.role = role;
	}
	public Map<String, byte[]> getData() {
		return data;
	}
	public void setData(Map<String, byte[]> data) {
		this.data = data;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	
}
