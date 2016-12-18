package org.dc.penguin;

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
	private static Bootstrap boot = new Bootstrap().group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true);
	static{
		
	}
	private ChannelHandlerContext leaderChannel;
	private CountDownLatch downLatch = new CountDownLatch(1);
	private boolean leaderException = true;
	private Message resultMessage;

	public NettyClient(final String...hostAndPort){
		try{
			//this.hostAndPort = hostAndPort;
			//初始化boot
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
							if(message.getReqType() == MsgType.YES_LEADER){
								leaderChannel = ctx;
								leaderException = false;
							}else{
								resultMessage = message;
								downLatch.countDown();
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

			//启动leader服务器异常检查，并自动获取leader服务器
			new Thread(new Runnable() {
				public void run() {
					while(true){

						if(leaderException){
							LOG.info("正在获取leader");
							for (int i = 0; i < hostAndPort.length; i++) {
								try {
									String host = hostAndPort[i].split(":")[0];
									int port = Integer.parseInt(hostAndPort[i].split(":")[1]);
									Channel channel1 = boot.connect(host, port).sync().channel();

									Message msg = new Message();
									msg.setReqType(MsgType.GET_LEADER);
									channel1.writeAndFlush(JSON.toJSONString(msg));
								} catch (Exception e) {
									LOG.info("",e);
								}
							}
						}
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							LOG.error("",e);
						}
					}
				}
			}).start();
		}catch (Exception e) {
			LOG.info("",e);
		}
	}
	public static void main(String[] args) {
		new NettyClient("localhost:9001");
	}
	public Object get(String key) throws Exception{
		resultMessage = null;
		Message msg =new Message();
		msg.setReqType(MsgType.GET_DATA);
		msg.setBody(key);
		leaderChannel.writeAndFlush(msg.toJSONString());
		downLatch.await(8,TimeUnit.SECONDS);
		if(resultMessage==null || resultMessage.getReqType() != MsgType.SUCCESS){
			throw new Exception("获取数据异常");
		}
		return resultMessage.getBody();
	}
	public Message sendMessage(Message msg) throws Exception{
		resultMessage = null;
		leaderChannel.writeAndFlush(msg.toJSONString());
		downLatch.await(5,TimeUnit.SECONDS);
		if(resultMessage==null || resultMessage.getReqType() != MsgType.SUCCESS){
			throw new Exception("获取数据异常");
		}
		return resultMessage;
	}
}
/*class ClientInitializer extends ChannelInitializer<SocketChannel> {
	private CountDownLatch lathc;
	private ClientInitializer handler;

    public ClientInitializer(CountDownLatch lathc) {
        this.lathc = lathc;
    }

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		handler = new ClientInitializer(lathc);
		ch.config().setAllowHalfClosure(true);
		ChannelPipeline pipeline = ch.pipeline();

		pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
		pipeline.addLast("decoder", new StringDecoder());
		pipeline.addLast("encoder", new StringEncoder());

		// 客户端的逻辑
		pipeline.addLast("handler", new SimpleChannelInboundHandler<String>() {
			@Override
			protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
				System.out.println(msg);
				System.out.println(JSON.parseObject(msg, Message.class));
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

	public String getServerResult(){
        return handler.getServerResult();
    }
    //重置同步锁
    public void resetLathc(CountDownLatch initLathc) {
    	handler.lathc = initLathc;
    }
}*/
/*class ClientHandler extends SimpleChannelInboundHandler<String> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		//RESULT.set(JSON.parseObject(msg, Message.class));
		System.out.println("Server say : " + msg);

		if ("ping".equals(msg)) {
			System.out.println("ping");
			ctx.channel().writeAndFlush("OK\n");
		} else {
			//业务逻辑
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
		//super.channelInactive(ctx);
	}
}*/