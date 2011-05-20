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
package de.fau.cs.jstk.lm;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import de.fau.cs.jstk.arch.TokenTree;
import de.fau.cs.jstk.arch.TokenTree.TreeNode;
import de.fau.cs.jstk.arch.Tokenization;
import de.fau.cs.jstk.arch.Tokenizer;
import de.fau.cs.jstk.exceptions.OutOfVocabularyException;

/**
 * The Srilm class imports statistical SRILM language models. Up to now, only
 * the statistical n-grams are supported. Unfortunately, we need the "real" 
 * probabilities, so they are Math.pow(10, logprob)'ed when read from file.
 * 
 * http://www.speech.sri.com/projects/srilm/
 * 
 * @author sikoried
 */
public class Srilm extends LanguageModel {
	/** logger instance */
	private static Logger logger = Logger.getLogger(Srilm.class);
	
	/** default n-gram size for network generation */
	private int defn = 2;
	
	/** Tokenizer to prevent OOV information */
	private Tokenizer tok;
	
	/** index ngram->Ngram-instance */
	private HashMap<String, Ngram> ngrams = new HashMap<String, Ngram>();
	
	public Srilm(Tokenizer tok, BufferedReader br) throws IOException, OutOfVocabularyException {
		this.tok = tok;

		String lin;
		int i = 0;
		int cn = 0;
		while ((lin = br.readLine()) != null) {
			lin = lin.trim();
			// increment line counter
			i++;
			
			// ignore line?
			if (lin.length() < 1) {
				logger.info("ignoring empty line " + i);
				continue;
			}
			
			// there are a couple of special lines we can read
			if (lin.equals("\\data\\")) {
				logger.info("reading \\data\\ section");
				continue;
			} else if (lin.startsWith("ngram ")) {
				logger.info(lin);
				continue;
			} else if (lin.startsWith("\\") && lin.endsWith("grams:")) {
				String context = lin.substring(1, lin.length()-7);
				logger.info("reading n-grams n=" + context);
				cn = Integer.parseInt(context);
				continue;
			} else if (lin.equals("\\end\\")) {
				logger.info("found \\end\\ tag!");
				break;
			}
			// we skip <s> and </s>
			if (lin.contains("<s>") || lin.contains("</s>")) {
				logger.info("ignoring sos/eos");
				continue;
			}
			
			// regular n-gram line!
			String [] spl = lin.split("\\s+");
			if (spl.length < (cn + 1) || spl.length > (cn + 2)) {
				logger.error("ignoring malformed line " + i + " -- error in LM format?");
				continue;
			}
			
			// get probability
			double prob = (spl[0].equals("99") ? -Double.MAX_VALUE : Math.pow(10, Double.parseDouble(spl[0])));
			
			// validate all words
			Tokenization [] tn = new Tokenization[cn];
			for (int j = 1; j < (cn + 1); ++j)
				tn[j-1] = tok.getWordTokenization(spl[j]);
			
			// back-off weight?
			double bow = Double.NaN;
			if (spl.length == (1 + cn + 1))
				bow = (spl[cn+1].equals("99") ? -Double.MAX_VALUE : Math.pow(10, Double.parseDouble(spl[cn+1])));
			
			// allocate and insert ngram
			Ngram ngram = new Ngram(tn, prob, bow);
			ngrams.put(ngram.toString(), ngram);
		}
	}
		
	/**
	 * The Ngram class represents the 
	 * @author sikoried
	 *
	 */
	private class Ngram {
		Tokenization [] ngram;
		
		/** n-gram probability (NOT log) */
		double prob;
		
		/** n-gram back-off weight (NOT log) */
		double bow;
		
		Ngram(Tokenization [] ngram, double prob, double bow) {
			this.ngram = ngram;
			this.prob = prob;
			this.bow = bow;
		}
		
		public boolean equals(Object o) {
			if (o instanceof Ngram)
				return equals((Ngram) o);
			else return false;
		}
		
		public boolean equals(Ngram o) {
			if (ngram.length != o.ngram.length)
				return false;
			for (int i = 0; i < ngram.length; ++i)
				if (!ngram[i].word.equals(o.ngram[i].word))
					return false;
			return true;
		}
		
		public String toString() {
			if (ngram.length == 1)
				return ngram[0].word;
			else if (ngram.length == 2)
				return ngram[0].word + " " + ngram[1].word;
			else {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < ngram.length; ++i) {
					sb.append(ngram[i].word);
					if (i == ngram.length - 1)
						break;
					sb.append(" ");
				}
				return sb.toString();
			}
		}
	}
	
	/**
	 * Generate the decoding network using the given silences and silprob
	 */
	public TreeNode generateNetwork(TokenTree tree, HashMap<Tokenization, Double> sils) 
		throws OutOfVocabularyException {
		logger.info("construct initial word tree");
		// make a copy of the original tree
		TokenTree tt = tree.clone();
		
		// step 1: factor the uni-gram language model weights
		HashMap<Tokenization, Double> weights = new HashMap<Tokenization, Double>();
		
		// put weights for silences
		weights.putAll(sils);

		// unigram weights
		for (Tokenization t : tok.tokenizations) {
			// we have fixed weights for silences
			if (sils.containsKey(t))
				continue;

			// we need an observed unigram!
			Ngram n = ngrams.get(t.word);
			double prob;
			if (n != null)
				prob = n.prob;
			else {
				logger.info("Putting OOV probability for Tokenization " + t);
				prob = Double.MIN_VALUE;
			}
			
			weights.put(t, prob);
		}
		
		logger.info("factoring LM weights");
		tt.factorLanguageModelWeights(weights);
		
		// -%<------------------------------------------------------------------
		// actual network generation 
		// -%<------------------------------------------------------------------
		
		if (defn == 1) {
			logger.info("building uni-gram LM network");
			// this is easy: just link all the leaves with the root node again!
			for (TreeNode n : tt.wordLeaves.values()) 
				n.children = new TreeNode [] { tt.root };
		} else if (defn == 2){
			// bi-gram model with optional silences!
			logger.info("building bi-gram LM network");
			HashMap<Tokenization, TokenTree> bt = new HashMap<Tokenization, TokenTree>();
			for (TreeNode nd : tt.wordLeaves.values()) {
				// initial silence, so just loop!
				if (sils.containsKey(nd.word)) {
					nd.children = new TreeNode [] { tt.root };
				} else {
					// bi-gram weights
					for (Tokenization t : tok.tokenizations) {
						// we have fixed weights for silences 
						if (sils.containsKey(t))
							continue;
						
						// we need an observed unigram!
						String bigram = nd.word.word + " " + t.word;
						double prob;
						
						Ngram n = ngrams.get(bigram);
						if (n == null) {
							Ngram n1 = ngrams.get(nd.word.word);
							Ngram n2 = ngrams.get(t.word);
							
							double n1bow = (n1 == null ? Double.MIN_VALUE : n1.bow);
							double b2prob = (n2 == null ? Double.MIN_VALUE : n2.prob);
							
							prob = n1bow * b2prob;
						} else
							prob = n.prob;
						
						weights.put(t, prob);
					}
					
					// construct LST and factor LM weights
					TokenTree lst = tree.clone();
					lst.factorLanguageModelWeights(weights);
					nd.children = new TreeNode [] { lst.root };
					
					// loop silences
					for (Tokenization to : sils.keySet()) {
						TreeNode n = lst.wordLeaves.get(to.word);
						n.children = new TreeNode [] { lst.root };
					}
					
					// remember this LST
					bt.put(nd.word, lst);
				}
			}
			
			// finish up the linking
			for (TokenTree lst : bt.values())
				for (TreeNode n : lst.wordLeaves.values())
					n.children = new TreeNode [] { bt.get(n.word).root };
		}
		
		return tt.root;
	}
	
	
	
	public void setDefaultNgram(int n) {
		defn = n;
	}
	
	/**
	 * @param args
	 */
	public static void main(String [] args) {
		// TODO Auto-generated method stub

	}
}
