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

/**
 * The TreeNode class is the basic element of a TokenTree used for training
 * and decoding.
 * 
 * @author sikoried
 *
 */
public final class TreeNode {
	/** ID of the associated Tree (only set for root nodes) */
	public long treeId = -Integer.MAX_VALUE;
	
	/** distributed language model weight */
	public double p = 0.;
	
	/** factored language model weight (logarithmic) */
	public double f = 0.;
			
	/** Associated token */
	public Token token = null;
	
	/** If this is a leaf, there will be a word -- otherwise null */
	public Tokenization word = null;
	
	/** link to parent tree node */
	public TreeNode parent;
	
	/** list of children tree nodes */
	public TreeNode [] children = new TreeNode [0];
	
	/**
	 * Generate a new TreeNode associated with the given Token and linked to the
	 * given parent TreeNode.
	 * @param token
	 * @param parent
	 */
	public TreeNode(Token token, TreeNode parent) {
		this.token = token;
		this.parent = parent;
	}
	
	/**
	 * Generate a new TreeNode representing a word with the given language model
	 * probability and linked to the given parent node.
	 * @param parent
	 * @param word
	 * @param lmprob
	 */
	public TreeNode(TreeNode parent, Tokenization word, double lmprob) {
		this(null, parent);
		this.word = word;
		this.p = lmprob;
	}
	
	/**
	 * Generate a new root TreeNode and assign the respective tree id
	 * @param id ID of the respective tree
	 */
	public TreeNode(long id) {
		this(null, null);
		treeId = id;
	}
	
	/** 
	 * Determine the root node of this TreeNode
	 * @return corresponding root node
	 */
	public TreeNode root() {
		TreeNode n = this;
		while (!n.isRootNode())
			n = n.parent;
		return n;
	}
	
	/**
	 * Add the given TreeNode to the list of children.
	 * @param node node to insert
	 */
	public void addChild(TreeNode node) {
		TreeNode [] nc = new TreeNode [children.length + 1];
		System.arraycopy(children, 0, nc, 0, children.length); 
		nc[children.length] = node;
		children = nc;
		node.parent = this;
	}
	
	/**
	 * Set the given TreeNode as the only child.
	 * @param node
	 */
	public void setChild(TreeNode node) {
		if (children.length == 1)
			children[0] = node;
		else
			children = new TreeNode [] { node };
		
		node.parent = this;
	}
	
	/** 
	 * Add a lexical successor tree to the node.
	 * @param root
	 */
	public void addLst(TreeNode root) {
		TreeNode [] nc = new TreeNode [children.length + 1];
		System.arraycopy(children, 0, nc, 0, children.length); 
		nc[children.length] = root;
		children = nc;
	}
	
	/**
	 * Set the given lexical successor tree as the only child.
	 * @param root
	 */
	public void setLst(TreeNode root) {
		if (children.length == 1)
			children[0] = root;
		else
			children = new TreeNode [] { root };
	}
	
	/**
	 * Check if node is actually a word leaf
	 */
	public boolean isWordNode() {
		return word != null;
	}
	
	/**
	 * Check if node is a (dummy) root node
	 * @return
	 */
	public boolean isRootNode() {
		return parent == null || 
				token == null && word == null;
	}
	
	/** return a string representation of this node */
	public String toString() {
		if (isWordNode())
			return word.word;
		else if (token == null)
			return "[root id=" + treeId + "]";
		else
			return token.toString();
	}
}
