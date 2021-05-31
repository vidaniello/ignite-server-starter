package com.github.vidaniello.igniteserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.BaselineNode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;

public class RemoveNodeFromBaselineCallable implements IgniteCallable<String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final int maxIterationWaitUntilNoseIsDown = 20;
	
	@IgniteInstanceResource
	private Ignite ignite;

	private Serializable consistenId;
	
	public RemoveNodeFromBaselineCallable() {
		
	}
	
	
	public RemoveNodeFromBaselineCallable(Serializable consistenId) {
		super();
		this.consistenId = consistenId;
	}


	@Override
	public String call() throws Exception {
		new Thread(new RemoveNodeFromBaselineThread(ignite, consistenId)).start();
		return "Request accepted! removing of ["+consistenId+"] node instance from current baseline in progress...";
	}
	
	


}
