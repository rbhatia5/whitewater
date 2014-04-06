package vServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.gstreamer.State;
import org.gstreamer.StateChangeReturn;

public class TCPServer implements Runnable{
	
	public void run() 
	{
		String clientString;
		String serverString;
		ServerSocket socket = null;
		
		try {
			System.out.println("SERVER: Initializing server socket");
			socket = new ServerSocket(5000);
			System.out.printf("SERVER: Server socket initialized %s\n", socket.toString());
			while(true)
			{
				System.out.println("SERVER: Connecting to socket port 5000");
				Socket connectionSocket = socket.accept();
				System.out.printf("SERVER: Connected to socket %s\n", connectionSocket.toString());
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				System.out.println("SERVER: Reading from socket");
				clientString = inFromClient.readLine() + "\n";
				ServerData.clientCommand = clientString;
				if(ServerData.state.equals(ServerData.State.NEGOTIATING))
				{
					serverString = negotiate();
					ServerData.state = ServerData.State.STREAMING;
					outToClient.writeBytes(serverString + "\n");
					ServerData.mainThread.interrupt();
				}
				else if(ServerData.state.equals(ServerData.State.STREAMING))
				{
					adaptPipeline();
					serverString = "State change successful";
					outToClient.writeBytes(serverString + "\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	public static String negotiate()
	{
		String requestedFrameRate = ServerData.clientCommand;
		String negotiatedFrameRate = "framerate=10";
		return negotiatedFrameRate;
	}
	
	public static void adaptPipeline()
	{
		if(ServerData.clientCommand.contains("play"))
		{
			StateChangeReturn ret = ServerData.pipe.setState(State.PLAYING);
			System.out.println(ret.toString());
		}
		else if(ServerData.clientCommand.contains("pause"))
		{
			StateChangeReturn ret = ServerData.pipe.setState(State.PAUSED);
			System.out.println(ret.toString());
		}
	}
}
