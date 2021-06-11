package tractorDT;

import jade.core.Agent;
import jade.core.AgentContainer;
import jade.core.ContainerID;
import jade.core.Profile;
import jade.core.ProfileImpl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;

import tractorDT.TISAgent;
import tractorDT.TISGUI;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class TISGUI extends JFrame {

private TISAgent myAgent;
	
private JTextField LocationManagerField, FuelUsageManagerField, DigitalTwinField, DeleteField;
private JTextArea LogArea, AgentArea;
private JLabel LMLabel, FUMLabel, DTLabel, LogLabel, TractorLabel, TimeLabel, FuelUsageLabel, LocationLabel, AgentLabel, DeleteLabel, TISLabel, TractorInformationSystemLabel;
private int flag, deleteflag,LM2Add, FU2Add, DT2Add = 0;
private String Agent2Delete;

	TISGUI(TISAgent a) {
		super("Tractor Information System");
		super.setSize(800, 800);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		super.setLocation(centerX-500, centerY-500);
		
		myAgent = a;
		
		JPanel p = new JPanel();
		p.setLayout(null);
		
		LMLabel = new JLabel("Location Managers to add:");
		LMLabel.setSize(200, 20);
		LMLabel.setLocation(500, 30);
		p.add(LMLabel);
		
		LocationManagerField = new JTextField(15);
		LocationManagerField.setSize(50, 20);
		LocationManagerField.setLocation(500, 50);
		p.add(LocationManagerField);
		
		FUMLabel = new JLabel("Fuel Usage Managers to add:");
		FUMLabel.setSize(200, 20);
		FUMLabel.setLocation(500, 80);
		p.add(FUMLabel);
		
		FuelUsageManagerField = new JTextField(15);
		FuelUsageManagerField.setSize(50, 20);
		FuelUsageManagerField.setLocation(500, 100);
		p.add(FuelUsageManagerField);
			
		DTLabel = new JLabel("Tractor Digital Twins to add:");
		DTLabel.setSize(200, 50);
		DTLabel.setLocation(500, 115);
		p.add(DTLabel);
		
		DigitalTwinField = new JTextField(15);
		DigitalTwinField.setSize(50, 20);
		DigitalTwinField.setLocation(500, 150);
		p.add(DigitalTwinField);
		
		LocationManagerField.setText("0");
		FuelUsageManagerField.setText("0");
		DigitalTwinField.setText("0");
		
		LogLabel = new JLabel("Digital Twin Log:");
		LogLabel.setSize(200, 50);
		LogLabel.setLocation(20, 5);
		p.add(LogLabel);
		
		AgentLabel = new JLabel("Active Agents:");
		AgentLabel.setSize(200, 50);
		AgentLabel.setLocation(20, 370);
		p.add(AgentLabel);
		
		TractorLabel = new JLabel("Tractor:");
		TractorLabel.setSize(200, 50);
		TractorLabel.setLocation(20, 35);
		p.add(TractorLabel);
		
		TimeLabel = new JLabel("Time:");
		TimeLabel.setSize(200, 50);
		TimeLabel.setLocation(110, 35);
		p.add(TimeLabel);
		
		FuelUsageLabel = new JLabel("Fuel Usage:");
		FuelUsageLabel.setSize(200, 50);
		FuelUsageLabel.setLocation(200, 35);
		p.add(FuelUsageLabel);
		
		LocationLabel = new JLabel("Location:");
		LocationLabel.setSize(200, 50);
		LocationLabel.setLocation(285, 35);
		p.add(LocationLabel);
		
	    LogArea = new JTextArea(300,600);
	    LogArea.setSize(400, 300);
	    LogArea.setLocation(20, 70);
	    LogArea.setEditable(false);
	    LogArea.setBorder(new EtchedBorder());
	    p.add(LogArea);
	    
	    AgentArea = new JTextArea(300,600);
	    AgentArea.setSize(400, 300);
	    AgentArea.setLocation(20, 410);
	    AgentArea.setEditable(false);
	    AgentArea.setBorder(new EtchedBorder());
	    p.add(AgentArea);
	    
	    DeleteField = new JTextField(15);
	    DeleteField.setSize(200, 20);
	    DeleteField.setLocation(500, 540);
		p.add(DeleteField);
		
		DeleteLabel = new JLabel("Enter Agent Name to Delete:");
		DeleteLabel.setSize(200, 20);
		DeleteLabel.setLocation(500, 520);
		p.add(DeleteLabel);
		
		TISLabel = new JLabel("T.I.S.");
		TISLabel.setFont(new Font("Serif", Font.PLAIN, 50));
		TISLabel.setSize(200, 50);
		TISLabel.setLocation(550, 320);
		p.add(TISLabel);
		
		TractorInformationSystemLabel = new JLabel("Tractor Information System");
		TractorInformationSystemLabel.setSize(300, 20);
		TractorInformationSystemLabel.setFont(new Font("Serif", Font.PLAIN, 20));
		TractorInformationSystemLabel.setLocation(500, 380);
		p.add(TractorInformationSystemLabel);
		
		getContentPane().add(p);
		

		
		JButton addButton = new JButton("Add");
		addButton.setLocation(500, 180);
		addButton.setSize(70, 50);
		addButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					
					LM2Add = Integer.parseInt(LocationManagerField.getText().trim());
					FU2Add = Integer.parseInt(FuelUsageManagerField.getText().trim());
					DT2Add = Integer.parseInt(DigitalTwinField.getText().trim());
					flag = 1;
					
					LocationManagerField.setText("0");
					FuelUsageManagerField.setText("0");
					DigitalTwinField.setText("0");
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(TISGUI.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		} );
		p.add(addButton);
		getContentPane().add(p);
		
		JButton deleteButton = new JButton("Delete");
		deleteButton.setLocation(500, 570);
		deleteButton.setSize(70, 50);
		deleteButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					Agent2Delete = DeleteField.getText().trim();
					deleteflag = 1;
					DeleteField.setText("");
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(TISGUI.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		} );
		p.add(deleteButton);
		getContentPane().add(p);

		
		// Make the agent terminate when the user closes 
		// the GUI using the button on the upper right corner	
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
				
				TISAgent myTISAgent = new TISAgent();
				myTISAgent.shutdownPlatform(myAgent);
				
			}
		} );
		
		setResizable(true);
	}
	
	public void showGui() {

		super.setVisible(true);
	}	
	
	public int getFlag() {
		return flag;
	}
	
	public void resetFlag() {
		flag = 0;
	}
	
	public int getLM2Add() {
		return LM2Add;
	}
	
	public int getFU2Add() {
		return FU2Add;
	}
	
	public int getDT2Add() {
		return DT2Add;
	}
	
	public void setLogArea(String s) {
		LogArea.setText(s);
	}
	
	public String getAgent2Delete() {
		return Agent2Delete;
	}
	
	public int getDeleteFlag() {
		return deleteflag;
	}
	
	public void resetDeleteFlag() {
		deleteflag = 0;
	}
	
	public void setAgentArea(String s) {
		AgentArea.setText(s);
	}
	
}
