package vServer;

import org.gstreamer.*;

public class vServerManager implements Runnable {
	
	protected static ServerData data;
	
	vServerManager(int serverNumber)
	{
		data = new ServerData(serverNumber);
	}
	
	public void initializeTCPServer(int port, TCPServer.TYPE type)
	{
		TCPServer server = new TCPServer(port, type);
		new Thread(server).start();
	}

	public void run() {
		
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
		ServerPipelineManager.modify_pipeline();
		data.pipe.setState(State.READY);
		
		Gst.main();
	}

}
