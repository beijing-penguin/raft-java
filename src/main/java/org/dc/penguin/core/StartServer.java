package org.dc.penguin.core;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MsgType;
import org.dc.penguin.core.pojo.RoleType;
import org.dc.penguin.core.raft.NodeConfig;

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
		/*//1.启动定时器
		Scanner sc = new Scanner(System.in);
		while(true) {
			String sys_inp = sc.nextLine();
			if("close".equals(sys_inp) || "end".equals(sys_inp) || "quit".equals(sys_inp)) {
				break;
			}
			if(sys_inp.startsWith("start")) {
				String[] str_arr = sys_inp.split(" ");
				if(str_arr.length==1) {
					System.out.println("请输入端口号，如start 8880 7770");
				}
				for (int i = 1; i < str_arr.length; i++) {
					//启动数据端口
					EventLoopGroup bossGroup = new NioEventLoopGroup();
					EventLoopGroup workerGroup = new NioEventLoopGroup();
					ServerBootstrap bootstrap = new ServerBootstrap();

					try {
						DataServerChannelHandler channelHandler = new DataServerChannelHandler(localMachine);
						channelHandler.setLocalMachine(localMachine);
						bootstrap.group(bossGroup,workerGroup)
						.channel(NioServerSocketChannel.class)
						.option(ChannelOption.SO_BACKLOG, 1024)
						.childHandler(channelHandler).bind(localMachine.getDataServerPort()).sync();
						System.out.println("数据通信服务开启成功，port="+localMachine.getDataServerPort());
					} catch (InterruptedException e) {
						LOG.error("",e);
						bootstrap = null;
						workerGroup.shutdownGracefully();
						bossGroup.shutdownGracefully();
					}
					//启动选举端口
					
				}
			}
		}*/
		for (NodeConfig nodeConfig: ConfigInfo.getNodeConfigList()) {
			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();
			ServerBootstrap bootstrap = new ServerBootstrap();
			int port = nodeConfig.getDataServerPort();
			System.out.print("正在启动数据通信服务端口："+port);
			try {
				bootstrap.group(bossGroup,workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 1024)
				.childHandler(new DataServerChannelHandler(nodeConfig)).bind(nodeConfig.getDataServerPort()).sync();
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
		
		for (NodeConfig nodeConfig: ConfigInfo.getNodeConfigList()) {
			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();
			ServerBootstrap bootstrap = new ServerBootstrap();
			int port = nodeConfig.getElectionServerPort();
			System.out.print("正在启动选举通信服务端口："+port);
			try {
				bootstrap.group(bossGroup,workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 1024)
				.childHandler(new ElectionServerChannelHandler(nodeConfig)).bind(nodeConfig.getElectionServerPort()).sync();
				System.out.print("开启成功!");
				
				//
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						while(true) {
							//监听集群中是否存在领导，如果不存在，则发起投票,如果存在，则改变该节点的角色id（默认启动时的角色id是候选人）
							if(nodeConfig.getRole().get() != RoleType.LEADER) {
								
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
			/*if(localMachine.isLocalhost()){
				localMachine.startDataServer();
				localMachine.startElectionServer();
			}*/
		}
	}
}
class DataServerChannelHandler extends ChannelInitializer<SocketChannel>{
	private NodeConfig nodeConfig;
	public DataServerChannelHandler(NodeConfig nodeConfig){
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
	public NodeConfig getNodeConfig() {
		return nodeConfig;
	}
	public void setLocalMachine(NodeConfig nodeConfig) {
		this.nodeConfig = nodeConfig;
	}
}
class DataServerHandler extends SimpleChannelInboundHandler<String> {
	private NodeConfig nodeConfig;
	public DataServerHandler(NodeConfig nodeConfig){
		this.nodeConfig = nodeConfig;
	}
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		System.out.println(ctx.channel().remoteAddress() + " Say : " + msg);
		Message message = JSON.parseObject(msg,Message.class);
		
		switch (message.getReqCode()) {
		case MsgType.GET_DATA:
			byte[] value = nodeConfig.getData().get(message.getKey());
			message.setValue(value);
			message.setRtnCode(MsgType.SUCCESS);
			ctx.channel().writeAndFlush(JSON.toJSONString(message)+"\n");
			break;
		case MsgType.SET_DATA:
			nodeConfig.getData().put(message.getKey(), message.getValue());
			message.setRtnCode(MsgType.SUCCESS);
			ctx.channel().writeAndFlush(JSON.toJSONString(message)+"\n");
			break;
		default:
			break;
		}
		//if(message.getMsgType())
		/*switch (message.getReqType()) {
		case MsgType.GET_LEADER:
			ConfigInfo initConfig = ConfigInfo.getInstance();
			for (int i = 0; i < initConfig.getConnVector().size(); i++) {
				if(initConfig.getConnVector().get(i).getRole() == Role.LEADER){

				}
			}
			break;
		default:
			break;
		}
		Message ms = new Message();
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

	public NodeConfig getNodeConfig() {
		return nodeConfig;
	}

	public void setLocalMachine(NodeConfig nodeConfig) {
		this.nodeConfig = nodeConfig;
	}
}

class ElectionServerChannelHandler extends ChannelInitializer<SocketChannel>{
	private NodeConfig nodeConfig;
	public ElectionServerChannelHandler(NodeConfig nodeConfig){
		this.nodeConfig = nodeConfig;
	}
	
	public NodeConfig getNodeConfig() {
		return nodeConfig;
	}

	public void setLocalMachine(NodeConfig nodeConfig) {
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