package vServer;

import java.util.ArrayList;

public class RequestServer {

	public final static int requestPort = 5000;
	public static ArrayList<Thread> servers = new ArrayList<Thread>();
	public static int serverNumber;
	
	public static void main(String[] args)
	{
		serverNumber = 0;
		TCPServer server = new TCPServer(requestPort, TCPServer.TYPE.REQUEST);
		server.run();
	}
}