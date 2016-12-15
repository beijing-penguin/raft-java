package org.dc.penguin;

public class NetworkContext {
	private static final ThreadLocal<NetworkContext> networkContext = new ThreadLocal<NetworkContext>();
	
	public static NetworkContext getContext(){
		NetworkContext context = networkContext.get();
		if(context==null){
			context = new NetworkContext();
			networkContext.set(context);
		}
		return context;
	}
	public static void closeContext(){
		networkContext.remove();
	}
}
