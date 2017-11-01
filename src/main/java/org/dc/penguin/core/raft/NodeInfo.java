package org.dc.penguin.core.raft;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.pojo.RoleType;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * 本机器配置信息
 * @author DC
 */
public class NodeInfo {
	private static Log LOG = LogFactory.getLog(NodeInfo.class);
	private String host;
	private int dataServerPort;
	private int electionServerPort;
	private AtomicInteger haveVoteNum = new AtomicInteger(1);//
	private boolean isLocalhost;
	private int role = RoleType.FOLLOWER;//当前身份
	private AtomicInteger leaderPingNum = new AtomicInteger(0);;
	private AtomicInteger voteTotalNum = new AtomicInteger(0);
	
	
	public AtomicInteger getVoteTotalNum() {
		return voteTotalNum;
	}
	public void setVoteTotalNum(AtomicInteger voteTotalNum) {
		this.voteTotalNum = voteTotalNum;
	}
	/**
	 * 想所有人发起投票
	 * @throws Exception
	 *//*
	public void sendPollInvitation() throws Exception {
		for (int i = 0; i < ConfigInfo.getConnVector().size(); i++) {
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
	}*/

	public void startDataServer(){
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();

		try {
			bootstrap.group(bossGroup,workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 1024)
			.childHandler(new DataServerChannelHandler()).bind(dataServerPort).sync();
			System.out.println("数据通信服务开启成功，port="+dataServerPort);
		} catch (InterruptedException e) {
			LOG.error("",e);
			bootstrap = null;
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
	public void startElectionServer() throws Exception{
		if(isLocalhost) {

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();
			ServerBootstrap bootstrap = new ServerBootstrap();

			try {
				bootstrap.group(bossGroup,workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 1024)
				.childHandler(new ElectionServerChannelHandler(this)).bind(electionServerPort).sync().channel();
				System.out.println("选举通信服务开启成功，port="+electionServerPort);

				//开启定时器
				new Thread(new Runnable() {

					@Override
					public void run() {
						while(true) {
							
						}
					}
				}).start();
				/*//向其他人询问是否存在leader，所谓其他人就是端口不等于当前端口，或者ip不等于本地的人。
			for (LocalMachine localStateMachine : ConfigInfo.getMachineVector()) {
				if((localStateMachine.isLocalhost==true&&port!=localStateMachine.getElectionServerPort())||localStateMachine.isLocalhost==false){
					//获取领导请求，发送getleader请求
					try {
						NettyConnection conn = new NettyConnection(localStateMachine.getHost(), localStateMachine.getElectionServerPort());
						Message message = conn.sendMessage(new Message(MsgType.GET_LEADER));
					} catch (Exception e) {
						e.printStackTrace();
					}
					//向其他ip同伴发送获取领导的协议请求
					NettyConnection conn = new NettyConnection(localStateMachine.getHost(), localStateMachine.getPort());
					try {
						conn.sendData(MsgType.GET_LEADER);
					} catch (Exception e) {
						e.printStackTrace();
					}finally{
						conn.close();
					}
				}
			}*/

			} catch (InterruptedException e) {
				LOG.error("",e);
				bootstrap = null;
				workerGroup.shutdownGracefully();
				bossGroup.shutdownGracefully();
			}
		}
	}
	/*public String getOnlineLeader() throws Exception {
		for (LocalMachine machine : ConfigInfo.getMachineVector()) {
			try {
				NettyConnection connection = new NettyConnection(machine.getHost(), machine.getElectionServerPort());
				Message arg_msg = new Message(MsgType.GET_LEADER);
				arg_msg.setBody((machine.getHost()+":"+machine.getElectionServerPort()).getBytes());
				Message rt_msg = connection.sendMessage(arg_msg);

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
	public int getDataServerPort() {
		return dataServerPort;
	}
	public void setDataServerPort(int dataServerPort) {
		this.dataServerPort = dataServerPort;
	}

	public boolean isLocalhost() {
		return isLocalhost;
	}

	public void setLocalhost(boolean isLocalhost) {
		this.isLocalhost = isLocalhost;
	}

	public AtomicInteger getHaveVoteNum() {
		return haveVoteNum;
	}
	public void setHaveVoteNum(AtomicInteger haveVoteNum) {
		this.haveVoteNum = haveVoteNum;
	}
	public int getElectionServerPort() {
		return electionServerPort;
	}
	public void setElectionServerPort(int electionServerPort) {
		this.electionServerPort = electionServerPort;
	}
	public int getRole() {
		return role;
	}
	public void setRole(int role) {
		this.role = role;
	}
	public AtomicInteger getLeaderPingNum() {
		return leaderPingNum;
	}
	public void setLeaderPingNum(AtomicInteger leaderPingNum) {
		this.leaderPingNum = leaderPingNum;
	}
	
}


class ElectionServerChannelHandler extends ChannelInitializer<SocketChannel>{
	NodeInfo nodeConfig;
	public ElectionServerChannelHandler(NodeInfo nodeConfig) {
		this.nodeConfig = nodeConfig;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.config().setAllowHalfClosure(true);
		ChannelPipeline pipeline = ch.pipeline();

		//IdleStateHandler 与客户端链接后，根据超出配置的时间自动触发userEventTriggered
		//readerIdleTime服务端长时间没有读到数据，则为读空闲，触发读空闲监听，并自动关闭链路连接，周期性按readerIdleTime的超时间触发空闲监听方法
		//writerIdleTime服务端长时间没有发送写请求，则为空闲，触发写空闲监听,空闲期间，周期性按writerIdleTime的超时间触发空闲监听方法
		//allIdleTime 服务端在allIdleTime时间内未接收到客户端消息，或者，也未去向客户端发送消息，则触发周期性操作
		pipeline.addLast("ping", new IdleStateHandler(10, 20, 35, TimeUnit.SECONDS));
		// 以("\n")为结尾分割的 解码器
		pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
		// 字符串解码 和 编码
		pipeline.addLast("decoder", new StringDecoder());
		pipeline.addLast("encoder", new StringEncoder());
		// 自己的逻辑Handler
		pipeline.addLast("handler", new ElectionServerHandler());
	}

}
class ElectionServerHandler extends SimpleChannelInboundHandler<String> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		/*	System.out.println(ctx.channel().remoteAddress() + " Say : " + msg);
		Message message = JSON.parseObject(msg,Message.class);
		switch (message.getReqType()) {
		case MsgType.GET_LEADER:
			ConfigInfo initConfig = ConfigInfo.getInstance();
			for (int i = 0; i < initConfig.getConnVector().size(); i++) {
				if(initConfig.getConnVector().get(i).getRole() == Role.LEADER){

				}
			}
			break;
		default:
			break;
		}*/
		/*Message ms = new Message();
		ms.setReqType(MsgType.YES_LEADER);
		ctx.channel().writeAndFlush(JSON.toJSONString(ms)+"\n");
		if (!"OK".equals(msg)) {
			//业务逻辑
		}*/
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state().equals(IdleState.READER_IDLE)) {
				System.out.println("READER_IDLE");
			} else if (event.state().equals(IdleState.WRITER_IDLE)) {
				System.out.println("WRITER_IDLE");
			} else if (event.state().equals(IdleState.ALL_IDLE)) {
				System.out.println("ALL_IDLE");
				// 发送心跳
				// ctx.channel().writeAndFlush("ping\n");
			}
		}
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("链接异常中断");
		ctx.close();
	}

}


class DataServerChannelHandler extends ChannelInitializer<SocketChannel>{

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.config().setAllowHalfClosure(true);
		ChannelPipeline pipeline = ch.pipeline();

		//IdleStateHandler 与客户端链接后，根据超出配置的时间自动触发userEventTriggered
		//readerIdleTime服务端长时间没有读到数据，则为读空闲，触发读空闲监听，并自动关闭链路连接，周期性按readerIdleTime的超时间触发空闲监听方法
		//writerIdleTime服务端长时间没有发送写请求，则为空闲，触发写空闲监听,空闲期间，周期性按writerIdleTime的超时间触发空闲监听方法
		//allIdleTime 服务端在allIdleTime时间内未接收到客户端消息，或者，也未去向客户端发送消息，则触发周期性操作
		pipeline.addLast("ping", new IdleStateHandler(10, 20, 35, TimeUnit.SECONDS));
		// 以("\n")为结尾分割的 解码器
		pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
		// 字符串解码 和 编码
		pipeline.addLast("decoder", new StringDecoder());
		pipeline.addLast("encoder", new StringEncoder());
		// 自己的逻辑Handler
		pipeline.addLast("handler", new DataServerHandler());
	}

}
class DataServerHandler extends SimpleChannelInboundHandler<String> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		/*	System.out.println(ctx.channel().remoteAddress() + " Say : " + msg);
		Message message = JSON.parseObject(msg,Message.class);
		switch (message.getReqType()) {
		case MsgType.GET_LEADER:
			ConfigInfo initConfig = ConfigInfo.getInstance();
			for (int i = 0; i < initConfig.getConnVector().size(); i++) {
				if(initConfig.getConnVector().get(i).getRole() == Role.LEADER){

				}
			}
			break;
		default:
			break;
		}*/
		/*Message ms = new Message();
		ms.setReqType(MsgType.YES_LEADER);
		ctx.channel().writeAndFlush(JSON.toJSONString(ms)+"\n");
		if (!"OK".equals(msg)) {
			//业务逻辑
		}*/
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state().equals(IdleState.READER_IDLE)) {
				System.out.println("READER_IDLE");
			} else if (event.state().equals(IdleState.WRITER_IDLE)) {
				System.out.println("WRITER_IDLE");
			} else if (event.state().equals(IdleState.ALL_IDLE)) {
				System.out.println("ALL_IDLE");
				// 发送心跳
				// ctx.channel().writeAndFlush("ping\n");
			}
		}
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("链接异常中断");
		ctx.close();
	}

}