package org.dc.penguin.core.raft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地机器对象
 * @author DC
 *
 */
public class NodeData {
	private Map<String,byte[]> data = new ConcurrentHashMap<String,byte[]>();

	public Map<String, byte[]> getData() {
		return data;
	}

	public void setData(Map<String, byte[]> data) {
		this.data = data;
	}
	
}