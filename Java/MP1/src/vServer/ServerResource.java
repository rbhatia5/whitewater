/**
 * 
 */
package vServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;



/**
 * @author kdperei2
 *
 */
public class ServerResource {
	private static ServerResource instance = null; 
	protected int bandwidth;
	protected String filepath;
	
	protected ServerResource(){
		
	}
	
	public static ServerResource getInstance()
	{
		if(instance == null)
			instance = new ServerResource();
		
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
	
	public int saveToFile()
	{
		if(filepath == null) {
			return -1;
		}
		try {
			File serverResources = new File(filepath);
			BufferedWriter writer = new BufferedWriter(new FileWriter(serverResources));
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
		File serverResources = new File(filepath);
		int savedValue = 0; 
		try {
			Scanner s = new Scanner(serverResources);
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
		File serverResources = new File(filepath);
		BufferedWriter wtr; 
		try {
			wtr = new BufferedWriter(new FileWriter(serverResources));
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
