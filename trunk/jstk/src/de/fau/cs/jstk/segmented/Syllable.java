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
