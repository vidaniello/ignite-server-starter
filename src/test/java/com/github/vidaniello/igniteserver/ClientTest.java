package com.github.vidaniello.igniteserver;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientAddressFinder;
import org.apache.ignite.client.IgniteClient;
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
	@Test //@Ignore
	public void testOne() {
		IgniteConfiguration conf = new IgniteConfiguration();
		conf.setClientMode(true);
		
		/*
		TcpDiscoverySpi spi = new TcpDiscoverySpi();
		TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
		ipFinder.setMulticastGroup("228.10.10.160");
		spi.setIpFinder(ipFinder);
		conf.setDiscoverySpi(spi);
		*/
		
		try(Ignite igniteClient = Ignition.start(conf);) {
			
			boolean cluster_state = igniteClient.cluster().state().active();
			
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
