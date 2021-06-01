package com.github.vidaniello.igniteserver;

import java.util.Date;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.client.ClientAddressFinder;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.jetbrains.annotations.TestOnly;
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
				
				
				IgniteCache<?, ?> cacheX = igniteClient.cache("CacheX");
				if(cacheX==null) {
					//INITIALIZATION
				}
				
				
			}catch (Exception e) { e.printStackTrace();	}
			
			try {
			
				CacheConfiguration<String, String> cacheAcfg = new CacheConfiguration<>();
				cacheAcfg.setName("CacheA");
				
				IgniteCache<String, String> cacheA = igniteClient.getOrCreateCache(cacheAcfg);
				
				//CacheMetrics cm = cacheA.metrics();
				
				if(!cacheA.containsKey("A")) {
					System.out.println(cacheAcfg.getName()+" New created");
					cacheA.put("A", this.toString());
				}
				System.out.println("Cache "+cacheAcfg.getName()+", key A: "+cacheA.get("A"));
				cacheA.put("B", new Date().toString());
				
			}catch (Exception e) { e.printStackTrace();	}
			
			
			try {
				
				CacheConfiguration<String, String> cacheBcfg = new CacheConfiguration<>();
				cacheBcfg.setName("CacheB");
				
				IgniteCache<String, String> cacheB = igniteClient.getOrCreateCache(cacheBcfg);
				
				if(!cacheB.containsKey("A")) {
					System.out.println(cacheBcfg.getName()+" New created");
					cacheB.put("A", "A normal string value");
				}
				System.out.println("Cache "+cacheBcfg.getName()+", key A: "+cacheB.get("A"));
				cacheB.put("B", new Date().toString());
				
			}catch (Exception e) { e.printStackTrace();	}
			
			
			try {
				
				CacheConfiguration<String, String> cachePersistentAcfg = new CacheConfiguration<>();
				cachePersistentAcfg.setName("CachePersistentA");
				cachePersistentAcfg.setDataRegionName("persistent_region");
				
				IgniteCache<String, String> cachePersistentA = igniteClient.getOrCreateCache(cachePersistentAcfg);
				
				if(!cachePersistentA.containsKey("A")) {
					System.out.println(cachePersistentAcfg.getName()+" New created");
					cachePersistentA.put("A", "Persistent value in persistent cache in persistent data region");
				}
				System.out.println("Cache "+cachePersistentAcfg.getName()+", key A: "+cachePersistentA.get("A"));
				cachePersistentA.put("B", new Date().toString());
				
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
