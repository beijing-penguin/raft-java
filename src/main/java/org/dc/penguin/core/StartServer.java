package org.dc.penguin.core;

import org.dc.penguin.core.raft.LocalStateMachine;


public class StartServer {

	public static void main(String[] args) throws Exception {
		ConfigInfo.initConfig();
		for (LocalStateMachine localStateMachine: ConfigInfo.dataServerVector) {
			localStateMachine.startDataServer(localStateMachine.getPort());
		}
		for (LocalStateMachine localStateMachine: ConfigInfo.electionServerVector) {
			localStateMachine.startElectionServer(localStateMachine.getPort());
		}
	}
}
