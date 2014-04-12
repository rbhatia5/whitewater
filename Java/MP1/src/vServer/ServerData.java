package vServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;


import org.gstreamer.Format;
import org.gstreamer.Pipeline;
import org.gstreamer.SeekFlags;
import org.gstreamer.elements.good.RTPBin;

import vNetwork.Message;

public class ServerData {

	public enum Mode
	{
		SERVER
	}
	
	public enum State {
		NEGOTIATING, STREAMING
	}
	
	protected static final String clientIP = "127.0.0.1";
	protected final static int comPort = 5001;
	protected final static int videoRTP = 5002;
	protected final static int videoRTCPout = 5003;
	protected final static int videoRTCPin = 5004;
	protected final static int audioRTP = 5005;
	protected final static int audioRTCPout = 5006;
	protected final static int audioRTCPin = 5007;
	protected static Thread mainThread;
	protected static State state;
	protected static Pipeline pipe;
	protected static int Rate; 
	protected static RTPBin rtpBin;
	protected static String clientCommand;
	protected static Mode mode;
	protected static BufferedReader resourcesReader;
	protected static BufferedWriter resourcesWriter;
	protected static int width;
	protected static int height;
	protected static int framerate;
	protected static Message clientMessage;
	protected static String ipAddress;
	protected static String activity;
	
	
	
	
	public static String getIpAddress() {
		return ipAddress;
	}




	public static void setIpAddress(String ipAddress) {
		ServerData.ipAddress = ipAddress;
	}




	protected static void setRate(Pipeline pipe, int rate)
	{
		Format format = org.gstreamer.Format.TIME;
		
		int flags = SeekFlags.ACCURATE | SeekFlags.FLUSH;
		
		ServerData.Rate = rate;
		System.out.println("Rate is now: " + rate);
		pipe.seek(rate, format, flags, org.gstreamer.SeekType.NONE, 0, org.gstreamer.SeekType.NONE, 0);
	}



}
