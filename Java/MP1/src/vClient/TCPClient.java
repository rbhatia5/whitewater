package vClient;

import java.io.*;
import java.net.*;

import org.gstreamer.State;

public class TCPClient implements Runnable{

	private String message;
	
	TCPClient(String message)
	{
		this.message = message;
	}
	
	public void run()
	{
		try
		{
			System.out.println("CLIENT: Initializing socket port 5000");
			Socket socket = new Socket("localhost", 5000);
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
			System.out.println("CLIENT: Writing to socket");
			outToServer.writeBytes(message + "\n");
			System.out.println("CLIENT: Reading from socket");
			String serverString = inFromServer.readLine();
			ClientData.serverResponse = serverString;
			System.out.printf("Server sent: %s\n", serverString);
			socket.close();
			if(ClientData.state.equals(ClientData.State.NEGOTIATING))
			{
				ClientData.state = ClientData.State.STREAMING;
				ClientData.mainThread.interrupt();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
