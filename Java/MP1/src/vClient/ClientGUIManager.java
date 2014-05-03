package vClient;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import vNetwork.Message;
import vNetwork.Message.MessageType;

import org.gstreamer.State;
import org.json.JSONException;


public class ClientGUIManager {
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static JPanel createControlPanel()
	{
		JButton negotiateButton = new JButton("Connect");
		negotiateButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				
				if(ClientResource.getInstance().checkForResource(ClientData.getProposedBandwidth()))
				{
					//first request ports, then negotiate properties
					System.out.println("CLIENT: Requesting Server");
					ClientData.data[ClientData.activeWindow].comPort = 5000;
					Message portRequest = new Message();
					portRequest.setSender("CL??"); 
					portRequest.setType(MessageType.NEW);
					TCPClient.requestPort(portRequest);
					
					System.out.println("CLIENT: Negotiating with Server");
					Message streamRequest = new Message();
					try {
						streamRequest.setSender("CL??"); 
						streamRequest.setType(MessageType.REQUEST); 
						streamRequest.addData(Message.FRAMERATE_KEY, ClientData.frameRate);
						streamRequest.addData(Message.FRAME_WIDTH_KEY, ClientData.FrameRes.getWidth());
						streamRequest.addData(Message.FRAME_HEIGHT_KEY, ClientData.FrameRes.getHeight());
						streamRequest.addData(Message.CLIENT_IP_ADDRESS_KEY, ClientData.ipAddress);
						streamRequest.addData(Message.ACTIVITY_KEY, ClientData.data[ClientData.activeWindow].mode);
					}catch(JSONException j)
					{
						System.err.println("Could not build a stream request");
						return;
					}
					boolean result = TCPClient.negotiateProperties(streamRequest);
					
					if(result)
						ClientPipelineManager.modify_pipeline();
				}
				else {
					System.err.println("Client Resource Admission Failed");
				}
				
				
			}					
		});
		
		JButton playButton = new JButton("Play");
		playButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Setting state to playing");
				ClientData.data[ClientData.activeWindow].pipe.setState(State.PLAYING);
				ClientData.data[ClientData.activeWindow].RTCPSink.setState(State.PLAYING);
				//ClientData.appSink.setState(State.PLAYING);
				ClientData.rate = 1;
				
				Message play = new Message(MessageType.CONTROL);
				
				try {
					play.setSender("VC00");
					play.addData(Message.ACTION_KEY, Message.PLAY_ACTION);
					
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
				
				TCPClient.sendServerMessage(play);
				
			}					
		});
		
		//pause button
		JButton pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Setting state to paused");
				//ClientData.pipe.setState(State.PAUSED);
				
				//TCPClient.sendServerMessage("pause");
				
				Message pause = new Message(MessageType.CONTROL);
				
				try {
					pause.setSender("VC00");
					pause.addData(Message.ACTION_KEY, Message.PAUSE_ACTION);
					
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				TCPClient.sendServerMessage(pause);
			}					
		});
		
		//stop button
		JButton stopButton = new JButton("Stop");
		stopButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Setting state to ready");
				ClientData.data[ClientData.activeWindow].pipe.setState(State.NULL);
				Message stop = new Message(MessageType.CONTROL);
				
				try {
					stop.setSender("VC00");
					stop.addData(Message.ACTION_KEY, Message.STOP_ACTION);
					
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
				
				TCPClient.sendServerMessage(stop);
				
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				ClientData.data[ClientData.activeWindow].state = ClientData.State.REQUESTING;
			}					
		});
		
		//fast forward
		JButton fastForwardButton = new JButton("Fastforward");
		fastForwardButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				//TCPClient.sendServerMessage("fastforward");
			
				Message ff = new Message(MessageType.CONTROL);
				
				try {
					ff.setSender("VC00");
					ff.addData(Message.ACTION_KEY, Message.FAST_FORWARD_ACTION);
					
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				TCPClient.sendServerMessage(ff);
			}
		});
		
		//rewind
		JButton rewindButton = new JButton("Rewind");
		rewindButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				//TCPClient.sendServerMessage("rewind");
				
				Message rw = new Message(MessageType.CONTROL);
				
				try {
					rw.setSender("VC00");
					rw.addData(Message.ACTION_KEY, Message.REWIND_ACTION);
					
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				TCPClient.sendServerMessage(rw);
				
			}
		});
		

//		ClientData.duration = 51;
//		//System.out.println((int)(ClientData.duration/1000000000));
//		ClientData.slider = new JSlider(0,(int)(ClientData.duration/1000000000),0);
//		ClientData.slider.addChangeListener(new ChangeListener(){
//			public void stateChanged(ChangeEvent e) {
//				if(ClientData.seek)
//				{
//					ClientData.pipe.seek(ClientData.slider.getValue(), TimeUnit.SECONDS);
//					ClientData.seek = false;
//				}
//			}
//		});
//		ClientData.slider.addMouseListener(new MouseListener()
//				{
//					public void mouseClicked(MouseEvent e) {
//						ClientData.seek = true;
//					}
//
//					public void mousePressed(MouseEvent e) {
//
//					}
//
//					public void mouseReleased(MouseEvent e) {
//						ClientData.seek = true;
//					}
//
//					public void mouseEntered(MouseEvent e) {
//
//					}
//
//					public void mouseExited(MouseEvent e) {
//					}
//					
//				});
		
		JPanel controls = new JPanel();
		controls.add(negotiateButton);
		controls.add(playButton);
		controls.add(pauseButton);
		controls.add(stopButton);
		controls.add(fastForwardButton);
		controls.add(rewindButton);
		//controls.add(ClientData.slider);
		
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
					//.addComponent(ClientData.slider)
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
				    //.addComponent(ClientData.slider)
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
		String[] resList = {"320x240", "640x480"};
		String[] frList = {"10"};
		String[] actpass = {"Active", "Passive"};
		
		JTextField serverIPField = new JTextField("Enter server IP address");
		serverIPField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JTextField source = (JTextField) e.getSource();
				ClientData.data[ClientData.activeWindow].serverAddress = source.getText();
			}
		});
		
		ClientData.optionsComponents.add(serverIPField);
		
		
		
		JLabel activity = new JLabel("Mode");
		activity.setPreferredSize(new Dimension(150, 30));
		
		JComboBox activeOrPassive = new JComboBox(actpass);
		activeOrPassive.setPreferredSize(new Dimension(150, 30));
		activeOrPassive.setSelectedIndex(1);
		activeOrPassive.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e){
			JComboBox source = (JComboBox)e.getSource();
			int selection = source.getSelectedIndex();
			if(selection == 0)
				ClientData.setMode(ClientData.Mode.ACTIVE);
			else
				ClientData.setMode( ClientData.Mode.PASSIVE);
			}
		});
		
		ClientData.optionsComponents.add(activeOrPassive);
		
		
	
		JLabel resLabel = new JLabel("Resolution");
		resLabel.setPreferredSize(new Dimension(150, 30));
		//frame rate list picker
		JComboBox resCB = new JComboBox(resList);
		//resCB.setPreferredSize(new Dimension(100,50));
		resCB.setSelectedIndex(0);
		resCB.setPreferredSize(new Dimension(150,30));
		resCB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JComboBox source = (JComboBox)e.getSource();
				String selected = (String)source.getSelectedItem();
				if(selected.equals("320x240"))
					ClientData.FrameRes.setRes("320x240");
				else if(selected.equals("640x480"))
					ClientData.FrameRes.setRes("640x480");
			}
		});
		
		ClientData.optionsComponents.add(resCB);
		
		JLabel frLabel = new JLabel("Frame Rate");
		frLabel.setPreferredSize(new Dimension(150, 30));
		//resolution list picker
		JComboBox frCB = new JComboBox(frList);
		//frCB.setPreferredSize(new Dimension(10,10));
		frCB.setSelectedIndex(0);
		frCB.setEnabled(false);
		frCB.setPreferredSize(new Dimension(150,40));
		frCB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				
				JComboBox source = (JComboBox)e.getSource();
				int selected = source.getSelectedIndex();
				if( ClientData.data[ClientData.activeWindow].mode == ClientData.Mode.PASSIVE)
					ClientData.frameRate = 10;
				else
					ClientData.frameRate = selected+15;
			}
		});
		
		ClientData.optionsComponents.add(frCB);
			
		JButton editResources = new JButton("Edit Resource");
		editResources.setPreferredSize(new Dimension(150,40));
		editResources.addActionListener( new ActionListener(){
				public void actionPerformed(ActionEvent e)
				{
					String s = (String)JOptionPane.showInputDialog(
		                    ClientData.frame,
		                    "What would you like the new resources to be?",
		                    "Modify Resources",
		                    JOptionPane.PLAIN_MESSAGE,
		                    null,
		                    null,
		                    ClientResource.getInstance().getBandwidth()
		                    );
					if(s != null && !s.equals("") )
					{	
						try {
							int newRes = Integer.parseInt(s);
							ClientResource.getInstance().setBandwidth(newRes);
						
						}catch(NumberFormatException n)
						{
							// do nothing.
						}
					}
					
				}
		});
		
		JPanel userOptions = new JPanel();
		userOptions.setPreferredSize(new Dimension(200,200));
		userOptions.add(resCB);
		userOptions.add(frCB);
		//userOptions.add(activity);
		userOptions.add(activeOrPassive);
		userOptions.add(editResources);
		
		GroupLayout userOptionsLayout = new GroupLayout(userOptions);
		userOptionsLayout.setAutoCreateGaps(true);
		userOptionsLayout.setAutoCreateContainerGaps(true);
		
		userOptionsLayout.setHorizontalGroup(
			userOptionsLayout.createParallelGroup()
				.addComponent(resLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
	            		  GroupLayout.PREFERRED_SIZE)
				.addComponent(resCB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
				          GroupLayout.PREFERRED_SIZE)
				.addComponent(frLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
	            		  GroupLayout.PREFERRED_SIZE)
				.addComponent(frCB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
				          GroupLayout.PREFERRED_SIZE)
	            .addComponent(activity, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
	            		  GroupLayout.PREFERRED_SIZE)
				.addComponent(activeOrPassive, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
				          GroupLayout.PREFERRED_SIZE)
				.addComponent(editResources, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
				          GroupLayout.PREFERRED_SIZE)
				.addComponent(serverIPField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
				          GroupLayout.PREFERRED_SIZE)
		);
		userOptionsLayout.setVerticalGroup(
			userOptionsLayout.createSequentialGroup()
				.addComponent(resLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
	            		  GroupLayout.PREFERRED_SIZE)
				.addComponent(resCB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
				          GroupLayout.PREFERRED_SIZE)
				.addComponent(frLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
	            		  GroupLayout.PREFERRED_SIZE)
				.addComponent(frCB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
				          GroupLayout.PREFERRED_SIZE)
				.addComponent(activity, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
				          GroupLayout.PREFERRED_SIZE)
				.addComponent(activeOrPassive, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
				          GroupLayout.PREFERRED_SIZE)
				.addComponent(editResources, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
				          GroupLayout.PREFERRED_SIZE)
				.addComponent(serverIPField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
				          GroupLayout.PREFERRED_SIZE)          
		);
		userOptions.setLayout(userOptionsLayout);
		
		return userOptions;
	}
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 * @return 
	 */
	
	protected static void addTextToMonitor(String text){
		
		ClientData.monitor.setText(text); //(ClientData.monitor.getText()+"\n"+
		
	}
	
	protected static void addTextToFramerateMonitor(String text)
	{
		ClientData.framerateMonitor.setText(text);
	}
	
	protected static JPanel createEncodingOptionsPanel()
	{
		//create user options panel
		JPanel userOptions = createUserOptionsPanel();	
		
		//Monitor data
		ClientData.monitor = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(ClientData.monitor);
		scrollPane.setPreferredSize(new Dimension(300, 50));
		ClientData.monitor.setEditable(false);
		
		ClientData.framerateMonitor = new JTextArea();
		JScrollPane bandScroll = new JScrollPane(ClientData.framerateMonitor);
		bandScroll.setPreferredSize(new Dimension(300,50));
		ClientData.framerateMonitor.setEditable(false);
		
		JPanel encOptions = new JPanel();
		encOptions.setPreferredSize(new Dimension(320,150));
		encOptions.add(userOptions);
		
		//define layout
		GroupLayout encLayout = new GroupLayout(encOptions);
		encLayout.setAutoCreateGaps(true);
		encLayout.setAutoCreateContainerGaps(true);
		
		encLayout.setHorizontalGroup(
				encLayout.createParallelGroup()
					.addGroup(encLayout.createSequentialGroup()						
					.addComponent(scrollPane,  GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					          GroupLayout.PREFERRED_SIZE)
					)
					.addGroup(encLayout.createSequentialGroup()
					.addComponent(bandScroll, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					          GroupLayout.PREFERRED_SIZE)
					)
					.addGroup(encLayout.createSequentialGroup()						
					.addComponent(userOptions, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					          GroupLayout.PREFERRED_SIZE)
					)
					
		);
		encLayout.setVerticalGroup(
				encLayout.createSequentialGroup()		
					.addGroup(encLayout.createParallelGroup()						
					.addComponent(scrollPane)
					)
					.addGroup(encLayout.createParallelGroup()
					.addComponent(bandScroll)
					)
					.addGroup(encLayout.createParallelGroup()						
					.addComponent(userOptions)
					)
		);
		encOptions.setLayout(encLayout);
		return encOptions;
	}
	
	protected static JPanel createWindowsPanel()
	{
		GridLayout windowLayout = new GridLayout(1,2,5,0);
		
		JPanel windowsPanel = new JPanel();
		windowsPanel.setLayout(windowLayout);
		windowsPanel.add(ClientData.vidComp1);
		ClientData.vidComp1.setPreferredSize(new Dimension(640, 480)); 
		windowsPanel.add(ClientData.vidComp2);
		ClientData.vidComp2.setPreferredSize(new Dimension(640, 480));
		return windowsPanel;
	}
	
	
	protected static JPanel createWindowPicker()
	{
		JPanel windowPicker = new JPanel();
		windowPicker.setLayout(new GridLayout(1,2,20,0));
		
		ButtonGroup buttonGroup = new ButtonGroup();
		JRadioButton window1Button = new JRadioButton("1");
		window1Button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				ClientData.activeWindow = 0;
			}
		});
		JRadioButton window2Button = new JRadioButton("2");
		window2Button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				ClientData.activeWindow = 1;
			}
		});
		
		buttonGroup.add(window1Button);
		buttonGroup.add(window2Button);
		
		windowPicker.add(window1Button);
		windowPicker.add(window2Button);
		
		window1Button.doClick();
		
		return windowPicker;
	}
}
