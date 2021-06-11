package tractorDT;

import jade.core.Agent;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class DigitalTwinAgent extends Agent{
	private int numTract;
	private AID[] locationAgents;
	private AID[] fuelUsageAgents;
	private String Time2Save ="";
	private String FuelUsage2Save="";
	private String Location2Save="";


	protected void setup() { //arguments: (int numTract)
		// Get the  start-up arguments
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			String tempNum = (String) args[0];
			numTract =  Integer.valueOf(tempNum); //reads the number of the farm
		}


		// Register the tractor agent in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("TractorDT");
		sd.setName("TractorDT" + numTract);
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		System.out.println("Hello, TractorDT agent " + getAID().getName() + " is ready, and represents tractor: " + numTract);


		// Add a TickerBehaviour that schedules a request to location agents every minute
		addBehaviour(new TickerBehaviour(this, 5000) {
			protected void onTick() {
				// Update the list of location agents
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("LocationManager");
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					locationAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						locationAgents[i] = result[i].getName();
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}

				//System.out.println("Location request being performed");
				// Perform a Location request
				myAgent.addBehaviour(new LocationRequestPerformer());
				//System.out.println("FuelUsage request being performed");
				//Perform a Fuel Usage request
				myAgent.addBehaviour(new FuelUsageRequestPerformer());//add behaviour here)
				//saves the current data to an XML file
				myAgent.addBehaviour(new SaveDataPerformer());//a behaviour to save the latest information to the log file
			}
		} );
		// Add the behaviour serving queries from buyer agents
		addBehaviour(new DataRequestsServer());
		// Add the behaviour receiving queries from TIS to delete agents
		addBehaviour(new AgentDeleteServer());
	}

	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Printout a dismissal message
		System.out.println("TractorDT agent " + getAID().getName() + " is terminating!");
	}

	private class SaveDataPerformer extends Behaviour{
		public void action() {

			XMLfunctions myXMLfunctions = new XMLfunctions();

			String XMLstring = null;
			try {

				XMLstring = myXMLfunctions.ServerConvertMessage2send("tractor"+numTract, Time2Save, FuelUsage2Save, Location2Save);

				Document XMLdoc = XMLfunctions.convertStringToXML(XMLstring);


				File fXMLfile = new File("C:\\Users\\Xander\\eclipse-workspace\\TractorDigitalTwin\\Output_files\\TractorDTlog_tractor"+numTract+".XML");

				if(!fXMLfile.exists()){
					fXMLfile.createNewFile();
					FileOutputStream writer = new FileOutputStream(fXMLfile);
					writer.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><TIScontent><tractor1 FuelUsage=\"\" Location=\"\" Time=\"\"/></TIScontent>").getBytes());
					writer.close();
				}

				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document primary = dBuilder.parse(fXMLfile);

				XMLdoc = myXMLfunctions.MergeXMLdocs( primary, XMLdoc);
				myXMLfunctions.CreateXMLFile(XMLdoc, "TractorDTlog_tractor"+ numTract);

			} catch (TransformerException | ParserConfigurationException | SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		public boolean done() {
			return true;
		}
	}

	private class FuelUsageRequestPerformer extends Behaviour{
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		public void action() {
			switch (step) {
			case 0:
				//get name of agent that manages the fuel usage for this tractor
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("FuelUsageManager");
				sd.setName("Tractor"+numTract);
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					fuelUsageAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						fuelUsageAgents[i] = result[i].getName();
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}


				ACLMessage fuelRequest = new ACLMessage(ACLMessage.REQUEST);
				if (fuelUsageAgents.length != 0) {
					fuelRequest.addReceiver(fuelUsageAgents[0]);
				}else {
					System.out.println("No Fuel Usage Agents found!");
					break;
				}
				fuelRequest.setContent(String.valueOf(numTract));
				fuelRequest.setConversationId("fuelUsage-request");
				fuelRequest.setReplyWith("request"+System.currentTimeMillis()); // Unique value
				myAgent.send(fuelRequest);

				// Prepare the template to get replies
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("fuelUsage-request"),
						MessageTemplate.MatchInReplyTo(fuelRequest.getReplyWith()));
				step = 1;
				break;
			case 1:

				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {

						//System.out.println("Received content from FuelUsageManager"+numTract+": "+reply.getContent()); //this works fine
						Document XMLdoc = XMLfunctions.convertStringToXML(reply.getContent()); 
						
						FuelUsage2Save = XMLfunctions.retreiveXMLDocElement("tractor"+numTract, XMLdoc, "FuelUsage"); 
						//System.out.println("Received FuelUsage2Save from Tractor"+numTract+": "+FuelUsage2Save);
					}
					step = 2;
					
				}
				else {
					block();
				}
				break;		
			}
		}
		public boolean done() {
			return (step == 2 );
		}
	}


	private class LocationRequestPerformer extends Behaviour {
		private int latestTime = 0;
		private int bestTime = 0;
		private String latestLocation = "";
		private String latestFuelUsage="";
		private String latestTractor="";
		private int repliesCnt = 0; // The counter of replies from LM agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		private AID bestLocation = null;


		public void action() {
			switch (step) {
			case 0:
				// Send the CFP to ALL LM agents
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

				if (locationAgents.length != 0) {
					for (int i = 0; i < locationAgents.length; ++i) {
						cfp.addReceiver(locationAgents[i]);
					}
				}else {
					System.out.println("No Location Agents found!");
					break;
				} 
				cfp.setContent(String.valueOf(numTract));
				cfp.setConversationId("location-request");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				//System.out.println("CFP sent for location");
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("location-request"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						//System.out.println("Proposal received");
						// This is an offer 
						latestTime = Integer.parseInt(reply.getContent());
						//System.out.println("Received location proposal: "+latestTime);

						//some logic to see if it is the latest location
						if (bestTime == 0 || latestTime > bestTime) {
							// This is the most recent location time at present
							bestTime = latestTime;
							bestLocation = reply.getSender();
							//System.out.println("Best Location received");
						}
					}
					repliesCnt++;
					if (repliesCnt >= locationAgents.length) {
						// We received all replies
						step = 2; 
					}
				}
				else {
					block();
				}
				break;
			case 2:
				// Send the request to the LM agent with the most recent info
				ACLMessage request = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				request.addReceiver(bestLocation);
				request.setContent(String.valueOf(numTract));
				request.setConversationId("location-request");
				request.setReplyWith("request"+System.currentTimeMillis());
				myAgent.send(request);
				//System.out.println("Proposal Accepted");

				// Prepare the template to get the  reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("location-request"),
						MessageTemplate.MatchInReplyTo(request.getReplyWith()));
				step = 3;
				break;
			case 3:      
				// Receive the  reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// retrieve the location information from the received XML message	



						System.out.println("Received Location from Tractor"+numTract+": "+reply.getContent());
						Document XMLdoc = XMLfunctions.convertStringToXML(reply.getContent());

						Time2Save = XMLfunctions.retreiveXMLDocElement("tractor"+numTract, XMLdoc, "Time");
						Location2Save = XMLfunctions.retreiveXMLDocElement("tractor"+numTract, XMLdoc, "Location");
						
						//System.out.println("Received Time2Save from Tractor"+numTract+": "+Time2Save);
						//System.out.println("Received Location2Save from Tractor"+numTract+": "+Location2Save);


					}

					step = 4;
				}
				else {
					block();
				}
				break;
			}
		}

		public boolean done() {
			return ((step == 2 && bestLocation == null) || step == 4);
		}
	}  // End of inner class RequestPerformer
	
	private class DataRequestsServer extends CyclicBehaviour {
		public void action() {
			
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			Document replyDoc = null;
			if (msg != null) {
				// CFP Message received. Process it
				ACLMessage reply = msg.createReply();
				
				if (msg.getContent().equals("Data-request")) {
					// The requested tractor's information is available. Reply with the latest data
					System.out.println("Data request received at DT"+numTract);
					reply.setPerformative(ACLMessage.INFORM);
					
					
					File fXMLfile = new File("C:\\Users\\Xander\\eclipse-workspace\\TractorDigitalTwin\\Output_files\\TractorDTlog_tractor"+numTract+".XML");

					if(!fXMLfile.exists()){
						System.out.println("DTAgent"+numTract+" file does not exist!");
					}

					try {
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					replyDoc = dBuilder.parse(fXMLfile);
					
					String replyString = XMLfunctions.XML2String(replyDoc);
					reply.setContent(replyString);
					System.out.println("DTAgent"+numTract+" is sending data");
					
					}catch(TransformerException e){
						e.printStackTrace();
					} catch (ParserConfigurationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SAXException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				else {
					// The requested tractor is NOT available for info.
					reply.setPerformative(ACLMessage.REFUSE);
					System.out.println("Data request refused by DTAgent"+numTract);
				}
				if (!XMLfunctions.retreiveXMLDocElement("tractor"+numTract, replyDoc, "Time").equals("") || !XMLfunctions.retreiveXMLDocElement("tractor"+numTract, replyDoc, "Location").equals("")) {
					myAgent.send(reply);
					System.out.println("DTAgent"+numTract+" is sending data");
				}
				
			}
			else {
				block();
			}
		}
	}
	private class AgentDeleteServer extends CyclicBehaviour {
		public void action() {
			
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				ACLMessage reply = msg.createReply();
				
				if (msg.getContent().equals("delete")) {
					doDelete();
				}
			}
			else {
				block();
			}
			
		}
	}

}
