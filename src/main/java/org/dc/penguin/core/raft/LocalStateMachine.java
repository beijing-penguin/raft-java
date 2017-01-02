package org.dc.penguin.core.raft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.InitSystemHandle;
import org.dc.penguin.core.entity.MachineInfo;
import org.dc.penguin.core.entity.Message;
import org.dc.penguin.core.entity.MsgType;
import org.dc.penguin.core.entity.Role;
import org.dc.penguin.core.utils.NettyConnection;
import org.dc.penguin.core.utils.RaftMessageFactory;

import com.alibaba.fastjson.JSON;

/**
 * 本地状态机
 * @author DC
 *
 */
public class LocalStateMachine {
	private static Log LOG = LogFactory.getLog(LocalStateMachine.class);
	private static InitSystemHandle initConfig = InitSystemHandle.getInstance();
	
	private MachineInfo machineInfo;
	
	private int voteNum;//票数
	private boolean isLocalhost;


	private NettyConnection nettyConnection;

	public LocalStateMachine(String host ,int port ){
		this.host = host;
		this.port = port;
	}

	public void sendPollInvitation() throws Exception {
		for (int i = 0; i < initConfig.getConnVector().size(); i++) {
			try {
				LocalStateMachine machine = initConfig.getConnVector().get(i);

				NettyConnection connection = new NettyConnection(machine.getHost(), machine.getPort());
				Message rt_msg = connection.sendMessage(RaftMessageFactory.createGetLeaderMsg());
				if(rt_msg.getReqType()==MsgType.SUCCESS){
					voteNum++;
				}
			} catch (Exception e) {
				LOG.info("",e);
			}
		}
	}

	/*public boolean getLeaderAndSyncData() {
		boolean haveLeader = false;
		for (int i = 0; i < initConfig.getConnVector().size(); i++) {
			try {
				LocalStateMachine machine = initConfig.getConnVector().get(i);
				NettyConnection connection = new NettyConnection(machine.getHost(), machine.getPort());
				Message rt_msg = connection.sendMessage(RaftMessageFactory.createGetLeaderMsg());

				if(rt_msg.getReqType()==MsgType.YES_LEADER){
					data.clear();
					data = JSON.parseObject(rt_msg.getBody(), Map.class);
					haveLeader = true;
					break;
				}
			} catch (Exception e) {
				LOG.info("",e);
			}
		}
		return haveLeader;
	}*/
	public String getOnlineLeader() throws Exception {
		for (int i = 0; i < initConfig.getConnVector().size(); i++) {
			try {
				LocalStateMachine machine = initConfig.getConnVector().get(i);
				NettyConnection connection = new NettyConnection(machine.getHost(), machine.getPort());
				Message rt_msg = connection.sendMessage(RaftMessageFactory.createGetLeaderMsg());

				if(rt_msg.getReqType()==MsgType.SUCCESS){
					return new String(rt_msg.getBody());
				}
			} catch (Exception e) {
				LOG.info("",e);
			}
		}
		return null;
	}

	public boolean sendAllMachineInfo() throws Exception{
		for (int i = 0; i < initConfig.getConnVector().size(); i++) {
			LocalStateMachine machine = initConfig.getConnVector().get(i);
			NettyConnection connection = new NettyConnection(machine.getHost(), machine.getPort());
			Message rt_msg = connection.sendMessage(RaftMessageFactory.createMachineInfo(this));
			if(rt_msg.getReqType()!=MsgType.SUCCESS){
				return false;
			}
		}
		return true;
	}
	public void joinLeaderCluster(String host,int port) throws Exception {
		NettyConnection connection = new NettyConnection(host,port);
		connection.sendMessage(RaftMessageFactory.createjoinLeaderMsg(this));
	}
	/**
	 * 
	 * @param host 领导的者的ip
	 * @param port 领导者的端口
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void syncAllClusterInfo(String host, int port) throws Exception {
		NettyConnection connection = new NettyConnection(host,port);
		Message message = connection.sendMessage(RaftMessageFactory.createSyncAllClusterInfoMsg(this));
		String hostInfo = new String(message.getBody()).split(";")[0];
		initConfig.getConnVector().clear();
		for (int i = 0; i < hostInfo.split(",").length; i++) {
			String hs = hostInfo.split(",")[i].split(":")[0];
			int pt = Integer.parseInt(hostInfo.split(",")[i].split(":")[1]);
			
			LocalStateMachine machine = new LocalStateMachine(hs, pt);
			initConfig.getConnVector().add(machine);
		}
		Map<?,?> dataMap = JSON.parseObject(new String(message.getBody()).split(";")[1], Map.class);
		data.clear();
		data.putAll((Map<String, byte[]>) dataMap);
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
	public int getVoteNum() {
		return voteNum;
	}
	public void setVoteNum(int voteNum) {
		this.voteNum = voteNum;
	}
	public int getRole() {
		return role;
	}
	public void setRole(int role) {
		this.role = role;
	}

	public NettyConnection getNettyConnection() {
		return nettyConnection;
	}

	public void setNettyConnection(NettyConnection nettyConnection) {
		this.nettyConnection = nettyConnection;
	}

	public boolean isLocalhost() {
		return isLocalhost;
	}

	public void setLocalhost(boolean isLocalhost) {
		this.isLocalhost = isLocalhost;
	}

	public Map<String, byte[]> getData() {
		return data;
	}

	public void setData(Map<String, byte[]> data) {
		this.data = data;
	}

	public MachineInfo getMachineInfo() {
		return machineInfo;
	}

	public void setMachineInfo(MachineInfo machineInfo) {
		this.machineInfo = machineInfo;
	}
}