package vNetwork;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {

	//Key Names
	private final static String SENDER_KEY = "sender";
	private final static String MESSAGE_TYPE_KEY = "message-type";
	private final static String DATA_KEY = "data";
	
	//Stream Request Keys
	public final static String FRAMERATE_KEY = "frame-rate";
	public final static String FRAME_WIDTH_KEY = "frame-width";
	public final static String FRAME_HEIGHT_KEY = "frame-height";
	
	//Control Request Keys
	public final static String ACTION_KEY = "action";
	public final static String PLAY_ACTION = "play";
	public final static String PAUSE_ACTION = "pause";
	public final static String REWIND_ACTION = "rw";
	public final static String FAST_FORWARD_ACTION = "ff";
	public final static String STOP_ACTION = "stop";
	public static final String RESULT_KEY = "result";
	public static final String RESULT_ACCEPT_VALUE = "accept";
	public static final String RESULT_REJECT_VALUE = "reject";
	public static final String CLIENT_IP_ADDRESS_KEY = "ip-address";
	public static final String ACTIVITY_KEY = "activity";
	public static final String ACTIVITY_ACTIVE_VALUE = "active";
	public static final String ACTIVITY_PASSIVE_VALUE = "passive";
	public final static String PORT_REQUEST_KEY = "port";
	public final static String POSITION_KEY = "position";
	
	public enum MessageType {
		NEW, REQUEST, CONTROL, RESPONSE, NULL
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
	
	public Message(MessageType type)
	{
		sender = new String();
		mType = type;
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
			if(message == null)
				return  new Message();
			Message ret = new Message();
			JSONObject msgObj = new JSONObject(message);
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
		else if(newType.equals("NEW"))
		{
			setType(MessageType.NEW);
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
		if(key != null && value != null)	
			data.put(key, value);
	}
	
	public Object getData(String key) throws JSONException
	{
		return data.get(key);
	}
	
}
