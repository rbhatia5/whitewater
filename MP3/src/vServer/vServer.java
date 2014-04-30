package vServer;

public class vServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		//Create thread to listen for new connections here!!

		ServerLoop serve = new ServerLoop(5000);
		
		serve.start();
		
	}

}
