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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.github.sikoried.jstk.arch.mf.ModelFactory;
import com.github.sikoried.jstk.exceptions.CodebookException;
import com.github.sikoried.jstk.io.IOUtil;
import com.github.sikoried.jstk.stat.*;
import com.github.sikoried.jstk.stat.hmm.Hmm;
import com.github.sikoried.jstk.stat.hmm.SCState;
import com.github.sikoried.jstk.stat.hmm.State;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * The Codebook stores HMM models (indexed by ids) and corresponding emission
 * probabilities (usually Mixtures of some sort)
 * 
 * @author sikoried
 */
public class Codebook {
	private static Logger logger = LogManager.getLogger(Codebook.class);
	
	/** index of shared emission mixture densities */
	private HashMap<Integer, Mixture> shared = new HashMap<Integer, Mixture>();
	
	/** corresponding MLE accumulators */
	private HashMap<Integer, MleMixtureAccumulator> sharedAccs = new HashMap<Integer, MleMixtureAccumulator>();
	
	/** index of HMM models */ 
	private HashMap<Integer, Hmm> models = new HashMap<Integer, Hmm>();
	
	private File origin = null;
	
	public Codebook() {
		
	}
	
	/**
	 * Allocate a new Codebook and load the parameters from the given File
	 * @param file
	 * @throws IOException
	 */
	public Codebook(File file) throws IOException {
		read(file);
	}
	
	/**
	 * Attach the models from the Codebook to the Tokens in the TokenHierarchy
	 * @param th
	 * @throws CodebookException
	 */
	public void attachModels(TokenHierarchy th) throws CodebookException {
		for (Token t : th.tokens.values()) {
			if (!models.containsKey(t.hmmId))
				throw new CodebookException("No model for Token " + t.toString());
			t.setHMM(models.get(t.hmmId));
		}
	}
	
	/**
	 * Obtain a collection of the shared Mixtures
	 * @return
	 */
	public Collection<Mixture> getSharedMixtures() {
		return shared.values();
	}
	
	/**
	 * Replace the shared Mixture whose ID matches an already existing one.
	 * @param m Mixture to insert (make sure ID is right)
	 */
	public void replaceSharedMixture(Mixture m) {
		shared.put(m.id, m);
	}
	
	/**
	 * Initialize the models for a given TokenHierarchy using a ModelFactory.
	 * @param th
	 * @param mf
	 */
	public void initializeModels(TokenHierarchy th, ModelFactory mf) {
		for (Token t : th.tokens.values()) {
			Hmm model = mf.allocateModel(t);
			
			t.setHMM(model);
			
			models.put(model.id, model);
			for (State ss : model.s) {
				if (ss instanceof SCState) {
					Mixture m = ((SCState )ss).getMixture();
					if (!shared.containsKey(m.id))
						shared.put(m.id, m);
				}
			}
		}
	}
	
	/**
	 * Read a Codebook from the given file
	 * @param file
	 * @throws IOException
	 */
	public void read(File file) throws IOException {
		logger.info("Reading codebook from " + file.getAbsolutePath());
		origin = file;
		
		FileInputStream fis = new FileInputStream(file);
		
		// read shared mixtures, if any
		int n = IOUtil.readInt(fis, ByteOrder.LITTLE_ENDIAN);
		logger.debug("Reading " + n + " Mixtures");
		for (int i = 0; i < n; ++i) {
			Mixture m = new Mixture(fis);
			shared.put(m.id, m);
		}
		
		// read in models
		n = IOUtil.readInt(fis, ByteOrder.LITTLE_ENDIAN);
		logger.debug("Reading " + n + " Hmm");
		for (int i = 0; i < n; ++i) {
			Hmm m = new Hmm(fis, shared);
			models.put(m.id, m);
		}
	}
	
	/**
	 * Write the current codebook to the given file.
	 * @param file
	 * @throws IOException
	 */
	public void write(File file) throws IOException {
		logger.info("Writing codebook to " + file.getAbsolutePath());
		
		FileOutputStream fos = new FileOutputStream(file);
		
		// write out shared mixtures, if any
		logger.debug("writing out " + shared.size() + " shared Mixtures");
		IOUtil.writeInt(fos, shared.size(), ByteOrder.LITTLE_ENDIAN);
		for (Mixture m : shared.values())
			m.write(fos);
		
		// write out HMMs
		logger.debug("writing out " + models.size() + " HMM paramters");
		IOUtil.writeInt(fos, models.size(), ByteOrder.LITTLE_ENDIAN);
		for (Hmm m : models.values())
			m.write(fos);
		
		// flush and close file
		fos.flush();
		fos.close();
	}
	
	/** 
	 * Produce a complete ASCII dump of the codebook
	 * @param bw
	 * @throws IOException
	 */
	public void dump(BufferedWriter bw) throws IOException {
		if (shared.size() > 0) {
			bw.append("# shared codebooks\n");
			for (Mixture m : shared.values())
				bw.append(m.toString());
		}
		
		if (models.size() > 0) {
			bw.append("# models\n");
			for (Hmm m : models.values())
				bw.append(m.toString());
		}
	}
	
	/**
	 * Initialize Accumulators for model parameters
	 */
	public void init() {
		logger.info("setting up Codebook accumulators");
		for (int id : shared.keySet()) {
			Mixture m = shared.get(id);
			try {
				sharedAccs.put(id, new MleMixtureAccumulator(m.fd, m.nd, m.diagonal() ? DensityDiagonal.class : DensityFull.class));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e.toString());
			}
		}
		
		// set up the model accumulators
		for (Hmm m : models.values()) {
			m.init();
			
			for (State s : m.s) {
				// don't forget to link the shared accumulators
				if (s instanceof SCState) {
					SCState sc = (SCState) s;
					sc.setSharedAccumulator(sharedAccs.get(sc.cb.id));
				}
			}
		}
	}
	
	/**
	 * Consume Accumulators of the referenced source Codebook
	 * @param source
	 */
	public void consume(Codebook source) {
		for (Entry<Integer, Mixture> e : shared.entrySet()) {
			if (!source.shared.containsKey(e.getKey())) {
				logger.fatal("source Accumulator for shared mixture " + e.getKey() + " not found -- exitting.");
				throw new RuntimeException();
			}
			
			if (!source.sharedAccs.containsKey(e.getKey())) {
				logger.fatal("source Accumulator for shared mixture " + e.getKey() + " not found -- exitting.");
				throw new RuntimeException();
			}
			
			sharedAccs.get(e.getKey()).propagate(source.sharedAccs.get(e.getKey()));
		}
		
		for (Entry<Integer, Hmm> e : models.entrySet()) {
			if (!source.models.containsKey(e.getKey())) {
				logger.fatal("source Accumulator for model " + e.getKey() + " not found -- exitting.");
				System.exit(1);
			}
			e.getValue().propagate(source.models.get(e.getKey()));
		}
	}
	
	/**
	 * Reestimate model parameters from accumulators
	 */
	public void reestimate() {
		logger.info("reestimating Codebook from accumulators");
		for (Entry<Integer, Mixture> e : shared.entrySet()) {
			Mixture old = e.getValue().clone();
			MleMixtureAccumulator.MleUpdate(old, MleDensityAccumulator.MleOptions.pDefaultOptions, Density.Flags.fAllParams, sharedAccs.get(e.getKey()), e.getValue());
		}
		for (Hmm m : models.values())
			m.reestimate();
	}
	
	/**
	 * Discard all accumulators
	 */
	public void discard() {
		logger.info("discarding all Codebook accumulators");
		
		sharedAccs.clear();
		
		for (Hmm m : models.values())
			m.discard();
	}
	
	/**
	 * Interpolate the local codebook with a different one (typically an earlier
	 * iteration). 
	 * 
	 * Step 4 of APIS, see Schukat-Talamazzini p. 300ff
	 * 
	 * @param rho interpolation weight: this = rho * source + (1 - rho) * this
	 * @param source
	 */
	public void interpolate(double rho, Codebook source) {
		for (Map.Entry<Integer, Mixture> m : shared.entrySet())
			m.getValue().pinterpolate(rho, source.shared.get(m.getKey()));
		
		for (Map.Entry<Integer, Hmm> m : models.entrySet())
			m.getValue().pinterpolate(rho, source.models.get(m.getKey()));
	}
	
	/**
	 * Get the originating file, if any.
	 * @return null if not loaded from file
	 */
	public File getOrigin() {
		return origin; 
	}
	
	public static final String SYNOPSIS = 
		"sikoried, 2/8/2011\n" +
		"Generate an ASCII dump of the codebook.\n" +
		"usage: arch.Codebook file > out\n";
	
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
			
		Codebook cb = new Codebook(new File(args[0]));
		
		cb.dump(new BufferedWriter(new OutputStreamWriter(System.out)));
	}
}
