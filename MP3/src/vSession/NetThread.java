package vSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.json.JSONException;

import vNetwork.Message;

public class NetThread extends Thread {

	private volatile boolean quit = false;
	private Socket conn;
	private Session parent;
	
	public NetThread(Session parent)
	{
		
	}
	
	public NetThread(Session parent, Socket listen)
	{
		conn = listen; 
	}

	
	public void run()
	{
		quit = false;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while(!quit)
			{
				String line = reader.readLine();
				Message msg = Message.destringify(line);
				parent.handleMessage(msg);
			}
		
		}catch(IOException e)
		{
			
		}
		catch(JSONException j)
		{
		
		}
	}
	
	public void destroy()
	{
		quit = true;
	}

}

