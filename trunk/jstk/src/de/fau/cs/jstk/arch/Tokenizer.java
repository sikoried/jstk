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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.fau.cs.jstk.exceptions.InvalidFormatException;
import de.fau.cs.jstk.exceptions.OutOfVocabularyException;

/**
 * The Tokenizer translates a (sequence of) word(s) into the respective token
 * series (sometimes known as 'lexicon'). The involved Alphabet provides the 
 * possible Tokens and the respective number of HMM states. 
 * 
 * @author sikoried
 */
public final class Tokenizer {
	/** maps token to number of HMM states */
	private Alphabet alphabet;
	
	/** tokenizations, supposedly sorted */
	public LinkedList<Tokenization> tokenizations = new LinkedList<Tokenization>();
	
	/**
	 * Generate a new Tokenizer using the given Alphabet.
	 * @param Alpabet
	 */
	public Tokenizer(Alphabet alphabet) {
		this.alphabet = alphabet;
	}
	
	/**
	 * Generate a new Tokenizer and add all tokenizations from the given file using
	 * the referenced alphabet.
	 * @param alphabet
	 * @param tokenizationFile
	 * @throws IOException
	 */
	public Tokenizer(Alphabet alphabet, File tokenizationFile) throws IOException {
		this(alphabet);
		addTokenizationsFromFile(tokenizationFile);
	}
	
	/**
	 * Use the Tokenizer to obtain a token sequence for a given word. Runs a 
	 * binary search on the lexicon list.
	 * 
	 * @param word requested word
	 * @return token sequence as String array
	 * @throws OutOfVocabularyException
	 */
	public String [] tokenize(String word) 
		throws OutOfVocabularyException {
		int pos = Collections.binarySearch(tokenizations, new Tokenization(word, new String [0]));
		
		if (pos < 0)
			throw new OutOfVocabularyException(word);
		
		return tokenizations.get(pos).sequence;
	}
	
	/**
	 * Validate if the Tokenizer knows the referenced word.
	 * @param word
	 * @return
	 */
	public boolean validate(String word) {
		int pos = Collections.binarySearch(tokenizations, new Tokenization(word, new String [0]));
		return (pos >= 0);
	}
	
	/**
	 * Get a String representation of the Tokenizer
	 */
	public String toString() {
		return "Tokenizer with " + tokenizations.size() + " tokenizations";
	}
	
	/**
	 * Generate an ASCII dump of the Tokenizer.
	 * @return
	 */
	public void dump(PrintStream ps) {
		for (Tokenization e : tokenizations)
			ps.println(e);
	}
	
	/**
	 * Insert an entry without sorting the lexicon afterwards
	 * @param e
	 */
	public void addTokenization(Tokenization e) {
		tokenizations.add(e);
	}
	
	/**
	 * Sort the lexicon
	 */
	private void sortTokenizations() {
		Collections.sort(tokenizations);
	}
	
	/** 
	 * Retrieve the corresponding Tokenization for the given word
	 * @param word
	 * @return
	 */
	public Tokenization getWordTokenization(String word) throws OutOfVocabularyException {
		for (Tokenization e : tokenizations)
			if (e.word.equals(word))
				return e;
		throw new OutOfVocabularyException(word);
	}
	
	/**
	 * Retrieve the corresponding Tokenizations for the given word sequence
	 * (separated by white spaces).
	 * @see getTokenization
	 * @param sentence words separated by whitespace
	 * @return
	 */
	public List<Tokenization> getSentenceTokenization(String sentence) throws OutOfVocabularyException {
		String [] words = sentence.split("\\s+");
		LinkedList<Tokenization> list = new LinkedList<Tokenization>();
		for (String w : words)
			list.add(getWordTokenization(w));
		return list;
	}

	/**
	 * Add a Tokenization to the Tokenizer
	 * @param word
	 * @param transcription
	 * @throws IOException
	 */
	public void addTokenization(String word, String transcription) throws IOException {
		addTokenization(new Tokenization(word + " " + transcription, alphabet));
	}
	
	/**
	 * Add tokenizations of a file containing lines a la "word tok1 [tok2 ...]"
	 * @param file
	 * @throws InvalidFormatException
	 * @throws IOException
	 */
	public void addTokenizationsFromFile(File file) throws IOException {
		String buf;
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		while ((buf = br.readLine()) != null)
			addTokenization(new Tokenization(buf, alphabet));
		br.close();
		
		sortTokenizations();
	}

	public static final String SYNOPSIS = 
		"sikoried, 3/6/2010\n" +
		"The Tokenizer can  be used to verify a lexicon using alphabet and tokenization file.\n" +
		"usage: Tokenizer alphabet tokenization1 [tokenization2 ...]\n";
	
	public static void main(String [] args) throws IOException {
		if (args.length != 2) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}

		Tokenizer tok = new Tokenizer(new Alphabet(new File(args[0])), new File(args[1]));
		
		tok.dump(System.out);
	}
}
