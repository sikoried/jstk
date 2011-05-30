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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import de.fau.cs.jstk.arch.TokenHierarchy;
import de.fau.cs.jstk.arch.TokenTree;
import de.fau.cs.jstk.arch.Tokenization;
import de.fau.cs.jstk.arch.Tokenizer;
import de.fau.cs.jstk.arch.TreeNode;
import de.fau.cs.jstk.decoder.ViterbiBeamSearch.Hypothesis;
import de.fau.cs.jstk.exceptions.OutOfVocabularyException;

/**
 * The FixedSequences language model allows to generate decoder networks which 
 * only allow certain sequences of words. This is useful to do a "beam" forced
 * alignment of large files.
 * 
 * @author sikoried
 */
public class FixedSequences implements LanguageModel {
	/** Tokenizer to get the token sequences */
	private Tokenizer tok;
	
	/** TokenHierarchy to construct the nodes from */
	private TokenHierarchy th;
	
	/** list of used silence tokens (used to match hypotheses to allowed sequences) */
	private HashSet<Tokenization> silences;
	
	/** list of root nodes for the possible sequences */
	private List<TreeNode> roots = new LinkedList<TreeNode>();
	
	/** list of Token sequences for the allowed Tokenization sequences */
	private List<String []> seqs = new LinkedList<String []>();
	
	private int treeId = 0;
	
	/**
	 * Generate a new FixedSequences model based on the given TokenHierarchy
	 * @param th
	 */
	public FixedSequences(Tokenizer tok, TokenHierarchy th, String [] silences) 
		throws OutOfVocabularyException {
		this.tok = tok;
		this.silences = new HashSet<Tokenization>();
		for (String s : silences)
			this.silences.add(tok.getWordTokenization(s));
	}
	
	/**
	 * Add a Tokenization sequence to the list of possible decoding roots
	 * @param transcription sequence of words separated by whitespace
	 * @param silences whitespace separated list of silence symbols
	 */
	public void addSequence(String transcription) 
		throws OutOfVocabularyException {
		// remove silence words, if present! otherwise they would be required
		for (Tokenization s : silences)
			transcription = transcription.replaceAll("\\b" + s.word + "\\b", "");
		
		// tokenize the transcription
		String [] tok = transcription.trim().split("\\s+");
		
		// remember token sequence
		seqs.add(tok);

		// now build word trees and link correspondingly
		TokenTree prev = null, init = null;
		for (int i = 0; i < tok.length; ++i) {
			// build silence tree with optional silences
			TokenTree tree = new TokenTree(treeId++);
			for (Tokenization s : silences)
				tree.addToTree(s, th.tokenizeWord(s.sequence), 0.5 / silences.size());
			
			// build word tree
			Tokenization t = this.tok.getWordTokenization(tok[i]);
			tree.addToTree(t, th.tokenizeWord(t.sequence), 0.5);
			
			// factorize
			tree.factorLanguageModelWeights();
			
			// now link the silences as a loop
			for (TreeNode n : tree.leaves())
				if (silences.contains(n.word))
					n.setLst(tree.root);
			
			// link the previous words to the new tree
			if (prev != null)
				for (TreeNode n : tree.leaves())
					if (!silences.contains(n.word))
						n.setLst(tree.root);
			else
				init = tree;
			
			prev = tree;
		}
		
		// build trailing silence tree with optional silences
		TokenTree trail = new TokenTree(treeId++);
		for (Tokenization s : silences)
			trail.addToTree(s, th.tokenizeWord(s.sequence), 1. / silences.size());
		
		for (TreeNode n : prev.leaves())
			if (!silences.contains(n.word))
				n.setLst(trail.root);
		
		// add the initial root to the sequence list
		roots.add(init.root);
	}
	
	/**
	 * Find the best Hypothesis for the allowed sequences
	 * @param list sorted list of Hypotheses (typically a surviving beam)
	 * @return null if no match
	 */
	public Hypothesis findBestForcedAlignment(List<Hypothesis> list) {
		// check all hypotheses
		for (Hypothesis h : list) {
			List<Hypothesis> cand = h.extractWords();
			
			// see if the tokenization matches -- the first match we get 
			// will be the best due to the sorting
			for (int i = 0; i < seqs.size(); ++i) {
				String [] ref = seqs.get(i);
				
				// sanitize the candidate word sequence by removing the adequate silences
				List<String> clean = new LinkedList<String>();
				for (Hypothesis hi : cand)
					if (!silences.contains(hi.node.word))
						clean.add(hi.node.word.word);
								
				if (clean.size() != ref.length)
					continue;
				
				String [] can = clean.toArray(new String [clean.size()]);
				boolean match = true;
				for (int j = 0; j < ref.length && j < can.length && match; ++j) {
					match &= can[j].equals(ref[j]);
				}
			
				if (match)
					return h;
			}
		}
		
		return null;
	}
	
	/**
	 * Generate the fixed network for beam forced alignment.
	 * @param tokenizer (null; all info is already present)
	 * @param hierarchy (null; all info is already present)
	 * @param sils (null; all info is already present)
	 */
	public TreeNode generateNetwork() {
		
		TreeNode root = new TreeNode(treeId++);
		
		for (TreeNode r : roots) {
			for (TreeNode c : r.children)
				root.addChild(c);
		}
		
		return root;
	}
}
