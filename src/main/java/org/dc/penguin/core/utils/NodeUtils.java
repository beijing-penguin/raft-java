package org.dc.penguin.core.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.jdbc.core.ConnectionManager;
import org.dc.jdbc.core.DBHelper;
import org.dc.penguin.core.NodeConfigInfo;
import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MsgType;
import org.dc.penguin.core.pojo.RoleType;
import org.dc.penguin.core.raft.NodeInfo;

import com.alibaba.fastjson.JSON;

public class NodeUtils {
	private static Log LOG = LogFactory.getLog(NodeUtils.class);
	public static NodeInfo leaderNodeInfo = null;
	public static AtomicInteger voteNum = new AtomicInteger(0);
	public static void getLeaderNodeInfo() {//获取当前集群下领导节点的NodeConfig自身配置信息
		try {
			for (NodeInfo nodeInfo: NodeConfigInfo.nodeVector) {
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
			for (NodeInfo nodeInfo: NodeConfigInfo.nodeVector) {
				//if(!nodeInfo.getHost().equals(mynodeInfo.getHost()) && nodeInfo.getElectionServerPort()!=mynodeInfo.getElectionServerPort()) {
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
				//}
			}
		} catch (Exception e) {
			LOG.error("",e);
		}
	}
	public static void sendLeaderPing(NodeInfo mynodeInfo) {
		try {
			int size = NodeConfigInfo.nodeVector.size();
			CountDownLatch cdl = new CountDownLatch(size/2);
			for (NodeInfo nodeInfo: NodeConfigInfo.nodeVector) {
				if(!nodeInfo.getHost().equals(mynodeInfo.getHost()) || (nodeInfo.getHost().equals(mynodeInfo.getHost()) && nodeInfo.getElectionServerPort()!=mynodeInfo.getElectionServerPort())) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							SocketConnection conn = null;
							try {
								SocketPool pool = RaftUtils.getSocketPool(nodeInfo.getHost(), nodeInfo.getElectionServerPort());
								conn = pool.getSocketConnection();
								Message msg = new Message();
								msg.setValue(JSON.toJSONString(mynodeInfo).getBytes());
								msg.setMsgCode(MsgType.LEADER_PING);
								Message ms = JSON.parseObject(conn.sendMessage(msg.toJSONString()), Message.class);
								if(ms.getMsgCode()== MsgType.SUCCESS) {
									cdl.countDown();
								}
							} catch (Exception e) {
								LOG.error("",e);
							}finally {
								if(conn!=null) {
									conn.close();
								}
							}
						}
					}).start();
				}
			}

			cdl.await(5,TimeUnit.SECONDS);

			if(cdl.getCount()!=0) {
				mynodeInfo.setRole(RoleType.FOLLOWER);
			}
		} catch (Exception e) {
			LOG.error("",e);
		}
	}
	public static String createLeaderKeyByWriteLog(NodeInfo nodeInfo) {
		return nodeInfo.getHost()+":"+nodeInfo.getDataServerPort()+":"+nodeInfo.getElectionServerPort()+":"+nodeInfo.getTerm().get()+":"+nodeInfo.getDataIndex().incrementAndGet();
	}
	public static void main(String[] args) throws Exception {

	}
	public static void logSync(NodeInfo leaderNode) {
		for (NodeInfo nodeInfo: NodeConfigInfo.nodeVector) {
			if(!nodeInfo.getHost().equals(leaderNode.getHost()) || (nodeInfo.getHost().equals(leaderNode.getHost()) && nodeInfo.getDataServerPort()!=leaderNode.getDataServerPort())) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Message msg = new Message();
							msg.setMsgCode(MsgType.GET_DATAINDEX_POS);
							Message nodePosMsg = RaftUtils.sendMessage(nodeInfo.getHost(), nodeInfo.getElectionServerPort(),msg.toJSONString());
							Long nodeOther_dataIndex = nodePosMsg.getDataIndex();

							//领导者数据大于其他node时，对其他node做增量数据同步
							if(leaderNode.getDataIndex().intValue()>nodeOther_dataIndex) {
								//开始同步从node_dataIndex位置到leader_dataIndex的数据同步
								DBHelper dbHelper = RaftUtils.getDBHelper(leaderNode.getHost(), leaderNode.getDataServerPort());
								ConnectionManager.setReadOnly(true);
								
								int num =0;
								while(nodeOther_dataIndex.longValue() != leaderNode.getDataIndex().longValue()) {
									nodeOther_dataIndex++;
									Message msgData = dbHelper.selectOne("select * from RAFT_TABLE where data_index = "+nodeOther_dataIndex+" and order by id desc limit 1",Message.class);
									if(msgData!=null) {
										msgData.setMsgCode(MsgType.LEADER_SET_DATA);
										
										Message rt_msg = RaftUtils.sendMessage(nodeInfo.getHost(), nodeInfo.getElectionServerPort(),msgData.toJSONString());
										if(num>5) {
											break;
										}
										if(rt_msg.getMsgCode()!=MsgType.SUCCESS) {
											nodeOther_dataIndex--;
											num++;
										}
									}
								}
							}
						} catch (Exception e) {
							LOG.error("",e);
						} 
					}
				}).start();
			}
		}
	}
}
