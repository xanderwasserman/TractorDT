package tractorDT;

import jade.core.Agent;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.xml.transform.TransformerException;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


public class FuelUsageManagerAgent extends Agent {
	// number of this tractor agent
	private int numTractor;
	String dataReceived;
	String latestFuelUsage;
	DataOutputStream outToServer;

	protected void setup() {

		// Get the number of the tractor as a start-up argument
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			String tempNum = (String) args[0];
			numTractor =  Integer.valueOf(tempNum);
		}
		
		// Register the tractor agent in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("FuelUsageManager");
		sd.setName("Tractor" + numTractor);
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		System.out.println(
				"Hello, Fuel Usage agent " + getAID().getName() + " is ready, and represents Tractor: " + numTractor);

		addBehaviour(new TickerBehaviour(this, 1000) { //update once every second
			protected void onTick() {
				// Get updated fuel usage for each agent from the erlang simulation
				int Port = 9000 + numTractor;
				String msgToSend = "request";

				try {
					// setup socket
					Socket socket;
					socket = new Socket("localhost", Port);

					// send message over socket
					outToServer = new DataOutputStream(socket.getOutputStream());
					byte[] outByteString = msgToSend.getBytes("UTF-8");
					outToServer.write(outByteString);

					// read replied message from socket
					byte[] inByteString = new byte[500];
					int numOfBytes = socket.getInputStream().read(inByteString);
					dataReceived = new String(inByteString, 0, numOfBytes, "UTF-8");
					latestFuelUsage = dataReceived;
					//System.out.println("Received Tractor" + numTractor + " fuel usage: " + latestFuelUsage);

					// close connection
					socket.close();

				} catch (ConnectException e) {
				    // TODO Auto-generated catch block
					System.out.println("Exception: Cannot connect to: "+Port);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		});

		// Add the behaviour serving queries from buyer agents
		addBehaviour(new FuelUsageRequestsServer());
		// Add the behaviour receiving queries from TIS to delete agents
		addBehaviour(new AgentDeleteServer());

	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Printout a dismissal message
		System.out.println("Tractor agent " + getAID().getName() + " is terminating!");
	}
	
	private class FuelUsageRequestsServer extends CyclicBehaviour {
		public void action() {
			
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				int tractorRequestNumber = Integer.parseInt(msg.getContent());
				ACLMessage reply = msg.createReply();

				if (tractorRequestNumber == numTractor) {
					// The requested tractor's information is available. Reply with the latest fuel usage measurement time
					reply.setPerformative(ACLMessage.INFORM);
					
					XMLfunctions myXMLfunctions = new XMLfunctions();
					
					//replace this message content with a XML message that indicates the tractor, time and fuel usage
					String XMLstring = null;
					try {
						XMLstring = myXMLfunctions.ServerConvertMessage2send("tractor" + numTractor, "", latestFuelUsage, "");
					} catch (TransformerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					reply.setContent(XMLstring);
					
				}
				else {
					// The requested tractor is NOT available for info.
					reply.setPerformative(ACLMessage.REFUSE);
				}
				myAgent.send(reply);
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
