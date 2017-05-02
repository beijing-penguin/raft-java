package org.dc.penguin.core.raft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.ConfigInfo;
import org.dc.penguin.core.entity.Message;
import org.dc.penguin.core.entity.MsgType;
import org.dc.penguin.core.utils.NettyConnection;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 本地状态机
 * @author DC
 *
 */
public class LocalStateMachine {
	private static Log LOG = LogFactory.getLog(LocalStateMachine.class);
	private static ConfigInfo initConfig = ConfigInfo.getInstance();

	private int role = 0;
	private Map<String,byte[]> data = new ConcurrentHashMap<String,byte[]>();
	private String host;
	private int port;
	private int voteNum;//票数
	private boolean isLocalhost;

	/**
	 * 想所有人发起投票
	 * @throws Exception
	 */
	public void sendPollInvitation() throws Exception {
		for (int i = 0; i < initConfig.getConnVector().size(); i++) {
			try {
				LocalStateMachine machine = initConfig.getConnVector().get(i);
				NettyConnection connection = new NettyConnection(machine.getHost(), machine.getPort());
				Message rt_msg = connection.sendMessage(new Message(MsgType.POLL_ME));
				if(rt_msg.getReqType()==MsgType.SUCCESS){
					//machine.setVoteNum(machine.);
				}
			} catch (Exception e) {
				LOG.info("",e);
			}
		}
	}
	
	public void startDataServer(int port){
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();
		
		try {
			bootstrap.group(bossGroup,workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 1024)
			.childHandler(new RaftServerChannelHandler()).bind(port).sync();
			System.out.println("数据通信服务开启成功，port="+port);
		} catch (InterruptedException e) {
			LOG.error("",e);
			bootstrap = null;
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
	public void startElectionServer(int port){
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();
		
		try {
			bootstrap.group(bossGroup,workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 1024)
			.childHandler(new RaftServerChannelHandler()).bind(port).sync();
			System.out.println("选举服务开启成功，port="+port);
			
			//向其他人询问是否存在leader，所谓其他人就是端口不等于当前端口，或者ip不等于本地的人。
			
			
		} catch (InterruptedException e) {
			LOG.error("",e);
			bootstrap = null;
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
	/*public String getOnlineLeader() throws Exception {
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
	}*/

	/*public void joinLeaderCluster(String host,int port) throws Exception {
		NettyConnection connection = new NettyConnection(host,port);
		connection.sendMessage(RaftMessageFactory.createjoinLeaderMsg(this));
	}*/
	/**
	 * 
	 * @param host 领导的者的ip
	 * @param port 领导者的端口
	 * @throws Exception
	 */
	/*@SuppressWarnings("unchecked")
	public void syncAllClusterInfoFromLeader(String host, int port) throws Exception {
		NettyConnection connection = new NettyConnection(host,port);
		Message message = connection.sendMessage(RaftMessageFactory.createSyncAllClusterInfoMsg(this));
		String hostInfo = new String(message.getBody()).split(";")[0];
		initConfig.getConnVector().clear();
		for (int i = 0; i < hostInfo.split(",").length; i++) {
			String hs = hostInfo.split(",")[i].split(":")[0];
			int pt = Integer.parseInt(hostInfo.split(",")[i].split(":")[1]);

			LocalStateMachine machine = new LocalStateMachine();
			machine.setHost(hs);
			machine.setPort(pt);
			initConfig.getConnVector().add(machine);
		}
		Map<?,?> dataMap = JSON.parseObject(new String(message.getBody()).split(";")[1], Map.class);
		data.clear();
		data.putAll((Map<String, byte[]>) dataMap);
	}*/
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
}