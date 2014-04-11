package vNetwork;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {

	private final static String SENDER_KEY = "sender";
	private final static String MESSAGE_TYPE_KEY = "message-type";
	private final static String DATA_KEY = "data";
	
	
	public enum MessageType {
		REQUEST, CONTROL, RESPONSE, NULL
	}
	
	private String sender; 
	// SV## or CL##
	
	private MessageType mType;
	
	private JSONObject data;
	
	public Message()
	{
		sender = new String();
		mType = Message.MessageType.NULL;
		data = new JSONObject();
	}

	public String stringify() throws JSONException
	{
		if(mType == Message.MessageType.NULL)
			return null;
		
		JSONObject messageObject;
	
			messageObject = new JSONObject();
			messageObject.put(SENDER_KEY, this.sender);
			messageObject.put(MESSAGE_TYPE_KEY, this.mType);
			messageObject.put(DATA_KEY, this.data);
			
			return messageObject.toString();
			
	}
	
	public static Message destringify(String message) throws JSONException
	{	

			Message ret = new Message();
			JSONObject msgObj = new JSONObject(message);
			
			System.out.println(msgObj);
			
			ret.setSender(msgObj.getString(SENDER_KEY));
			
			ret.setType(msgObj.getString(MESSAGE_TYPE_KEY));
			
			ret.setData(msgObj.getJSONObject(DATA_KEY));

			return ret;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public MessageType getType() {
		return mType;
	}

	public void setType(MessageType mType) {
		this.mType = mType;
	}
	public void setType(String newType)
	{
		
		if(newType.equals("REQUEST"))
		{		
			setType(MessageType.REQUEST);
		}
		else if(newType.equals("CONTROL"))
		{
			setType(MessageType.CONTROL);
		}
		else if(newType.equals("RESPONSE"))
		{
			setType(MessageType.RESPONSE);
		}
		else if(newType.equals("NULL"))
		{
			setType(MessageType.NULL);
		}
		else
			setType(MessageType.NULL);
	}

	public JSONObject getData() {
		return data;
	}

	public void setData(JSONObject data) {
		this.data = data;
	}
	
	public void addData(String key, Object value) throws JSONException
	{
		data.put(key, value);
	}
	
	public Object getData(String key) throws JSONException
	{
		return data.get(key);
	}
	
}
