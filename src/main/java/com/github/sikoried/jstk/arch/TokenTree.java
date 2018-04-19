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
package com.github.sikoried.jstk.arch;

import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;


/**
 * The TokenTree is made up by TreeNodes and is used for decoder network
 * generation.
 *  
 * @author sikoried
 */
public class TokenTree {
	public int id = 0;
	
	/** The root of the tree to reach all words */
	public TreeNode root;
	
	/**
	 * Generate a new TokenTree for the given root node.
	 * @param id (unique) ID of the new tree
	 */
	public TokenTree(int id) {
		this(id, new TreeNode(null, null));
	}
	
	/**
	 * Generate a new TokenTree for the given root node.
	 * @param id (unique) ID of the new tree
	 * @param root null for an empty tree
	 */
	public TokenTree(int id, TreeNode root) {
		this.root = root;
		setId(id);
	}
		
	/**
	 * Add a token sequence (i.e. a word) to the token tree
	 * @param word Tokenization
	 * @param sequence Token sequence to represent the Tokenisation
	 * @param prob language model probability
	 * @return leaf node of the newly inserted trace
	 */
	public TreeNode addToTree(Tokenization word, Token [] sequence, float prob) {
		TreeNode target = root, inspect = root;
		for (Token tok : sequence) {
			// check if there is a matching token on this level
			for (int i = 0; i < inspect.children.length; ++i) {
				if (!inspect.children[i].isWordNode() && inspect.children[i].token.equals(tok)) {
					target = inspect.children[i];
					break;
				}
			}
			
			// no, we need a new branch!
			if (target == inspect) {
				target = new TreeNode(tok, inspect);
				inspect.addChild(target);
			}
			
			inspect = target;
		}

		// add the leaf
		TreeNode child = new TreeNode(target, word, prob);
		target.addChild(child);
		
		return child;
	}
	
	/**
	 * Set the tree id and upate all nodes
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
		if (root != null)
			root.treeId = id;
	}
	
	/**
	 * Get the size (node count) of the tree.
	 * @return
	 */
	public int size() {
		if (root == null)
			return 0;
		
		int size = 0;
		
		Stack<TreeNode> agenda = new Stack<TreeNode>();
		agenda.add(root);
		
		// depth first search
		while (agenda.size() > 0) {
			size++;
			TreeNode c = agenda.pop();
			for (TreeNode t : c.children)
				agenda.push(t);
		}
		
		return size;
	}
	
	/**
	 * Get a list of all TreeNodes (including leaves) in a DFS manner.
	 * @return
	 */
	public List<TreeNode> nodes() {
		return dfs();
	}
	
	/**
	 * Get a DFS list of all nodes.
	 * @return
	 */
	public List<TreeNode> dfs() {
		LinkedList<TreeNode> li = new LinkedList<TreeNode>();
		if (root == null)
			return li;
		
		Stack<TreeNode> agenda = new Stack<TreeNode>();
		agenda.add(root);
		
		// depth first search
		while (agenda.size() > 0) {
			TreeNode c = agenda.pop();
			
			li.addFirst(c);
			
			for (TreeNode t : c.children)
				agenda.push(t);
		}
		
		return li;
	}
	
	/**
	 * Get a BFS list of all nodes.
	 * @return
	 */
	public List<TreeNode> bfs() {
		LinkedList<TreeNode> li = new LinkedList<TreeNode>();
		if (root == null)
			return li;
		
		Stack<TreeNode> agenda = new Stack<TreeNode>();
		agenda.add(root);
		
		// depth first search
		while (agenda.size() > 0) {
			TreeNode c = agenda.pop();
			
			li.add(c);
			
			for (TreeNode t : c.children)
				agenda.push(t);
		}
		
		return li;
	}
	
	/**
	 * Get a list of leaves (ignores LST links!)
	 * @return
	 */
	public List<TreeNode> leaves() {
		LinkedList<TreeNode> li = new LinkedList<TreeNode>();
		if (root == null)
			return li;
		
		Stack<TreeNode> agenda = new Stack<TreeNode>();
		agenda.add(root);
		
		// depth first search
		while (agenda.size() > 0) {
			TreeNode c = agenda.pop();
			
			if (c.isWordNode())
				li.add(c);
			
			for (TreeNode t : c.children) {
				if (!t.isRootNode())
					agenda.push(t);
			}
		}
		
		return li;
	}
	
	/**
	 * Return a String representation of the lexical tree.
	 */
	public String toString() {
		return "LexicalTree: " + leaves().size() + " words using " + size() + " nodes";
	}
	
	public String treeAsString(String indent) {
		StringBuffer sb = new StringBuffer();
		LinkedList<Pair<Integer, TreeNode>> agenda = new LinkedList<Pair<Integer, TreeNode>>();
		agenda.add(Pair.of(0, root));

		// depth first search
		while (agenda.size() > 0) {
			Pair<Integer, TreeNode> pair = agenda.remove(agenda.size() - 1);

			// do correct indent
			for (int i = 0; i < pair.getLeft(); ++i)
				sb.append(indent);

			// print current
			sb.append(pair.getRight().toString());

			// add the children
			for (TreeNode child : pair.getRight().children)
				agenda.add(Pair.of(pair.getLeft() + 1, child));

			// finish line
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Build up a map linking Tokenizations to the respective word leaf.
	 * @return
	 */
	public HashMap<Tokenization, TreeNode> generateLeafMap() {
		HashMap<Tokenization, TreeNode> m = new HashMap<Tokenization, TreeNode>();
		
		for (TreeNode l : leaves())
			m.put(l.word, l);
		
		return m;
	}
	
	/**
	 * Generate a trace of Tokens that lead to the given leaf
	 * @param leaf
	 * @return
	 */
	public static List<Token> trace(TreeNode leaf) {
		LinkedList<Token> li = new LinkedList<Token>();
		
		TreeNode it = leaf.parent;
		while (!it.isRootNode())
			li.addFirst(it.token);
		
		return li;
	}
	
	/**
	 * Compute the distributed and factored language model weights for this 
	 * lexical tree using the given word probability list. Make sure this 
	 * tree is not part of a network, as recursiveness is NOT checked.
	 */
	public void factor() {
		List<TreeNode> dfs = dfs();
		
		// distribute probs
		for (TreeNode n : dfs) {
			if (n.isWordNode())
				continue;
			
			n.f = 0.f;
			for (TreeNode c : n.children)
				n.f += c.f;
		}
		
		// factorize
		for (TreeNode n : dfs) {
			// factorization is not applied at the leaf level!
			if (n.isWordNode())
				continue;
			
			// c.f = Math.log(c.f - n.f)			
			float logsum = (float) Math.log(n.f);			
			for (TreeNode c : n.children)
				c.f = (float) Math.log(c.f) - logsum;
		}
		
		// for consistency
		root.f = 0.f;
	}
	
	/** 
	 * Traverse a network of TokenTree's to visualize the decoding network. This
	 * makes sure that no subtree is printed twice.
	 * @param root
	 * @return
	 */
	public static String traverseNetwork(TreeNode root, String indent) {
		StringBuffer sb = new StringBuffer();
		NumberFormat fmt = new DecimalFormat("#0.0000");
		
		HashSet<TreeNode> visited = new HashSet<TreeNode>();
		Stack<Pair<TreeNode, Integer>> agenda = new Stack<Pair<TreeNode, Integer>>();
		
		// the top agenda is for LST roots
		agenda.add(Pair.of(root, 0));
		while (agenda.size() > 0) {
			// get next tree
			Pair<TreeNode, Integer> p = agenda.pop();
			
			// mark as visited
			visited.add(p.getLeft());
			
			// now explore the tree with a local agenda
			Stack<Pair<TreeNode, Integer>> local = new Stack<Pair<TreeNode, Integer>>();
			local.add(p);
			
			while (local.size() > 0) {
				Pair<TreeNode, Integer> l = local.pop();
				
				// do correct indent
				for (int i = 0; i < l.getRight(); ++i)
					sb.append(indent);
			
				// print TreeNode
				sb.append(l.getLeft() + " F=" + fmt.format(l.getLeft().f));
				if (l.getLeft().isWordNode()) {
					for (TreeNode c : l.getLeft().children) {
						sb.append(" LST=" + c);
					}
				}
			
				sb.append("\n");
			
				// add children
				for (TreeNode n : l.getLeft().children) {
					if (n.isRootNode()) {
						// LSTs are treatet separately
						if (!visited.contains(n))
							agenda.push(Pair.of(n, p.getRight()));
					} else
						local.push(Pair.of(n, l.getRight() + 1));
				}
			}
		}
		
		return sb.toString();
	}
	
	public static final String SYNOPSIS = 
		"sikoried, 3/6/2010\n\n" +
		"Construct and/or view a token tree that is required for training and recognition.\n\n" +
		"usage: arch.TokenTree config [options]\n" +
		"    Load configuration and build up TokenTree\n" +
		"\n" +
		"  --list-words\n" +
		"    List all the words and their transcriptions (in alphabetical order)";
	
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		Configuration conf = new Configuration(new File(args[0]));
		
		boolean listWords = false;

		LinkedList<String> findWord = new LinkedList<String>();
		LinkedList<String> compSent = new LinkedList<String>();
		
		for (int i = 1; i < args.length; ++i) {
			if (args[i].equals("--list-words"))
				listWords = true;
			else if (args[i].equals("--find")) {
				String [] words = args[++i].split(",");
				for (String w : words)
					findWord.add(w);
			} else if (args[i].equals("--compose-sentence"))
				compSent.add(args[++i]);
			else
				throw new IOException("Invalid argument \"" + args[i] + "\"");
		}
		
		if (!conf.hasAlphabet() || !conf.hasTokenizer() || !conf.hasTokenHierarchy())
			throw new Exception("Config does not provide Alphabet+Tokenizer+Hierarchy");
		
		if (listWords) {
			List<TreeNode> leafs = conf.tt.leaves();
			for (TreeNode n : leafs) {
				System.out.print(n.word);
				LinkedList<Token> toreverse = new LinkedList<Token>();
				toreverse.add(n.token);
				while (n.parent != null && n.parent.token != null) {
					n = n.parent;
					toreverse.add(n.token);
				}
				while (toreverse.size() > 0)
					System.out.print(" " + toreverse.remove(toreverse.size() - 1));
				System.out.println();
			}
		}
	}
}
