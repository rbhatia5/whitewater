package vServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.JSONException;

import vNetwork.Message;
import vNetwork.Message.MessageType;

public class ServerLoop {
	
	boolean interrupt = false; 
	private int serverPort; 
	
	
	ServerLoop()
	{
		serverPort = 5000; 
	}
	
	ServerLoop(int port)
	{
		serverPort = port; 
	}
	
	void start()
	{
		interrupt = false; 
		
		ServerSocket socket = null;
		
		try {
			System.out.println("SERVER: Initializing server socket");
			socket = new ServerSocket(serverPort);
			System.out.printf("SERVER: Server socket initialized %s\n", socket.toString());
		
			while(!interrupt)
			{
			
				System.out.println("SERVER: Connecting to socket port localhost:" + serverPort);
				Socket connectionSocket = socket.accept();
				System.out.printf("SERVER: Connected to socket %s\n", connectionSocket.toString());	
				
				// Now I have a socket from the client
				// Lets start a ConnectionThread which evaluates the message
				Thread connection = new Thread(new ConnectionThread(connectionSocket));
				connection.start(); 
			}
			
		}catch (IOException e)
		{
			System.err.printf("Server was unable to grant server socket %d", serverPort);
			e.printStackTrace();
		}
	}
	
	void quit()
	{
		interrupt = true;
	}
	
}
