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
		
		
		
		
		
		
		args = Gst.init("Server Pipeline", args);
		
		pipe = new Pipeline("server-pipeline");
		
		Element source = ElementFactory.make("v4l2src", "webcam");
		Element encoder = ElementFactory.make("ffenc_h263", "encoder");
		Element pay = ElementFactory.make("rtph263pay", "pay");
		RTPBin rtpBin = (RTPBin)ElementFactory.make("gstrtpbin", "rtp-bin"); 
		Element udpSink = ElementFactory.make("udpsink", "udpsink");
		
		// RTCP communication elements:
		Element rtcpSrc = ElementFactory.make("udpsrc", "rtcpsrc");
		Element rtcpSink = ElementFactory.make("udpsink", "rtcpsink");
		
		// Set port communications for src
		rtcpSrc.set("port", "5002");
		//System.out.println(rtcpSrc.get("port"));
				
		// rtcpSink host and port
		rtcpSink.set("host", "127.0.0.1");
		rtcpSink.set("port", "5003");
		
		
		
		if(source == null || encoder == null || pay == null || rtpBin == null || udpSink == null)
			System.err.println("Could not create all elements");
		
		source.set("device", "/dev/video0");
		
		pipe.addMany(source, encoder, pay, rtpBin, udpSink);
		
		Element.linkMany(source, encoder, pay);
		
		udpSink.set("host", "127.0.0.1");
		udpSink.set("port", "5001");
		
		rtpBin.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				
				if(("send_rtp_src_0").equals(newPad.getName()))  {
				
					System.out.printf("New pad %s added to %s\n", newPad.getName(), source.getName());
					Pad udpSinkPad = pipe.getElementByName("udpsink").getStaticPad("sink");
					System.out.println(udpSinkPad.getName());
					
					System.out.println(source.getPads().toString());
					
					newPad.link(udpSinkPad);
				
				}
			}
		});
		
		Pad send_rtp_sink_0 = rtpBin.getRequestPad("send_rtp_sink_0");
		Pad paySrcPad = pay.getStaticPad("src");
		if(send_rtp_sink_0 == null || paySrcPad == null)
			System.err.println("Could not create rtpbin.send_rtp_sink_0 or pay.src pad");
		paySrcPad.link(send_rtp_sink_0);
		
		
		//Hook up rtcp udp src/sink
		Pad send_rtcp_src_0 = rtpBin.getRequestPad("send_rtcp_src_0");
		//System.out.println(rtpBin.getPads().toString());
		
		Pad send_rtcp_sink_0 = rtpBin.getRequestPad("recv_rtcp_sink_0");
		//System.out.println(rtpBin.getPads().toString());
		
		// Hook up udp src to rtpbin's rtcp0 sink
		rtcpSrc.getStaticPad("src").link(send_rtcp_sink_0);
		
		// Hook up rtpbin's rtcp0 src to udp sink which should connect to client 
		send_rtcp_src_0.link(rtcpSink.getStaticPad("sink"));
		
		
		
		
		pipe.setState(State.PLAYING);
		Gst.main();		
	}
}
