/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		Tobias Bocklet

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
 * Subdivision of a turn into an unit suitable for (dialog) training.
 * Mostly but not always coincides with B3 boundaries (de.fau.cs.jstk.segmented.Boundary)
 * @author hoenig
 *
 */
public class Subdivision implements Serializable, PubliclyCloneable{
	private static final long serialVersionUID = -8814031215182493871L;
	
	/**
	 * the index of the word that initiates this Subdivision
	 */
	private int index;
		
	/**
	 * default (empty) constructor for XMLEncoder/Decoder.
	 */
	public Subdivision(){
		index = 0;
		//firstCharacterInOrthography = 0;
	}
	
	public Subdivision(int firstWord){
		this.setIndex(firstWord);
	}
	
	public Subdivision clone(){
		return new Subdivision(index);
	}
	
	public static Subdivision read(Node node) throws Exception{
		String nodeName = node.getNodeName();
		
		if (!nodeName.equals("subdivision"))
			throw new Exception("Expecting node name subdivision, got " + nodeName);
				
		int firstWord = 
			Integer.parseInt(node.getAttributes().getNamedItem("firstWord").getNodeValue());


		return new Subdivision(firstWord);				
	}	
	
	public void setIndex(int firstWord) {
		this.index = firstWord;
	}
	public int getIndex() {
		return index;
	}
}
