package org.dc.penguin.core.entity;

public class ServerInfo {
	private int port;
	private String host;
	private int role;
	
	private boolean isLocalhost;
	
	/**
	 * 开启链路重连心跳检查,以及raft协议发包
	 */
	/*public void startHeartbeat(){
		new Thread(new Runnable() {
			public void run() {
				NettyRaftClient client = new NettyRaftClient(port+":"+host);
			}
		}).start();
	}*/
	
	/*public Channel getChannel() {
		return channel;
	}
	public void setChannel(Channel channel) {
		this.channel = channel;
	}*/
	
	
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
