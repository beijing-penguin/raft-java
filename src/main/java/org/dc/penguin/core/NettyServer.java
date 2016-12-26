package org.dc.penguin.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.entity.Message;
import org.dc.penguin.entity.MsgType;
import org.dc.penguin.entity.ServerInfo;
import org.dc.penguin.entity.ServerRole;

import com.alibaba.fastjson.JSON;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
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

public class NettyServer {
	public static void main(String[] args) {
		NettyServer server = new NettyServer();
		server.startServer();
	}
	private static Log LOG = LogFactory.getLog(NettyServer.class);
	//初始化
	private List<ServerInfo> serverList = new ArrayList<ServerInfo>();
	//初始化集群心跳客户端
	List<NettyClient> clientList = new ArrayList<NettyClient>();
	
	//服务端不会一直创建实例此，所以这里都用非静态
	private EventLoopGroup bossGroup = new NioEventLoopGroup();
	private EventLoopGroup workerGroup = new NioEventLoopGroup();
	private ServerBootstrap bootstrap = new ServerBootstrap();

	private int role;
	private int start_port = 9001;

	public void startServer(){
		try{
			bootstrap.group(bossGroup,workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 1024)
			.childOption(ChannelOption.SO_KEEPALIVE, true)
			.childHandler(new ServerChannelHandler());
			
			
			
			initServerList();
			initHeartbeatList();

			new Thread(new Runnable() {
				public void run() {
					
					while(true){
						try {
							for (int i = 0; i < clientList.size(); i++) {
								try{
									Message msg = new Message();
									msg.setReqType(MsgType.GET_LEADER);//返回当前集群中可用机器
									Message message = clientList.get(i).sendMessage(msg);
									if(message.getReqType() == MsgType.YES_LEADER){
										role  = ServerRole.FOLLOWER;
										//同步获取数据，并阻塞leader服务器，直到数据同步完成。
										break;
									}
								}catch (Exception e) {
									LOG.error("",e);
								}
							}
							Thread.sleep(3000);
						} catch (Exception e1) {
							LOG.error("",e1);
						}

					}
				}
			}).start();
			
			
			ChannelFuture f = bootstrap.bind(start_port).sync();
			System.out.println("Server start Successful,Port="+start_port);
			f.channel().closeFuture().sync();
		}catch (Exception e) {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
			LOG.error("",e);
		}
		
	}
	
	private void initHeartbeatList() {
		for (int i = 0; i <serverList.size(); i++) {
			ServerInfo serverInfo = serverList.get(i);
			if(!serverInfo.isLocalhost()){//排除本机，只和其他机器保持心跳
				NettyClient client = new NettyClient(serverInfo.getHost()+":"+serverInfo.getPort(),true);
				clientList.add(client);
			}
		}
	}

	private void initServerList() throws Exception {
		InputStream in = NettyServer.class.getResourceAsStream("/config.properties");
		Properties prop =  new  Properties();

		prop.load(in);
		in.close();
		start_port = Integer.parseInt(prop.getProperty("startPort","9001"));
		
		for (Object key : prop.keySet()) {
			System.out.println(key);
			String pro_key = key.toString();
			if(pro_key.startsWith("server")){
				String value = prop.getProperty(pro_key);
				String host = value.split(":")[0];
				int port = Integer.parseInt(value.split(":")[1]);

				ServerInfo serverInfo = new ServerInfo();

				serverInfo.setHost(host);
				serverInfo.setPort(port);
				if((InetAddress.getByName("localhost").getHostAddress().equalsIgnoreCase("127.0.0.1") ||
						host.equalsIgnoreCase(InetAddress.getLocalHost().getHostAddress()))&& port == start_port){
					serverInfo.setLocalhost(true);
				}
				serverList.add(serverInfo);
			}
		}
	}
}
class ServerChannelHandler extends ChannelInitializer<SocketChannel>{

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.config().setAllowHalfClosure(true);
		ChannelPipeline pipeline = ch.pipeline();

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
		pipeline.addLast("handler", new ServerHandler());
	}
}
class ServerHandler extends SimpleChannelInboundHandler<String> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		Thread.sleep(3000);
		System.out.println(ctx.channel().remoteAddress() + " Say : " + msg);
		Message ms = new Message();
		ms.setReqType(MsgType.YES_LEADER);
		ctx.channel().writeAndFlush(JSON.toJSONString(ms)+"\n");
		if (!"OK".equals(msg)) {
			//业务逻辑
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
		//super.userEventTriggered(ctx, evt); 
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("链接异常中断");
		ctx.close();
		//super.exceptionCaught(ctx, cause);
	}
}