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
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Node;

import de.fau.cs.jstk.util.ArrayUtils;
import de.fau.cs.jstk.util.ArrayUtils.PubliclyCloneable;

/**
 * a word as listed in a pronunciation dictionary: 
 * graphemes (i.e. for English in lowercase unless it's a name), 
 * phonemes, and a possible phrase accent.
 * 
 * @author hoenig
 *
 */
public class Word implements Serializable, Cloneable, PubliclyCloneable {
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
		setSyllables(syllables);
		setPhonemes(phonemes);
		this.setPhraseAccent(phraseAccent);
	}
	
	@Override
	public Word clone(){		
		return new Word(graphemes, syllables, phonemes, phraseAccent);
	}

	public void setGraphemes(String graphemes) {
		this.graphemes = graphemes;
	}

	public String getGraphemes() {
		return graphemes;
	}

	public void setSyllables(Syllable [] syllables) {
		this.syllables = ArrayUtils.arrayClone(syllables);
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
		this.phonemes = ArrayUtils.arrayClone(phonemes);
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
