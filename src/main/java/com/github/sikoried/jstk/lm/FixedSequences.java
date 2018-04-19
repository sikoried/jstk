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
package com.github.sikoried.jstk.lm;

import com.github.sikoried.jstk.arch.*;
import com.github.sikoried.jstk.decoder.ViterbiBeamSearch;
import com.github.sikoried.jstk.exceptions.OutOfVocabularyException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.github.sikoried.jstk.decoder.ViterbiBeamSearch.Hypothesis;

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
	
	/** list of first LST in the sequences */
	private List<TokenTree> firstw = new LinkedList<TokenTree>();
	
	/** list of last LST in the sequences */
	private List<TokenTree> lastw = new LinkedList<TokenTree>();
	
	/** list of Token sequences for the allowed Tokenization sequences */
	private List<String []> seqs = new LinkedList<String []>();
	
	private int treeId = 0;
	
	private boolean force_keep_silences;
	
	
	/**
	 * Generate a new FixedSequences model based on the given TokenHierarchy
	 * @param th
	 */
	public FixedSequences(Tokenizer tok, TokenHierarchy th, String [] silences, 
			boolean force_keep_silences) 
		throws OutOfVocabularyException {
		this.tok = tok;
		this.th = th;
		this.silences = new HashSet<Tokenization>();
		for (String s : silences){
			if (s.isEmpty())
				continue;
			System.err.println("adding silence \"" + s + "\"");
			this.silences.add(tok.getWordTokenization(s));
		}
		this.force_keep_silences = force_keep_silences;
	}
	
	/**
	 * Convenience shortcut for FixedSequences(..., false)
	 */
	public FixedSequences(Tokenizer tok, TokenHierarchy th, String [] silences) throws OutOfVocabularyException{
		this(tok, th, silences, false);	
	}
	
	/**
	 * Add a Tokenization sequence to the list of possible decoding roots
	 * @param transcription sequence of words separated by whitespace
	 */
	public void addSequence(String transcription) 
		throws OutOfVocabularyException {
		// tokenize the transcription
		String [] tok = transcription.trim().split("\\s+");
		
		// remember token sequence
		seqs.add(tok);

		// now build word trees and link correspondingly
		TokenTree prev = null;
		for (int i = 0; i < tok.length; ++i) {
			
			// (unless force_keep_silences is true:)
			// ignore silence tokens, these will be treated in a special way
			if (!force_keep_silences && silences.contains(new Tokenization(tok[i])))
				continue;
			
			// build word tree
			TokenTree tree = new TokenTree(treeId++);
			Tokenization t = this.tok.getWordTokenization(tok[i]);
			tree.addToTree(t, th.tokenizeWord(t.sequence), 1.f);
						
			if (prev != null) {
				// build silence tree with optional silences
				TokenTree silt = new TokenTree(treeId++);
				for (Tokenization s : silences)
					silt.addToTree(s, th.tokenizeWord(s.sequence), 1.f / silences.size()).setLst(tree.root);
				
				// link the previous words to the silence and the new tree
				for (TreeNode n : prev.leaves()) {
					if (!silences.contains(new Tokenization(tok[i - 1])) &&
					    !silences.contains(new Tokenization(tok[i])) &&
					    // FIXME: this still doesn't prevent that at the end, two consecutive silences can occur.
					    (i == tok.length - 1 ||
					     !silences.contains(new Tokenization(tok[i + 1]))))					    
						n.setLst(silt.root);
					n.addLst(tree.root);
				}
			} else {
				// this was the first tree, so remember the root
				firstw.add(tree);
			}
			
			prev = tree;
		}
		
		lastw.add(prev);
	}
	
	/**
	 * Generate the fixed network for beam forced alignment.
	 */
	public TreeNode generateNetwork() {
		// beginning LST (silence + first words
		TokenTree begs = new TokenTree(treeId++);
		for (Tokenization s : silences)
			begs.addToTree(s, th.tokenizeWord(s.sequence), 1.f / silences.size());
		
		// link in the initial words of the sequences
		for (TokenTree fw : firstw)
			for (TreeNode n : fw.root.children)
				begs.root.addChild(n);
				
		// ending silence
		TokenTree ends = new TokenTree(treeId++);
		for (Tokenization s : silences)
			ends.addToTree(s, th.tokenizeWord(s.sequence), 1.f / silences.size());
		
		// link all last words of the sequences to the silence
		for (TokenTree lw : lastw) {
			for (TreeNode l : lw.leaves())
				l.addLst(ends.root);
		}
		
		return begs.root;
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
				List<String> cleanr = new LinkedList<String>();
				for (String t : ref) 
					if (!silences.contains(new Tokenization(t)))
						cleanr.add(t);
				
				List<String> cleanh = new LinkedList<String>();
				for (Hypothesis hi : cand)
					if (!silences.contains(hi.node.word))
						cleanh.add(hi.node.word.word);
		
				if (cleanh.size() != cleanr.size())
					continue;
				
				boolean match = true;
				Iterator<String> cri = cleanr.iterator();
				Iterator<String> chi = cleanh.iterator();
				while (cri.hasNext() && chi.hasNext() && match)
					match &= cri.next().equals(chi.next());
			
				if (match)
					return h;
			}
		}
		
		return null;
	}
}
