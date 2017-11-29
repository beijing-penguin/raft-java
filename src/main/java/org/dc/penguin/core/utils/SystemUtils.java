package org.dc.penguin.core.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;
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
}
