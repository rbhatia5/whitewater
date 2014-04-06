package vServer;

import org.gstreamer.*;
import org.gstreamer.elements.good.RTPBin;
import org.gstreamer.Pipeline;

public class vServerManager {

	private static Pipeline pipe;
	
	public static void main(String[] args)
	{
		
		
		
		//Instantiate a Server!!
		TCPServer serve = new TCPServer();
		serve.waitForResponse();
		serve.push("Hello client");
		serve.waitForResponse();
		serve.push("I am fine, thank you");






		args = Gst.init("Simple Pipeline", args);
		
		pipe = new Pipeline("server-pipeline");
	
		/*
		___________	   ______________    ______________    _____________    ____________________    __________________    _______________
		| v4l2src |	-> | ffenc_h263 | -> | rtph263pay | -> | gstrtpbin | -> | .send_rtp_sink_0 | -> |.send_rtp_src_0 | -> | udpsink 5001| 
		___________    ______________    ______________    _____________    ____________________    __________________    _______________
																|
																|			____________________    ________________
																|___\		| .send_rtcp_src_0 | -> | udpsink 5002 |
																|	/		____________________    ________________
																|			_______________    _____________________
																|___\		| udpsrc 5003 | -> | .recv_rtcp_sink_0 |
																	/		_______________    _____________________																	
		*/
		
		//Initialize elements
		Element source = ElementFactory.make("videotestsrc", "webcam");
		Element encoder = ElementFactory.make("ffenc_h263", "encoder");
		Element pay = ElementFactory.make("rtph263pay", "pay");
		RTPBin rtpBin = (RTPBin)ElementFactory.make("gstrtpbin", "rtp-bin"); 
		Element udpRTPSink = ElementFactory.make("udpsink", "udp-rtp-sink");
		Element udpRTCPSrc = ElementFactory.make("udpsrc", "udp-rtcp-src");
		Element udpRTCPSink = ElementFactory.make("udpsink", "udp-rtcp-sink");
		//Error check
		if(source == null || encoder == null || pay == null || rtpBin == null || udpRTPSink == null || udpRTCPSrc == null || udpRTCPSink == null)
			System.err.println("Could not create all elements");
		
		pipe.addMany(source, encoder, pay, rtpBin, udpRTPSink, udpRTCPSrc, udpRTCPSink);
		
		//Link link-able elements
		Element.linkMany(source, encoder, pay);
		
		//Send RTP packets on 5001
		udpRTPSink.set("host", "127.0.0.1");
		udpRTPSink.set("port", "5001");
		//Receive RTCP packets on 5003
		udpRTCPSrc.set("port", "5003");
		//Send RTCP packets on 5002
		udpRTCPSink.set("host", "127.0.0.1");
		udpRTCPSink.set("port", "5002");
		
		//Link sometimes pads manually
		rtpBin.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				System.out.printf("New pad %s added to %s\n", newPad.getName(), source.getName());
				if(newPad.getName().contains("send_rtp_src"))
				{
					Pad udpSinkPad = pipe.getElementByName("udp-rtp-sink").getStaticPad("sink");
					newPad.link(udpSinkPad);
				}
			}
		});
		
		//Link request pads manually
		Pad send_rtp_sink_0 = rtpBin.getRequestPad("send_rtp_sink_0");
		Pad paySrcPad = pay.getStaticPad("src");
		if(send_rtp_sink_0 == null || paySrcPad == null)
			System.err.println("Could not create rtpbin.send_rtp_sink_0 or pay.src pad");
		paySrcPad.link(send_rtp_sink_0);
		
		Pad send_rtcp_src_0 = rtpBin.getRequestPad("send_rtcp_src_0");
		Pad udpSinkPadRTCP = udpRTCPSink.getStaticPad("sink");
		if(send_rtcp_src_0 == null || udpSinkPadRTCP == null)
			System.err.println("Could not create rtpbin.send_rtcp_src_0 or udp.src pad");
		send_rtcp_src_0.link(udpSinkPadRTCP);
		
		Pad recv_rtcp_sink_0 = rtpBin.getRequestPad("recv_rtcp_sink_0");
		Pad udpSrcPadRTCP = udpRTCPSrc.getStaticPad("src");
		udpSrcPadRTCP.link(recv_rtcp_sink_0);
		
		pipe.setState(State.PLAYING);
		Gst.main();		
	}
}
