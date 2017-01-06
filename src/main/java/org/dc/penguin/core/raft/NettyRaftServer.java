package org.dc.penguin.core.raft;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.InitSystemHandle;
import org.dc.penguin.core.entity.Message;
import org.dc.penguin.core.entity.MsgType;
import org.dc.penguin.core.entity.Role;
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

public class NettyRaftServer {
	private static Log LOG = LogFactory.getLog(NettyRaftServer.class);

	private EventLoopGroup bossGroup = new NioEventLoopGroup();
	private EventLoopGroup workerGroup = new NioEventLoopGroup();
	private ServerBootstrap bootstrap = new ServerBootstrap();
	private static InitSystemHandle init = InitSystemHandle.getInstance();
	public static void main(String[] args) {
		try {
			init.initConfig();
			NettyRaftServer server = new NettyRaftServer();
			server.startServer();
		} catch (Exception e) {
			LOG.info("",e);
		}
	}
	public void startServer(){
		try{
			bootstrap.group(bossGroup,workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 1024)
			.childHandler(new RaftServerChannelHandler());
			for (int i = 0; i < init.getConnVector().size(); i++) {
				LocalStateMachine machine = init.getConnVector().get(i);
				if(machine.isLocalhost()){
					bootstrap.bind(machine.getPort()).sync();
					//machine.startLeaderListen();
					System.out.println("Server start Successful,Port="+machine.getPort());
					//告诉leader， 我来了。如果得到主的响应，则同步主的数据。主服务自动和该服务保持心跳联系。
					try{
						String leaderInnfo = machine.getOnlineLeader();
						if(leaderInnfo!=null){
							//同步集群信息和data数据
							machine.syncAllClusterInfoFromLeader(leaderInnfo.split(":")[0],Integer.parseInt(leaderInnfo.split(":")[1]));
							//通知领导，数据已同步完，加入集群
							machine.joinLeaderCluster(leaderInnfo.split(":")[0],Integer.parseInt(leaderInnfo.split(":")[1]));
						}else{
							//开始选举。我发现集群里没有leader，那么我就主动通知所有人，开始选举
							machine.sendPollInvitation();
						}
					} catch (Exception e) {
						LOG.error("",e);
					}
				}
			}
			/*LocalStateMachine machine = init.connVector.get(0);
			//开始获取领导
			if(init.connVector.get(0).getLeaderAndSyncData()){
				//开始同步集群信息和存储数据
				if(machine.sendAllMachineInfo()){
					machine.setRole(Role.LEADER);
				}
			}*/
			//开始监听leader的心跳
			/*new Thread(new Runnable() {
				public void run() {
					while(true){
						try {
							Thread.sleep(3000);
							if(RaftUtils.getOnlineLeader() == null){
								//设置领导
								
							}
						} catch (InterruptedException e) {
							LOG.info("",e);
						}
					}
				}
			}).start();*/
		}catch (Exception e) {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
			LOG.error("",e);
		}
	}
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
		System.out.println(ctx.channel().remoteAddress() + " Say : " + msg);
		Message message = JSON.parseObject(msg,Message.class);
		switch (message.getReqType()) {
		case MsgType.GET_LEADER:
			InitSystemHandle initConfig = InitSystemHandle.getInstance();
			for (int i = 0; i < initConfig.getConnVector().size(); i++) {
				if(initConfig.getConnVector().get(i).getRole() == Role.LEADER){
					
				}
			}
			break;
		default:
			break;
		}
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