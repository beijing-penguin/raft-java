package org.dc.penguin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.entity.Message;
import org.dc.penguin.entity.MsgType;

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
	private static Log LOG = LogFactory.getLog(NettyClient.class);
	//多线程情况下，公用线程组
	private static EventLoopGroup group = new NioEventLoopGroup();
	//多线程公用Bootstrap对象
	private Bootstrap boot = new Bootstrap().group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true);
	private Channel leaderChannel;
	private CountDownLatch downLatch = new CountDownLatch(1);
	private Message resultMessage;
	
	public NettyClient(String...hostAndPort){
		try{
			//初始化boot
			initBoot();
			//启动leader服务器异常检查，并自动获取leader服务器
			startChannelListener(hostAndPort,false);
		}catch (Exception e) {
			LOG.info("",e);
		}
	}
	public NettyClient(String hostAndPort,boolean directConnection){
		try{
			//初始化boot
			initBoot();
			//启动leader服务器异常检查，并自动获取leader服务器
			startChannelListener(new String[]{hostAndPort},directConnection);
		}catch (Exception e) {
			LOG.info("",e);
		}
	}
	public NettyClient(Channel channel){
		this.leaderChannel = channel;
	}
	private void initBoot(){
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
						Message message = JSON.parseObject(msg, Message.class);
						switch (message.getReqType()) {
						case MsgType.YES_LEADER:
							String leaderIp = message.getBody().toString();
							String host = leaderIp.split(":")[0];
							int port = Integer.parseInt(leaderIp.split(":")[1]);
							setLeaderChannel(boot.connect(host, port).sync().channel());
							ctx.close();
							break;
						default:
							resultMessage = message;
							downLatch.countDown();
							break;
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
						ctx.channel().close();
					}
				});
			}
		});
	}
	/**
	 * @throws Exception 
	 * 
	 */
	public void startChannelListener (final String[] hostAndPort,final boolean directConnection){
		try{
			if(directConnection){
				String host = hostAndPort[0].split(":")[0];
				int port = Integer.parseInt(hostAndPort[0].split(":")[1]);
				leaderChannel = boot.connect(host, port).sync().channel();
			}else{
				new Thread(new Runnable() {
					public void run() {
						List<String> all = new ArrayList<String>();
						for (int i = 0; i < hostAndPort.length; i++) {
							all.add(hostAndPort[i]);
						}
						List<String> normals = new ArrayList<String>();
						List<String> exceptions = new ArrayList<String>();

						//服务器可用顺序
						while(true){
							if(leaderChannel==null || !leaderChannel.isActive() || !leaderChannel.isOpen()){
								normals.clear();
								exceptions.clear();
								Channel channel = null;
								LOG.info("正在获取leader");
								for (int i = 0; i < all.size(); i++) {
									String hostPort = all.get(i);
									try {
										String host = hostPort.split(":")[0];
										int port = Integer.parseInt(hostPort.split(":")[1]);
										if(directConnection){
											leaderChannel = boot.connect(host, port).sync().channel();
											break;
										}else{
											channel = boot.connect(host, port).sync().channel();

											Message msg = new Message();
											msg.setReqType(MsgType.GET_LEADER);
											channel.writeAndFlush(msg.toJSONString());
											normals.add(hostPort);
											Thread.sleep(3000);
											if(leaderChannel==null || !leaderChannel.isActive() || !leaderChannel.isOpen()){
												continue;
											}else{
												break;
											}
										}
									} catch (Exception e) {
										LOG.error("",e);
										exceptions.add(hostPort);
									}finally{
										if(channel!=null){
											channel.close();
										}
									}
								}
								all.clear();
								all.addAll(normals);
								all.addAll(exceptions);
							}else{
								try {
									Thread.sleep(3000);
								} catch (InterruptedException e) {
									LOG.error("",e);
								}
							}
						}
					}
				}).start();
			}
		}catch (Exception e) {
			LOG.error("",e);
		}
	}
	public static void main(String[] args) throws Exception {
		new NettyClient("localhost:9001");
	}
	public byte[] getData(String key) throws Exception{
		resultMessage = null;
		Message msg =new Message();
		msg.setReqType(MsgType.GET_DATA);
		msg.setBody(key.getBytes());
		leaderChannel.writeAndFlush(msg.toJSONString());
		downLatch.await(10,TimeUnit.SECONDS);//等待10秒
		if(resultMessage==null || resultMessage.getReqType() != MsgType.SUCCESS){
			throw new Exception("获取数据异常");
		}
		return resultMessage.getBody();
	}
	public Message sendMessage(Message msg) throws Exception{
		resultMessage = null;
		leaderChannel.writeAndFlush(msg.toJSONString());
		downLatch.await(8,TimeUnit.SECONDS);
		if(resultMessage==null){
			throw new Exception("获取数据异常");
		}
		return resultMessage;
	}
	private synchronized void setLeaderChannel(Channel leaderChannel){
		if(leaderChannel==null || !leaderChannel.isActive() || !leaderChannel.isOpen()){
			this.leaderChannel = leaderChannel;
		}else{
			leaderChannel.close();
		}
	}
}