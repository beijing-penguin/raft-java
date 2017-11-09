package org.dc.penguin.core.raft;

import java.util.concurrent.atomic.AtomicInteger;

import org.dc.penguin.core.pojo.RoleType;

/**
 * 本机器配置信息
 * @author DC
 */
public class NodeInfo {
	private String host;
	private int dataServerPort;
	private int electionServerPort;
	private AtomicInteger haveVoteNum = new AtomicInteger(1);//
	private boolean isLocalhost;
	private int role = RoleType.FOLLOWER;//当前身份
	private AtomicInteger leaderPingNum = new AtomicInteger(0);
	private AtomicInteger voteTotalNum = new AtomicInteger(0);
	private String leaderKey;//当前leaderKey=ip+dataServerPort+electionServerPort
	private AtomicInteger dataIndex = new AtomicInteger(0);//数据索引
	private AtomicInteger term = new AtomicInteger(0);//任期
	
	
	public AtomicInteger getTerm() {
		return term;
	}
	public void setTerm(AtomicInteger term) {
		this.term = term;
	}
	public AtomicInteger getDataIndex() {
		return dataIndex;
	}
	public void setDataIndex(AtomicInteger dataIndex) {
		this.dataIndex = dataIndex;
	}
	public String getLeaderKey() {
		return leaderKey;
	}
	public void setLeaderKey(String leaderKey) {
		this.leaderKey = leaderKey;
	}
	public AtomicInteger getVoteTotalNum() {
		return voteTotalNum;
	}
	public void setVoteTotalNum(AtomicInteger voteTotalNum) {
		this.voteTotalNum = voteTotalNum;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getDataServerPort() {
		return dataServerPort;
	}
	public void setDataServerPort(int dataServerPort) {
		this.dataServerPort = dataServerPort;
	}

	public boolean isLocalhost() {
		return isLocalhost;
	}

	public void setLocalhost(boolean isLocalhost) {
		this.isLocalhost = isLocalhost;
	}

	public AtomicInteger getHaveVoteNum() {
		return haveVoteNum;
	}
	public void setHaveVoteNum(AtomicInteger haveVoteNum) {
		this.haveVoteNum = haveVoteNum;
	}
	public int getElectionServerPort() {
		return electionServerPort;
	}
	public void setElectionServerPort(int electionServerPort) {
		this.electionServerPort = electionServerPort;
	}
	public int getRole() {
		return role;
	}
	public void setRole(int role) {
		this.role = role;
	}
	public AtomicInteger getLeaderPingNum() {
		return leaderPingNum;
	}
	public void setLeaderPingNum(AtomicInteger leaderPingNum) {
		this.leaderPingNum = leaderPingNum;
	}
	
}