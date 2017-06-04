package org.dc.penguin.core.raft;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.ConfigInfo;
import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MsgType1;
import org.dc.penguin.core.pojo.RoleType;

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
	public void startServer(){
		try{
			/*bootstrap.group(bossGroup,workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 1024)
			.childHandler(new RaftServerChannelHandler());
			for (int i = 0; i < init.getConnVector().size(); i++) {
				LocalStateMachine machine = init.getConnVector().get(i);
				machine.setRole(Role.CONDIDATE);
				if(machine.isLocalhost()){
					bootstrap.bind(machine.getPort()).sync();
					System.out.println("Server start Successful,Port="+machine.getPort());
				}

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
			//随机选择一个人优先发起投票
			init.getConnVector().get(0).sendPollInvitation();
			//投票完了，开始统计票数，并设置role=FOLLOWER
			for (int i = 0; i < init.getConnVector().size(); i++) {
				LocalStateMachine machine = init.getConnVector().get(i);
				if(i==0){
					machine.setRole(Role.LEADER);
				}
				machine.sendPollInvitation();
			}*/
		}catch (Exception e) {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
			LOG.error("",e);
		}
	}
}
