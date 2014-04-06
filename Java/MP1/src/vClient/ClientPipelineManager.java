package vClient;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.gstreamer.*;
import org.gstreamer.elements.AppSink;
import org.gstreamer.elements.good.RTPBin;
import org.gstreamer.event.SeekEvent;

public class ClientPipelineManager{

	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void modify_pipeline()
	{
		switch(ClientData.mode)
		{
		case CLIENT:
			System.out.println("Initializing Client");
			discard_pipeline();
			client_pipeline();
			connect_to_signals();
			ClientData.pipe.setState(State.READY);
			break;
		default:
			System.out.println("Unrecognized pipeline");
			ClientData.pipe.setState(State.READY);
			break;
		}
	}
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void discard_pipeline()
	{
		if(ClientData.pipe != null)
		{
			//need to explicitly remove windowSink
			ClientData.pipe.setState(State.READY);
			ClientData.pipe.remove(ClientData.windowSink);
			ClientData.pipe.remove(ClientData.RTCPSink);
			ClientData.RTCPSink = null;
			ClientData.pipe.setState(State.NULL);
		}
	}
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void client_pipeline()
	{
		ClientData.pipe = new Pipeline("client-pipeline");
		
		/*
		_______________	   _____________    ___________________    _______________    ______________    _______________
		| udpsrc 5001 |	-> | gstrtpbin | -> | .recv_rtp_src_0 | -> | rtp263depay | -> | ffdec_h263 | -> | xvimagesink | 
		_______________    _____________    ___________________    _______________    ______________    _______________
								|
								|			____________________    ________________
								|___\		| .send_rtcp_src_0 | -> | udpsink 5003 |
								|	/		____________________    ________________
								|			_______________    _____________________
								|___\		| udpsrc 5002 | -> | .recv_rtcp_sink_0 |
									/		_______________    _____________________																	
		*/
		//Initialize elements
		Element udpSrc = ElementFactory.make("udpsrc", "udp-src");
		RTPBin rtpBin = (RTPBin)ElementFactory.make("gstrtpbin", "rtp-bin");
		Element depay = ElementFactory.make("rtph263depay", "depay");
		Element decoder = ElementFactory.make("ffdec_h263", "decoder");
		Element udpSrcRTCP = ElementFactory.make("udpsrc", "udp-src-rtcp");
		Element udpSinkRTCP = ElementFactory.make("udpsink", "udp-sink-rtcp");
		
		//Error check
		if(udpSrc == null || rtpBin == null || depay == null || decoder == null || udpSrcRTCP == null || udpSinkRTCP == null)
			System.err.println("Could not create all elements");
	
		ClientData.pipe.addMany(udpSrc, rtpBin, depay, decoder, ClientData.windowSink);
		
		//Link link-able elements
		Element.linkMany(udpSrc, rtpBin);
		Element.linkMany(depay, decoder, ClientData.windowSink);
		
		//Receive RTP packets on 5001
		Caps udpCaps = Caps.fromString("application/x-rtp,encoding-name=(string)H263,media=(string)video,clock-rate=(int)90000,payload=(int)96");
		System.out.println("Caps: " + udpCaps.toString());
		udpSrc.setCaps(udpCaps);
		udpSrc.set("port", "5001");
		//Receive RTCP packets on 5002
		udpSrcRTCP.set("port", "5002");
		//Send RTP packets on 5003
		udpSinkRTCP.set("host", "127.0.0.1");
		udpSinkRTCP.set("port", "5003");
		
		//Link sometimes pads manually
		rtpBin.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				//System.out.printf("New pad %s added to %s\n", newPad.toString(), source.toString());
				if(newPad.getName().contains("recv_rtp_src"))
				{
					Pad depaySink = ClientData.pipe.getElementByName("depay").getStaticPad("sink");
					if(!depaySink.isLinked())
					{
						newPad.link(depaySink);
					}
				}
			}
		});
		
		//Link request pads manually
		Pad send_rtcp_src_0 = rtpBin.getRequestPad("send_rtcp_src_0");
		Pad udpSinkPadRTCP = udpSinkRTCP.getStaticPad("sink");
		send_rtcp_src_0.link(udpSinkPadRTCP);
		
		Pad recv_rtcp_sink_0 = rtpBin.getRequestPad("recv_rtcp_sink_0");
		Pad udpSrcPadRTCP = udpSrcRTCP.getStaticPad("src");
		udpSrcPadRTCP.link(recv_rtcp_sink_0);
	}
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void connect_to_signals()
	{
		//connect to signal TAG
		ClientData.pipe.getBus().connect(new Bus.TAG() {
			public void tagsFound(GstObject source, TagList tagList) {
				for(String tagName : tagList.getTagNames())
				{
					for(Object tagData : tagList.getValues(tagName))
					{
						String data = "[" + tagName + "] = " + tagData + " \n";
						ClientData.monitor.append(data);
					}
				}
			}
		});
		
		//connect to signal EOS
		ClientData.pipe.getBus().connect(new Bus.EOS() {
			public void endOfStream(GstObject source) {
				//exit gracefully
				System.out.printf("[%s} reached EOS.\n", source);
				Gst.quit();
			}
		});
		
		//connect to signal ERROR
		ClientData.pipe.getBus().connect(new Bus.ERROR() {
			@Override
			public void errorMessage(GstObject source, int code, String message) {
				//print error from message
				System.out.printf("[%s] encountered error code %d: %s\n",source, code, message);
				Gst.quit();
			}
		});
		
		//connect to change of state
		ClientData.pipe.getBus().connect(new Bus.STATE_CHANGED() {
			public void stateChanged(GstObject source, State oldstate, State newstate, State pending) {
				if(source.equals(ClientData.pipe))
				{
					System.out.printf("[%s] changed state from %s to %s\n", source.getName(), oldstate.toString(), newstate.toString());
				}
				else if(source.equals(ClientData.RTCPSink))
				{
					if(newstate.equals(State.PLAYING))
						ClientData.pipe.setState(State.PLAYING);
					else if(newstate.equals(State.PAUSED))
						ClientData.pipe.setState(State.PAUSED);
				}
			}
		});
	}
}
