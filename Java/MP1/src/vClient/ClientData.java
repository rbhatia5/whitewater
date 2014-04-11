package vClient;


import java.util.*;

import vClient.ClientData.Mode;
import vNetwork.Message;

import javax.swing.*;

import org.gstreamer.*;
import org.gstreamer.elements.AppSink;
import org.gstreamer.elements.good.RTPBin;
import org.gstreamer.swing.VideoComponent;

public class ClientData {

	public enum Mode
	{
		ACTIVE, PASSIVE
	}
	
	public enum State {
		NEGOTIATING, STREAMING
	}
	
	protected static final int BYTES_PER_PIXEL = 3;



	public static final String serverAddress = "127.0.0.1";
	
	public static String getIpAddress() {
		return ipAddress;
	}
	public static void setIpAddress(String ipAddress) {
		ClientData.ipAddress = ipAddress;
	}
	protected static Thread mainThread;
	protected static State state;
	protected static Mode mode;
	
	protected static String serverResponse;
	protected static Message serverMessage;
	
	protected static Pipeline pipe;
	protected static AppSink RTCPSink;
	
	protected static RTPBin rtpBin;
	protected static Element windowSink;
	
	protected static JFrame frame;
	protected static VideoComponent vid_comp;
	protected static List<JButton> controlButtons = new ArrayList<JButton>(); 
	protected static ArrayList<JComponent> optionsComponents = new ArrayList<JComponent>();
	protected static JPanel controls;
	protected static JTextArea monitor;
	//protected static JSlider slider;
	
	protected static int frameRate;
	protected static String resolution;
	protected static boolean seek;
	protected static int rate;
	protected static long duration;
	protected static long position;
	protected static long timeStamp;
	protected static long encDecTime;
	protected static String ipAddress;
	
	
	
	
	
	
	
	
	
	
	
	public static class FrameRes {
		private static int width;
		private static int height;
		
		FrameRes(String res){
			FrameRes.setRes(res);
		}
		
		public static void setRes(String res){
			String params[] = res.split("x");
			width = Integer.parseInt(params[0]);
			height = Integer.parseInt(params[1]);
		}
		
		public static int getWidth() {
			return width;
		}
		
		public static void setWidth(int width) {
			FrameRes.width = width;
		}


		public static int getHeight() {
			return height;
		}
		
		public static void setHeight(int height) {
			FrameRes.height = height;
		}
		
		public static int getFrameSize() {
			return width*height;
		}
		
	}
	
	//Main Thread
	public static Thread getMainThread() {
		return mainThread;
	}
	public static void setMainThread(Thread mainThread) {
		ClientData.mainThread = mainThread;
	}
	
	//State
	public static State getState() {
		return state;
	}
	public static void setState(State state) {
		ClientData.state = state;
	}
	
	//serverResponse
	public static String getServerResponse() {
		return serverResponse;
	}
	public static void setServerResponse(String serverResponse) {
		ClientData.serverResponse = serverResponse;
	}
	
	//Pipe
	public static Pipeline getPipe() {
		return pipe;
	}
	public static void setPipe(Pipeline pipe) {
		ClientData.pipe = pipe;
	}
	
	//Appsink
	public static AppSink getRTCPSink() {
		return RTCPSink;
	}
	public static void setRTCPSink(AppSink rTCPSink) {
		RTCPSink = rTCPSink;
	}
	
	//RTPBin
	public static RTPBin getRtpBin() {
		return rtpBin;
	}
	public static void setRtpBin(RTPBin rtpBin) {
		ClientData.rtpBin = rtpBin;
	}
	
	//WindowSink
	public static Element getWindowSink() {
		return windowSink;
	}
	public static void setWindowSink(Element windowSink) {
		ClientData.windowSink = windowSink;
	}
	
	//JFrame
	public static JFrame getFrame() {
		return frame;
	}
	public static void setFrame(JFrame frame) {
		ClientData.frame = frame;
	}
	
	//Mode
	public static Mode getMode() {
		return mode;
	}
	public static void setMode(Mode mode) {
		ClientData.mode = mode;
		if(mode == Mode.ACTIVE)
		{
			
			// Set framerate to be modifiable
			JComboBox frCB = (JComboBox)ClientData.optionsComponents.get(2);
			frCB.setEnabled(true);
			String[] array = new String[11];
			
			for(int i  = 15; i <= 25; i++)
				array[i-15] = Integer.toString(i);
		
			DefaultComboBoxModel dmodel = new DefaultComboBoxModel(array);
			
			frCB.setModel(dmodel);
			frCB.setSelectedIndex(0);
		
		}
		else
		{
			//set framerate to be 10
			JComboBox frCB = (JComboBox)ClientData.optionsComponents.get(2);
			String[] array = {"10"};
			DefaultComboBoxModel dmodel = new DefaultComboBoxModel(array);
			frCB.setModel(dmodel);
			frCB.setSelectedIndex(0);
			frCB.setEnabled(false);
		}
		
	}
	
	//Videocomp
	public static VideoComponent getVid_comp() {
		return vid_comp;
	}
	public static void setVid_comp(VideoComponent vid_comp) {
		ClientData.vid_comp = vid_comp;
	}
	
	//FrameRate
	public static int getFrameRate() {
		return frameRate;
	}
	public static void setFrameRate(int frameRate) {
		ClientData.frameRate = frameRate;
	}
	
	//Resolution
	public static String getResolution() {
		return resolution;
	}
	public static void setResolution(String resolution) {
		ClientData.resolution = resolution;
		ClientData.FrameRes.setRes(resolution);
	}
	
	public static int getProposedBandwidth() {
		return BYTES_PER_PIXEL*FrameRes.getFrameSize()*getFrameRate();
	}
	
	//ControlButton
	public static List<JButton> getControlButtons() {
		return controlButtons;
	}
	public static void setControlButtons(List<JButton> controlButtons) {
		ClientData.controlButtons = controlButtons;
	}
	
	//Control
	public static JPanel getControls() {
		return controls;
	}
	public static void setControls(JPanel controls) {
		ClientData.controls = controls;
	}
	
	//Monitor
	public static JTextArea getMonitor() {
		return monitor;
	}
	public static void setMonitor(JTextArea monitor) {
		ClientData.monitor = monitor;
	}
	
	//Slider
//	public static JSlider getSlider() {
//		return slider;
//	}
//	public static void setSlider(JSlider slider) {
//		ClientData.slider = slider;
//	}
	
	//Seek
	public static boolean isSeek() {
		return seek;
	}
	public static void setSeek(boolean seek) {
		ClientData.seek = seek;
	}
	
	//Rate
	public static int getRate() {
		return rate;
	}
	public static void setRate(int rate) {
		ClientData.rate = rate;
	}
	
	//Duration
	public static long getDuration() {
		return duration;
	}
	public static void setDuration(long duration) {
		ClientData.duration = duration;
	}
	
	//Get Position
	public static long getPosition() {
		return position;
	}
	public static void setPosition(long position) {
		ClientData.position = position;
	}
	
	//GetTimeStamp
	public static long getTimeStamp() {
		return timeStamp;
	}
	public static void setTimeStamp(long timeStamp) {
		ClientData.timeStamp = timeStamp;
	}
	
	//EncDecTime
	public static long getEncDecTime() {
		return encDecTime;
	}
	public static void setEncDecTime(long encDecTime) {
		ClientData.encDecTime = encDecTime;
	}
	


}

