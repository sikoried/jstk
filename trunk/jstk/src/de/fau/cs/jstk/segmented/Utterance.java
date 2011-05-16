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
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Node;

import de.fau.cs.jstk.segmented.Boundary.BOUNDARIES;

public class Utterance implements Serializable{
	
	private static final long serialVersionUID = 3535642214459508273L;

	/**
	 * the utterance as displayed, with punctuation etc.
	 */
	private String orthography;

	/**
	 * who is speaking this, e.g. "Prince Hamlet" as in Prince Hamlet:
	 * "To be or not to be"
	 */
	private String speaker;

	/*
	 * Word [] words;
	 */

	private Boundary [] boundaries;
	
	/**
	 * possible subdivisions of this utterance. First subdivision starts at the first word, 
	 * i.e. there is always at least one subdivision.
	 */
	private Subdivision [] subdivisions;

	public Utterance(){
		setOrthography(null);
		setSpeaker(null);
		boundaries = new Boundary[0];
		setSubdivisions(new Subdivision[0]);
	}
	
	public Utterance(String orthography, String speaker, Boundary [] boundaries, Subdivision [] subdivisions) {
		this.setOrthography(orthography);
		this.setSpeaker(speaker);
		this.boundaries = boundaries;
		this.setSubdivisions(subdivisions);
	}

	static Utterance read(Node node, String speaker) throws Exception{
		String nodeName = node.getNodeName();
		
		if (!nodeName.equals("utterance"))
			throw new Exception("Expecting node name utterance, got " + nodeName);			
		
		String orthography = null;
		
		List<Boundary> boundaries = new LinkedList<Boundary>();
		List<Subdivision> subdivisions = new LinkedList<Subdivision>();
		
		node = node.getFirstChild();
		
		
		while (node != null) {
			nodeName = node.getNodeName();
			if (nodeName.equals("#text")){
				node = node.getNextSibling();				
				continue;
			}
			else if (nodeName.equals("orthography")) {
				orthography = node.getTextContent();
			}

			else if (nodeName.equals("boundary")){				
				boundaries.add(Boundary.read(node));
			}

			else if (nodeName.equals("subdivision")){				
				subdivisions.add(Subdivision.read(node));
			}
			else{
				throw new Exception("unexpected node name in utterance: " + nodeName);
			}

			node = node.getNextSibling();
		}
		//System.out.println("orthography = " + orthography + ", speaker = " + speaker);
		
		Boundary [] boundaryDummy = new Boundary[0];
		Subdivision[] subdivisionDummy = new Subdivision[0];
				
		return new Utterance(orthography, speaker, 
				boundaries.toArray(boundaryDummy),
				subdivisions.toArray(subdivisionDummy));
	}
	
	/**
	 * @return the number of main phrases, according to B3 boundaries,
	 * (i.e. the number of B3 boundaries + 1)
	 */
	public int getNMainPhrasess(){
		int ret = 1;
		for (Boundary b : boundaries){
			if (b.getType() == BOUNDARIES.B3)
				ret++;			
		}
		return ret;		
	}
	
	/**
	 * 
	 * @param i
	 * @return the part of orthography that belongs to main phrase number i 
	 */
	public String getMainPhraseOrthographyy(int i){
		int start, end;
		
		if (boundaries.length == 0)
			return getOrthography();
		
		// find (B3) boundaries surrounding main phrase i
		int nB3 = 0;
		int boundaryBefore = Integer.MIN_VALUE, boundaryAfter = Integer.MIN_VALUE;
		
		int boundary;
		for (boundary = 0; boundary < boundaries.length; boundary++) {
			if (boundaries[boundary].getType() == BOUNDARIES.B3)
				nB3++;
			if (nB3 == i && 
					// don't overwrite!
					boundaryBefore == Integer.MIN_VALUE){
				boundaryBefore = boundary;
			}
			if (nB3 == i + 1){
				boundaryAfter = boundary;
				break;
			}			
		}		
				
		if (i == 0)
			boundaryBefore = -1;
		
		if (i == subdivisions.length - 1)
			boundaryAfter = boundaries.length;
		
		if (boundaryBefore == Integer.MIN_VALUE || boundaryAfter == Integer.MIN_VALUE)
			throw new Error("getMainPhraseOrthography: Implementation Error?");		
		
		
		/*
		System.out.println(orthography);
		System.out.println("i = "  + i + ", boundaryBefore = " + boundaryBefore + ", boundaryAfter = " + boundaryAfter);
		*/
		

		if (boundaryBefore == -1)		
			start = 0;
		else
			start = boundaries[boundaryBefore].getBeginsInOrthography();
		
		if (boundaryAfter == boundaries.length)
			end = getOrthography().length();
		else {	
			end = boundaries[boundaryAfter].getBeginsInOrthography();
		}
		
		//System.out.println("-> " + orthography.substring(start, end));
		
		return getOrthography().substring(start, end);		
		
	}

	public void setOrthography(String orthography) {
		this.orthography = orthography;
	}

	public String getOrthography() {
		return orthography;
	}

	public void setSpeaker(String speaker) {
		this.speaker = speaker;
	}

	public String getSpeaker() {
		return speaker;
	}
	
	// FIXME
	public String toString(){
		String ret = "subdivisions = ";
		for (Subdivision s : getSubdivisions()){
			ret += "" + s.getFirstWord() + "," + s.getFirstCharacterInOrthography() + "; ";
			
		}
		ret += "\n";
		return ret;
		
		
	}

	public String getSubdivisionOrthography(int i) {
		int start, end;

		start = getSubdivisions()[i].getFirstCharacterInOrthography();
		if (i == getSubdivisions().length - 1)
			end = getOrthography().length();
		else
			end = getSubdivisions()[i + 1].getFirstCharacterInOrthography();
		/*
		System.out.println("start = " + start);
		System.out.println("end = " + end);
		System.out.println("strlen = " + getOrthography().length());
		*/		
		
		return getOrthography().substring(start, end);		

	}

	public void setSubdivisions(Subdivision [] subdivisions) {
		this.subdivisions = subdivisions;
	}

	public Subdivision [] getSubdivisions() {
		return subdivisions;
	}

}

