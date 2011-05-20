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
import java.util.LinkedList;
import java.util.List;

import de.fau.cs.jstk.arch.TokenTree;
import de.fau.cs.jstk.arch.TokenTree.TreeNode;
import de.fau.cs.jstk.arch.Tokenization;
import de.fau.cs.jstk.decoder.ViterbiBeamSearch.Hypothesis;
import de.fau.cs.jstk.exceptions.OutOfVocabularyException;

/**
 * The FixedSequences language model allows to generate decoder networks which 
 * only allow certain sequences of words. This is useful to do a "beam" forced
 * alignment of large files.
 * 
 * @author sikoried
 */
public class FixedSequences extends LanguageModel {

	/** TokenHierarchy to construct the nodes from */
	private TokenTree tt = null;
	
	/** list of root nodes for the possible sequences */
	private List<TreeNode> roots = new LinkedList<TreeNode>();
	
	/** list of Token sequences for the allowed Tokenization sequences */
	private List<String []> seqs = new LinkedList<String []>();
	
	/** list of used silence tokens (used to match hypotheses to allowed sequences) */
	private List<String []> sils = new LinkedList<String []>();
	
	/**
	 * Generate a new FixedSequences model based on the given TokenHierarchy
	 * @param th
	 */
	public FixedSequences(TokenTree tt) {
		this.tt = tt;
	}
	
	/**
	 * Add a Tokenization sequence to the list of possible decoding roots
	 * @param transcription sequence of words separated by whitespace
	 * @param silences whitespace separated list of silence symbols
	 */
	public void addSequence(String transcription, String silences) 
		throws OutOfVocabularyException {
		
		// tokenize the silences and retrieve transcription
		String [] sils = silences.trim().split("\\s+");
		
		// remove silence words, if present! otherwise they would be required
		for (String s : sils)
			transcription = transcription.replaceAll("\\b" + s + "\\b", "");
		
		// tokenize the transcription
		String [] tok = transcription.trim().split("\\s+");
		
		// remember token sequence
		seqs.add(tok);
		
		// remember possible silences
		this.sils.add(sils);
		
		// initial silence tree
		long treeid = 0;
		TokenTree init = new TokenTree(tt.tokenHierarchy, tt.tokenizer, false);
		init.setId(treeid++);
		for (String s : sils) {
			Tokenization t = tt.tokenizer.getWordTokenization(s);
			init.addToTree(t, tt.tokenHierarchy.tokenizeWord(t.sequence));
		}

		// now build word trees and link correspondingly
		TokenTree prev = init;
		for (int i = 0; i < tok.length; ++i) {
			// build silence tree with optional silences
			TokenTree st = new TokenTree(tt.tokenHierarchy, tt.tokenizer, false);
			if (prev != init) {
				st.setId(treeid++);
				
				for (String s : sils) {
					Tokenization ts = tt.tokenizer.getWordTokenization(s);
					st.addToTree(ts, tt.tokenHierarchy.tokenizeWord(ts.sequence));
				}
			}
			
			// build word tree
			TokenTree wt = new TokenTree(tt.tokenHierarchy, tt.tokenizer, false);
			wt.setId(treeid++);
			Tokenization t = tt.tokenizer.getWordTokenization(tok[i]);
			wt.addToTree(t, tt.tokenHierarchy.tokenizeWord(t.sequence));
			
			// link the previous tree to the word and attach silence model
			for (TreeNode n : prev.wordLeaves.values()) {
				n.addLST(wt.root);
				if (prev != init)
					n.addLST(st.root);
			}
			
			for (TreeNode n : st.wordLeaves.values())
				n.addLST(wt.root);
			
			prev = wt;
		}
		
		// build trailing silence tree with optional silences
		TokenTree trail = new TokenTree(tt.tokenHierarchy, tt.tokenizer, false);
		trail.setId(treeid++);
		for (String s : sils) {
			Tokenization ts = tt.tokenizer.getWordTokenization(s);
			trail.addToTree(ts, tt.tokenHierarchy.tokenizeWord(ts.sequence));
		}
		
		for (TreeNode n : prev.wordLeaves.values())
			n.addLST(trail.root);
		
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
				String [] sil = sils.get(i);
				
				// sanitize the candidate word sequence by removing the adequate silences
				List<String> clean = new LinkedList<String>();
				for (Hypothesis hi : cand) {
					boolean si = false;
					String w = hi.node.word.word;
					for (String s : sil)
						if (w.equals(s))
							si = true;
					if (!si)
						clean.add(w);
				}
								
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
	 * @param tree (null; all info is already present)
	 * @param sils (null; all info is already present)
	 */
	public TreeNode generateNetwork(TokenTree tree, HashMap<Tokenization, Double> sils) 
		throws OutOfVocabularyException {
		
		TreeNode root = new TreeNode(null, null);
		
		for (TreeNode r : roots) {
			for (TreeNode c : r.children)
				root.addChild(c);
		}
		
		return root;
	}
}
