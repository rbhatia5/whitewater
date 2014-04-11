package vServer;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

import org.gstreamer.*;
import org.gstreamer.swing.*;
import org.gstreamer.elements.*;
import org.gstreamer.elements.good.RTPBin;

import vClient.ClientData;

//import vServer.*;

public class vServerManager {
	
	public final static int comPort = 5001;
	public final static int videoRTP = 5002;
	public final static int videoRTCPin = 5003;
	public final static int videoRTCPout = 5004;
	
	
	public static void initializeTCPServer()
	{
		TCPServer server = new TCPServer();
		new Thread(server).start();
	}
	
	public static void main(String[] args)
	{	
		ServerData.mainThread = Thread.currentThread();
		ServerData.state = ServerData.State.NEGOTIATING;
		
		File resources = new File("server-resources.txt");
		try {
			ServerData.resourcesReader = new BufferedReader(new FileReader(resources));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	
		
		initializeTCPServer();
		
		while(!ServerData.mainThread.interrupted());
		
		args = Gst.init("Server Pipeline", args);
		
		ServerData.mode = ServerData.Mode.SERVER;
		ServerData.width = 352;
		ServerData.height = 288;
		ServerData.framerate = 30;
		
		ServerPipelineManager.modify_pipeline();
		
		ServerData.pipe.setState(State.PLAYING);
		
		Gst.main();
	}
}
