package de.fau.cs.jstk.segmented;

import java.io.Serializable;

import org.w3c.dom.Node;

public class Phoneme implements Serializable {
	private static final long serialVersionUID = -4536269674274469022L;
	/**
	 * IPA representation, e.g. "i", "iː" or "eɪ"
	 */
	private String symbols = null;
	
	public Phoneme(){
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

	public static Phoneme read(Node node) throws Exception {
		String nodeName = node.getNodeName();

		if (!nodeName.equals("phoneme"))
			throw new Exception("Expecting node name phoneme, got " + nodeName);			

		String symbols = node.getAttributes().getNamedItem("symbols").getNodeValue();

		return new Phoneme(symbols);		
	}

	
}
