/*
	Copyright (c) 2011
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
package de.fau.cs.jstk.stat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

public final class MleMixtureAccumulator {
	private static Logger logger = Logger.getLogger(MleMixtureAccumulator.class);
	
	private Class<? extends Density> host;
	
	private int fd, nd;
	
	private MleDensityAccumulator [] accs;
	
	public MleMixtureAccumulator(int fd, int nd, Class<? extends Density> host) 
			throws ClassNotFoundException {
		this.fd = fd;
		this.nd = nd;
		this.host = host;
		
		// verify that we support that host density
		if (!(host.equals(DensityDiagonal.class) || host.equals(DensityFull.class)))
			throw new ClassNotFoundException("MleMixtureAccumulator not implemented for " + host.toString());
		
		// allocate accumulators
		accs = new MleDensityAccumulator [nd];
		for (int i = 0; i < nd; ++i)
			accs[i] = new MleDensityAccumulator(fd, host);
	}
	
	public MleMixtureAccumulator(MleMixtureAccumulator copy) {
		this.fd = copy.fd;
		this.nd = copy.nd;
		this.host = copy.host;
		
		accs = new MleDensityAccumulator [nd];
		for (int i = 0; i < nd; ++i)
			accs[i] = new MleDensityAccumulator(copy.accs[i]);
	}	
	
	public MleMixtureAccumulator(InputStream is) throws IOException {
		// TODO
		throw new RuntimeException("Method not yet implemented");
	}
	
	public void write(OutputStream os) throws IOException {
		// TODO
		throw new RuntimeException("Method not yet implemented");
	}
	
	public void accumulate(double gamma, double [] x, int i) {
		accs[i].accumulate(gamma, x);
	}
	
	public void accumulate(double [] gamma, double [] x) {
		for (int i = 0; i < nd; ++i)
			accs[i].accumulate(gamma[i], x);
	}
	
	public void propagate(MleMixtureAccumulator source) {
		if (fd != source.fd || nd != source.nd)
			throw new RuntimeException("Feature dim and/or number of densities mismatch!");
		
		for (int i = 0; i < nd; ++i)
			accs[i].propagate(source.accs[i]);
	}
	
	public void interpolate(MleMixtureAccumulator source, double weight) {
		if (fd != source.fd || nd != source.nd)
			throw new RuntimeException("Feature dim and/or number of densities mismatch!");
		
		for (int i = 0; i < nd; ++i)
			accs[i].interpolate(source.accs[i], weight);
	}
	
	public void flush() {
		for (MleDensityAccumulator a : accs)
			a.flush();
	}
	
	public static void MleUpdate(Mixture min, 
			MleDensityAccumulator.MleOptions opt, 
			Density.Flags flags, 
			MleMixtureAccumulator acc, Mixture mout) {
		
		// compute normalization for weights (sum of component occupancies)
		double norm = 0.0;
		for (int i = 0; i < min.nd; ++i)
			norm += acc.accs[i].occ;
		
		if (norm == 0.0) {
			logger.info("No occupancy logged; aborting reestimation");
			return;
		}
		
		for (int i = 0; i < min.nd; ++i)
			MleDensityAccumulator.MleUpdate(min.components[i], opt, flags, 
					acc.accs[i], acc.accs[i].occ / norm, mout.components[i]);
	}
}
