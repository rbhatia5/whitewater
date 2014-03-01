import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import javax.swing.*;
import org.gstreamer.*;
import org.gstreamer.swing.*;

public class simplePipe
{
	public enum Mode
	{
		PLAYER, VIDEO_RECORDER, AUDIO_RECORDER
	}
	
	public enum VideoEncoding
	{
		MJPEG, MPEG4
	}
	
	public enum AudioEncoding
	{
		ALAW, MULAW, MKV
	}
	
	private static Pipeline pipe;
	private static List<Element> elems = new ArrayList<Element>();
	private static Element windowSink;
	private static JFrame frame;
	private static Mode mode;
	private static VideoEncoding vidEnc;
	private static AudioEncoding audEnc;
	private static VideoComponent vid_comp;
	private static String frameRate;
	private static String resolution;
	private static List<JButton> controlButtons = new ArrayList<JButton>(); //{0:Play, 1:Pause, 2:Stop, 3:Record, 4:Open, 5:Player, 6:Recorder 
	private static String file;
	private static JPanel controls;
	private static JTextArea monitor;
	
/////////////////////////////////////////////////////////////////////MODIFY_PIPELINE///////////////////////////////////////////////////////////////////////////
	
	private static void modify_pipeline()
	{
		switch(mode)
		{
		case PLAYER:
			System.out.println("Initializing player");
			discard_pipeline();
			player_pipeline();
			connect_to_signals();
			pipe.setState(State.READY);
			break;
		case VIDEO_RECORDER:
			System.out.println("Initializing video recorder");
			discard_pipeline();
			videorecorder_pipeline();
			connect_to_signals();
			pipe.setState(State.READY);
			break;
		case AUDIO_RECORDER:
			System.out.println("Initializing audio recorder");
			discard_pipeline();
			audiorecorder_pipeline();
			connect_to_signals();
			pipe.setState(State.READY);
			break;
		default:
			System.out.println("Unrecognized pipeline");
			pipe.setState(State.READY);
			break;
		}
	}
	
/////////////////////////////////////////////////////////////////////DISCARD_PIPELINE///////////////////////////////////////////////////////////////////////////
	
	private static void discard_pipeline()
	{
		if(pipe != null)
		{
			//need to explicitly remove windowSink
			pipe.setState(State.READY);
			pipe.remove(windowSink);
			pipe.setState(State.NULL);
		}
		for(int i=0; i < elems.size(); i++)
		{
			elems.get(0).dispose();
			elems.remove(0);
		}
	}
	
/////////////////////////////////////////////////////////////////////PLAYER_PIPELINE///////////////////////////////////////////////////////////////////////////
	
	private static void player_pipeline()
	{
		pipe = new Pipeline("player-pipeline");
		
		Element source = ElementFactory.make("filesrc", "source");
		Element decoder = ElementFactory.make("decodebin2", "decoder");
		
		source.set("location", file);
		
		decoder.connect(new Element.PAD_ADDED() {
			
			@Override
			public void padAdded(Element source, Pad newPad) {
				System.out.printf("New pad %s added to %s\n", newPad.getName(), source.getName());
				Pad sink_pad = windowSink.getStaticPad("sink");
				if(sink_pad.isLinked())
					System.out.println("Pad already linked");
				PadLinkReturn ret = newPad.link(sink_pad);
				if(ret == null)
					System.out.println("Pad link failed");				
			}
		});
		
		elems.add(source);
		elems.add(decoder);
		
		pipe.addMany(source, decoder, windowSink);
		Element.linkMany(source, decoder, windowSink);
		//gray out undesired buttons
		if(controlButtons.size() != 0)
		{
			controlButtons.get(0).setEnabled(true);
			controlButtons.get(1).setEnabled(true);
			controlButtons.get(2).setEnabled(true);
			controlButtons.get(3).setEnabled(false);
			controlButtons.get(4).setEnabled(true);
		}
	}
	
/////////////////////////////////////////////////////////////////////VIDEORECORDER_PIPELINE///////////////////////////////////////////////////////////////////////////
	
	private static void videorecorder_pipeline()
	{
		pipe = new Pipeline("video-recorder");
		
		Element source = ElementFactory.make("v4l2src", "source");
		Element tee = ElementFactory.make("tee", "tee");
		Element streamQueue = ElementFactory.make("queue", "stream-queue");
		Element recorderQueue = ElementFactory.make("queue", "recorder-queue");
		Caps encCaps = new Caps();
		encCaps = Caps.fromString("video/x-raw-yuv, " + resolution + frameRate); // width=640, height=480, framerate=20/1");
		Element colorspace = ElementFactory.make("ffmpegcolorspace", "colorspace");
		Element encoder = ElementFactory.make("jpegenc", "encoder");;
		switch(vidEnc)
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
		
		if(controlButtons.size() != 0 )
		{
			controlButtons.get(0).setEnabled(false);
			controlButtons.get(1).setEnabled(true);
			controlButtons.get(2).setEnabled(true);
			controlButtons.get(3).setEnabled(true);
			controlButtons.get(4).setEnabled(false);
		}
	}
	
/////////////////////////////////////////////////////////////////////AUDIORECORDER_PIPELINE///////////////////////////////////////////////////////////////////////////
	
	private static void audiorecorder_pipeline()
	{
		pipe = new Pipeline("audio-recorder");
		
		Element source = ElementFactory.make("alsasrc", "source");
		Element converter = ElementFactory.make("audioconvert", "converter");
		Element encoder = ElementFactory.make("vorbisenc", "encoder");
		switch(audEnc)
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
		
		elems.add(source);
		elems.add(converter);
		elems.add(encoder);
		elems.add(mux);
		elems.add(sink);
		
		pipe.addMany(source, converter, encoder, mux, sink);
		
		Element.linkMany(source, converter, encoder, mux, sink);
	}

/////////////////////////////////////////////////////////////////////CONNECT_TO_SINGALS///////////////////////////////////////////////////////////////////////////	
	
	private static void connect_to_signals()
	{
		//connect to signal TAG
		pipe.getBus().connect(new Bus.TAG() {
			public void tagsFound(GstObject source, TagList tagList) {
				// TODO Auto-generated method stub
				for(String tagName : tagList.getTagNames())
				{
					for(Object tagData : tagList.getValues(tagName))
					{
						String data = "[" + tagName + "] = " + tagData + " \n";
						monitor.append(data);
					}
				}
			}
		});
		
		//connect to signal EOS
		pipe.getBus().connect(new Bus.EOS() {
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
	
	
/////////////////////////////////////////////////////////////////////CREATE_CONTROL_PANEL///////////////////////////////////////////////////////////////////////////
	
	static JPanel createControlPanel()
	{
		//Player button				
		JButton player_button = new JButton("Player");
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
		JButton playButton;
		URL playURL;
		ImageIcon play;
		File check = new File("resources/123play.bmp");
		if(check.exists())
		{
			//playURL = getClass().getResource("resources/play.gif");
			//play = new ImageIcon(playURL);
			//playButton = new JButton(play);
			play = new ImageIcon("resources/play.gif","haha");
			playButton = new JButton(play);
		}
		else
			playButton = new JButton("Play");
		playButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Setting state to playing");
				pipe.setState(State.PLAYING);
			}					
		});
		//pause button
		JButton pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Setting state to paused");
				pipe.setState(State.PAUSED);
			}					
		});
		//stop button
		JButton stopButton = new JButton("Stop");
		stopButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Setting state to ready");
				pipe.setState(State.READY);
			}					
		});
		//record button
		JButton recordButton = new JButton("Record");
		recordButton.setEnabled(false);
		recordButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Setting state to playing");
				pipe.setState(State.PLAYING);
			}					
		});
		//file open button
		JButton fileOpenButton = new JButton("Open");
		fileOpenButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Opening file picker");
				pipe.setState(State.READY);
				JFileChooser fileChooser = new JFileChooser();
				int ret = fileChooser.showOpenDialog(null);
				if (ret == JFileChooser.APPROVE_OPTION) {
		            File f = fileChooser.getSelectedFile();
		            String fWPath = f.getAbsolutePath();
		            file = fWPath;
		            System.out.println("Opening: " + fWPath);
		            modify_pipeline();
		        } else {
		        	System.out.println("Open command cancelled by user");
		        }
			}	
		});
		
		JPanel controls = new JPanel();
		controls.add(player_button);
		controls.add(recorder_button);
		controls.add(playButton);
		controls.add(pauseButton);
		controls.add(stopButton);
		controls.add(recordButton);
		
		//add buttons to list
		controlButtons.add(playButton);
		controlButtons.add(pauseButton);
		controlButtons.add(stopButton);
		controlButtons.add(recordButton);
		controlButtons.add(fileOpenButton);
		controlButtons.add(recorder_button);
		controlButtons.add(player_button);
		
		//define layout
		GroupLayout layout = new GroupLayout(controls);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		layout.setHorizontalGroup(
				layout.createSequentialGroup()						
				.addComponent(playButton)
				.addComponent(pauseButton)
				.addComponent(stopButton)
				.addComponent(recordButton)
				.addComponent(fileOpenButton)
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
				.addComponent(fileOpenButton)
				.addComponent(recorder_button)
				.addComponent(player_button)
		);
		controls.setLayout(layout);
		return controls;
	}
	
	
/////////////////////////////////////////////////////////////////////CREATE_USER_OPTIONS_PANEL///////////////////////////////////////////////////////////////////////////

static JPanel createUserOptionsPanel()
{
	//resolution options
	String[] resList = {"320x240", "640x480", "960x720", "1280x1080"};
	//framerate options
	String[] frList = {"10", "15", "20", "30"};
	
	//frame rate list picker
	JComboBox resCB = new JComboBox(resList);
	resCB.setPreferredSize(new Dimension(100,50));
	resCB.setSelectedIndex(0);
	resCB.setPreferredSize(new Dimension(70,30));
	resCB.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			JComboBox source = (JComboBox)e.getSource();
			String selected = (String)source.getSelectedItem();
			if(selected.equals("320x240"))
				resolution = ",width=320, height=240 ";
			else if(selected.equals("640x480"))
				resolution = ",width=640, height=480 ";
			else if(selected.equals("960x720"))
				resolution = ",width=960, height=720 ";
			else if(selected.equals("1280x1080"))
				resolution = ",width=1280, height=1080 ";
		}
	});
	//resolution list picker
	JComboBox frCB = new JComboBox(frList);
	frCB.setPreferredSize(new Dimension(100,50));
	frCB.setSelectedIndex(0);
	frCB.setPreferredSize(new Dimension(70,30));
	frCB.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			JComboBox source = (JComboBox)e.getSource();
			String selected = (String)source.getSelectedItem();
			if(selected.equals("10"))
				frameRate = ",framerate=10/1 ";
			else if(selected.equals("15"))
				frameRate = ",framerate=15/1 ";
			else if(selected.equals("20"))
				frameRate = ",framerate=20/1 ";
			else if(selected.equals("30"))
				frameRate = ",framerate=30/1 ";
		}
	});
	
	JPanel userOptions = new JPanel();
	userOptions.setPreferredSize(new Dimension(100,150));
	userOptions.add(resCB);
	userOptions.add(frCB);
	
	GroupLayout userOptionsLayout = new GroupLayout(userOptions);
	userOptionsLayout.setAutoCreateGaps(true);
	userOptionsLayout.setAutoCreateContainerGaps(true);
	
	userOptionsLayout.setHorizontalGroup(
		userOptionsLayout.createParallelGroup()
			.addComponent(resCB)
			.addComponent(frCB)
	);
	userOptionsLayout.setVerticalGroup(
		userOptionsLayout.createSequentialGroup()
			.addComponent(resCB)
			.addComponent(frCB)
	);
	userOptions.setLayout(userOptionsLayout);
	
	return userOptions;
}
	
	
/////////////////////////////////////////////////////////////////////CREATE_ENCODING_OPTIONS_PANEL///////////////////////////////////////////////////////////////////////////
	
	static JPanel createEncodingOptionsPanel()
	{
		//create user options panel
		JPanel userOptions = createUserOptionsPanel();	
		
		//mjpeg radio button
		JRadioButton mjpegButton = new JRadioButton("mjpeg");
		mjpegButton.setSelected(true);
		mjpegButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("MJPEG encoding");
				vidEnc = VideoEncoding.MJPEG;
				modify_pipeline();
			}					
		});
		
		//mpeg4 radio button
		JRadioButton mpeg4Button = new JRadioButton("mpeg4");
		mpeg4Button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("MPEG4 encoding");
				vidEnc = VideoEncoding.MPEG4;
				modify_pipeline();
			}					
		});
		
		//alaw radio button
		JRadioButton alawButton = new JRadioButton("alaw");
		alawButton.setSelected(true);
		alawButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("ALAW encoding");
				audEnc = AudioEncoding.ALAW;
				modify_pipeline();
			}					
		});
		
		//mulaw radio button
		JRadioButton mulawButton = new JRadioButton("mulaw");
		mulawButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("MULAW encoding");
				audEnc = AudioEncoding.MULAW;
				modify_pipeline();
			}					
		});
		
		ButtonGroup vidGroup = new ButtonGroup();
		vidGroup.add(mjpegButton);
		vidGroup.add(mpeg4Button);
		
		ButtonGroup audGroup = new ButtonGroup();
		audGroup.add(alawButton);
		audGroup.add(mulawButton);
		
		//Monitor data
		monitor = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(monitor);
		monitor.setEditable(false);
		
		JPanel encOptions = new JPanel();

		encOptions.add(userOptions);
		encOptions.add(mjpegButton);
		encOptions.add(mpeg4Button);
		encOptions.add(alawButton);
		encOptions.add(mulawButton);
		
		//define layout
		GroupLayout encLayout = new GroupLayout(encOptions);
		encLayout.setAutoCreateGaps(true);
		encLayout.setAutoCreateContainerGaps(true);
		
		encLayout.setHorizontalGroup(
				encLayout.createParallelGroup()
					.addGroup(encLayout.createSequentialGroup()						
					.addComponent(monitor)
					)
					.addGroup(encLayout.createSequentialGroup()						
					.addComponent(userOptions)
					)
					.addGroup(encLayout.createSequentialGroup()						
					.addComponent(mjpegButton)
					.addComponent(mpeg4Button)
					)
					.addGroup(encLayout.createSequentialGroup()						
					.addComponent(alawButton)
					.addComponent(mulawButton)
					)
		);
		encLayout.setVerticalGroup(
				encLayout.createSequentialGroup()		
					.addGroup(encLayout.createParallelGroup()						
					.addComponent(monitor)
					)
					.addGroup(encLayout.createParallelGroup()						
					.addComponent(userOptions)
					)
					.addGroup(encLayout.createParallelGroup()						
					.addComponent(mjpegButton)
					.addComponent(mpeg4Button)
					)
					.addGroup(encLayout.createParallelGroup()						
					.addComponent(alawButton)
					.addComponent(mulawButton)
					)
		);
		encOptions.setLayout(encLayout);
		return encOptions;
	}

	
/////////////////////////////////////////////////////////////////////MAIN///////////////////////////////////////////////////////////////////////////
	
	public static void main(String[] args)
	{
		//initialize GStreamer
		args = Gst.init("Simple Pipeline", args);
		
		//set startup mode
		mode = Mode.PLAYER;
		vidEnc = VideoEncoding.MJPEG;
		audEnc = AudioEncoding.ALAW;
		resolution = ",width=640, height=480";
		frameRate = ",framerate=10/1";
		file = "Cranes.mpg";
		
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
				//create the control panel
				controls = createControlPanel();
				
				//create encoding options panel
				JPanel encOptions = createEncodingOptionsPanel();
				
				//Actual top level widget
	            frame = new JFrame("vPlayer"); 
	            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	            
	            frame.add(controls, BorderLayout.SOUTH);
	            frame.add(encOptions, BorderLayout.EAST);
	            frame.getContentPane().add(vid_comp, BorderLayout.CENTER);
	            vid_comp.setPreferredSize(new Dimension(640, 480)); 
	            frame.setSize(1080, 920);
	            frame.setVisible(true);
	        } 
	    });
		pipe.setState(State.NULL);
	}
}