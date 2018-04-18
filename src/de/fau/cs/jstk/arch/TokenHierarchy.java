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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

import org.apache.log4j.Logger;

import de.fau.cs.jstk.exceptions.OutOfVocabularyException;
import de.fau.cs.jstk.stat.hmm.Hmm;


/**
 * The TokenHierarchy handles storage and loading of token hierarchies. It
 * is also used within the TokenTree to model words.
 * 
 * @author sikoried
 */
public final class TokenHierarchy {
	private static Logger logger = Logger.getLogger(TokenHierarchy.class);
	
	/** the roots of the token hierarchy, mapped for convenient access */
	public HashMap<String, Token> rtokens = new HashMap<String, Token>();
	
	/** container for all tokens */
	public HashMap<String, Token> tokens = new HashMap<String, Token>();
	
	/**
	 * Generate a new empty TokenHierarchy.
	 */
	public TokenHierarchy() {
		
	}
	
	/**
	 * Add all tokens occurring in the given Tokenizer
	 * @param lex
	 */
	public void addTokensFromTokenizer(Tokenizer toks, int contextSize) {
		for (Tokenization tok : toks.tokenizations) {
			addTokens(Token.extractTokensInContext(tok.sequence, contextSize));
		}
	}
	
	/**
	 * Clear the inventory for a clean start.
	 */
	public void clearTokenHierarchy() {
		rtokens.clear();
	}
	
	/**
	 * Add a single token to the inventory, ignore if already present.
	 * Ensures the correct placement within the hierarchy.
	 * 
	 * @param p Token to insert.
	 */
	public void addToken(Token p) {
		if (tokens.containsKey(p.uniqueIdentifier()))
			return;

		// add it to the pool
		tokens.put(p.uniqueIdentifier(), p);
		
		// see if there is already a root node for the center phone
		Token root = rtokens.get(p.token);
		
		if (root == null) {
			// wee, first root node for that phone
			rtokens.put(p.token, p);
		} else if (p.generalizes(root)) {
			// we need to replace the root
			rtokens.put(p.token, p);
			p.addChild(root);
		} else {
			// dig down to the right point; note that context expansion goes by right first
			root.addChild(p);
		}	
	}
	
	/**
	 * Add a list of tokens to the inventory.
	 * @param tokens
	 */
	public void addTokens(Token [] tokens) {
		for (Token p : tokens)
			addToken(p);
	}
	
	/**
	 * Rebuild the token hash set by an iterative DFS
	 */
	private void rebuildTokenHash() {
		tokens.clear();
		
		LinkedList<Token> agenda = new LinkedList<Token>();
		
		for (Token p : rtokens.values())
			agenda.add(p);
		
		while (agenda.size() > 0) {
			Token p = agenda.remove();
			tokens.put(p.uniqueIdentifier(), p);
			for (Token i : p.moreContext)
				agenda.add(i);
		}
	}
	
	/**
	 * Prune the unneeded tokens, i.e. extra "idle" links in the token hierarchy.
	 */
	public void pruneTokenHierarchy() {
		for (Token p : rtokens.values())
			p.pruneHierarchy();
		
		rebuildTokenHash();
	}

	/**
	 * Prune the token hierarchy by number of occurrence.
	 * 
	 * @param minOcc minimum number of occurrences of tokens to be retained
	 * @param tokenizer tokenizer to use for the sentence file
	 * @param sentenceFile file containing line-by-line sentences
	 */
	public void pruneHierarchyByOccurrence(int minOcc, Tokenizer tokenizer, File sentenceFile) 
		throws IOException, OutOfVocabularyException {
		logger.info("pruning TokenHierarchy by min-occurrence (" + sentenceFile.getName() + ":" + minOcc);
		
		// reset all occurrence counters
		for (Token p : rtokens.values())
			p.resetOccurrenceCounter();
		
		// read all sentences
		BufferedReader br = new BufferedReader(new FileReader(sentenceFile));
		
		String buf = null;
		while ((buf = br.readLine()) != null) {
			// split the string by whitespaces
			String [] words = buf.trim().split("\\s+");
			
			for (String w : words) {				
				// translate the word to transcription, get the polyphone sequence
				Token [] trans = tokenizeWord(tokenizer.tokenize(w));
				
				// do the counting; don't forget to include tokens with lesser contexts!
				for (Token p : trans) {
					do {
						p.occurrences++;
					} while ((p = p.lessContext) != null);					
				}
			}
		}
		
		br.close();
		
		// now do the actual pruning
		for (Token p : rtokens.values()) {
			p.pruneHierarchyByOccurrence(minOcc);
			p.pruneHierarchy();
		}
		
		rebuildTokenHash();
	}
	
	/**
	 * Get the size of the phone inventory.
	 * 
	 * @return Size of the phone inventory.
	 */
	public int size() {
		return tokens.size();
	}
	
	/**
	 * Retrieve the most general token for the given phone
	 * @param phone
	 * @return
	 */
	public Token getToken(String phone) {
		return rtokens.get(phone);
	}
	
	/**
	 * Retrieve a token with certain context
	 * @param token e.g. S/n/o
	 * @return null if token does not exist in this hierarchy
	 */
	public Token getPolyphone(String uniqueIdentifier) {
		if (tokens.containsKey(uniqueIdentifier))
			return tokens.get(uniqueIdentifier);
		else
			return null;
	}
	
	/**
	 * Retrieve the token that is associated with the requested
	 * HMM id.
	 * @param modelId
	 * @return
	 */
	public Token getPolyphone(int modelId) {
		for (Token p : tokens.values())
			if (p.hmm != null && p.hmm.id == modelId)
				return p;
		
		return null;
	}
	
	/**
	 * Construct the Token sequence for a given transcription. The tokens 
	 * with the longest matching context will be used.
	 * 
	 * @param tokenization tokenization of a single word
	 * @return 
	 */
	public Token [] tokenizeWord(String [] transcription) {
		LinkedList<Token> seq = new LinkedList<Token>();
		
		// for each token, get the token with the longest matching context
		for (int i = 0; i < transcription.length; ++i) {
			
			// retrieve root token, then dig down
			Token cand = rtokens.get(transcription[i]);
			Token oldc = null;
			
			// descend 
			while (cand.matchesTokenization(transcription, i)) {
				if (cand.moreContext.length == 0)
					break;
				
				// find the next candidate to dig down
				for (Token p : cand.moreContext) {
					if (p.matchesTokenization(transcription, i)) {
						oldc = cand;
						cand = p;
						break;
					} else
						oldc = cand;
				}
				
				// detect if we're at the bottom
				if (oldc == cand)
					break;
			}
			
			seq.add(cand);
		}
		
		return seq.toArray(new Token [seq.size()]);
	}
	
	/**
	 * Get a String representation of the phone inventory, including the polyphone
	 * hierarchy.
	 * @return the hierarchy in ASCII art
	 */
	public String hierarchyAsString() {
		StringBuffer sb = new StringBuffer();
		
		for (Token p : rtokens.values())
			sb.append(p.hierarchyAsString());

		return sb.toString();
	}
	
	/**
	 * Dump the Hierarchy to the given PrintStream
	 * @param out
	 */
	public void dump(PrintStream ps) {
		ps.println(this.toString());
		for (Token p : rtokens.values())
			p.dump(ps);
	}
	
	/**
	 * Return information about this TokenHierarchy.
	 */
	public String toString() {
		return "TokenHierarchy with " + rtokens.size() + " root tokens and a total of " + tokens.size() + " tokens";
	}
	
	/**
	 * Reduce the maximum context of the token hierarchy to the given number.
	 * Note that contexts are built up starting right.
	 * @param context new maximum context size
	 */
	public void reduceContext(int context) {
		// reset the general hash set to the root tokens
		tokens.clear();
		
		// visit all children, remove subsequent children if the context is too 
		// large, add valid children to agenda
		LinkedList<Token> agenda = new LinkedList<Token>(rtokens.values());
		while (agenda.size() > 0) {
			Token t = agenda.remove();
			tokens.put(t.uniqueIdentifier(), t);
			
			LinkedList<Token> reducedContext = new LinkedList<Token>();
			for (Token i : t.moreContext) {
				if (i.satisfiesTokenContext(context)) {
					reducedContext.add(i);
					agenda.add(i);
				}
			}
			
			if (t.moreContext.length != reducedContext.size())
				t.moreContext = reducedContext.toArray(new Token [reducedContext.size()]);
		}
	}
	
	/**
	 * Propagate the sufficient statistics bottom-up
	 * 
	 * Step 2 of APIS, see Schukat-Talamazzini p. 300ff.
	 */
	public void propagate() {
		// do a DFS to build up top-down and bottom-up lists
		Stack<Token> bu = new Stack<Token>();
		Stack<Token> agenda = new Stack<Token>();
		
		// add all root tokens
		for (Token t : rtokens.values())
			agenda.push(t);
		
		while (agenda.size() > 0) {
			Token t = agenda.pop();
			
			bu.push(t);
			
			for (Token c : t.moreContext)
				agenda.push(c);
		}
		
		// bottom-up propagation of sufficient statistics
		for (Token t: bu) {
			Hmm mother = t.hmm;
			
			for (Token c : t.moreContext)
				mother.propagate(c.hmm);
		}
	}
	
	/**
	 * Interpolate the sufficient statistics top-down
	 * 
	 * Step 3 of APIS, see Schukat-Talamazzini p. 300ff
	 * 
	 * @param rho interpolation weight ( rho(i) =  rho / (rho + gamma(i) )
	 */
	public void interpolate(double rho) {
		// do a DFS to build up top-down and bottom-up lists
		LinkedList<Token> td = new LinkedList<Token>();
		Stack<Token> agenda = new Stack<Token>();
		
		// add all root tokens
		for (Token t : rtokens.values())
			agenda.push(t);
		
		while (agenda.size() > 0) {
			Token t = agenda.pop();
			
			td.add(t);
			
			for (Token c : t.moreContext)
				agenda.push(c);
		}
		
		// top-down interpolation 
		for (Token t: td) {
			Hmm mother = t.hmm;
			
			for (Token c : t.moreContext)
				c.hmm.interpolate(mother, rho);
		}
	}
}
