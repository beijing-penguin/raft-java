package org.dc.penguin.core;

import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MsgType;
import org.dc.penguin.core.utils.SocketCilentUtils;
import org.dc.penguin.core.utils.SocketConnection;
import org.dc.penguin.core.utils.SocketPool;

import com.alibaba.fastjson.JSON;

public class Test {
	public static void main(String[] args) throws Exception {
		SocketPool pool = SocketCilentUtils.getSocketPool("192.168.1.104", 8881);
		SocketConnection conn = pool.getSocketConnection();
		Message ms =new Message();
		ms.setMsgCode(MsgType.CLIENT_SET_DATA);
		ms.setLeaderKey("192.168.1.104:8881:7771:11:0");
		ms.setValue("asd".getBytes());
		System.out.println(JSON.toJSONString(conn.sendMessage(ms.toJSONString())));
	}
}
