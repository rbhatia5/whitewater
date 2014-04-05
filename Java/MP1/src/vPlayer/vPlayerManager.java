package vPlayer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

import org.gstreamer.*;
import org.gstreamer.swing.*;

public class vPlayerManager {
	
	public static void main(String[] args)
	{
		//initialize GStreamer
		args = Gst.init("Simple Pipeline", args);
		
		//set startup mode
		PlayerData.mode = PlayerData.Mode.PLAYER;
		PlayerData.vidEnc = PlayerData.VideoEncoding.MJPEG;
		PlayerData.audEnc = PlayerData.AudioEncoding.ALAW;
		PlayerData.resolution = ",width=640, height=480";
		PlayerData.frameRate = ",framerate=10/1";
		PlayerData.file = "Cranes.mpg";
		PlayerData.seek = false;
		
		//initialize static window reference
		PlayerData.vid_comp = new VideoComponent();
		PlayerData.windowSink = PlayerData.vid_comp.getElement();
		
		PipelineManager.modify_pipeline();
		
		SwingUtilities.invokeLater(new Runnable() 
		{ 
			public void run() 
			{
				PlayerData.pipe.setState(State.READY);
				PlayerData.pipe.setState(State.PLAYING);
	    		System.out.println(PlayerData.pipe.queryDuration(Format.TIME));
	    		PlayerData.pipe.setState(State.PAUSED);
	    		
				//create the control panel
	    		PlayerData.controls = GUIManager.createControlPanel();
				
				//create encoding options panel
				JPanel encOptions = GUIManager.createEncodingOptionsPanel();
				
				//Actual top level widget
				PlayerData.frame = new JFrame("vPlayer"); 
				PlayerData.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	            
				PlayerData.frame.add(PlayerData.controls, BorderLayout.SOUTH);
				PlayerData.frame.add(encOptions, BorderLayout.EAST);
				PlayerData.frame.getContentPane().add(PlayerData.vid_comp, BorderLayout.CENTER);
				PlayerData.vid_comp.setPreferredSize(new Dimension(640, 480)); 
				PlayerData.frame.setSize(1080, 920);
				PlayerData.frame.setVisible(true);
				/*
	            Timer timer = new Timer(1000, new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						if(PlayerData.pipe.isPlaying() && !PlayerData.seek)
						{
							PlayerData.position = PlayerData.pipe.queryPosition(Format.TIME);
							PlayerData.slider.setValue((int)(PlayerData.position/1000000000));
						}
					}
	            });	
	            timer.start();
	            */
	        } 
	    });
		
		//PlayerData.pipe.setState(State.PLAYING);
		//Gst.main();
		//
	}
}
