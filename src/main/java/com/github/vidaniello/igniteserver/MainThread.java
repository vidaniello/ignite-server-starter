package com.github.vidaniello.igniteserver;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ignite.cluster.ClusterState;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MainThread implements Runnable{
	
	private int inetSocketPort = 34445;
	
	private CountDownLatch cdl;
	private Vertx vertx;
	private IgniteNode igniteNode;
	
	private String bootloader_status_;
	private DateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS");
	
	public MainThread(String[] args) {
		
		if(args!=null)
		if(args.length>0)
			try {
				inetSocketPort = new Integer(args[0]);
			}catch(NumberFormatException e) {
				System.err.println(args[0]+" is not valid inet port");
			}
		
		updateStatus("starting bootloader webserver on port " + inetSocketPort +"...");
		this.cdl = new CountDownLatch(1);
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			cdl.countDown();
		}));
	}
	
	private CountDownLatch cdl_httpServer;
	private AtomicBoolean canStartIgniteNode;
	
	@Override
	public void run() {
		try {
			cdl_httpServer = new CountDownLatch(1);
			canStartIgniteNode = new AtomicBoolean(false);
						
			Future<HttpServer> fut = startVertxHttpServer();
			
			fut
			.onFailure(twr->{				
				cdl_httpServer.countDown();
			})
			.onSuccess(hnd->{
				canStartIgniteNode.set(true);
				cdl_httpServer.countDown();
			});
			
			try{cdl_httpServer.await();}catch(InterruptedException e) {}
			
			if(canStartIgniteNode.get())
				onSuccessHttpServerStarted();
			else
				throw fut.cause();
			
			System.exit(0);
			
		}catch(Throwable e) {
			e.printStackTrace();
			if(vertx!=null)vertx.close();
			System.err.println("Start error!");
			System.exit(1);
		}
		
	}
	
	private void onSuccessHttpServerStarted() throws Exception {
		
		startIgniteNode();
		
		try{cdl.await();}catch(InterruptedException e) {}
		
		stopIgniteNode();
		
		updateStatus("stopping bootloader...");
		
		vertx.close();
		
		updateStatus("bootloader stopped!");
			
	}
	
	private void onStatus(RoutingContext ctx){
		ctx.response().putHeader("content-type", "text/plain")
		.end(getStatus()+"\n"+(igniteNode!=null?igniteNode.getNodeStatus():"")+"\n");
	}
	
	private void onStop(RoutingContext ctx){
		stopIgniteNode();
		if(ctx!=null) 
			ctx.response().putHeader("content-type", "text/plain").end("Ignite node stopped!\nrequest stopping bootloader...\n");
		
		cdl.countDown();
	}
	
	private void onStopnode(RoutingContext ctx){
		stopIgniteNode();
		if(ctx!=null) 
			ctx.response().putHeader("content-type", "text/plain").end("Ignite node stopped! request 'restart' for starting again ignite node.\n");
	}
	
	private void onRestart(RoutingContext ctx){
		stopIgniteNode();
		try {
			Thread.sleep(1000);
			startIgniteNode();
			
			if(ctx!=null) 
				ctx.response().putHeader("content-type", "text/plain").end("Restart successful!\n");
			
		} catch (Exception e) {
			if(ctx!=null)
				ctx.fail(500,e);
		}
	}
	
	private void onHelp(RoutingContext ctx) {
		
		String name = ManagementFactory.getRuntimeMXBean().getName();
		String pid = name.substring(0, name.indexOf("@"));
		
		ctx.response().putHeader("content-type", "text/plain").end(    
				  "'CTRL+C' from command line to send SIGTERM" + "\n" 
				+ "'kill pid " + pid + "' from unix command line to send SIGTERM" + "\n" + "\n"
				+ "from the 'http://localhost:"+inetSocketPort+"/[command]'"+ " or command line:" + "\n" + "\n"
				+ "status             : get status information" + "\n"
				+ "stop               : stop instance, also stop bootloader and webserver" + "\n"
				+ "stopnode           : stop only ignite node, call 'restart' command for starting after 'stopnode' command " + "\n"
				+ "restart            : restart te instance" + "\n"
				+ "help               : this help" + "\n"
				+ "switchclusterstate : switch cluster state: From ACTIVE to INACTIVE and viceversa" + "\n"
				+ "switchfrombaseline : put the node instance in the cluster baseline if out and viceversa" + "\n"
				);
	}
	
	private void onSwitchclusterstate(RoutingContext ctx) {
		String outMessage = "";
		
		if(igniteNode!=null)
			outMessage = igniteNode.switchClusterState();
		
		if(outMessage.isEmpty())
			outMessage = "Ignite local server node not present!"+"\n";
			
		if(ctx!=null) 
			ctx.response().putHeader("content-type", "text/plain").end(outMessage);
	}
	
	private void onSwitchFromBaselinee(RoutingContext ctx) {
		String outMessage = "";
		
		if(igniteNode!=null)
			outMessage = igniteNode.switchFromBaseline();
		
		if(outMessage.isEmpty())
			outMessage = "Ignite local server node not present!"+"\n";
			
		if(ctx!=null) 
			ctx.response().putHeader("content-type", "text/plain").end(outMessage);
	}
	
	
	private void stopIgniteNode() {
		if(igniteNode!=null) {
			updateStatus("stopping ignite node...");
			igniteNode.stopNode();
			updateStatus("ignite node stopped!");
			igniteNode=null;
		}
	}
	
	private void startIgniteNode() throws Exception {
		stopIgniteNode();
		igniteNode = new IgniteNode(this);
		updateStatus("starting ignite node...");
		igniteNode.startNode();
		updateStatus("ignite node started!");
		System.out.println("CTRL+C for shutdown all!");
	}
	
	private Future<HttpServer> startVertxHttpServer() throws Exception{
		InetSocketAddress isa = new InetSocketAddress(inetSocketPort);
		
		//check porta in uso
		try(Socket sk = new Socket(isa.getAddress(),inetSocketPort);){
			throw new Exception("Port "+inetSocketPort+" already in use!");
		}catch(IOException e){}
		
		SocketAddress sa = SocketAddress.inetSocketAddress(isa);
		
		vertx = Vertx.vertx();
		Router router = Router.router(vertx);
		
		router.get("/status")				.blockingHandler(this::onStatus);
		router.get("/stop")					.blockingHandler(this::onStop);
		router.get("/stopnode")				.blockingHandler(this::onStopnode);
		router.get("/restart")				.blockingHandler(this::onRestart);
		router.get("/help")					.blockingHandler(this::onHelp);
		router.get("/switchclusterstate")	.blockingHandler(this::onSwitchclusterstate);
		router.get("/switchfrombaseline")	.blockingHandler(this::onSwitchFromBaselinee);
		router.get()
		.blockingHandler(ctx->{
			if(ctx.request().path().equals("/"))
				onStatus(ctx);
			else
				ctx.fail(404);
		})
		.failureHandler(ctx->{
			ctx.response().setStatusCode(ctx.statusCode()).putHeader("content-type", "text/plain").end("Error code "+ctx.statusCode());
		});;
		
		return vertx
				.createHttpServer()
				.requestHandler(router)
				.listen(sa);
	}
	
	private String getStatus() {
		return bootloader_status_;
	}
	
	void updateStatus(String status) {
		String newStatus = "[BOOTLOADER "+df.format(new Date())+" - "+status;
		bootloader_status_ = newStatus;
		System.out.println(newStatus);
	}
	
}
