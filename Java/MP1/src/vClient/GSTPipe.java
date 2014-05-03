package vClient;
import java.io.*;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.gstreamer.Pipeline;

public class GSTPipe implements Runnable {
	
	private String ipAddress;
	private MediaCom audioPort;
	private MediaCom videoPort;
	private Pipeline pipe;
	private String senderAddress;
	
	
		//Audioport
		public MediaCom getAudioPort() {
			return audioPort;
		}
		public void setAudioPort(MediaCom audioPort) {
			this.audioPort = audioPort;
		}
		
		//Videoport
		public MediaCom getVideoPort() {
			return videoPort;
		}
		public void setVideoPort(MediaCom videoPort) {
			this.videoPort = videoPort;
		}
		
		//Pipeline
		public Pipeline getPipe() {
			return pipe;
		}
		public void setPipe(Pipeline pipe) {
			this.pipe = pipe;
		}
		
		//Mainthread
		public Thread getMainThread() {
			return Thread.currentThread();
		}
	
		
		//SenderAddress
		public String getSenderAddress() {
			return senderAddress;
		}
		
		//IP adddress
		public String getIpAddress() {
			return ipAddress;
		}
		
		public void destroy(){
			
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			
		}
}
