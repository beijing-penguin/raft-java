package org.dc.penguin.core.utils;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.dc.penguin.core.Commons;
import org.dc.penguin.core.entity.ServerInfo;
import org.dc.penguin.core.raft.NettyRaftClient;

public class Utils {
	public static String getLocalHostAndPort() throws Exception{
		for (int i = 0,len=Commons.serverList.size(); i < len; i++) {
			ServerInfo serverInfo = Commons.serverList.get(i);
			if(serverInfo.isLocalhost()){
				return serverInfo.getHost()+":"+serverInfo.getPort();
			}
		}
		throw new Exception("未找到本机IP地址和服务端口");
	}
	/**
	 * 服务器启动时，异步努力加载3次去得到leader
	 * @return
	 * @throws Exception 
	 */
	public static String getLeaderByHard(){
		try{
			Future<String> f  = Commons.threadPool.submit(new Callable<String>() {
				public String call() throws Exception {
					try{
						Random random=new Random();
						int n = 3;
						while(true){
							for (int i = 0,len= Commons.serverList.size(); i < len; i++) {
								ServerInfo serverInfo = Commons.serverList.get(i);
								if(!serverInfo.isLocalhost()){
									//如果不是本地配置的，就询问是不是leader
									NettyRaftClient raftClient = new NettyRaftClient(serverInfo.getHost()+":"+serverInfo.getPort());
									String leaderInfo = raftClient.getLeader();
									if(leaderInfo!=null){
										return leaderInfo;
									}
								}
							}
							//Thread.sleep(random.nextInt(6)*1000);
							n++;
							if(n>3){
								break;
							}
						}
					}catch (Exception e) {
						// TODO: handle exception
					}
					return null;
				}
			});
			return f.get();
		}catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
}
