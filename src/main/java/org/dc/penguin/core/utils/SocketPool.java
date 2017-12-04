package org.dc.penguin.core.utils;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MsgType;

public class SocketPool implements PooledObjectFactory<SocketConnection> {
	
	private String host;
	private int port;
	
	public SocketPool(String host, int port){
		this.host = host;
		this.port = port;
	}
	private GenericObjectPool<SocketConnection> objectPool;
	
	@Override
	public void activateObject(PooledObject<SocketConnection> arg0) throws Exception {
		SocketConnection conn = arg0.getObject();
		conn.sendUrgentData(0xFF);
	}

	@Override
	public void destroyObject(PooledObject<SocketConnection> arg0) throws Exception {
		SocketConnection conn = arg0.getObject();
		conn.destroy();
	}

	@Override
	public PooledObject<SocketConnection> makeObject() throws Exception {
		SocketConnection conn = new SocketConnection(host, port);
		conn.setObjectPool(objectPool);
		return new DefaultPooledObject<SocketConnection>(conn);
	}

	@Override
	public void passivateObject(PooledObject<SocketConnection> arg0) throws Exception {
		//System.out.println("passivate Object");
	}

	@Override
	public boolean validateObject(PooledObject<SocketConnection> arg0) {
		System.out.println("validate Object");
		return true;
	}  

	public SocketConnection getSocketConnection() throws Exception {
		return objectPool.borrowObject();
	}
	

	public GenericObjectPool<SocketConnection> getObjectPool() {
		return objectPool;
	}

	public void setObjectPool(GenericObjectPool<SocketConnection> objectPool) {
		this.objectPool = objectPool;
	}

	public static void main(String[] args) throws Exception {
		SocketPool pool = new SocketPool("192.168.1.109",8880);
		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
		poolConfig.setMaxTotal(5);
		GenericObjectPool<SocketConnection> objectPool = new GenericObjectPool<SocketConnection>(pool,poolConfig);
		
		pool.setObjectPool(objectPool);
		
		for (int i = 0; i < 10; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					SocketConnection conn = null;
					try {
						conn = pool.getSocketConnection();
						Message ms = new Message();
						ms.setMsgCode(MsgType.SET_DATA);
						conn.sendMessage(ms);
						conn.close();
					} catch (Exception e) {
						e.printStackTrace();
						conn.destroy();
					}
				}
			}).start();
		}
	}
}  