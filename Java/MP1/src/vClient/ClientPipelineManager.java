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
	
	protected static boolean hasLastBuf = false;

	protected static Buffer lastBuf = null;
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
			ClientData.data[ClientData.activeWindow].pipe.remove(ClientData.data[ClientData.activeWindow].windowAppSink);
			ClientData.data[ClientData.activeWindow].windowAppSink = null;
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
PASSIVE/ACTIVE MODE
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
ACTIVE MODE
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

		//Make video pipeline

		//Initialize elements
		Element udpVideoSrc 		= ElementFactory.make("udpsrc", "udp-video-src");
		ClientData.data[ClientData.activeWindow].rtpBin = (RTPBin)ElementFactory.make("gstrtpbin", "rtp-bin");
		Element videodepay 			= ElementFactory.make("rtph263depay", "video-depay");
		Element videodecoder 		= ElementFactory.make("ffdec_h263", "video-decoder");
		Element udpVideoSrcRTCP 	= ElementFactory.make("udpsrc", "udp-video-src-rtcp");
		Element udpVideoSinkRTCP 	= ElementFactory.make("udpsink", "udp-video-sink-rtcp");

		//Window appsink
		Element windowTee = ElementFactory.make("tee", "window-tee");
		Element windowQueue = ElementFactory.make("queue", "window-queue");
		Element windowAppQueue = ElementFactory.make("queue", "window-app-sink-queue");
		ClientData.data[ClientData.activeWindow].windowAppSink = (AppSink)ElementFactory.make("appsink", "window-app-sink");

		//udp appsink
		Element udpVideoTee = ElementFactory.make("tee", "udp-video-tee");
		Element udpVidQueue = ElementFactory.make("queue", "udp-video-queue");
		Element udpVidAppQueue = ElementFactory.make("queue", "udp-vid-app-sink-queue");
		ClientData.data[ClientData.activeWindow].udpVideoAppSink = (AppSink)ElementFactory.make("appsink", "udp-vid-app-sink");

		//Error check
		if(udpVideoSrc == null || ClientData.data[ClientData.activeWindow].rtpBin == null || videodepay == null || videodecoder == null || udpVideoSrcRTCP == null || udpVideoSinkRTCP == null)
		{
			System.err.println("Could not create all elements");
		}

		if(udpVideoTee == null || udpVidQueue == null || udpVidAppQueue==null || ClientData.data[ClientData.activeWindow].udpVideoAppSink==null)
			System.err.println("Could not udp appsink elements");

		ClientData.data[ClientData.activeWindow].pipe.addMany(udpVideoSrc, ClientData.data[ClientData.activeWindow].rtpBin, videodepay, videodecoder, ClientData.data[ClientData.activeWindow].windowSink, udpVideoSrcRTCP, udpVideoSinkRTCP,
				udpVideoTee,udpVidQueue,udpVidAppQueue,ClientData.data[ClientData.activeWindow].udpVideoAppSink,
				windowTee,windowQueue,windowAppQueue,ClientData.data[ClientData.activeWindow].windowAppSink);

		//Link link-able elements
		//udp and rtpbin
		if(!Element.linkMany(udpVideoSrc,udpVideoTee))
			System.err.println("Could not link udp video to tee");
		if(!Element.linkMany(udpVidQueue,ClientData.data[ClientData.activeWindow].rtpBin))
			System.err.println("Could not link udpQ with rtp bin");
		if(!Element.linkMany(udpVidAppQueue, ClientData.data[ClientData.activeWindow].udpVideoAppSink))
			System.err.println("Could not connect udpvid appQ -> udp appsink");

		//Window sink and appsink
		if(!Element.linkMany(windowQueue,ClientData.data[ClientData.activeWindow].windowSink))
			System.err.println("Could not link queue video to window");
		if(!Element.linkMany(videodepay, videodecoder,windowTee))
			System.err.println("Could not link video depay -> video decoder -> window tee");
		if(!Element.linkMany(windowAppQueue, ClientData.data[ClientData.activeWindow].windowAppSink))
			System.err.println("Could not link appsink queue -> window app sink");

		Caps udpVideoCaps = Caps.fromString("application/x-rtp,encoding-name=(string)H263,media=(string)video,clock-rate=(int)90000,payload=(int)96");
		udpVideoSrc.setCaps(udpVideoCaps);
		udpVideoSrc.set("port", ClientData.data[ClientData.activeWindow].videoRTP);
		udpVideoSrcRTCP.set("port", ClientData.data[ClientData.activeWindow].videoRTCPin);
		udpVideoSinkRTCP.set("host", ClientData.data[ClientData.activeWindow].serverAddress);
		udpVideoSinkRTCP.set("port", ClientData.data[ClientData.activeWindow].videoRTCPout);

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

		//Link udp tee to queues
		Pad udpVideoTeeSrcPad = udpVideoTee.getRequestPad("src%d");
		Pad udpVidAppTeeSrcPad = udpVideoTee.getRequestPad("src%d");
		Pad udpVidQueueSinkPad = udpVidQueue.getStaticPad("sink");
		Pad udpVidAppQueueSinkPad = udpVidAppQueue.getStaticPad("sink");
		ret = udpVideoTeeSrcPad.link(udpVidQueueSinkPad);
		if(!ret.equals(PadLinkReturn.OK))
			System.err.printf("UDP: Could not link tee to queue, %s\n", ret.toString());
		ret = udpVidAppTeeSrcPad.link(udpVidAppQueueSinkPad);
		if(!ret.equals(PadLinkReturn.OK))
			System.err.printf("UDP: Could not link tee to appsink queue, %s\n", ret.toString());

		udpVideoTee.set("silent", false);
		ClientData.data[ClientData.activeWindow].udpVideoAppSink.set("emit-signals", true);

		//Link window tee to queues
		Pad windowTeeSrcPad = windowTee.getRequestPad("src%d");
		Pad windowAppTeeSrcPad = windowTee.getRequestPad("src%d");
		Pad windowQueueSinkPad = windowQueue.getStaticPad("sink");
		Pad windowAppQueueSinkPad = windowAppQueue.getStaticPad("sink");
		ret = windowTeeSrcPad.link(windowQueueSinkPad);
		if(!ret.equals(PadLinkReturn.OK))
			System.err.printf("Could not link tee to RTCP queue, %s\n", ret.toString());
		ret = windowAppTeeSrcPad.link(windowAppQueueSinkPad);
		if(!ret.equals(PadLinkReturn.OK))
			System.err.printf("Could not link tee to appsink queue, %s\n", ret.toString());

		windowTee.set("silent", false);
		ClientData.data[ClientData.activeWindow].windowAppSink.set("emit-signals", true);
		//QOS event listener
		Pad windowSink = ClientData.data[ClientData.activeWindow].windowSink.getStaticPad("sink");
		windowSink.addEventProbe(new EVENT_PROBE() {
			public boolean eventReceived(Pad arg0, Event arg1) {
				if(arg0.equals( ClientData.data[ClientData.activeWindow].windowSink.getStaticPad("sink")))
				{
					try {
						ClientGUIManager.addTextToMonitor(Double.toString(((QOSEvent) arg1).getDifference()/Math.pow(10,6)));
					} catch (ClassCastException e) {
						
					}
				}
				return false;
			}
		});

		//Add the audio pipeline if the session is Active
		if(ClientData.data[ClientData.activeWindow].mode == ClientData.Mode.ACTIVE){

			Element udpAudioSrc 		= ElementFactory.make("udpsrc", "udp-audio-src");
			Element audiodepay 			= ElementFactory.make("rtpspeexdepay", "audio-depay");
			Element audiodecoder 		= ElementFactory.make("speexdec", "audio-decoder");
			Element volume 				= ElementFactory.make("volume", "volume");
			Element audiosink 			= ElementFactory.make("autoaudiosink", "audio-sink");
			Element udpAudioSrcRTCP 	= ElementFactory.make("udpsrc", "udp-audio-src-rtcp");
			Element udpAudioSinkRTCP 	= ElementFactory.make("udpsink", "udp-audio-sink-rtcp");

			//audio out appsink
			Element audioTee = ElementFactory.make("tee", "audio-tee");
			Element audioQueue = ElementFactory.make("queue", "audio-queue");
			Element audioAppQueue = ElementFactory.make("queue", "audio-app-sink-queue");
			ClientData.data[ClientData.activeWindow].audioOutAppsink = (AppSink)ElementFactory.make("appsink", "audio-app-sink");

			//udp audio appsink
			Element udpAudioTee = ElementFactory.make("tee", "udp-audio-tee");
			Element udpAudQueue = ElementFactory.make("queue", "udp-audio-queue");
			Element udpAudAppQueue = ElementFactory.make("queue", "udp-aud-app-sink-queue");
			ClientData.data[ClientData.activeWindow].udpAudioAppSink = (AppSink)ElementFactory.make("appsink", "udp-aud-app-sink");
			
			//for muting
			ClientData.data[ClientData.activeWindow].volume = volume;
			volume.set("mute", false);
			
			if(udpAudioSrc == null || audiodepay == null || audiodecoder == null || audiosink == null || udpAudioSrcRTCP == null || udpAudioSinkRTCP == null)
				System.err.println("Could not create all elements");

			if(audioTee==null||audioQueue==null||audioAppQueue==null||udpAudioTee==null||udpAudQueue==null||udpAudAppQueue==null||
					ClientData.data[ClientData.activeWindow].audioOutAppsink ==null||ClientData.data[ClientData.activeWindow].udpAudioAppSink==null)
				System.err.println("Could not create all app elements");


			ClientData.data[ClientData.activeWindow].pipe.addMany(udpAudioSrc, audiodepay, audiodecoder, volume, audiosink, udpAudioSrcRTCP, udpAudioSinkRTCP);
			ClientData.data[ClientData.activeWindow].pipe.addMany(audioTee,audioQueue,audioAppQueue,udpAudioTee,udpAudQueue,udpAudAppQueue, 
					ClientData.data[ClientData.activeWindow].audioOutAppsink,ClientData.data[ClientData.activeWindow].udpAudioAppSink );

			//Audio
			if(!Element.linkMany(udpAudioSrc,udpAudioTee))
				System.err.println("1 Could not link udp audio to rtpbin");
			if(!Element.linkMany(udpAudQueue, ClientData.data[ClientData.activeWindow].rtpBin))
				System.err.println("2 Could not link udp audio to rtpbin");

			if(!Element.linkMany(audiodepay, audiodecoder,audioTee))
				System.err.println("1 Could not link audio depay -> audio decoder -> audio sink");
			if(!Element.linkMany(audioQueue, volume, audiosink))
				System.err.println("2 Could not link audio depay -> audio decoder -> audio sink");

			if(!Element.linkMany(audioAppQueue,ClientData.data[ClientData.activeWindow].audioOutAppsink))
				System.err.println("1 Could not connect aud appsinks");
			if(!Element.linkMany(udpAudAppQueue,ClientData.data[ClientData.activeWindow].udpAudioAppSink))
				System.err.println("2 Could not connect aud appsinks");

			Caps udpAudioCaps = Caps.fromString("application/x-rtp,encoding-name=(string)SPEEX,media=(string)audio,clock-rate=(int)48000,payload=(int)96");
			udpAudioSrc.setCaps(udpAudioCaps);
			udpAudioSrc.set("port", ClientData.data[ClientData.activeWindow].audioRTP);
			udpAudioSrcRTCP.set("port", ClientData.data[ClientData.activeWindow].audioRTCPin);
			udpAudioSinkRTCP.set("host", ClientData.data[ClientData.activeWindow].serverAddress);
			udpAudioSinkRTCP.set("port", ClientData.data[ClientData.activeWindow].audioRTCPout);

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


			//Link udp tee to queues
			Pad udpAudioTeeSrcPad = udpAudioTee.getRequestPad("src%d");
			Pad udpAudAppTeeSrcPad = udpAudioTee.getRequestPad("src%d");
			Pad udpAudQueueSinkPad = udpAudQueue.getStaticPad("sink");
			Pad udpAudAppQueueSinkPad = udpAudAppQueue.getStaticPad("sink");
			ret = udpAudioTeeSrcPad.link(udpAudQueueSinkPad);
			if(!ret.equals(PadLinkReturn.OK))
				System.err.printf("UDP: Could not link tee to queue, %s\n", ret.toString());
			ret = udpAudAppTeeSrcPad.link(udpAudAppQueueSinkPad);
			if(!ret.equals(PadLinkReturn.OK))
				System.err.printf("UDP: Could not link tee to appsink queue, %s\n", ret.toString());

			udpAudioTee.set("silent", false);
			ClientData.data[ClientData.activeWindow].udpAudioAppSink.set("emit-signals", true);

			//Link window tee to queues
			Pad audioTeeSrcPad = audioTee.getRequestPad("src%d");
			Pad audioAppTeeSrcPad = audioTee.getRequestPad("src%d");
			Pad audioQueueSinkPad = audioQueue.getStaticPad("sink");
			Pad audioAppQueueSinkPad = audioAppQueue.getStaticPad("sink");
			ret = audioTeeSrcPad.link(audioQueueSinkPad);
			if(!ret.equals(PadLinkReturn.OK))
				System.err.printf("Could not link tee to RTCP queue, %s\n", ret.toString());
			ret = audioAppTeeSrcPad.link(audioAppQueueSinkPad);
			if(!ret.equals(PadLinkReturn.OK))
				System.err.printf("Could not link tee to appsink queue, %s\n", ret.toString());

			audioTee.set("silent", false);
			ClientData.data[ClientData.activeWindow].audioOutAppsink.set("emit-signals", true);

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
				else if(source.equals(ClientData.data[ClientData.activeWindow].windowAppSink))
				{
					/*
					if(newstate.equals(State.PLAYING))
						ClientData.data[ClientData.activeWindow].pipe.setState(State.PLAYING);
					else if(newstate.equals(State.PAUSED))
						ClientData.data[ClientData.activeWindow].pipe.setState(State.PLAYING);
					 */
				}
			}
		});

		//connect to new buffer
		ClientData.data[ClientData.activeWindow].windowAppSink.connect(new AppSink.NEW_BUFFER() {
			public void newBuffer(AppSink source) {


				Buffer windowBuffer = source.pullBuffer();
				if(source.equals(ClientData.data[ClientData.activeWindow].windowAppSink))
					pullWindowSinkBuff(windowBuffer);
			}
		});


		ClientData.data[ClientData.activeWindow].udpVideoAppSink.connect(new AppSink.NEW_BUFFER() {
			public void newBuffer(AppSink source) {


				Buffer udpVidBuffer = source.pullBuffer();
				if(source.equals(ClientData.data[ClientData.activeWindow].udpVideoAppSink))
					pullUdpVidBuff(udpVidBuffer);
			}
		});

		if(ClientData.data[ClientData.activeWindow].mode == ClientData.Mode.ACTIVE){

			ClientData.data[ClientData.activeWindow].udpAudioAppSink.connect(new AppSink.NEW_BUFFER() {
				public void newBuffer(AppSink source) {


					Buffer buf = source.pullBuffer();
					if(source.equals(ClientData.data[ClientData.activeWindow].udpVideoAppSink))
						pullUdpAudBuff(buf);
				}
			});

			ClientData.data[ClientData.activeWindow].audioOutAppsink.connect(new AppSink.NEW_BUFFER() {
				public void newBuffer(AppSink source) {
					Buffer buf = source.pullBuffer();
					if(source.equals(ClientData.data[ClientData.activeWindow].udpVideoAppSink))
						pullAudioBuff(buf);
				}
			});

		}

		//Link sometimes pads on RTPBin
		ClientData.data[ClientData.activeWindow].rtpBin.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				System.out.println("Pad added: recv_rtp_src_0");
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
				System.out.println("Pad added: recv_rtp_src_1");
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
	}

	static void pullUdpVidBuff(Buffer buffer) {

		if(buffer != null){
			Caps caps = buffer.getCaps();
			if(caps == null){
				return;
			}
			ClientData.vidBandwidth = buffer.getSize();
			if(ClientData.data[ClientData.activeWindow].mode.equals(ClientData.Mode.ACTIVE))
			{
				try {
					ClientGUIManager.addTextToBandwidthMonitor(Integer.toString(ClientData.vidBandwidth + ClientData.audBandwidth));
				} catch(NullPointerException e) {
					ClientGUIManager.addTextToBandwidthMonitor(Integer.toString(ClientData.vidBandwidth));
				}
			}
			else
				ClientGUIManager.addTextToBandwidthMonitor(Integer.toString(ClientData.vidBandwidth));
		}
	}

	static void pullUdpAudBuff(Buffer buffer){
		if(buffer != null){
			Caps caps = buffer.getCaps();
			if(caps == null){
				return;
			}
			ClientData.audBandwidth = buffer.getSize();
		}
	}

	static void pullAudioBuff(Buffer buffer){
		if(buffer != null){
			Caps caps = buffer.getCaps();
			if(caps == null){
				return;
			}
			//ClientGUIManager.addTextToFramerateMonitor("Size 3 is :" + Integer.toString(buffer.getSize()));
		}
	}

	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	static void pullWindowSinkBuff(Buffer buffer)
	{
		
		if(buffer != null) {
			
			Caps caps = buffer.getCaps();
			if(caps == null){
				return;
			}
			Structure s = caps.getStructure(0);
			//Fraction fps = s.getFraction("framerate");
			//int intFps = (int)fps.toDouble();
			
			Double timediff;
			
			if(hasLastBuf)
			{	
				
				timediff = (double) (buffer.getTimestamp().toNanos() - lastBuf.getTimestamp().toNanos()) ;
			
				Double fps = 1.0/(timediff); 
				fps *= Math.pow(10, 9);
				
				System.out.printf(" Timestamp now: %s, Timestamp last: %s, Diff: %s, FPS: %s \n", buffer.getTimestamp(),lastBuf.getTimestamp() ,timediff, fps);
			
				ClientGUIManager.addTextToFramerateMonitor(Double.toString(Math.round(fps)));
			}
			lastBuf= buffer;
			hasLastBuf = true;
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
