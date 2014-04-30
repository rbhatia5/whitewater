package vClient;

import org.gstreamer.Element;
import org.gstreamer.Pipeline;
import org.gstreamer.elements.AppSink;
import org.gstreamer.elements.good.RTPBin;

import vClient.ClientData.Mode;
import vClient.ClientData.State;

public class GstData {
		
	public void setPorts(int serverNumber)
	{
		comPort = 5001 + (serverNumber-1)*7;
		videoRTP = 5002 + (serverNumber-1)*7;
		videoRTCPout = 5003 + (serverNumber-1)*7;
		videoRTCPin = 5004 + (serverNumber-1)*7;
		audioRTP = 5005 + (serverNumber-1)*7;
		audioRTCPout = 5006 + (serverNumber-1)*7;
		audioRTCPin = 5004 + (serverNumber-1)*7;
	}
	
	protected int comPort;
	protected int videoRTP;
	protected int videoRTCPout;
	protected int videoRTCPin;
	protected int audioRTP;
	protected int audioRTCPout;
	protected int audioRTCPin;
	protected String serverAddress = "127.0.0.1";
	protected State state;
	protected Mode mode;
	protected Pipeline pipe;
	protected AppSink RTCPSink;
	protected RTPBin rtpBin;
	protected Element windowSink;
}
