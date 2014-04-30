package vClient;

import java.io.*;
import java.net.*;

import org.json.JSONException;

import vNetwork.Message;

public class TCPClient implements Runnable{

	protected String message;
	protected Message msg;
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	public static void sendServerMessage(String message)
	{
		TCPClient client = new TCPClient(message);
		Thread clientThread = new Thread(client);
		clientThread.start();
	}
	
	public static void sendServerMessage(Message msg)
	{
		TCPClient client = new TCPClient(msg);
		Thread clientThread = new Thread(client);
		clientThread.start();
	}
	
	TCPClient(String message)
	{
		this.message = message;
	}
	
	TCPClient(Message msg)
	{
		this.msg = msg;
	}
	
	public void run()
	{
		try
		{
			System.out.println("CLIENT: Initializing socket port " + ClientData.comPort);
			Socket socket = null;
			while(true)
			{
				try
				{
					socket = new Socket(ClientData.serverAddress, ClientData.comPort);
				} catch (ConnectException ignore){};
				if(socket != null)
					break;
			}
			
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
			System.out.println("CLIENT: Writing to socket");
			System.out.println("CLIENT: Writing " + msg.stringify());
			outToServer.writeBytes(msg.stringify() + "\n");
			
			System.out.println("CLIENT: Reading from socket");
			String serverString = inFromServer.readLine();
			
			ClientData.serverResponse = serverString;
			ClientData.serverMessage = Message.destringify(serverString);
			
			System.out.printf("Server sent: %s\n", serverString);
			socket.close();
			if(ClientData.state.equals(ClientData.State.REQUESTING))
			{
				System.out.println("CLIENT: Beginning negotiation");
				ClientData.state = ClientData.State.NEGOTIATING;
				ClientData.mainThread.interrupt();
			}
			else if(ClientData.state.equals(ClientData.State.NEGOTIATING))
			{
				System.out.println("CLIENT: Beginning streaming");
				ClientData.state = ClientData.State.STREAMING;
				ClientData.mainThread.interrupt();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (JSONException j)
		{
			j.printStackTrace();
			System.out.println("JSON message not stringifiable");
		}
	}

	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	public static String negotiateProperties(String properties)
	{		
		TCPClient.sendServerMessage(properties);
		while(!ClientData.mainThread.interrupted());
		
		if(!ClientData.serverResponse.equals(properties))
		{
			System.out.println("Negotiation Failed: Server cannot facilitate request. Modify properties to " + ClientData.serverResponse);
		}
		else
		{
			System.out.println("Negotiation Successful: Setting properties to " + ClientData.serverResponse);
		}
		return ClientData.serverResponse;
	}
	
	public static boolean negotiateProperties(Message properties)
	{		
		TCPClient.sendServerMessage(properties);
		while(!ClientData.mainThread.interrupted());
		
		if(ClientData.serverMessage.getType() != Message.MessageType.RESPONSE)
			return false;
		
		String result;
		try {
			result = (String) ClientData.serverMessage.getData("result");
		} catch (JSONException e) {
			
			e.printStackTrace();
			result = null;
			return false;
		}
		
		if(result.equals("reject"))
		{
			System.out.println("Negotiation Failed: Server cannot facilitate request");
			return false;
		}
		else if(result.equals("accept"))
		{
			System.out.println("Negotiation Successful");
			return true;
		}
		else
			return false;
		
	}
	
	public static void requestPort(Message portRequest)
	{
		TCPClient.sendServerMessage(portRequest);
		while(!ClientData.mainThread.interrupted());
		try {
			Integer serverNumber = (Integer) ClientData.serverMessage.getData(Message.PORT_REQUEST_KEY);  
			ClientData.comPort = 5001 + (serverNumber-1)*7;
			ClientData.videoRTP = 5002 + (serverNumber-1)*7;
			ClientData.videoRTCPout = 5003 + (serverNumber-1)*7;
			ClientData.videoRTCPin = 5004 + (serverNumber-1)*7;
			ClientData.audioRTP = 5005 + (serverNumber-1)*7;
			ClientData.audioRTCPout = 5006 + (serverNumber-1)*7;
			ClientData.audioRTCPin = 5004 + (serverNumber-1)*7;
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
