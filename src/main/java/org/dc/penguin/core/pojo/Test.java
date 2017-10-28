package org.dc.penguin.core.pojo;

import com.alibaba.fastjson.JSON;

public enum Test {
	A,B,C;
	public static void main(String[] args) {
		System.out.println(JSON.parseObject(JSON.toJSONString(new Demo()), Demo.class).getTt()==Test.A);
	}
}
class Demo{
	public Test tt = Test.A;

	public Test getTt() {
		return tt;
	}

	public void setTt(Test tt) {
		this.tt = tt;
	}
	
}