package org.dc.penguin.core.raft;

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.InitSystemHandle;

import io.netty.channel.Channel;

public class NettyRaftDao {
	private static final Log LOG = LogFactory.getLog(NettyRaftDao.class);
	private static InitSystemHandle initConfig = InitSystemHandle.getInstance();
	
	public static String getLeader() throws Exception{
		for (int i = 0,len=initConfig.connVector.size(); i < len; i++) {
			NettyRaftConnection raftConnection = initConfig.connVector.get(i);
			
			int connNum = 0;
			try{
				Channel channel = raftConnection.getConnection();
				channel.writeAndFlush(null);
			}catch (Exception e) {
				Thread.sleep(new Random().nextInt(6)*1000);
			}
		}
		
		return null;
		/*Message msg = new Message();
		msg.setReqType(MsgType.GET_LEADER);
		msg.setBody(Utils.getLocalHostAndPort().getBytes());
		connection.getConnection().writeAndFlush(msg.toJSONString());

		connection.getDownLatch().await(8,TimeUnit.SECONDS);
		if(connection.getResultMessage()==null){
			throw new Exception("获取数据异常");
		}
		return new String(connection.getResultMessage().getBody());*/
	}
}