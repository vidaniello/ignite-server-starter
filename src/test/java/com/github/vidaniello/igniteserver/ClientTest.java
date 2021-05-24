package com.github.vidaniello.igniteserver;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.jetbrains.annotations.TestOnly;
import org.junit.Test;

public class ClientTest {

	/**
	 * On ignite started instance, execute this test
	 */
	@Test
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
			
			
			int i = 0;
			
			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
