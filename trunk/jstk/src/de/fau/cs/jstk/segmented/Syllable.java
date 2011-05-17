package de.fau.cs.jstk.segmented;

import java.io.Serializable;

import org.w3c.dom.Node;

import de.fau.cs.jstk.segmented.Boundary.BOUNDARIES;

public class Syllable implements Serializable {
	private static final long serialVersionUID = -8714844619781382351L;

	/**
	 * the index (within the current word) of the first phoneme of this syllable 
	 */
	private int position; 
	
	/**
	 * 
	 */
	private int nPhonemes;

	
	public Syllable(){
		position = nPhonemes = 0;
	}
	
	public Syllable(int position, int n_phonemes){
		this.setPosition(position);
		this.setN_phonemes(n_phonemes);				
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getPosition() {
		return position;
	}

	public void setN_phonemes(int n_phonemes) {
		this.nPhonemes = n_phonemes;
	}

	public int getN_phonemes() {
		return nPhonemes;
	}

	public static Syllable read(Node node) throws Exception {
	String nodeName = node.getNodeName();
		
		if (!nodeName.equals("syllable"))
			throw new Exception("Expecting node name syllable, got " + nodeName);
		
		
		int position = Integer.parseInt(node.getAttributes().getNamedItem("position").getNodeValue());
		int nPhonemes = Integer.parseInt(node.getAttributes().getNamedItem("nPhonemes").getNodeValue());
		
		return new Syllable(position, nPhonemes);		
	}



}
