package de.fau.cs.jstk.segmented;

import java.io.Serializable;

import org.w3c.dom.Node;

public class Subdivision implements Serializable{
	private static final long serialVersionUID = -8814031215182493871L;
	private int firstWord;
	private int firstCharacterInOrthography;
	
	public Subdivision(){
		firstWord = 0;
		firstCharacterInOrthography = 0;
	}
	
	public Subdivision(int firstWord, int firstCharacterInOrthography){
		this.setFirstWord(firstWord);
		this.setFirstCharacterInOrthography(firstCharacterInOrthography);
	}
	
	public static Subdivision read(Node node) throws Exception{
		String nodeName = node.getNodeName();
		
		if (!nodeName.equals("subdivision"))
			throw new Exception("Expecting node name subdivision, got " + nodeName);
				
		int firstWord = 
			Integer.parseInt(node.getAttributes().getNamedItem("firstWord").getNodeValue());

		int firstCharacterInOrthography = 
			Integer.parseInt(node.getAttributes().getNamedItem("firstCharacterInOrthography").getNodeValue());
				

		//System.err.println("subdivision: " + firstWord + ", " + firstCharacterInOrthography);
				
		return new Subdivision(firstWord, firstCharacterInOrthography);				
	}	
	
	public void setFirstWord(int firstWord) {
		this.firstWord = firstWord;
	}
	public int getFirstWord() {
		return firstWord;
	}
	public void setFirstCharacterInOrthography(int firstCharacterInOrthography) {
		this.firstCharacterInOrthography = firstCharacterInOrthography;
	}
	public int getFirstCharacterInOrthography() {
		return firstCharacterInOrthography;
	}
}
