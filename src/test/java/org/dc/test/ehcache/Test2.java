package org.dc.test.ehcache;

import java.util.concurrent.atomic.AtomicInteger;

public class Test2 {
	private AtomicInteger a = new AtomicInteger(1);
	public static void main(String[] args) throws InterruptedException {
		Test2 t2 = new Test2();
		Thread th = new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println(t2.getA());
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		th.start();
		Thread.sleep(1000);
		t2.setA(new AtomicInteger(5));
		new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println(t2.getA());
			}
		}).start();
	}
	public AtomicInteger getA() {
		return a;
	}
	public void setA(AtomicInteger a) {
		this.a = a;
	}


}
