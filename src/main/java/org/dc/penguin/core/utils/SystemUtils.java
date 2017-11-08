package org.dc.penguin.core.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class SystemUtils {
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
			NetworkInterface ni = (NetworkInterface) netInterfaces.nextElement();  
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
	/*public static LocalStateMachine getOnlineLeader(){
		for (int i = 0; i < initConfig.getConnVector().size(); i++) {
			LocalStateMachine lsm = initConfig.getConnVector().get(i);
			if(lsm.getRole() == Role.LEADER){
				
			}
		}
		for (int i = 0; i < initConfig.getConnVector().size(); i++) {
			try {
				LocalStateMachine machine = initConfig.getConnVector().get(i);
				NettyConnection connection = new NettyConnection(machine.getHost(), machine.getPort());
				Message rt_msg = connection.sendMessage(RaftMessageFactory.createGetLeaderMsg());

				if(rt_msg.getReqType()==MsgType.YES_LEADER){
					return machine;
				}
			} catch (Exception e) {
				LOG.info("",e);
			}
		}
		return null;
	}*/
	public static void main(String[] args) throws Exception {
		System.out.println(new Random().nextInt(10)*1000);
		System.out.println(SystemUtils.getAllLocalHostIP());
	}
}
