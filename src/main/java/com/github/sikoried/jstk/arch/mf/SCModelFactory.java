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
package com.github.sikoried.jstk.arch.mf;

import com.github.sikoried.jstk.arch.Alphabet;
import com.github.sikoried.jstk.arch.Token;
import com.github.sikoried.jstk.stat.Mixture;
import com.github.sikoried.jstk.stat.hmm.Hmm;
import com.github.sikoried.jstk.stat.hmm.SCState;

import java.util.HashMap;

/**
 * The SCModelFactory produces semi-continuous states depending on the root
 * token. If no special codebook is available, the default codebook is used.
 * @author sikoried
 *
 */
public class SCModelFactory implements ModelFactory {
	/** internal model counter for creation of HMM ids */
	private int modelcount = 0;
	
	/** alphabet to determine the number of states */
	private Alphabet a;
	
	private Hmm.Topology topo;
	
	/** default mixture to use */
	private Mixture def;
	
	/** special per-root-token mixtures */
	private HashMap<String, Mixture> special = new HashMap<String, Mixture>();
	
	/**
	 * Allocate a new SCModelFactory with the given default mixture
	 * @param a Alphabet to use
	 * @param topo topology, typically Topology.LINEAR
	 * @param def
	 */
	public SCModelFactory(Alphabet a, Hmm.Topology topo, Mixture def) {
		this.a = a;
		this.topo = topo;
		this.def = def;
	}
	
	/**
	 * Add a special per-root-token mixture.
	 * @param token
	 * @param m
	 */
	public void addSpecificMixture(String token, Mixture m) {
		special.put(token, m);
	}
	
	/**
	 * Allocate a new Hmm with semi-continuous states using either default or 
	 * special mixture as emission codebook.
	 * @param t 
	 */
	public Hmm allocateModel(Token t) {
		Hmm ret = null;
		if (special.containsKey(t.token))
			ret = new Hmm(modelcount++, a.lookup.get(t.token), new SCState(special.get(t.token)));
		else
			ret = new Hmm(modelcount++, a.lookup.get(t.token), new SCState(def));
		
		ret.setTransitions(topo);
		return ret;
	}
}
