package vSession;

import org.gstreamer.*;

public class GstThread extends Thread {
	
	private String ipAddress;
	private MediaComm audioPort;
	private MediaComm videoPort;
	private Pipeline pipe;
	private String senderAddress;	
	
	private Session parent; 
	
	
	
	public GstThread(Session parent)
	{
		this.parent = parent; 
	}
	
	public void run()
	{
		
	}
	
	public void destroy()
	{
		
	}
}
