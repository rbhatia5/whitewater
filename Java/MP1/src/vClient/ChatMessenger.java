package vClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.JSONException;

import vNetwork.Message;
import vNetwork.Message.MessageType;
import vServer.RequestServer;

public class ChatMessenger implements Runnable {
	
	private volatile boolean quit = false;
	protected BufferedReader inFromClient;
	protected DataOutputStream outToClient;
	
	public ChatMessenger()
	{
		
	}

	
	public void run()
	{
		ServerSocket socket;
		try {
			socket = new ServerSocket(4999);
			
			while(!quit)
			{
				Socket serv = socket.accept(); 
				System.err.println("HERE1");
				inFromClient = new BufferedReader(new InputStreamReader(serv.getInputStream()));
				outToClient = new DataOutputStream(serv.getOutputStream());
				String clientCommand = inFromClient.readLine();
				System.err.println(clientCommand);
				Message clientMessage = Message.destringify(clientCommand);
				if(clientCommand.contains("connect"))
					ClientGUIManager.handleMessage(clientMessage);
				Message response = new Message();
				response.setSender("CL00"); 
				response.setType(MessageType.RESPONSE); 
				outToClient.writeBytes(response.stringify() + "\n");			
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		
	}
	
	/*
	public void sendMessage(Message newMsg)
	{
		try {
			//BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			
			//writer.write(newMsg.stringify());
			
		}
		catch(IOException e)
		{
			
		}catch(JSONException j)
		{
			
		}
		
	}
	*/
	
	public void destroy()
	{
		quit = true;
	}

}
