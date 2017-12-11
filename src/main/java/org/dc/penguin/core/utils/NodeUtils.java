package org.dc.penguin.core.utils;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
			int size = NodeConfigInfo.getNodeConfigList().size();
			CountDownLatch cdl = new CountDownLatch(size/2+size%2);
			for (NodeInfo nodeInfo: NodeConfigInfo.getNodeConfigList()) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							SocketPool pool = SocketCilentUtils.getSocketPool(nodeInfo.getHost(), nodeInfo.getElectionServerPort());
							SocketConnection conn = pool.getSocketConnection();
							Message msg = new Message();
							msg.setValue(JSON.toJSONString(mynodeInfo).getBytes());
							msg.setMsgCode(MsgType.LEADER_PING);
							Message ms = JSON.parseObject(conn.sendMessage(msg.toJSONString()), Message.class);
							if(ms.getMsgCode()== MsgType.SUCCESS) {
								cdl.countDown();
							}
						} catch (Exception e) {
							LOG.error("",e);
						}
					}
				}).start();
			}

			cdl.await(5,TimeUnit.SECONDS);

			if(cdl.getCount()!=0) {
				mynodeInfo.setRole(RoleType.FOLLOWER);
			}
		} catch (Exception e) {
			LOG.error("",e);
		}
	}
	public static String createLeaderKey(NodeInfo nodeInfo) {
		return nodeInfo.getHost()+":"+nodeInfo.getDataServerPort()+":"+nodeInfo.getElectionServerPort()+":"+nodeInfo.getTerm().get()+":"+nodeInfo.getDataIndex();
	}
	public static String createLeaderKeyByWriteLog(NodeInfo nodeInfo) {
		return nodeInfo.getHost()+":"+nodeInfo.getDataServerPort()+":"+nodeInfo.getElectionServerPort()+":"+nodeInfo.getTerm().get()+":"+nodeInfo.getDataIndex().incrementAndGet();
	}
	public static void initNodeInfo(NodeInfo nodeInfo) throws Exception {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile("/data/raft/dataLog8881", "r");
			long len = raf.length();
			String lastLine = null;
			if (len != 0L) {
				long pos = len - 1;
				while (pos > 0) {
					pos--;
					raf.seek(pos);
					if (raf.readByte() == '\n') {
						break;
					}
				}
				if (pos == 0) {  
					raf.seek(0);  
				}
				byte[] bytes = new byte[(int) (len - pos)];
				raf.read(bytes);
				lastLine = new String(bytes, "utf-8"); 
			}
			
			if(StringUtils.isNotEmpty(lastLine)) {
				Message message = JSON.parseObject(lastLine,Message.class);
				nodeInfo.setDataIndex(new AtomicInteger(Integer.parseInt(message.getLeaderKey().split(":")[4])));
				nodeInfo.setTerm(new AtomicInteger(Integer.parseInt(message.getLeaderKey().split(":")[3])));
				System.out.println("当前节点状态="+JSON.toJSONString(nodeInfo));
			}
		}catch (Exception e) {
			throw e;
		}finally {
			if(raf!=null) {
				raf.close();
			}
		}
	}
	public static void main(String[] args) throws Exception {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile("/data/raft/dataLog8881", "r");
			long len = raf.length();
			String lastLine = null;
			if (len != 0L) {
				long pos = len - 1;
				while (pos > 0) {
					pos--;
					raf.seek(pos);
					if (raf.readByte() == '\n') {
						break;
					}
				}
				if (pos == 0) {  
					raf.seek(0);  
				}  
				byte[] bytes = new byte[(int) (len - pos)];
				raf.read(bytes);
				lastLine = new String(bytes, "utf-8"); 
			}
			System.out.println(lastLine);
			System.out.println(JSON.parseObject(lastLine, Message.class));
		}catch (Exception e) {
			throw e;
		}finally {
			if(raf!=null) {
				raf.close();
			}
		}
	}
	public static void logSync(NodeInfo mynodeInfo) {
		try {
			for (NodeInfo nodeInfo: NodeConfigInfo.getNodeConfigList()) {
				if(nodeInfo.getHost().equals(mynodeInfo.getHost()) && mynodeInfo.getElectionServerPort() != nodeInfo.getElectionServerPort())
					new Thread(new Runnable() {
						@Override
						public void run() {
							SocketPool pool = SocketCilentUtils.getSocketPool(nodeInfo.getHost(), nodeInfo.getElectionServerPort());
							SocketConnection conn = null;
							try {
								conn = pool.getSocketConnection();
								Message msg = new Message();
								msg.setMsgCode(MsgType.GET_DATA_POS);
								String rt_str = conn.sendMessage(msg.toJSONString());
								int node_term = Integer.parseInt(rt_str.split(":")[0]);
								int node_dataIndex = Integer.parseInt(rt_str.split(":")[1]);

								List<Message> syncList = new ArrayList<Message>(); 
								RandomAccessFile rf = null;
								try {
									rf = new RandomAccessFile(NodeConfigInfo.dataLogDir, "r");
									long len = rf.length();
									long start = rf.getFilePointer();
									long nextend = start + len - 1;
									String line = null;
									rf.seek(nextend);
									int c = -1;
									while (nextend > start) {
										c = rf.read();
										if (c == '\n' || c == '\r') {
											line = new String(rf.readLine().getBytes("ISO-8859-1"), "utf-8");
											nextend--;
										}
										nextend--;
										rf.seek(nextend);
										if (nextend == 0) {// 当文件指针退至文件开始处，输出第一行
											// System.out.println(rf.readLine());
											line = new String(rf.readLine().getBytes("ISO-8859-1"), "utf-8");
										}

										Message data_msg = JSON.parseObject(line, Message.class);

										int my_term = Integer.parseInt(data_msg.getLeaderKey().split(":")[3]);
										int my_dataIndex = Integer.parseInt(data_msg.getLeaderKey().split(":")[4]);

										if(my_term>=node_term && my_dataIndex>node_dataIndex) {
											syncList.add(data_msg);
										}else {
											break;
										}
									}
								}  catch (Exception e) {
									LOG.error("",e);
								} finally {
									try {
										if (rf != null)
											rf.close();
									} catch (Exception e) {
										LOG.error("",e);
									}
								}

								for (int i = syncList.size(); i > 0; i--) {
									Message msg_sync = syncList.get(i-1);
									msg_sync.setMsgCode(MsgType.LEADER_SET_DATA);
									conn.sendMessage(msg_sync.toJSONString());
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
		} catch (Exception e) {
			LOG.error("",e);
		}
	}
}
