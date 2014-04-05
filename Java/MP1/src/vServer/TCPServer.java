package vServer;

import java.io.*;
import java.net.*;


public class TCPServer implements Runnable{

	ServerSocket serverSocket = null;
	Socket socket = null;
	BufferedReader readFromClient = null;
	BufferedReader readFromServer = null;
	PrintWriter writeToClient = null;
	String message = null;

	public TCPServer() {
		try {
			serverSocket = new ServerSocket(5002);
			System.out.println("Waiting for connection...");
			socket = serverSocket.accept();
			System.out.println("Connection accepted...");

			readFromClient = new BufferedReader(
					new InputStreamReader(
							socket.getInputStream()));
			writeToClient = new PrintWriter(
					socket.getOutputStream(), true);
			readFromServer = new BufferedReader(
					new InputStreamReader(
							System.in));

			new Thread(this).start();

//			while(true) {
//				message = readFromServer.readLine();
//				writeToClient.println(message);
//				writeToClient.flush();
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
//				message = readFromClient.readLine();
//				
//			}
//		} catch(Exception exp) {
//			exp.printStackTrace();
//		}
	}
	
	public void push(String msg) {
		writeToClient.println(msg);
		System.out.println(msg);
		writeToClient.flush();
		
	}
	
	public String waitForResponse()
	{
		while(message == null) {
			try {
				message = readFromClient.readLine();
				
				
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
