package org.dc.penguin.core;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dc.penguin.core.entity.ServerInfo;
import org.dc.penguin.core.utils.ConfigManager;

public class Commons {
	
	private static final Log LOG = LogFactory.getLog(Commons.class);
	
	public static ExecutorService threadPool = Executors.newFixedThreadPool(10);
	public static List<ServerInfo> serverList = new ArrayList<ServerInfo>();
	public static int start_port;
	
	
	static {
		try {
			
			Properties prop = ConfigManager.getInstance().loadProps("config.properties");
			start_port = Integer.parseInt(prop.getProperty("localPort"));
			
			for (Object key : prop.keySet()) {
				System.out.println(key);
				String pro_key = key.toString();
				if(pro_key.startsWith("server")){
					String value = prop.getProperty(pro_key);
					String host = value.split(":")[0];
					int port = Integer.parseInt(value.split(":")[1]);

					ServerInfo serverInfo = new ServerInfo();

					serverInfo.setHost(host);
					serverInfo.setPort(port);
					if((InetAddress.getByName("localhost").getHostAddress().equals("127.0.0.1") ||
							host.equalsIgnoreCase(InetAddress.getLocalHost().getHostAddress()))&& port == start_port){
						serverInfo.setLocalhost(true);
					}
					serverList.add(serverInfo);
				}
			}
		} catch (Exception e) {
			LOG.info("",e);
		}
	}
}
