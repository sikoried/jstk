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
package de.fau.cs.jstk.lm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.fau.cs.jstk.arch.TokenTree;
import de.fau.cs.jstk.arch.TokenTree.TreeNode;
import de.fau.cs.jstk.arch.Tokenization;
import de.fau.cs.jstk.arch.Tokenizer;
import de.fau.cs.jstk.exceptions.LanguageModelException;
import de.fau.cs.jstk.exceptions.OutOfVocabularyException;


/**
 * The uni-gram is based on the individual relative frequency of the words. A
 * zero-gram (i.e. uni-gram with uniform probabilities) can be enforced.
 * 
 * @author sikoried
 *
 */
public final class Unigram extends LanguageModel {
	private static Logger logger = Logger.getLogger(Unigram.class);
	
	/** simple language model weight look-up */
	private HashMap<Tokenization, Double> weights = new HashMap<Tokenization, Double>();
	
	/**
	 * Initialize a new uni-gram language model for the given Tokenizer
	 * @param tok
	 */
	public Unigram(Tokenizer tok) {
		for (Tokenization t : tok.tokenizations)
			weights.put(t, 0.);
	}
	
	/**
	 * Set all ocurrances to 1, making resulting in a zero-gram (uniform word
	 * probabilities).
	 */
	public void makeZeroGram() {
		for (Map.Entry<Tokenization, Double> e : weights.entrySet())
			e.setValue(1.);
	}
	
	/**
	 * Estimate the language model weights.
	 */
	public void estimateWeights() throws LanguageModelException {
		double total = 0.;
		for (Map.Entry<Tokenization, Double> e : weights.entrySet()) {
			double n = e.getValue();
			if (n < 1.)
				logger.info("Unigram.estimateWeights(): " + e.getKey().toString() + " has less than 1 ocurrance!");
			total += n;
		}
		
		if (total == 0)
			throw new LanguageModelException("No ocurrances, no estimate!");
		
		for (Map.Entry<Tokenization, Double> e : weights.entrySet())
			e.setValue(e.getValue() / total);
	}
	
	/**
	 * Collect raw frequencies from the given tokenization
	 * @param sent
	 * @throws OutOfVocabularyException
	 */
	public void collectStats(List<Tokenization> sent) throws OutOfVocabularyException {
		for (Tokenization tok : sent) {
			if (!weights.containsKey(tok))
				throw new OutOfVocabularyException("Tokenization '" + tok + "' not in language model!");
			weights.put(tok, weights.get(tok) + 1.);
		}
	}
	
	/**
	 * Generate a string representation of the uni-gram language model, which is
	 * basically just the relative frequencies.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("lm.Unigram size=" + weights.keySet().size() + "\n");
		for (Map.Entry<Tokenization, Double> e : weights.entrySet())
			sb.append(e.getKey() + " " + e.getValue() + "\n");
		
		return sb.toString();
	}
	
	/**
	 * If desired, merge the LM scores for alternative pronunciations. This is
	 * usually used to add untranscribed pronunciation alternatives. The LM
	 * weight is distributed uniformly among the alternatives.
	 * @param ap List of alternative pronunciations
	 */
	public void mergeAlternativePronunciations(List<Tokenization> ap) {
		logger.fatal("Unigram.mergeAlternativePronunciations(): Method not implemented.");
	}
	
	/**
	 * For unigram, basically every word can follow another with the same
	 * possibility. Thus, we factor the language model probabilities once
	 * and make copies of the according tree. Silences are inserted with the
	 * given silprob.
	 * @param tree TokenTree of the available words (including silences)
	 * @param sil list of Tokenizations considered silence
	 * @param silprob total probability to insert silence (0...1), will be distributed among all symbols
	 */
	public TreeNode generateNetwork(TokenTree tree, List<Tokenization> sil, double silprob) 
		throws OutOfVocabularyException {
		// make a copy of the original tree
		TokenTree tt = tree.clone();
		
		// update the weights for all other symbols
		if (sil.size() > 0) {
			// compute the compensation
			double compensation = silprob;
			
			int n = 0;
			for (Tokenization si : sil) {
				if (weights.containsKey(si)) {
					compensation -= weights.get(si);
					n++;
				}
			}
			
			// distribute silprob equally
			silprob /= sil.size();
			
			// the compensation needs to be adjusted by the number of affected elements
			compensation /= (weights.size() - n);
			
			// go through the whole weights list
			for (Map.Entry<Tokenization, Double> e : weights.entrySet()) {
				if (sil.contains(e.getKey())) {
					// silence symbol, attribute share of silprob
					e.setValue(silprob);
					sil.remove(e.getKey());
				} else {
					// regular symbol, reduce weight to compensate for the add'l silprob
					e.setValue(e.getValue().doubleValue() - compensation);
				}
			}
		}
		
		// step 1: factor the uni-gram language model weights
		tt.factorLanguageModelWeights(weights);
		
		// step 2: build up the network by adding the global root as child to
		//         the leafs
		for (TreeNode n : tt.wordLeaves.values()) 
			n.children = new TreeNode [] { tt.root };
		
		return tt.root;
	}

	public static void main(String[] args) {
		
	}
}
