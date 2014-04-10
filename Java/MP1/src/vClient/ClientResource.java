package vClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;



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
	}
	
	public boolean isValid()
	{
		if(bandwidth != -1)
			return true;
		else
			return false;
	}
	public void saveToFile()
	{
		
	}
	
	public void readFromFile()
	{
		File resources = new File(filepath);
		BufferedReader rdr; 
		int savedValue = 0; 
		try {
			rdr = new BufferedReader(new FileReader(resources));
			savedValue = rdr.read(); 
			if(savedValue >= 0)
				bandwidth = savedValue;
			else
				bandwidth = -1; 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			bandwidth = -1; 
		} catch (IOException e)
		{
			e.printStackTrace();
			bandwidth = -1;
		}
	}
	
	public void initWithFile(String fname)
	{
		this.setResourcePath("client-resouces.txt");
		this.readFromFile();
	}
	
	
}
