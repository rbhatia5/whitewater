package vServer;

import java.io.BufferedReader;

import org.gstreamer.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.gstreamer.Gst;
import org.gstreamer.State;
import org.gstreamer.StateChangeReturn;
import org.json.JSONException;

import vNetwork.Message;
import vNetwork.Message.MessageType;

public class TCPServer implements Runnable{
	
	public enum TYPE {
		REQUEST, CONTROL
	}
	
	protected int comPort;
	protected TYPE type;
	protected ServerSocket socket;
	protected Socket connectionSocket;
	protected BufferedReader inFromClient;
	protected DataOutputStream outToClient;
	
	TCPServer(int port, TYPE serverType)
	{
		comPort = port;
		type = serverType;
	}

	public void run() 
	{
		if(type == TYPE.REQUEST)
		{
			requestHandler();		
		}
		
		else if(type == TYPE.CONTROL)
		{
			controlHandler();
		}
	}	
	
	protected void requestHandler()
	{
		try {
			initializeSocket();
			while(true)
			{
				System.out.println("SERVER: Connecting to socket port localhost:" + comPort);
				connectionSocket = socket.accept();
				System.out.printf("SERVER: Connected to %s\n", connectionSocket.toString());
				inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				System.out.println("SERVER: Reading from socket");
				String clientCommand = inFromClient.readLine();
				System.out.println("Server received Message: " + clientCommand);
				Message clientMessage = Message.destringify(clientCommand);
				
				if(clientMessage.getType().equals(Message.MessageType.NEW))
				{
					RequestServer.serverNumber++;
					vServerManager server = new vServerManager(RequestServer.serverNumber);
					Thread serverThread = new Thread(server); 
					RequestServer.servers.add(serverThread);
					serverThread.start();
						
					Message response = new Message();
					response.setSender("SV00"); 
					response.setType(MessageType.RESPONSE); 
					int newComPort = 5001 + (RequestServer.serverNumber-1)*7;
					response.addData(Message.PORT_REQUEST_KEY, RequestServer.serverNumber);
					System.out.println("SERVER: Writing " + response.stringify());
					outToClient.writeBytes(response.stringify() + "\n");					
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	protected void controlHandler()
	{
		try {
			initializeSocket();
			while(true)
			{
				listenForMessage();
				
				if(vServerManager.data.state.equals(ServerData.State.NEGOTIATING))
				{
					try {
						if(negotiate())
						{
							vServerManager.data.state = ServerData.State.STREAMING;
							Message msg = new Message();
							
							msg.setSender("SV00"); 
							msg.setType(MessageType.RESPONSE); 
							msg.addData(Message.RESULT_KEY, Message.RESULT_ACCEPT_VALUE);
							outToClient.writeBytes(msg.stringify() + "\n");
							vServerManager.data.mainThread.interrupt();
						}
						else
						{
							Message msg = new Message();
							msg.setSender("SV00"); msg.setType(MessageType.RESPONSE); msg.addData(Message.RESULT_KEY, Message.RESULT_REJECT_VALUE);
							outToClient.writeBytes(msg.stringify() + "\n");
							vServerManager.data.mainThread.interrupt();
						}
					} catch (JSONException e) {
						e.printStackTrace();
						System.err.println("Could not send response to client");
					}
				}
				else if(vServerManager.data.state.equals(ServerData.State.STREAMING))
				{
					try {
						adaptPipeline();
						Message response = new Message (MessageType.RESPONSE);
						response.setSender("SV00");
						response.addData(Message.RESULT_KEY, Message.RESULT_ACCEPT_VALUE);
						outToClient.writeBytes(response.stringify() + '\n');
					} catch (JSONException e) {
						e.printStackTrace();
						System.err.println("could not send rseponse to client");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	
	protected void initializeSocket() throws IOException
	{
		socket = null;
		System.out.println("SERVER: Initializing server socket");
		socket = new ServerSocket(comPort);
		System.out.printf("SERVER: Server socket initialized %s\n", socket.toString());
	}
	
	protected void listenForMessage() throws IOException, JSONException
	{
		System.out.println("SERVER: Connecting to socket port localhost:" + comPort);
		connectionSocket = socket.accept();
		System.out.printf("SERVER: Connected to %s\n", connectionSocket.toString());
		inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		outToClient = new DataOutputStream(connectionSocket.getOutputStream());
		System.out.println("SERVER: Reading from socket");
		vServerManager.data.clientCommand = inFromClient.readLine();
		vServerManager.data.clientMessage = Message.destringify(vServerManager.data.clientCommand);
		System.out.println("Server received Message: " + vServerManager.data.clientMessage.stringify());
	}
	
	public static boolean negotiate() throws JSONException
	{
		int framerate, width, height;
		String ip;
		
		framerate = (Integer)vServerManager.data.clientMessage.getData(Message.FRAMERATE_KEY);
		width = (Integer)vServerManager.data.clientMessage.getData(Message.FRAME_WIDTH_KEY);
		height = (Integer)vServerManager.data.clientMessage.getData(Message.FRAME_HEIGHT_KEY);
		ip = (String)vServerManager.data.clientMessage.getData(Message.CLIENT_IP_ADDRESS_KEY);
		String activity = (String)vServerManager.data.clientMessage.getData(Message.ACTIVITY_KEY);
		
		vServerManager.data.ipAddress = ip;
		vServerManager.data.framerate = framerate;
		vServerManager.data.width = width;
		vServerManager.data.height = height;
		if(activity.equalsIgnoreCase(Message.ACTIVITY_ACTIVE_VALUE))
			vServerManager.data.activity = "Active";
		else
			vServerManager.data.activity = "Passive";
		
		int proposedBandwidth = framerate*width*height*3;
		if(proposedBandwidth<= ServerResource.getInstance().getBandwidth()){
			ServerResource.getInstance().adjustResources(proposedBandwidth);
			return true;
		}
		else
			return false;
	}
	
	public static void adaptPipeline() throws JSONException
	{
		if(vServerManager.data.clientMessage == null || vServerManager.data.clientMessage.getType() != Message.MessageType.CONTROL)
			return;
		String action = (String)vServerManager.data.clientMessage.getData(Message.ACTION_KEY);
		
		if(action.equals(Message.PLAY_ACTION))
		{
			StateChangeReturn ret = vServerManager.data.pipe.setState(State.PLAYING);
			vServerManager.data.setRate(vServerManager.data.pipe, 1); 
			System.out.println(ret.toString());
		}
		else if(action.equals(Message.PAUSE_ACTION))
		{
			StateChangeReturn ret = vServerManager.data.pipe.setState(State.PAUSED);
			System.out.println(ret.toString());
		}
		else if(action.equals(Message.STOP_ACTION))
		{
			StateChangeReturn ret = vServerManager.data.pipe.setState(State.PAUSED);
			Gst.quit();
			vServerManager.data.state = ServerData.State.NEGOTIATING;
			System.out.println(ret.toString());
		}
		else if(action.equals(Message.FAST_FORWARD_ACTION))
		{ 
			if(vServerManager.data.Rate > 0) {
				vServerManager.data.setRate(vServerManager.data.pipe, 2 * vServerManager.data.Rate);
			}
			else
			{
				vServerManager.data.setRate(vServerManager.data.pipe, 1);
			}
		}
		else if(action.equals(Message.REWIND_ACTION))
		{
			if(vServerManager.data.Rate < 0)
				vServerManager.data.setRate(vServerManager.data.pipe, 2 * vServerManager.data.Rate);
			else if ( vServerManager.data.Rate == 1)
				vServerManager.data.setRate(vServerManager.data.pipe, -2);
			else if ( vServerManager.data.Rate > 1)
				vServerManager.data.setRate(vServerManager.data.pipe, 1);
		}
	}
	
}
