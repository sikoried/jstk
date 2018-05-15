/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		Tobias Bocklet
		Stephan Steidl
		Florian Hoenig

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

package com.github.sikoried.jstk.lm;

import com.github.sikoried.jstk.arch.*;
import com.github.sikoried.jstk.exceptions.OutOfVocabularyException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;


/**
 * The uni-gram allows (in contrast to the zero-gram) specify language model
 * weights for each word.
 * 
 * @author sikoried
 *
 */
public class Unigram implements LanguageModel {
	/** The default language model probability for OOV words */
	public static final float DEFAULT_OOV = 0.001f;
	
	/** out-of-vocabulary probability */
	private float oovProb = DEFAULT_OOV;
	
	private Tokenizer tok;
	private TokenHierarchy th;
	private HashMap<Tokenization, Float> sils = new HashMap<Tokenization, Float>();
	private HashMap<Tokenization, Float> probs = new HashMap<Tokenization, Float>();
	
	/**
	 * Generate a new Zerogram for all words in the given Tokenizer. The words
	 * are uniformly weighted after subtraction of the silence probability mass.
	 * 
	 * @param tokenizer
	 * @param hierarchy
	 * @param sils
	 */
	public Unigram(Tokenizer tokenizer, TokenHierarchy hierarchy, HashMap<Tokenization, Float> sils) {
		this.tok = tokenizer;
		this.th = hierarchy;
		this.sils = sils;
	}
	
	/**
	 * Set the uni-gram probability
	 * @param t
	 * @param p
	 */
	public void setProb(Tokenization t, float p) {
		probs.put(t, p);
	}
	
	public void setOovProb(float p) {
		oovProb = p;
	}
	
	public double getOovProb() {
		return oovProb;
	}
	
	/**
	 * Load uni-gram Probabilities froom SRILM-stype LM file
	 * @param file
	 * @throws IOException
	 */
	public void loadSrilm(File file) throws IOException, OutOfVocabularyException {
		loadSrilm(new BufferedReader(new FileReader(file)));
	}

	public void loadSrilm(BufferedReader br) throws IOException, OutOfVocabularyException {
		String lin;
		
		// skip everything till \1-gram
		while ((lin = br.readLine()) != null) {
			if (lin.equals("\\1-grams:"))
				break;
		}
		
		// now read everything till next thing starts with a backslash
		while ((lin = br.readLine()) != null) {
			if (lin.startsWith("\\"))
				break;
			if (lin.trim().length() < 3)
				continue;
			String [] sp = lin.trim().split("\\s+");
			
			// ignore words not in the tokenizer
			if (!tok.validate(sp[1]))
				continue;
			
			// set the prob, mind the exponentiation!
			probs.put(tok.getWordTokenization(sp[1]), (float) Math.pow(10, Float.parseFloat(sp[0])));
		}
	}

	public String toString() {
		return "Unigram 1-grams=" + probs.keySet().size();
	}
	
	public TreeNode generateNetwork() {
		// re-distribute the probability masses to compensate for the silences
		float pmass = 0.f;
		for (Map.Entry<Tokenization, Float> e : sils.entrySet()) 
			pmass += e.getValue();
		
		float umass = 0.f;
		for (Tokenization t : tok.tokenizations) {
			if (sils.containsKey(t))
				continue;
			Float p = probs.get(t);
			if (p == null)
				probs.put(t, p = oovProb);
			umass += p;
		}
		
		float skew = (1.f - pmass) / umass;
		for (Tokenization t : probs.keySet())
			probs.put(t, probs.get(t) * skew);
		
		// build lexical tree
		TokenTree tree = new TokenTree(0);
		for (Tokenization t : tok.tokenizations) {
			if (sils.containsKey(t))
				tree.addToTree(t, th.tokenizeWord(t.sequence), sils.get(t));
			else 
				tree.addToTree(t, th.tokenizeWord(t.sequence), probs.get(t));
		}
		
		// factor
		tree.factor();
		
		// loop
		for (TreeNode n : tree.leaves())
			n.setLst(tree.root);
		
		return tree.root;
	}
}
