package vNetwork;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.json.JSONException;

public class MessageTest {

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException {
		// TODO Auto-generated method stub
		
		Message test = new Message();
		test.setType(Message.MessageType.REQUEST);
		test.setSender("SV00");
		
		try {
		test.addData("method", "play");
		test.addData("file", "filename");
		
		System.out.println("Input Object: \n" + test.stringify());
		
		}catch(JSONException e)
		{
			e.printStackTrace();
			System.err.println("JSON FAILED");
		}
		
		
		try {
			Message received = Message.destringify(test.stringify());
			System.out.println("Output Object: \n" + received.stringify());
			
			System.out.println(Inet4Address.getLocalHost().getHostAddress());
			
		} catch (JSONException e) {
			
			e.printStackTrace();
			
			System.err.println("JSON FAILED");
		}
		

	}

}
