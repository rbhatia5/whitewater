package vClient;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.gstreamer.Element;
import org.gstreamer.Pipeline;
import org.gstreamer.elements.AppSink;
import org.gstreamer.elements.good.RTPBin;
import org.gstreamer.swing.VideoComponent;

import vClient.ClientData.Mode;
import vClient.ClientData.State;
import vNetwork.Message;

public class Member {
	
	
	private String ipAddress;
	private String senderAddress;
	private Thread mainThread;
	private Thread gstThread;
	private Thread listenerThread;
	private GSTPipe gstPipe;
	private TCPcom tcpCom;
	
	private int bandwidth;
	
	public Member(){
		this.setIpAddress();
		this.gstPipe = new GSTPipe();
		this.tcpCom = new TCPcom();
		this.gstThread = new Thread(gstPipe);
		this.listenerThread = new Thread(tcpCom);
		
	}
	
	public Member(int bandwidth){
		this.setIpAddress();
		this.gstPipe = new GSTPipe();
		this.tcpCom = new TCPcom();
		this.setGstThread();
		this.setListenerThread();
		this.bandwidth = bandwidth;
		
	}
	public Member(String senderAddress){
		this.setIpAddress();
		this.senderAddress = senderAddress;
		this.gstPipe = new GSTPipe();
		this.tcpCom = new TCPcom();
		this.setGstThread();
		this.setListenerThread();
	}
	
	
	public void destroyThread(){
		this.gstPipe.destroy();
		this.tcpCom.destroy();
	}
	
	
	//gstPipe
	public GSTPipe getGstPipe() {
		return gstPipe;
	}
	public void setGstPipe(GSTPipe gstPipe) {
		this.gstPipe = gstPipe;
	}
	
	//Mainthread
	public Thread getMainThread() {
		return mainThread;
	}
	public void setMainThread(Thread mainThread) {
		this.mainThread = Thread.currentThread();
	}
	
	//gstThread
	public Thread getGstThread() {
		
		return gstThread;
	}
	public void setGstThread() {
		this.gstThread = gstPipe.getMainThread();
	}
	
	//Listenerthread
	public Thread getListenerThread() {
		return listenerThread;
	}
	public void setListenerThread() {
		this.listenerThread = tcpCom.getMainThread();
	}
	

	
	
	//Bandwidth
	public int getBandwidth() {
		return bandwidth;
	}
	public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}
	
	//SenderAddress
	public String getSenderAddress() {
		return senderAddress;
	}
	public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
	}
	
	//IP adddress
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress() {
		try {
			this.ipAddress = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	//TCPcom
	public TCPcom getTcpCom() {
		return tcpCom;
	}
	public void setTcpCom(TCPcom tcpCom) {
		this.tcpCom = tcpCom;
	}

	
}
