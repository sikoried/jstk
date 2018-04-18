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
package de.fau.cs.jstk.arch;

import java.io.IOException;

/**
 * A Tokenization is the word and the respective token sequence.
 * 
 * @author sikoried
 */
public final class Tokenization implements Comparable <Tokenization> {
	private static final String [] nullseq = new String [0];
	
	/** The actual word */
	public String word;
	
	/** The token sequence using tokens */
	public String [] sequence;
	
	/** 
	 * Generate a phony Tokenization with only the word but no actual 
	 * tokenization. This can be used to search within the Tokenizer
	 * @param word
	 */
	public Tokenization(String word) {
		this.word = word;
		this.sequence = nullseq;
	}
	
	/**
	 * Create a new lexicon entry using given word and transcription
	 * @param word
	 * @param transcription
	 */
	public Tokenization(String word, String [] sequence) {
		this.word = word;
		this.sequence = sequence;
	}
	
	/**
	 * Create a new lexicon entry using the given line containing something
	 * like "word tok1 [tok2 ...]".
	 * @param line
	 */
	public Tokenization(String line, Alphabet alphabet) throws IOException {
		String [] split = line.trim().split("\\s+");
		this.word = split[0];
		this.sequence = new String [split.length-1];
		for (int i = 1; i < split.length; ++i) {
			if (!alphabet.isValidToken(split[i]))
				throw new IOException("Unknown token '" + split[i] + "'");
			sequence[i-1] = split[i];
		}
	}
	
	/**
	 * lexical sort
	 */
	public int compareTo(Tokenization e) {
		return word.compareTo(e.word);
	}
	
	/**
	 * Two Tokenizations are equal if the respective word is the same.
	 */
	public boolean equals(Object o) {
		if (o instanceof Tokenization)
			return ((Tokenization) o).word.equals(word);
		return false;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(word);
		for (String t : sequence)
			sb.append(" " + t);
		return sb.toString();
	}
	
	public int hashCode() {
		return word.hashCode();
	}
}
