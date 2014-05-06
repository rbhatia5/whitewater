package vServer;

import java.util.ArrayList;

public class RequestServer implements Runnable{

	public final static int requestPort = 5000;
	public static ArrayList<Thread> servers = new ArrayList<Thread>();
	public static int serverNumber;
	
	
	public static void main(String[] args)
	{
		RequestServer RS = new RequestServer();
		RS.run();
	}

	public void run() {
		serverNumber = 0;
		TCPServer server = new TCPServer(requestPort, TCPServer.TYPE.REQUEST);
		server.run();
	}
}