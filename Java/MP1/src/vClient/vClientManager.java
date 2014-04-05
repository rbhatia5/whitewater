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
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		args = Gst.init("Client Pipeline", args);
		pipe = new Pipeline("client-pipeline");
		
		Element udpSrc = ElementFactory.make("udpsrc", "udp-src");
		System.out.println("1 " + udpSrc);
		RTPBin rtpBin = (RTPBin)ElementFactory.make("gstrtpbin", "rtp-bin");
		System.out.println("2 " + rtpBin);
		Element depay = ElementFactory.make("rtph263depay", "depay");
		System.out.println("3 " + depay);
		Element decoder = ElementFactory.make("ffdec_h263", "decoder");
		System.out.println("4 " + decoder);
		Element sink = ElementFactory.make("xvimagesink", "sink");
		System.out.println("5 " + sink);
		
		Caps udpCaps = Caps.fromString("application/x-rtp,encoding-name=(string)H263,media=(string)video,clock-rate=(int)90000,payload=(int)96");
		System.out.println("6 " + udpCaps.toString());
		udpSrc.setCaps(udpCaps);
		udpSrc.set("port", "5001");
		
		rtpBin.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				if(newPad.getName().contains("recv_rtp_src_0")) {
					System.out.printf("New pad %s added to %s\n", newPad.toString(), source.toString());
					Pad depaySink = pipe.getElementByName("depay").getStaticPad("sink");
					PadLinkReturn ret = newPad.link(depaySink);
					System.out.println(ret.toString());
				}
			}
		});
		
		pipe.addMany(udpSrc, rtpBin, depay, decoder, sink);
		
		boolean link = Element.linkMany(udpSrc, rtpBin);
		System.out.println("9 " + link);
		link = Element.linkMany(depay, decoder, sink);
		System.out.println("10 " + link);
		
		
		
		
		pipe.setState(State.PLAYING);
		Gst.main();
	}
}
