package tractorDT;

import jade.core.Agent;
import jade.core.AgentContainer;
import jade.core.ContainerID;
import jade.core.Profile;
import jade.core.ProfileImpl;
import tractorDT.TISGUI;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import jade.content.ContentException;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPANames.Ontology;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.CreateAgent;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;

public class TISAgent extends Agent {

	private TISGUI myGui;
	private AID[] DTAgents;
	AID[] allAgents;
	private int ContainerNum = 1;
	private int numLM, numFUM, numDT = 0;
	

	protected void setup() {

		// Register the tractor agent in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("TIS");
		sd.setName("TIS");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		System.out.println("Hello, Tractor Information Service agent " + getAID().getName() + " is ready");

		// Create and show the GUI
		myGui = new TISGUI(this);
		myGui.showGui();


		addBehaviour(new TickerBehaviour(this, 5000) {
			protected void onTick() {

				// get name of agent that manages the fuel usage for this tractor
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("TractorDT");
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					DTAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						DTAgents[i] = result[i].getName();
					}
				} catch (FIPAException fe) {
					fe.printStackTrace();
				}

				//System.out.println("DigitalTwin request being performed");
				// Perform a Fuel Usage request
				myAgent.addBehaviour(new DataRequestPerformer());// add behaviour here)
			}
		});

		addBehaviour(new TickerBehaviour(this, 5000) {
			protected void onTick() {
				String PrintString = "";
				// get name of agent that manages the fuel usage for this tractor
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					allAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						allAgents[i] = result[i].getName();
					}
				} catch (FIPAException fe) {
					fe.printStackTrace();
				}

				if (allAgents.length != 0) {
					for (int i = 0; i < allAgents.length; ++i) {
						PrintString = PrintString + allAgents[i].getName() + "\n";
					}
				}

				myGui.setAgentArea(PrintString);
			}
		});

		// Add the behaviour serving queries from GUI to add agents
		addBehaviour(new AgentAddServer());
		// Add the behaviour serving queries from GUI to delete agents
		addBehaviour(new AgentDeleteRequests());

	}

	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("TIAgent " + getAID().getName() + " terminating.");
	}

	private class DataRequestPerformer extends Behaviour {
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		private int repliesCnt = 0;
		
		private String PrintStringText = "";
		private String path = "C:\\Users\\Xander\\eclipse-workspace\\TractorDigitalTwin\\Output_files\\TISAgent_log.txt";
		private int replyNum = 0;
		private String[] replyPos = new String[20];
		

		public void action() {

			switch (step) {
			case 0:

				ACLMessage DataRequest = new ACLMessage(ACLMessage.REQUEST);

				if (DTAgents.length != 0) {
					for (int i = 0; i < DTAgents.length; ++i) {
						DataRequest.addReceiver(DTAgents[i]);
						//System.out.println("Data request receiver" + i + " is:" + DTAgents[i]);
					}
				} else {
					//System.out.println("No Digital Twin Agents found!");
					break;
				}

				DataRequest.setContent("Data-request");
				DataRequest.setConversationId("Data-request");
				DataRequest.setReplyWith("request" + System.currentTimeMillis()); // Unique value
				myAgent.send(DataRequest);

				// Prepare the template to get replies
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Data-request"),
						MessageTemplate.MatchInReplyTo(DataRequest.getReplyWith()));
				step = 1;
				break;
			case 1:
				replyNum++;
				
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						System.out.println("Data transmission received from:" + reply.getSender());
						//System.out.println("Data transmission is:" + reply.getContent());
						Document XMLdoc = XMLfunctions.convertStringToXML(reply.getContent());

						String numTract = String
								.valueOf(XMLfunctions.retreiveXMLDocElementFirstChildName(XMLdoc).charAt(7));
						
						replyPos[Integer.parseInt(numTract)-1]= reply.getContent();
					
					}
					repliesCnt++;
					if (repliesCnt >= DTAgents.length) {
						// We received all replies
						step = 2;
					}

				} else {
					block();
				}
				break;
			case 2:
				String PrintString = "";
				for (int i=0; i<repliesCnt;i++) {
					
					Document XMLdoc = XMLfunctions.convertStringToXML(replyPos[i]);
					
					String iString = Integer.toString(i+1);
					
					PrintString =  PrintString +"Tractor" + iString +  "\t"
							+ XMLfunctions.retreiveXMLDocElement("tractor" + iString, XMLdoc, "Time") + "\t"
							+ XMLfunctions.retreiveXMLDocElement("tractor" + iString, XMLdoc, "FuelUsage") + "\t"
							+ XMLfunctions.retreiveXMLDocElement("tractor" + iString, XMLdoc, "Location") + "\n";
				
					
					
					PrintStringText =  "Tractor" + iString +  "\t"
							+ XMLfunctions.retreiveXMLDocElement("tractor" + iString, XMLdoc, "Time") + "\t"
							+ XMLfunctions.retreiveXMLDocElement("tractor" + iString, XMLdoc, "FuelUsage") + "\t"
							+ XMLfunctions.retreiveXMLDocElement("tractor" + iString, XMLdoc, "Location") + "\n";;
					try {
						FileWriter write = new FileWriter(path, true);
						PrintWriter print_line = new PrintWriter(write);
						print_line.printf("%s", PrintStringText);
						print_line.close();


					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					
				}
				myGui.setLogArea(PrintString);
				step = 3;
				break;
			}

			
		}

		public boolean done() {
			return (step == 3);
		}
	}

	private class AgentAddServer extends CyclicBehaviour {
		public void action() {



			if (myGui.getFlag() == 1) {

				//Get the JADE runtime interface (singleton) //from here
				jade.core.Runtime runtime = jade.core.Runtime.instance();
				//Create a Profile, where the launch arguments are stored
				Profile profile = new ProfileImpl();	
				profile.setParameter(Profile.CONTAINER_NAME, "Agent-Container"+ ContainerNum);
				ContainerNum++;
				profile.setParameter(Profile.MAIN_HOST, "localhost");

				ContainerController container = runtime.createAgentContainer(profile);//to here. this section needs to run only once. must solve

				//System.out.println("LM2Add:"+myGui.getLM2Add());
				if (myGui.getLM2Add() > 0 ) {
					for (int i=numLM+1;i<=numLM+myGui.getLM2Add();i++) {
						try {
							//start location managers
							String istring = Integer.toString(i);
							AgentController lm = container.createNewAgent("LM"+istring, "tractorDT.LocationManagerAgent",  new Object[] {istring});//arguments
							lm.start();

						} catch (StaleProxyException e) {
							e.printStackTrace();
						}
					}
					numLM =+ myGui.getLM2Add();
				}

				//System.out.println("FU2Add:"+myGui.getFU2Add());
				if (myGui.getFU2Add() > 0) {
					for (int i=numFUM+1;i<=numFUM+myGui.getFU2Add();i++) {
						try {
							//start fuel usage managers
							String istring = Integer.toString(i);
							AgentController fum = container.createNewAgent("FUM"+istring, "tractorDT.FuelUsageManagerAgent",  new Object[] {istring});//arguments
							fum.start();

						} catch (StaleProxyException e) {
							e.printStackTrace();
						}
					}
					numFUM =+ myGui.getFU2Add();
				}

				//System.out.println("DT2Add:"+myGui.getDT2Add());
				if (myGui.getDT2Add() > 0) {
					for (int i=numDT+1;i<=numDT+myGui.getDT2Add();i++) {
						try {
							//start tractor digital twins
							String istring = Integer.toString(i);
							AgentController dt = container.createNewAgent("DT"+istring, "tractorDT.DigitalTwinAgent",  new Object[] {istring});//arguments
							dt.start();

						} catch (StaleProxyException e) {
							e.printStackTrace();
						}
					}
					numDT =+ myGui.getDT2Add();
				}
				
			}
			myGui.resetFlag();
		}
	}

	private class AgentDeleteRequests extends CyclicBehaviour {
		public void action() {
			AID deleteAgent = null;
			if (myGui.getDeleteFlag() == 1) {	

				if (allAgents.length != 0) {
					for (int i = 0; i < allAgents.length; ++i) {

						if (allAgents[i].getName().equals(myGui.getAgent2Delete())) {
							deleteAgent = allAgents[i];
							break;
						}
					}

					
				}
				
				if (deleteAgent.getName().contains("FUM") && numFUM <0) {
					numFUM--;
				}
				if (deleteAgent.getName().contains("LM") && numLM <0) {
					numLM--;
				}
				if (deleteAgent.getName().contains("DT") && numDT <0) {
					numDT--;
				}
				
				ACLMessage delete = new ACLMessage(ACLMessage.REQUEST);
				delete.addReceiver(deleteAgent);
				delete.setContent("delete");
				delete.setConversationId("delete-request");
				myAgent.send(delete);
				System.out.println("Delete request sent to:"+deleteAgent);
				
			}
			myGui.resetDeleteFlag();
		}
	}
	
	public void shutdownPlatform(Agent a) {
		Action actExpr = new Action();
		actExpr.setActor(a.getAMS());
		actExpr.setAction(new ShutdownPlatform());

		SLCodec codec = new SLCodec();
		a.getContentManager().registerOntology(JADEManagementOntology.getInstance());
		a.getContentManager().registerLanguage(codec, FIPANames.ContentLanguage.FIPA_SL0);

		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.addReceiver(a.getAMS());
		request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		request.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
		request.setOntology(JADEManagementOntology.NAME);

		      try {
		a.getContentManager().fillContent(request, actExpr);
		a.send(request);
		      }
		      catch (ContentException ce) {
		            // Should never happen
		            ce.printStackTrace();
		      }
		}
}
