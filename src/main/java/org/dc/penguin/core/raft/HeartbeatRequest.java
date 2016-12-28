package org.dc.penguin.core.raft;


import org.dc.penguin.core.entity.Message;

public class HeartbeatRequest extends Message {

	@Override
	public void setReqType(int reqType) {
		super.setReqType(reqType);
	}
}
