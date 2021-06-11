package tractorDT;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLfunctions extends javax.swing.JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static String retreiveDataElement(String searchTerm) {

		String deals = null;

		try {

			File fXMLfile = new File("/Users/alexanderwasserman/Google Drive/Varsity/Masters/Reconfigurable control/TaskD1/SpecialsXML.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXMLfile);

			doc.getDocumentElement().normalize();

			NodeList nList = doc.getFirstChild().getChildNodes();

			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);

				if (node.getNodeType() == Node.ELEMENT_NODE) {	

					if(searchTerm.equals(node.getNodeName())) {
						deals = nodeToString(node);
						break;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return deals;

	}
	
	

	public static String nodeToString(Node input) throws TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		transformer = tf.newTransformer();
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(input), new StreamResult(writer));
		String XMLstring = writer.getBuffer().toString();

		return XMLstring;
	}

	public static Document convertStringToXML(String xmlString) 
	{
		Document doc = null;
		//API to obtain DOM Document instance
		
		try
		{
			//Parser that produces DOM object trees from XML content
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			//Create DocumentBuilder with default configuration
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.newDocument();

			//Parse the content to Document object
			doc = builder.parse(new InputSource(new StringReader(xmlString)));
			return doc;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return doc;
	}

	public static String retreiveXMLDocElement(String searchTerm, Document doc, String attribute) {

		String message = null;
		doc.getDocumentElement().normalize();
		NodeList nList = doc.getFirstChild().getChildNodes();

		for (int i = 0; i < nList.getLength(); i++) {
			Node node = nList.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {	

				Element eElement = (Element) node;

				if(searchTerm.equals(node.getNodeName())) {
					message = eElement.getAttribute(attribute);
					break;
				}
			}
		}

		return message;
	}
	
	public static String retreiveXMLDocElementFirstChildName(Document doc) {

		doc.getDocumentElement().normalize();
		return doc.getFirstChild().getFirstChild().getNodeName();

	}



	public static String XML2String(Document document) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(document);
		StringWriter strWriter = new StringWriter();
		StreamResult result = new StreamResult(strWriter);

		transformer.transform(source, result);

		return strWriter.getBuffer().toString();

	}

	public static String ClientConvertMessage2send(String msg) throws TransformerException {

		String m2s = null;

		try {

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			Document doc = docBuilder.newDocument();
			Element Message = doc.createElement("MessageToServer");
			doc.appendChild(Message);

			// set attribute to message element
			Attr attr = doc.createAttribute("Message");
			attr.setValue(msg);
			Message.setAttributeNode(attr);

			m2s = XML2String(doc);

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		}
		m2s = m2s.replace("\n", "");
		return m2s;
	}

	public String ServerConvertMessage2send(String numTractor, String time, String fuelUsage, String location) throws TransformerException {

		String m2s = null;

		try {

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			Document doc = docBuilder.newDocument();
			
			
			Element TIS = doc.createElement("TIScontent"); 
			doc.appendChild(TIS);
			  
			Element tract = doc.createElement(numTractor); 
			TIS.appendChild(tract);
			 

			// set time attribute to message element
			Attr attr1 = doc.createAttribute("Time");
			attr1.setValue(time);
			tract.setAttributeNode(attr1);
	
			// set fuel usage attribute to message element
			Attr attr2 = doc.createAttribute("FuelUsage");
			attr2.setValue(fuelUsage);
			tract.setAttributeNode(attr2);

			// set location attribute to message element
			Attr attr3 = doc.createAttribute("Location");
			attr3.setValue(location);
			tract.setAttributeNode(attr3);

			m2s = XML2String(doc);

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		}
		//m2s = m2s.replace("\n", "");
		return m2s;
	}

	public void  CreateXMLFile(Document doc, String filename) {

		final String xmlFilePath = "C:\\Users\\Xander\\eclipse-workspace\\TractorDigitalTwin\\Output_files\\" + filename + ".XML";


		try {

			// create the xml file
			//transform the DOM Object to an XML File
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource domSource = new DOMSource(doc);
			File filename1 = new File(xmlFilePath);
			filename1.createNewFile();	

			StreamResult streamResult = new StreamResult(filename1);

			// use
			// StreamResult result = new StreamResult(System.out);
			// to push to the standard output ...

			transformer.transform(domSource, streamResult);

		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Document MergeXMLdocs(Document primary, Document secondary) throws ParserConfigurationException {

		String secondaryName = secondary.getFirstChild().getFirstChild().getNodeName();

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document newDoc = docBuilder.newDocument();
		Element secondaryElement = (Element) secondary.getFirstChild().getFirstChild();

		Element root = newDoc.createElement("TIScontent");
		newDoc.appendChild(root);

		Element tractor = newDoc.createElement(secondaryName);
		root.appendChild(tractor);

		// set time attribute to message element
		Attr attr1 = newDoc.createAttribute("Time");
		attr1.setValue(secondaryElement.getAttribute("Time"));
		tractor.setAttributeNode(attr1);

		// set fuel usage attribute to message element
		Attr attr2 = newDoc.createAttribute("FuelUsage");
		attr2.setValue(secondaryElement.getAttribute("FuelUsage"));
		tractor.setAttributeNode(attr2);

		// set location attribute to message element
		Attr attr3 = newDoc.createAttribute("Location");
		attr3.setValue(secondaryElement.getAttribute("Location"));
		tractor.setAttributeNode(attr3);


		primary.getDocumentElement().normalize();

		NodeList nList = primary.getFirstChild().getChildNodes();

		for (int i = 0; i < nList.getLength(); i++) {
			Node node = nList.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {	

				if(!secondaryName.equals(node.getNodeName())) {

					Element primaryElement = (Element) node;

					Element tractor2 = newDoc.createElement(node.getNodeName());
					root.appendChild(tractor2);


					Attr attr4 = newDoc.createAttribute("Time");
					attr4.setValue(primaryElement.getAttribute("Time"));
					tractor2.setAttributeNode(attr4);

					Attr attr5 = newDoc.createAttribute("FuelUsage");
					attr5.setValue(primaryElement.getAttribute("FuelUsage"));
					tractor2.setAttributeNode(attr5);

					Attr attr6 = newDoc.createAttribute("Location");
					attr6.setValue(primaryElement.getAttribute("Location"));
					tractor2.setAttributeNode(attr6);


				}
			}
		}

		return newDoc;

	}




}
