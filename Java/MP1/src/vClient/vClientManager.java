package vClient;

import java.awt.*;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import javax.swing.*;

import org.gstreamer.*;
import org.gstreamer.swing.*;
import vServer.*;

public class vClientManager {
	
	public static void main(String[] args)
	{
		ChatMessenger msgr = new ChatMessenger();
		Thread CMS = new Thread(msgr);
		CMS.start();
		
		RequestServer RS = new RequestServer();
		Thread RSThread = new Thread(RS);
		RSThread.start();
		
		ClientData.activeWindow = 0;
		ClientData.data[0] = new GstData();
		ClientData.data[1] = new GstData();
		ClientData.data[0].state = ClientData.State.REQUESTING;
		ClientData.data[1].state = ClientData.State.REQUESTING;
		ClientData.data[0].mode = ClientData.Mode.PASSIVE;
		ClientData.data[1].mode = ClientData.Mode.PASSIVE;
		
		
		ClientResource res = ClientResource.getInstance();
		res.initWithFile("client-resources.txt");
		
		args = Gst.init("Client Pipeline", args);
		
		ClientData.bounce = 0;
		ClientData.t = System.currentTimeMillis();
		ClientData.muted = false;		
		ClientData.FrameRes.setRes("320x240");
		ClientData.frameRate = 10;
		ClientData.seek = false;
		
		try {
			ClientData.ipAddress = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		ClientData.vidComp1 = new VideoComponent();
		ClientData.data[0].windowSink = ClientData.vidComp1.getElement();
		ClientData.vidComp2 = new VideoComponent();
		ClientData.data[1].windowSink = ClientData.vidComp2.getElement();
		
		SwingUtilities.invokeLater(new Runnable() 
		{ 
			public void run() 
			{	    		
				ClientData.mainThread = Thread.currentThread();
				//create the control panel
				ClientData.controls = ClientGUIManager.createControlPanel();
				
				//create encoding options panel
				JPanel encOptions = ClientGUIManager.createEncodingOptionsPanel();
				
				//create windows panel
				JPanel windows = ClientGUIManager.createWindowsPanel();
				
				//create window picker
				JPanel windowPicker = ClientGUIManager.createWindowPicker();
				
				//Actual top level widget
				ClientData.frame = new JFrame("vPlayer"); 
				ClientData.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	            
				ClientData.frame.add(ClientData.controls, BorderLayout.SOUTH);
				ClientData.frame.add(encOptions, BorderLayout.EAST);
				ClientData.frame.add(windows, BorderLayout.CENTER);
				ClientData.frame.add(windowPicker, BorderLayout.NORTH);
				
				ClientData.frame.setSize(1080, 920);
				ClientData.frame.setVisible(true);
	        } 
	    });
	}
}
