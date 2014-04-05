package vClient;

import java.io.*;
import java.net.*;

public class TCPClient implements Runnable {

	Socket socket = null;
	BufferedReader readFromClient = null;
	BufferedReader readFromServer = null;
	PrintWriter writeToServer = null;
	String message = null;

	public TCPClient(Socket socket) {
		try {
			this.socket = socket;
			readFromServer = new BufferedReader(
					new InputStreamReader(
							socket.getInputStream()));
			writeToServer = new PrintWriter(
					socket.getOutputStream(), true);
			readFromClient = new BufferedReader(
					new InputStreamReader(System.in));

			new Thread(this).start();

//			while(true) {
//				message = readFromClient.readLine();
//				writeToServer.println(message);
//				writeToServer.flush();
//				if(message.equalsIgnoreCase("exit")) {
//					System.exit(0);
//				}
//			}
		} catch(IOException exp) {
			exp.printStackTrace();
		}
	}


	public void run() {
//		try {
//			while(true) {
//				message = readFromServer.readLine();
//			}
//		} catch(Exception exp) {
//			exp.printStackTrace();
//		}
	}
	
	public void push(String msg) {
		writeToServer.println(msg);
		writeToServer.flush();
		
	}
	
	public String waitForResponse()
	{
		while(message == null) {
			try {
				message = readFromServer.readLine();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(message);
		String resp = new String(message); 
		message = null;
		return resp;
	}
}

