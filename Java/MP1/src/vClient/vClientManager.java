package vClient;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import javax.swing.*;

import org.gstreamer.*;
import org.gstreamer.swing.*;

public class vClientManager {
	
	public static void main(String[] args)
	{
		//Set the client state
		ClientData.state = ClientData.State.NEGOTIATING;
		
		// create teh client resource singleton
		ClientResource res = ClientResource.getInstance();
		res.initWithFile("client-resources.txt");
		//System.out.println("client has " + res.getBandwidth() + " resources");
		
		
		args = Gst.init("Client Pipeline", args);
		
		ClientData.mode = ClientData.Mode.PASSIVE;
		
		ClientData.FrameRes.setRes("320x240");
		ClientData.frameRate = 10;
		ClientData.seek = false;
		
		try {
			ClientData.ipAddress = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//initialize static window reference
		ClientData.vid_comp = new VideoComponent();
		ClientData.windowSink = ClientData.vid_comp.getElement();
		
		
		SwingUtilities.invokeLater(new Runnable() 
		{ 
			public void run() 
			{	    		
				ClientData.mainThread = Thread.currentThread();
				//create the control panel
				ClientData.controls = ClientGUIManager.createControlPanel();
				
				//create encoding options panel
				JPanel encOptions = ClientGUIManager.createEncodingOptionsPanel();
				
				//ClientData.frame.set
				
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
	}
}
