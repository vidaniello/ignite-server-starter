package com.github.vidaniello.igniteserver;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.ClusterMetricsMXBeanImpl;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.IgnitionMXBeanAdapter;
import org.apache.ignite.internal.processors.metric.GridMetricManager;
import org.apache.ignite.lang.IgniteProductVersion;
import org.apache.ignite.spi.IgniteSpi;
import org.apache.ignite.spi.IgniteSpiContext;
import org.apache.ignite.spi.IgniteSpiManagementMBean;
import org.apache.ignite.spi.discovery.DiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;

public class IgniteNode {
	
	private MainThread mainThread;
	
	private IgniteConfiguration conf;
	private Ignite igniteInstance;
	
	public IgniteNode(MainThread mainThread) {
		this.mainThread = mainThread;
		
		//Here the property before startup ignite node.
		//If the property must declared ad WM startup, refer to boot.sh/bat.
		//System.setProperty(key, value);
		System.setProperty("java.net.preferIPv4Stack", "true");
	}
	
	public void startNode() throws Exception{
		
		conf = new IgniteConfiguration();
		
		//ConsistentId
		conf.setConsistentId("Node_1_TEST");
		
		
		
		//Discovery section
		/*
		TcpDiscoverySpi spi = new TcpDiscoverySpi();
		TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
		ipFinder.setMulticastGroup("228.10.10.160");
		spi.setIpFinder(ipFinder);
		conf.setDiscoverySpi(spi);
		*/
		
		
		//Data regions section
		DataStorageConfiguration storageCfg = new DataStorageConfiguration();

		/*
		DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
		defaultRegion.setName("default_region");
		defaultRegion.setMaxSize((long)(500l * 1024l * 1024l)*16l);
		storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
		*/
		DataRegionConfiguration persistentDataregion = new DataRegionConfiguration();
		persistentDataregion.setName("persistent_region");
		persistentDataregion.setPersistenceEnabled(true);
		storageCfg.setDataRegionConfigurations(persistentDataregion);
		
		conf.setDataStorageConfiguration(storageCfg);
		
		
		//Starting
		igniteInstance = Ignition.start(conf);
		
		Thread.sleep(1000);
		
		if(!igniteInstance.cluster().state().active()) {
			igniteInstance.cluster().state(ClusterState.ACTIVE);
			System.out.println("Cluster state INACTIVE, set to ACTIVE");
		}
	}
	
	public void stopNode() {
		try {
			igniteInstance.close();
			
			
			igniteInstance = null;
			conf = null;
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public String getNodeStatus() {
		if(igniteInstance!=null) {
			
			IgniteConfiguration ic = igniteInstance.configuration();
			ClusterMetrics cm = igniteInstance.cluster().metrics();
			DiscoverySpi dspi = ic.getDiscoverySpi();
			IgniteProductVersion s = igniteInstance.version();
			
			int servers = 0;
			int clients = 0;
			for(ClusterNode cn : igniteInstance.cluster().nodes()) {
				if(cn.isClient())clients++; else servers++;
				
			}
			
			
			StringBuilder str = new StringBuilder();
			str.append("\n");
			
			
			
			long executionTime = cm.getUpTime();
			long days = TimeUnit.MILLISECONDS.toDays(executionTime);
			long hour = TimeUnit.MILLISECONDS.toHours(executionTime)-TimeUnit.DAYS.toHours(days);
			long minutes = TimeUnit.MILLISECONDS.toMinutes(executionTime)-TimeUnit.HOURS.toMinutes(hour);
			long seconds = TimeUnit.MILLISECONDS.toSeconds(executionTime)-TimeUnit.MINUTES.toSeconds(minutes);			
			String upTime = days+".days "+hour+".hours "+minutes+".mins "+seconds+".secs";
			
			str.append("Consistent ID---: "+ic.getConsistentId()+"\n");
			str.append("UUID------------: "+igniteInstance.cluster().localNode().id()+"\n");
			str.append("Node version----: "+igniteInstance.version()+"\n");
			str.append("Node start time-: "+new Date(cm.getNodeStartTime())+"\n");
			str.append("Uptime----------: "+upTime+"\n");
			str.append("Cluster state---: "+(igniteInstance.cluster().state().active() ? "ACTIVE" : "NOT ACTIVE")+"\n");
			
			
			str.append("\n");
			
			
			
			long gb = ((cm.getHeapMemoryUsed()/1024)/1024)/1024;
			long mb = ((cm.getHeapMemoryUsed()/1024)/1024)-(gb*1024);
			long kb = (cm.getHeapMemoryUsed()/1024)-(mb*1024);
			String usedHeap = gb<=0 ? mb+"."+kb+"MB" : gb+"."+mb+"GB";
			
			long tgb = (((cm.getHeapMemoryTotal()/1024)/1024)/1024);
			long tmb = ((cm.getHeapMemoryTotal()/1024)/1024)-(tgb*1024);
			String totalHeap = tgb+"."+tmb+"GB";
			
			/*
			long gb1 = ((cm.getNonHeapMemoryUsed() /1024)/1024)/1024;
			long mb1 = ((cm.getNonHeapMemoryUsed()/1024)/1024)-(gb1*1024);
			long kb1 = (cm.getNonHeapMemoryUsed()/1024)-(mb1*1024);
			String nonUsedHeap = gb1<=0 ? mb1+"."+kb1+"MB" : gb1+"."+mb1+"GB";
			
			long tgb1 = (((cm.getNonHeapMemoryTotal()/1024)/1024)/1024);
			long tmb1 = ((cm.getNonHeapMemoryTotal()/1024)/1024)-(tgb1*1024);
			String nonTotalHeap = tgb1+"."+tmb1+"GB";
			*/
			
			str.append("Total cpus------: "+cm.getTotalCpus()+"\n");
			str.append("Total memory----: "+totalHeap+"\n");
			str.append("Used memory-----: "+usedHeap+"\n");
			//str.append("Total required--: "+nonTotalHeap+"\n");
			//str.append("Required Used---: "+nonUsedHeap+"\n");
			str.append("Total servers---: "+servers+"\n");
			str.append("Total clients---: "+clients+"\n");
			
			
			
			str.append("\n");
			
			
			String cacheNames = "";
			if(!igniteInstance.cacheNames().isEmpty()) {
				for(String cacheName : igniteInstance.cacheNames())
					cacheNames += cacheName+", ";
				cacheNames = cacheNames.substring(0, cacheNames.length()-3);
			}
			
			
			
			str.append("Cache names-----: "+cacheNames+"\n");
			
			/*
			try {
				esploraMbean();
			}catch(Exception e) {e.printStackTrace();}
			*/
			return str.toString();
		}
		return "NODE STOPPED!";
	}
	
	/**
	 * Alcune procedure per esplorare il contesto server con MBeanServer
	 * @throws ReflectionException 
	 * @throws InstanceNotFoundException 
	 * @throws IntrospectionException 
	 */
	private void esploraMbean() throws IntrospectionException, InstanceNotFoundException, ReflectionException {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		//QueryExp query = Query.anySubString(Query.attr("protocol"), Query.value("Http11"));
		Set<ObjectName> objs = mbs.queryNames(null, null);
		
		for(ObjectName obN : objs) {
			
			String type = obN.getKeyProperty("type");
			MBeanInfo inf = mbs.getMBeanInfo(obN);
			System.out.println(inf.getClassName()+" "+obN.getKeyPropertyListString()+"("+inf.getDescription()+")");
			for(MBeanAttributeInfo attInfo : inf.getAttributes()) {
				System.out.println("\t"+attInfo.getName()+"("+attInfo.getType()+")"+attInfo.getDescription());
				try {
					System.out.println("\t\t"+(mbs.getAttribute(obN,attInfo.getName())!=null?mbs.getAttribute(obN,attInfo.getName()).toString():"null"));
				}catch(NullPointerException e) {
					System.err.println("\t\tnull");
				}catch(Exception e) {
					System.err.println("\t\t"+e.getMessage());
				}
			}
		}
	}
	
}
