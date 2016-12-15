package org.dc.penguin.entity;

import io.netty.channel.Channel;

public class ChannelInfo {
	private int port;
	private String host;
	private Channel channel;
	
	
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
	public Channel getChannel() {
		return channel;
	}
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	
	
}
