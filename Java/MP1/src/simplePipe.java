//java utilities
import java.util.ArrayList;
import java.util.List;
//gstreamer
import org.gstreamer.Bus;
import org.gstreamer.Element; 
import org.gstreamer.ElementFactory; 
import org.gstreamer.Gst; 
import org.gstreamer.Pad;
import org.gstreamer.Caps;
import org.gstreamer.PadDirection;
import org.gstreamer.PadLinkReturn;
import org.gstreamer.Pipeline; 
import org.gstreamer.State;
import org.gstreamer.GstObject;
import org.gstreamer.TagList;
import org.gstreamer.swing.VideoComponent;
//jframe
import java.awt.BorderLayout; 
import java.awt.Dimension; 
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JFrame; 
import javax.swing.JPanel;
import javax.swing.LayoutStyle;
import javax.swing.SwingUtilities; 
import javax.swing.JButton;

public class simplePipe
{
	public enum Mode
	{
		PLAYER, VIDEO_RECORDER, AUDIO_RECORDER
	}
	
	private static Pipeline pipe;
	private static List<Element> elems = new ArrayList<Element>();
	private static Element windowSink;
	private static Mode mode;
	private static VideoComponent vid_comp;
	
	//private methods	
	private static void modify_pipeline()
	{
		switch(mode)
		{
		case PLAYER:
			System.out.println("Initializing player");
			discard_pipeline();
			player_pipeline();
			pipe.setState(State.READY);
			break;
		case VIDEO_RECORDER:
			System.out.println("Initializing video recorder");
			discard_pipeline();
			videorecorder_pipeline();
			pipe.setState(State.READY);
			break;
		case AUDIO_RECORDER:
			System.out.println("Initializing audio recorder");
			discard_pipeline();
			audiorecorder_pipeline();
			pipe.setState(State.READY);
			break;
		default:
			System.out.println("Unrecognized pipeline");
			pipe.setState(State.READY);
			break;
		}
	}
	
	private static void discard_pipeline()
	{
		if(pipe != null)
		{
			//need to explicitly remove windowSink
			pipe.remove(windowSink);
			pipe.setState(State.NULL);
		}
		for(int i=0; i < elems.size(); i++)
		{
			elems.get(0).dispose();
			elems.remove(0);
		}
	}
	
	private static void player_pipeline()
	{
		pipe = new Pipeline("player-pipeline");
		
		Element source = ElementFactory.make("filesrc", "source");
		Element decoder = ElementFactory.make("decodebin2", "decoder");
		
		source.set("location", "Cranes.mpg");
		
		decoder.connect(new Element.PAD_ADDED() {
			
			@Override
			public void padAdded(Element source, Pad newPad) {
				// TODO Auto-generated method stub
				System.out.printf("New pad %s added to %s\n", newPad.getName(), source.getName());
				
				//get sink pad
				Pad sink_pad = windowSink.getStaticPad("sink");
				
				if(sink_pad.isLinked())
					System.out.println("Pad already linked");
				
				//link pads
				PadLinkReturn ret = newPad.link(sink_pad);
				if(ret == null)
					System.out.println("Pad link failed");				
			}
		});
		
		elems.add(source);
		elems.add(decoder);
		
		pipe.addMany(source, decoder, windowSink);
		Element.linkMany(source, decoder, windowSink);
	}
	
	private static void videorecorder_pipeline()
	{
		pipe = new Pipeline("video-recorder");
		
		Element source = ElementFactory.make("v4l2src", "source");
		Element tee = ElementFactory.make("tee", "tee");
		Element streamQueue = ElementFactory.make("queue", "stream-queue");
		Element recorderQueue = ElementFactory.make("queue", "recorder-queue");
		Caps encCaps = new Caps();
		encCaps = Caps.fromString("video/x-raw-yuv, width=640, height=480, framerate=20/1");
		Element colorspace = ElementFactory.make("ffmpegcolorspace", "colorspace");
		Element encoder = ElementFactory.make("jpegenc", "encoder");
		Element mux = ElementFactory.make("avimux", "mux");
		Element sink = ElementFactory.make("filesink", "sink");
		
		tee.set("silent", "false");
		sink.set("location", "Recording1.mpg");
		
		elems.add(source);
		elems.add(tee);
		elems.add(streamQueue);
		elems.add(recorderQueue);
		elems.add(colorspace);
		elems.add(encoder);
		elems.add(mux);
		elems.add(sink);
		
		pipe.addMany(source, tee, streamQueue, recorderQueue, colorspace, encoder, mux, sink, windowSink);
		
		if(!Element.linkMany(source, tee))
			System.out.println("Failed: webcam -> tee");
		if(!Element.linkPadsFiltered(recorderQueue, "src", colorspace, "sink", encCaps))
			System.out.println("Failed: recorder queue -> colorspace");
		if(!Element.linkMany(colorspace, encoder, mux, sink))
			System.out.println("Failed: colorspace -> encoder -> file sink");
		if(!Element.linkMany(streamQueue, windowSink))
			System.out.println("Failed: stream queue -> widget");
		
		//create two new src pads with and link them with queue pads
		Pad teeStreamSrc = tee.getRequestPad("src%d");
		Pad teeRecorderSrc = tee.getRequestPad("src%d");
		Pad queueStreamSink = streamQueue.getStaticPad("sink");
		Pad queueRecorderSink = recorderQueue.getStaticPad("sink");
		
		teeStreamSrc.link(queueStreamSink);
		teeRecorderSrc.link(queueRecorderSink);
	}
	
	private static void audiorecorder_pipeline()
	{
		pipe = new Pipeline("audio-recorder");
		
		Element source = ElementFactory.make("alsasrc", "source");
		Element converter = ElementFactory.make("audioconvert", "converter");
		Element encoder = ElementFactory.make("vorbisenc", "encoder");
		Element mux = ElementFactory.make("webmmux", "mux");
		Element sink = ElementFactory.make("filesink", "sink");
		
		source.set("device", "hw:2");
		sink.set("location", "AudioRec1.mkv");
		
		elems.add(source);
		elems.add(converter);
		elems.add(encoder);
		elems.add(mux);
		elems.add(sink);
		
		pipe.addMany(source, converter, encoder, mux, sink);
		
		Element.linkMany(source, converter, encoder, mux, sink);
	}
	
	private static void connect_to_signals()
	{
		//connect to signal TAG
		pipe.getBus().connect(new Bus.TAG() {
			
			@Override
			public void tagsFound(GstObject source, TagList tagList) {
				// TODO Auto-generated method stub
				for(String tagName : tagList.getTagNames())
				{
					for(Object tagData : tagList.getValues(tagName))
					{
						System.out.printf("[%s]=%s\n", tagName, tagData);
					}
				}
			}
		});
		
		//connect to signal EOS
		pipe.getBus().connect(new Bus.EOS() {
			
			@Override
			public void endOfStream(GstObject source) {
				// TODO Auto-generated method stub
				//exit gracefully
				System.out.printf("[%s} reached EOS.\n", source);
				Gst.quit();
			}
		});
		
		//connect to signal ERROR
		pipe.getBus().connect(new Bus.ERROR() {
			
			@Override
			public void errorMessage(GstObject source, int code, String message) {
				// TODO Auto-generated method stub
				//print error from message
				System.out.printf("[%s] encountered error code %d: %s\n",source, code, message);
				Gst.quit();
			}
		});
		
		//connect to change of state
		pipe.getBus().connect(new Bus.STATE_CHANGED() {
			
			@Override
			public void stateChanged(GstObject source, State oldstate, State newstate, State pending) {
				// TODO Auto-generated method stub
				if(source.equals(pipe))
					System.out.printf("[%s] changed state from %s to %s\n", source.getName(), oldstate.toString(), newstate.toString());
			}
		});
	}
	
	public static void main(String[] args)
	{
		//initialize GStreamer
		args = Gst.init("Simple Pipeline", args);
		
		//set startup mode
		mode = Mode.PLAYER;
		
		//initialize static window reference
		vid_comp = new VideoComponent();
		windowSink = vid_comp.getElement();
		
		//construct pipeline
		modify_pipeline();
		
		//subscribe to messages
		connect_to_signals();
		
		SwingUtilities.invokeLater(new Runnable() 
		{ 
			public void run() 
			{
				//Player button
				JButton player_button = new JButton("Player");
				//player_button.setSize(50,100);
				//player_button.setLocation(1,1);
				player_button.addActionListener(new ActionListener() 
				{
					public void actionPerformed(ActionEvent e) 
					{
						System.out.println("Switching to player");
						mode = Mode.PLAYER;
						modify_pipeline();
					}
				});
				//Recorder button
				JButton recorder_button = new JButton("Recorder");
				//recorder_button.setSize(50,100);
				//recorder_button.setLocation(1,114);
				recorder_button.addActionListener(new ActionListener() 
				{
					public void actionPerformed(ActionEvent e) 
					{
						System.out.println("Switching to recorder");
						mode = Mode.VIDEO_RECORDER;
						modify_pipeline();
					}
				});
				//play button
				JButton playButton = new JButton("Play");
				playButton.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e) {
						// TODO Auto-generated method stub
						System.out.println("Setting state to playing");
						pipe.setState(State.PLAYING);
					}					
				});
				//pause button
				JButton pauseButton = new JButton("Pause");
				pauseButton.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e) {
						// TODO Auto-generated method stub
						System.out.println("Setting state to paused");
						pipe.setState(State.PAUSED);
					}					
				});
				//stop button
				JButton stopButton = new JButton("Stop");
				stopButton.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e) {
						// TODO Auto-generated method stub
						System.out.println("Setting state to ready");
						pipe.setState(State.READY);
					}					
				});
				//record button
				JButton recordButton = new JButton("Record");
				recordButton.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e) {
						// TODO Auto-generated method stub
						System.out.println("Setting state to playing");
						pipe.setState(State.PLAYING);
					}					
				});
				
				JPanel panel = new JPanel();
				//panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
				panel.add(player_button);
				panel.add(recorder_button);
				panel.add(playButton);
				panel.add(pauseButton);
				panel.add(stopButton);
				panel.add(recordButton);
				//define layout
				GroupLayout layout = new GroupLayout(panel);
				layout.setAutoCreateGaps(true);
				layout.setAutoCreateContainerGaps(true);
				
				layout.setHorizontalGroup(
						layout.createSequentialGroup()						
						.addComponent(playButton)
						.addComponent(pauseButton)
						.addComponent(stopButton)
						.addComponent(recordButton)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                     GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(recorder_button)						
						.addComponent(player_button)
				);
				layout.setVerticalGroup(
						layout.createParallelGroup()						
						.addComponent(playButton)
						.addComponent(pauseButton)
						.addComponent(stopButton)
						.addComponent(recordButton)
						.addComponent(recorder_button)
						.addComponent(player_button)
				);
				panel.setLayout(layout);
				
				
	            JFrame frame = new JFrame("vPlayer"); 
	            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	            
	            frame.add(panel, BorderLayout.SOUTH);
	            frame.getContentPane().add(vid_comp, BorderLayout.CENTER);
	            vid_comp.setPreferredSize(new Dimension(640, 480)); 
	            frame.setSize(1080, 920);
	            frame.setVisible(true); 
	            // Start the pipeline processing 
	            pipe.setState(State.PLAYING); 
	        } 
	    });
		pipe.setState(State.NULL);
	}
}