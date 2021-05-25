package com.github.vidaniello.igniteserver;

import static org.apache.ignite.internal.IgniteComponentType.SPRING;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
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
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.ClusterMetricsMXBeanImpl;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.IgnitionMXBeanAdapter;
import org.apache.ignite.internal.processors.metric.GridMetricManager;
import org.apache.ignite.internal.util.spring.IgniteSpringHelper;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.lang.IgniteProductVersion;
import org.apache.ignite.spi.IgniteSpi;
import org.apache.ignite.spi.IgniteSpiContext;
import org.apache.ignite.spi.IgniteSpiManagementMBean;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.DiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;

public class IgniteNode {
	
	private MainThread mainThread;
	
	private IgniteConfiguration conf;
	private Ignite igniteInstance;
	
	
	private String consistentId = null;
	private URL springConfigurationURL;
	
	private String multicastDiscoveryGroupIpv4 = null;
	private Integer multicastDiscoveryGroupPort = null;

	private String tcpDiscoveryLocalAddress = null;
	private Integer tcpDiscoveryLocalPort = null;
	private Integer tcpDiscoveryLocalPortRange = null;
	
	private String tcpCommunicationLocalAddress = null;
	private Integer tcpCommunicationLocalPort = null;
	private Integer tcpCommunicationLocalPortRange = null;
	
	public IgniteNode(MainThread mainThread) {
		this.mainThread = mainThread;
		
		//Here the property before startup ignite node.
		//If the property must declared ad WM startup, refer to boot.sh/bat.
		//System.setProperty(key, value);
		System.setProperty("java.net.preferIPv4Stack", "true");
	}
	
	public void startNode() throws Exception{
		
		loadConfiguration();
		
		//Starting
		igniteInstance = Ignition.start(conf);
		
		Thread.sleep(1000);
		
		if(!igniteInstance.cluster().state().active()) {
			igniteInstance.cluster().state(ClusterState.ACTIVE);
			System.out.println("Cluster state INACTIVE, set to ACTIVE");
		}
		
	}
	
	private void loadConfiguration() throws Exception {
		
		String prependTestUserDir = "";
		if(System.getProperty("prependTestUserDir")!=null)
			prependTestUserDir = System.getProperty("prependTestUserDir");
				
		String ignite_node_properties_filename = "";
		if(System.getProperty("IGNITE_NODE_PROP_FILE")!=null)
			ignite_node_properties_filename = System.getProperty("IGNITE_NODE_PROP_FILE");
		
		//LOADING of node properties file
		Properties ignite_node_properties = new Properties();
		InputStream propertiesIs = null;
		try{
			
			try {
				URL igniteNodePropertiesUrl = new URL(ignite_node_properties_filename);
				propertiesIs = igniteNodePropertiesUrl.openStream();
			}catch(MalformedURLException mue) {
				File file = new File(prependTestUserDir+ignite_node_properties_filename);
				if(file.exists()) 
					propertiesIs = new FileInputStream(file);
				else				
					System.out.println("No IGNITE_NODE_PROP_FILE valid configuration found! Loading default ignite configuration server values.");
			}
			
			
			
			if(propertiesIs!=null){
				
				ignite_node_properties.load(propertiesIs);
				
				String temp = null;
				
				temp = ignite_node_properties.getProperty("consistentId");
				if(temp!=null)if(!temp.isEmpty())consistentId = temp;
				
				temp = ignite_node_properties.getProperty("springConfigurationURL");
				if(temp!=null)if(!temp.isEmpty()) 
					try {
						springConfigurationURL = new URL(temp);
					}catch(MalformedURLException mue) {
						File cfgFilePath = new File(prependTestUserDir+temp);
						if(cfgFilePath.exists()) 
							springConfigurationURL = cfgFilePath.toURI().toURL();
					}
				
				
				temp = ignite_node_properties.getProperty("multicastDiscoveryGroupIpv4");
				if(temp!=null)if(!temp.isEmpty())multicastDiscoveryGroupIpv4 = temp;
				
				temp = ignite_node_properties.getProperty("multicastDiscoveryGroupPort");
				if(temp!=null)if(!temp.isEmpty())multicastDiscoveryGroupPort = Integer.parseInt(temp);
				
				temp = ignite_node_properties.getProperty("tcpDiscoveryLocalAddress");
				if(temp!=null)if(!temp.isEmpty())tcpDiscoveryLocalAddress = temp;
				
				temp = ignite_node_properties.getProperty("tcpDiscoveryLocalPort");
				if(temp!=null)if(!temp.isEmpty())tcpDiscoveryLocalPort = Integer.parseInt(temp);
				
				temp = ignite_node_properties.getProperty("tcpDiscoveryLocalPortRange");
				if(temp!=null)if(!temp.isEmpty())tcpDiscoveryLocalPortRange = Integer.parseInt(temp);
				
				temp = ignite_node_properties.getProperty("tcpCommunicationLocalAddress");
				if(temp!=null)if(!temp.isEmpty())tcpCommunicationLocalAddress = temp;
				
				temp = ignite_node_properties.getProperty("tcpCommunicationLocalPort");
				if(temp!=null)if(!temp.isEmpty())tcpCommunicationLocalPort = Integer.parseInt(temp);
				
				temp = ignite_node_properties.getProperty("tcpCommunicationLocalPortRange");
				if(temp!=null)if(!temp.isEmpty())tcpCommunicationLocalPortRange = Integer.parseInt(temp);
			}
		}catch(Exception e) {
			System.err.println("ERROR reading configuration file");
			e.printStackTrace();
			throw e;
		}finally {
			if(propertiesIs!=null)
				propertiesIs.close();
		}
		
		
		
		if(springConfigurationURL!=null) {
			IgniteSpringHelper spring = SPRING.create(false);
			IgniteBiTuple<Collection<IgniteConfiguration>, ?> ib = spring.loadConfigurations(springConfigurationURL);
			Collection<IgniteConfiguration> icc = ib.get1();
			if(!icc.isEmpty()) 
				conf = icc.iterator().next();
		}else
			conf = new IgniteConfiguration();
		
		
		//ConsistentId
		if(consistentId!=null)
			conf.setConsistentId(consistentId);
		
		
		
		
		//Discovery section
		if(tcpDiscoveryLocalPortRange!=null || tcpDiscoveryLocalPort!=null || 
		   tcpDiscoveryLocalAddress!=null || multicastDiscoveryGroupIpv4!=null || multicastDiscoveryGroupPort!=null) {
			TcpDiscoverySpi spi = new TcpDiscoverySpi();
			
			if(tcpDiscoveryLocalAddress!=null) 
				spi.setLocalAddress(tcpDiscoveryLocalAddress);
			
			if(tcpDiscoveryLocalPort!=null) 
				spi.setLocalPort(tcpDiscoveryLocalPort);
			
			if(tcpDiscoveryLocalPortRange!=null) 
				spi.setLocalPortRange(tcpDiscoveryLocalPortRange);
			
			if(multicastDiscoveryGroupIpv4!=null || multicastDiscoveryGroupPort!=null) {
				TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
				
				if(multicastDiscoveryGroupIpv4!=null)
					ipFinder.setMulticastGroup(multicastDiscoveryGroupIpv4);
				
				if(multicastDiscoveryGroupPort!=null)
					ipFinder.setMulticastPort(multicastDiscoveryGroupPort);
				
				spi.setIpFinder(ipFinder);
			}
		
			conf.setDiscoverySpi(spi);
		}
		
		if(tcpCommunicationLocalAddress!=null || tcpCommunicationLocalPort!=null || tcpCommunicationLocalPortRange!=null) {
			TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
			
			if(tcpCommunicationLocalAddress!=null)
			commSpi.setLocalAddress(tcpCommunicationLocalAddress);
			
			if(tcpCommunicationLocalPort!=null)
				commSpi.setLocalPort(tcpCommunicationLocalPort);
			
			if(tcpCommunicationLocalPortRange!=null)
				commSpi.setLocalPortRange(tcpCommunicationLocalPortRange);
			
			conf.setCommunicationSpi(commSpi);
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
			long serversHeapMemoryMaximum = 0;
			long serversHeapMemoryUsed = 0;
			for(ClusterNode cn : igniteInstance.cluster().nodes()) {
				if(cn.isClient()) {
					clients++; 
				}else { 
					servers++;
					ClusterMetrics cmmmm = cn.metrics();
					serversHeapMemoryMaximum += cmmmm.getHeapMemoryMaximum();
					serversHeapMemoryUsed += cmmmm.getHeapMemoryUsed();
				}
				cn.metrics();
			}
			
			
			StringBuilder str = new StringBuilder();
			str.append("\n");
			
			
			
			
			
			str.append("Consistent ID---: "+ic.getConsistentId()+"\n");
			str.append("UUID------------: "+igniteInstance.cluster().localNode().id()+"\n");
			str.append("Node version----: "+igniteInstance.version()+"\n");
			
			
			
			str.append("\n");
			
			
			
			long executionTime = cm.getUpTime();
			long days = TimeUnit.MILLISECONDS.toDays(executionTime);
			long hour = TimeUnit.MILLISECONDS.toHours(executionTime)-TimeUnit.DAYS.toHours(days);
			long minutes = TimeUnit.MILLISECONDS.toMinutes(executionTime)-TimeUnit.HOURS.toMinutes(hour);
			long seconds = TimeUnit.MILLISECONDS.toSeconds(executionTime)-TimeUnit.MINUTES.toSeconds(minutes);			
			String upTime = days+".days "+hour+".hours "+minutes+".mins "+seconds+".secs";
			
			str.append("Grid start time-: "+new Date(cm.getNodeStartTime())+"\n");
			str.append("Grid uptime-----: "+upTime+"\n");
			str.append("Grid state------: "+(igniteInstance.cluster().state().active() ? "ACTIVE" : "NOT ACTIVE")+"\n");
			
			
			str.append("\n");
			
			
			/*
			long gb = ((cm.getHeapMemoryUsed()/1024)/1024)/1024;
			long mb = ((cm.getHeapMemoryUsed()/1024)/1024)-(gb*1024);
			long kb = (cm.getHeapMemoryUsed()/1024)-(mb*1024);
			String usedHeap = gb<=0 ? mb+"."+kb+"MB" : gb+"."+mb+"GB";
			*/
			/*
			long tgb = (((cm.getHeapMemoryTotal()/1024)/1024)/1024);
			long tmb = ((cm.getHeapMemoryTotal()/1024)/1024)-(tgb*1024);
			String totalHeap = tgb+"."+tmb+"GB";
			*/
			
			long gb = ((serversHeapMemoryUsed/1024)/1024)/1024;
			long mb = ((serversHeapMemoryUsed/1024)/1024)-(gb*1024);
			long kb = (serversHeapMemoryUsed/1024)-(mb*1024);
			String usedHeap = gb<=0 ? mb+"."+kb+"MB" : gb+"."+mb+"GB";
			
			long tgb = (((serversHeapMemoryMaximum/1024)/1024)/1024);
			long tmb = ((serversHeapMemoryMaximum/1024)/1024)-(tgb*1024);
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
			
			//long gbMaximum = ((cm.getHeapMemoryMaximum ()/1024)/1024)/1024;
			
			/**
			long mbhmc = (cm.getHeapMemoryCommitted()/1024)/1024;
			long mbhmi = (cm.getHeapMemoryInitialized()/1024)/1024;
			long mbhmm = (cm.getHeapMemoryMaximum()/1024)/1024;
			long mbhmt = (cm.getHeapMemoryTotal()/1024)/1024;
			long mbhmu = (cm.getHeapMemoryUsed()/1024)/1024;
			
			long mbnhmc = (cm.getNonHeapMemoryCommitted()/1024)/1024;
			long mbnhmi = (cm.getNonHeapMemoryInitialized()/1024)/1024;
			long mbnhmm = (cm.getNonHeapMemoryMaximum()/1024)/1024;
			long mbnhmt = (cm.getNonHeapMemoryTotal()/1024)/1024;
			long mbnhmu = (cm.getNonHeapMemoryUsed()/1024)/1024;
			
			str.append("getHeapMemoryCommitted--: "+mbhmc+"\n");
			str.append("getHeapMemoryInitialized: "+mbhmi+"\n");
			str.append("getHeapMemoryMaximum----: "+mbhmm+"\n");
			str.append("getHeapMemoryTotal------: "+mbhmt+"\n");
			str.append("getHeapMemoryUsed-------: "+mbhmu+"\n");
			
			str.append("getNonHeapMemoryCommitted--: "+mbnhmc+"\n");
			str.append("getNonHeapMemoryInitialized: "+mbnhmi+"\n");
			str.append("getNonHeapMemoryMaximum----: "+mbnhmm+"\n");
			str.append("getNonHeapMemoryTotal------: "+mbnhmt+"\n");
			str.append("getNonHeapMemoryUsed-------: "+mbnhmu+"\n");
			*/
						
			str.append("Node cpus-------: "+cm.getTotalCpus()+"\n");
			str.append("Grid Max RAM----: "+totalHeap+"\n");
			str.append("Grid used RAM---: "+usedHeap+"\n");
			str.append("Servers node----: "+servers+"\n");
			str.append("Clients node----: "+clients+"\n");
			
			
			
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
