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


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import de.fau.cs.jstk.stat.hmm.Hmm;
import de.fau.cs.jstk.util.Pair;


/**
 * A token consists of the central token, its left and right context and the 
 * statistical model linked to it. The token hierarchy is fixed, the token tree
 * structure references to these instances on demand.
 * 
 * @author sikoried
 */
public final class Token {

	/** left context, including boundaries */
	String [] left;
	
	/** right context, including boundaries */
	String [] right;
	
	/** central phone */
	public String token;
		
	/** parent token w/ less context, if any */
	public Token lessContext = null;
	
	/** child token w/ more context, if any */
	public Token [] moreContext = new Token [0];
	
	/** internal hash number, requires update if context is changed */
	private int hashval;
	
	/** number of occurrences in the training set */
	public transient int occurrences = 0;
	
	/** statistic model associated with this token */
	public Hmm hmm;
	
	/** store the HMM id (for persistence) */
	public int hmmId = -1;
	
	/**
	 * Create a token with the given context.
	 * @param left
	 * @param token
	 * @param right
	 */
	public Token(String [] left, String token, String [] right) {
		this.left = left;
		this.right = right;
		this.token = token;
		updateHash();
	}
	
	/**
	 * Create a token without context.
	 * @param token
	 */
	public Token(String token) {
		this.left = new String [0];
		this.right = new String [0];
		this.token = token;
		updateHash();
	}
	
	/** 
	 * Generate a Token and initialize from the given ObjectInputStream
	 * @param ois
	 * @throws IOException
	 */
	public Token(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		if (!((String) ois.readObject()).equals("arch.Token"))
			throw new IOException("Token.Token(): Stream does not point to arch.Token");
		
		token = (String) ois.readObject();
		left = (String []) ois.readObject();
		right = (String []) ois.readObject();
		hmmId = (Integer) ois.readObject();
		
		updateHash();
	}
	
	/**
	 * Write the Token (and all involved data) to the given ObjectOutputStream.
	 * @param oos
	 * @throws IOException
	 */
	public void write(ObjectOutputStream oos) throws IOException {
		oos.writeObject("arch.Token");
		
		oos.writeObject(token);
		oos.writeObject(left);
		oos.writeObject(right);
		oos.writeObject(hmmId);
	}
	
	public String uniqueIdentifier() {
		StringBuffer sb = new StringBuffer();
		if (left.length > 0)
			sb.append(left[0]);
		for (int i = 1; i < left.length; ++i) {
			sb.append(left[i]);
			if (i < left.length-1)
				sb.append("_");
		}
		sb.append("/" + token + "/");
		if (right.length > 0)
			sb.append(right[0]);
		for (int i = 1; i < right.length; ++i) {
			sb.append(right[i]);
			if (i < right.length-1)
				sb.append("_");
		}
		return sb.toString();
	}
	
	/**
	 * If the context of the token (or the token itself) was changed, you need
	 * to update the hash!
	 */
	void updateHash() {
		hashval = uniqueIdentifier().hashCode();
	}
	
	/**
	 * Attach a HMM to the token
	 * @param hmm
	 */
	public void setHMM(Hmm hmm) {
		this.hmm = hmm;
		hmmId = hmm.id;
		hmm.textualId = uniqueIdentifier();
	}
	
	/**
	 * Obtain a string representation of the polyphone, e.g. raI/s/@
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (String s : left)
			sb.append(s);
		sb.append("/" + token + "/");
		for (String s : right)
			sb.append(s);

		return sb.toString();
	}
	
	/**
	 * Obtain a hash value using the string representation of the polyphone
	 */
	public int hashCode() {
		return hashval;
	}
	
	/**
	 * Two tokens are equals if and only if the token itself and both left and right context match.
	 * @param p
	 * @return
	 */
	public boolean equals(Token p) {
		// check the actual token
		if (!token.equals(p.token))
			return false;
		
		// check left and right context
		return matchContexts(left, p.left) && matchContexts(right, p.right);
	}
	
	public boolean equals(Object o) {
		if (o instanceof Token)
			return equals((Token) o);
		else
			return false;
	}
	
	/** 
	 * Test if the token has the requested context length. Contexts are built up
	 * from the right side, i.e., /h/ has context zero, /h/a has context one,
	 * a/h/a has context two and so on. Inhomogeneous context sizes will return
	 * false.
	 * 
	 * @param length
	 * @return
	 */
	public boolean hasTokenContext(int length) {
		return 
			left.length == length / 2 
			&& right.length == (length / 2) + (length % 2);
	}
	
	/**
	 * Test if the token has a homogeneous context of length or less. Internally,
	 * hasTokenContext is called for length, ..., 0.
	 * @param length
	 * @return
	 */
	public boolean satisfiesTokenContext(int length) {
		boolean sat = false;
		while (length >= 0)
			sat |= hasTokenContext(length--);
		return sat;
	}
	
	/**
	 * Reset the occurrence counter of this token and its children
	 */
	public void resetOccurrenceCounter() {
		occurrences = 0;
		for (Token p : moreContext)
			p.resetOccurrenceCounter();
	}
	
	/**
	 * Manually insert designated node to the child list. Caution! The Token 
	 * hash of the enclosing TokenHierarchy is not updated!
	 * @param pos -1 to append to child list
	 * @param node
	 */
	public void insertChild(int pos, Token node) {
		if (pos == -1)
			pos = moreContext.length;
		
		Token [] newc = new Token [moreContext.length + 1];
		int i;
		for (i = 0; i < pos; ++i)
			newc[i] = moreContext[i];
		newc[pos] = node;
		newc[pos].lessContext = this;
		for (int j = pos + 1; j < newc.length; ++j, ++i)
			newc[j] = moreContext[i];
		
		moreContext = newc;
	}
	
	/**
	 * Add a Token to the hierarchy
	 * @param child
	 */
	public void addChild(Token node) {
		// find insert position
		
		// candidates that specialize the node (will be moved, if any)
		List<Token> sli = new LinkedList<Token>();
		
		// candidates that generalize the node (best candidate first)
		List<Token> gli = new LinkedList<Token>();
		
		for (int i = 0; i < moreContext.length; ++i) {
			Token cand = moreContext[i];
		
			// examine contexts
			boolean match_l = matchContexts(cand.left, node.left);
			boolean match_r = matchContexts(cand.right, node.right);
			
			// impossible case, but still...
			if (match_l && match_r)
				continue;
			
			// have a closer look if desired
			int comp_l = match_l ? 0 : compareContextsL(cand.left, node.left);
			int comp_r = match_r ? 0 : compareContextsR(cand.right, node.right);
		
			// comp > 0 : cand specializes node
			// comp < 0 : cand generalizes node
			
			// evaluate...
			if (comp_l > 0 && comp_r > 0) {
				// cand specializes node in both contexts
				sli.add(cand);
			} else if (match_l && comp_r > 0 || match_r && comp_l > 0) {
				// match one context and specialize the other
				sli.add(cand);
			} else if (!match_l && !match_r && comp_l < 0 && comp_r < 0) {
				// cand generalizes node in both contexts (best fit)
				gli.add(0, cand);
			} else if (match_r && comp_l < 0) {
				// cand generalizes node in right context (2nd best fit)
				gli.add(0, cand);
			} else if (match_l && comp_r < 0) {
				// cand generalizes node in left context (3rd best fit)
				gli.add(cand);
			}
		}
		
		if (gli.size() > 0) {
			// we found a node that generalizes the new node, so add it here!
			gli.get(0).addChild(node);
		} else if (sli.size() > 0) {
			// we found no node that generalizes, but some nodes that specialize
			// the new node => build up new moreContext list
			List<Token> nc = new LinkedList<Token>();
			for (Token t : moreContext) {
				if (sli.contains(t))
					node.addChild(t);
				else
					nc.add(t);
			}
			nc.add(node);
			node.lessContext = this;
			moreContext = nc.toArray(new Token [nc.size()]);
		} else {
			// regular insert
			insertChild(-1, node);
		}
	}
	
	/**
	 * Prune the token hierarchy to remove extra "idle" links. This is handled
	 * through recursion!
	 */
	public void pruneHierarchy() {
		// down at the very bottom -- nothing to do
		if (moreContext.length == 0)
			return;
		
		// DFS by recursion
		for (Token p : moreContext) {
			p.pruneHierarchy();
			
			if (p.moreContext.length == 1) {
				p.moreContext = p.moreContext[0].moreContext;
				for (Token c : moreContext)
					c.lessContext = this;
			}
		}
		
		if (moreContext.length == 1) {
			moreContext = moreContext[0].moreContext;
			for (Token c : moreContext)
				c.lessContext = this;
		}
	}
	
	/**
	 * Prune the token hierarchy by removing tokens appearing less than
	 * a certain frequency (recursive call).
	 * 
	 * @param minOcc minimum number of occurrences
	 */
	public void pruneHierarchyByOccurrence(int minOcc) {
		if (moreContext.length == 0)
			return;
		
		LinkedList<Token> prunedContext = new LinkedList<Token>();
		for (Token p : moreContext) {
			// if we keep p, we need to prune its hierarchy
			if (p.occurrences >= minOcc) {
				p.pruneHierarchyByOccurrence(minOcc);
				prunedContext.add(p);
			}
		}
		
		// if there was any pruning update the moreContext array
		if (prunedContext.size() != moreContext.length)
			moreContext = prunedContext.toArray(new Token [prunedContext.size()]);
	}
	
	/**
	 * Generate a String representation of the hierarchy using ASCII art
	 * @return 
	 */
	public String hierarchyAsString() {
		StringBuffer sb = new StringBuffer();
		LinkedList<Pair<Integer, Token>> agenda = new LinkedList<Pair<Integer, Token>>();
		agenda.add(new Pair<Integer, Token>(0, this));
		final String INDENT = "    ";

		// depth first search
		while (agenda.size() > 0) {
			Pair<Integer, Token> pair = agenda.remove(agenda.size() - 1);

			// do correct indent
			for (int i = 0; i < pair.a; ++i)
				sb.append(INDENT);

			// print current
			sb.append(pair.b.toString());
			
			// hmm stats?
			if (pair.b.hmm != null)
				sb.append(" id=" + pair.b.hmm.id);
			
			// add the children
			for (Token child : pair.b.moreContext)
				agenda.add(new Pair<Integer, Token>(pair.a + 1, child));

			// finish line
			sb.append("\n");
		}
		return sb.toString();
	}
	
	/** 
	 * Dump the Token and its children to the given PrintStream
	 * @param ps
	 */
	public void dump(PrintStream ps) {
		LinkedList<Pair<Integer, Token>> agenda = new LinkedList<Pair<Integer, Token>>();
		agenda.add(new Pair<Integer, Token>(0, this));

		// depth first search
		while (agenda.size() > 0) {
			Pair<Integer, Token> pair = agenda.remove(agenda.size() - 1);

			// print current
			ps.println(pair.b.toString());
			
			// hmm stats?
			if (pair.b.hmm != null)
				ps.println(pair.b.hmm);
			
			// add the children
			for (Token child : pair.b.moreContext)
				agenda.add(new Pair<Integer, Token>(pair.a + 1, child));
		}
	}
	
	/**
	 * Check two contexts for identity.
	 * @param a
	 * @param b
	 * @return
	 */
	private static boolean matchContexts(String [] a, String [] b) {
		if (a.length == 0 && b.length == 0)
			return true;
		else if (a.length != b.length)
			return false;
		
		for (int i = 0; i < a.length; ++i)
			if (!a[i].equals(b[i]))
				return false;
	
		return true;
	}
	
	/**
	 * Compare two right contexts for generalization/specialization
	 * @param a
	 * @param b
	 * @return zero for indecisive (zero/equal length or Token mismatch), negative for A generalizes B, positive for A specializes B
	 */
	private static int compareContextsR(String [] a, String [] b) {
		if (a.length == 0 && b.length == 0 || a.length == b.length) 
			return 0;
		
		// trivial cases -- one context has zero length
		if (a.length == 0)
			return -b.length;
		if (b.length == 0)
			return a.length;
		
		int i;
		for (i = 0; i < a.length && i < b.length; ++i)
			if (!a[i].equals(b[i]))
				return 0;
		
		return a.length - b.length;
	}
	
	/**
	 * Compare two left contexts for generalization/specialization
	 * @param a
	 * @param b
	 * @return zero for indecisive (zero/equal length or Token mismatch), negative for A generalizes B, positive for A specializes B
	 */
	private static int compareContextsL(String [] a, String [] b) {
		if (a.length == 0 && b.length == 0 || a.length == b.length) 
			return 0;
		
		// trivial cases -- one context has zero length
		if (a.length == 0)
			return -b.length;
		if (b.length == 0)
			return a.length;
		
		// for left context, check from the most right side!
		for (int i = a.length-1, j = b.length-1; i >= 0 && j >= 0; --i, --j)
			if (!a[i].equals(b[j]))
				return 0;
		
		return a.length - b.length;
	}
	
	/**
	 * Check whether or not the token generalizes the referenced token by its
	 * context.
	 * NB: An equal token is not a generalization!
	 * 
	 * @param p
	 * @return
	 */
	public boolean generalizes(Token p) {
		// false, if the core token doesn't match or if the tokens are identical
		if (!p.token.equals(token))
			return false;
		
		boolean match_l = matchContexts(left, p.left);
		boolean match_r = matchContexts(right, p.right);
		int comp_l = compareContextsL(left, p.left);
		int comp_r = compareContextsR(right, p.right);
		
		if (match_l && match_r)
			return false;
		else if (match_l && comp_r < 0)
			return true;
		else if (match_r && comp_l < 0) 
			return true;
		else 
			return false;
	}
	
	/**
	 * Extract all possible tokens/contexts from the given word transcription.
	 * 
	 * @param tokenization Tokenization of a single word in form of a String 
	 *        array of tokens.
	 * @param contextSize Maximum context size to consider.
	 * @return Array of tokens (not linked)
	 */
	public static Token [] extractTokensInContext(String [] tokenization, int contextSize) {
		LinkedList<Token> tokens = new LinkedList<Token>();
		
		// this will be the growing context
		LinkedList<String> left = new LinkedList<String>();
		LinkedList<String> right = new LinkedList<String>();
		
		// For each center phoneme, get all possible polyphones
		for (int i = 0; i < tokenization.length; ++i) {
			// add the token w/o context
			tokens.add(new Token(tokenization[i]));
			
			// now generate every possible polyphone, expand right-left-right-left-...
			// until both contexts are exploited
			int il = i-1;
			int ir = i+1;
			int expansions = 0;
			while ((il >= 0 || ir < tokenization.length) && expansions < contextSize) {
				// expand right
				if (ir < tokenization.length) {
					right.add(tokenization[ir]);
					ir++;
					
					tokens.add(new Token(
							left.toArray(new String [left.size()]), 
							tokenization[i], 
							right.toArray(new String [right.size()])
							));
					
					expansions++;
				}
				
				if (expansions == contextSize)
					break;
				
				// expand left
				if (il >= 0) {
					left.add(0, tokenization[il]);
					il--;
					
					tokens.add(new Token(
							left.toArray(new String [left.size()]), 
							tokenization[i], 
							right.toArray(new String [right.size()])
							));
					
					expansions++;
				}
			}
			
			left.clear();
			right.clear();
		}
		
		return tokens.toArray(new Token [tokens.size()]);
	}
	
	/**
	 * Check if the the token and its context fit in a sequence of tokens at a 
	 * given point
	 * @param sequence tokenization of the target word
	 * @param position index of the central token
	 * @return true if the token fits in at the position
	 */
	public boolean matchesTokenization(String [] sequence, int position) {
		// pre-check lengths
		if (left.length > position || right.length >= sequence.length - position)
			return false;
		
		// contexts
		for (int i = 0, j = left.length - 1; i < left.length; ++i, --j) {
			if (!sequence[position - 1 - i].equals(left[j]))
				return false;
		}
		for (int i = 0; i < right.length; ++i) {
			if (!sequence[position + 1 + i].equals(right[i]))
				return false;
		}
		
		// everything fits!
		return true;
	}
}
