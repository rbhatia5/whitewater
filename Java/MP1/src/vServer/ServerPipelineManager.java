package vServer;

import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Message;
import org.gstreamer.Pad;
import org.gstreamer.PadLinkReturn;
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
		Element videorate		= ElementFactory.make("videorate", "video-rate");
		Element videoscale		= ElementFactory.make("videoscale", "video-scale");
		Element autoconvert 	= ElementFactory.make("autoconvert", "video-convert");
		Element encoder 		= ElementFactory.make("ffenc_h263", "h263-encoder");
		Element videopay 		= ElementFactory.make("rtph263pay", "video-pay");
		Element audioDecodeBin2 = ElementFactory.make("decodebin2", "audio-decodebin2");
		Element queue2 			= ElementFactory.make("queue", "audio-queue");
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

			SM.data.pipe.addMany(source, avidemux, queue1, videoDecodeBin2, videorate, videoscale, autoconvert, encoder, videopay, queue2, audioDecodeBin2, audioconvert, speexenc, audiopay, SM.data.rtpBin, udpRTPVSink, udpRTCPVSrc, udpRTCPVSink, udpRTPASink, udpRTCPASrc, udpRTCPASink);

			videoDecodeBin2.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("src"))
					{
						Pad autoSinkPad = SM.data.pipe.getElementByName("video-rate").getStaticPad("sink");
						if(!newPad.link(autoSinkPad).equals(PadLinkReturn.OK))
							System.err.println("Could not connect video decodebin2 -> video rate");
					}
				}
			});

			audioDecodeBin2.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("src"))
					{
						Pad autoSinkPad = SM.data.pipe.getElementByName("audio-convert").getStaticPad("sink");
						if(!newPad.link(autoSinkPad).equals(PadLinkReturn.OK))
							System.err.println("Could not link audioDecodeBin -> audioconvert");
					}
				}
			});

			avidemux.connect(new Element.PAD_ADDED() {
				public void padAdded(Element source, Pad newPad) {
					if(newPad.getName().contains("video"))
					{
						Pad queueSinkPad = SM.data.pipe.getElementByName("avimux-queue").getStaticPad("sink");
						if(!newPad.link(queueSinkPad).equals(PadLinkReturn.OK))
							System.err.println("Could not link demux_video -> video queue");
					}
					else if(newPad.getName().contains("audio"))
					{
						Pad queueSinkPad = SM.data.pipe.getElementByName("audio-queue").getStaticPad("sink");
						if(!newPad.link(queueSinkPad).equals(PadLinkReturn.OK))
							System.err.println("Could not link demux_audio -> audio queue");
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

			int bandwidth = ServerResource.getInstance().getBandwidth();
			int numFrames = bandwidth/(704*576*8);
			if(numFrames > 25)
				numFrames = 25;
			else if(numFrames < 15)
				numFrames = 15;
			
			String rateCapsStr = String.format("video/x-raw-yuv,framerate=%s/1", numFrames);
			Caps rateCaps = Caps.fromString(rateCapsStr);
			String scaleCapsStr = "video/x-raw-yuv,width=704,height=576";
			Caps scaleCaps = Caps.fromString(scaleCapsStr);
			
			if(!Element.linkMany(source, avidemux))
				System.err.println("Could not link file source to mux");
			if(!Element.linkMany(queue1, videoDecodeBin2))
				System.err.println("Could not link queue -> decodebin");
			if(!Element.linkMany(autoconvert, encoder))
				System.err.println("Could not link autoconvert -> encoder");
			if(!Element.linkMany(encoder, videopay))
				System.err.println("Could not link encoder -> videopay");
			
			if(!Element.linkMany(queue2, audioDecodeBin2))
				System.err.println("Could not link decodebin -> audio convert");
			if(!Element.linkMany(audioconvert, speexenc))
				System.err.println("Could not link audio convert -> speex enc");
			if(!Element.linkMany(speexenc, audiopay))
				System.err.println("Could not link speex enc -> audio pay");
			
			if(!Element.linkPadsFiltered(videorate, "src", videoscale, "sink", rateCaps))
				System.err.println("Could not connect videorate -> videoscale");
			if(!Element.linkPadsFiltered(videoscale, "src", autoconvert, "sink", scaleCaps))
				System.err.println("Could not connect videoscale -> autoconvert");

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

			Pad send_rtp_sink_0 = SM.data.rtpBin.getRequestPad("send_rtp_sink_0");
			Pad paySrcPad = videopay.getStaticPad("src");
			if(send_rtp_sink_0 == null || paySrcPad == null)
				System.err.println("Could not create rtpbin.send_rtp_sink_0 or pay.src pad");
			if(!paySrcPad.link(send_rtp_sink_0).equals(PadLinkReturn.OK))
				System.err.println("Could not link video pay -> send_rtp_sink_0");

			Pad send_rtcp_src_0 = SM.data.rtpBin.getRequestPad("send_rtcp_src_0");
			Pad udpSinkPadRTCP = udpRTCPVSink.getStaticPad("sink");
			if(send_rtcp_src_0 == null || udpSinkPadRTCP == null)
				System.err.println("Could not create rtpbin.send_rtcp_src_0 or udp.src pad");
			if(!send_rtcp_src_0.link(udpSinkPadRTCP).equals(PadLinkReturn.OK))
				System.err.println("Could not link sent_rtp_sink_0 -> udp RTCP vid sink");
				
			Pad recv_rtcp_sink_0 = SM.data.rtpBin.getRequestPad("recv_rtcp_sink_0");
			Pad udpSrcPadRTCP = udpRTCPVSrc.getStaticPad("src");
			if(!udpSrcPadRTCP.link(recv_rtcp_sink_0).equals(PadLinkReturn.OK))
				System.err.println("Could not link udp RTCP vid src -> recv_rtcp_sink_0");

			Pad send_rtp_sink_1 = SM.data.rtpBin.getRequestPad("send_rtp_sink_1");
			Pad audioPaySrcPad = audiopay.getStaticPad("src");
			if(send_rtp_sink_1 == null || audioPaySrcPad == null)
				System.err.println("Could not create rtpbin.send_rtp_sink_1 or pay.src pad");
			if(!audioPaySrcPad.link(send_rtp_sink_1).equals(PadLinkReturn.OK))
				System.err.println("Could not link audio pay -> send_rtp_sink_1");

			Pad send_rtcp_src_1 = SM.data.rtpBin.getRequestPad("send_rtcp_src_1");
			Pad udpAudioSinkPadRTCP = udpRTCPASink.getStaticPad("sink");
			if(send_rtcp_src_1 == null || udpAudioSinkPadRTCP == null)
				System.err.println("Could not create rtpbin.send_rtcp_src_1 or udp.src pad");
			if(!send_rtcp_src_1.link(udpAudioSinkPadRTCP).equals(PadLinkReturn.OK))
				System.err.println("Could not link sent_rtp_sink_1 -> udp RTCP aud sink");

			Pad recv_rtcp_sink_1 = SM.data.rtpBin.getRequestPad("recv_rtcp_sink_1");
			Pad udpAudioSrcPadRTCP = udpRTCPASrc.getStaticPad("src");
			if(!udpAudioSrcPadRTCP.link(recv_rtcp_sink_1).equals(PadLinkReturn.OK))
				System.err.println("Could not link sent_rtp_sink_1 -> udp RTCP aud src");

		}else if(SM.data.activity.equals("Passive")){

			SM.data.pipe.addMany(source, avidemux, queue1, videoDecodeBin2, videorate, videoscale, autoconvert, encoder, videopay,SM.data.rtpBin, udpRTPVSink, udpRTCPVSrc, udpRTCPVSink, fakesink);

			if(!Element.linkMany(source, avidemux))
				System.err.println("Could not link file source to mux");
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
					}
				}
			});

			String rateCapsStr = "video/x-raw-yuv,framerate=10/1";
			Caps rateCaps = Caps.fromString(rateCapsStr);
			if(!Element.linkPadsFiltered(videorate, "src", videoscale, "sink", rateCaps))
				System.err.println("Could not linke video rate -> video scale");
			
			String scaleCapsStr = "video/x-raw-yuv,width=352,height=288";
			Caps scaleCaps = Caps.fromString(scaleCapsStr);
			if(!Element.linkPadsFiltered(videoscale, "src", autoconvert, "sink", scaleCaps))
				System.err.println("Could not linke video scale -> video convert");
			
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
	}
}
