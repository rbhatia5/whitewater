package vClient;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;



/*Client Side resource Singleton!
 * Used to manage bandwidth resouces on the client
 * Implements functionality to set,get,write,read resources
 * Will be extended to allow resource validation
 */

public class ClientResource {

	private static ClientResource instance = null; 
	protected int bandwidth;
	protected String filepath;
	
	protected ClientResource()
	{
		
	}
	public static ClientResource getInstance()
	{
		if(instance == null)
			instance = new ClientResource();
		
		return instance;
	}
	
	public void setResourcePath(String fname)
	{
		filepath = fname;
	}
	
	public int getBandwidth()
	{
		return bandwidth; 
	}
	
	public void setBandwidth(int newband)
	{
		if(newband >= 0)
			bandwidth = newband; 
		writeToFile();
	}
	
	public boolean isValid()
	{
		if(bandwidth != -1)
			return true;
		else
			return false;
	}
	
	public int saveToFile()
	{
		if(filepath == null) {
			return -1;
		}
		try {
			File resources = new File(filepath);
			BufferedWriter writer = new BufferedWriter(new FileWriter(resources));
			writer.write(String.valueOf(bandwidth));
	
			
		}catch (FileNotFoundException e)
		{
			//e.printStackTrace();
			System.err.println("Resource File not found for writing");
			return -1;
		}
		catch (IOException e)
		{
			System.err.println("Resource File IOException when writing");
			return -1;
		}
		return 0;
	}
	
	public void readFromFile()
	{
		File resources = new File(filepath);
		int savedValue = 0; 
		try {
			Scanner s = new Scanner(resources);
			if(s.hasNextInt())
			{
				savedValue = s.nextInt();
				if(savedValue >= 0)
					bandwidth = savedValue;
				else
					bandwidth = -1; 
			}
			else
				bandwidth = -1; 
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			bandwidth = -1; 
		} 
	}
	
	public void writeToFile()
	{
		File resources = new File(filepath);
		BufferedWriter wtr; 
		try {
			wtr = new BufferedWriter(new FileWriter(resources));
			wtr.write(Integer.toString(getBandwidth()));
			wtr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			bandwidth = -1; 
		} catch (IOException e)
		{
			e.printStackTrace();
			bandwidth = -1;
		}
	}

	
	public boolean checkForResource(int requestedBand)
	{
		if(requestedBand <= getBandwidth())
			return true;
		else
			return false;
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
	
	
}
