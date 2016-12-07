package org.dc.raft;

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

public class NettyClient {
	private static int port = 3000;
	private static String host = "localhost";
	private static EventLoopGroup group = new NioEventLoopGroup();
	private static Channel ch;
	public static void main(String[] args) {
		
		try{
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
			.handler(new HelloClientInitializer());
			ch = b.connect(host, port).sync().channel();
			for (int i = 0; i < 10000; i++) {
				new Thread(new Runnable() {
					
					public void run() {
						ch.writeAndFlush("你好" + "\r\n");
					}
				}).start();
			}
			
		}catch (Exception e) {
			e.printStackTrace();
			ch.close();
			group.shutdownGracefully();
		}
	}
}
class HelloClientInitializer extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();

		/*
		 * 这个地方的 必须和服务端对应上。否则无法正常解码和编码
		 * 
		 * 解码和编码 我将会在下一张为大家详细的讲解。再次暂时不做详细的描述
		 * 
		 * */
		pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
		pipeline.addLast( new StringDecoder());
		pipeline.addLast( new StringEncoder());

		// 客户端的逻辑
		pipeline.addLast(new HelloClientHandler());
	}
}
class HelloClientHandler extends SimpleChannelInboundHandler<String> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

		//System.out.println("Server say : " + msg);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Client active ");
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Client close ");
		super.channelInactive(ctx);
	}
}