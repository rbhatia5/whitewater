package vServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.net.Socket;

public class ServerState {

	//SINGLETON STUFF
	private static ServerState instance = null;
	
	protected HashMap<Socket, ServerSession> client;
	
	protected ArrayList<portTuple> portGroup;
	
	
	
	protected ServerState()
	{
		for(int i = 1; i < 20; i++)
		{
			portGroup.add(new portTuple(5000 + i, 5000 + 2 * i ));
		}
	
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
	
	private class portTuple {
		int portV, portA;
		boolean taken = false;
		
		portTuple(int a, int b)
		{
			portV = a;
			portA = b;
			taken = false;
		}
	}
	
}
