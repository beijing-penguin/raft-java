package org.dc.penguin.entity;

public class ServerInfo {
	private int port;
	private String host;
	private boolean firstConnGood;
	private int role;
	
	
	
	public int getRole() {
		return role;
	}
	public void setRole(int role) {
		this.role = role;
	}
	public boolean isFirstConnGood() {
		return firstConnGood;
	}
	public void setFirstConnGood(boolean firstConnGood) {
		this.firstConnGood = firstConnGood;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
}
