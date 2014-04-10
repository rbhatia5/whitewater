package vClient;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.gstreamer.Bus;
import org.gstreamer.Format;
import org.gstreamer.Message;
import org.gstreamer.MessageType;
import org.gstreamer.Pad;
import org.gstreamer.SeekType;
import org.gstreamer.State;
import org.gstreamer.StateChangeReturn;
import org.gstreamer.Structure;
import org.gstreamer.event.FlushStartEvent;
import org.gstreamer.event.FlushStopEvent;
import org.gstreamer.lowlevel.*;

import vClient.ClientData.Mode;
import vServer.ServerData;

public class ClientGUIManager {
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	public static String adjustProperties()
	{
		
		String resourcesFR = "";
		String resourcesWidth = "";
		String resourcesHeight = "";

		try {
			resourcesFR = ClientData.resourcesReader.readLine();
			resourcesWidth = ClientData.resourcesReader.readLine();
			resourcesHeight = ClientData.resourcesReader.readLine();
			//ClientData.resourcesReader.reset();
		} catch (IOException e) {
			System.err.println("Could not read from resources file");
		}
		if(Integer.parseInt(resourcesFR) < Integer.parseInt(ClientData.frameRate))
			ClientData.frameRate = resourcesFR;
		String resolution[] = ClientData.resolution.split("x");
		if(Integer.parseInt(resourcesWidth) < Integer.parseInt(resolution[0]))
			resolution[0] = resourcesWidth;
		if(Integer.parseInt(resourcesFR) < Integer.parseInt(resolution[1]))
			resolution[1] = resourcesHeight;
		
		ClientData.resolution = resolution[0] + "x" + resolution[1];
		
		String properties = ClientData.frameRate + " " + resolution[0] + " " + resolution[1];
		return properties;
	}
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	public static String negotiateProperties(String properties)
	{		
		ClientGUIManager.sendServerMessage(properties);
		while(!ClientData.mainThread.interrupted());
		
		if(!ClientData.serverResponse.equals(properties))
		{
			System.out.println("Negotiation Failed: Server cannot facilitate request. Modify properties to " + ClientData.serverResponse);
		}
		else
		{
			System.out.println("Negotiation Successful: Setting properties to " + ClientData.serverResponse);
		}
		return ClientData.serverResponse;
	}
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	public static void sendServerMessage(String message)
	{
		TCPClient client = new TCPClient(message);
		Thread clientThread = new Thread(client);
		clientThread.start();
	}
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static JPanel createControlPanel()
	{
		JButton negotiateButton = new JButton("Negotiate");
		negotiateButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Negotiating with Server");
				String properties = ClientGUIManager.adjustProperties();
				properties = negotiateProperties(properties);
				ClientPipelineManager.modify_pipeline();
			}					
		});
		
		JButton playButton = new JButton("Play");
		playButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Setting state to playing");
				ClientData.pipe.setState(State.PLAYING);
				//ClientData.appSink.setState(State.PLAYING);
				ClientData.rate = 1;
				
				sendServerMessage("play");
			}					
		});
		
		//pause button
		JButton pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Setting state to paused");
				ClientData.pipe.setState(State.PAUSED);
				
				sendServerMessage("pause");
			}					
		});
		
		//stop button
		JButton stopButton = new JButton("Stop");
		stopButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Setting state to ready");
				ClientData.pipe.setState(State.READY);
				
				sendServerMessage("stop");
			}					
		});
		
		//fast forward
		JButton fastForwardButton = new JButton("Fastforward");
		fastForwardButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(ClientData.rate < 0)
					ClientData.rate = 0;
				ClientData.rate *= 2;
				ClientData.pipe.seek(ClientData.rate, Format.TIME, 0, SeekType.NONE, ClientData.position/1000000000, SeekType.NONE, ClientData.duration/1000000000);
				
				sendServerMessage("fastforward");
			}
		});
		
		//rewind
		JButton rewindButton = new JButton("Rewind");
		rewindButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(ClientData.rate > 0)
					ClientData.rate = 0;
				ClientData.rate -= 2;
				ClientData.pipe.seek(ClientData.rate, Format.TIME, 0, SeekType.NONE, ClientData.position/1000000000, SeekType.NONE, ClientData.duration/1000000000);
				sendServerMessage("rewind");
			}
		});
		

		ClientData.duration = 51;
		//System.out.println((int)(ClientData.duration/1000000000));
		ClientData.slider = new JSlider(0,(int)(ClientData.duration/1000000000),0);
		ClientData.slider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				//when state changes, get the new position
				if(ClientData.seek)
				{
					//move stream to that position
					//System.out.println(ClientData.slider.getValue());
					ClientData.pipe.seek(ClientData.slider.getValue(), TimeUnit.SECONDS);
					ClientData.seek = false;
				}
			}
		});
		ClientData.slider.addMouseListener(new MouseListener()
				{
					public void mouseClicked(MouseEvent e) {
						ClientData.seek = true;
					}

					public void mousePressed(MouseEvent e) {

					}

					public void mouseReleased(MouseEvent e) {
						ClientData.seek = true;
					}

					public void mouseEntered(MouseEvent e) {

					}

					public void mouseExited(MouseEvent e) {
					}
					
				});
		
		JPanel controls = new JPanel();
		controls.add(negotiateButton);
		controls.add(playButton);
		controls.add(pauseButton);
		controls.add(stopButton);
		controls.add(fastForwardButton);
		controls.add(rewindButton);
		controls.add(ClientData.slider);
		
		//add buttons to list
		ClientData.controlButtons.add(negotiateButton);
		ClientData.controlButtons.add(playButton);
		ClientData.controlButtons.add(pauseButton);
		ClientData.controlButtons.add(stopButton);
		ClientData.controlButtons.add(fastForwardButton);
		ClientData.controlButtons.add(rewindButton);
		
		//define layout
		GroupLayout layout = new GroupLayout(controls);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		layout.setHorizontalGroup(
				layout.createParallelGroup()
					.addGroup(layout.createSequentialGroup()
						.addComponent(rewindButton)
						.addComponent(playButton)
						.addComponent(pauseButton)
						.addComponent(fastForwardButton)
						.addComponent(stopButton)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
				         GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				        .addComponent(negotiateButton)
					.addComponent(ClientData.slider)
					)
		);				
		layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup()
						.addComponent(rewindButton)
					    .addComponent(playButton)
					    .addComponent(pauseButton)
					    .addComponent(fastForwardButton)
					    .addComponent(stopButton)
					    .addComponent(negotiateButton)
				    .addComponent(ClientData.slider)
					)
		);
		controls.setLayout(layout);
		return controls;
	}
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static JPanel createUserOptionsPanel()	
	{
		String[] resList = {"320x240", "640x480", "960x720", "1280x1080"};
		String[] frList = {"10", "15", "20", "30"};
		
		//frame rate list picker
		JComboBox resCB = new JComboBox(resList);
		resCB.setPreferredSize(new Dimension(100,50));
		resCB.setSelectedIndex(0);
		resCB.setPreferredSize(new Dimension(70,30));
		resCB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JComboBox source = (JComboBox)e.getSource();
				String selected = (String)source.getSelectedItem();
				if(selected.equals("320x240"))
					ClientData.resolution = "320x240";
				else if(selected.equals("640x480"))
					ClientData.resolution = "640x480";
				else if(selected.equals("960x720"))
					ClientData.resolution = "960x720";
				else if(selected.equals("1280x1080"))
					ClientData.resolution = "1280x1080";
			}
		});
		//resolution list picker
		JComboBox frCB = new JComboBox(frList);
		frCB.setPreferredSize(new Dimension(10,10));
		frCB.setSelectedIndex(0);
		frCB.setPreferredSize(new Dimension(70,30));
		frCB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JComboBox source = (JComboBox)e.getSource();
				String selected = (String)source.getSelectedItem();
				if(selected.equals("10"))
					ClientData.frameRate = "10";
				else if(selected.equals("15"))
					ClientData.frameRate = "15";
				else if(selected.equals("20"))
					ClientData.frameRate = "20";
				else if(selected.equals("30"))
					ClientData.frameRate = "30";
			}
		});
		
		JPanel userOptions = new JPanel();
		userOptions.setPreferredSize(new Dimension(100,150));
		userOptions.add(resCB);
		userOptions.add(frCB);
		
		GroupLayout userOptionsLayout = new GroupLayout(userOptions);
		userOptionsLayout.setAutoCreateGaps(true);
		userOptionsLayout.setAutoCreateContainerGaps(true);
		
		userOptionsLayout.setHorizontalGroup(
			userOptionsLayout.createParallelGroup()
				.addComponent(resCB)
				.addComponent(frCB)
		);
		userOptionsLayout.setVerticalGroup(
			userOptionsLayout.createSequentialGroup()
				.addComponent(resCB)
				.addComponent(frCB)
		);
		userOptions.setLayout(userOptionsLayout);
		
		return userOptions;
	}
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static JPanel createEncodingOptionsPanel()
	{
		//create user options panel
		JPanel userOptions = createUserOptionsPanel();	
		
		//Monitor data
		ClientData.monitor = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(ClientData.monitor);
		ClientData.monitor.setEditable(false);
		
		JPanel encOptions = new JPanel();

		encOptions.add(userOptions);
		
		//define layout
		GroupLayout encLayout = new GroupLayout(encOptions);
		encLayout.setAutoCreateGaps(true);
		encLayout.setAutoCreateContainerGaps(true);
		
		encLayout.setHorizontalGroup(
				encLayout.createParallelGroup()
					.addGroup(encLayout.createSequentialGroup()						
					.addComponent(scrollPane)
					)
					.addGroup(encLayout.createSequentialGroup()						
					.addComponent(userOptions)
					)
		);
		encLayout.setVerticalGroup(
				encLayout.createSequentialGroup()		
					.addGroup(encLayout.createParallelGroup()						
					.addComponent(scrollPane)
					)
					.addGroup(encLayout.createParallelGroup()						
					.addComponent(userOptions)
					)
		);
		encOptions.setLayout(encLayout);
		return encOptions;
	}
}
