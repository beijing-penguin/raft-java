package org.dc.penguin.core;

import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MsgType;
import org.dc.penguin.core.pojo.MsgType1;
import org.dc.penguin.core.raft.LocalMachine;

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
		final Vector<LocalMachine> machineList = ConfigInfo.getMachineVector();
		for (LocalMachine localMachine: ConfigInfo.getMachineVector()) {
			if(localMachine.isLocalhost()){
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
			}
		}
		for (int i = 0; i < machineList.size(); i++) {
			final LocalMachine localMachine = machineList.get(i);
			if(localMachine.isLocalhost()){
				EventLoopGroup bossGroup = new NioEventLoopGroup();
				EventLoopGroup workerGroup = new NioEventLoopGroup();
				ServerBootstrap bootstrap = new ServerBootstrap();

				try {
					bootstrap.group(bossGroup,workerGroup)
					.channel(NioServerSocketChannel.class)
					.option(ChannelOption.SO_BACKLOG, 1024)
					.childHandler(new ElectionServerChannelHandler()).bind(localMachine.getElectionServerPort()).sync();
					System.out.println("数据通信服务开启成功，port="+localMachine.getElectionServerPort());
				} catch (InterruptedException e) {
					LOG.error("",e);
					bootstrap = null;
					workerGroup.shutdownGracefully();
					bossGroup.shutdownGracefully();
				}
			}
		}
	}
}
class DataServerChannelHandler extends ChannelInitializer<SocketChannel>{
	public DataServerChannelHandler(LocalMachine localMachine){
		this.localMachine = localMachine;
	}
	private LocalMachine localMachine;
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
		pipeline.addLast("handler", new DataServerHandler(localMachine));
	}
	public LocalMachine getLocalMachine() {
		return localMachine;
	}
	public void setLocalMachine(LocalMachine localMachine) {
		this.localMachine = localMachine;
	}
}
class DataServerHandler extends SimpleChannelInboundHandler<String> {
	public DataServerHandler(LocalMachine localMachine){
		this.localMachine = localMachine;
	}
	private LocalMachine localMachine;
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		System.out.println(ctx.channel().remoteAddress() + " Say : " + msg);
		Message message = JSON.parseObject(msg,Message.class);
		
		switch (message.getReqCode()) {
		case MsgType.GET_DATA:
			byte[] value = localMachine.getData().get(message.getKey());
			message.setValue(value);
			message.setRtnCode(MsgType.SUCCESS);
			ctx.channel().writeAndFlush(JSON.toJSONString(message)+"\n");
			break;
		case MsgType.SET_DATA:
			localMachine.getData().put(message.getKey(), message.getValue());
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

	public LocalMachine getLocalMachine() {
		return localMachine;
	}

	public void setLocalMachine(LocalMachine localMachine) {
		this.localMachine = localMachine;
	}
}

class ElectionServerChannelHandler extends ChannelInitializer<SocketChannel>{

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