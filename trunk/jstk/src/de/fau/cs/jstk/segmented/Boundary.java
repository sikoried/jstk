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
