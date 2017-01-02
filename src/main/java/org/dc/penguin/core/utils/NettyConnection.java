package org.dc.penguin.core.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.dc.penguin.core.entity.Message;

import com.alibaba.fastjson.JSON;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class NettyConnection {
	private static EventLoopGroup group = new NioEventLoopGroup();
	private Bootstrap boot = new Bootstrap().group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true);
	private Message resultMessage;
	private CountDownLatch downLatch = new CountDownLatch(1);
	
	private String host;
	private int port;
	private Channel channel;
	
	public NettyConnection(String host,int port){
		this.host = host;
		this.port = port;
		initBoot();
	}
	public void initBoot(){
		boot.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.config().setAllowHalfClosure(true);
				ChannelPipeline pipeline = ch.pipeline();

				pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
				pipeline.addLast("decoder", new StringDecoder());
				pipeline.addLast("encoder", new StringEncoder());
				// 客户端的逻辑
				pipeline.addLast("handler", new SimpleChannelInboundHandler<String>() {
					@Override
					protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
						resultMessage = JSON.parseObject(msg, Message.class);
						downLatch.countDown();
					}

					@Override
					public void channelActive(ChannelHandlerContext ctx) throws Exception {
						System.out.println("Client active ");
						super.channelActive(ctx);
					}

					@Override
					public void channelInactive(ChannelHandlerContext ctx) throws Exception {
						System.out.println("Client close ");
						//ctx.channel().close();
					}
					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
						System.out.println("链接异常中断");
						ctx.close();
						//super.exceptionCaught(ctx, cause);
					}
				});
			}
		});
	}

	private Channel getChannel() throws Exception{
		if(channel==null){
			channel = boot.connect(host, port).sync().channel();
		}else if(!channel.isOpen() || !channel.isActive()){
			channel.close().sync();
			channel = null;
			channel = boot.connect(host, port).sync().channel();
		}
		return channel;
	}
	public Message sendMessage(Message msg) throws Exception{
		resultMessage = null;
		Channel channel = getChannel();
		channel.writeAndFlush(msg.toJSONString());
		downLatch.await(3, TimeUnit.SECONDS);
		return resultMessage;
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
}
