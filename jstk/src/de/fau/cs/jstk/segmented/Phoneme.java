package de.fau.cs.jstk.segmented;

import java.io.Serializable;

public class Phoneme implements Serializable {
	private static final long serialVersionUID = -4536269674274469022L;
	/**
	 * IPA representation, e.g. "i", "iː" or "eɪ"
	 */
	private String symbols;
	
	public Phoneme(){
		symbols = null;
	}
	
	public Phoneme(String symbols){
		this.symbols = symbols;
	}

	public void setSymbols(String symbols) {
		this.symbols = symbols;
	}

	public String getSymbols() {
		return symbols;
	}
}
