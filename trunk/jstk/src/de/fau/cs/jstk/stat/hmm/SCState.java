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
package de.fau.cs.jstk.stat.hmm;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.HashMap;

import de.fau.cs.jstk.io.IOUtil;
import de.fau.cs.jstk.stat.Mixture;


/**
 * The semi-continuous HMM state uses a shared codebook of Gaussians and its own
 * mixture weights to compute the emission probabilities. As the codebook is
 * shared, so is the accumulator of the codebook. 
 * 
 * @author sikoried
 */
public final class SCState extends State {
	/** shared codebook */
	Mixture cb = null;
	
	/** individual mixture weights */
	double [] c = null;
	
	/** cached emission probability */
	double b;
	
	/** cached mixture posterior */
	double [] p;
	
	/** accumulator for the weights */
	double [] caccu = null;
	
	/**
	 * Create a new semi-continuous state using the given codebook.
	 * @param codebook
	 */
	public SCState(Mixture codebook) {
		this.cb = codebook;
		this.c = new double [cb.nd];
		this.p = new double [cb.nd];
		
		for (int i = 0; i < cb.nd; ++i)
			c[i] = 1. / cb.nd;
	}
	
	/**
	 * Create a new semi-continuous state based on the referenced one.
	 * @param copy
	 */
	public SCState(SCState copy) {
		this.cb = copy.cb;
		this.c = copy.c.clone();
		this.p = new double [this.cb.nd];
	}

	/** 
	 * Create an HMM state with semi-continuous output probability by reading
	 * from the given InputStream and shared densities
	 * @param is
	 * @param shared lookup table for shared mixtures
	 */
	public SCState(InputStream is, HashMap<Integer, Mixture> shared) 
		throws IOException {
		
		c = new double [IOUtil.readInt(is, ByteOrder.LITTLE_ENDIAN)];
		if (!IOUtil.readDouble(is, c, ByteOrder.LITTLE_ENDIAN))
			throw new IOException("could not read weights");
		
		int cbId = IOUtil.readInt(is, ByteOrder.LITTLE_ENDIAN);
		
		if (!shared.containsKey(cbId))
			throw new IOException("SCState(): missing codebook " + cbId);
		
		cb = shared.get(cbId);
		p = new double [cb.nd];
	}
	
	/**
	 * Write out the SCState. Note that instead of the MixtureDensity, only its 
	 * ID is written!
	 * @param os
	 */
	void write(OutputStream os) throws IOException {
		IOUtil.writeByte(os, getTypeCode());
		IOUtil.writeInt(os, c.length, ByteOrder.LITTLE_ENDIAN);
		IOUtil.writeDouble(os, c, ByteOrder.LITTLE_ENDIAN);
		IOUtil.writeInt(os, cb.id, ByteOrder.LITTLE_ENDIAN);
	}
	
	/**
	 * Probability of this state to emit the feature vector x
	 */
	public double emits(double[] x) {
		cb.evaluate2(x);
		b = 0.;
		
		for (int i = 0; i < c.length; ++i)
			b += c[i] * cb.components[i].score;
		
		return b;
	}

	/**
	 * Initialize the internal accumulator. As the codebook is shared, only
	 * the weight accumulator is initialized and a request for accumulator
	 * initialization is sent to the codebook.
	 */
	public void init() {
		caccu = new double [c.length];
		
		// we can call this any time, since the codebook must not allocate a new
		// accumulator in presence of an older one!
		cb.init();
	}
	
	/**
	 * Accumulate the feature vector given the state's posterior probability.
	 */
	public void accumulate(double gamma, double[] x) {
		// save your breath
		if (gamma == 0.)
			return;
		
		// evaluate codebook, compute posteriors
		cb.evaluate2(x);
		cb.posteriors(p, c);
		
		// for all densities...
		double gamma2;
		for (int j = 0; j < cb.nd; ++j) {
			// gamma_t(i,k)
			gamma2 = gamma * p[j];
			
			// caccu is state-dependent!
			caccu[j] += gamma2;
			
			// this is the sum over all states, as the cb and its accu are shared!
			cb.accumulate(gamma2, x, j);
		}
	}
	
	/**
	 * Absorb the given state's accumulator and delete it afterwards.
	 */
	public void absorb(State source) {
		SCState state = (SCState) source;
		
		// absorb the statistics
		for (int i = 0; i < c.length; ++i)
			caccu[i] += state.caccu[i];
		
		cb.absorb(state.cb);
	}

	/**
	 * Re-estimate this state's parameters from the accumulator and, if
	 * necessary, re-estimate the shared codebook. After the re-estimation, the
	 * accumulators are discarded.
	 */
	public void reestimate() {
		double sum = 0.;
		for (int i = 0; i < c.length; ++i)
			sum += caccu[i];
		
		if (sum <= 0.)
			System.err.println("Wurgs!" + sum);
		for (int i = 0; i < c.length; ++i)
			c[i] = caccu[i] / sum;
		
		cb.reestimate();
		discard();
	}
	
	/**
	 * Discard the current accumulator as well as the shared codebook's.
	 */
	public void discard() {
		caccu = null;
		cb.discard();
	}
	
	/**
	 * Generate a String representation of this state.
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("c = [");
		for (double cc : c) 
			buf.append(" " + cc);
		buf.append(" ]");
		
		if (caccu != null)
			buf.append(" weight accumulator present");
		
		return buf.toString();
	}
	
	public Mixture getMixture() {
		return cb;
	}

	/**
	 * Get the type byte for semi-contiuous states 's'
	 */
	public byte getTypeCode() {
		return 's';
	}
}
