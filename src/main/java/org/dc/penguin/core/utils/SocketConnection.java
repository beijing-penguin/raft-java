package org.dc.penguin.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class SocketConnection {
	private static Log LOG = LogFactory.getLog(SocketConnection.class);

	private BufferedReader br;
	private InputStreamReader isr;
	private InputStream is;
	private PrintWriter pw;
	private OutputStream ots;
	private Socket socket;
	private GenericObjectPool<SocketConnection> objectPool;

	public SocketConnection(String host, int port) throws Exception {
		//创建一个客户端socket
		socket = new Socket(host,port);
	}
	public String sendMessage(String data) throws Exception{
		try {
			//向服务器端传递信息
			ots = socket.getOutputStream();
			pw = new PrintWriter(ots);
			pw.write(data);
			pw.flush();
			//关闭输出流
			socket.shutdownOutput();
			//获取服务器端传递的数据
			is = socket.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String info = null;
			while((info=br.readLine())!=null){
				return info;
			}
		}catch (Exception e) {
			this.destroy();
			throw e;
		}
		return null;
	}
	public void close() {
		objectPool.returnObject(this);
	}
	public void destroy() {
		if(br!=null) {
			try {
				br.close();
			}catch (Exception e) {
				LOG.error("",e);
			}
		}
		if(isr!=null) {
			try {
				isr.close();
			}catch (Exception e) {
				LOG.error("",e);
			}
		}
		if(is!=null) {
			try {
				is.close();
			}catch (Exception e) {
				LOG.error("",e);
			}
		}
		if(pw!=null) {
			try {
				pw.close();
			}catch (Exception e) {
				LOG.error("",e);
			}
		}
		if(ots!=null) {
			try {
				ots.close();
			}catch (Exception e) {
				LOG.error("",e);
			}
		}
		if(socket!=null) {
			try {
				socket.close();
				socket=null;
			}catch (Exception e) {
				LOG.error("",e);
			}
		}
	}

	public GenericObjectPool<SocketConnection> getObjectPool() {
		return objectPool;
	}
	public void setObjectPool(GenericObjectPool<SocketConnection> objectPool) {
		this.objectPool = objectPool;
	}
	public void sendUrgentData(int i) throws IOException {
		socket.sendUrgentData(i);
	}
}