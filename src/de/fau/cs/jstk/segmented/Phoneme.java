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
 * phoneme identified by an IPA string (http://www.langsci.ucl.ac.uk/ipa/)
 * @author hoenig
 *
 */
public class Phoneme implements Serializable, PubliclyCloneable {
	private static final long serialVersionUID = -4536269674274469022L;
	/**
	 * IPA representation, e.g. "i", "iː" or "eɪ"
	 */
	private String symbols = null;


	/**
	 * default (empty) constructor for XMLEncoder/Decoder.
	 */
	public Phoneme(){
	}
	
	public Phoneme(String symbols){
		this.symbols = symbols;
	}
	
	public Phoneme clone(){
		return new Phoneme(symbols);
	}

	public void setSymbols(String symbols) {
		this.symbols = symbols;
	}

	public String getSymbols() {
		return symbols;
	}

	public static Phoneme read(Node node) throws Exception {
		String nodeName = node.getNodeName();

		if (!nodeName.equals("phoneme"))
			throw new Exception("Expecting node name phoneme, got " + nodeName);			

		String symbols = node.getAttributes().getNamedItem("symbols").getNodeValue();

		return new Phoneme(symbols);		
	}	
}