package com.github.vidaniello.igniteserver;

import java.util.Arrays;
import java.util.Date;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.processors.cache.CacheInvalidStateException;
import org.junit.Ignore;
import org.junit.Test;

public class ClientTest {

	/**
	 * On ignite started instance, execute this test
	 */
	@Test @Ignore
	public void testOne() {
		IgniteConfiguration conf = new IgniteConfiguration();
		conf.setClientMode(true);
		
		conf.setPeerClassLoadingEnabled(true);
		
		/*
		TcpDiscoverySpi spi = new TcpDiscoverySpi();
		TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
		ipFinder.setMulticastGroup("228.10.10.160");
		spi.setIpFinder(ipFinder);
		conf.setDiscoverySpi(spi);
		*/
		
		try(Ignite igniteClient = Ignition.start(conf);) {
			
			//boolean cluster_state = igniteClient.cluster().state().active();
			
			
			try {
				
				String cacheName = "CacheX";
				IgniteCache<?, ?> cacheX = igniteClient.cache("CacheX");
				
				if(cacheX==null) {
					//INITIALIZATION
				}
				
				
			}catch (Exception e) { e.printStackTrace();	}
			
			try {
				String cacheName = "CacheInMemoryA";
 				CacheConfiguration<String, String> cachecfg = new CacheConfiguration<>();
				cachecfg.setName(cacheName);
				cachecfg.setBackups(1);
				
				
				IgniteCache<String, String> cache = igniteClient.getOrCreateCache(cachecfg);
				
				try {
					cache.containsKey("A");
				}catch(IgniteException ex) {
					if(ex.getCause() instanceof CacheInvalidStateException) {
						int before = cache.lostPartitions().size();
						igniteClient.resetLostPartitions(Arrays.asList(cacheName));
						int after = cache.lostPartitions().size();
						
						int i = 0;
					}
				}
				
				//CacheMetrics cm = cacheA.metrics();
				
				if(!cache.containsKey("A")) {
					System.out.println(cachecfg.getName()+" New created");
					cache.put("A", this.toString());
				}
				System.out.println("Cache "+cachecfg.getName()+", key A: "+cache.get("A"));
				cache.put("B", new Date().toString());
				
			}catch (Exception e) { e.printStackTrace();	}
			
			
			try {
				String cacheName = "CacheInMemoryB";
				CacheConfiguration<String, String> cachecfg = new CacheConfiguration<>();
				cachecfg.setName(cacheName);
				
				
				IgniteCache<String, String> cache = igniteClient.getOrCreateCache(cachecfg);
				
				/*
				try {
					cache.containsKey("A");
				}catch(IgniteException ex) {
					if(ex.getCause() instanceof CacheInvalidStateException) {
				*/
				if(cache.lostPartitions().size()>0) {
					int before = cache.lostPartitions().size();
					igniteClient.resetLostPartitions(Arrays.asList(cacheName));
					int after = cache.lostPartitions().size();
				}
						
				/*	
						int i = 0;
					}
				}
				*/
				
				if(!cache.containsKey("A")) {
					System.out.println(cachecfg.getName()+" New created");
					cache.put("A", "A normal string value");
				}
				System.out.println("Cache "+cachecfg.getName()+", key A: "+cache.get("A"));
				cache.put("B", new Date().toString());
				
			}catch (Exception e) { e.printStackTrace();	}
			
			
			try {
				String cacheName = "CachePersistentA";
				CacheConfiguration<String, String> cachecfg = new CacheConfiguration<>();
				cachecfg.setName(cacheName);
				cachecfg.setDataRegionName("persistent_region");
				cachecfg.setBackups(1);
				
				IgniteCache<String, String> cache = igniteClient.getOrCreateCache(cachecfg);
				
				try {
					cache.containsKey("A");
				}catch(IgniteException ex) {
					if(ex.getCause() instanceof CacheInvalidStateException) {
						
						try {
							
							int before = cache.lostPartitions().size();
							igniteClient.resetLostPartitions(Arrays.asList(cacheName));
							int after = cache.lostPartitions().size();
							
							
							int i = 0;
							
						}catch(Exception e) {
							e.printStackTrace();
						}
						
					}
				}
				
				if(!cache.containsKey("A")) {
					System.out.println(cachecfg.getName()+" New created");
					cache.put("A", "Persistent value in persistent cache in persistent data region");
				}
				System.out.println("Cache "+cachecfg.getName()+", key A: "+cache.get("A"));
				cache.put("B", new Date().toString());
				
			}catch (Exception e) { e.printStackTrace();	}

			
			int i = 0;
			
			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * On ignite started instance, execute this test
	 */
	@Test @Ignore
	public void testThinClient() {
		ClientConfiguration cc = new ClientConfiguration().setAddresses(/*"172.16.16.90",*/"172.16.16.60").setPartitionAwarenessEnabled(true);
		
		try(IgniteClient igniteClient = Ignition.startClient(cc);) {
			
			boolean cluster_state = igniteClient.cluster().state().active();
			
			int i = 0;
			
			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
