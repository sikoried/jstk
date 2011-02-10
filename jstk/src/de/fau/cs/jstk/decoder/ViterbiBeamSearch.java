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
package de.fau.cs.jstk.decoder;


import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import de.fau.cs.jstk.arch.TokenTree;
import de.fau.cs.jstk.arch.TokenTree.TreeNode;
import de.fau.cs.jstk.exceptions.AlignmentException;
import de.fau.cs.jstk.stat.hmm.Alignment;
import de.fau.cs.jstk.stat.hmm.MetaAlignment;
import de.fau.cs.jstk.stat.hmm.State;

/**
 * The ViterbiBeamSearch is a classic implementation with either a fixed maximum
 * beam size or an adaptive size depending on acoustic similarity
 * 
 * @author sikoried
 */
public class ViterbiBeamSearch {
	/**
	 * Root node of the LST network
	 */
	private TreeNode root;
	
	/** word insertion penalty (logarithmic) */
	private double wip;
	
	/** language model weight */
	private double lmwt;
	
	/** beam size */
	private int bs;
	
	/** beam width for implicit beam size */
	private double bw = 0.;
	
	/** list of active hypotheses */
	private ViterbiList active = new ViterbiList();
	
	/** list of expanded hypotheses */
	private ViterbiList expanded = new ViterbiList();
	
	/** remember size of expanbded beam of last step */
	private int lastExpanded = 0;
	
	/**
	 * Create a new Decoder instance with the given LST network
	 * @param root
	 */
	public ViterbiBeamSearch(TreeNode root) {
		this(root, 1., 1.);
	}
	
	/**
	 * Create a new Decoder instance with the given LST nework, language
	 * model weight and word insertion penalty.
	 * @param root root of the LST network
	 * @param siltree TokenTree containing the possible silences
	 * @param lmWeight (linear)
	 * @param insertionPenalty word insertion penalty (0...1)
	 */
	public ViterbiBeamSearch(TreeNode root, double lmWeight, double insertionPenalty) {
		this.root = root;
		this.lmwt = lmWeight;
		this.wip = Math.log(insertionPenalty);
	}
	
	/**
	 * Initialize the beam with the first observation
	 * @param beamsize maximum size of the beam
	 * @param beamwidth beam width, Double.MAX_VALUE for infinite beam width
	 * @param first observation
	 * @return current beam width
	 */
	public double initialize(int beamsize, double beamwidth, double [] x) {
		this.bs = beamsize;
		this.bw = beamwidth;
		
		// make sure the lists are clear
		active.clear();
		expanded.clear();
		
		// generate the initial active hypotheses
		Hypothesis h0 = new Hypothesis(root);
		for (TreeNode n : root.children) 
			expanded.add(new Hypothesis(h0, n, Math.log(n.token.hmm.s[0].emits(x)), lmwt, wip));
		
		// sort and prune if necessary
		Collections.sort(expanded);
		
		double best = expanded.get(0).vs;
		double width = 0;
		while (active.size() < bs) {
			Hypothesis h = expanded.remove(0);
			if ((width = best - h.vs) > bw)
				break;
			
			active.add(h);
		}
		
		// ready to go, clear expanded list
		expanded.clear();
		
		return width;
	}
	
	/**
	 * Feed the next observation to the Viterbi beam search
	 * @param x
	 * @return current beam width (best-worst score)
	 */
	public double step(double [] x) {
		List<Hypothesis> nodeExpansions = new LinkedList<Hypothesis>();
		
		while (active.size() > 0) {
			Hypothesis h = active.remove(0);
			
			int cs = h.s;
			State [] s = h.node.token.hmm.s;
			float [] a = h.node.token.hmm.a[cs];
			
			// step 1: intra-node transitions
			for (short i = 0; i < s.length; ++i) {
				if (a[i] > 0.f)
					expanded.vadd(new Hypothesis(h, i, Math.log(a[i]) + Math.log(s[i].emits(x))));
			}
			
			// step 2: final state: mark for inter-node transitions
			if (cs == s.length - 1)
				nodeExpansions.add(h);
		}
		
		// node expansions
		while (nodeExpansions.size() > 0) {
			Hypothesis h = nodeExpansions.remove(0);
			
			for (TreeNode succ : h.node.children) {
				if (succ.isWordLeaf()) {
					// generate the null-hypothesis with the word
					// this is not an active hypothesis!!
					// h.p -> h -> token [null] -> word [null] ---> expansion
					Hypothesis token = new Hypothesis(h, h.node, 0.);
					Hypothesis word = new Hypothesis(token, succ, lmwt);
					
					
					// iterate over the lexical successor trees linked with this word leaf
					for (TreeNode lst : succ.children) {
						for (TreeNode t : lst.children)
							expanded.vadd(new Hypothesis(word, t, Math.log(t.token.hmm.s[0].emits(x)), lmwt, wip));
					}
				} else {
					// generate the null-hypothesis with the current node (no lmwt!)
					// this is not an active hypothesis!!
					// h.p -> h -> token[null] ---> expansion
					Hypothesis token = new Hypothesis(h, h.node, 0.);
		
					// no word insertion penalty!
					expanded.vadd(new Hypothesis(token, succ, Math.log(succ.token.hmm.s[0].emits(x)), lmwt, 0.));
				}
			}
		}
		
		// sort active hypotheses
		Collections.sort(expanded);
		
		// prune down to beam size
		Iterator<Hypothesis> it = expanded.iterator();
		double best = expanded.get(0).vs;
		double width = 0.;
		for (int i = 0; i < bs && it.hasNext(); ++i) {
			Hypothesis h = it.next();
			if ((width = best - h.vs) > bw)
				break;
			
			active.add(h);
		}
				
		// clear expanded hypotheses
		lastExpanded = expanded.size();
		expanded.clear();
		
		return width;
	}
	
	/**
	 * Prune all hypotheses which are NOT in final state
	 */
	public void pruneActiveHypotheses() {
		while (active.size() > 0) {
			Hypothesis h = active.remove(0);
			
			if (h.finalStateActive())
				expanded.add(h);
		}
		
		active.addAll(expanded);
		expanded.clear();
	}
	
	/**
	 * Get the current active beam size
	 * @return
	 */
	public int getCurrentBeamSize() {
		return active.size();
	}
	
	/**
	 * Get the current Expansion size
	 * @return
	 */
	public int getCurrentExpandedSize() {
		return lastExpanded;
	}
	
	/**
	 * Conclude the decoding by reducing to active hypotheses which are in the
	 * final state (and adding respective null-hypotheses)
	 */
	public void conclude() {
		while (active.size() > 0) {
			Hypothesis h = active.remove(0);
			
			// if h is in final state, add potential children
			if (h.finalStateActive()) {
				for (TreeNode succ : h.node.children) {
					if (succ.isWordLeaf()) {
						Hypothesis help = new Hypothesis(h, h.node, 0.);
						expanded.vadd(new Hypothesis(help, succ, lmwt));
					} else
						expanded.vadd(new Hypothesis(h, h.node, 0.));
				}
			} else {
				// track back to the last proper hypothesis, but maintain the 
				// viterbi score!
				Hypothesis it = h;
				while (it.p != null && !(it.nullhyp && it.node.isWordLeaf()))
					it = it.p;
				if (it.p != null) {
					it.vs = h.vs;
					expanded.vadd(it);
				}
			}
		}
		
		// sort active hypotheses
		Collections.sort(expanded);
		
		// prune down to beam size
		Iterator<Hypothesis> it = expanded.iterator();
		for (int i = 0; i < bs && it.hasNext(); ++i)
			active.add(it.next());
		
		// clear expanded hypotheses
		expanded.clear();
	}
	
	/**
	 * Feed a list of observations to the Viterbi beam search
	 * @param list
	 */
	public void step(List<double []> list) {
		for (double [] x : list)
			step(x);
	}
	
	/**
	 * Get the best n active hypotheses
	 * @param n if n == 0 or n > active.size then n = active.size
	 * @return
	 */
	public List<Hypothesis> getBestHypotheses(int n) {
		if (n == 0)
			n = active.size();
		return active.subList(0, n > active.size()? active.size() : n);
	}
	
	/**
	 * Return the best hypothesis surviving the beam.
	 * @return null if the beam is empty
	 */
	public Hypothesis getBestHypothesis() {
		if (active.size() > 0)
			return active.peek();
		else 
			return null;
	}
	
	/**
	 * The ViterbiList overwrites the add method of the LinkedList to help the
	 * Viterbi beam search. In case of an existing Hypothesis, only the better 
	 * scoring one is kept, otherwise the Hypothesis is appended to the list.
	 * 
	 * @author sikoried
	 */
	public static final class ViterbiList extends LinkedList<Hypothesis> {
		private static final long serialVersionUID = 1L;

		/**
		 * Add the given Hypothesis: Insert if no matching Hypothesis, replace 
		 * matching worse Hypothesis, discard if matching Hypothesis is better.
		 * 
		 * @return true if the referenced Hypothesis was appended to the list
		 */
		public boolean vadd(Hypothesis h) {		
			Iterator<Hypothesis> it = iterator();

			while (it.hasNext()) {
				Hypothesis cand = it.next();
				
				// hypotheses match
				if (cand.equals(h)) {
					if(cand.vs < h.vs) {
						// new hyp is better than old -> replace!
						it.remove();
						add(h);
						return true;
					} else {
						// new hyp is worse than old -> don't bother
						return false;
					} 
				}
			}
			
			// no matching hypothesis found, insert!
			return add(h);
		}
	}
	
	/**
	 * The Hypothesis couples all necessary information for the decoding process:
	 * Current Viterbi and acoustic score, node and (HMM) state as well as a pointer
	 * to the predecessor Hypothesis to track down the origin.
	 * 
	 * @author sikoried
	 */
	public static final class Hypothesis implements Comparable<Hypothesis> {
		/** predecessor of this hypothesis */
		public Hypothesis p = null;
		
		/** associated lexical successor tree node */
		public TreeNode node = null;
		
		/** originating Hypothesis  */
		public Hypothesis origin = null;
		
		/** current state */
		public short s = 0;
		
		/** Viterbi score */
		public double vs = 0.;
		
		/** acoustic score */
		public double as = 0.;
		
		/** is this node a null-hypothesis? */
		public boolean nullhyp = false;
		
		/**
		 * Generate a stub hypothesis as a root hypothesis for the initial 
		 * active states
		 * @param root root node of a lexical successor tree network
		 */
		public Hypothesis(TreeNode root) {
			p = null;
			node = root;
		}
		
		/**
		 * Clone the current hypothesis
		 */
		public Hypothesis clone() {
			Hypothesis nh = new Hypothesis(null);
			
			nh.node = node;
			nh.p = p;
			nh.s = s;
			nh.as = as;
			nh.vs = vs;
			nh.nullhyp = nullhyp;
			nh.origin = origin;
			
			return nh;
		}
		
		/**
		 * Allocate a new Hypothesis modeling a node internal state transition
		 * to the given state with the given probability
		 * @param parent parent hypothesis
		 * @param state target state
		 * @param prob log-probability for given state transition: log(a[?][state]) + log(s[state].emits(x))
		 */
		public Hypothesis(Hypothesis parent, short state, double prob) {
			this.p = parent;
			this.node = parent.node;
			this.origin = parent.origin;
			
			s = state;
			as = p.as + prob;
			vs = p.vs + prob;
		}
		
		/**
		 * Allocate a new Hypothesis expanding to a new lexical TreeNode
		 * @param parent (null-hypothesis)
		 * @param n
		 * @param aprob acoustic log-probability for given state transition log(s[0].emits(x))
		 * @param lmwt language model weight
		 * @param wip word insertion penalty (logarithmic), set to 0. for intra-word
		 */
		public Hypothesis(Hypothesis parent, TreeNode n, double aprob, double lmwt, double wip) {
			this.p = parent;
			this.node = n;
			this.origin = this;
			
			as = aprob;
			vs = p.vs + aprob + lmwt * n.f + wip;
		}
		
		/**
		 * Allocate a new Hypothesis as a null-hypothesis carrying the 
		 * associated node (or word leaf) and the respective total acoustic score
		 * @param parent
		 * @param n word leaf
		 * @param lmwt language model weight (set to 0. for intra-word node)
		 */
		public Hypothesis(Hypothesis parent, TreeNode n, double lmwt) {
			this.p = parent;
			this.node = n;
			this.origin = parent.origin;
			
			// conclude the Viterbi score by adding the final LM weight
			as = p.as;
			vs = p.vs + lmwt * n.f;
			
			// mark as null-hypothesis
			nullhyp = true;
		}
		
		/**
		 * Two Hypotheses are considered equal if they are in the same state s at 
		 * the same time t and share the previous word arc
		 * @param h
		 * @return true if states and node match
		 */
		public boolean equals(Hypothesis h) {
			// same HMM state?
			if (s != h.s)
				return false;
			
			// same originating hypothesis
			if (origin == h.origin)
				return true;
			
			// the current and origin nodes match
			if (node.equals(h.node) && origin.p.node.equals(h.origin.p.node)) {
				// trace back the history of null-hypotheses
				Hypothesis ita = origin;
				Hypothesis itb = h.origin;
				
				while (ita != null && itb != null) {
					// get next null-hypotheses
					ita = ita.getPreviousNullHypothesis();
					itb = itb.getPreviousNullHypothesis();
					
					// we reached the root asynchronously
					if (ita == null ^ itb == null)
						return false;
					
					// both are at the root, thats fine
					if (ita == null && itb == null)
						return true;
					
					// ah, same originating model
					if (ita.origin == itb.origin)
						return true;
					
					// whoops, different history!
					if (!ita.node.equals(itb.node))
						return false;
				}
			}
			
			return false;
		}
		
		/**
		 * Trace back to the previous null hypothesis
		 * @return
		 */
		public Hypothesis getPreviousNullHypothesis() {
			Hypothesis it = p;
			while (it != null && !it.nullhyp)
				it = it.p;
			return it;
		}
		
		/**
		 * Rank hypothesis by their current Viterbi score
		 */
		public int compareTo(Hypothesis t) {
			return (int) Math.signum(t.vs - vs);
		}
		
		/**
		 * Determine, if the Hypothesis is in final state (and thus possible a possible
		 * word or token). Throws RuntimeException if called on word null-hypothesis
		 * @return
		 */
		public boolean finalStateActive() {
			if (node.token == null)
				throw new RuntimeException("A null-hypothesis does not have any attached HMM!");
			return s == node.token.hmm.ns - 1;
		}
		
		/**
		 * Generate a simple String representation of the Hypotheses
		 */
		public String toString() {
			StringBuffer sb = new StringBuffer();
			
			Stack<String> trace = new Stack<String>();
			
			Hypothesis it = this;
			while (it.p != null) {
				if (it.nullhyp) {
					if (it.node.isWordLeaf())
						trace.push(it.node.word.word);
					else
						trace.push(it.node.toString());
				} else
					trace.push(it.node.toString() + ":" + it.s);
				it = it.p;
			}
			
			while (trace.size() > 0)
				sb.append(trace.pop() + " ");
			
			return sb.toString();
		}
		
		/**
		 * Get a String representation of this Hypothesis including acoustic scores
		 */
		public String toDetailedString() {
			StringBuffer sb = new StringBuffer();
			
			sb.append(vs + " ");
			
			Stack<String> trace = new Stack<String>();
			Hypothesis it = this;
			while (it.p != null) {
				if (it.nullhyp) {
					if (it.node.isWordLeaf())
						trace.push("[" + it.node.word.word + ", " + it.as + "]");
					else
						trace.push("(" + it.node.toString() + ", " + it.as + ")");
				} else
					trace.push(it.node.toString() + ":" + it.s);
				it = it.p;
			}
			
			while (trace.size() > 0)
				sb.append(trace.pop() + " ");
			
			return sb.toString();
		}
		
		/**
		 * Generate a compact String representation where intra-node transitions are
		 * compacted
		 * @return
		 */
		public String toCompactString() {
			Stack<Hypothesis> trace1 = new Stack<Hypothesis>();
			Stack<Integer> trace2 = new Stack<Integer>();
			Stack<Integer> trace3 = new Stack<Integer>();
			
			// follow trace, add word leaves
			Hypothesis it = clone();
			int nodec = 0;
			int wordc = 0;
			
			// make sure the trailing thing is considered as null-hypothesis
			if (!it.nullhyp)
				it.nullhyp = true;
			
			// push the trailing null-hyps
			while (it.nullhyp) {
				trace1.push(it);
				it = it.p;
			}
			
			// follow the trace
			while (it.p != null) {
				if (it.nullhyp) {
					trace1.push(it);
					if (it.node.isWordLeaf()) {
						trace2.push(wordc);
						wordc = 0;
					} else {
						trace3.push(nodec);
						nodec = 0;
					}
				} else {
					nodec++;
					wordc++;
				}
				it = it.p;
			}
			
			trace2.push(wordc);
			trace3.push(nodec);
			
			StringBuffer sb = new StringBuffer();
			sb.append(vs + " ");
			
			while (trace1.size() > 0) {
				Hypothesis h = trace1.pop();
				if (h.node.isWordLeaf())
					if (trace2.size() > 0)
						sb.append("[" + h.node.word.word + ", " + trace2.pop() + ", " + h.as + "] ");
					else
						sb.append("[" + h.node.word.word + ", 0, " + h.as + "] ");
				else {
					if (trace3.size() > 0)
						sb.append("(" + h.node.toString() + ", " + trace3.pop() + ", " + h.as + ") ");
					else 
						sb.append("(" + h.node.toString() + ", 0, " + h.as + ") ");
				}
			}
			
			return sb.toString();
		}

		/**
		 * Generate a MetaAlignment corresponde Hypothesis
		 * @param observation
		 * @param tt
		 * @return
		 */
		public MetaAlignment toMetaAlignment(TokenTree tt) throws AlignmentException {
			// reverse the hypothesis
			Stack<Hypothesis> trace = new Stack<Hypothesis>();
		
			Hypothesis it = this;
			while (it.p != null) {
				// skip null-hypotheses
				if (!it.nullhyp)
					trace.add(it);
				it = it.p;
			}
			
			// build up the alignments
			List<Alignment> algs = new LinkedList<Alignment>();
			
			TreeNode node = trace.peek().node;
			List<Integer> sseq = new LinkedList<Integer>();
			while (trace.size() > 0) {
				Hypothesis h = trace.pop();
				
				if (!h.node.equals(node)) {
					// build state sequence
					int [] qstar = new int [sseq.size()];
					for (int i = 0; i < qstar.length; ++i)
						qstar[i] = sseq.remove(0);
					
					// generate alignment
					Alignment a = new Alignment(node.token.hmm, null, qstar);
					algs.add(a);
										
					// reset the pointers
					sseq = new LinkedList<Integer>();
					node = h.node;
				}
				
				// build state sequence
				if (trace.size() > 0)
					sseq.add((int) h.s);
			}
			
			// there is an unfinished state sequence
			if (sseq.size() > 0) {
				// build state sequence
				int [] qstar = new int [sseq.size()];
				for (int i = 0; i < qstar.length; ++i)
					qstar[i] = sseq.remove(0);
				
				Alignment a = new Alignment(node.token.hmm, null, qstar);
				algs.add(a);
			}
			
			MetaAlignment ma = new MetaAlignment(tt, algs);
			return ma;
		}
		
		/**
		 * Generate a list of Hypotheses containing only the word null-hypotheses
		 * @return word list in correct time order
		 */
		public synchronized List<Hypothesis> extractWords() {
			LinkedList<Hypothesis> ws = new LinkedList<Hypothesis>();
			
			Hypothesis it = this;
			
			// follow trace, add word leaves
			while (it.p != null) {
				if (it.nullhyp && it.node.isWordLeaf())
					ws.add(0, it);
				it = it.p;
			}
			
			return ws;
		}
		
		/**
		 * Generate a list of Hypotheses containing only the Token null-hypotheses
		 * @return token list in correct time order
		 */
		public synchronized List<Hypothesis> extractTokens() {
			LinkedList<Hypothesis> ts = new LinkedList<Hypothesis>();
			
			Hypothesis it = this;
			
			while (it.p != null) {
				if (it.nullhyp && !it.node.isWordLeaf())
					ts.add(0, it);
				it = it.p;
			}
			
			return ts;
		}
	}
}
