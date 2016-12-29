package org.dc.penguin.core;
import java.io.BufferedInputStream;
/** 
 * 
 * @author Administrator 
 */  
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;  

public class TestAddr {

	// From HBase Addressing.Java
	/** 
	 * 获取本机所有IP 
	 */  
	private static String[] getAllLocalHostIP() {  
		List<String> res = new ArrayList<String>();  
		Enumeration<NetworkInterface> netInterfaces;  
		try {  
			netInterfaces = NetworkInterface.getNetworkInterfaces();  
			InetAddress ip = null;  
			while (netInterfaces.hasMoreElements()) {  
				NetworkInterface ni = (NetworkInterface) netInterfaces  
						.nextElement();  
				System.out.println("---Name---:" + ni.getName());  
				Enumeration<InetAddress> nii = ni.getInetAddresses();  
				while (nii.hasMoreElements()) {  
					ip = (InetAddress) nii.nextElement();  
					if (ip.getHostAddress().indexOf(":") == -1) {  
						res.add(ip.getHostAddress());  
						System.out.println(ip.getHostName()+"本机的ip=" + ip.getHostAddress());  
					}  
				}  
			}  
		} catch (SocketException e) {  
			e.printStackTrace();  
		}  
		return (String[]) res.toArray(new String[0]);  
	}  
	public static String getLocalIP() {  
		String ip = "";  
		try {  
			Enumeration<?> e1 = (Enumeration<?>) NetworkInterface.getNetworkInterfaces();  
			while (e1.hasMoreElements()) {  
				NetworkInterface ni = (NetworkInterface) e1.nextElement();  
				System.out.println ("getLocalIP--nic.getDisplayName ():" + ni.getDisplayName ());   
				System.out.println ("getLocalIP--nic.getName ():" + ni.getName ());  
				if (!ni.getName().equals("eth0")) {  
					continue;  
				} else {  
					Enumeration<?> e2 = ni.getInetAddresses();  
					while (e2.hasMoreElements()) {  
						InetAddress ia = (InetAddress) e2.nextElement();  
						if (ia instanceof Inet6Address)  
							continue;  
						ip = ia.getHostAddress();  
					}  
					break;  
				}  
			}  
		} catch (SocketException e) {  
			e.printStackTrace();  
			System.exit(-1);  
		}  
		return ip;  
	}  
	public static String getWinLocalIP ()   
	{   
		String ip = "";   
		try   
		{   
			Enumeration <?> e1 = (Enumeration <?>) NetworkInterface.getNetworkInterfaces ();   
			while (e1.hasMoreElements ())   
			{   
				NetworkInterface ni = (NetworkInterface) e1.nextElement ();  
				System.out.println ("getWinLocalIP--nic.getDisplayName ():" + ni.getDisplayName ());   
				System.out.println ("getWinLocalIP--nic.getName ():" + ni.getName ());  
				Enumeration <?> e2 = ni.getInetAddresses ();   
				while (e2.hasMoreElements ())   
				{   
					InetAddress ia = (InetAddress) e2.nextElement ();   
					ip = ia.getHostAddress ();   
				}   
			}   
		}   
		catch (SocketException e)   
		{   
			e.printStackTrace ();   
			System.exit (-1);   
		}   
		return ip;   
	}  
	/** 
	 * 获取本机所有物理地址 
	 * 
	 * @return 
	 */  
	public static String getMacAddress() {  
		String mac = "";  
		String line = "";  

		String os = System.getProperty("os.name");  

		if (os != null && os.startsWith("Windows")) {  
			try {  
				String command = "cmd.exe /c ipconfig /all";  
				Process p = Runtime.getRuntime().exec(command);  

				BufferedReader br = new BufferedReader(new InputStreamReader(p  
						.getInputStream()));  

				while ((line = br.readLine()) != null) {  
					if (line.indexOf("Physical Address") > 0) {  
						int index = line.indexOf(":") + 2;  

						mac = line.substring(index);  

						break;  
					}  
				}  

				br.close();  

			} catch (IOException e) {  
			}  
		}  

		return mac;  
	}  

	public String getMacAddress(String host) {  
		String mac = "";  
		StringBuffer sb = new StringBuffer();  

		try {  
			NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress  
					.getByName(host));  

			// byte[] macs = ni.getHardwareAddress();  

			// for (int i = 0; i < macs.length; i++) {  
			// mac = Integer.toHexString(macs[i] & 0xFF);  
			//  
			// if (mac.length() == 1) {  
			// mac = '0' + mac;  
			// }  
			//  
			// sb.append(mac + "-");  
			// }  

		} catch (SocketException e) {  
			e.printStackTrace();  
		} catch (UnknownHostException e) {  
			e.printStackTrace();  
		}  

		mac = sb.toString();  
		mac = mac.substring(0, mac.length() - 1);  

		return mac;  
	}  

	/** 
	 * @param args 
	 */  
	private static String ipv4OrIpv6(InetAddress ita) {
		String[] itn = ita.toString().split("/");
		String str = itn[1];
		if (str.length() > 16) {
			return "IPv6\t" + ita.toString();
		}
		return "IPv4\t" + ita.toString();
	}
	/*private void getIpAddr() throws SocketException, UnknownHostException {
		// 这XXXXXXX呢，是指计算机名，右键我的电脑，点属性查看就知道
		for (InetAddress it : InetAddress.getAllByName("DC-PC")) {
			System.out.println(ipv4OrIpv6(it));
		}
	}*/
	public static void main(String[] args) throws Exception {
		/*Process ps = null;
		try {
			ps = Runtime.getRuntime().exec("ipconfig");
			InputStream in = ps.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in,"GBK"));
			String line;
			in = new BufferedInputStream(in);
			StringBuffer buffer = new StringBuffer();
			while((line=reader.readLine())!=null){
				buffer.append(line+"\n");
			}
			String rtn = buffer.toString();
			System.out.println(rtn);
		} finally {
			if (ps != null) ps.destroy();
		}*/

		TestAddr.getAllLocalHostIP();  
		/*System.out.println("LocalIP:"+TestAddr.getLocalIP());  
		System.out.println("getWinLocalIP:"+TestAddr.getWinLocalIP());  
		System.out.println(TestAddr.getMacAddress());  */
	}  

}  