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

import java.util.HashMap;
import java.util.Map;

import com.github.sikoried.jstk.arch.*;


/**
 * The zero-gram can be used to enforce a strictly acoustic decoding, as there
 * is literally no language model weight attached.
 * 
 * @author sikoried
 *
 */
public class Zerogram implements LanguageModel {
	private Tokenizer tok;
	private TokenHierarchy th;
	private HashMap<Tokenization, Float> sils;
	
	/**
	 * Generate a new Zerogram for all words in the given Tokenizer. The words
	 * are uniformly weighted after subtraction of the silence probability mass.
	 * 
	 * @param tokenizer
	 * @param hierarchy
	 * @param sils
	 */
	public Zerogram(Tokenizer tokenizer, TokenHierarchy hierarchy, HashMap<Tokenization, Float> sils) {
		this.tok = tokenizer;
		this.th = hierarchy;
		this.sils = sils;
	}
	
	public TreeNode generateNetwork() {
		float unif = 1.f;
		for (Map.Entry<Tokenization, Float> e : sils.entrySet()) 
			unif -= e.getValue();
		
		unif /= (tok.tokenizations.size() - sils.size());
		
		// build lexical tree
		TokenTree tree = new TokenTree(0);
		for (Tokenization t : tok.tokenizations) {
			if (sils.containsKey(t))
				tree.addToTree(t, th.tokenizeWord(t.sequence), sils.get(t));
			else
				tree.addToTree(t, th.tokenizeWord(t.sequence), unif);
		}
		
		// factor language model weights
		tree.factor();
		
		// loop
		for (TreeNode n : tree.leaves())
			n.setLst(tree.root);
		
		return tree.root;
	}
}
