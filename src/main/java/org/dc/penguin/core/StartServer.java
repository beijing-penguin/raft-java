package org.dc.penguin.core;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MsgType;
import org.dc.penguin.core.pojo.RoleType;
import org.dc.penguin.core.raft.NodeInfo;
import org.dc.penguin.core.utils.NodeUtils;

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
		for (NodeInfo nodeInfo: ConfigInfo.getNodeConfigList()) {
			if(nodeInfo.isLocalhost()) {
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

		for (NodeInfo nodeInfo: ConfigInfo.getNodeConfigList()) {
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
									}else if(nodeInfo.getLeaderPingNum().get()>leaderPingNum){
										leaderPingNum = nodeInfo.getLeaderPingNum().get();
									}else {
										while(true) {
											System.out.println(nodeInfo.getRole()+"-"+nodeInfo.getLeaderPingNum().get()+"-"+leaderPingNum);
											nodeInfo.getHaveVoteNum().set(1);//当前节点没有leaderPing，则让该节点具备投票权。

											int leaderPingNum2 = nodeInfo.getLeaderPingNum().get();
											LOG.info(nodeInfo.getLeaderPingNum().get()+"-"+JSON.toJSONString(nodeInfo)+"发起vote");
											//优先投自己一票
											nodeInfo.getVoteTotalNum().incrementAndGet();
											nodeInfo.getHaveVoteNum().incrementAndGet();
											//...//
											//System.out.println(nodeInfo.getRole()+"-"+nodeInfo.getLeaderPingNum()+"-"+leaderPingNum);
											NodeUtils.sendVote(nodeInfo);
											Thread.sleep(3000);//3秒后获取投票结果
											LOG.info(nodeInfo.getHost()+"投票结果voteTotalNum="+nodeInfo.getVoteTotalNum());
											if(nodeInfo.getLeaderPingNum().get()>leaderPingNum2) {//已经存在leader
												nodeInfo.getVoteTotalNum().set(0);
												break;
											}
											if(nodeInfo.getVoteTotalNum().get()>ConfigInfo.getNodeConfigList().size()/2 ) {
												nodeInfo.setRole(RoleType.LEADER);
												NodeUtils.sendLeaderPing(nodeInfo);
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
	private NodeInfo nodeConfig;
	public DataServerChannelHandler(NodeInfo nodeConfig){
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
		pipeline.addLast("handler", new DataServerHandler(nodeConfig));
	}
	public NodeInfo getNodeConfig() {
		return nodeConfig;
	}
	public void setLocalMachine(NodeInfo nodeConfig) {
		this.nodeConfig = nodeConfig;
	}
}
class DataServerHandler extends SimpleChannelInboundHandler<String> {
	private NodeInfo nodeInfo;
	public DataServerHandler(NodeInfo nodeInfo){
		this.nodeInfo = nodeInfo;
	}
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		System.out.println(ctx.channel().remoteAddress() + " Say : " + msg);
		Message message = JSON.parseObject(msg,Message.class);

		switch (message.getMsgCode()) {
		case MsgType.GET_DATA:
			/*byte[] value = nodeConfig.getData().get(message.getKey());
			message.setValue(value);
			message.setRtnCode(MsgType.SUCCESS);
			ctx.channel().writeAndFlush(JSON.toJSONString(message)+"\n");*/
			break;
		case MsgType.SET_DATA:
			/*nodeConfig.getData().put(message.getKey(), message.getValue());
			message.setRtnCode(MsgType.SUCCESS);
			ctx.channel().writeAndFlush(JSON.toJSONString(message)+"\n");*/
			break;
		default:
			break;
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
			switch (message.getMsgCode()) {
			case MsgType.VOTE:
				if(nodeInfo.getRole()!=RoleType.LEADER && nodeInfo.getHaveVoteNum().incrementAndGet()==2) {
					ctx.channel().writeAndFlush(message.toJSONString());
				}
				break;
			case MsgType.LEADER_PING:
				nodeInfo.getLeaderPingNum().incrementAndGet();
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

}