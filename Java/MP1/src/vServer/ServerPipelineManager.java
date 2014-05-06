package vServer;

import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Message;
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

		Element source;
		System.out.println("Building a pipeline for : " + SM.data.mediaType);
		
		if(SM.data.mediaType == ServerData.MediaType.WEBCHAT)
			source 	= ElementFactory.make("v4l2src", "videoCamera");
		else
			source = ElementFactory.make("filesrc", "file-source");

		Element microphone		= ElementFactory.make("alsasrc", "microphone");
		Element avidemux 		= ElementFactory.make("avidemux", "avi-demux");
		Element queue1 			= ElementFactory.make("queue", "avimux-queue");
		Element videoDecodeBin2 = ElementFactory.make("decodebin2", "video-decodebin2");
		Element videorate		= ElementFactory.make("videorate", "video-rate");
		Element videoscale		= ElementFactory.make("videoscale", "video-scale");
		Element autoconvert 	= ElementFactory.make("autoconvert", "video-convert");
		Element encoder 		= ElementFactory.make("ffenc_h263", "h263-encoder");
		Element videopay 		= ElementFactory.make("rtph263pay", "video-pay");
		Element queue2 			= ElementFactory.make("queue", "audio-queue");
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
		Element videoCapsFilter = ElementFactory.make("capsfilter", "videocaps");
		SM.data.fakeSink 		= fakesink;

		if(source == null || microphone == null || avidemux == null || queue1 == null || videoDecodeBin2 == null || autoconvert == null || encoder == null || videopay == null || SM.data.rtpBin == null || 
				audioDecodeBin2 == null || audioconvert == null || speexenc == null || audiopay == null || 
				udpRTPVSink == null || udpRTCPVSrc == null || udpRTCPVSink == null || udpRTPASink == null || udpRTCPASrc == null || udpRTCPASink == null)
		{
			System.err.println("Could not create all elements");
		}


		if(SM.data.activity.equals("Active")){

			SM.data.pipe.addMany(source, queue1, videoDecodeBin2, autoconvert, encoder, videopay, audioDecodeBin2, audioconvert, speexenc, audiopay, SM.data.rtpBin, udpRTPVSink, udpRTCPVSrc, udpRTCPVSink, udpRTPASink, udpRTCPASrc, udpRTCPASink);
			if(SM.data.mediaType == ServerData.MediaType.MOVIE) {

				SM.data.pipe.addMany(avidemux, videorate, videoscale, queue2);

				source.set("location", "sample.avi");

				if(!Element.linkMany(queue1, videoDecodeBin2))
					System.err.println("Could not link queue -> decodebin");
				if(!Element.linkMany(autoconvert, encoder))
					System.err.println("Could not link autoconvert -> encoder");
				if(!Element.linkMany(encoder, videopay))
					System.err.println("Could not link encoder -> videopay");

				videoDecodeBin2.connect(new Element.PAD_ADDED() {
					public void padAdded(Element source, Pad newPad) {
						if(newPad.getName().contains("src"))
						{
							Pad autoSinkPad = SM.data.pipe.getElementByName("video-rate").getStaticPad("sink");
							newPad.link(autoSinkPad);
							System.out.println("Connected decodebin2");
						}
					}
				});
				
				if(!Element.linkMany(source, avidemux))
					System.err.println("Could not link file source to mux");
				int bandwidth = ServerResource.getInstance().getBandwidth();
				int numFrames = bandwidth/(704*576*8);
				if(numFrames > 25)
					numFrames = 25;
				else if(numFrames < 15)
					numFrames = 15;
				
				String rateCapsStr = String.format("video/x-raw-yuv,framerate=15/1", numFrames);
				Caps rateCaps = Caps.fromString(rateCapsStr);
				String scaleCapsStr = "video/x-raw-yuv,width=704,height=576";
				Caps scaleCaps = Caps.fromString(scaleCapsStr);
	
				if(!Element.linkPadsFiltered(videorate, "src", videoscale, "sink", rateCaps))
					System.err.println("Could not connect videorate -> videoscale");
				if(!Element.linkPadsFiltered(videoscale, "src", autoconvert, "sink", scaleCaps))
					System.err.println("Could not connect videoscale -> autoconvert");		
				if(!Element.linkMany(queue2, audioDecodeBin2))
					System.err.println("Could not link audio queue -> audio decodebin2");
				
				avidemux.connect(new Element.PAD_ADDED() {
					public void padAdded(Element source, Pad newPad) {
						if(newPad.getName().contains("video"))
						{
							Pad queueSinkPad = SM.data.pipe.getElementByName("avimux-queue").getStaticPad("sink");
							newPad.link(queueSinkPad);
						}
						else if(newPad.getName().contains("audio"))
						{
							Pad decodebinSinkPad = SM.data.pipe.getElementByName("audio-queue").getStaticPad("sink");
							newPad.link(decodebinSinkPad);
						}
					}
				});
				
			} else {
				
				String videoCapsString = String.format("video/x-raw-yuv,width=320,height=240,framerate=15/1");
				Caps videoCaps = Caps.fromString(videoCapsString);
				videoCapsFilter.setCaps(videoCaps);
				
				if(!Element.linkMany(source,videoCapsFilter,queue1))
					System.err.println("Could not link webcam");
				
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
				if(!Element.linkMany(microphone,audioDecodeBin2));
			}
			
			audioDecodeBin2.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("src"))
					{
						Pad autoSinkPad = SM.data.pipe.getElementByName("audio-convert").getStaticPad("sink");
						newPad.link(autoSinkPad);
						System.out.println("Connected audiodecodebin");
					}
				}
			});

			if(!Element.linkMany(audioDecodeBin2, audioconvert))
				System.err.println("Could not link decodebin -> audio convert");
			if(!Element.linkMany(audioconvert, speexenc))
				System.err.println("Could not link audio convert -> speex enc");
			if(!Element.linkMany(speexenc, audiopay))
				System.err.println("Could not link speex enc -> audio pay");

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

			SM.data.pipe.addMany(source, avidemux, queue1, videoDecodeBin2, videorate, videoscale, autoconvert, encoder, videopay, SM.data.rtpBin, udpRTPVSink, udpRTCPVSrc, udpRTCPVSink);

			if(!Element.linkMany(source, avidemux))
				System.err.println("Could not link queue -> decodebin");
			if(!Element.linkMany(queue1, videoDecodeBin2))
				System.err.println("Could not link queue -> decodebin");
			if(!Element.linkMany(autoconvert, encoder))
				System.err.println("Could not link autoconvert -> encoder");
			if(!Element.linkMany(encoder, videopay))
				System.err.println("Could not link encoder -> videopay");
			
			Caps rateCaps = Caps.fromString("video/x-raw-yuv,framerate=10/1");
			String scaleCapsStr = "video/x-raw-yuv,width=352,height=288";
			Caps scaleCaps = Caps.fromString(scaleCapsStr);

			if(!Element.linkPadsFiltered(videorate, "src", videoscale, "sink", rateCaps))
				System.err.println("Could not connect videorate -> videoscale");
			if(!Element.linkPadsFiltered(videoscale, "src", autoconvert, "sink", scaleCaps))
				System.err.println("Could not connect videoscale -> autoconvert");		
			
			videoDecodeBin2.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("src"))
					{
						Pad autoSinkPad = SM.data.pipe.getElementByName("video-rate").getStaticPad("sink");
						newPad.link(autoSinkPad);
						System.out.println("Connected decodebin2");
					}
				}

			});
			
			source.set("location", "sample.avi");

			udpRTPVSink.set("host", SM.data.clientIP);
			udpRTPVSink.set("port", SM.data.videoRTP);
			udpRTCPVSrc.set("port", SM.data.videoRTCPin);
			udpRTCPVSink.set("host", SM.data.clientIP);
			udpRTCPVSink.set("port", SM.data.videoRTCPout);

			avidemux.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("video"))
					{
						Pad queueSinkPad = SM.data.pipe.getElementByName("avimux-queue").getStaticPad("sink");
						newPad.link(queueSinkPad);
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
		SM.data.pipe.getBus().connect(new Bus.MESSAGE() {
			public void busMessage(Bus bus, Message msg) {
				SM.data.pipeMsgThread = Thread.currentThread();
				SM.data.pipe.getBus().disconnect(new Bus.MESSAGE() {
					public void busMessage(Bus bus, Message msg) {
					}
				});
			}
		});

		/*
		SM.data.pipe.getBus().connect(new Bus.ASYNC_DONE() {
			public void asyncDone(GstObject source) {
				synchronized(SM.data.pipeMsgThread)
				{
					SM.data.pipeMsgThread.notify();
				}
			}
		});
		 */

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
				if(source.equals(SM.data.pipe))
				{
					if(SM.data.notify)
					{
						synchronized(SM.data.pipeMsgThread)
						{
							SM.data.pipeMsgThread.notify();
						}
						SM.data.notify = false;
					}
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
