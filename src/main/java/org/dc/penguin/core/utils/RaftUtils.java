package org.dc.penguin.core.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.dc.jdbc.core.ConnectionManager;
import org.dc.jdbc.core.DbHelper;
import org.dc.penguin.core.NodeConfigInfo;
import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MessageQueue;
import org.dc.penguin.core.pojo.MsgType;
import org.dc.penguin.core.pojo.RoleType;
import org.dc.penguin.core.raft.NodeInfo;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSON;

public class RaftUtils {
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
			Enumeration<InetAddress> ni_enum = ni.getInetAddresses();  
			while (ni_enum.hasMoreElements()) {  
				ip = (InetAddress) ni_enum.nextElement();
				if (ip.getHostAddress().indexOf(":") == -1) {  
					ipSet.add(ip.getHostAddress());
				}  
			}  
		}
		return ipSet;
	}
	
	private static Map<String,SocketPool> socketPoolMap = new ConcurrentHashMap<String,SocketPool>();
	private static Lock lock = new ReentrantLock();
	public static SocketPool getSocketPool(String host , int port) {
		String key = host+":"+port;
		SocketPool pool = socketPoolMap.get(key);
		if(pool==null) {
			lock.lock();
			pool = socketPoolMap.get(key);
			if(pool==null) {
				pool = new SocketPool(host,port);
				GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
				poolConfig.setMaxTotal(10);
				GenericObjectPool<SocketConnection> objectPool = new GenericObjectPool<SocketConnection>(pool,poolConfig);
				
				pool.setObjectPool(objectPool);
				
				socketPoolMap.put(key, pool);
			}
			lock.unlock();
		}
		return pool;
	}
	private static Map<String,DbHelper> dbHelperMap = new ConcurrentHashMap<String,DbHelper>();
	public static DbHelper getDBHelper(String host , int port) {
		String key = host+":"+port;
		DbHelper dbHelper = dbHelperMap.get(key);
		if(dbHelper==null) {
			lock.lock();
			dbHelper = dbHelperMap.get(key);
			if(dbHelper==null) {
				DruidDataSource dataSource = new DruidDataSource();
				dataSource.setUrl("jdbc:h2:file:"+NodeConfigInfo.dataDir+"raftdb_"+port+"");
				dataSource.setFailFast(true);
				dataSource.setInitialSize(10);
				dataSource.setLoginTimeout(5);
				dataSource.setMaxActive(200);
				dataSource.setMinIdle(1);
				dataSource.setMaxWait(20000);
				dataSource.setValidationQuery("SELECT 1");
				dataSource.setTestOnBorrow(true);
				dataSource.setTestWhileIdle(true);
				dataSource.setPoolPreparedStatements(false);
				dbHelper = new DbHelper(dataSource);
				dbHelperMap.put(key, dbHelper);
			}
			lock.unlock();
		}
		return dbHelper;
	}
	
	public static String createLeaderKey(NodeInfo nodeInfo) {
		return nodeInfo.getHost()+":"+nodeInfo.getDataServerPort()+":"+nodeInfo.getElectionServerPort()+":"+nodeInfo.getTerm().get()+":"+nodeInfo.getDataIndex();
	}
	public static int getTerm(String leaderKey) {
		return Integer.parseInt(leaderKey.split(":")[3]);
	}
	
	public static void initNodeInfo(NodeInfo nodeInfo) throws Throwable {
		DbHelper dbHelper = RaftUtils.getDBHelper(nodeInfo.getHost(),nodeInfo.getDataServerPort());
		ConnectionManager.setReadOnly(true);//设置数据库查询只读事务
		Map<String, Object> rt_map = dbHelper.selectOne("select table_name from INFORMATION_SCHEMA.TABLES where table_name = 'RAFT_TABLE'");
		if(rt_map==null) {
			dbHelper.excuteSql("create table RAFT_TABLE(id bigInt PRIMARY KEY,key varchar(1000),value BLOB,data_index bigInt,term int)");
		}else {
			Message msgData = dbHelper.selectOne("select * from RAFT_TABLE order by data_index desc limit 1",Message.class);
			if(msgData!=null) {
				nodeInfo.setDataIndex(new AtomicLong(msgData.getDataIndex()));
				nodeInfo.setTerm(new AtomicInteger(msgData.getTerm()));
			}
		}
	}

	public static Message sendMessage(String host,int port,String data) throws Exception {
		SocketPool pool = RaftUtils.getSocketPool(host, port);
		SocketConnection conn = null;
		try {
			conn = pool.getSocketConnection();
			return JSON.parseObject(conn.sendMessage(data), Message.class);
		}catch (Exception e) {
			throw e;
		}finally {
			if(conn!=null) {
				conn.close();
			}
		}
	}

	public static NodeInfo getLeaderNodeInfo() {
		for (NodeInfo nodeInfo : NodeConfigInfo.nodeVector) {
			if(nodeInfo.getRole()==RoleType.LEADER) {
				return nodeInfo;
			}
		}
		return null;
	}

	public static void dataSync(NodeInfo leaderNode, NodeInfo node) throws Exception {
		//1.获取领导dataIndex
		Message msg = new Message();
		msg.setMsgCode(MsgType.GET_LEADER_LAST_DATAINDEX_POS);
		Message mm = RaftUtils.sendMessage(leaderNode.getHost(), leaderNode.getElectionServerPort(), msg.toJSONString());
		AtomicLong leaderIndex = new AtomicLong(Long.parseLong(new String(mm.getValue())));
		//2.批量同步数据
		
		
		//return true;
	}

	public static boolean dataSave(MessageQueue msgQue) throws Throwable {
		DbHelper dbHelper = RaftUtils.getDBHelper(msgQue.getNodeInfo().getHost(), msgQue.getNodeInfo().getDataServerPort());
		Long  max_dataIndex = dbHelper.selectOne("select CAST(IFNULL(max(data_index),0) as bigInt) from RAFT_TABLE",Long.class);
		if(max_dataIndex.longValue()==msgQue.getMessage().getDataIndex().longValue()-1) {
			dbHelper.insertEntity(msgQue.getNodeInfo());//插入本地
			Message msg_succ = new Message();
			msg_succ.setMsgCode(MsgType.SUCCESS);
			msgQue.getHandlerContext().channel().writeAndFlush(msg_succ.toJSONString());
			return true;
		}else {
			return false;
		}
	}
	
}
