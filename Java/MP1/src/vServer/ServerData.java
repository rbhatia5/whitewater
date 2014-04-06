package vServer;

import org.gstreamer.Pipeline;

public class ServerData {

	public enum Mode
	{
		SERVER
	}
	
	public enum State {
		NEGOTIATING, STREAMING
	}
	
	protected static Thread mainThread;
	protected static State state;
	protected static Pipeline pipe;
	protected static String clientCommand;
	protected static Mode mode;
}
