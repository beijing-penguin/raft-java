package org.dc.penguin.core.raft;

import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.entity.Message;
import org.dc.penguin.core.entity.MsgType;
import org.dc.penguin.core.utils.Utils;

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

public class NettyRaftConnection {
	private static Log LOG = LogFactory.getLog(NettyRaftConnection.class);
	//多线程情况下，公用线程组
	private static EventLoopGroup group = new NioEventLoopGroup();
	private Bootstrap boot = new Bootstrap().group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true);

	private String host;
	private int port;
	private boolean isLocalhost;
	
	
	private Channel channel;
	
	private CountDownLatch downLatch = new CountDownLatch(1);
	private Message resultMessage;


	public NettyRaftConnection(String host,int port){
		this.host = host;
		this.port = port;
		//初始化boot
		initBoot();
		//启动leader服务器异常检查，并自动获取leader服务器
		//startChannelListener(hostAndPort,false);
	}
	/*public NettyClient2(String hostAndPort,boolean directConnection){
		try{
			//初始化boot
			initBoot();
			//启动leader服务器异常检查，并自动获取leader服务器
			startChannelListener(new String[]{hostAndPort},directConnection);
		}catch (Exception e) {
			LOG.info("",e);
		}
	}*/
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
						/*switch (message.getReqType()) {
						case MsgType.SUCCESS:
							resultMessage = message;
							downLatch.countDown();
							break;
						case MsgType.YES_LEADER:
							String leaderIp = message.getBody().toString();
							String host = leaderIp.split(":")[0];
							int port = Integer.parseInt(leaderIp.split(":")[1]);
							//leaderChannel = boot.connect(host, port).sync().channel();
							ctx.close();
							break;
						case MsgType.NO_LEADER:
							break;
						default:
							throw new Exception("消息解析失败");
						}*/
						resultMessage = message;
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

		//channel = boot.connect(host, port).sync().channel();

	}
	/**
	 * @throws Exception 
	 * 
	 */
	/*private void startChannelListener(final String[] hostAndPort,final boolean directConnection){
		try{
			if(directConnection){
				final String host = hostAndPort[0].split(":")[0];
				final int port = Integer.parseInt(hostAndPort[0].split(":")[1]);
				try{
					leaderChannel = boot.connect(host, port).sync().channel();
				}catch (Exception e) {
					LOG.error("",e);
				}
				new Thread(new Runnable() {
					public void run() {
						while(true){
							if(close){
								close();
								break;
							}
							try {
								if(leaderChannel==null || !leaderChannel.isActive() || !leaderChannel.isOpen()){
									leaderChannel = boot.connect(host, port).sync().channel();
								}
								Thread.sleep(3000);
							} catch (Exception e) {
								LOG.error("",e);
							}
						}
					}
				}).start();
			}else{
				final String host = hostAndPort[0].split(":")[0];
				final int port = Integer.parseInt(hostAndPort[0].split(":")[1]);
				try{
					Channel channel = boot.connect(host, port).sync().channel();
					Message msg = new Message();
					msg.setReqType(MsgType.GET_LEADER);
					channel.writeAndFlush(msg.toJSONString());
					getleader_downLatch.await(8, TimeUnit.SECONDS);
				}catch (Exception e) {
					LOG.error("",e);
				}
				new Thread(new Runnable() {
					public void run() {
						List<String> all = Arrays.asList(hostAndPort);
						List<String> normals = new ArrayList<String>();
						List<String> exceptions = new ArrayList<String>();

						//服务器可用顺序
						while(true){
							if(close){
								close();
								break;
							}
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
										channel = boot.connect(host, port).sync().channel();

										Message msg = new Message();
										msg.setReqType(MsgType.GET_LEADER);
										channel.writeAndFlush(msg.toJSONString());
										normals.add(hostPort);
										getleader_downLatch.await(8, TimeUnit.SECONDS);
										if(leaderChannel==null || !leaderChannel.isActive() || !leaderChannel.isOpen()){
											continue;
										}else{
											break;
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
	}*/
	/*public static void main(String[] args) throws Exception {
		Message msg = new Message();
		msg.setReqType(MsgType.GET_LEADER);
		msg.setBody(Utils.getLocalHostAndPort().getBytes());
		System.out.println(msg.getBody());
		System.out.println(msg.toJSONString());
		byte[]   data = new byte[]{108, 111, 99, 97, 108, 104, 111, '@', 116, 58, 57, 48, 48, '&'} ;
		Message msgs = JSON.parseObject("{\"body\":\"bG9jYWxob3N0OjkwMDE=\",\"reqType\":101}", Message.class);
		System.out.println(new String(msgs.getBody()));
		//client.getData("aa");
	}*/
	/*public byte[] getData(String key) throws Exception{
		resultMessage = null;
		Message msg =new Message();
		msg.setReqType(MsgType.GET_DATA);
		msg.setBody(key.getBytes());
		channel.writeAndFlush(msg.toJSONString());
		downLatch.await(10,TimeUnit.SECONDS);//等待10秒
		if(resultMessage==null || resultMessage.getReqType() != MsgType.SUCCESS){
			throw new Exception("获取数据异常");
		}
		return resultMessage.getBody();
	}*/
	/*public Message sendMessage(Message msg) throws Exception{
		resultMessage = null;
		channel.writeAndFlush(msg.toJSONString());
		downLatch.await(8,TimeUnit.SECONDS);
		if(resultMessage==null){
			throw new Exception("获取数据异常");
		}
		return resultMessage;
	}*/
	/*public void close(){
		this.close = true;
		boot = null;
		resultMessage = null;
		try{
			if(channel!=null){
				channel.close();
				channel = null;
			}
		}catch (Exception e) {
			LOG.error("",e);
		}
	}*/
	/*public String getLeader() throws Exception {
		Message msg = new Message();
		msg.setReqType(MsgType.GET_LEADER);
		msg.setBody(Utils.getLocalHostAndPort().getBytes());
		channel.writeAndFlush(msg.toJSONString());

		downLatch.await(8,TimeUnit.SECONDS);
		if(resultMessage==null){
			throw new Exception("获取数据异常");
		}
		return new String(resultMessage.getBody());
	}*/
	
	public Channel getConnection() throws InterruptedException{
		if(channel==null){
			channel = boot.connect(host, port).sync().channel();
		}else if(!channel.isOpen() || !channel.isActive()){
			channel.close().sync();
			channel = null;
			channel = boot.connect(host, port).sync().channel();
		}
		return channel;
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
	public CountDownLatch getDownLatch() {
		return downLatch;
	}
	public void setDownLatch(CountDownLatch downLatch) {
		this.downLatch = downLatch;
	}
	public Message getResultMessage() {
		return resultMessage;
	}
	public void setResultMessage(Message resultMessage) {
		this.resultMessage = resultMessage;
	}
	public boolean isLocalhost() {
		return isLocalhost;
	}
	public void setLocalhost(boolean isLocalhost) {
		this.isLocalhost = isLocalhost;
	}
}