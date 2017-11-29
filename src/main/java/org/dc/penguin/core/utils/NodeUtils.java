package org.dc.penguin.core.utils;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.NodeConfigInfo;
import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MsgType;
import org.dc.penguin.core.raft.NodeInfo;

import com.alibaba.fastjson.JSON;

public class NodeUtils {
	private static Log LOG = LogFactory.getLog(NodeUtils.class);
	public static NodeInfo leaderNodeInfo = null;
	public static AtomicInteger voteNum = new AtomicInteger(0);
	public static void getLeaderNodeInfo() {//获取当前集群下领导节点的NodeConfig自身配置信息
		try {
			for (NodeInfo nodeInfo: NodeConfigInfo.getNodeConfigList()) {
				NettyConnection conn = new NettyConnection(nodeInfo.getHost(),nodeInfo.getElectionServerPort());
				Message msg = new Message();
				msg.setMsgCode(MsgType.LEADER_PING);//领导消息ping
				conn.sendMessage(msg);
			}
		} catch (Exception e) {
			LOG.error("",e);
		}
	}
	public static void sendVote(NodeInfo mynodeInfo) {
		try {
			for (NodeInfo nodeInfo: NodeConfigInfo.getNodeConfigList()) {
				if(!nodeInfo.getHost().equals(mynodeInfo.getHost()) && nodeInfo.getElectionServerPort()!=mynodeInfo.getElectionServerPort()) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								NettyConnection conn = new NettyConnection(nodeInfo.getHost(),nodeInfo.getElectionServerPort());
								Message msg = new Message();
								msg.setValue(JSON.toJSONString(mynodeInfo).getBytes());
								msg.setMsgCode(MsgType.VOTE);
								conn.sendMessage(msg);
							} catch (Exception e) {
								LOG.error("",e);
							}
						}
					}).start();
				}
			}
		} catch (Exception e) {
			LOG.error("",e);
		}
	}
	public static void sendLeaderPing(NodeInfo mynodeInfo) {
		try {
			for (NodeInfo nodeInfo: NodeConfigInfo.getNodeConfigList()) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							NettyConnection conn = new NettyConnection(nodeInfo.getHost(),nodeInfo.getElectionServerPort());
							Message msg = new Message();
							msg.setValue(JSON.toJSONString(mynodeInfo).getBytes());
							msg.setMsgCode(MsgType.LEADER_PING);
							conn.sendMessage(msg);
						} catch (Exception e) {
							LOG.error("",e);
						}
					}
				}).start();
			}
		} catch (Exception e) {
			LOG.error("",e);
		}
	}
	public static String createLeaderKey(NodeInfo nodeInfo) {
		return nodeInfo.getHost()+":"+nodeInfo.getDataServerPort()+":"+nodeInfo.getElectionServerPort()+":"+nodeInfo.getTerm().get()+":"+nodeInfo.getDataIndex();
	}
}
