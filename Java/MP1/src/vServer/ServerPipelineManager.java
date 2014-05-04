package vServer;

import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.elements.good.RTPBin;

public class ServerPipelineManager {

	protected vServerManager SM;
	
	ServerPipelineManager(vServerManager sm)
	{
		SM = sm;
	}
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected void modify_pipeline()
	{
		switch(SM.data.mode)
		{
		case SERVER:
			System.out.println("Initializing Server");
			discard_pipeline();
			server_pipeline();
			connect_to_signals();
			SM.data.pipe.setState(State.READY);
			//vServerManager.data.pipe.setState(State.PAUSED);
			break;
		default:
			System.out.println("Unrecognized pipeline");
			SM.data.pipe.setState(State.READY);
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
	protected void discard_pipeline()
	{
		if(SM.data.pipe != null)
		{
			SM.data.pipe.setState(State.READY);
			//vServerManager.data.pipe.remove(vServerManager.data.RTCPSink);
			//vServerManager.data.RTCPSink = null;
			SM.data.pipe.setState(State.NULL);
		}
	}

	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected void server_pipeline()
	{

		SM.data.pipe = new Pipeline("server-pipeline");

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
		SM.data.rtpBin 			= (RTPBin)ElementFactory.make("gstrtpbin", "rtp-bin"); 
		Element udpRTPVSink 	= ElementFactory.make("udpsink", "udp-rtp-video-sink");
		Element udpRTCPVSrc		= ElementFactory.make("udpsrc", "udp-rtcp-video-src");
		Element udpRTCPVSink 	= ElementFactory.make("udpsink", "udp-rtcp-video-sink");
		Element udpRTPASink 	= ElementFactory.make("udpsink", "udp-rtp-audio-sink");
		Element udpRTCPASrc 	= ElementFactory.make("udpsrc", "udp-rtcp-audio-src");
		Element udpRTCPASink 	= ElementFactory.make("udpsink", "udp-rtcp-audio-sink");
		Element fakesink 		= ElementFactory.make("fakesink", "fake");
		SM.data.fakeSink 		= fakesink;
		
		//Error check
		if(source == null || avidemux == null || queue1 == null || videoDecodeBin2 == null || autoconvert == null || encoder == null || videopay == null || SM.data.rtpBin == null || 
				audioDecodeBin2 == null || audioconvert == null || speexenc == null || audiopay == null || 
				udpRTPVSink == null || udpRTCPVSrc == null || udpRTCPVSink == null || udpRTPASink == null || udpRTCPASrc == null || udpRTCPASink == null)
		{
			System.err.println("Could not create all elements");
		}


		if(SM.data.activity.equals("Active")){

			SM.data.pipe.addMany(source, avidemux, queue1, videoDecodeBin2, autoconvert, encoder, videopay, audioDecodeBin2, audioconvert, speexenc, audiopay, SM.data.rtpBin, udpRTPVSink, udpRTCPVSrc, udpRTCPVSink, udpRTPASink, udpRTCPASrc, udpRTCPASink);

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
						Pad autoSinkPad = SM.data.pipe.getElementByName("video-convert").getStaticPad("sink");
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
						Pad autoSinkPad = SM.data.pipe.getElementByName("audio-convert").getStaticPad("sink");
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

			udpRTPVSink.set("host", SM.data.clientIP);
			udpRTPVSink.set("port", SM.data.videoRTP);
			udpRTCPVSrc.set("port", SM.data.videoRTCPin);
			udpRTCPVSink.set("host", SM.data.clientIP);
			udpRTCPVSink.set("port", SM.data.videoRTCPout);

			udpRTPASink.set("host", SM.data.clientIP);
			udpRTPASink.set("port", SM.data.audioRTP);
			udpRTCPASrc.set("port", SM.data.audioRTCPin);
			udpRTCPASink.set("host", SM.data.clientIP);
			udpRTCPASink.set("port", SM.data.audioRTCPout);

			//Link sometimes pads manually
			avidemux.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("video"))
					{
						Pad queueSinkPad = SM.data.pipe.getElementByName("avimux-queue").getStaticPad("sink");
						newPad.link(queueSinkPad);
					}
					else if(newPad.getName().contains("audio"))
					{
						Pad decodebinSinkPad = SM.data.pipe.getElementByName("audio-decodebin2").getStaticPad("sink");
						newPad.link(decodebinSinkPad);
					}
				}
			});

			SM.data.rtpBin.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("send_rtp_src_0"))
					{
						Pad udpSinkPad = SM.data.pipe.getElementByName("udp-rtp-video-sink").getStaticPad("sink");
						newPad.link(udpSinkPad);
					}
					else if(newPad.getName().contains("send_rtp_src_1"))
					{
						Pad udpSinkPad = SM.data.pipe.getElementByName("udp-rtp-audio-sink").getStaticPad("sink");
						newPad.link(udpSinkPad);
					}
				}
			});

			//Link request pads manually
			Pad send_rtp_sink_0 = SM.data.rtpBin.getRequestPad("send_rtp_sink_0");
			Pad paySrcPad = videopay.getStaticPad("src");
			if(send_rtp_sink_0 == null || paySrcPad == null)
				System.err.println("Could not create rtpbin.send_rtp_sink_0 or pay.src pad");
			paySrcPad.link(send_rtp_sink_0);

			Pad send_rtcp_src_0 = SM.data.rtpBin.getRequestPad("send_rtcp_src_0");
			Pad udpSinkPadRTCP = udpRTCPVSink.getStaticPad("sink");
			if(send_rtcp_src_0 == null || udpSinkPadRTCP == null)
				System.err.println("Could not create rtpbin.send_rtcp_src_0 or udp.src pad");
			send_rtcp_src_0.link(udpSinkPadRTCP);

			Pad recv_rtcp_sink_0 = SM.data.rtpBin.getRequestPad("recv_rtcp_sink_0");
			Pad udpSrcPadRTCP = udpRTCPVSrc.getStaticPad("src");
			udpSrcPadRTCP.link(recv_rtcp_sink_0);

			Pad send_rtp_sink_1 = SM.data.rtpBin.getRequestPad("send_rtp_sink_1");
			Pad audioPaySrcPad = audiopay.getStaticPad("src");
			if(send_rtp_sink_1 == null || audioPaySrcPad == null)
				System.err.println("Could not create rtpbin.send_rtp_sink_1 or pay.src pad");
			audioPaySrcPad.link(send_rtp_sink_1);

			Pad send_rtcp_src_1 = SM.data.rtpBin.getRequestPad("send_rtcp_src_1");
			Pad udpAudioSinkPadRTCP = udpRTCPASink.getStaticPad("sink");
			if(send_rtcp_src_1 == null || udpAudioSinkPadRTCP == null)
				System.err.println("Could not create rtpbin.send_rtcp_src_1 or udp.src pad");
			send_rtcp_src_1.link(udpAudioSinkPadRTCP);

			Pad recv_rtcp_sink_1 = SM.data.rtpBin.getRequestPad("recv_rtcp_sink_1");
			Pad udpAudioSrcPadRTCP = udpRTCPASrc.getStaticPad("src");
			udpAudioSrcPadRTCP.link(recv_rtcp_sink_1);

		}else if(SM.data.activity.equals("Passive")){

			SM.data.pipe.addMany(source, avidemux, queue1, videoDecodeBin2, autoconvert, encoder, videopay,SM.data.rtpBin, udpRTPVSink, udpRTCPVSrc, udpRTCPVSink, fakesink);

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
						Pad autoSinkPad = SM.data.pipe.getElementByName("video-convert").getStaticPad("sink");
						newPad.link(autoSinkPad);
						System.out.println("Connected decodebin2");
					}
				}

			});



			String rateCapsStr = String.format("video/x-raw-yuv,framerate=%s/1", SM.data.framerate);
			System.out.println(rateCapsStr);
			Caps rateCaps = Caps.fromString(rateCapsStr);

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

			udpRTPVSink.set("host", SM.data.clientIP);
			udpRTPVSink.set("port", SM.data.videoRTP);
			udpRTCPVSrc.set("port", SM.data.videoRTCPin);
			udpRTCPVSink.set("host", SM.data.clientIP);
			udpRTCPVSink.set("port", SM.data.videoRTCPout);


			//Link sometimes pads manually
			avidemux.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("video"))
					{
						Pad queueSinkPad = SM.data.pipe.getElementByName("avimux-queue").getStaticPad("sink");
						newPad.link(queueSinkPad);
					}else if(newPad.getName().contains("audio")){
						Pad fakeSinkPad = SM.data.pipe.getElementByName("fake").getStaticPad("sink");
						newPad.link(fakeSinkPad);
						
						System.out.println("connected audio... Not!!!");
					}
				}
			});

			SM.data.rtpBin.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("send_rtp_src_0"))
					{
						Pad udpSinkPad = SM.data.pipe.getElementByName("udp-rtp-video-sink").getStaticPad("sink");
						newPad.link(udpSinkPad);
					}
				}
			});

			//Link request pads manually
			Pad send_rtp_sink_0 = SM.data.rtpBin.getRequestPad("send_rtp_sink_0");
			Pad paySrcPad = videopay.getStaticPad("src");
			if(send_rtp_sink_0 == null || paySrcPad == null)
				System.err.println("Could not create rtpbin.send_rtp_sink_0 or pay.src pad");
			paySrcPad.link(send_rtp_sink_0);

			Pad send_rtcp_src_0 = SM.data.rtpBin.getRequestPad("send_rtcp_src_0");
			Pad udpSinkPadRTCP = udpRTCPVSink.getStaticPad("sink");
			if(send_rtcp_src_0 == null || udpSinkPadRTCP == null)
				System.err.println("Could not create rtpbin.send_rtcp_src_0 or udp.src pad");
			send_rtcp_src_0.link(udpSinkPadRTCP);

			Pad recv_rtcp_sink_0 = SM.data.rtpBin.getRequestPad("recv_rtcp_sink_0");
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
	protected void connect_to_signals()
	{
		SM.data.pipe.getBus().connect(new Bus.ASYNC_DONE() {
			public void asyncDone(GstObject source) {
				System.err.println("This thread " + Thread.currentThread().getId());
				synchronized(SM.data.pipeMsgThread)
				{
					SM.data.pipeMsgThread.notify();
				}
				//System.err.println("Main thread" + SM.data.mainThread.getId());
				//System.err.println("Server thread" + SM.data.serverThread.getId());
			}
		});
		
		//connect to signal EOS
		SM.data.pipe.getBus().connect(new Bus.EOS() {
			public void endOfStream(GstObject source) {
				System.out.printf("[%s} reached EOS.\n", source);
				Gst.quit();
			}
		});

		//connect to signal ERROR
		SM.data.pipe.getBus().connect(new Bus.ERROR() {
			public void errorMessage(GstObject source, int code, String message) {
				System.out.printf("[%s] encountered error code %d: %s\n",source, code, message);
				Gst.quit();
			}
		});

		//connect to change of state
		SM.data.pipe.getBus().connect(new Bus.STATE_CHANGED() {
			public void stateChanged(GstObject source, State oldstate, State newstate, State pending) {
				SM.data.pipeMsgThread = Thread.currentThread();
				if(source.equals(SM.data.pipe))
				{
					System.out.printf("[%s] changed state from %s to %s\n", source.getName(), oldstate.toString(), newstate.toString());
				}
			}
		});

		SM.data.rtpBin.connect(new RTPBin.ON_NEW_SSRC() {
			public void onNewSsrc(RTPBin rtpBin, int sessionid, int ssrc) {
				//System.out.printf("1 : RTCP packet received from ssrc: %s session: %s\n", ssrc, sessionid);
			}
		});

		SM.data.rtpBin.connect(new RTPBin.ON_SSRC_SDES() {
			public void onSsrcSdes(RTPBin rtpBin, int sessionid, int ssrc) {
				//System.out.printf("2 : RTCP packet received from ssrc: %s session: %s\n", ssrc, sessionid);
			}
		});

		SM.data.rtpBin.connect(new RTPBin.ON_SSRC_ACTIVE() {
			public void onSsrcActive(RTPBin rtpBin, int sessionid, int ssrc) {
				//System.out.printf("3 : RTCP packet received from ssrc: %s session: %s\n", ssrc, sessionid);
				//Element rtpSession = vServerManager.data.rtpBin.getElementByName("rtpsession0");
			}
		});
	}
}
