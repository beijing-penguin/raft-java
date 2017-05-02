package org.dc.penguin.core.raft;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.entity.Message;
import org.dc.penguin.core.utils.NettyConnection;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class RaftServer {
	private static Log LOG = LogFactory.getLog(NettyRaftServer.class);
	public void startDataServer(int port){
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();
		
		try {
			bootstrap.group(bossGroup,workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 1024)
			.childHandler(new RaftServerChannelHandler()).bind(port).sync();
			System.out.println("数据通信服务开启成功，port="+port);
		} catch (InterruptedException e) {
			LOG.error("",e);
			bootstrap = null;
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
	public void startElectionServer(int port){
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();
		
		try {
			bootstrap.group(bossGroup,workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 1024)
			.childHandler(new RaftServerChannelHandler()).bind(port).sync();
			System.out.println("选举服务开启成功，port="+port);
			
			//向其他人询问是否存在leader，所谓其他人就是端口不等于当前端口，或者ip不等于本地的人。
			
			
		} catch (InterruptedException e) {
			LOG.error("",e);
			bootstrap = null;
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
}
