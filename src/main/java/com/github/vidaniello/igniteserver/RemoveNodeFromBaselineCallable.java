package com.github.vidaniello.igniteserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.BaselineNode;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;

public class RemoveNodeFromBaselineCallable implements IgniteCallable<String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
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
		String ret = "";
		if(consistenId!=null) {
			if(ignite.cluster().localNode().consistentId().equals(consistenId))
				throw new Exception("node ["+consistenId+"] try to exit itself from baseline.");
			else {
				Collection<BaselineNode> currentBaseline = ignite.cluster().currentBaselineTopology();
				Collection<BaselineNode> newBaseline = new ArrayList<>();
				
				boolean nodeDropped = false;
				for(BaselineNode bn : currentBaseline)
					if(!bn.consistentId().equals(consistenId))
						newBaseline.add(bn);
					else
						nodeDropped = true;
				
				if(nodeDropped) {
					
					Thread.sleep(5000);
					//TODO wait until node is closed inspecting the ignite.cluster().forServers().nodes() presence every x seconds, for max x iteration
					
					ignite.cluster().setBaselineTopology(newBaseline);
					ret = "node ["+consistenId+"] dropped fom baseline.";
					System.out.println(ret);
				}else
					throw new Exception("node ["+consistenId+"] not finded in the current baseline.");
			}
		
		}else
			throw new Exception("'null' consistenId not allowed!");
		
		return ret;
	}

}
