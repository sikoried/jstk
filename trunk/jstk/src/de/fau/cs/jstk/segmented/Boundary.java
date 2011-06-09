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

import de.fau.cs.jstk.util.ArrayUtils.PubliclyCloneable;

/**
 * phrase boundary: smaller (B2) and larger (B3) breaks in a sentence
 * @author hoenig
 *
 */
public class Boundary implements Serializable, PubliclyCloneable{
	
	private static final long serialVersionUID = -5380280871782636847L;
	public enum BOUNDARIES{
		//NONE, outdated: implicitly given by non-existent boundaries
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
	 *  the index of the word *before* which this boundary is located;
	 *  Boundaries before the first word (index 0) and after the last are currently 
	 *  disallowed!
	 */
	private int index;
	
	/**
	 * default (empty) constructor for XMLEncoder/Decoder.
	 */
	public Boundary(){
		type = null;
		setIndex(0);
	}
	
	/**
	 * 
	 * @param type
	 * @param index the index of the word *before* which this boundary is located
	 */
	public Boundary(BOUNDARIES type, int index){
		this.setType(type);
		this.setIndex(index);
	}
	
	public Boundary clone(){
		return new Boundary(type, index);
	}
	
	/**
	 * read from annotool xml-format, see resource /segmented/dialog.xml for examples.
	 */
	public static Boundary read(Node node) throws Exception{
		String nodeName = node.getNodeName();
		
		if (!nodeName.equals("boundary"))
			throw new Exception("Expecting node name boundary, got " + nodeName);	
		
		int beforeWhichWord = Integer.parseInt(node.getAttributes().getNamedItem("beforeWord").getNodeValue());
		BOUNDARIES type = BOUNDARIES.valueOf(node.getAttributes().getNamedItem("type").getNodeValue());
		
		/*
		System.out.println("Boundary " + type.toString() + " beforeWhichWord " + beforeWhichWord +
				" beforeOrthography " + beforeOrthography);*/				
				
		return new Boundary(type, beforeWhichWord);				
	}

	public void setType(BOUNDARIES type) {
		this.type = type;
	}

	public BOUNDARIES getType() {
		return type;
	}

	/**
	 * set *before* which word this boundary is located
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * 
	 * @return *before* which word this boundary is located
	 */
	public int getIndex() {
		return index;
	}	

}
