package vServer;


import java.io.*;


import org.gstreamer.*;


//import vServer.*;

public class vServerManager {
	
	public final static int comPort = 5001;
	public final static int videoRTP = 5002;
	public final static int videoRTCPin = 5003;
	public final static int videoRTCPout = 5004;
	
	
	public static void initializeTCPServer()
	{
		TCPServer server = new TCPServer();
		new Thread(server).start();
	}
	
	public static void main(String[] args)
	{	
		ServerData.mainThread = Thread.currentThread();
		ServerData.state = ServerData.State.NEGOTIATING;
		
		ServerResource res = ServerResource.getInstance();
		res.initWithFile("server-resources.txt");
		
		initializeTCPServer();
		while(!ServerData.mainThread.interrupted());
		
		args = Gst.init("Server Pipeline", args);
		
		ServerData.mode = ServerData.Mode.SERVER;
		ServerData.width = 352;
		ServerData.height = 288;
		ServerData.framerate = 30;
		
		ServerPipelineManager.modify_pipeline();
		
		ServerData.pipe.setState(State.READY);
		
		Gst.main();
	}

}
