package org.dc.penguin.core.pojo;

import org.dc.penguin.core.raft.NodeInfo;

import io.netty.channel.ChannelHandlerContext;

public class MessageQueue {
	private NodeInfo nodeInfo;
	private Message message;
	private ChannelHandlerContext handlerContext;
	
	
	public NodeInfo getNodeInfo() {
		return nodeInfo;
	}
	public void setNodeInfo(NodeInfo nodeInfo) {
		this.nodeInfo = nodeInfo;
	}
	public Message getMessage() {
		return message;
	}
	public void setMessage(Message message) {
		this.message = message;
	}
	public ChannelHandlerContext getHandlerContext() {
		return handlerContext;
	}
	public void setHandlerContext(ChannelHandlerContext handlerContext) {
		this.handlerContext = handlerContext;
	}
}
