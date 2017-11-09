package org.dc.penguin.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.ConfigInfo;
import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MsgType;
import org.dc.penguin.core.raft.NodeInfo;

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

public class NettyConnection{
	private static Log LOG = LogFactory.getLog(NettyConnection.class);

	//private static Map<String, CountDownLatch> syncMap = new ConcurrentHashMap<String, CountDownLatch>();
	private long readTimeout = 3*1000;//默认3秒
	private static EventLoopGroup group = new NioEventLoopGroup(50);
	private static Bootstrap boot = new Bootstrap().group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true);
	private String host;
	private int port;
	private static Map<String,Channel> channeMap = new ConcurrentHashMap<String,Channel>();

	public NettyConnection(String host,int port){
		this.host = host;
		this.port = port;
		//initBoot();
	}
	static {
		boot.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.config().setAllowHalfClosure(true);
				ChannelPipeline pipeline = ch.pipeline();

				pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
				pipeline.addLast("decoder", new StringDecoder());
				pipeline.addLast("encoder", new StringEncoder());
				pipeline.addLast("handler", new SimpleChannelInboundHandler<String>() {
					@Override
					protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
						try {
							Message resultMessage = JSON.parseObject(msg, Message.class);
							switch (resultMessage.getMsgCode()) {
							case MsgType.LEADER_PING:
								/*NodeInfo node = JSON.parseObject(resultMessage.getValue(), NodeInfo.class);
								for (NodeInfo nodeInfo : ConfigInfo.getNodeConfigList()) {
									if(nodeInfo.getHost().equals(node.getHost()) && nodeInfo.getElectionServerPort() == node.getElectionServerPort()) {
										nodeInfo.getVoteTotalNum().incrementAndGet();
									}
								}*/
								//NodeUtils.leaderNodeInfo = JSON.parseObject(resultMessage.getValue(), NodeInfo.class);
								/*NodeInfo node1 = JSON.parseObject(resultMessage.getValue(), NodeInfo.class);
								for (NodeInfo nodeInfo : ConfigInfo.getNodeConfigList()) {
									if(nodeInfo.getHost().equals(node1.getHost()) && nodeInfo.getElectionServerPort() == node1.getElectionServerPort()) {
										nodeInfo.setLeaderPingNum(nodeInfo.getLeaderPingNum()+1);
									}
								}*/
								break;
							case MsgType.VOTE:
								NodeInfo node = JSON.parseObject(resultMessage.getValue(), NodeInfo.class);
								for (NodeInfo nodeInfo : ConfigInfo.getNodeConfigList()) {
									if(nodeInfo.getHost().equals(node.getHost()) && nodeInfo.getElectionServerPort() == node.getElectionServerPort()) {
										nodeInfo.getVoteTotalNum().incrementAndGet();
									}
								}
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
		String key = host+port;
		Channel channel =null;
		if(channeMap.containsKey(key)) {
			channel = channeMap.get(key);
		}
		if(channel==null || !channel.isOpen() || !channel.isActive()) {
			channel = boot.connect(host, port).sync().channel();
			channeMap.put(key, channel);
		}
		return channel;
	}
	public void sendMessage(Message msg) throws Exception{
		Channel channel = getChannel();
		channel.writeAndFlush(msg.toJSONString());
	}
	public void close() throws Exception {
		String key = host+port;
		Channel channel = channeMap.get(key);
		if(channel!=null){
			channel.close().sync();
			channel=null;
		}
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
	public long getReadTimeout() {
		return readTimeout;
	}
	public void setReadTimeout(long readTimeout) {
		this.readTimeout = readTimeout;
	}
}
