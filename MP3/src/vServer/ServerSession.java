package vServer;

import java.io.IOException;
import java.net.Socket;

import org.gstreamer.Pipeline;

import vNetwork.Message;
import vSession.*;

public class ServerSession extends Session{

	private Socket mySocket = null;
	private String clientIP;
	
	private NetThread net;
	private GstThread gst;
	
	
	
	ServerSession(Socket connection)
	{
		super();
		mySocket = connection;
		clientIP = connection.getInetAddress().toString();
		
		gst = new GstThread(this);
		net = new NetThread(this);
	
	}
	
	
	
	public void handleMessage(Message msg)
	{
		//Switch here based on msg content
		//call relevant functions in gst thread
	}
	
	public Pipeline buildPipe()
	{
		
		//Needs writing
		return new Pipeline();
	}
	
	
	
	public void destroy()
	{
		net.destroy();
		gst.destroy();
		
		ServerState.getInstance().removeServerSession(mySocket);
		
		try {
			mySocket.close();
		} catch (IOException e) {
			System.out.println("Failed to close socket");
			e.printStackTrace();
		}
		
		System.out.printf("Killed net thread, gst thread, socket\n");
	}
	
}
