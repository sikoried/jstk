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



import java.io.Serializable;

import org.w3c.dom.Node;


public class Boundary implements Serializable{
	
	private static final long serialVersionUID = -5380280871782636847L;
	public enum BOUNDARIES{
		NONE,
		/**
		 * smaller break
		 */
		B2, 
		/**
		 * larger break as after comma
		 */
		B3;
	}
	
	private BOUNDARIES type;
	
	/**
	 *  the number of the word before which this boundary is located;
	 *  words are counted starting with zero.
	 *  FIXME: currently not provided by libpronunciation/annotation-process --annotation-in-xml
	 */
	private int beforeWhichWord;
	
	/**
	 * the number of the character before which this boundary is located
	 * in Utterance.orthography
	 * 
	 * FIXME: currently not provided by libpronunciation/annotation-process --annotation-in-xml
	 */
	private int beginsInOrthography;
	
	public Boundary(){
		type = BOUNDARIES.NONE;
		setBeforeWhichWord(0);
		beginsInOrthography = 0;
	}
	
	public Boundary(BOUNDARIES type, int beforeWhichWord, int beginsInOrthography){
		this.setType(type);
		this.setBeforeWhichWord(beforeWhichWord);
		this.setBeginsInOrthography(beginsInOrthography);
	}
	
	public static Boundary read(Node node) throws Exception{
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

	public void setBeginsInOrthography(int beginsInOrthography) {
		this.beginsInOrthography = beginsInOrthography;
	}

	public int getBeginsInOrthography() {
		return beginsInOrthography;
	}

	public void setType(BOUNDARIES type) {
		this.type = type;
	}

	public BOUNDARIES getType() {
		return type;
	}

	public void setBeforeWhichWord(int beforeWhichWord) {
		this.beforeWhichWord = beforeWhichWord;
	}

	public int getBeforeWhichWord() {
		return beforeWhichWord;
	}

}
