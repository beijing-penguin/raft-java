package org.dc.penguin.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class SocketCilentUtils {
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
}
