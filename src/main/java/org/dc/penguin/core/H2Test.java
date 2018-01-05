package org.dc.penguin.core;


import org.dc.jdbc.core.DBHelper;
import org.dc.penguin.core.pojo.Message;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSON;

public class H2Test {
	public static void main(String[] args) throws Exception {
		/*Server server = Server.createTcpServer("-tcpPort", "9123", "-tcpAllowOthers").start();
		System.out.println(server.getPort());*/
		
		DruidDataSource dataSource  =new DruidDataSource();
		dataSource.setUrl("jdbc:h2:file:./data/raft/raftdb_8881;AUTO_SERVER=TRUE");
		
		DBHelper dbHelper = new DBHelper(dataSource);
		//dbHelper.excuteSQL("create table RAFT_TABLE(id bigInt UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,key varchar(1000),value BLOB,data_index bigInt,term int)");
		//dbHelper.excuteSQL("DROP TABLE RAFT_TABLE");
		dbHelper.insert("insert into RAFT_TABLE(key,value,data_index,term) values('dc',null,5,1)");
		/*RaftData data = new RaftData();
		data.setDataIndex(1L);
		data.setKey("dc");
		data.setTerm(1);
		data.setValue("测试".getBytes());
		dbHelper.insertEntity(data);*/
		Message msg = dbHelper.selectOne("select * from RAFT_TABLE order by data_index desc limit 1",Message.class);
		System.out.println(JSON.toJSONString(msg));
		Long  count = dbHelper.selectOne("select CAST(IFNULL(max(data_index),0) as bigInt) from RAFT_TABLE",Long.class);
		System.out.println(count);
	}
}
