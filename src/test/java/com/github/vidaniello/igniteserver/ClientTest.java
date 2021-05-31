package com.github.vidaniello.igniteserver;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
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
			
			boolean cluster_state = igniteClient.cluster().state().active();
			
			CacheConfiguration<String, String> c1Cfg = new CacheConfiguration<>();
			c1Cfg.setName("CacheA");
			//c1Cfg.setDataRegionName("default region");
			IgniteCache<String, String> cache= igniteClient.getOrCreateCache(c1Cfg);
			
			String aValue = null;
			if(cache.containsKey("A")) {
				aValue = cache.get("A");
				cache.put("B", this.toString());
			}else {
				aValue = "value";
				cache.put("A", "value");
			}
			
			System.out.println("Key "+"A: "+aValue);
			System.out.println("Key "+"B: "+cache.get("B"));
			
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
