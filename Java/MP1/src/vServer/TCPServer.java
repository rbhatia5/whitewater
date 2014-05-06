package vServer;

import java.io.BufferedReader;

import org.gstreamer.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

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
	
	protected boolean quit;
	protected int comPort;
	protected TYPE type;
	protected ServerSocket socket;
	protected Socket connectionSocket;
	protected BufferedReader inFromClient;
	protected DataOutputStream outToClient;
	protected vServerManager SM;
	
	TCPServer(int port, TYPE serverType)
	{
		quit = false;
		comPort = port;
		type = serverType;
	}
	
	TCPServer(vServerManager sm, int port, TYPE serverType)
	{
		quit = false;
		SM = sm;
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
			SM.quit = true;
			//Thread.currentThread().n
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
					server.data.clientIP = connectionSocket.getInetAddress().toString().substring(1);
					
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
			while(!quit)
			{
				listenForMessage();
				
				if(SM.data.state.equals(ServerData.State.NEGOTIATING))
				{
					try {
						if(negotiate())
						{
							SM.data.state = ServerData.State.STREAMING;
							SM.data.mainThread.interrupt();

							synchronized(SM.data.mainThread)
							{
								try {
									SM.data.mainThread.wait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
					
							Message msg = new Message();
							msg.setSender("SV00"); 
							msg.setType(MessageType.RESPONSE); 
							msg.addData(Message.RESULT_KEY, Message.RESULT_ACCEPT_VALUE);
							outToClient.writeBytes(msg.stringify() + "\n");
						}
						else
						{
							Message msg = new Message();
							msg.setSender("SV00"); msg.setType(MessageType.RESPONSE); msg.addData(Message.RESULT_KEY, Message.RESULT_REJECT_VALUE);
							outToClient.writeBytes(msg.stringify() + "\n");
							SM.data.mainThread.interrupt();
						}
					} catch (JSONException e) {
						e.printStackTrace();
						System.err.println("Could not send response to client");
					}
				}
				else if(SM.data.state.equals(ServerData.State.STREAMING))
				{
					try {
						
						Message msg = SM.data.clientMessage;
						
							SM.data.clientMessage.getData(Message.ACTION_KEY);
							adaptPipeline();
							Message response = new Message (MessageType.RESPONSE);
							response.setSender("SV00");
							response.addData(Message.RESULT_KEY, Message.RESULT_ACCEPT_VALUE);
							outToClient.writeBytes(response.stringify() + '\n');
					
					
						
						
					} catch (JSONException e) {
						changeActivity();
						Message response = new Message(MessageType.RESPONSE);
						response.setSender("SV00");
						response.addData(Message.POSITION_KEY, Long.toString(SM.data.position));
						outToClient.writeBytes(response.stringify() + '\n');
					}
					/*
					try {
						adaptPipeline();
						Message response = new Message (MessageType.RESPONSE);
						response.setSender("SV00");
						response.addData(Message.RESULT_KEY, Message.RESULT_ACCEPT_VALUE);
						outToClient.writeBytes(response.stringify() + '\n');
						//if(quit)
						//	SM.quit = true;
					} catch (JSONException e) {
						e.printStackTrace();
						System.err.println("could not send rseponse to client");
					}
					*/
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
		SM.data.clientCommand = inFromClient.readLine();
		SM.data.clientMessage = Message.destringify(SM.data.clientCommand);
		System.out.println("Server received Message: " + SM.data.clientMessage.stringify());
	}
	
	public boolean negotiate() throws JSONException
	{
		int framerate, width, height;
		String ip, source;
		
		source = (String)SM.data.clientMessage.getData(Message.SOURCE_KEY);
		if(source == null)
			source = Message.MOVIE_SOURCE_VALUE;
		
		framerate = (Integer)SM.data.clientMessage.getData(Message.FRAMERATE_KEY);
		width = (Integer)SM.data.clientMessage.getData(Message.FRAME_WIDTH_KEY);
		height = (Integer)SM.data.clientMessage.getData(Message.FRAME_HEIGHT_KEY);
		ip = (String)SM.data.clientMessage.getData(Message.CLIENT_IP_ADDRESS_KEY);
		String activity = (String)SM.data.clientMessage.getData(Message.ACTIVITY_KEY);
		
		SM.data.ipAddress = ip;
		SM.data.framerate = framerate;
		SM.data.width = width;
		SM.data.height = height;
		
		if(source.equals( Message.MOVIE_SOURCE_VALUE))
			SM.data.mediaType = ServerData.MediaType.MOVIE;
		else if( source.equals(Message.WEBCAM_SOURCE_VALUE))
			SM.data.mediaType = ServerData.MediaType.WEBCHAT;
		
		if(activity.equalsIgnoreCase(Message.ACTIVITY_ACTIVE_VALUE))
			SM.data.activity = "Active";
		else
			SM.data.activity = "Passive";
		
		int proposedBandwidth = framerate*width*height*3;
		if(proposedBandwidth<= ServerResource.getInstance().getBandwidth()){
			ServerResource.getInstance().adjustResources(proposedBandwidth);
			return true;
		}
		else
			return false;
	}
	
	public void adaptPipeline() throws JSONException
	{
		if(SM.data.clientMessage == null || SM.data.clientMessage.getType() != Message.MessageType.CONTROL)
			return;
		String action = (String)SM.data.clientMessage.getData(Message.ACTION_KEY);
		
		if(action.equals(Message.PLAY_ACTION))
		{
			synchronized(SM.data.pipeMsgThread)
			{
				SM.data.notify = true;
				SM.data.pipe.setState(State.PLAYING);
				try {
					SM.data.pipeMsgThread.wait();
					if(SM.data.pipe.getState().equals(State.PLAYING))
						System.err.println("PLAY");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if(SM.data.position != 0)
			{
				boolean success = SM.data.pipe.seek(SM.data.position, TimeUnit.SECONDS);
				if(!success)
					System.err.println("SERVER seek to " + SM.data.position);
			}
			SM.data.setRate(SM.data.pipe, 1); 
			SM.data.fakeSink.setState(State.PLAYING);
		}
		else if(action.equals(Message.PAUSE_ACTION))
		{
			synchronized(SM.data.pipeMsgThread)
			{
				SM.data.notify = true;
				SM.data.pipe.setState(State.PAUSED);
				try {
					SM.data.pipeMsgThread.wait();
					if(SM.data.pipe.getState().equals(State.PAUSED))
						System.err.println("PAUSE");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		else if(action.equals(Message.STOP_ACTION))
		{
			synchronized(SM.data.pipeMsgThread)
			{
				SM.data.notify = true;
				SM.data.pipe.setState(State.NULL);
				try {
					SM.data.pipeMsgThread.wait();
					if(SM.data.pipe.getState().equals(State.NULL))
						System.err.println("STOP");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			quit = true;
		}
		else if(action.equals(Message.FAST_FORWARD_ACTION))
		{ 
			if(SM.data.Rate > 0) {
				SM.data.setRate(SM.data.pipe, 2 * SM.data.Rate);
			}
			else
			{
				SM.data.setRate(SM.data.pipe, 1);
			}
		}
		else if(action.equals(Message.REWIND_ACTION))
		{
			if(SM.data.Rate < 0)
				SM.data.setRate(SM.data.pipe, 2 * SM.data.Rate);
			else if ( SM.data.Rate == 1)
				SM.data.setRate(SM.data.pipe, -2);
			else if ( SM.data.Rate > 1)
				SM.data.setRate(SM.data.pipe, 1);
		}
	}
	
	protected void changeActivity()
	{
		try {
			String position = (String) SM.data.clientMessage.getData(Message.POSITION_KEY);
			SM.data.position = Long.parseLong(position);
			System.out.println("Server position is " + SM.data.position);
		} catch (JSONException e) {
			SM.data.position = SM.data.pipe.queryPosition(TimeUnit.SECONDS);
			System.out.println("Server position is " + SM.data.position);
		}
	}
	
}
