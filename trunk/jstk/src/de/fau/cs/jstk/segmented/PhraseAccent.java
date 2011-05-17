package de.fau.cs.jstk.segmented;

import java.io.Serializable;


public class PhraseAccent implements Serializable {
	
	private static final long serialVersionUID = 7187918001211793979L;

	public enum PHRASE_ACCENTS{		
		PRIMARY, 
		SECONDARY, 

		/**
		 * union of emphatic or contrastive accent. currently not used.
		 */
		EXTRA
	};

	private PHRASE_ACCENTS type;

	/**
	 *  the index of the word that bears this accent
	 */
	private int index;

	public void setType(PHRASE_ACCENTS type) {
		this.type = type;
	}

	public PHRASE_ACCENTS getType() {
		return type;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	
}
