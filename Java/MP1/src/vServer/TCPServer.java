package vServer;

import java.io.BufferedReader;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.gstreamer.State;
import org.gstreamer.StateChangeReturn;
import org.json.JSONException;

import vNetwork.Message;
import vNetwork.Message.MessageType;



public class TCPServer implements Runnable{
	
	
	public void run() 
	{
		String clientString;
		ServerSocket socket = null;
		
		try {
			System.out.println("SERVER: Initializing server socket");
			socket = new ServerSocket(vServerManager.comPort);
			System.out.printf("SERVER: Server socket initialized %s\n", socket.toString());
			while(true)
			{
				System.out.println("SERVER: Connecting to socket port "+ vServerManager.comPort);
				Socket connectionSocket = socket.accept();
				System.out.printf("SERVER: Connected to socket %s\n", connectionSocket.toString());
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				System.out.println("SERVER: Reading from socket");
				clientString = inFromClient.readLine() + "\n";
				ServerData.clientCommand = clientString;
				try {
					ServerData.clientMessage = Message.destringify(clientString);
					System.out.println("Server received Message: " + ServerData.clientMessage.stringify());
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(ServerData.state.equals(ServerData.State.NEGOTIATING))
				{
					if(negotiate())
					{
						ServerData.state = ServerData.State.STREAMING;
						Message msg = new Message();
						try {
							msg.setSender("SV00"); 
							msg.setType(MessageType.RESPONSE); 
							msg.addData(Message.RESULT_KEY, Message.RESULT_ACCEPT_VALUE);
							
							
							outToClient.writeBytes(msg.stringify() + "\n");
						}
						catch(JSONException j){
							System.err.println("Could not send response to client");
						}
						
						ServerData.mainThread.interrupt();
					}
					else
					{
						Message msg = new Message();
						try {
							msg.setSender("SV00"); msg.setType(MessageType.RESPONSE); msg.addData(Message.RESULT_KEY, Message.RESULT_REJECT_VALUE);
							
							outToClient.writeBytes(msg.stringify() + "\n");
						}
						catch(JSONException j){
							System.err.println("Could not send response to client");
							
						}
						
						ServerData.mainThread.interrupt();
					}
				}
				else if(ServerData.state.equals(ServerData.State.STREAMING))
				{
					adaptPipeline();
					//SEND SUCCESS MESSAGE
					//serverString = "State change successful";
					//outToClient.writeBytes(serverString + "\n");
					
					Message response = new Message (MessageType.RESPONSE);
					try {
						response.setSender("SV00");
						response.addData(Message.RESULT_KEY, Message.RESULT_ACCEPT_VALUE);
						
						outToClient.writeBytes(response.stringify() + '\n');
					} catch(JSONException j)
					{
						j.printStackTrace();
						System.err.println("could not send rseponse to client");
					}
					
					
					
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	public static boolean negotiate()
	{
		
		
		int framerate, width, height;
		
		try {
			 framerate = (Integer)ServerData.clientMessage.getData(Message.FRAMERATE_KEY);
			 width = (Integer)ServerData.clientMessage.getData(Message.FRAME_WIDTH_KEY);
			 height = (Integer)ServerData.clientMessage.getData(Message.FRAME_HEIGHT_KEY);
			
			
		} catch (JSONException e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return false;
		} 
		
		int proposedBandwidth = framerate*width*height*3;
		
		if(proposedBandwidth<= ServerResource.getInstance().getBandwidth()){
			
			ServerResource.getInstance().adjustResources(proposedBandwidth);
			//String negotiatedResources = framerate + " " + width + "x" + height;
			return true;
		}
		else
			return false;
			

	}
	
	public static void adaptPipeline()
	{
		
		if(ServerData.clientMessage == null || ServerData.clientMessage.getType() != Message.MessageType.CONTROL)
			return;
		

		try {
			String action = (String)ServerData.clientMessage.getData(Message.ACTION_KEY);
			
			if(action.equals(Message.PLAY_ACTION))
			{
				StateChangeReturn ret = ServerData.pipe.setState(State.PLAYING);
				ServerData.setRate(ServerData.pipe, 1); 
				System.out.println(ret.toString());
			}
			else if(action.equals(Message.PAUSE_ACTION))
			{
				StateChangeReturn ret = ServerData.pipe.setState(State.PAUSED);
				System.out.println(ret.toString());
			}
			else if(action.equals(Message.FAST_FORWARD_ACTION))
			{ 
				if(ServerData.Rate > 0) {
					ServerData.setRate(ServerData.pipe, 2 * ServerData.Rate);
				}
				else
				{
					ServerData.setRate(ServerData.pipe, 1);
				}
			}
			else if(action.equals(Message.REWIND_ACTION))
			{

				if(ServerData.Rate < 0)
					ServerData.setRate(ServerData.pipe, 2 * ServerData.Rate);
				else if ( ServerData.Rate == 1)
					ServerData.setRate(ServerData.pipe, -2);
				else if ( ServerData.Rate > 1)
					ServerData.setRate(ServerData.pipe, 1);

			}
			
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			System.err.println("Action coudl not be read");
			e.printStackTrace();
		}
	}
	
}
