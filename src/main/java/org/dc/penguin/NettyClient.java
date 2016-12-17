package org.dc.penguin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.entity.Message;
import org.dc.penguin.entity.ReqType;
import org.dc.penguin.entity.ServerInfo;
import org.dc.penguin.entity.ServerRole;

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

public class NettyClient {
	public ThreadLocal<String> RESULT = new ThreadLocal<String>();
	private static Log LOG = LogFactory.getLog(NettyClient.class);
	private EventLoopGroup group = new NioEventLoopGroup();
	private Bootstrap boot = new Bootstrap();

	private Channel leaderChannel;

	private Map<Channel,ServerInfo> channelMap = new HashMap<Channel,ServerInfo>();

	public NettyClient(String...hostAndPort){

		try{
			for (int i = 0; i < hostAndPort.length; i++) {
				boot.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
				.handler(new ChannelInitializer<SocketChannel>() {
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
								RESULT.set(JSON.parseObject(msg, Message.class).getBody());
							}

							@Override
							public void channelActive(ChannelHandlerContext ctx) throws Exception {
								System.out.println("Client active ");
								super.channelActive(ctx);
							}

							@Override
							public void channelInactive(ChannelHandlerContext ctx) throws Exception {
								System.out.println("Client close ");
								ctx.channel().close();
								//super.channelInactive(ctx);
							}
						});
					}
				});

				String host = hostAndPort[i].split(":")[0];
				int port = Integer.parseInt(hostAndPort[i].split(":")[1]);

				Channel channel = boot.connect(host,Integer.parseInt(hostAndPort[i].split(":")[1])).sync().channel();

				ServerInfo serverInfo = new ServerInfo();
				serverInfo.setHost(host);
				serverInfo.setPort(port);
				serverInfo.setFirstConnGood(true);
				channelMap.put(channel, serverInfo);

				getLeaderChannel(channelMap);
			}
		}catch (Exception e) {
			LOG.info("",e);
		}
	}

	//选举
	public void election(){

	}
	public String sendRequest(Object message){
		RESULT.remove();
		leaderChannel.writeAndFlush(JSON.toJSONString(message)+"\n");
		return RESULT.get();
	}
	private Channel getLeaderChannel(Map<Channel,ServerInfo> channelMap) throws Exception{
		while(true){
			for (Channel channel : channelMap.keySet()) {
				Message msg = new Message();
				msg.setReqType(ReqType.GET_LEADER.getRequestType());
				ServerInfo serverInfo = JSON.parseObject(sendRequest(msg), ServerInfo.class);
				if(serverInfo.getRole() == ServerRole.LEADER){
					leaderChannel = channel;
					break;
				}
			}
			Thread.sleep(3000);
		}
	}
	public static void main(String[] args) {
		Message msg = new Message();
		msg.setReqType(ReqType.RAFT_PING.getRequestType());
		System.out.println(JSON.toJSONString(msg));
	}
}
class ClientInitializer extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.config().setAllowHalfClosure(true);
		ChannelPipeline pipeline = ch.pipeline();

		pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
		pipeline.addLast("decoder", new StringDecoder());
		pipeline.addLast("encoder", new StringEncoder());

		// 客户端的逻辑
		pipeline.addLast("handler", new ClientHandler());
	}
}
class ClientHandler extends SimpleChannelInboundHandler<String> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		//RESULT.set(JSON.parseObject(msg, Message.class));
		/*System.out.println("Server say : " + msg);

		if ("ping".equals(msg)) {
			System.out.println("ping");
			ctx.channel().writeAndFlush("OK\n");
		} else {
			//业务逻辑
		}*/
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Client active ");
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Client close ");
		ctx.channel().close();
		//super.channelInactive(ctx);
	}
}