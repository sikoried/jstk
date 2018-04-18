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
package de.fau.cs.jstk.arch.mf;

import java.util.HashMap;

import de.fau.cs.jstk.arch.Alphabet;
import de.fau.cs.jstk.arch.Token;
import de.fau.cs.jstk.stat.Mixture;
import de.fau.cs.jstk.stat.hmm.CState;
import de.fau.cs.jstk.stat.hmm.Hmm;
import de.fau.cs.jstk.stat.hmm.Hmm.Topology;

public class CModelFactory implements ModelFactory {
	/** internal model counter for allocation */
	private int modelcount = 0;
	
	/** Alphabet token->number of states */
	private Alphabet a;
	
	/** Topology to assign to HMM */
	private Topology topo;
	
	/** index of mixtures to use for the tokens */
	private HashMap<String, Mixture> mixtures = new HashMap<String, Mixture>();
	
	private Mixture template = null;
	
	/**
	 * Allocate a new continuous model factory with the given Alphabet and topology
	 * @param a
	 * @param topo
	 */
	public CModelFactory(Alphabet a, Topology topo) {
		this.a = a;
		this.topo = topo;
	}
	
	/**
	 * Allocate a new continuous model factory with the given Alphabet and topology
	 * @param a
	 * @param topo
	 */
	public CModelFactory(Alphabet a, Topology topo, Mixture template) {
		this.a = a;
		this.topo = topo;
		this.template = template;
	}
	
	/**
	 * Assign a certain token to the given Mixture
	 * @param token
	 * @param m
	 */
	public void setMixture(String token, Mixture m) {
		mixtures.put(token, m);
	}
	
	public Hmm allocateModel(Token t) {
		Hmm ret = null;
		
		if (template != null)
			ret = new Hmm(modelcount++, a.lookup.get(t.token), new CState(template));
		else {
			if (mixtures.containsKey(t.token))
				ret = new Hmm(modelcount++, a.lookup.get(t.token), new CState(mixtures.get(t.token)));
			else
				throw new RuntimeException("No emission density for token " + t.toString());
		}
		
		ret.setTransitions(topo);
		return ret;
	}
}
