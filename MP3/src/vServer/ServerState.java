package vServer;

import java.util.HashMap;
import java.net.Socket;

public class ServerState {

	//SINGLETON STUFF
	private static ServerState instance = null;
	
	protected HashMap<Socket, ServerSession> client;
	
	
	
	protected ServerState()
	{
		
	
	}
	
	public static ServerState getInstance()
	{
		if(instance == null)
			instance = new ServerState();
		
		return instance;
				
	}
	
	
	public boolean addServerSession(Socket connection)
	{
		ServerSession newSession = new ServerSession(connection);
		client.put(connection, newSession);
		return true; 
	}
	
	public boolean removeServerSession(Socket connection)
	{
		client.remove(connection);
		return true;
	}
	
	public ServerSession getServerSession(Socket connection)
	{
		ServerSession sesh = client.get(connection);
		return sesh;
	}
	
}
