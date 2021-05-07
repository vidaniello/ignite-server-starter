package com.github.vidaniello.igniteserver;

public class IgniteNode {
	
	private MainThread mainThread;
	
	public IgniteNode(MainThread mainThread) {
		this.mainThread = mainThread;
		
		//Here the property before startup ignite node
		//System.setProperty(key, value);
	}
	
	
	public void startNode() throws Exception{
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void stopNode() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getNodeStatus() {
		return "NODE ACTIVE!";
	}
	
}
