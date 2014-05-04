package vServer;

import org.gstreamer.*;

public class vServerManager implements Runnable {
	
	protected ServerData data;
	protected boolean quit;
	
	vServerManager(int serverNumber)
	{
		data = new ServerData(serverNumber);
	}
	
	public void initializeTCPServer(int port, TCPServer.TYPE type)
	{
		TCPServer server = new TCPServer(this, port, type);
		data.serverThread = new Thread(server);
		data.serverThread.start();
	}

	public void run() {
		quit = false;
		data.mainThread = Thread.currentThread();
		data.state = ServerData.State.NEGOTIATING;
		data.setIpAddress("localhost");
		
		ServerResource res = ServerResource.getInstance();
		res.initWithFile("server-resources.txt");
	
		initializeTCPServer(data.comPort, TCPServer.TYPE.CONTROL);
		
		while(!data.mainThread.interrupted());
		String[] args = new String[0];
		Gst.init("Server Pipeline", args);
		data.mode = ServerData.Mode.SERVER;
		data.position = (long) 0;
		ServerPipelineManager SPM = new ServerPipelineManager(this);
		SPM.modify_pipeline();
		data.pipe.setState(State.READY);
		
		//Gst.main();
		while(!quit);
		System.out.println("SERVER: Destroying Server " + Thread.currentThread().getId());
	}

}
