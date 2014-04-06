package vClient;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

import org.gstreamer.*;
import org.gstreamer.swing.*;
import org.gstreamer.elements.*;
import org.gstreamer.elements.good.RTPBin;

public class vClientManager {
	
	public static String negotiateProperties(String properties)
	{		
		ClientGUIManager.sendServerMessage(properties);
		while(!ClientData.mainThread.interrupted());
		
		if(!ClientData.serverResponse.equals(properties))
		{
			System.out.println("Negotiation Failed: Server cannot facilitate request. Modify properties to " + ClientData.serverResponse);
		}
		else
		{
			System.out.println("Negotiation Successful: Setting properties to " + ClientData.serverResponse);
		}
		return ClientData.serverResponse;
	}
	
	public static void main(String[] args)
	{
		ClientData.mainThread = Thread.currentThread();
		ClientData.state = ClientData.State.NEGOTIATING;
		
		String properties = ClientGUIManager.adjustProperties();
		
		properties = negotiateProperties(properties);
		
		//ClientGUIManager.sendServerMessage("play");
			
		args = Gst.init("Client Pipeline", args);
		
		ClientData.mode = ClientData.Mode.CLIENT;
		
		ClientData.resolution = ",width=640, height=480";
		ClientData.frameRate = ",framerate=10/1";
		ClientData.file = "Cranes.mpg";
		ClientData.seek = false;
		
		//initialize static window reference
		ClientData.vid_comp = new VideoComponent();
		ClientData.windowSink = ClientData.vid_comp.getElement();
		
		ClientPipelineManager.modify_pipeline();
		
		SwingUtilities.invokeLater(new Runnable() 
		{ 
			public void run() 
			{	    		
				//create the control panel
				ClientData.controls = ClientGUIManager.createControlPanel();
				
				//create encoding options panel
				JPanel encOptions = ClientGUIManager.createEncodingOptionsPanel();
				
				//Actual top level widget
				ClientData.frame = new JFrame("vPlayer"); 
				ClientData.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	            
				ClientData.frame.add(ClientData.controls, BorderLayout.SOUTH);
				ClientData.frame.add(encOptions, BorderLayout.EAST);
				ClientData.frame.getContentPane().add(ClientData.vid_comp, BorderLayout.CENTER);
				ClientData.vid_comp.setPreferredSize(new Dimension(640, 480)); 
				ClientData.frame.setSize(1080, 920);
				ClientData.frame.setVisible(true);
	        } 
	    });
		
		//pipe.setState(State.PLAYING);
		//Gst.main();
		//while(true);
	}
}
