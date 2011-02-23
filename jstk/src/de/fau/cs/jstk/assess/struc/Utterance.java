package de.fau.cs.jstk.assess.struc;


import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Node;

import de.fau.cs.jstk.assess.struc.Boundary.BOUNDARIES;

public class Utterance implements Serializable{
	
	private static final long serialVersionUID = 3535642214459508273L;

	/**
	 * the utterance as displayed, with punctuation etc.
	 */
	public String orthography;

	/**
	 * who is speaking this, e.g. "Prince Hamlet" as in Prince Hamlet:
	 * "To be or not to be"
	 */
	public String speaker;

	/*
	 * Word [] words;
	 */

	Boundary [] boundaries;

	Utterance(String orthography, String speaker, Boundary [] boundaries) {
		this.orthography = orthography;
		this.speaker = speaker;
		this.boundaries = boundaries;
	}

	static Utterance read(Node node, String speaker) throws Exception{
		String nodeName = node.getNodeName();
		
		if (!nodeName.equals("utterance"))
			throw new Exception("Expecting node name utterance, got " + nodeName);			
		
		String orthography = null;
		
		List<Boundary> boundaries = new LinkedList<Boundary>();
		
		node = node.getFirstChild();
		
		Boundary [] boundaryDummy = new Boundary[0];
		
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
			else{
				throw new Exception("unexpected node name in utterance: " + nodeName);
			}

			node = node.getNextSibling();
		}
		//System.out.println("orthography = " + orthography + ", speaker = " + speaker);
		return new Utterance(orthography, speaker, boundaries.toArray(boundaryDummy));
	}
	
	/**
	 * @return the number of main phrases, according to B3 boundaries,
	 * (i.e. the number of B3 boundaries + 1)
	 */
	public int getNMainPhrases(){
		int ret = 1;
		for (Boundary b : boundaries){
			if (b.type == BOUNDARIES.B3)
				ret++;			
		}
		return ret;		
	}
	
	/**
	 * 
	 * @param i
	 * @return the part of orthography that belongs to main phrase number i 
	 */
	public String getMainPhraseOrthography(int i){
		int start, end;
		
		if (boundaries.length == 0)
			return orthography;
		
		// find (B3) boundaries surrounding main phrase i
		int nB3 = 0;
		int boundaryBefore = Integer.MIN_VALUE, boundaryAfter = Integer.MIN_VALUE;
		
		int boundary;
		for (boundary = 0; boundary < boundaries.length; boundary++) {
			if (boundaries[boundary].type == BOUNDARIES.B3)
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
		
		if (i == getNMainPhrases() - 1)
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
			start = boundaries[boundaryBefore].beginsInOrthography;
		
		if (boundaryAfter == boundaries.length)
			end = orthography.length();
		else {	
			end = boundaries[boundaryAfter].beginsInOrthography;
		}
		
		//System.out.println("-> " + orthography.substring(start, end));
		
		return orthography.substring(start, end);		
		
	}
	
	
}

