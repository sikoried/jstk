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

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.BasicConfigurator;

import de.fau.cs.jstk.exceptions.OutOfVocabularyException;
import de.fau.cs.jstk.util.Pair;


/**
 * The TokenTree stores the compiled Tokenizer/TokenHierarchy. The tree is 
 * organized by token prefixes and used for both training and recognition.
 *  
 * @author sikoried
 */
public class TokenTree {
	public long id = 0;
	
	/**
	 * Helper class to build up a tree. Each node stores the token (for the 
	 * statistical parameters), its parent and children. If the end of the word
	 * is reached, the lexical entry is non-null.
	 * 
	 * @author sikoried
	 *
	 */
	public static final class TreeNode {
		/** ID of the associated Tree (only set for root nodes) */
		public long treeId;
		
		/** distributed language model weight */
		public double p = 0.;
		
		/** factored language model weight (logarithmic) */
		public double f = 0.;
				
		/** Associated token */
		public Token token = null;
		
		/** If this is a leaf, there will be a word -- otherwise null */
		public Tokenization word = null;
		
		public TreeNode parent;
		public TreeNode [] children = new TreeNode [0];
		
		public TreeNode(Token phone, TreeNode parent, long id) {
			this(phone, parent);
			treeId = id;
		}
		
		public TreeNode(Token phone, TreeNode parent) {
			this.token = phone;
			this.parent = parent;
		}
		
		public boolean equals(TreeNode n) {
			return n == this;
		}
		
		/** 
		 * Determine the start node for this word (required at training time)
		 * @return origin of this node
		 */
		public TreeNode startNode() {
			TreeNode n = this;
			while (!n.isRootNode())
				n = n.parent;
			return n;
		}
		
		/**
		 * Return the ID of the tree this node belongs to
		 * @return
		 */
		public long getTreeId() {
			return treeId;
		}
		
		/**
		 * Add the given TreeNode to the list of children.
		 * @param node Node to insert
		 * @return inserted node
		 */
		public TreeNode addChild(TreeNode node) {
			TreeNode [] nc = new TreeNode [children.length + 1];
			System.arraycopy(children, 0, nc, 0, children.length); 
			nc[children.length] = node;
			children = nc;
			
			// ensure proper parent link
			node.parent = this;
			return node;
		}
		
		/**
		 * Add a LST to the list of children (the parent is not updated!)
		 * @param root
		 */
		public void addLST(TreeNode root) {
			TreeNode [] nc = new TreeNode [children.length + 1];
			System.arraycopy(children, 0, nc, 0, children.length); 
			nc[children.length] = root;
			children = nc;
		}
		
		/**
		 * Remove all children and set the given node as new child
		 * @param node
		 */
		public void setChild(TreeNode node) {
			children = new TreeNode [] { node };
		}
		
		/**
		 * Remove specified child==node
		 * @param node
		 * @return true on success
		 */
		public boolean removeChild(TreeNode node) {
			if (token == null)
				return false;
			
			// locate child
			int ndx = -1;
			for (int i = 0; i < children.length; ++i) {
				if (children[i].equals(node)) {
					ndx = i;
					break;
				}
			}
			
			if (ndx == -1)
				return false;
			
			TreeNode [] nc = new TreeNode[children.length - 1];
			int k = 0;
			for (int j = 0; j < children.length; ++j)
				if (j != ndx)
					nc[k++] = children[j];
			
			children = nc;
			
			return true;
		}
		
		/**
		 * Add a given Tokenization to the TreeNode (marking it 
		 * as a WORD_BOUNDARY)
		 * @param w
		 * @return inserted Tokenization
		 */
		public TreeNode addTokenization(Tokenization w) {
			TreeNode tn = new TreeNode(null, this);
			tn.word = w;
			addChild(tn);
			return tn;
		}
		
		/**
		 * Check if node is actually a word leaf
		 */
		public boolean isWordLeaf() {
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
			if (isWordLeaf())
				return word.word;
			else if (token == null)
				return "[root id=" + treeId + "]";
			else
				return token.toString();
		}
	}
	
	/** The root of the tree to reach all words */
	public TreeNode root = new TreeNode(null, null);
	
	/** The available phones */
	public TokenHierarchy tokenHierarchy;
	
	/** The selected Tokenizer */
	public Tokenizer tokenizer;
	
	/** translation table for word -> leaf in the token tree */
	public HashMap<String, TreeNode> wordLeaves = new HashMap<String, TreeNode>();
	
	/**
	 * Construct a token tree using the given token hierarchy and fill it 
	 * using with elements of the tokenizer.
	 * @param tokenHierarchy
	 * @param tokenizer
	 */
	public TokenTree(TokenHierarchy tokenHierarchy, Tokenizer tokenizer) {
		this(tokenHierarchy, tokenizer, true);
	}
	
	/**
	 * Construct a TokenTree using the given hierarchy and tokenizer
	 * @param tokenHierarchy
	 * @param tokenizer
	 * @param fill fill the tree with the elements of the tokenizer?
	 */
	public TokenTree(TokenHierarchy tokenHierarchy, Tokenizer tokenizer, boolean fill) {
		this.tokenHierarchy = tokenHierarchy;
		this.tokenizer = tokenizer;
		
		if (fill) {
			// add words
			for (Tokenization entry : tokenizer.tokenizations)
				addToTree(entry, tokenHierarchy.tokenizeWord(entry.sequence));
		}
	}
	
	/**
	 * Generate a deep copy of the TokenTree
	 */
	public TokenTree clone() {
		TokenTree tt = new TokenTree(tokenHierarchy, tokenizer);
		tt.id = id;
		return tt;
	}
	
	/**
	 * Link all word leaves to the given lexical successor tree (used by the
	 * decoder)
	 * @param lst
	 * @return
	 */
	public void linkLeaves(TreeNode lst) {
		for (TreeNode n : wordLeaves.values())
			n.setChild(lst);
	}
	
	/**
	 * Clear the tree of all nodes.
	 */
	public void emptyTree() {
		root = new TreeNode(null, null);
	}
	
	/**
	 * Detach a subtree from the TokenTree; used for example to separate the 
	 * silence branches for the decoder. all nodes will be placed  
	 * @param nodes list of nodes (the node and everything below it will be removed from the tree)
	 * @return
	 */
	public TokenTree detachSubtree(List<TreeNode> nodes) {
		TokenTree subtree = new TokenTree(tokenHierarchy, tokenizer);
		
		// BFS search for nodes
		for (TreeNode n : nodes) {
			Stack<TreeNode> agenda = new Stack<TreeNode>();
			agenda.add(root);
			
			while (agenda.size() > 0) {
				TreeNode inspect = agenda.pop();
				if (inspect.removeChild(n)) {
					subtree.root.addChild(n);
					break;
				}
			}
		}
		
		return subtree;
	}
	
	/**
	 * Add a token sequence (i.e. a word) to the token tree
	 * @param sequence
	 */
	public void addToTree(Tokenization word, Token [] sequence) {
		TreeNode target = root, inspect = root;
		for (Token tok : sequence) {
			// check if there is a matching token on this level
			for (int i = 0; i < inspect.children.length; ++i) {
				if (!inspect.children[i].isWordLeaf() && inspect.children[i].token.equals(tok)) {
					target = inspect.children[i];
					break;
				}
			}
			
			// no, we need a new branch!
			if (target == inspect) 
				target = inspect.addChild(new TreeNode(tok, null, id));
			
			inspect = target;
		}
		
		// we're at the leaf, add word arc
		wordLeaves.put(word.word, target.addTokenization(word));
	}
	
	/**
	 * Translate a Tokenization into the corresponding Tokens
	 * @param w Tokenization
	 * @return sequence of Token
	 */
	public List<Token> translate(Tokenization w) throws OutOfVocabularyException {
		if (!wordLeaves.containsKey(w.word))
			throw new OutOfVocabularyException("unknown word \"" + w.word + "\"");
		
		TreeNode leaf = wordLeaves.get(w.word);
		
		// discard the actual leaf!
		leaf = leaf.parent;
		
		// follow the leaf to the root, build up the acoustic model
		LinkedList<Token> reverse = new LinkedList<Token>();
		while (leaf.parent != null) {
			reverse.addFirst(leaf.token);
			leaf = leaf.parent;
		}
		
		return reverse;
	}
	
	/**
	 * Translate a Tokenization into the corresponding TreeNodes
	 * @param w Tokenization
	 * @return sequence of Token
	 */
	public List<TreeNode> nodeTranslate(Tokenization w) throws OutOfVocabularyException {
		if (!wordLeaves.containsKey(w.word))
			throw new OutOfVocabularyException("unknown word \"" + w.word + "\"");
		
		TreeNode leaf = wordLeaves.get(w.word);
		
		// follow the leaf to the root, build up the acoustic model
		LinkedList<TreeNode> reverse = new LinkedList<TreeNode>();
		while (leaf.parent != null) {
			reverse.addFirst(leaf);
			leaf = leaf.parent;
		}
		
		return reverse;
	}
	
	/**
	 * Translate a Tokenization sequence ("sentence") into the corresponding Tokens
	 * @param sent Iterable Tokenizations representing the sentence
	 * @return sequence of Token
	 */
	public List<Token> translate(Iterable<Tokenization> sent)
		throws OutOfVocabularyException {
		LinkedList<Token> tokens = new LinkedList<Token>();
		
		for (Tokenization w : sent) {
			if (!wordLeaves.containsKey(w.word))
				throw new OutOfVocabularyException("unknown word \"" + w.word + "\"");
			
			TreeNode leaf = wordLeaves.get(w.word);
			
			// discard the actual leaf
			leaf = leaf.parent;
			
			// follow the leaf to the root, build up the acoustic model
			LinkedList<Token> reverse = new LinkedList<Token>();
			while (leaf.parent != null) {
				reverse.addFirst(leaf.token);
				leaf = leaf.parent;
			}
			
			// add the model to the big list
			tokens.addAll(reverse);
		}
		
		return tokens;
	}
	
	/**
	 * Set the tree id and upate all nodes
	 * @param id
	 */
	public void setId(long id) {
		this.id = id;
		List<TreeNode> agenda = new LinkedList<TreeNode>();
		agenda.add(root);
		
		while (agenda.size() > 0) {
			TreeNode n = agenda.remove(0);
			
			// update id
			n.treeId = id;
			
			// enqueue children
			for (TreeNode c : n.children)
				agenda.add(c);
		}
	}
	
	/**
	 * Return a String representation of the lexical tree.
	 */
	public String toString() {
		return "LexicalTree: " + wordLeaves.keySet().size() + " words";
	}
	
	public String treeAsString() {
		StringBuffer sb = new StringBuffer();
		LinkedList<Pair<Integer, TreeNode>> agenda = new LinkedList<Pair<Integer, TreeNode>>();
		agenda.add(new Pair<Integer, TreeNode>(0, root));
		final String INDENT = "    ";

		// depth first search
		while (agenda.size() > 0) {
			Pair<Integer, TreeNode> pair = agenda.remove(agenda.size() - 1);

			// do correct indent
			for (int i = 0; i < pair.a; ++i)
				sb.append(INDENT);

			// print current
			sb.append(pair.b.toString());

			// add the children
			for (TreeNode child : pair.b.children)
				agenda.add(new Pair<Integer, TreeNode>(pair.a + 1, child));

			// finish line
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Compute the distributed and factored language model weights for this 
	 * lexical tree using the given word probability list. Make sure this 
	 * tree is not part of a network, as recursiveness is NOT checked.
	 * @param wts
	 */
	public void factorLanguageModelWeights(HashMap<Tokenization, Double> wts) 
		throws OutOfVocabularyException {				
		// DFS search to null the probs
		Stack<TreeNode> agenda1 = new Stack<TreeNode>();
		Stack<TreeNode> agenda2 = new Stack<TreeNode>();
		Stack<TreeNode> agenda3 = new Stack<TreeNode>();
		agenda1.add(root);
		
		while (agenda1.size() > 0) {
			TreeNode c = agenda1.pop();
			
			// build agendas for prob distribution and factorization
			agenda2.push(c);
			agenda3.push(c);
			
			// reset probs
			if (c.word == null)
				c.p = 0.;
			else {
				Double wt = wts.get(c.word);
				if (wt == null)
					throw new OutOfVocabularyException(c.word.toString());
				c.p = wt;
			}
			c.f = 0.;
			
			// add children to agenda
			for (TreeNode n : c.children)
				agenda1.push(n);
		}
		
		// use the existing DFS agenda to distribute the probabilities
		while (agenda2.size() > 0) {
			TreeNode c = agenda2.pop();

			// sum over children
			for (TreeNode n : c.children)
				c.p += n.p;
		}
		
		// use the existing DFS agenda to factorize the probabilities
		while (agenda3.size() > 0) {
			TreeNode c = agenda3.pop();
			
			// leafs don't require factorization
			if (c.children.length < 1)
				continue;
			
			// factorize the probs of the children
			double sum = 0.;
			for (TreeNode n : c.children)
				sum += n.p;
			
			for (TreeNode n : c.children)
				n.f = Math.log(n.p / sum);
		}
		
		// for consistency
		root.p = 1.;
		root.f = 0.;
	}
	
	/** 
	 * Traverse a newtork of TokenTree's to visualize the decoding network. This
	 * makes sure that no subtree is printed twice.
	 * @param root
	 * @return
	 */
	public static String traverseNetwork(TreeNode root) {
		final String INDENT = "    ";
		
		StringBuffer sb = new StringBuffer();
		
		HashSet<TreeNode> visited = new HashSet<TreeNode>();
		Stack<Pair<TreeNode, Integer>> agenda = new Stack<Pair<TreeNode, Integer>>();
		
		// DFS search
		agenda.add(new Pair<TreeNode, Integer>(root, 0));
		NumberFormat fmt = new DecimalFormat("#0.0000");
		while (agenda.size() > 0) {
			Pair<TreeNode, Integer> p = agenda.pop();
			
			// this can be true if it is a LST root node that has already been printed
			if (visited.contains(p.a))
				continue;
			
			// mark as visited
			visited.add(p.a);
			
			// do correct indent
			for (int i = 0; i < p.b; ++i)
				sb.append(INDENT);
			
			// print TreeNode
			sb.append(p.a + " P=" + fmt.format(p.a.p) + " F=" + fmt.format(p.a.f));
			if (p.a.isWordLeaf())
				for (TreeNode c : p.a.children) {
					sb.append(" LST=" + c);
				}
			
			sb.append("\n");
			
			for (TreeNode n : p.a.children) {
				if (!visited.contains(n))
					agenda.push(new Pair<TreeNode, Integer>(n, p.b + 1));
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
		"  --print-all\n" +
		"    Print the complete tree\n" +
		"  --list-words\n" +
		"    List all the words and their transcriptions (in alphabetical order)\n" +
		"  --find word\n" +
		"    Check if a word exists in the tree and show its transcription. Use\n" +
		"    a comma separated list if you want to query more than one word.\n" +
		"  --compose-sentence \"your sentence to compose\"" +
		"  --factor";
	
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		
		if (args.length < 1) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		Configuration conf = new Configuration(new File(args[0]));
		
		boolean printAll = false;
		boolean listWords = false;

		LinkedList<String> findWord = new LinkedList<String>();
		LinkedList<String> compSent = new LinkedList<String>();
		
		for (int i = 1; i < args.length; ++i) {
			if (args[i].equals("--print-all"))
				printAll = true;
			else if (args[i].equals("--list-words"))
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
		
		conf.buildTokenTree();
		
		if (printAll) {
			System.out.println(conf.tt.treeAsString());
		}
		
		if (listWords) {
			LinkedList<String> leafs = new LinkedList<String>(conf.tt.wordLeaves.keySet());
			Collections.sort(leafs);
			for (String l : leafs) {
				TreeNode n = conf.tt.wordLeaves.get(l);
				System.out.print(l);
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
		
		for (String w : findWord) {
			TreeNode n = conf.tt.wordLeaves.get(w);
			if (n == null)
				System.out.println("Word \"" + w + "\" not found");
			else {
				System.out.print(w);
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
		
		for (String s : compSent) {
			System.out.println(s);
			for (Token t : conf.tt.translate(conf.tok.getSentenceTokenization(s)))
				System.out.print(t + " ");
			System.out.println();
		}
	}
}
