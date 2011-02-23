package de.fau.cs.jstk.assess.struc;



import java.io.Serializable;

import org.w3c.dom.Node;


public class Boundary implements Serializable{
	
	private static final long serialVersionUID = -5380280871782636847L;
	public enum BOUNDARIES{
		NONE,
		B2, 
		/**
		 * larger break as after comma
		 */
		B3;
	}
	
	BOUNDARIES type;
	
	/**
	 *  the number of the word before which this boundary is located;
	 *  words are counted starting with zero.
	 *  FIXME: currently not provided by libpronunciation/annotation-process --annotation-in-xml
	 */
	public int beforeWhichWord;
	
	/**
	 * the number of the character before which this boundary is located
	 * in Utterance.orthography
	 */
	public int beginsInOrthography;
	
	Boundary(BOUNDARIES type, int beforeWhichWord, int beginsInOrthography){
		this.type = type;
		this.beforeWhichWord = beforeWhichWord;
		this.beginsInOrthography = beginsInOrthography;
	}
	
	static Boundary read(Node node) throws Exception{
		String nodeName = node.getNodeName();
		
		if (!nodeName.equals("boundary"))
			throw new Exception("Expecting node name boundary, got " + nodeName);
		
		String typeString = node.getTextContent();
		BOUNDARIES type = BOUNDARIES.NONE;
		
		for (BOUNDARIES t : BOUNDARIES.values()){		
			if (typeString.equals(t.toString()))
				type = t;
		}
		if (type == BOUNDARIES.NONE){
			throw new Exception("Unacceptable value for boundary type: " + typeString);
		}		
		
		int beforeWhichWord = Integer.parseInt(node.getAttributes().getNamedItem("beforeWord").getNodeValue());
		int beforeOrthography = Integer.parseInt(node.getAttributes().getNamedItem("beforeOrthography").getNodeValue());
		
		/*
		System.out.println("Boundary " + type.toString() + " beforeWhichWord " + beforeWhichWord +
				" beforeOrthography " + beforeOrthography);*/			
				
				
		return new Boundary(type, beforeWhichWord, beforeOrthography); 
				
				
	}

}
