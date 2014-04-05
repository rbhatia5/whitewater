package vClient;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

import org.gstreamer.*;
import org.gstreamer.swing.*;
import org.gstreamer.elements.*;
import org.gstreamer.elements.good.RTPBin;

public class vClientManager {

	private static Pipeline pipe;
	
	public static void main(String[] args)
	{
		args = Gst.init("Simple Pipeline", args);
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
				System.out.printf("New pad %s added to %s\n", newPad.toString(), source.toString());
				Pad depaySink = pipe.getElementByName("depay").getStaticPad("sink");
				PadLinkReturn ret = newPad.link(depaySink);
				System.out.println(ret.toString());
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
