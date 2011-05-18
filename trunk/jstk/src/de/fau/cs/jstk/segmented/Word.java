package de.fau.cs.jstk.segmented;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Node;

/**
 * a word as listed in a pronunciation dictionary, i.e. for english words lowercase unless it's a name.
 * @author hoenig
 *
 */
public class Word implements Serializable {
	private static final long serialVersionUID = 8791266506748252565L;
	private String graphemes = null;
	
	private Syllable [] syllables = null;
	
	private Phoneme [] phonemes = null;
	
	public enum PHRASE_ACCENT{		
		NONE,
		PRIMARY, 
		SECONDARY, 

		/**
		 * union of emphatic or contrastive accent. currently not used.
		 */
		EXTRA
	};
	
	/**
	 * the phrase accent that this word bears, according to a idealized, prototypical realization: 
	 * primary, secondary, or none.
	 * usually, there should be exactly one primary accent per main phrase (see {@link Utterance.getB3Boundaries})
	 * 
	 */
	private PHRASE_ACCENT phraseAccent = null;	
	
	public Word(){
	}
	
	public Word(String graphemes, Syllable [] syllables, Phoneme [] phonemes, PHRASE_ACCENT phraseAccent){
		this.setGraphemes(graphemes);
		this.setSyllables(syllables);
		this.setPhonemes(phonemes);
		this.setPhraseAccent(phraseAccent);
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
	    
	    PHRASE_ACCENT phraseAccent =
	    	PHRASE_ACCENT.valueOf(
	    			node.getAttributes().getNamedItem("phraseAccent").getNodeValue());	    

		List<Syllable> syllables = new LinkedList<Syllable>();
		List<Phoneme> phonemes = new LinkedList<Phoneme>();
	    
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
			else if (nodeName.equals("phoneme")) {
				phonemes.add(Phoneme.read(node));
			}
			else{
				throw new Exception("unexpected node name in word: " + nodeName);
			}

			node = node.getNextSibling();	    	
	    }
	    
	    
		Syllable[] syllableDummy = new Syllable[0];
		Phoneme[] phonemeDummy = new Phoneme[0];
	    
	    return new Word(graphemes, syllables.toArray(syllableDummy), phonemes.toArray(phonemeDummy),
	    		phraseAccent);
			
	}

	public void setPhonemes(Phoneme [] phonemes) {
		this.phonemes = phonemes;
	}

	public Phoneme [] getPhonemes() {
		return phonemes;
	}

	public void setPhraseAccent(PHRASE_ACCENT phraseAccent) {
		this.phraseAccent = phraseAccent;
	}

	public PHRASE_ACCENT getPhraseAccent() {
		return phraseAccent;
	}

}
