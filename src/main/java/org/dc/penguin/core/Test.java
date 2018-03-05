package org.dc.penguin.core;

import java.awt.Checkbox;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MsgType;
import org.dc.penguin.core.utils.RaftUtils;
import org.dc.penguin.core.utils.SocketConnection;
import org.dc.penguin.core.utils.SocketPool;

import com.alibaba.fastjson.JSON;

public class Test {
	public static void main(String[] args) throws Exception {
		/*SocketPool pool = RaftUtils.getSocketPool("192.168.1.104", 8881);
		SocketConnection conn = pool.getSocketConnection();
		Message ms =new Message();
		ms.setMsgCode(MsgType.CLIENT_SET_DATA);
		ms.setValue("asd".getBytes());
		System.out.println(JSON.toJSONString(conn.sendMessage(ms.toJSONString())));*/
		/*String s = "非非非";
        String gbkStr = new String(s.getBytes("gbk"), "gbk");
        System.out.println(gbkStr);*/
		//2a+7b+12c+24d+23e=123442
		int ev = 123;
		for(int d=0;d<=ev;d+=24) {
			for(int e=0;e<=ev-d;e+=23) {
				for (int c=0;c<=ev-d-e;c+=12) {
					for (int b=0;b<=ev-d-e-c;b+=7) {
						int remain = ev-b-c-d-e;
						if(remain%2==0) {
							int a = remain/2;
							System.out.format("a:%d,b:%d,c:%d,d:%d,e:%d%n",a,b/7,c/12,d/24,e/23);
						}
					}
				}
			}
		}
	}
}
