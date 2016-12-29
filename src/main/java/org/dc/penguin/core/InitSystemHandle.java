package org.dc.penguin.core;

import java.net.InetAddress;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dc.penguin.core.raft.NettyRaftConnection;
import org.dc.penguin.core.utils.ConfigManager;
import org.dc.penguin.core.utils.Utils;

public class InitSystemHandle {
	public Vector<NettyRaftConnection> connVector = new Vector<NettyRaftConnection>();
	public ExecutorService threadPool = Executors.newFixedThreadPool(10);

	private static InitSystemHandle INSTANCE = new InitSystemHandle();
	private InitSystemHandle(){}
	public static InitSystemHandle getInstance(){
		return INSTANCE;
	}
	public void initConfig() throws Exception{
		Properties prop = ConfigManager.getInstance().loadProps("config.properties");
		for (Object key : prop.keySet()) {
			System.out.println(key);
			String pro_key = key.toString();
			if(pro_key.startsWith("server")){
				String value = prop.getProperty(pro_key);
				String host = value.split(":")[0];
				int port = Integer.parseInt(value.split(":")[1]);

				NettyRaftConnection connInfo = new NettyRaftConnection(host, port);
				if((InetAddress.getByName(host).getHostAddress().equals("127.0.0.1") || Utils.getAllLocalHostIP().contains(host))){
					connInfo.setLocalhost(true);
				}
				connVector.add(connInfo);
			}
		}
	}
}
