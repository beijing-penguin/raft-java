package org.dc.penguin.core.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.InitSystemHandle;
import org.dc.penguin.core.raft.NettyRaftConnection;
import org.dc.penguin.core.raft.NettyRaftServer;

import sun.applet.Main;

public class Utils {
	private static InitSystemHandle initCofig = InitSystemHandle.getInstance();
	private static Log LOG = LogFactory.getLog(NettyRaftServer.class);

	public static NettyRaftConnection getLocal() throws Exception{
		for (int i = 0,len = initCofig.connVector.size(); i < len; i++) {
			NettyRaftConnection raftConnection = initCofig.connVector.get(i);
			if(raftConnection.isLocalhost()){
				return raftConnection;
			}
		}
		throw new Exception("未找到本机IP地址和服务端口");
	}
	/**
	 * 服务器启动时，异步努力加载3次去得到leader
	 * @return
	 * @throws Exception 
	 * @throws Exception 
	 */
	/*public static String getLeader() throws Exception{

		for (int i = 0,len=initCofig.connVector.size(); i < len; i++) {
			NettyRaftConnection raftConnection = initCofig.connVector.get(i);
			int connNum = 0;
			while(true){
				try{
					Channel channel = raftConnection.getConnection();
					String leaderInfo = NettyRaftDao.getLeader(raftConn);
				}catch (Exception e) {
					connNum++;
					Thread.sleep(3000);
				}
			}
		}*/


	/*try{
			Future<String> f = initCofig.threadPool.submit(new Callable<String>() {
				public String call() throws Exception {
					try{
						Random random=new Random();
						int n = 0;
						while(true){
							for (int i = 0,len= initCofig.connVector.size(); i < len; i++) {
								NettyRaftConnection raftConn = initCofig.connVector.get(i);
								if(!raftConn.isLocalhost()){
									String hostAndPort = raftConn.getHost()+":"+raftConn.getPort();
									LOG.info("程序启动获取leader="+hostAndPort);
									//如果不是本地配置的，就询问是不是leader
									try{
										String leaderInfo = NettyRaftDao.getLeader(raftConn);
										if(leaderInfo!=null){
											return leaderInfo;
										}
									}catch (Exception e) {

									}
								}
							}
							Thread.sleep(random.nextInt(6)*1000);
							n++;
							if(n>2){
								break;
							}
						}
					}catch (Exception e) {
					}
					return null;
				}
			});
			return f.get();
		}catch (Exception e) {
		}
		return null;*/

	/** 
	 * 获取本机所有IP 
	 * @throws Exception 
	 */
	//private static Set<String> ipSet = new HashSet<String>();
	public static Set<String> getAllLocalHostIP() throws Exception {
		Set<String> ipSet = new HashSet<String>();
		Enumeration<NetworkInterface> netInterfaces;  
		netInterfaces = NetworkInterface.getNetworkInterfaces();  
		InetAddress ip = null;  
		while (netInterfaces.hasMoreElements()) {  
			NetworkInterface ni = (NetworkInterface) netInterfaces  
					.nextElement();  
			//System.out.println("---Name---:" + ni.getName());  
			Enumeration<InetAddress> nii = ni.getInetAddresses();  
			while (nii.hasMoreElements()) {  
				ip = (InetAddress) nii.nextElement();  
				if (ip.getHostAddress().indexOf(":") == -1) {  
					ipSet.add(ip.getHostAddress());
				}  
			}  
		}  
		return ipSet;
	}  
}
