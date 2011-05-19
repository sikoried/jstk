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

public class Syllable implements Serializable {
	private static final long serialVersionUID = -8714844619781382351L;

	/**
	 * the index (within the current word) of the first phoneme of this syllable 
	 */
	private int position = 0; 
	
	/**
	 * 
	 */
	private int nPhonemes = 0;

	public enum SYLLABLE_STRESS{		
		NONE,
		PRIMARY, 
		SECONDARY
	};
	
	/** the canonical lexical stress */
	private SYLLABLE_STRESS stress = null;
	
	public Syllable(){
	}
	
	public Syllable(int position, int n_phonemes, SYLLABLE_STRESS stress){
		this.setPosition(position);
		this.setN_phonemes(n_phonemes);				
		this.setStress(stress);
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getPosition() {
		return position;
	}

	public void setN_phonemes(int n_phonemes) {
		this.setnPhonemes(n_phonemes);
	}

	public int getN_phonemes() {
		return getnPhonemes();
	}

	public static Syllable read(Node node) throws Exception {
	String nodeName = node.getNodeName();
		
		if (!nodeName.equals("syllable"))
			throw new Exception("Expecting node name syllable, got " + nodeName);
		
		
		int position = Integer.parseInt(node.getAttributes().getNamedItem("position").getNodeValue());
		int nPhonemes = Integer.parseInt(node.getAttributes().getNamedItem("nPhonemes").getNodeValue());
		SYLLABLE_STRESS stress = SYLLABLE_STRESS.valueOf(
				node.getAttributes().getNamedItem("stress").getNodeValue());
		
		return new Syllable(position, nPhonemes, stress);		
	}

	public int setnPhonemes(int nPhonemes) {
		this.nPhonemes = nPhonemes;
		return nPhonemes;
	}

	public int getnPhonemes() {
		return nPhonemes;
	}

	public void setStress(SYLLABLE_STRESS stress) {
		this.stress = stress;
	}

	public SYLLABLE_STRESS getStress() {
		return stress;
	}



}
