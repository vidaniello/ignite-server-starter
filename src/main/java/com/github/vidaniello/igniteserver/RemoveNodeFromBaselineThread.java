package com.github.vidaniello.igniteserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cluster.BaselineNode;
import org.apache.ignite.cluster.ClusterNode;

public class RemoveNodeFromBaselineThread implements Runnable {

private static final int maxIterationWaitUntilNoseIsDown = 20;
	
	private Ignite ignite;

	private Serializable consistenId;
	
	public RemoveNodeFromBaselineThread() {
		
	}
	
	
	
	public RemoveNodeFromBaselineThread(Ignite ignite, Serializable consistenId) {
		super();
		this.ignite = ignite;
		this.consistenId = consistenId;
	}



	@Override
	public void run() {
		try {
			
			System.out.println("TRY REMOVE FROM BASELINE node ["+consistenId+"]");
			
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
						
						Thread.sleep(3000);
						waitUntilNodeIsDown();
						Thread.sleep(3000);
						
						System.out.println("Setting new baseline topology");
						ignite.cluster().setBaselineTopology(newBaseline);
						
						System.out.println("node ["+consistenId+"] dropped fom baseline.");
						
						
						Thread.sleep(5000);
						//Try resetLostPartitions caches who have lost partitions
						for(String cacheName : ignite.cacheNames()) {
							IgniteCache<?, ?> icache = ignite.cache(cacheName);
							if(icache.lostPartitions().size()>0)
								try {
									System.out.println("cache "+cacheName+" has lost partitions, try to resetLostPartitions...");
									ignite.resetLostPartitions(Arrays.asList(cacheName));
									System.out.println("cache "+cacheName+" resetLostPartitions complete!");
								}catch(Exception e) {
									System.err.println("cache "+cacheName+" resetLostPartitions error:");
									e.printStackTrace();
								}

						}
						
						
					}else
						throw new Exception("node ["+consistenId+"] not finded in the current baseline.");
				}
			
			}else
				throw new Exception("'null' consistenId not allowed!");
		
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void waitUntilNodeIsDown() throws Exception {
		
		int nIterations = 0;
		boolean isPresent = true;
		
		while(isPresent) {
			
			nIterations++;
			isPresent = false;
			
			for(ClusterNode cn : ignite.cluster().forServers().nodes()) 
				if(cn.consistentId().equals(consistenId)) {
					isPresent = true;
					break;
				}
			
			if(isPresent) {
				
				if(nIterations>maxIterationWaitUntilNoseIsDown)
					throw new Exception("Time exceded to wait cluset node ["+consistenId+"] to become offline.");
				
				System.out.println("Sleep waiting ["+consistenId+"] to become offline.");
				Thread.sleep(2000);
			}
			
		}
	}

}
