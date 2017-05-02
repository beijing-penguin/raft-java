package org.dc.penguin.core;

import java.net.InetAddress;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dc.penguin.core.raft.LocalStateMachine;
import org.dc.penguin.core.utils.ConfigManager;
import org.dc.penguin.core.utils.SystemUtils;

public class ConfigInfo {
	private Vector<LocalStateMachine> electionServerVector = new Vector<LocalStateMachine>();
	private Vector<LocalStateMachine> dataServerVector = new Vector<LocalStateMachine>();
	public ExecutorService threadPool = Executors.newFixedThreadPool(10);

	private static ConfigInfo INSTANCE = new ConfigInfo();
	private ConfigInfo(){}
	public static ConfigInfo getInstance(){
		return INSTANCE;
	}
	public void initConfig() throws Exception{
		Properties prop = ConfigManager.getInstance().loadProps("config.properties");
		for (Object key : prop.keySet()) {
			System.out.println(key);
			String pro_key = key.toString();
			if(pro_key.startsWith("server")){
				String[] value_arr = prop.getProperty(pro_key).split(":");
				String host = value_arr[0];
				
				LocalStateMachine machine = new LocalStateMachine();
				machine.setPort(Integer.parseInt(value_arr[1]));
				machine.setHost(host);
				if((InetAddress.getByName(value_arr[0]).getHostAddress().equals("127.0.0.1") || SystemUtils.getAllLocalHostIP().contains(value_arr[0]))){
					machine.setLocalhost(true);
				}
				
				LocalStateMachine machine2 = new LocalStateMachine();
				machine2.setPort(Integer.parseInt(value_arr[2]));
				machine2.setHost(host);
				if((InetAddress.getByName(host).getHostAddress().equals("127.0.0.1") || SystemUtils.getAllLocalHostIP().contains(host))){
					machine2.setLocalhost(true);
				}
				
				
				electionServerVector.add(machine2);
				dataServerVector.add(machine);
			}
		}
	}
	public Vector<LocalStateMachine> getConnVector() throws Exception {
		return electionServerVector;
	}
	public void setConnVector(Vector<LocalStateMachine> electionServerVector) {
		this.electionServerVector = electionServerVector;
	}
}
