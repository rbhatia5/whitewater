package vClient;


import org.gstreamer.*;
import org.gstreamer.elements.good.RTPBin;
import java.io.*;
import java.net.*;


public class vClientManager {

	private static Pipeline pipe;
	
	public static void main(String[] args)
	{
		Socket sock;
		try {
			sock = new Socket("localhost", 5002);
			TCPClient client = new TCPClient(sock);
			client.push("Hello Server!");
			System.out.println("Hello Server!");
			client.waitForResponse();
			//System.out.println("Server told Client: " + message);
			client.push("How are you?");
			System.out.println("How are you?");
			client.waitForResponse();
			
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		args = Gst.init("Simple Pipeline", args);
		pipe = new Pipeline("client-pipeline");
		
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
		Element sink = ElementFactory.make("xvimagesink", "sink");
		Element udpSrcRTCP = ElementFactory.make("udpsrc", "udp-src-rtcp");
		Element udpSinkRTCP = ElementFactory.make("udpsink", "udp-sink-rtcp");
		
		//Error check
		if(udpSrc == null || rtpBin == null || depay == null || decoder == null || sink == null || udpSrcRTCP == null || udpSinkRTCP == null)
			System.err.println("Could not create all elements");
	
		pipe.addMany(udpSrc, rtpBin, depay, decoder, sink);
		
		//Link link-able elements
		Element.linkMany(udpSrc, rtpBin);
		Element.linkMany(depay, decoder, sink);
		
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
				System.out.printf("New pad %s added to %s\n", newPad.toString(), source.toString());
				if(newPad.getName().contains("recv_rtp_src"))
				{
					Pad depaySink = pipe.getElementByName("depay").getStaticPad("sink");
					if(!depaySink.isLinked())
					{
						PadLinkReturn ret = newPad.link(depaySink);
						System.out.println(ret.toString());
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
		
		pipe.setState(State.PLAYING);
		Gst.main();
	}
}
