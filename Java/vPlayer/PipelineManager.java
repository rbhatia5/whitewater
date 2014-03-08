package vPlayer;

import org.gstreamer.*;
import org.gstreamer.event.SeekEvent;

public class PipelineManager{

	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void modify_pipeline()
	{
		switch(PlayerData.mode)
		{
		case PLAYER:
			System.out.println("Initializing player");
			discard_pipeline();
			player_pipeline();
			connect_to_signals();
			PlayerData.pipe.setState(State.READY);
			break;
		case VIDEO_RECORDER:
			System.out.println("Initializing video recorder");
			discard_pipeline();
			videorecorder_pipeline();
			connect_to_signals();
			PlayerData.pipe.setState(State.READY);
			break;
		case AUDIO_RECORDER:
			System.out.println("Initializing audio recorder");
			discard_pipeline();
			audiorecorder_pipeline();
			connect_to_signals();
			PlayerData.pipe.setState(State.READY);
			break;
		default:
			System.out.println("Unrecognized pipeline");
			PlayerData.pipe.setState(State.READY);
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
		if(PlayerData.pipe != null)
		{
			//need to explicitly remove windowSink
			PlayerData.pipe.setState(State.READY);
			PlayerData.pipe.remove(PlayerData.windowSink);
			PlayerData.pipe.setState(State.NULL);
		}
		for(int i=0; i < PlayerData.elems.size(); i++)
		{
			PlayerData.elems.get(0).dispose();
			PlayerData.elems.remove(0);
		}
	}
	
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void player_pipeline()
	{
		PlayerData.pipe = new Pipeline("player-pipeline");
		
		Element source = ElementFactory.make("filesrc", "source");
		Element colorspace = ElementFactory.make("ffmpegcolorspace", "colorspace");
		Element decoder = ElementFactory.make("decodebin2", "decoder");
		
		source.set("location", PlayerData.file);
		
		//set probes in the pads
		//Pad decoderPad = decoder.getStaticPad("sink");
		//decoderPad.addEventProbe(probeHandler);
		//source.getStaticPad("src").addEventProbe(probeHandler);

		/*Pad sourcePad = source.getStaticPad("src");
		Caps sourceCaps = sourcePad.getCaps();
		Caps colorspaceCaps = sourceCaps.copy();
		System.out.println(colorspaceCaps.toString());
		Structure c = sourceCaps.getStructure(0);
		String capsString = "video/x-raw-yuv" + ",height=" + c.getValue("height") + ",width=" + c.getValue("width") + ",framerate=15" + ",format=\\(fourcc\\)" + c.getValue("format"); 
		String capsString = "video/x-raw-yuv,height=240,width=320,rate=15/1,framerate=15/1,format=\\(fourcc\\)I420";
		System.out.println(capsString);
		Caps colorspaceCaps = Caps.fromString(capsString);*/
		
		decoder.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				System.out.printf("New pad %s added to %s\n", newPad.getName(), source.getName());
				Pad sink_pad = PlayerData.windowSink.getStaticPad("sink");
				if(sink_pad.isLinked())
					System.out.println("Pad already linked");
				PadLinkReturn ret = newPad.link(sink_pad);
				if(ret == null)
					System.out.println("Pad link failed");				
			}
		});
		
		PlayerData.elems.add(source);
		PlayerData.elems.add(colorspace);
		PlayerData.elems.add(decoder);
		
		PlayerData.pipe.addMany(source, colorspace, decoder, PlayerData.windowSink);
		//Element.linkPadsFiltered(source, "src", colorspace, "sink", colorspaceCaps);
		//Element.linkMany(colorspace, decoder, windowSink);
		Element.linkMany(source, decoder, PlayerData.windowSink);
		//gray out undesired buttons
		if(PlayerData.controlButtons.size() != 0)
		{
			PlayerData.controlButtons.get(0).setEnabled(true);
			PlayerData.controlButtons.get(1).setEnabled(true);
			PlayerData.controlButtons.get(2).setEnabled(true);
			PlayerData.controlButtons.get(3).setEnabled(false);
			PlayerData.controlButtons.get(4).setEnabled(true);
		}

		SeekEvent seekEvent = new SeekEvent(2, Format.TIME, 0, SeekType.CUR, 0, SeekType.CUR, 100);
		System.out.println(seekEvent.getRate());
		
	}
	
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void videorecorder_pipeline()
	{
		PlayerData.pipe = new Pipeline("video-recorder");
		
		Element source = ElementFactory.make("v4l2src", "source");
		Element tee = ElementFactory.make("tee", "tee");
		Element streamQueue = ElementFactory.make("queue", "stream-queue");
		Element recorderQueue = ElementFactory.make("queue", "recorder-queue");
		Caps encCaps = new Caps();
		encCaps = Caps.fromString("video/x-raw-yuv, " + PlayerData.resolution + PlayerData.frameRate); // width=640, height=480, framerate=20/1");
		Element colorspace = ElementFactory.make("ffmpegcolorspace", "colorspace");
		Element encoder = ElementFactory.make("jpegenc", "encoder");;
		switch(PlayerData.vidEnc)
		{
		case MJPEG:
			encoder = ElementFactory.make("jpegenc", "encoder");
			break;
		case MPEG4:
			encoder = ElementFactory.make("ffenc_mpeg4", "encoder");
			break;
		default:
			break;
		}
		Element mux = ElementFactory.make("avimux", "mux");
		Element sink = ElementFactory.make("filesink", "sink");
		
		tee.set("silent", "false");
		sink.set("location", "Recording1.mpg");
		
		PlayerData.elems.add(source);
		PlayerData.elems.add(tee);
		PlayerData.elems.add(streamQueue);
		PlayerData.elems.add(recorderQueue);
		PlayerData.elems.add(colorspace);
		PlayerData.elems.add(encoder);
		PlayerData.elems.add(mux);
		PlayerData.elems.add(sink);
		
		PlayerData.pipe.addMany(source, tee, streamQueue, recorderQueue, colorspace, encoder, mux, sink, PlayerData.windowSink);
		
		if(!Element.linkMany(source, tee))
			System.out.println("Failed: webcam -> tee");
		if(!Element.linkPadsFiltered(recorderQueue, "src", colorspace, "sink", encCaps))
			System.out.println("Failed: recorder queue -> colorspace");
		if(!Element.linkMany(colorspace, encoder, mux, sink))
			System.out.println("Failed: colorspace -> encoder -> file sink");
		if(!Element.linkMany(streamQueue, PlayerData.windowSink))
			System.out.println("Failed: stream queue -> widget");
		
		//create two new src pads with and link them with queue pads
		Pad teeStreamSrc = tee.getRequestPad("src%d");
		Pad teeRecorderSrc = tee.getRequestPad("src%d");
		Pad queueStreamSink = streamQueue.getStaticPad("sink");
		Pad queueRecorderSink = recorderQueue.getStaticPad("sink");
		
		teeStreamSrc.link(queueStreamSink);
		teeRecorderSrc.link(queueRecorderSink);
		
		if(PlayerData.controlButtons.size() != 0 )
		{
			PlayerData.controlButtons.get(0).setEnabled(false);
			PlayerData.controlButtons.get(1).setEnabled(true);
			PlayerData.controlButtons.get(2).setEnabled(true);
			PlayerData.controlButtons.get(3).setEnabled(true);
			PlayerData.controlButtons.get(4).setEnabled(false);
		}
	}
	
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void audiorecorder_pipeline()
	{
		PlayerData.pipe = new Pipeline("audio-recorder");
		
		Element source = ElementFactory.make("alsasrc", "source");
		Element converter = ElementFactory.make("audioconvert", "converter");
		Element encoder = ElementFactory.make("vorbisenc", "encoder");
		switch(PlayerData.audEnc)
		{
		case ALAW:
			encoder = ElementFactory.make("alawenc", "encoder");
			break;
		case MULAW:
			encoder = ElementFactory.make("mulawenc", "encoder");
			break;
		case MKV:
			encoder = ElementFactory.make("vorbisenc", "encoder");
			break;
		default:
			break;
		}
		Element mux = ElementFactory.make("webmmux", "mux");
		Element sink = ElementFactory.make("filesink", "sink");
		
		source.set("device", "hw:2");
		sink.set("location", "AudioRec1.mkv");
		
		PlayerData.elems.add(source);
		PlayerData.elems.add(converter);
		PlayerData.elems.add(encoder);
		PlayerData.elems.add(mux);
		PlayerData.elems.add(sink);
		
		PlayerData.pipe.addMany(source, converter, encoder, mux, sink);
		
		Element.linkMany(source, converter, encoder, mux, sink);
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
		PlayerData.pipe.getBus().connect(new Bus.TAG() {
			public void tagsFound(GstObject source, TagList tagList) {
				for(String tagName : tagList.getTagNames())
				{
					for(Object tagData : tagList.getValues(tagName))
					{
						String data = "[" + tagName + "] = " + tagData + " \n";
						PlayerData.monitor.append(data);
					}
				}
			}
		});
		
		//connect to signal EOS
		PlayerData.pipe.getBus().connect(new Bus.EOS() {
			public void endOfStream(GstObject source) {
				//exit gracefully
				System.out.printf("[%s} reached EOS.\n", source);
				Gst.quit();
			}
		});
		
		//connect to signal ERROR
		PlayerData.pipe.getBus().connect(new Bus.ERROR() {
			
			@Override
			public void errorMessage(GstObject source, int code, String message) {
				//print error from message
				System.out.printf("[%s] encountered error code %d: %s\n",source, code, message);
				Gst.quit();
			}
		});
		
		//connect to change of state
		PlayerData.pipe.getBus().connect(new Bus.STATE_CHANGED() {
			@Override
			public void stateChanged(GstObject source, State oldstate, State newstate, State pending) {
				if(source.equals(PlayerData.pipe))
				{
					System.out.printf("[%s] changed state from %s to %s\n", source.getName(), oldstate.toString(), newstate.toString());
				}
			}
		});
		 
		//connect to buffering signal for monitor data
		PlayerData.pipe.getBus().connect(new Bus.BUFFERING() {
			public void bufferingData(GstObject source, int percentage) {
				System.out.println("Source " + source.getName() + ": " + percentage);
			}
		});
	}
}
