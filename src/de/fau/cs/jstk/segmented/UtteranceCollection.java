/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		Tobias Bocklet
		Florian Hoenig
		Stefan Steidl

	This file is part of the Java Speech Toolkit (JSTK).

	The JSTK is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	The JSTK is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with the JSTK. If not, see <http://www.gnu.org/licenses/>.
*/
package de.fau.cs.jstk.segmented;

import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;

import de.fau.cs.jstk.util.ArrayUtils;
import de.fau.cs.jstk.util.ArrayUtils.PubliclyCloneable;

/**
 * a collection of utterances, e.g. read by one speaker during one training session.
 * @author hoenig
 *
 */
public class UtteranceCollection implements Serializable, PubliclyCloneable{
	private static final long serialVersionUID = -2577602064537299436L;
	

	
	Utterance [] turns = null;

	public UtteranceCollection(){		
	}
	
	public UtteranceCollection(Utterance [] turns){
		setTurns(turns);
	}
	
	public UtteranceCollection clone(){
		return new UtteranceCollection(turns); 
	}
	
	public static UtteranceCollection read(Node node) throws Exception {
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
		//Node utteranceNode;
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
			
			Utterance utterance = Utterance.read(textsegment);			
			
			turns.add(utterance);			
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
		
		return new UtteranceCollection(turns.toArray(dummy));
	}

	public static UtteranceCollection read(BufferedInputStream in) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db;

		db = dbf.newDocumentBuilder();

		org.w3c.dom.Document doc = db.parse(new BufferedInputStream(in));
		if (doc == null)
			throw new Error("could not parse document!");

		Node root;
		root = doc.getFirstChild();
		return read(root);
	}
	
	public int getNSubdivisions(){
		int n = 0;
		for (Utterance u : getTurns()){
			n += u.getSubdivisions().length;			
		}
		return n;
	}

	public void setTurns(Utterance [] turns) {	
		this.turns = ArrayUtils.arrayClone(turns);
	}

	public Utterance [] getTurns() {
		return turns;
	}
	
	
	private static void synopsis() {
		System.err.println("UtteranceCollection: options:\n" +
	      "--input=bla.xml\n" +
	      "--output=blu.xml\n" +
	      "[--subdivide]\n" +
	      "\n");	
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws FileNotFoundException, Exception {

		// process arguments
		
		String inputName = null, outputName = null;
		UtteranceCollection inputSession = null;
		boolean doSubdivide = false;
		
		for (int i = 0; i < args.length; i++) {
			System.err.println("arg = " + args[i]);
			if (args[i].equals("--help") || args[i].equals("-h")){
				synopsis();
				System.exit(1);
			}
			if (args[i].equals("--input")){
				inputName = args[++i];
				continue;
			}
			if (args[i].equals("--output")){
				outputName = args[++i];
				continue;
			}
			if (args[i].equals("--subdivide")){
				doSubdivide = true;
				continue;
			}
			System.err.println("unused argument: " + args[i]);
		}
		
		if (inputName == null){
			System.err.println("please give option --input");
			System.exit(1);
		}
		inputSession = UtteranceCollection.read(new BufferedInputStream(new FileInputStream(inputName)));
		
		if (outputName == null){
			System.err.println("please give option --output");
			System.exit(1);
		}
		
		PrintWriter outputWriter = new PrintWriter(new FileOutputStream(outputName));
		
		if (doSubdivide){
			int i, j;
			for (i = 0; i < inputSession.turns.length; i++){
				Utterance u = inputSession.turns[i];
				// put out plain
				String text = u.toTextString(true);
				outputWriter.println("sentence" + (i + 1) + " " + text);
				if (u.getSubdivisions().length > 1){
					for (j = 0; j < u.getSubdivisions().length; j++){
						text = u.getSubUtterance(j, j).toTextString(true);
						outputWriter.println("sentence" + (i + 1) + "." + j + " " + text);						
					}
				}
				
			}
				
			
			outputWriter.close();
			
			
		}
		else{
			throw new Exception("no action given!");
		}
		
		System.err.println("inputName = " + inputName);
		
		if (false){
			try {
				UtteranceCollection session = UtteranceCollection.read(
						new BufferedInputStream(
								UtteranceCollection.class.getResource("/segmented/dialog.xml").openStream()));
				//new BufferedInputStream(new FileInputStream(
				//"pronunciation/test/dialog.xml")));


				int i;
				for (i = 0; i < session.getTurns().length; i++){
					System.out.println(i + ": " + session.getTurns()[i].getOrthography() + 
							session.getTurns()[i]);				
				}

				{
					XMLEncoder e = new XMLEncoder(System.out);
					e.writeObject(session);
					e.close();				
				}

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

	

}
