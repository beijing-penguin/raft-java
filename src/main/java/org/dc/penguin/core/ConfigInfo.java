package org.dc.penguin.core;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;

import org.dc.penguin.core.raft.LocalMachine;
import org.dc.penguin.core.utils.ConfigManager;
import org.dc.penguin.core.utils.PropertiesUtil;
import org.dc.penguin.core.utils.SystemUtils;

public class ConfigInfo {
	private static Vector<LocalMachine> machineVector = new Vector<LocalMachine>();
	public static Vector<LocalMachine> getMachineVector() throws Exception{
		machineVector.clear();
		PropertiesUtil prop = ConfigManager.getInstance().loadProps("config.properties");
		Enumeration<Object> enum_obj = prop.keys();
		while (enum_obj.hasMoreElements()) {
			String pro_key =  enum_obj.nextElement().toString();
			if(pro_key.startsWith("server")){
				String[] value_arr = prop.getProperty(pro_key).split(":");
				String host = value_arr[0];
				
				LocalMachine machine = new LocalMachine();
				machine.setDataServerPort(Integer.parseInt(value_arr[1]));
				machine.setElectionServerPort(Integer.parseInt(value_arr[2]));
				machine.setHost(host);
				if((InetAddress.getByName(value_arr[0]).getHostAddress().equals("127.0.0.1") || SystemUtils.getAllLocalHostIP().contains(value_arr[0]))){
					machine.setLocalhost(true);
				}
				machineVector.add(machine);
			}
		}
		/*for (Enumeration<Object> key : ) {
			String pro_key = key.toString();
			if(pro_key.startsWith("server")){
				String[] value_arr = prop.getProperty(pro_key).split(":");
				String host = value_arr[0];
				
				LocalMachine machine = new LocalMachine();
				machine.setDataServerPort(Integer.parseInt(value_arr[1]));
				machine.setElectionServerPort(Integer.parseInt(value_arr[2]));
				machine.setHost(host);
				if((InetAddress.getByName(value_arr[0]).getHostAddress().equals("127.0.0.1") || SystemUtils.getAllLocalHostIP().contains(value_arr[0]))){
					machine.setLocalhost(true);
				}
				machineVector.add(machine);
			}
		}*/
		return machineVector;
	}
}
