package org.dc.penguin.core;

import java.io.File;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;

import org.dc.penguin.core.raft.NodeInfo;
import org.dc.penguin.core.utils.ConfigManager;
import org.dc.penguin.core.utils.PropertiesUtil;
import org.dc.penguin.core.utils.SystemUtils;

import com.alibaba.fastjson.JSON;

public class NodeConfigInfo {
	static {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println(JSON.toJSONString(machineVector));
				}
			}
		}).start();
	}
	private static Vector<NodeInfo> machineVector = new Vector<NodeInfo>();
	
	public static String dataLogDir;
	
	public static Vector<NodeInfo> getNodeConfigList() throws Exception{
		if(machineVector.size()==0) {
			PropertiesUtil prop = ConfigManager.getInstance().loadProps("config.properties");
			Enumeration<Object> enum_obj = prop.keys();
			while (enum_obj.hasMoreElements()) {
				String pro_key =  enum_obj.nextElement().toString();
				if(pro_key.startsWith("server")){
					String[] value_arr = prop.getProperty(pro_key).split(":");
					String host = value_arr[0];

					NodeInfo nodeInfo = new NodeInfo();
					nodeInfo.setHost(host);
					nodeInfo.setDataServerPort(Integer.parseInt(value_arr[1]));
					nodeInfo.setElectionServerPort(Integer.parseInt(value_arr[2]));
					if((InetAddress.getByName(value_arr[0]).getHostAddress().equals("127.0.0.1") || SystemUtils.getAllLocalHostIP().contains(value_arr[0]))){
						nodeInfo.setLocalhost(true);
					}
					machineVector.add(nodeInfo);
				}
			}
		}
		return machineVector;
	}
	public static void initConfig() throws Exception {
		ConfigManager.getInstance().loadProps("config.properties");
		dataLogDir = ConfigManager.getInstance().get("config.properties", "dataLogDir");
		File file = new File(dataLogDir);
		if(!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		if(!file.exists()) {
			file.createNewFile();
		}
	}
}
