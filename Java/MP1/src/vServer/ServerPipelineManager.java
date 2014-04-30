package vServer;

import org.gstreamer.Bus;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.elements.good.RTPBin;

public class ServerPipelineManager {

	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void modify_pipeline()
	{
		switch(vServerManager.data.mode)
		{
		case SERVER:
			System.out.println("Initializing Server");
			discard_pipeline();
			server_pipeline();
			connect_to_signals();
			vServerManager.data.pipe.setState(State.READY);
			//vServerManager.data.pipe.setState(State.PAUSED);
			break;
		default:
			System.out.println("Unrecognized pipeline");
			vServerManager.data.pipe.setState(State.READY);
			//vServerManager.data.pipe.setState(State.PAUSED);
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
		if(vServerManager.data.pipe != null)
		{
			vServerManager.data.pipe.setState(State.READY);
			//vServerManager.data.pipe.remove(vServerManager.data.RTCPSink);
			//vServerManager.data.RTCPSink = null;
			vServerManager.data.pipe.setState(State.NULL);
		}
	}

	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void server_pipeline()
	{

		vServerManager.data.pipe = new Pipeline("server-pipeline");

		/*
		___________	   ______________      _________     ______________    _______________    ______________    ______________    _____________    ____________________    __________________    ________________
		| filesrc |	-> | avimux-video | -> | queue | ->  | decodebin2 | -> | autoconvert | -> | ffenc_h263 | -> | rtph263pay | -> | gstrtpbin | -> | .send_rtp_sink_0 | -> |.send_rtp_src_0 | -> | udpsink 5002 | 
		___________    ______________      _________     ______________    _______________    ______________    ______________    _____________    ____________________    __________________    ________________
							|
							|																											|			____________________    ________________
							|																											|___\		| .send_rtcp_src_0 | -> | udpsink 5003 |
							|																											|	/		____________________    ________________
							|																											|			_______________    _____________________
							|																											|___\		| udpsrc 5004 | -> | .recv_rtcp_sink_0 |
							|																												/		_______________    _____________________
							|
							|
							|              ______________    ________________    ____________    ______________    _____________    ____________________    __________________    ________________
							|______\       | decodebin2 | -> | audioconvert | -> | speexenc | -> |rtpspeexpay | -> | gstrtpbin | -> | .send_rtp_sink_1 | -> |.send_rtp_src_1 | -> | udpsink 5005 |
								   /       ______________    ________________    ____________    ______________    _____________    ____________________    __________________    ________________
																														|
																														|			____________________    ________________
																														|___\		| .send_rtcp_src_1 | -> | udpsink 5006 |
																														|	/		____________________    ________________
																														|			_______________    _____________________
																														|___\		| udpsrc 5007 | -> | .recv_rtcp_sink_1 |
																															/		_______________    _____________________
		 */

		//Initialize elements
		Element source 			= ElementFactory.make("filesrc", "file-source");
		Element avidemux 		= ElementFactory.make("avidemux", "avi-demux");
		Element queue1 			= ElementFactory.make("queue", "avimux-queue");
		Element videoDecodeBin2 = ElementFactory.make("decodebin2", "video-decodebin2");
		Element autoconvert 	= ElementFactory.make("autoconvert", "video-convert");
		Element encoder 		= ElementFactory.make("ffenc_h263", "h263-encoder");
		Element videopay 		= ElementFactory.make("rtph263pay", "video-pay");
		Element audioDecodeBin2 = ElementFactory.make("decodebin2", "audio-decodebin2");
		Element audioconvert 	= ElementFactory.make("audioconvert", "audio-convert");
		Element speexenc 		= ElementFactory.make("speexenc", "speex-enc");
		Element audiopay 		= ElementFactory.make("rtpspeexpay", "audio-pay");
		vServerManager.data.rtpBin 		= (RTPBin)ElementFactory.make("gstrtpbin", "rtp-bin"); 
		Element udpRTPVSink 	= ElementFactory.make("udpsink", "udp-rtp-video-sink");
		Element udpRTCPVSrc		= ElementFactory.make("udpsrc", "udp-rtcp-video-src");
		Element udpRTCPVSink 	= ElementFactory.make("udpsink", "udp-rtcp-video-sink");
		Element udpRTPASink 	= ElementFactory.make("udpsink", "udp-rtp-audio-sink");
		Element udpRTCPASrc 	= ElementFactory.make("udpsrc", "udp-rtcp-audio-src");
		Element udpRTCPASink 	= ElementFactory.make("udpsink", "udp-rtcp-audio-sink");
		Element fakesink 		= ElementFactory.make("fakesink", "fake");

		//Error check
		if(source == null || avidemux == null || queue1 == null || videoDecodeBin2 == null || autoconvert == null || encoder == null || videopay == null || vServerManager.data.rtpBin == null || 
				audioDecodeBin2 == null || audioconvert == null || speexenc == null || audiopay == null || 
				udpRTPVSink == null || udpRTCPVSrc == null || udpRTCPVSink == null || udpRTPASink == null || udpRTCPASrc == null || udpRTCPASink == null)
		{
			System.err.println("Could not create all elements");
		}


		if(vServerManager.data.activity.equals("Active")){

			vServerManager.data.pipe.addMany(source, avidemux, queue1, videoDecodeBin2, autoconvert, encoder, videopay, audioDecodeBin2, audioconvert, speexenc, audiopay, vServerManager.data.rtpBin, udpRTPVSink, udpRTCPVSrc, udpRTCPVSink, udpRTPASink, udpRTCPASrc, udpRTCPASink);

			if(!Element.linkMany(source, avidemux))
				System.err.println("Could not link file source to mux");
			//if(!Element.linkMany(queue1, videoDecodeBin2, autoconvert, encoder, videopay))

			if(!Element.linkMany(queue1, videoDecodeBin2))
				System.err.println("Could not link queue -> decodebin");
			if(!Element.linkMany(videoDecodeBin2, autoconvert))
				System.err.println("Could not link videoDecodeBin2 -> autoconvert");
			if(!Element.linkMany(autoconvert, encoder))
				System.err.println("Could not link autoconvert -> encoder");
			if(!Element.linkMany(encoder, videopay))
				System.err.println("Could not link encoder -> videopay");

			videoDecodeBin2.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("src"))
					{
						Pad autoSinkPad = vServerManager.data.pipe.getElementByName("video-convert").getStaticPad("sink");
						newPad.link(autoSinkPad);
						System.out.println("Connected decodebin2");
					}
					else
					{
						System.err.println("Could not link videoDecodeBin2 -> autoconvert");
					}
				}

			});

			audioDecodeBin2.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("src"))
					{
						Pad autoSinkPad = vServerManager.data.pipe.getElementByName("audio-convert").getStaticPad("sink");
						newPad.link(autoSinkPad);
						System.out.println("Connected audiodecodebin");
					}
					else
					{
						System.err.println("Could not link audioDecodeBin -> audioconvert");
					}
				}

			});


			//System.err.println("Could not link queue -> decodebin -> auto convert -> encoder -> video pay");
			if(!Element.linkMany(audioDecodeBin2, audioconvert))
				System.err.println("Could not link decodebin -> audio convert");
			if(!Element.linkMany(audioconvert, speexenc))
				System.err.println("Could not link audio convert -> speex enc");
			if(!Element.linkMany(speexenc, audiopay))
				System.err.println("Could not link speex enc -> audio pay");


			//String rateCapsStr = String.format("video/x-raw-yuv,framerate=%s/1", vServerManager.data.framerate);
			//System.out.println(rateCapsStr);
			//Caps rateCaps = Caps.fromString(rateCapsStr);

			//String scaleCapsStr = String.format("video/x-raw-yuv,width=%s,height=%s", vServerManager.data.width, vServerManager.data.height);
			//System.out.println(scaleCapsStr);
			//Caps scaleCaps = Caps.fromString(scaleCapsStr);

			//Link link-able elements
			//Element.linkMany(source, videorate);
			//if(!Element.linkPadsFiltered(videorate, "src", videoscale, "sink", rateCaps))
			//	System.err.println("Could not connect videotestsrc -> videorate");
			//if(!Element.linkPadsFiltered(videoscale, "src", encoder, "sink", scaleCaps))
			//	System.err.println("Could not connect videorate -> videoscale");
			//Element.linkMany(encoder, pay);
			//Element.linkMany(source, encoder, pay);

			source.set("location", "sample.avi");

			udpRTPVSink.set("host", vServerManager.data.clientIP);
			udpRTPVSink.set("port", vServerManager.data.videoRTP);
			udpRTCPVSrc.set("port", vServerManager.data.videoRTCPin);
			udpRTCPVSink.set("host", vServerManager.data.clientIP);
			udpRTCPVSink.set("port", vServerManager.data.videoRTCPout);

			udpRTPASink.set("host", vServerManager.data.clientIP);
			udpRTPASink.set("port", vServerManager.data.audioRTP);
			udpRTCPASrc.set("port", vServerManager.data.audioRTCPin);
			udpRTCPASink.set("host", vServerManager.data.clientIP);
			udpRTCPASink.set("port", vServerManager.data.audioRTCPout);

			//Link sometimes pads manually
			avidemux.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("video"))
					{
						Pad queueSinkPad = vServerManager.data.pipe.getElementByName("avimux-queue").getStaticPad("sink");
						newPad.link(queueSinkPad);
					}
					else if(newPad.getName().contains("audio"))
					{
						Pad decodebinSinkPad = vServerManager.data.pipe.getElementByName("audio-decodebin2").getStaticPad("sink");
						newPad.link(decodebinSinkPad);
					}
				}
			});

			vServerManager.data.rtpBin.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("send_rtp_src_0"))
					{
						Pad udpSinkPad = vServerManager.data.pipe.getElementByName("udp-rtp-video-sink").getStaticPad("sink");
						newPad.link(udpSinkPad);
					}
					else if(newPad.getName().contains("send_rtp_src_1"))
					{
						Pad udpSinkPad = vServerManager.data.pipe.getElementByName("udp-rtp-audio-sink").getStaticPad("sink");
						newPad.link(udpSinkPad);
					}
				}
			});

			//Link request pads manually
			Pad send_rtp_sink_0 = vServerManager.data.rtpBin.getRequestPad("send_rtp_sink_0");
			Pad paySrcPad = videopay.getStaticPad("src");
			if(send_rtp_sink_0 == null || paySrcPad == null)
				System.err.println("Could not create rtpbin.send_rtp_sink_0 or pay.src pad");
			paySrcPad.link(send_rtp_sink_0);

			Pad send_rtcp_src_0 = vServerManager.data.rtpBin.getRequestPad("send_rtcp_src_0");
			Pad udpSinkPadRTCP = udpRTCPVSink.getStaticPad("sink");
			if(send_rtcp_src_0 == null || udpSinkPadRTCP == null)
				System.err.println("Could not create rtpbin.send_rtcp_src_0 or udp.src pad");
			send_rtcp_src_0.link(udpSinkPadRTCP);

			Pad recv_rtcp_sink_0 = vServerManager.data.rtpBin.getRequestPad("recv_rtcp_sink_0");
			Pad udpSrcPadRTCP = udpRTCPVSrc.getStaticPad("src");
			udpSrcPadRTCP.link(recv_rtcp_sink_0);

			Pad send_rtp_sink_1 = vServerManager.data.rtpBin.getRequestPad("send_rtp_sink_1");
			Pad audioPaySrcPad = audiopay.getStaticPad("src");
			if(send_rtp_sink_1 == null || audioPaySrcPad == null)
				System.err.println("Could not create rtpbin.send_rtp_sink_1 or pay.src pad");
			audioPaySrcPad.link(send_rtp_sink_1);

			Pad send_rtcp_src_1 = vServerManager.data.rtpBin.getRequestPad("send_rtcp_src_1");
			Pad udpAudioSinkPadRTCP = udpRTCPASink.getStaticPad("sink");
			if(send_rtcp_src_1 == null || udpAudioSinkPadRTCP == null)
				System.err.println("Could not create rtpbin.send_rtcp_src_1 or udp.src pad");
			send_rtcp_src_1.link(udpAudioSinkPadRTCP);

			Pad recv_rtcp_sink_1 = vServerManager.data.rtpBin.getRequestPad("recv_rtcp_sink_1");
			Pad udpAudioSrcPadRTCP = udpRTCPASrc.getStaticPad("src");
			udpAudioSrcPadRTCP.link(recv_rtcp_sink_1);

		}else if(vServerManager.data.activity.equals("Passive")){

			vServerManager.data.pipe.addMany(source, avidemux, queue1, videoDecodeBin2, autoconvert, encoder, videopay,vServerManager.data.rtpBin, udpRTPVSink, udpRTCPVSrc, udpRTCPVSink, fakesink);

			if(!Element.linkMany(source, avidemux))
				System.err.println("Could not link file source to mux");
			//if(!Element.linkMany(queue1, videoDecodeBin2, autoconvert, encoder, videopay))

			if(!Element.linkMany(queue1, videoDecodeBin2))
				System.err.println("Could not link queue -> decodebin");
			if(!Element.linkMany(videoDecodeBin2, autoconvert))
				System.err.println("Could not link videoDecodeBin2 -> autoconvert");
			if(!Element.linkMany(autoconvert, encoder))
				System.err.println("Could not link autoconvert -> encoder");
			if(!Element.linkMany(encoder, videopay))
				System.err.println("Could not link encoder -> videopay");

			videoDecodeBin2.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("src"))
					{
						Pad autoSinkPad = vServerManager.data.pipe.getElementByName("video-convert").getStaticPad("sink");
						newPad.link(autoSinkPad);
						System.out.println("Connected decodebin2");
					}
				}

			});



			//String rateCapsStr = String.format("video/x-raw-yuv,framerate=%s/1", vServerManager.data.framerate);
			//System.out.println(rateCapsStr);
			//Caps rateCaps = Caps.fromString(rateCapsStr);

			//String scaleCapsStr = String.format("video/x-raw-yuv,width=%s,height=%s", vServerManager.data.width, vServerManager.data.height);
			//System.out.println(scaleCapsStr);
			//Caps scaleCaps = Caps.fromString(scaleCapsStr);

			//Link link-able elements
			//Element.linkMany(source, videorate);
			//if(!Element.linkPadsFiltered(videorate, "src", videoscale, "sink", rateCaps))
			//	System.err.println("Could not connect videotestsrc -> videorate");
			//if(!Element.linkPadsFiltered(videoscale, "src", encoder, "sink", scaleCaps))
			//	System.err.println("Could not connect videorate -> videoscale");
			//Element.linkMany(encoder, pay);
			//Element.linkMany(source, encoder, pay);

			source.set("location", "sample.avi");

			udpRTPVSink.set("host", vServerManager.data.clientIP);
			udpRTPVSink.set("port", vServerManager.data.videoRTP);
			udpRTCPVSrc.set("port", vServerManager.data.videoRTCPin);
			udpRTCPVSink.set("host", vServerManager.data.clientIP);
			udpRTCPVSink.set("port", vServerManager.data.videoRTCPout);


			//Link sometimes pads manually
			avidemux.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("video"))
					{
						Pad queueSinkPad = vServerManager.data.pipe.getElementByName("avimux-queue").getStaticPad("sink");
						newPad.link(queueSinkPad);
					}else if(newPad.getName().contains("audio")){
						Pad fakeSinkPad = vServerManager.data.pipe.getElementByName("fake").getStaticPad("sink");
						newPad.link(fakeSinkPad);
						
						System.out.println("connected audio... Not!!!");
					}
				}
			});

			vServerManager.data.rtpBin.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("send_rtp_src_0"))
					{
						Pad udpSinkPad = vServerManager.data.pipe.getElementByName("udp-rtp-video-sink").getStaticPad("sink");
						newPad.link(udpSinkPad);
					}
				}
			});

			//Link request pads manually
			Pad send_rtp_sink_0 = vServerManager.data.rtpBin.getRequestPad("send_rtp_sink_0");
			Pad paySrcPad = videopay.getStaticPad("src");
			if(send_rtp_sink_0 == null || paySrcPad == null)
				System.err.println("Could not create rtpbin.send_rtp_sink_0 or pay.src pad");
			paySrcPad.link(send_rtp_sink_0);

			Pad send_rtcp_src_0 = vServerManager.data.rtpBin.getRequestPad("send_rtcp_src_0");
			Pad udpSinkPadRTCP = udpRTCPVSink.getStaticPad("sink");
			if(send_rtcp_src_0 == null || udpSinkPadRTCP == null)
				System.err.println("Could not create rtpbin.send_rtcp_src_0 or udp.src pad");
			send_rtcp_src_0.link(udpSinkPadRTCP);

			Pad recv_rtcp_sink_0 = vServerManager.data.rtpBin.getRequestPad("recv_rtcp_sink_0");
			Pad udpSrcPadRTCP = udpRTCPVSrc.getStaticPad("src");
			udpSrcPadRTCP.link(recv_rtcp_sink_0);


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
		//connect to signal EOS
		vServerManager.data.pipe.getBus().connect(new Bus.EOS() {
			public void endOfStream(GstObject source) {
				System.out.printf("[%s} reached EOS.\n", source);
				Gst.quit();
			}
		});

		//connect to signal ERROR
		vServerManager.data.pipe.getBus().connect(new Bus.ERROR() {
			public void errorMessage(GstObject source, int code, String message) {
				System.out.printf("[%s] encountered error code %d: %s\n",source, code, message);
				Gst.quit();
			}
		});

		//connect to change of state
		vServerManager.data.pipe.getBus().connect(new Bus.STATE_CHANGED() {
			public void stateChanged(GstObject source, State oldstate, State newstate, State pending) {
				if(source.equals(vServerManager.data.pipe))
				{
					System.out.printf("[%s] changed state from %s to %s\n", source.getName(), oldstate.toString(), newstate.toString());
				}
			}
		});

		vServerManager.data.rtpBin.connect(new RTPBin.ON_NEW_SSRC() {
			public void onNewSsrc(RTPBin rtpBin, int sessionid, int ssrc) {
				//System.out.printf("1 : RTCP packet received from ssrc: %s session: %s\n", ssrc, sessionid);
			}
		});

		vServerManager.data.rtpBin.connect(new RTPBin.ON_SSRC_SDES() {
			public void onSsrcSdes(RTPBin rtpBin, int sessionid, int ssrc) {
				//System.out.printf("2 : RTCP packet received from ssrc: %s session: %s\n", ssrc, sessionid);
			}
		});

		vServerManager.data.rtpBin.connect(new RTPBin.ON_SSRC_ACTIVE() {
			public void onSsrcActive(RTPBin rtpBin, int sessionid, int ssrc) {
				//System.out.printf("3 : RTCP packet received from ssrc: %s session: %s\n", ssrc, sessionid);
				//Element rtpSession = vServerManager.data.rtpBin.getElementByName("rtpsession0");
			}
		});
	}
}
