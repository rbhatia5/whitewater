package vClient;

import java.nio.ByteBuffer;


import org.gstreamer.*;
import org.gstreamer.Pad.EVENT_PROBE;

import org.gstreamer.elements.AppSink;
import org.gstreamer.elements.good.RTPBin;
import org.gstreamer.event.QOSEvent;
 

public class ClientPipelineManager{

	public static final byte SR = (byte)(200);
	public static final byte RR = (byte)(201);
	public static final byte SDES = (byte)(202);
	public static final byte BYE = (byte)(203);
	public static final byte APP = (byte)(204);

	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void modify_pipeline()
	{
		System.out.println("Initializing Client");
		discard_pipeline();
		client_pipeline();
		connect_to_signals();
		ClientData.data[ClientData.activeWindow].pipe.setState(State.READY);
	}


	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void discard_pipeline()
	{
		if(ClientData.data[ClientData.activeWindow].pipe != null)
		{
			//need to explicitly remove windowSink
			ClientData.data[ClientData.activeWindow].pipe.setState(State.NULL);
			ClientData.data[ClientData.activeWindow].pipe.remove(ClientData.data[ClientData.activeWindow].windowSink);
			ClientData.data[ClientData.activeWindow].pipe.remove(ClientData.data[ClientData.activeWindow].RTCPSink);
			ClientData.data[ClientData.activeWindow].RTCPSink = null;
			ClientData.data[ClientData.activeWindow].pipe.setState(State.NULL);
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
		ClientData.data[ClientData.activeWindow].pipe = new Pipeline("client-pipeline");

		/*
		_______________	   _____________    ___________________    _______________    ______________    _______________
		| udpsrc 5002 |	-> | gstrtpbin | -> | .recv_rtp_src_0 | -> | rtp263depay | -> | ffdec_h263 | -> | xvimagesink | 
		_______________    _____________    ___________________    _______________    ______________    _______________
								|
								|			____________________    ________________
								|___\		| .send_rtcp_src_0 | -> | udpsink 5004 |
								|	/		____________________    ________________
								|			_______________     _____________________
								|___\		| udpsrc 5003 |  -> | .recv_rtcp_sink_0 |
									/		_______________     _____________________		

	 	_______________	   _____________    ___________________    _________________    ____________    _________________
		| udpsrc 5005 |	-> | gstrtpbin | -> | .recv_rtp_src_1 | -> | rtpspeexdepay | -> | speexdec | -> | autoaudiosink | 
		_______________    _____________    ___________________    _________________    ____________    _________________
								|
								|			____________________    ________________
								|___\		| .send_rtcp_src_1 | -> | udpsink 5007 |
								|	/		____________________    ________________
								|			_______________     _____________________
								|___\		| udpsrc 5006 |  -> | .recv_rtcp_sink_1 |
									/		_______________     _____________________		


		 */



		//Initialize elements
		Element udpVideoSrc 		= ElementFactory.make("udpsrc", "udp-video-src");
		ClientData.data[ClientData.activeWindow].rtpBin = (RTPBin)ElementFactory.make("gstrtpbin", "rtp-bin");
		Element videodepay 			= ElementFactory.make("rtph263depay", "video-depay");
		Element videodecoder 		= ElementFactory.make("ffdec_h263", "video-decoder");
		Element udpVideoSrcRTCP 	= ElementFactory.make("udpsrc", "udp-video-src-rtcp");
		Element udpVideoSinkRTCP 	= ElementFactory.make("udpsink", "udp-video-sink-rtcp");
		Element udpAudioSrc 		= ElementFactory.make("udpsrc", "udp-audio-src");
		Element audiodepay 			= ElementFactory.make("rtpspeexdepay", "audio-depay");
		Element audiodecoder 		= ElementFactory.make("speexdec", "audio-decoder");
		Element audiosink 			= ElementFactory.make("autoaudiosink", "audio-sink");
		Element udpAudioSrcRTCP 	= ElementFactory.make("udpsrc", "udp-audio-src-rtcp");
		Element udpAudioSinkRTCP 	= ElementFactory.make("udpsink", "udp-audio-sink-rtcp");

		Element teeRTCP = ElementFactory.make("tee", "rtcp-tee");
		Element queueRTCP = ElementFactory.make("queue", "rtcp-queue");
		Element queueAppSink = ElementFactory.make("queue", "app-sink-queue");
		ClientData.data[ClientData.activeWindow].RTCPSink = (AppSink)ElementFactory.make("appsink", "rtcp-sink");

		//Error check
		//if(udpSrc == null || ClientData.rtpBin == null || depay == null || decoder == null || udpSrcRTCP == null || udpSinkRTCP == null || teeRTCP == null || queueRTCP == null || queueAppSink == null || ClientData.RTCPSink == null)
		if(udpVideoSrc == null || ClientData.data[ClientData.activeWindow].rtpBin == null || videodepay == null || videodecoder == null || udpVideoSrcRTCP == null || udpVideoSinkRTCP == null ||
				udpAudioSrc == null || audiodepay == null || audiodecoder == null || audiosink == null || udpAudioSrcRTCP == null || udpAudioSinkRTCP == null)
		{
			System.err.println("Could not create all elements");
		}

		if(ClientData.data[ClientData.activeWindow].mode == ClientData.Mode.ACTIVE){

			ClientData.data[ClientData.activeWindow].pipe.addMany(udpVideoSrc, ClientData.data[ClientData.activeWindow].rtpBin, videodepay, videodecoder, ClientData.data[ClientData.activeWindow].windowSink, udpVideoSrcRTCP, udpVideoSinkRTCP, udpAudioSrc, audiodepay, audiodecoder, audiosink, udpAudioSrcRTCP, udpAudioSinkRTCP);

			//Link link-able elements
			if(!Element.linkMany(udpVideoSrc, ClientData.data[ClientData.activeWindow].rtpBin))
				System.err.println("Could not link udp video to rtpbin");
			if(!Element.linkMany(videodepay, videodecoder, ClientData.data[ClientData.activeWindow].windowSink))
				System.err.println("Could not link video depay -> video decoder -> window sink");
			if(!Element.linkMany(udpAudioSrc, ClientData.data[ClientData.activeWindow].rtpBin))
				System.err.println("Could not link udp audio to rtpbin");
			if(!Element.linkMany(audiodepay, audiodecoder, audiosink))
				System.err.println("Could not link audio depay -> audio decoder -> audio sink");
			
			//Element.linkMany(udpVideoSrcRTCP, teeRTCP);
			//Element.linkMany(queueAppSink, ClientData.data[ClientData.activeWindow].RTCPSink);

			Caps udpVideoCaps = Caps.fromString("application/x-rtp,encoding-name=(string)H263,media=(string)video,clock-rate=(int)90000,payload=(int)96");
			udpVideoSrc.setCaps(udpVideoCaps);
			udpVideoSrc.set("port", ClientData.data[ClientData.activeWindow].videoRTP);
			udpVideoSrcRTCP.set("port", ClientData.data[ClientData.activeWindow].videoRTCPin);
			udpVideoSinkRTCP.set("host", ClientData.data[ClientData.activeWindow].serverAddress);
			udpVideoSinkRTCP.set("port", ClientData.data[ClientData.activeWindow].videoRTCPout);

			Caps udpAudioCaps = Caps.fromString("application/x-rtp,encoding-name=(string)SPEEX,media=(string)audio,clock-rate=(int)48000,payload=(int)96");
			udpAudioSrc.setCaps(udpAudioCaps);
			udpAudioSrc.set("port", ClientData.data[ClientData.activeWindow].audioRTP);
			udpAudioSrcRTCP.set("port", ClientData.data[ClientData.activeWindow].audioRTCPin);
			udpAudioSinkRTCP.set("host", ClientData.data[ClientData.activeWindow].serverAddress);
			udpAudioSinkRTCP.set("port", ClientData.data[ClientData.activeWindow].audioRTCPout);

			
		//	teeRTCP.set("silent", false);
		//	ClientData.data[ClientData.activeWindow].RTCPSink.set("emit-signals", true);

			//Link request pads manually
			PadLinkReturn ret = null;
			//Link rtcp source to udpsink
			Pad send_rtcp_src_0 = ClientData.data[ClientData.activeWindow].rtpBin.getRequestPad("send_rtcp_src_0");
			Pad udpVideoSinkPadRTCP = udpVideoSinkRTCP.getStaticPad("sink");
			ret = send_rtcp_src_0.link(udpVideoSinkPadRTCP);
			if(!ret.equals(PadLinkReturn.OK))
				System.err.printf("Could not link send_rtcp_src_0 to udpsink, %s\n", ret.toString());

			//Link queue to rtcp receiver
			Pad recv_rtcp_sink_0 = ClientData.data[ClientData.activeWindow].rtpBin.getRequestPad("recv_rtcp_sink_0");
			Pad udpVideoSrcPadRTCP = udpVideoSrcRTCP.getStaticPad("src");
			ret = udpVideoSrcPadRTCP.link(recv_rtcp_sink_0);
			if(!ret.equals(PadLinkReturn.OK))
				System.err.printf("Could not link udpsrc to recv_rtcp_sink_0, %s\n", ret.toString());

			Pad send_rtcp_src_1 = ClientData.data[ClientData.activeWindow].rtpBin.getRequestPad("send_rtcp_src_1");
			Pad udpAudioSinkPadRTCP = udpAudioSinkRTCP.getStaticPad("sink");
			ret = send_rtcp_src_1.link(udpAudioSinkPadRTCP);
			if(!ret.equals(PadLinkReturn.OK))
				System.err.printf("Could not link send_rtcp_src_1 to udpsink, %s\n", ret.toString());

			//Link queue to rtcp receiver
			Pad recv_rtcp_sink_1 = ClientData.data[ClientData.activeWindow].rtpBin.getRequestPad("recv_rtcp_sink_1");
			Pad udpAudioSrcPadRTCP = udpAudioSrcRTCP.getStaticPad("src");
			ret = udpAudioSrcPadRTCP.link(recv_rtcp_sink_1);
			if(!ret.equals(PadLinkReturn.OK))
				System.err.printf("Could not link udpsrc to recv_rtcp_sink_1, %s\n", ret.toString());
		}
		else if(ClientData.data[ClientData.activeWindow].mode == ClientData.Mode.PASSIVE){
			
			ClientData.data[ClientData.activeWindow].pipe.addMany(udpVideoSrc, ClientData.data[ClientData.activeWindow].rtpBin, videodepay, videodecoder, ClientData.data[ClientData.activeWindow].windowSink, udpVideoSrcRTCP, udpVideoSinkRTCP
					,teeRTCP,queueRTCP,queueAppSink,ClientData.data[ClientData.activeWindow].RTCPSink);

			//Link link-able elements
			if(!Element.linkMany(udpVideoSrc,ClientData.data[ClientData.activeWindow].rtpBin))
				System.err.println("Could not link udp video to rtpbin");
			if(!Element.linkMany(queueRTCP,ClientData.data[ClientData.activeWindow].windowSink))
				System.err.println("Could not link queue video to rtpbin");
			if(!Element.linkMany(videodepay, videodecoder,teeRTCP))
				
				
				System.err.println("Could not link video depay -> video decoder -> window sink");
			
			
			Element.linkMany(queueAppSink, ClientData.data[ClientData.activeWindow].RTCPSink);

			Caps udpVideoCaps = Caps.fromString("application/x-rtp,encoding-name=(string)H263,media=(string)video,clock-rate=(int)90000,payload=(int)96");
			udpVideoSrc.setCaps(udpVideoCaps);
			udpVideoSrc.set("port", ClientData.data[ClientData.activeWindow].videoRTP);
			udpVideoSrcRTCP.set("port", ClientData.data[ClientData.activeWindow].videoRTCPin);
			udpVideoSinkRTCP.set("host", ClientData.data[ClientData.activeWindow].serverAddress);
			udpVideoSinkRTCP.set("port", ClientData.data[ClientData.activeWindow].videoRTCPout);

			
			teeRTCP.set("silent", false);
			ClientData.data[ClientData.activeWindow].RTCPSink.set("emit-signals", true);

			//Link request pads manually
			PadLinkReturn ret = null;
			//Link rtcp source to udpsink
			Pad send_rtcp_src_0 = ClientData.data[ClientData.activeWindow].rtpBin.getRequestPad("send_rtcp_src_0");
			Pad udpVideoSinkPadRTCP = udpVideoSinkRTCP.getStaticPad("sink");
			ret = send_rtcp_src_0.link(udpVideoSinkPadRTCP);
			if(!ret.equals(PadLinkReturn.OK))
				System.err.printf("Could not link send_rtcp_src_0 to udpsink, %s\n", ret.toString());

			//Link queue to rtcp receiver
			Pad recv_rtcp_sink_0 = ClientData.data[ClientData.activeWindow].rtpBin.getRequestPad("recv_rtcp_sink_0");
			Pad udpVideoSrcPadRTCP = udpVideoSrcRTCP.getStaticPad("src");
			ret = udpVideoSrcPadRTCP.link(recv_rtcp_sink_0);
			if(!ret.equals(PadLinkReturn.OK))
				System.err.printf("Could not link udpsrc to recv_rtcp_sink_0, %s\n", ret.toString());
			
			
			//Link tee to queues
			Pad teeSrcPadRTCP = teeRTCP.getRequestPad("src%d");
			Pad teeSrcPadAppSink = teeRTCP.getRequestPad("src%d");
			Pad queueSinkPadRTCP = queueRTCP.getStaticPad("sink");
			Pad queueSinkPadAppSink = queueAppSink.getStaticPad("sink");
			ret = teeSrcPadRTCP.link(queueSinkPadRTCP);
			if(!ret.equals(PadLinkReturn.OK))
				System.err.printf("Could not link tee to RTCP queue, %s\n", ret.toString());
			ret = teeSrcPadAppSink.link(queueSinkPadAppSink);
			if(!ret.equals(PadLinkReturn.OK))
				System.err.printf("Could not link tee to appsink queue, %s\n", ret.toString());
			
			Pad windowSink = ClientData.data[ClientData.activeWindow].windowSink.getStaticPad("sink");
			windowSink.addEventProbe(new EVENT_PROBE() {
				@Override
				public boolean eventReceived(Pad arg0, Event arg1) {
					if(arg0.equals( ClientData.data[ClientData.activeWindow].windowSink.getStaticPad("sink")))
					{
						
						System.out.println("Pad is : " + arg0.getNativeAddress().toString() + ", Global Pad is : " + ClientData.data[ClientData.activeWindow].windowSink.getStaticPad("sink").getNativeAddress().toString());
						
						//System.out.println("Event is : " + arg1.getStructure().toString());
					
						ClientGUIManager.addTextToMonitor("Jitter is : "+((QOSEvent) arg1).getDifference()/Math.pow(10,6));
					}
					return false;
					
				}
				
			});

		}

		
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
		ClientData.data[ClientData.activeWindow].pipe.getBus().connect(new Bus.TAG() {
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
		ClientData.data[ClientData.activeWindow].pipe.getBus().connect(new Bus.EOS() {
			public void endOfStream(GstObject source) {
				//exit gracefully
				System.out.printf("[%s} reached EOS.\n", source);
				Gst.quit();
			}
		});

		//connect to signal ERROR
		ClientData.data[ClientData.activeWindow].pipe.getBus().connect(new Bus.ERROR() {
			@Override
			public void errorMessage(GstObject source, int code, String message) {
				//print error from message
				System.out.printf("[%s] encountered error code %d: %s\n",source, code, message);
				Gst.quit();
			}
		});

		//connect to change of state
		ClientData.data[ClientData.activeWindow].pipe.getBus().connect(new Bus.STATE_CHANGED() {
			public void stateChanged(GstObject source, State oldstate, State newstate, State pending) {
				if(source.equals(ClientData.data[ClientData.activeWindow].pipe))
				{
					System.out.printf("[%s] changed state from %s to %s\n", source.getName(), oldstate.toString(), newstate.toString());
				}
				else if(source.equals(ClientData.data[ClientData.activeWindow].RTCPSink))
				{
					if(newstate.equals(State.PLAYING))
						ClientData.data[ClientData.activeWindow].pipe.setState(State.PLAYING);
					else if(newstate.equals(State.PAUSED))
						ClientData.data[ClientData.activeWindow].pipe.setState(State.PLAYING);
				}
			}
		});

		//connect to new buffer
		
		ClientData.data[ClientData.activeWindow].RTCPSink.connect(new AppSink.NEW_BUFFER() {
			public void newBuffer(AppSink source) {
				
				
				Buffer RTCPBuffer = source.pullBuffer();
				if(source.equals(ClientData.data[ClientData.activeWindow].RTCPSink))
					decodeRTCPPacket(RTCPBuffer);
			}
		});
		

		//Link sometimes pads on RTPBin
		ClientData.data[ClientData.activeWindow].rtpBin.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				System.out.printf("New pad %s added to %s\n", newPad.toString(), source.toString());
				if(newPad.getName().contains("recv_rtp_src_0"))
				{
					Pad depaySink = ClientData.data[ClientData.activeWindow].pipe.getElementByName("video-depay").getStaticPad("sink");
					if(!depaySink.isLinked())
					{
						newPad.link(depaySink);
					}
				}
			}
		});

		ClientData.data[ClientData.activeWindow].rtpBin.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				System.out.printf("New pad %s added to %s\n", newPad.toString(), source.toString());
				if(newPad.getName().contains("recv_rtp_src_1"))
				{
					Pad depaySink = ClientData.data[ClientData.activeWindow].pipe.getElementByName("audio-depay").getStaticPad("sink");
					if(!depaySink.isLinked())
					{
						newPad.link(depaySink);
					}
				}
			}
		});

		//For fetching RTCP packets
		ClientData.data[ClientData.activeWindow].rtpBin.connect(new RTPBin.ON_NEW_SSRC() {
			public void onNewSsrc(RTPBin rtpBin, int sessionid, int ssrc) {
				//System.out.printf("1 : RTCP packet received from ssrc: %s session: %s\n", ssrc, sessionid);
				//Pointer session = new Pointer(sessionid);
				//System.out.println(session.toString());
				//rtpBin.emit("get-internal-session", sessionid, session.getPointer(0));
				//System.out.println("SDES OBJ: " + sdesObj);
				//Pointer sessionObj = GObjectAPI.GOBJECT_API.g_object_new(GType.POINTER, sdesObj);
				//System.out.println("HERE" + sessionObj.toString());
			}
		});

		ClientData.data[ClientData.activeWindow].rtpBin.connect(new RTPBin.ON_SSRC_SDES() {
			public void onSsrcSdes(RTPBin rtpBin, int sessionid, int ssrc) {
				//System.out.printf("2 : RTCP packet received from ssrc: %s session: %s\n", ssrc, sessionid);
			}
		});

		ClientData.data[ClientData.activeWindow].rtpBin.connect(new RTPBin.ON_SSRC_ACTIVE() {
			public void onSsrcActive(RTPBin rtpBin, int sessionid, int ssrc) {
				//System.out.printf("3 : RTCP packet received from ssrc: %s session: %s\n", ssrc, sessionid);
			}
		});

		ClientData.data[ClientData.activeWindow].rtpBin.connect(new RTPBin.ON_BYE_SSRC() {
			public void onByeSsrc(RTPBin rtpBin, int sessionid, int ssrc) {
				System.out.printf("4 : RTCP packet received BYE from ssrc: %s session: %s\n", ssrc, sessionid);
			}
		});
	}


	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	static void decodeRTCPPacket(Buffer buffer)
	{
		
		if(buffer != null){
			Caps caps = buffer.getCaps();
			if(caps == null){
				return;
			}
			
			Structure s = caps.getStructure(0);
			
			//int fps =  s.getInteger("framerate") * 8 ; // bits per pixel
			Fraction fps = s.getFraction("framerate");
			int intFps = (int)fps.toDouble();
			ClientGUIManager.addTextToFramerateMonitor("Framerate is : " + intFps);
			
			
//			int width = s.getInteger("width");
//			int height = s.getInteger("height");
//			
//			if((width == 0) || (height ==0)){
//				System.err.println("Could not get sought dimension");
//			}
//			
//			
//			System.out.println("Buffer information: ");
//			System.out.println("\tWidth     : " + width);
//			System.out.println("\tHeight    : " + height);
//			System.out.println("\tSize      : " + buffer.getSize());
//			System.out.println("\tDuration  : " + buffer.getDuration().getNanoSeconds() + "ns");
//			System.out.println("\tOffset    : " + buffer.getOffset());
//			System.out.println("\tTimestamp : " + buffer.getTimestamp().getNanoSeconds() + "ns");
			//ClientGUIManager.addTextToFramerateMonitor(s.toString());
			
		}
		
		
		/*
		Byte RC;
		Byte PT;
		int SSRC_S;
		int SSRC_R;
		Byte fractionLost;
		int cumLost;
		int jitter; 
		RC = buffer.get(0);
		RC = (byte) (RC & 0x1F);
		PT = buffer.get(1);
		//System.out.printf("PT RECEIVED: %s\n", (256 + PT));
		if(PT == SR)
		{
			SSRC_S = buffer.getInt(4);
			SSRC_R = buffer.getInt(28);
			fractionLost = buffer.get(32);
			cumLost = buffer.getInt(32) & 0x00FFFFFF;
			jitter = buffer.getInt(40);
			System.out.printf("Received buffer of length %s\n", buffer.capacity());
			System.out.printf("SSRC Sender: %s SSRC Receiver: %s Fraction Lost: %s Total Lost: %s Jitter: %s\n" , 2*(Integer.MAX_VALUE + 1) + SSRC_S, 2*(Integer.MAX_VALUE + 1) + SSRC_R, 256 + fractionLost, cumLost, 2*(Integer.MAX_VALUE + 1) + jitter);
		}*/
	}
}
