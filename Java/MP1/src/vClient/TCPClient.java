package vClient;

import java.io.*;
import java.net.*;

import org.json.JSONException;

import vNetwork.Message;

public class TCPClient implements Runnable{

	protected String message;
	protected Message msg;
	protected static boolean control;
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	public static void sendServerMessage(Message msg)
	{
		TCPClient client = new TCPClient(msg);
		Thread clientThread = new Thread(client);
		clientThread.start();
		try {
			synchronized(clientThread)
			{
				clientThread.wait();
			}
		} catch (InterruptedException e3) {
			e3.printStackTrace();
		}
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
			System.out.println("CLIENT: Initializing socket port " + ClientData.data[ClientData.activeWindow].comPort);
			Socket socket = null;
			while(true)
			{
				try
				{
					socket = new Socket(ClientData.data[ClientData.activeWindow].serverAddress, ClientData.data[ClientData.activeWindow].comPort);
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
			if(ClientData.data[ClientData.activeWindow].state.equals(ClientData.State.REQUESTING))
			{
				System.out.println("CLIENT: Beginning negotiation");
				ClientData.data[ClientData.activeWindow].state = ClientData.State.NEGOTIATING;
			}
			else if(ClientData.data[ClientData.activeWindow].state.equals(ClientData.State.NEGOTIATING))
			{
				System.out.println("CLIENT: Beginning streaming");
				ClientData.data[ClientData.activeWindow].state = ClientData.State.STREAMING;
			}
			synchronized(Thread.currentThread())
			{
				Thread.currentThread().notify();
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
	public static boolean negotiateProperties(Message properties)
	{		
		TCPClient.sendServerMessage(properties);
		
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
		try {
			Integer serverNumber = (Integer) ClientData.serverMessage.getData(Message.PORT_REQUEST_KEY);  
			ClientData.data[ClientData.activeWindow].setPorts(serverNumber);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
