package org.dc.penguin.entity;

public class ServerInfo {
	private int port;
	private String host;
	private int role;
	
	private boolean isLocalhost;
	
	
	
	public boolean isLocalhost() {
		return isLocalhost;
	}
	public void setLocalhost(boolean isLocalhost) {
		this.isLocalhost = isLocalhost;
	}
	public int getRole() {
		return role;
	}
	public void setRole(int role) {
		this.role = role;
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
