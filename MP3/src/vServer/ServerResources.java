package vServer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import vSession.Resources;

public class ServerResources extends Resources{
	
	private static ServerResources instance = null;
	
	public int bandwidthAvailable;
	protected String filepath;

	final int minQoS = 15;
	final int desQoS = 30;
	
	public ServerResources(){
		
	}
	
	public static ServerResources getInstance()
	{
		if(instance == null)
			instance = new ServerResources();
		
		return instance;
	}
	
	
	public int getBandwidth() {
		
		readFromFile();
		
		return bandwidthAvailable;
	}
	
	
	
	public void readFromFile()
	{
		File serverResources = new File(filepath);
		int savedValue = 0; 
		try {
			Scanner s = new Scanner(serverResources);
			if(s.hasNextInt())
			{
				savedValue = s.nextInt();
				if(savedValue >= 0)
					bandwidthAvailable = savedValue;
				else
					bandwidthAvailable = -1; 
			}
			else
				bandwidthAvailable = -1; 
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			bandwidthAvailable = -1; 
		} 
	}
	
	public void writeToFile()
	{
		File serverResources = new File(filepath);
		BufferedWriter wtr; 
		try {
			wtr = new BufferedWriter(new FileWriter(serverResources));
			wtr.write(Integer.toString(getBandwidth()));
			wtr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			bandwidthAvailable = -1; 
		} catch (IOException e)
		{
			e.printStackTrace();
			bandwidthAvailable = -1;
		}
		
	}
	
	public void adjustResources(int requestedBand){
		int remainingBandwidth = this.getBandwidth() - requestedBand;
		setBandwidth(remainingBandwidth);
		writeToFile();
	}
	
	public void initWithFile(String fname)
	{
		this.setResourcePath(fname);
		this.readFromFile();
	}
	
	public void setResourcePath(String fname)
	{
		filepath = fname;
	}

	public void setBandwidth(int bw) {
		this.bandwidthAvailable = bw;
	}
	
}
