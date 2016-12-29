package org.dc.penguin.core.raft;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.Commons;
import org.dc.penguin.core.InitSystemHandle;
import org.dc.penguin.core.entity.Message;
import org.dc.penguin.core.entity.MsgType;
import org.dc.penguin.core.utils.Utils;

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

public class NettyRaftServer {
	private static Log LOG = LogFactory.getLog(NettyRaftServer.class);
	
	public static void main(String[] args) {
		InitSystemHandle init = InitSystemHandle.getInstance();
		try {
			init.initConfig();
			
			NettyRaftServer server = new NettyRaftServer();
			server.startServer();
			
		} catch (Exception e) {
			LOG.info("",e);
		}
		
		
	}
	
	//初始化

	//服务端不会一直创建实例此，所以这里都用非静态
	private EventLoopGroup bossGroup = new NioEventLoopGroup();
	private EventLoopGroup workerGroup = new NioEventLoopGroup();
	private ServerBootstrap bootstrap = new ServerBootstrap();

	private int role;


	public void startServer(){
		try{
			//判断当前是否有leader,如果有，则同步数据，如果同步超时，则删除本机器，同步成功，则加入raft集群
			/*boolean haveLeader = false;
			for (int i = 0,len= Commons.serverList.size(); i < len; i++) {
				ServerInfo serverInfo = Commons.serverList.get(i);
				if(!serverInfo.isLocalhost()){
					//如果不是本地配置的，就询问是不是leader
					NettyRaftClient raftClient = new NettyRaftClient(serverInfo.getHost()+":"+serverInfo.getPort());
					String leaderInfo = raftClient.getLeader();
					if(leaderInfo!=null){
						haveLeader = true;
						break;
					}
				}
			}
			if(haveLeader==false){//未找到leader
				if(Commons.serverList.size()>1){
					throw new Exception("未找到leader");
				}else if(Commons.serverList.size()==1){
					Commons.serverList.get(0).setRole(Role.LEADER);
				}
			}*/
			//String leaderInfo = Utils.getLeaderByHard();
			/*new Thread(new Runnable() {
				
				public void run() {
					try {
						String leaderInfo = Utils.getLeaderByHard();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();*/
			/*String leaderInfo = Commons.threadPool.submit(new Callable<String>() {

				public String call() throws Exception {
					return  Utils.getLeaderByHard();
				}
				
			}).get();*/
			
			/*if(leaderInfo == null){
				if(Commons.serverList.size()>1){
					throw new Exception("未找到leader");
				}else if(Commons.serverList.size()==1){
					Commons.serverList.get(0).setRole(Role.LEADER);
				}
			}*/

			bootstrap.group(bossGroup,workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 1024)
			.childHandler(new RaftServerChannelHandler());
			ChannelFuture f = bootstrap.bind(Utils.getLocal().getPort()).sync();
			System.out.println("Server start Successful,Port="+Utils.getLocal().getPort());
			f.channel().closeFuture().sync();
		}catch (Exception e) {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
			LOG.error("",e);
		}

	}

	/*private void initHeartbeatList() {
		for (int i = 0; i <serverList.size(); i++) {
			ServerInfo serverInfo = serverList.get(i);
			if(!serverInfo.isLocalhost()){//排除本机，只和其他机器保持心跳
				NettyRaftClient client = new NettyRaftClient(serverInfo.getHost()+":"+serverInfo.getPort());
				clientList.add(client);
			}
		}
	}*/
}
class RaftServerChannelHandler extends ChannelInitializer<SocketChannel>{

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
		pipeline.addLast("handler", new RaftServerHandler());
	}

}
class RaftServerHandler extends SimpleChannelInboundHandler<String> {

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