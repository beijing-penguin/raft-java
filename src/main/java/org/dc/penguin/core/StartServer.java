package org.dc.penguin.core;

import java.net.InetAddress;
import java.util.Properties;

import org.dc.penguin.core.raft.RaftServer;
import org.dc.penguin.core.utils.ConfigManager;
import org.dc.penguin.core.utils.SystemUtils;


public class StartServer {
	public static void main(String[] args) throws Exception {
		ConfigInfo config = ConfigInfo.getInstance();
		Properties prop = ConfigManager.getInstance().loadProps("config.properties");
		for (Object prop_key : prop.keySet()) {
			String key = prop_key.toString();
			if(key.contains("server")){
				String value = prop.getProperty(key);
				String[] value_arr = value.split(":");
				System.out.println();
				if(SystemUtils.getAllLocalHostIP().contains(value_arr[0]) || SystemUtils.getAllLocalHostIP().contains(InetAddress.getByName("localhost").getHostAddress())){
					//开启数据同步端口和领导选举与心跳同步端口
					new RaftServer().startDataServer(Integer.parseInt(value_arr[1]));
					new RaftServer().startElectionServer(Integer.parseInt(value_arr[2]));
					
				}
			}
		}
		
	}
}
