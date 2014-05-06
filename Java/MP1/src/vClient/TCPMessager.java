package vClient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.JSONException;

import vNetwork.Message;
import vNetwork.Message.MessageType;

public class TCPMessager implements Runnable {
		
		protected boolean quit;
		protected int comPort = 5114;
		protected ServerSocket socket;
		protected Socket connectionSocket;
		protected BufferedReader inFromClient;
		protected DataOutputStream outToClient;
		protected Message msg;
		protected String chatMsg;
		
		TCPMessager(int port)
		{
			quit = false;
			comPort = port;
		}

		public void run() 
		{
			try {
				initializeSocket();
				while(true)
				{
					System.out.println("MESSAGER: Connecting to socket port localhost:" + comPort);
					connectionSocket = socket.accept();
					
					System.out.printf("MESSAGER: Connected to %s\n", connectionSocket.toString());
					inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
					outToClient = new DataOutputStream(connectionSocket.getOutputStream());
					System.out.println("MESSAGER: Reading from socket");
					String clientCommand = inFromClient.readLine();
					System.out.println("MESSAGER received Message: " + clientCommand);
					msg = Message.destringify(clientCommand);
					
					if(msg.getType().equals(Message.MessageType.COMM))
					{
						if(msg.getData(Message.COMM_KEY).equals(Message.COMM_CONNECT_VALUE))
						{
							//pop up to accept connection
							
						}
						else
						{
							chatMsg = (String) msg.getData(Message.CHAT_KEY);
						}
						Message response = new Message();
						response.setSender("SV00"); 
						response.setType(MessageType.RESPONSE); 
						response.addData(Message.CHAT_KEY, "OK");
						System.out.println("MESSAGER: Writing " + response.stringify());
						outToClient.writeBytes(response.stringify() + "\n");					
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}	
		
		protected void initializeSocket() throws IOException
		{
			socket = null;
			System.out.println("MESSAGER: Initializing server socket");
			socket = new ServerSocket(comPort);
			System.out.printf("MESSAGER: Server socket initialized %s\n", socket.toString());
		}
		
		protected void listenForMessage() throws IOException, JSONException
		{
			System.out.println("MESSAGER: Connecting to socket port localhost:" + comPort);
			connectionSocket = socket.accept();
			System.out.printf("MESSAGER: Connected to %s\n", connectionSocket.toString());
			inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			System.out.println("MESSAGER: Reading from socket");
			//Message clientCommand = inFromClient.readLine();
			//String clientMessage = Message.destringify(clientCommand);
			//System.out.println("MESSAGER received Message: " + SM.data.clientMessage.stringify());
		}
}
