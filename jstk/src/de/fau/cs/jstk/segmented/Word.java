package de.fau.cs.jstk.segmented;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Node;

import de.fau.cs.jstk.segmented.Boundary.BOUNDARIES;

/**
 * a word as listed in a pronunciation dictionary, i.e. for english words lowercase unless it's a name.
 * @author hoenig
 *
 */
public class Word implements Serializable {
	private static final long serialVersionUID = 8791266506748252565L;
	private String graphemes;
	private Syllable [] syllables;
	
	// TODO
	//private Phoneme [] phonemes;
	
	public Word(){
		graphemes = null;
		syllables = null;		
	}
	
	public Word(String graphemes, Syllable [] syllables){
		this.setGraphemes(graphemes);
		this.setSyllables(syllables);
	}

	public void setGraphemes(String graphemes) {
		this.graphemes = graphemes;
	}

	public String getGraphemes() {
		return graphemes;
	}

	public void setSyllables(Syllable [] syllables) {
		this.syllables = syllables;
	}

	public Syllable [] getSyllables() {
		return syllables;
	}

	public static Word read(Node node) throws Exception {
		String nodeName = node.getNodeName();
		
		if (!nodeName.equals("word"))
			throw new Exception("Expecting node name word, got " + nodeName);
		
	    String graphemes = node.getAttributes().getNamedItem("graphemes").getNodeValue();
	    

		List<Syllable> syllables= new LinkedList<Syllable>();
	    
	    node = node.getFirstChild();
	    
	    while(node != null){
	    	nodeName = node.getNodeName();
			if (nodeName.equals("#text")){
				node = node.getNextSibling();				
				continue;
			}
			else if (nodeName.equals("syllable")) {
				syllables.add(Syllable.read(node));
				
				
			}

			else{
				throw new Exception("unexpected node name in word: " + nodeName);
			}

			node = node.getNextSibling();
	    	
	    }
	    
	    

		Syllable[] syllableDummy = new Syllable[0];
	    
	    return new Word(graphemes, syllables.toArray(syllableDummy));
			
	}

}
