package com.github.vidaniello.igniteserver;

import static org.apache.ignite.internal.IgniteComponentType.SPRING;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.BaselineNode;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.processors.cluster.baseline.autoadjust.BaselineAutoAdjustStatus;
import org.apache.ignite.internal.util.spring.IgniteSpringHelper;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.lang.IgniteProductVersion;
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
	
	private Boolean activeClusterAfterNodeStart = null;
	private Boolean baselineAutoAdjustEnabled = null;
	private Long baselineAutoAdjustTimeout = null;
	private String clusterTag = null;
	
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
		
		
		if(activeClusterAfterNodeStart!=null || baselineAutoAdjustEnabled!=null || baselineAutoAdjustTimeout!=null) {
		
			Thread.sleep(1000);
			
			if(activeClusterAfterNodeStart!=null)
				if(!activeClusterAfterNodeStart.equals(igniteInstance.cluster().state().active())){
						Thread.sleep(5000);
						igniteInstance.cluster().state(activeClusterAfterNodeStart?ClusterState.ACTIVE:ClusterState.INACTIVE);
						System.out.println("Cluster state from "+(activeClusterAfterNodeStart?ClusterState.INACTIVE:ClusterState.ACTIVE) + " to " + (activeClusterAfterNodeStart?ClusterState.ACTIVE:ClusterState.INACTIVE));
					}
			
			if(baselineAutoAdjustEnabled!=null)		
				if(!baselineAutoAdjustEnabled.equals(igniteInstance.cluster().isBaselineAutoAdjustEnabled())){
					Thread.sleep(3000);
					igniteInstance.cluster().baselineAutoAdjustEnabled(baselineAutoAdjustEnabled);
					System.out.println("Cluster baselineAutoAdjustEnabled from "+(!baselineAutoAdjustEnabled)+" to "+baselineAutoAdjustEnabled);
				}
			
			if(baselineAutoAdjustTimeout!=null)
				if(!baselineAutoAdjustTimeout.equals(igniteInstance.cluster().baselineAutoAdjustTimeout())) {
					long old = igniteInstance.cluster().baselineAutoAdjustTimeout();
					Thread.sleep(3000);
					igniteInstance.cluster().baselineAutoAdjustTimeout(baselineAutoAdjustTimeout);
					System.out.println("Cluster baselineAutoAdjustTimeout from "+old+" to "+baselineAutoAdjustTimeout+" mills");
				}
		}
		
		checkIgniteTag();
		
	}
	
	private void checkIgniteTag() {
		if(igniteInstance.cluster().state().active()) {
			
			if(clusterTag!=null)
				if(!clusterTag.isEmpty()) {
					
					
					String corrected = clusterTag;
					if(clusterTag.length()>IgniteCluster.MAX_TAG_LENGTH) 
						corrected = clusterTag.substring(0, IgniteCluster.MAX_TAG_LENGTH-1);
					
					
					if(!corrected.equals(igniteInstance.cluster().tag())){
						String old = igniteInstance.cluster().tag();
						
						try {
							
							igniteInstance.cluster().tag(corrected);
						
							System.out.println("Cluster tag from '"+old+"' to '"+corrected+"'");
							
						} catch (IgniteCheckedException e) {}
					}
				}
			
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
				
				temp = ignite_node_properties.getProperty("activeClusterAfterNodeStart");
				if(temp!=null)if(!temp.isEmpty())activeClusterAfterNodeStart = Boolean.parseBoolean(temp);
				
				temp = ignite_node_properties.getProperty("baselineAutoAdjustEnabled");
				if(temp!=null)if(!temp.isEmpty())baselineAutoAdjustEnabled = Boolean.parseBoolean(temp);
				
				temp = ignite_node_properties.getProperty("baselineAutoAdjustTimeout");
				if(temp!=null)if(!temp.isEmpty())baselineAutoAdjustTimeout = Long.parseLong(temp);

				temp = ignite_node_properties.getProperty("clusterTag");
				if(temp!=null)if(!temp.isEmpty())clusterTag = temp;
				
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
	
	public String switchClusterState() {
		String outMessage = "";
		try {
			if(igniteInstance!=null) {
				if(igniteInstance.cluster().state()==ClusterState.ACTIVE || igniteInstance.cluster().state()==ClusterState.ACTIVE_READ_ONLY) {
					outMessage = "cluster state from "+igniteInstance.cluster().state().toString()+" to "+ClusterState.INACTIVE.toString()+"\n";
					igniteInstance.cluster().state(ClusterState.INACTIVE);
					System.out.println(outMessage);
					Thread.sleep(3000);
					outMessage += "SUCCESS, new cluster state "+igniteInstance.cluster().state();
				} else {
					outMessage = "cluster state from "+igniteInstance.cluster().state().toString()+" to "+ClusterState.ACTIVE.toString()+"\n";
					igniteInstance.cluster().state(ClusterState.ACTIVE);
					System.out.println(outMessage);
					Thread.sleep(3000);
					outMessage += "SUCCESS, new cluster state "+igniteInstance.cluster().state();
					checkIgniteTag();
				}
			}
		}catch(IgniteException e) {
			e.printStackTrace();
			outMessage += e.getClass().getCanonicalName()+" - "+e.getMessage()+"\n";
		}catch(Exception e) {
			e.printStackTrace();
			outMessage += e.getClass().getCanonicalName()+" - "+e.getMessage()+"\n";
		}
		return outMessage;
	}

	public String getNodeStatus() {
		if(igniteInstance!=null) {
			
			IgniteConfiguration ic = igniteInstance.configuration();
			ClusterMetrics cm = igniteInstance.cluster().metrics();
			DiscoverySpi dspi = ic.getDiscoverySpi();
			IgniteProductVersion s = igniteInstance.version();
			
			//Refer to mac address, add only the same processors of server instances
			Map<String,Integer> macs_processors = new HashMap<>(); 
			
			Collection<ClusterNode> serverNodes = new ArrayList<>();
			
			int servers = 0;
			int clients = 0;
			long serversHeapMemoryMaximum = 0;
			long serversHeapMemoryUsed = 0;
			for(ClusterNode cn : igniteInstance.cluster().nodes()) {
				
				if(cn.isClient()) {
					clients++; 
				}else { 
					serverNodes.add(cn);
					servers++;
					ClusterMetrics cmmmm = cn.metrics();
					serversHeapMemoryMaximum += cmmmm.getHeapMemoryMaximum();
					serversHeapMemoryUsed += cmmmm.getHeapMemoryUsed();
					macs_processors.put(cn.attribute("org.apache.ignite.macs"), cmmmm.getTotalCpus());
				}
				
			}
			
			
			StringBuilder str = new StringBuilder();
			str.append("\n");
			
			
			
			
			
			str.append("Node consistent ID---------: "+ic.getConsistentId()+"\n");
			str.append("Node UUID------------------: "+igniteInstance.cluster().localNode().id()+"\n");
			str.append("Node version---------------: "+igniteInstance.version()+"\n");
			
			
			
			str.append("\n");
			
			
			
			long executionTime = cm.getUpTime();
			long days = TimeUnit.MILLISECONDS.toDays(executionTime);
			long hour = TimeUnit.MILLISECONDS.toHours(executionTime)-TimeUnit.DAYS.toHours(days);
			long minutes = TimeUnit.MILLISECONDS.toMinutes(executionTime)-TimeUnit.HOURS.toMinutes(hour);
			long seconds = TimeUnit.MILLISECONDS.toSeconds(executionTime)-TimeUnit.MINUTES.toSeconds(minutes);			
			String upTime = days+".days "+hour+".hours "+minutes+".mins "+seconds+".secs";
			
			str.append("Cluster ID-----------------: "+igniteInstance.cluster().id()+"\n");
			str.append("Cluster tag----------------: "+igniteInstance.cluster().tag()+"\n");
			str.append("Cluster start time---------: "+new Date(cm.getNodeStartTime())+"\n");
			str.append("Cluster uptime-------------: "+upTime+"\n");
			str.append("Cluster state--------------: "+(igniteInstance.cluster().state().active() ? "ACTIVE" : "NOT ACTIVE")+"\n");
			
			BaselineAutoAdjustStatus bas = igniteInstance.cluster().baselineAutoAdjustStatus();
			str.append("BaselineAutoAdjustStatus---: "+bas.getTaskState()+"\n");
			str.append("BaselineAutoAdjustEnabled--: "+igniteInstance.cluster().isBaselineAutoAdjustEnabled()+"\n");
			
			
			
			str.append("\n");
			
			
			long gb = ((serversHeapMemoryUsed/1024)/1024)/1024;
			long mb = ((serversHeapMemoryUsed/1024)/1024)-(gb*1024);
			String usedHeap = gb+"."+mb+"GB";
			
			long tgb = (((serversHeapMemoryMaximum/1024)/1024)/1024);
			long tmb = ((serversHeapMemoryMaximum/1024)/1024)-(tgb*1024);
			String totalHeap = tgb+"."+tmb+"GB";
			
			long freeRamBytes = serversHeapMemoryMaximum-serversHeapMemoryUsed;
			long freeRamGB = ((freeRamBytes/1024)/1024)/1024;
			long freeRamMB = ((freeRamBytes/1024)/1024)-(freeRamGB*1024);
			String freeRam = freeRamGB+"."+freeRamMB+"GB";
			
			int totCpus = macs_processors.values().stream().mapToInt(Integer::intValue).sum();
						
			str.append("Cluster CPUs---------------: "+totCpus+"\n");
			str.append("Cluster Tot.RAM------------: "+totalHeap+"\n");
			str.append("Cluster used RAM-----------: "+usedHeap+"\n");
			str.append("Cluster free RAM-----------: "+freeRam+"\n");
			str.append("Cluster servers------------: "+servers+"\n");
			str.append("Cluster clients------------: "+clients+"\n");
			
			//org.apache.ignite.data.regions.offheap.size
			//org.apache.ignite.offheap.size
			
			
			
			Collection<BaselineNode> onlineNodes = igniteInstance.cluster().currentBaselineTopology();
			if(onlineNodes==null)onlineNodes = new ArrayList<>();
			StringBuilder onlineNodesStr = new StringBuilder();
			
			Collection<BaselineNode> offlineNodes = new ArrayList<>();
			StringBuilder offlineNodesStr = new StringBuilder();
			
			
			for(BaselineNode bs : onlineNodes) 
				onlineNodesStr.append("("+bs.consistentId()+")");	
			
			for(ClusterNode scn : serverNodes) {
				
				boolean finded = false;
				
				
				for(BaselineNode bs : onlineNodes) 
						if(bs.consistentId().equals(scn.consistentId())) {
							finded = true;
							break;
						}
				
				if(!finded) {
					offlineNodes.add(scn);
					offlineNodesStr.append("("+scn.consistentId()+")");
				}
			}
			
			
			str.append("In topology server nodes---: "+onlineNodes.size()+"["+onlineNodesStr.toString()+"]\n");
			str.append("Out topology server nodes--: "+offlineNodes.size()+"["+offlineNodesStr.toString()+"]\n");
			
			
			
			
			str.append("\n");
			
			
			
			
			if(igniteInstance.cluster().state()!=ClusterState.INACTIVE) {
			
				String cacheNames = "";
				if(!igniteInstance.cacheNames().isEmpty()) {
					for(String cacheName : igniteInstance.cacheNames())
						cacheNames += cacheName+", ";
					cacheNames = cacheNames.substring(0, cacheNames.length()-3);
				}
				
				
				
				str.append("Cache names------------: "+cacheNames+"\n");
			}
			
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
