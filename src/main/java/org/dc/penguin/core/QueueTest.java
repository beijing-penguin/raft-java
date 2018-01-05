package org.dc.penguin.core;

import java.util.Comparator;
import java.util.PriorityQueue;

import org.dc.penguin.core.pojo.Message;
import org.dc.penguin.core.pojo.MessageQueue;

public class QueueTest {
	public static PriorityQueue<MessageQueue> queue = new PriorityQueue<MessageQueue>(new Comparator<MessageQueue>() {
		@Override
		public int compare(MessageQueue m1, MessageQueue m2) {
			return (int) (m1.getMessage().getDataIndex()-m2.getMessage().getDataIndex());
		}
	});
	
	public static void main(String[] args) {
		MessageQueue messageQueue = new MessageQueue();
		Message msg = new Message();
		msg.setDataIndex(2L);
		messageQueue.setMessage(msg);
		
		
		MessageQueue messageQueue1 = new MessageQueue();
		Message msg1 = new Message();
		msg1.setDataIndex(1L);
		messageQueue1.setMessage(msg1);
		
		
		queue.add(messageQueue);
		queue.add(messageQueue1);
		
		while (true) {
			MessageQueue queueMsg = queue.poll();
			if(queueMsg!=null) {
				System.out.println(queueMsg.getMessage().getDataIndex());
			}
		}
	}
}
