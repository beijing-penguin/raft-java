package org.dc.penguin.core.utils;

import org.dc.penguin.core.SystemInit;
import org.dc.penguin.core.entity.ServerInfo;
import org.dc.penguin.core.raft.NettyRaftClient;

public class Utils {
	public static String getLocalHostAndPort() throws Exception{
		for (int i = 0,len=SystemInit.serverList.size(); i < len; i++) {
			ServerInfo serverInfo = SystemInit.serverList.get(i);
			if(serverInfo.isLocalhost()){
				return serverInfo.getHost()+":"+serverInfo.getPort();
			}
		}
		throw new Exception("未找到本机IP地址和服务端口");
	}
	/**
	 * 服务器启动时，努力3次去得到leader
	 * @return
	 * @throws Exception 
	 */
	public static String getLeaderHard() throws Exception{
		int n = 3;
		while(true){
			for (int i = 0,len= SystemInit.serverList.size(); i < len; i++) {
				ServerInfo serverInfo = SystemInit.serverList.get(i);
				if(!serverInfo.isLocalhost()){
					//如果不是本地配置的，就询问是不是leader
					NettyRaftClient raftClient = new NettyRaftClient(serverInfo.getHost()+":"+serverInfo.getPort());
					String leaderInfo = raftClient.getLeader();
					if(leaderInfo!=null){
						return leaderInfo;
					}
				}
			}
			n++;
			if(n>3){
				break;
			}
		}
		return null;
	}
}
