package org.dc.penguin.core;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MsgType;
import org.dc.penguin.core.pojo.RoleType;
import org.dc.penguin.core.raft.NodeInfo;
import org.dc.penguin.core.utils.NodeUtils;
import org.dc.penguin.core.utils.SocketCilentUtils;
import org.dc.penguin.core.utils.SocketConnection;
import org.dc.penguin.core.utils.SocketPool;

import com.alibaba.fastjson.JSON;

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

public class StartServer {
	private static Log LOG = LogFactory.getLog(StartServer.class);
	public static void main(String[] args) throws Exception {
		for (NodeInfo nodeInfo: NodeConfigInfo.getNodeConfigList()) {
			if(nodeInfo.isLocalhost()) {
				NodeConfigInfo.initConfig(nodeInfo);
				//初始化本地节点nodeInfo中的term和dataIndex信息。
				NodeUtils.initNodeInfo(nodeInfo);
				
				EventLoopGroup bossGroup = new NioEventLoopGroup();
				EventLoopGroup workerGroup = new NioEventLoopGroup();
				ServerBootstrap bootstrap = new ServerBootstrap();
				int port = nodeInfo.getDataServerPort();
				System.out.print("正在启动数据通信服务端口："+port);
				try {
					bootstrap.group(bossGroup,workerGroup)
					.channel(NioServerSocketChannel.class)
					.option(ChannelOption.SO_BACKLOG, 1024)
					.childHandler(new DataServerChannelHandler(nodeInfo)).bind(nodeInfo.getDataServerPort()).sync();
					System.out.print("开启成功!");
				} catch (Exception e) {
					System.out.print("开启失败!");
					LOG.error("",e);
					bootstrap = null;
					workerGroup.shutdownGracefully();
					bossGroup.shutdownGracefully();
				}
				System.out.println();
			}
		}

		for (NodeInfo nodeInfo: NodeConfigInfo.getNodeConfigList()) {
			if(nodeInfo.isLocalhost()) {
				EventLoopGroup bossGroup = new NioEventLoopGroup();
				EventLoopGroup workerGroup = new NioEventLoopGroup();
				ServerBootstrap bootstrap = new ServerBootstrap();
				int port = nodeInfo.getElectionServerPort();
				System.out.print("正在启动选举通信服务端口："+port);
				try {
					bootstrap.group(bossGroup,workerGroup)
					.channel(NioServerSocketChannel.class)
					.option(ChannelOption.SO_BACKLOG, 1024)
					.childHandler(new ElectionServerChannelHandler(nodeInfo)).bind(nodeInfo.getElectionServerPort()).sync();
					System.out.print("开启成功!");

					//何时发起选举定时器
					new Thread(new Runnable() {
						@Override
						public void run() {
							int leaderPingNum = 0;
							while(true) {
								try {
									Thread.sleep(5000);//选举超时5秒
									if(nodeInfo.getRole()==RoleType.LEADER) {
										continue;
									}else if(nodeInfo.getLeaderPingNum().intValue()>leaderPingNum){
										leaderPingNum = nodeInfo.getLeaderPingNum().intValue();
									}else {
										while(true) {
											//根据日志记录的最终数据，确定该节点所具备统治能力（权利power），即设置最大任期号和最大数据索引
											NodeUtils.initNodeInfo(nodeInfo);
											System.out.println(nodeInfo.getRole()+"-"+nodeInfo.getLeaderPingNum().intValue()+"-"+leaderPingNum);
											nodeInfo.getHaveVoteNum().set(1);//当前节点没有leaderPing，则让该节点具备投票权。
											nodeInfo.setLeaderKey(null);//设置该节点无leaderKey

											int leaderPingNum2 = nodeInfo.getLeaderPingNum().intValue();
											LOG.info(nodeInfo.getLeaderPingNum().intValue()+"-"+JSON.toJSONString(nodeInfo)+"发起vote");
											//优先投自己一票
											nodeInfo.getVoteTotalNum().incrementAndGet();
											nodeInfo.getHaveVoteNum().incrementAndGet();
											//注释下面这行代码，是取消任期号加1重新选举策略，转而改用采用二分之一选举策略，所以不适用本地任期号+1策略，作为重新选举的首要条件，
											//原因是一次投票，二分之一策略下必定会产生有且只有一个领导，如果没有产生领导，则说明，集群宕机服务器过多，应该加入新的服务器，
											//或者修复未联通的服务器
											//nodeInfo.getTerm().incrementAndGet();
											
											NodeUtils.sendVote(nodeInfo);//向所有其他服务器发起投票申请，其他服务器接受到邀请后，是否同意的条件是数据log中的任期号大于该node，和数据log中数据索引大于等于该node
											Thread.sleep(3000);//3秒后获取投票结果
											LOG.info(nodeInfo.getHost()+"投票结果voteTotalNum="+nodeInfo.getVoteTotalNum());
											if(nodeInfo.getLeaderPingNum().intValue()>leaderPingNum2) {//已经存在leader
												nodeInfo.getVoteTotalNum().set(0);
												break;
											}
											if(nodeInfo.getVoteTotalNum().get()>NodeConfigInfo.getNodeConfigList().size()/2 ) {
												LOG.info("选举成功...");
												nodeInfo.setRole(RoleType.LEADER);
												nodeInfo.getTerm().incrementAndGet();
												nodeInfo.setLeaderKey(NodeUtils.createLeaderKey(nodeInfo));
												NodeUtils.sendLeaderPing(nodeInfo);
												//开始日志同步
												NodeUtils.logSync(nodeInfo);
												
												nodeInfo.getVoteTotalNum().set(0);
												break;
											}
											LOG.info("选举失败...");
											nodeInfo.getHaveVoteNum().set(1);
											nodeInfo.getVoteTotalNum().set(0);
											Thread.sleep(new Random().nextInt(10)*1000);//随机沉睡数秒后发起选举请求
										}
									}

								} catch (Exception e) {
									LOG.error("",e);
								}
							}
						}
					}).start();

					//领导ping定时器
					new Thread(new Runnable() {
						@Override
						public void run() {
							while(true) {
								try {
									Thread.sleep(2000);
									if(nodeInfo.getRole()==RoleType.LEADER) {
										NodeUtils.sendLeaderPing(nodeInfo);
									}
								} catch (Exception e) {
									LOG.error("",e);
								}
							}
						}
					}).start();
				} catch (Exception e) {
					System.out.print("开启失败!");
					LOG.error("",e);
					bootstrap = null;
					workerGroup.shutdownGracefully();
					bossGroup.shutdownGracefully();
				}
				System.out.println();
			}
		}
	}
}
class DataServerChannelHandler extends ChannelInitializer<SocketChannel>{
	private NodeInfo nodeInfo;
	public DataServerChannelHandler(NodeInfo nodeInfo){
		this.nodeInfo = nodeInfo;
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
		pipeline.addLast("handler", new DataServerHandler(nodeInfo));
	}
}
class DataServerHandler extends SimpleChannelInboundHandler<String> {
	private static Log LOG = LogFactory.getLog(DataServerHandler.class);
	private NodeInfo nodeInfo;
	public DataServerHandler(NodeInfo nodeInfo){
		this.nodeInfo = nodeInfo;
	}
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) {
		try {
			System.out.println(ctx.channel().remoteAddress() + " Say : " + msg);
			Message message = JSON.parseObject(msg,Message.class);

			switch (message.getMsgCode()) {
			case MsgType.GET_DATA:
				/*byte[] value = nodeConfig.getData().get(message.getKey());
			message.setValue(value);
			message.setRtnCode(MsgType.SUCCESS);
			ctx.channel().writeAndFlush(JSON.toJSONString(message)+"\n");*/
				ctx.channel().writeAndFlush(message.toJSONString());
				break;
			case MsgType.LEADER_SET_DATA:
				Files.write(Paths.get(NodeConfigInfo.dataLogDir), (msg+"\n").getBytes(),StandardOpenOption.APPEND);
				
				Message msss = new Message();
				msss.setMsgCode(MsgType.SUCCESS);
				ctx.channel().writeAndFlush(msss.toJSONString());
				break;
			case MsgType.CLIENT_SET_DATA:
				//确认身份
				if(nodeInfo.getRole()==RoleType.LEADER && nodeInfo.getHost().equals(message.getLeaderKey().split(":")[0]) && nodeInfo.getDataServerPort()==Integer.parseInt(message.getLeaderKey().split(":")[1]) && Integer.parseInt(message.getLeaderKey().split(":")[3])==nodeInfo.getTerm().get()) {
					//先保存在自己的log中，然后通知其他follower
					message.setLeaderKey(NodeUtils.createLeaderKeyByWriteLog(nodeInfo));
					Files.write(Paths.get(NodeConfigInfo.dataLogDir), message.toJSONString().getBytes(),StandardOpenOption.APPEND);
					message.setMsgCode(MsgType.LEADER_SET_DATA);
					
					int size = NodeConfigInfo.getNodeConfigList().size();
					CountDownLatch cdl = new CountDownLatch(size/2+size%2);
					for(NodeInfo node : NodeConfigInfo.getNodeConfigList()) {
						if(!node.getHost().equals(nodeInfo.getHost())) {
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										SocketPool pool = SocketCilentUtils.getSocketPool(node.getHost(), node.getDataServerPort());
										SocketConnection conn = pool.getSocketConnection();
										Message ms = JSON.parseObject(conn.sendMessage(message.toJSONString()), Message.class);
										if(ms.getMsgCode()==MsgType.SUCCESS) {
											cdl.countDown();
										}
									} catch (Exception e) {
										LOG.error("",e);
									}
								}
							}).start();
						}
					}
					cdl.await(5,TimeUnit.SECONDS);

					if(cdl.getCount()==0) {
						Message mssss = new Message();
						mssss.setMsgCode(MsgType.SUCCESS);
						ctx.channel().writeAndFlush(mssss.toJSONString());
					}else {
						Message mssss = new Message();
						mssss.setMsgCode(MsgType.FAIL);
						ctx.channel().writeAndFlush(mssss.toJSONString());
					}
				}else {
					Message mssss = new Message();
					mssss.setMsgCode(MsgType.NO_LEADER);
					ctx.channel().writeAndFlush(mssss.toJSONString());
				}
				break;
			default:
				Message mssss = new Message();
				mssss.setMsgCode(MsgType.FAIL);
				ctx.channel().writeAndFlush(mssss.toJSONString());
				break;
			}
		}catch (Exception e) {
			LOG.error("",e);
			Message ms =new Message();
			ms.setMsgCode(MsgType.FAIL);
			ctx.channel().writeAndFlush(ms.toJSONString());
		}
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
		System.out.println("链接异常中断:"+cause.getStackTrace());
		ctx.close();
	}

	public NodeInfo getNodeInfo() {
		return nodeInfo;
	}
}

class ElectionServerChannelHandler extends ChannelInitializer<SocketChannel>{
	private NodeInfo nodeInfo;
	public ElectionServerChannelHandler(NodeInfo nodeInfo){
		this.nodeInfo = nodeInfo;
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
		pipeline.addLast("handler", new ElectionServerHandler(nodeInfo));
	}

}
class ElectionServerHandler extends SimpleChannelInboundHandler<String> {
	private static Log LOG = LogFactory.getLog(StartServer.class);

	private NodeInfo nodeInfo;
	public ElectionServerHandler(NodeInfo nodeInfo){
		this.nodeInfo = nodeInfo;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) {
		try {
			Message message = JSON.parseObject(msg,Message.class);
			NodeInfo reqNode = JSON.parseObject(message.getValue(), NodeInfo.class);
			switch (message.getMsgCode()) {
			case MsgType.VOTE:
				NodeUtils.initNodeInfo(nodeInfo);//确定该节点的最大统治能力
				if(nodeInfo.getRole()!=RoleType.LEADER && reqNode.getTerm().get()>=nodeInfo.getTerm().get() && reqNode.getDataIndex().get()>=nodeInfo.getDataIndex().get() && nodeInfo.getHaveVoteNum().incrementAndGet()==2) {
					ctx.channel().writeAndFlush(message.toJSONString());
				}
				break;
			case MsgType.LEADER_PING:
				//大于二分之一选举策略，不可能存在有多个领导的冲突问题。
				/*if(nodeInfo.getRole()==RoleType.LEADER) {
					if(!reqNode.getHost().equals(nodeInfo.getHost()) && nodeInfo.getTerm().get()<=reqNode.getTerm().get() ) {
						nodeInfo.setLeaderKey(NodeUtils.createLeaderKey(reqNode));
						nodeInfo.setRole(RoleType.FOLLOWER);
					}
				}else {
					for (NodeInfo node_config : NodeConfigInfo.getNodeConfigList()) {//更新本节点内存中leaderNode的信息。
						if(node_config.getHost().equals(reqNode.getHost()) && node_config.getElectionServerPort() == reqNode.getElectionServerPort()) {
							node_config.setRole(RoleType.LEADER);
							node_config.setLeaderKey(reqNode.getLeaderKey());
							node_config.setTerm(reqNode.getTerm());
						}
					}
					nodeInfo.setLeaderKey(NodeUtils.createLeaderKey(reqNode));
				}
				if(nodeInfo.getLeaderKey()==null) {
					nodeInfo.setLeaderKey(reqNode.getLeaderKey());
				}else {
					if(reqNode.getTerm().get()>=Integer.parseInt(nodeInfo.getLeaderKey().split(":")[3]) && reqNode.getDataIndex().get()>=Integer.parseInt(nodeInfo.getLeaderKey().split(":")[4])) {
						nodeInfo.setLeaderKey(reqNode.getLeaderKey());
					}
				}*/
				
				for (NodeInfo node_config : NodeConfigInfo.getNodeConfigList()) {//更新本节点内存中leaderNode的信息。
					if(node_config.getHost().equals(reqNode.getHost()) && node_config.getElectionServerPort() == reqNode.getElectionServerPort()) {
						node_config.setRole(RoleType.LEADER);
						node_config.setLeaderKey(reqNode.getLeaderKey());
						node_config.setTerm(reqNode.getTerm());
					}
				}
				nodeInfo.setLeaderKey(reqNode.getLeaderKey());
				nodeInfo.setLeaderPingNum(nodeInfo.getLeaderPingNum().add(new BigInteger("1")));
				break;
			case MsgType.GET_DATA_POS:
				NodeUtils.initNodeInfo(nodeInfo);
				String rt_data = nodeInfo.getTerm().get()+":"+nodeInfo.getDataIndex().get()+"\n";
				ctx.channel().writeAndFlush(rt_data);
				break;
			default:
				break;
			}
		}catch (Exception e) {
			ctx.close();
			LOG.error("",e);
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state().equals(IdleState.READER_IDLE)) {
				//System.out.println("READER_IDLE");
			} else if (event.state().equals(IdleState.WRITER_IDLE)) {
				//System.out.println("WRITER_IDLE");
			} else if (event.state().equals(IdleState.ALL_IDLE)) {
				// System.out.println("ALL_IDLE");
				// 发送心跳
				// ctx.channel().writeAndFlush("ping\n");
			}
		}
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOG.info("链接异常中断:"+cause.getStackTrace());
		ctx.close();
	}
	/*public static void main(String[] args) throws Exception {
		ConfigManager.getInstance().loadProps("config.properties");
		Files.write(Paths.get(ConfigManager.getInstance().get("config.properties", "dataLogDir")), "段感受到asd".getBytes(),StandardOpenOption.APPEND);
	}*/
}