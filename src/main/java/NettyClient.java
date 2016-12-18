

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.entity.Message;
import org.dc.penguin.entity.ServerInfo;

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
	public static ThreadLocal<Message> RESULT = new ThreadLocal<Message>();
	private static Log LOG = LogFactory.getLog(NettyClient.class);

	private Map<Channel,ServerInfo> channelMap = new HashMap<Channel,ServerInfo>();

	public NettyClient(String...hostAndPort){
		EventLoopGroup group = new NioEventLoopGroup();
		try{

				Bootstrap b = new Bootstrap();
				b.group(group)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true)
				.handler(new ClientInitializer());
				
				String host = "localhost";
				int port = 9001;

				final Channel channel = b.connect(host,port).sync().channel();
				channel.writeAndFlush("asdasdasdasd\n");
				for (int i = 0; i < 100; i++) {
					final int num = i;
					new Thread(new Runnable() {
						public void run() {
							channel.writeAndFlush("asdasdasdasd"+num+"\n");
						}
					}).start();
				}

		}catch (Exception e) {
			group.shutdownGracefully();
			LOG.info("",e);
		}
	}
	
	//选举
	public void election(){
		
	}
	public void askAll() throws Exception{
		String body_rt = null;
		
		String local_ip = "";
	/*	for (int i = 0; i < channelList.size(); i++) {
			ChannelInfo cinfo = channelList.get(i);
			local_ip += cinfo.getHost()+":"+cinfo.getPort()+",";
			Message msg = new Message();
			msg.setReqType(ReqType.RAFT_PING.getRequestType());
			cinfo.getChannel().writeAndFlush(JSON.toJSONString(msg));
			Message msg_rs = RESULT.get();
			String body = msg_rs.getBody();
			if(body_rt == null){
				body_rt = msg_rs.getBody();
			}else{
				if(!body_rt.equals(body)){
					throw new Exception("服务器确认异常");
				}
			}
		}*/
		if(local_ip.equals(body_rt)){
			
		}
	}
	public static void main(String[] args) {
		new NettyClient(null);
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
		NettyClient.RESULT.set(JSON.parseObject(msg, Message.class));
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