package vServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;


import org.gstreamer.Format;
import org.gstreamer.Pipeline;
import org.gstreamer.SeekFlags;
import org.gstreamer.elements.good.RTPBin;

import vNetwork.Message;

public class ServerData {

	ServerData(int serverNumber)
	{
		comPort = 5001 + (serverNumber-1)*7;
		videoRTP = 5002 + (serverNumber-1)*7;
		videoRTCPout = 5003 + (serverNumber-1)*7;
		videoRTCPin = 5004 + (serverNumber-1)*7;
		audioRTP = 5005 + (serverNumber-1)*7;
		audioRTCPout = 5006 + (serverNumber-1)*7;
		audioRTCPin = 5004 + (serverNumber-1)*7;
	}
	
	public enum Mode
	{
		SERVER
	}
	
	public enum State {
		NEGOTIATING, STREAMING
	}
	
	protected String clientIP = "127.0.0.1";
	protected int comPort;
	protected int videoRTP;
	protected int videoRTCPout;
	protected int videoRTCPin;
	protected int audioRTP;
	protected int audioRTCPout;
	protected int audioRTCPin;
	protected Thread mainThread;
	protected State state;
	protected Pipeline pipe;
	protected int Rate; 
	protected RTPBin rtpBin;
	protected String clientCommand;
	protected Mode mode;
	protected BufferedReader resourcesReader;
	protected BufferedWriter resourcesWriter;
	protected int width;
	protected int height;
	protected int framerate;
	protected Message clientMessage;
	protected String ipAddress;
	protected String activity;
	
	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	protected void setRate(Pipeline pipe, int rate)
	{
		Format format = org.gstreamer.Format.TIME;
		
		int flags = SeekFlags.ACCURATE | SeekFlags.FLUSH;
		
		Rate = rate;
		System.out.println("Rate is now: " + rate);
		pipe.seek(rate, format, flags, org.gstreamer.SeekType.NONE, 0, org.gstreamer.SeekType.NONE, 0);
	}

}
