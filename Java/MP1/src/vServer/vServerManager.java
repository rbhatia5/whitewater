package vServer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

import org.gstreamer.*;
import org.gstreamer.swing.*;
import org.gstreamer.elements.*;
import org.gstreamer.elements.good.RTPBin;
import org.gstreamer.Pipeline;

public class vServerManager {

	private static Pipeline pipe;
	
	public static void main(String[] args)
	{
		args = Gst.init("Simple Pipeline", args);
		
		pipe = new Pipeline("server-pipeline");
		
		Element source = ElementFactory.make("v4l2src", "webcam");
		Element encoder = ElementFactory.make("ffenc_h263", "encoder");
		Element pay = ElementFactory.make("rtph263pay", "pay");
		RTPBin rtpBin = (RTPBin)ElementFactory.make("gstrtpbin", "rtp-bin"); 
		Element udpSink = ElementFactory.make("udpsink", "udpsink");
		
		if(source == null || encoder == null || pay == null || rtpBin == null || udpSink == null)
			System.err.println("Could not create all elements");
		
		pipe.addMany(source, encoder, pay, rtpBin, udpSink);
		
		Element.linkMany(source, encoder, pay);
		
		udpSink.set("host", "127.0.0.1");
		udpSink.set("port", "5001");
		
		rtpBin.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				System.out.printf("New pad %s added to %s\n", newPad.getName(), source.getName());
				Pad udpSinkPad = pipe.getElementByName("udpsink").getStaticPad("sink");
				System.out.println(udpSinkPad.getName());
				newPad.link(udpSinkPad);
			}
		});
		
		Pad send_rtp_sink_0 = rtpBin.getRequestPad("send_rtp_sink_0");
		Pad paySrcPad = pay.getStaticPad("src");
		if(send_rtp_sink_0 == null || paySrcPad == null)
			System.err.println("Could not create rtpbin.send_rtp_sink_0 or pay.src pad");
		paySrcPad.link(send_rtp_sink_0);
		
		pipe.setState(State.PLAYING);
		Gst.main();		
	}
}
