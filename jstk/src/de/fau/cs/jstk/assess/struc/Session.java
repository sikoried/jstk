package de.fau.cs.jstk.assess.struc;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;

public class Session implements Serializable{
	private static final long serialVersionUID = -2577602064537299436L;
	public Utterance [] turns;

	Session(Utterance [] turns){
		this.turns = turns;
	}
	
	public static Session read(Node node) throws Exception {
		List<Utterance> turns = new LinkedList<Utterance>();
		
		String nodeName = node.getNodeName();
		if (!nodeName.equals("recordingSessionAnnotation")) {
			throw new Exception("Expecting recordingSessionAnnotation, got "
					+ nodeName);
		}
		String attributeValue = node.getAttributes().getNamedItem("ANNOFORMAT")
				.getNodeValue();
		if (!attributeValue.equals("XML")) {
			throw new Exception("Need Attribute ANNOFORMAT = XML, but got "
					+ attributeValue);
		}
		Node textsegment = node.getFirstChild();
		Node utterance;
		//String orthography, speaker;
		//List<Boundary> boundaries = null;
		 
		while (textsegment != null) {
			nodeName = textsegment.getNodeName();
			if (nodeName.equals("#text")) {
				textsegment = textsegment.getNextSibling();
				continue;
			}
			if (!nodeName.equals("textsegment"))
				throw new Exception("expecting node textsegment, got "
						+ nodeName);
				
			String speaker = textsegment.getAttributes().getNamedItem("speaker").getNodeValue();
				
			utterance = textsegment.getFirstChild();
			nodeName = utterance.getNodeName();
			if (nodeName.equals("#text")) {
				utterance = utterance.getNextSibling();
				nodeName = utterance.getNodeName();
			}
			nodeName = utterance.getNodeName();
			if (!nodeName.equals("utterance")) {
				throw new Exception("expecting node utterance, got " + nodeName);
			}
			
			turns.add(Utterance.read(utterance, speaker));			

			textsegment = textsegment.getNextSibling();
		}
		
		//System.out.println(turns.toString());		

		/*
		 * // write out Source source = new DOMSource(doc); File file = new
		 * File("test.xml"); Result result = new StreamResult(file); Transformer
		 * xformer = TransformerFactory.newInstance().newTransformer();
		 * xformer.transform(source, result);
		 */
		
		Utterance [] dummy = new Utterance[0];
		
		return new Session(turns.toArray(dummy));
	}

	public static Session read(BufferedInputStream in) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db;

		db = dbf.newDocumentBuilder();

		org.w3c.dom.Document doc = db.parse(new BufferedInputStream(in));
		if (doc == null)
			throw new Error("sf");

		Node root;
		root = doc.getFirstChild();
		return read(root);
	}
	
	public int getNMainPhrases(){
		int n = 0;
		for (Utterance u : turns){
			n += u.getNMainPhrases();			
		}
		return n;
	}
	
	public static void main(String[] args) {		
		try {
			Session session = Session.read(new BufferedInputStream(new FileInputStream("pronunciation/test/dialog.xml")));
			
			int i;
			for (i = 0; i < session.turns.length; i++)
				System.out.println(i + ": " + session.turns[i].orthography);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
