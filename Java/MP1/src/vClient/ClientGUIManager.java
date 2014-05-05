package vClient;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import vNetwork.Message;
import vNetwork.Message.MessageType;

import org.gstreamer.Element;
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
						if(ClientData.mediaType == ClientData.MediaType.MOVIE)
							streamRequest.addData(Message.SOURCE_KEY, Message.MOVIE_SOURCE_VALUE);
						else 
							streamRequest.addData(Message.SOURCE_KEY, Message.WEBCAM_SOURCE_VALUE);
						
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
				if(!ClientData.data[ClientData.activeWindow].pipe.getState().equals(State.PLAYING))
				{
					System.out.println("Setting state to playing");
					ClientData.data[ClientData.activeWindow].pipe.setState(State.PLAYING);
					ClientData.data[ClientData.activeWindow].windowAppSink.setState(State.PLAYING);
					ClientData.data[ClientData.activeWindow].udpVideoAppSink.setState(State.PLAYING);
	
					if(ClientData.data[ClientData.activeWindow].mode == ClientData.Mode.ACTIVE){
						ClientData.data[ClientData.activeWindow].udpAudioAppSink.setState(State.PLAYING);
						ClientData.data[ClientData.activeWindow].audioOutAppsink.setState(State.PLAYING);
					}
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
			}					
		});

		//pause button
		JButton pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				if(!ClientData.data[ClientData.activeWindow].pipe.getState().equals(State.PAUSED))
				{
					System.out.println("Setting state to paused");
					ClientData.data[ClientData.activeWindow].pipe.setState(State.PAUSED);
					Message pause = new Message(MessageType.CONTROL);
					try {
						pause.setSender("VC00");
						pause.addData(Message.ACTION_KEY, Message.PAUSE_ACTION);
					} catch (JSONException e1) {
						e1.printStackTrace();
					}
					TCPClient.sendServerMessage(pause);
				}
			}					
		});

		//stop button
		JButton stopButton = new JButton("Stop");
		stopButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				if(!ClientData.data[ClientData.activeWindow].pipe.getState().equals(State.NULL))
				{
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
					ClientData.data[ClientData.activeWindow].state = ClientData.State.REQUESTING;
				}
			}					
		});

		//fast forward
		JButton fastForwardButton = new JButton("Fastforward");
		fastForwardButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Message ff = new Message(MessageType.CONTROL);
				try {
					ff.setSender("VC00");
					ff.addData(Message.ACTION_KEY, Message.FAST_FORWARD_ACTION);

				} catch (JSONException e1) {
					e1.printStackTrace();
				}
				TCPClient.sendServerMessage(ff);
			}
		});

		//rewind
		JButton rewindButton = new JButton("Rewind");
		rewindButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Message rw = new Message(MessageType.CONTROL);
				try {
					rw.setSender("VC00");
					rw.addData(Message.ACTION_KEY, Message.REWIND_ACTION);
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
				TCPClient.sendServerMessage(rw);
			}
		});

		JButton muteButton = new JButton("Mute");
		muteButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(ClientData.data[ClientData.activeWindow].mode.equals(ClientData.Mode.ACTIVE))
				{
					ClientData.muted = !ClientData.muted;
					ClientData.data[ClientData.activeWindow].volume.set("mute", ClientData.muted);
				}
			}
		});

		JPanel controls = new JPanel();
		controls.add(negotiateButton);
		controls.add(playButton);
		controls.add(pauseButton);
		controls.add(stopButton);
		controls.add(fastForwardButton);
		controls.add(rewindButton);
		controls.add(muteButton);
		
		ClientData.controlButtons.add(negotiateButton);
		ClientData.controlButtons.add(playButton);
		ClientData.controlButtons.add(pauseButton);
		ClientData.controlButtons.add(stopButton);
		ClientData.controlButtons.add(fastForwardButton);
		ClientData.controlButtons.add(rewindButton);
		ClientData.controlButtons.add(muteButton);
		
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
						.addComponent(muteButton)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(negotiateButton)
				));				
		layout.setVerticalGroup(
				layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
						.addComponent(rewindButton)
						.addComponent(playButton)
						.addComponent(pauseButton)
						.addComponent(fastForwardButton)
						.addComponent(stopButton)
						.addComponent(muteButton)
						.addComponent(negotiateButton)
				));
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
		String[] vchat = {"Movie", "Webchat"};

		JTextField serverIPField = new JTextField("Enter server IP address");
		serverIPField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JTextField source = (JTextField) e.getSource();
				ClientData.data[ClientData.activeWindow].serverAddress = source.getText();
			}
		});

		ClientData.optionsComponents.add(serverIPField);

		
		JComboBox videoChat = new JComboBox(vchat);
		videoChat.setPreferredSize(new Dimension(150,30));
		
		videoChat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				JComboBox src = (JComboBox) e.getSource();
				int sel = src.getSelectedIndex();
				
				if(ClientData.mediaType == ClientData.MediaType.MOVIE && sel == 0)
					return;
				if(ClientData.mediaType == ClientData.MediaType.WEBCHAT && sel == 1)
					return;
				
				switch(sel)
				{
				case 0: 
					ClientData.mediaType = ClientData.MediaType.MOVIE;
					break;
				case 1: 
					ClientData.mediaType = ClientData.MediaType.WEBCHAT;
					break; 
				default: 
					break;
				}
				
				
				if(ClientData.data[ClientData.activeWindow].state.equals(ClientData.State.STREAMING))
				{
					
					
					JButton stopButton = ClientData.controlButtons.get(3);
					JButton connectButton = ClientData.controlButtons.get(0);
					JButton playButton = ClientData.controlButtons.get(1);
					
					stopButton.doClick();
					//connectButton.doClick();
					//playButton.doClick();
				}
				else
				{
					
				}
				
			}
		});
		
		
		
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
				{
					if(ClientData.data[1-ClientData.activeWindow].mode.equals(ClientData.Mode.PASSIVE))
						ClientData.setMode(ClientData.Mode.ACTIVE);
					else
						JOptionPane.showMessageDialog(ClientData.frame, "Both windows cannot be active", "Activity Error", JOptionPane.ERROR_MESSAGE);
				}
				else
					ClientData.setMode(ClientData.Mode.PASSIVE);
				
				if(ClientData.data[ClientData.activeWindow].state.equals(ClientData.State.STREAMING))
				{
					Message activityMsg = new Message();
					try {
						activityMsg.setSender("VC00");
						activityMsg.setType(MessageType.CONTROL);
						activityMsg.addData(Message.ACTIVITY_KEY, ClientData.data[ClientData.activeWindow].mode);
					} catch (JSONException e1) {
						System.err.println("CLIENT: Could not create activity change request");
						e1.printStackTrace();
					}
					TCPClient.sendServerMessage(activityMsg);
					
					try {
						String position = (String) ClientData.serverMessage.getData(Message.POSITION_KEY);
						ClientData.position = Long.parseLong(position);
						System.out.println("CURRENT POSITION IS " + ClientData.position);
					} catch (JSONException e2) {
						System.err.println("CLIENT: Could not query position\n");
					}
					
					JButton stopButton = ClientData.controlButtons.get(3);
					JButton connectButton = ClientData.controlButtons.get(0);
					JButton playButton = ClientData.controlButtons.get(1);
					
					stopButton.doClick();
					connectButton.doClick();
					
					activityMsg = new Message();
					try {
						activityMsg.setSender("VC00");
						activityMsg.setType(MessageType.CONTROL);
						activityMsg.addData(Message.ACTIVITY_KEY, ClientData.data[ClientData.activeWindow].mode);
						activityMsg.addData(Message.POSITION_KEY, Long.toString(ClientData.position));
					} catch (JSONException e1) {
						System.err.println("CLIENT: Could not create activity change request");
						e1.printStackTrace();
					}
					TCPClient.sendServerMessage(activityMsg);
					
					
					playButton.doClick();
				}
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
					{}
				}
			}
		});

		JPanel userOptions = new JPanel();
		userOptions.setPreferredSize(new Dimension(200,200));
		userOptions.add(resCB);
		userOptions.add(frCB);
		userOptions.add(activeOrPassive);
		userOptions.add(editResources);

		GroupLayout userOptionsLayout = new GroupLayout(userOptions);
		userOptionsLayout.setAutoCreateGaps(true);
		userOptionsLayout.setAutoCreateContainerGaps(true);

		userOptionsLayout.setHorizontalGroup(
				userOptionsLayout.createParallelGroup()
				.addComponent(resLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(resCB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(frLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(frCB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(videoChat, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(activity, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(activeOrPassive, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(editResources, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(serverIPField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				);
		userOptionsLayout.setVerticalGroup(
				userOptionsLayout.createSequentialGroup()
				.addComponent(resLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(resCB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(frLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(frCB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(videoChat, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(activity, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(activeOrPassive, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(editResources, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(serverIPField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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

	protected static void addTextToMonitor(String text)
	{
		if(System.currentTimeMillis() - ClientData.t >= 1000)
		{
			ClientData.jitter.setText("Jitter: " + text);
			ClientData.t = System.currentTimeMillis();
		}
	}

	protected static void addTextToFramerateMonitor(String text)
	{
		if(System.currentTimeMillis() - ClientData.t >= 1000)
			ClientData.framerate.setText("Framerate: " + text);
	}
	
	protected static void addTextToBandwidthMonitor(String text)
	{
		if(System.currentTimeMillis() - ClientData.t >= 1000)
			ClientData.bandwidth.setText("Bandwidth: " + text);
	}

	protected static JPanel createEncodingOptionsPanel()
	{
		//create user options panel
		JPanel userOptions = createUserOptionsPanel();	

		ClientData.bandwidth = new JLabel();
		ClientData.bandwidth.setText("Bandwidth: ");
		
		ClientData.framerate = new JLabel();
		ClientData.framerate.setText("Framerate: ");
		
		ClientData.jitter = new JLabel(); 
		ClientData.jitter.setText("Jitter: ");
		
		/*
		//Monitor data
		ClientData.monitor = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(ClientData.monitor);
		scrollPane.setPreferredSize(new Dimension(300, 50));
		ClientData.monitor.setEditable(false);

		//Framerate data
		ClientData.framerateMonitor = new JTextArea();
		JScrollPane bandScroll = new JScrollPane(ClientData.framerateMonitor);
		bandScroll.setPreferredSize(new Dimension(300,50));
		ClientData.framerateMonitor.setEditable(false);
		*/
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
					.addComponent(ClientData.bandwidth,  GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGroup(encLayout.createSequentialGroup()
					.addComponent(ClientData.framerate, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGroup(encLayout.createSequentialGroup()
					.addComponent(ClientData.jitter, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))	
				.addGroup(encLayout.createSequentialGroup()						
					.addComponent(userOptions, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				);
		encLayout.setVerticalGroup(
				encLayout.createSequentialGroup()		
				.addGroup(encLayout.createParallelGroup()						
					.addComponent(ClientData.bandwidth))
				.addGroup(encLayout.createParallelGroup()
					.addComponent(ClientData.framerate))
				.addGroup(encLayout.createParallelGroup()						
					.addComponent(ClientData.jitter))
				.addGroup(encLayout.createParallelGroup()						
					.addComponent(userOptions))
				);
		
		/*
		encLayout.setHorizontalGroup(
				encLayout.createParallelGroup()
				.addGroup(encLayout.createSequentialGroup()						
					.addComponent(scrollPane,  GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGroup(encLayout.createSequentialGroup()
					.addComponent(bandScroll, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGroup(encLayout.createSequentialGroup()						
					.addComponent(userOptions, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				);
		encLayout.setVerticalGroup(
				encLayout.createSequentialGroup()		
				.addGroup(encLayout.createParallelGroup()						
					.addComponent(scrollPane))
				.addGroup(encLayout.createParallelGroup()
					.addComponent(bandScroll))
				.addGroup(encLayout.createParallelGroup()						
					.addComponent(userOptions))
				);
		*/
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
