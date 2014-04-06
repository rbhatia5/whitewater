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

//import vServer.*;

public class vServerManager {
	
	public static void initializeTCPServer()
	{
		TCPServer server = new TCPServer();
		new Thread(server).start();
	}
	
	public static void main(String[] args)
	{	
		ServerData.mainThread = Thread.currentThread();
		ServerData.state = ServerData.State.NEGOTIATING;
		
		initializeTCPServer();
		
		while(!ServerData.mainThread.interrupted());
		
		args = Gst.init("Server Pipeline", args);
		
		ServerData.mode = ServerData.mode.SERVER;
		
		ServerPipelineManager.modify_pipeline();
		
		ServerData.pipe.setState(State.PLAYING);
		
		Gst.main();
	}
}
