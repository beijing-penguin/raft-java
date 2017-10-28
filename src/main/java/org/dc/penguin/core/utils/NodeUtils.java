package org.dc.penguin.core.utils;

import org.dc.penguin.core.ConfigInfo;
import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MsgType;
import org.dc.penguin.core.raft.NodeConfig;

import com.alibaba.fastjson.JSON;

public class NodeUtils {
	public NodeConfig getLeaderNodeConfig() throws Exception {//获取当前集群下领导节点的NodeConfig自身配置信息
		for (NodeConfig nodeConfig: ConfigInfo.getNodeConfigList()) {
			NettyConnection conn = new NettyConnection(nodeConfig.getHost(),nodeConfig.getElectionServerPort());
			Message msg = new Message();
			msg.setReqCode(MsgType.LEADER_PING);//询问是否是领导消息ping
			Message message = conn.sendMessage(msg);
			if(message.getRtnCode()==MsgType.SUCCESS) {
				return JSON.parseObject(message.getValue(),NodeConfig.class);
			}
		}
		return null;
	}
}
