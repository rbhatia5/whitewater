package vServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.json.JSONException;

import vNetwork.Message;

public class ConnectionThread implements Runnable {

	private Socket conn;

	ConnectionThread(Socket connection)
	{
		conn = connection;
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try { 
			//Get a buffered reader and read the message
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			System.out.println("SERVER: Reading from socket");
			String msg = inFromClient.readLine() + "\n";
			
			//convert message into message class
			Message clientMessage = Message.destringify(msg);
		
			//Get framerate from message
			int framerate = Integer.parseInt((String)clientMessage.getData(Message.FRAMERATE_KEY));
		
			
			//compare resource!
			
			
			//assumes resources are fine, work out else if and else for resource adaptation and rejection
			if(true)
			{
				ServerState.getInstance().addServerSession(conn);
				System.out.printf("SERVER: Creating ServerSession Object\n");	
				
			}
		
		}catch(IOException e)
		{
			
		}
		catch (JSONException j)
		{
			
		}
	}

}
