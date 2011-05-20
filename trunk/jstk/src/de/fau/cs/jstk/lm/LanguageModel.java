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

import de.fau.cs.jstk.arch.TokenTree;
import de.fau.cs.jstk.arch.TokenTree.TreeNode;
import de.fau.cs.jstk.arch.Tokenization;
import de.fau.cs.jstk.exceptions.OutOfVocabularyException;


/**
 * The abstract LanguageModel enforces the generateNetwork function and provides
 * methods to reduce the network size.
 * 
 * @author sikoried
 */
public abstract class LanguageModel {
	/**
	 * Generate the LST network from the given TokenTrees and build silence
	 * models as requested
	 * @param tree
	 * @param sil list of Tokenizations considered silence
	 * @return Root node of the LST network
	 */
	public abstract TreeNode generateNetwork(TokenTree tree, HashMap<Tokenization, Double> sils) 
		throws OutOfVocabularyException;
	
	/**
	 * Reduce the given network by removing isomorphic subtrees 
	 * @param root
	 * @return
	 */
	public TreeNode reduceNetwork(TreeNode root) {
		return null;
	}
}
