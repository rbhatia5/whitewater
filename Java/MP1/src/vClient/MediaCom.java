package vClient;

public class MediaCom {
	private int RTP;
	private int RTCPout;
	private int RTCPin;
	
	MediaCom(int rTP,int rTCPin,int rTCPout){
		RTP = rTP;
		RTCPout = rTCPout;
		RTCPin = rTCPin;
	}
	
	//RTP
	public int getRTP() {
		return RTP;
	}
	public void setRTP(int rTP) {
		RTP = rTP;
	}
	
	//RTPout
	public int getRTCPout() {
		return RTCPout;
	}
	public void setRTCPout(int rTCPout) {
		RTCPout = rTCPout;
	}
	
	//RTPin
	public int getRTCPin() {
		return RTCPin;
	}
	public void setRTCPin(int rTCPin) {
		RTCPin = rTCPin;
	}
	
}
