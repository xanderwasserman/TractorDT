package tractorDT;

import jade.core.Agent;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class LocationManagerAgent extends Agent{

	private int numFarm;
	private int totalFarms;
	DataOutputStream outToServer;
	String dataReceived;
	String latestLocation;
	String latestTractor;
	String latestTime;

	protected void setup() {

		// Get the  start-up arguments
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			//read arguments here
			String tempNum = (String) args[0];
			numFarm =  Integer.valueOf(tempNum);
		}



		// Register the tractor agent in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("LocationManager");
		sd.setName("Farm" + numFarm);
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		System.out.println(
				"Hello, Location agent " + getAID().getName() + " is ready, and represents Farm: " + numFarm);

		addBehaviour(new TickerBehaviour(this, 5000) { //update once ever 10 seconds
			protected void onTick() {
				// Get updated location for each agent from the erlang simulation
				int Port = 9100 + numFarm;
				String msgToSend = "farm"+numFarm+"_p"; //this changes depending on the number of the specific location

				try {



					for (int i = 1; i <= 2; i++ ) {
						for (int j = 1; j <= 3; j++) {


							String msg = msgToSend+i+j;

							// setup socket
							Socket socket;
							socket = new Socket("localhost", Port);

							// send message over socket
							outToServer = new DataOutputStream(socket.getOutputStream());
							byte[] outByteString = msg.getBytes("UTF-8");
							outToServer.write(outByteString);
							//System.out.println("Sent: "+msg+" to port: "+Port);

							// read replied message from socket
							byte[] inByteString = new byte[500];
							int numOfBytes = socket.getInputStream().read(inByteString);
							dataReceived = new String(inByteString, 0, numOfBytes, "UTF-8");
							//System.out.println("Received: " + dataReceived);

							//format of received message: farm1_p11_tractor1_172622
							String[] part = dataReceived.split("_");
							latestLocation = part[0]+"_"+part[1];
							latestTractor = part[2];
							latestTime = part[3];


							XMLfunctions myXMLfunctions = new XMLfunctions();

							if (!latestTractor.contentEquals("none")  || !latestTime.contentEquals("none")) {
								String XMLstring = null;
								try {

									XMLstring = myXMLfunctions.ServerConvertMessage2send(latestTractor, latestTime, "", latestLocation);
									

									Document XMLdoc = XMLfunctions.convertStringToXML(XMLstring);

									File fXMLfile = new File("C:\\Users\\Xander\\eclipse-workspace\\TractorDigitalTwin\\Output_files\\LocationManagerLog_farm"+numFarm+".XML");

									if(!fXMLfile.exists()){
										fXMLfile.createNewFile();
										FileOutputStream writer = new FileOutputStream(fXMLfile);
										writer.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><TIScontent><tractor1 FuelUsage=\"\" Location=\"\" Time=\"\"/></TIScontent>").getBytes());
										writer.close();
									}

									DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
									DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
									Document primary = dBuilder.parse(fXMLfile);

//									try {
//										System.out.println(XMLfunctions.XML2String(XMLdoc));
//									} catch (TransformerException e) {
//										// TODO Auto-generated catch block
//										e.printStackTrace();
//									}
									
									
									XMLdoc = myXMLfunctions.MergeXMLdocs( primary, XMLdoc);//this is not working properly	
									
									
									myXMLfunctions.CreateXMLFile(XMLdoc, "LocationManagerLog_farm"+ numFarm);

								} catch (TransformerException | ParserConfigurationException | SAXException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

							// close connection
							socket.close();

							TimeUnit.SECONDS.sleep(3);

						}
					}



				} catch (ConnectException e) {
					// TODO Auto-generated catch block
					System.out.println("Exception: Cannot connect to: "+Port);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

		// Add the behaviour serving queries from FM agents
		addBehaviour(new LocationRequestsServer());

		// Add the behaviour serving purchase orders from FM agents
		addBehaviour(new LocationOrdersServer());
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
		System.out.println("Location agent " + getAID().getName() + " is terminating!");
	}


	private class LocationRequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				int tractorRequestNumber = Integer.parseInt(msg.getContent());
				ACLMessage reply = msg.createReply();

				//reply with the latest time that the tractor was seen at a location tag
				File fXMLfile = new File("C:\\Users\\Xander\\eclipse-workspace\\TractorDigitalTwin\\Output_files\\LocationManagerLog_farm"+numFarm+".XML");
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder;
				try {
					dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(fXMLfile);
					String time = XMLfunctions.retreiveXMLDocElement("tractor"+tractorRequestNumber, doc, "Time");

					if (time != null && time != "") {
						// The requested tractor location is available. Reply with the time.
						reply.setPerformative(ACLMessage.PROPOSE);
						reply.setContent(time);
					}
					else {
						// The tractor location is NOT available.
						reply.setPerformative(ACLMessage.REFUSE);
						reply.setContent("not-available");
					}
					myAgent.send(reply);
				} catch (SAXException | IOException | ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer


	private class LocationOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				int tractorRequestNumber = Integer.parseInt(msg.getContent());
				ACLMessage reply = msg.createReply();


				//reply with the latest time that the tractor was seen at a location tag
				File fXMLfile = new File("C:\\Users\\Xander\\eclipse-workspace\\TractorDigitalTwin\\Output_files\\LocationManagerLog_farm"+numFarm+".XML");
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder;
				try {
					dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(fXMLfile);
					String location = XMLfunctions.retreiveXMLDocElement("tractor"+tractorRequestNumber, doc, "Location");
					String time = XMLfunctions.retreiveXMLDocElement("tractor"+tractorRequestNumber, doc, "Time");

					if (location != null) {
						// The requested tractor location is available. Reply with the time.
						reply.setPerformative(ACLMessage.INFORM);
						//this is still replying with a string, must change to XML
						
						XMLfunctions myXMLfunctions = new XMLfunctions();
						
						//replace this message content with a XML message that indicates the tractor, time and fuel usage
						String XMLstring = null;
						try {
							XMLstring = myXMLfunctions.ServerConvertMessage2send("tractor" + tractorRequestNumber, time, "", location);
						} catch (TransformerException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						reply.setContent(XMLstring);
	
					}
					else {
						// The tractor location is NOT available.
						reply.setPerformative(ACLMessage.FAILURE);
						reply.setContent("not-available");
					}
					myAgent.send(reply);
				} catch (SAXException | IOException | ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

	private class AgentDeleteServer extends CyclicBehaviour {
		public void action() {
			
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it			
				if (msg.getContent().equals("delete")) {
					System.out.println("Delete command received!");
					doDelete();
				}
			}
			else {
				block();
			}
			
		}
	}
	
}
