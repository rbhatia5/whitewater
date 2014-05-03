package vClient;

import vNetwork.Message;

import java.io.*;
import java.net.*;

import org.json.JSONException;



public class TCPcom implements Runnable {
	
	private Message outgoing;
	private Message incoming;
	private String ipAddress;
	private int port;
	
	
	
	//Outgoing message
	public Message getOutgoing() {
		return outgoing;
	}
	public void setOutgoing(Message outgoing) {
		this.outgoing = outgoing;
	}
	
	//Incoming message
	public Message getIncoming() {
		return incoming;
	}
	public void setIncoming(Message incoming) {
		this.incoming = incoming;
	}
	
	//IP adddress
		public String getIpAddress() {
			return ipAddress;
		}
	
	//port
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	//Main Thread
	public Thread getMainThread() {
		return Thread.currentThread();
	}

	
	//Destroy the thread
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
}
