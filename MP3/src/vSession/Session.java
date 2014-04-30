package vSession;

import org.gstreamer.Pipeline;

import vNetwork.Message;

public abstract class Session {

	
	
	public Session()
	{
		
	}
	
	//all session objects must b e able to handle messages
	abstract public void handleMessage(Message msg);

	//all session objects must be able to build a pipeline
	abstract public Pipeline buildPipe();

}
