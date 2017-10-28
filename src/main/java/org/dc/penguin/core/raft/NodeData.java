package org.dc.penguin.core.raft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 本地机器对象
 * @author DC
 *
 */
public class NodeData {
	private static Log LOG = LogFactory.getLog(NodeData.class);
	private Map<String,byte[]> data = new ConcurrentHashMap<String,byte[]>();
}