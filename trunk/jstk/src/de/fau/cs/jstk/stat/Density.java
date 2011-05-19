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
package de.fau.cs.jstk.stat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.apache.log4j.Logger;

import de.fau.cs.jstk.io.IOUtil;
import de.fau.cs.jstk.util.Arithmetics;

/**
 * The abstract Density class provides the basic assets of a Gaussian density.
 * 
 * @author sikoried
 *
 */
public abstract class Density {
	private static Logger logger = Logger.getLogger(Density.class);
	
	/** minimum density score */
	public static final double MIN_PROB = 1e-256;
	
	/** minimum covariance */
	public static final double MIN_COV = 1e-10;
	
	/** minimum weight in case of Mixture */
	public static final double MIN_WEIGHT = 1e-10;
	
	/** feature dimension */
	public int fd;
	
	/** Density ID */
	public int id = 0;

	/** prior probability */
	public double apr = 1.;
	
	/** log(apr) */
	protected double lapr = 0.;
	
	/** log likelihood: log(apr*score) */
	public double lh = 0.;
	
	/** cached version of log(Det) */
	protected double logdet;
	
	/** cached version of the log(pi) constant */
	protected double logpiconst;
	
	/** cached score from the last evaluate call, no prior! */
	public double score;
	
	/** cached score from the last evaluate call, with prior! */
	public double ascore;
	
	/** mean vector */
	public double [] mue;
	
	/** covariance matrix: either diagonal, or packed lower triangle */
	public double [] cov;
	
	/** accumulator for sufficient statistics */
	public Accumulator accu = null;
	
	/**
	 * Create a new density with a certain feature dimension
	 * @param dim Feature dimesion
	 */
	public Density(int dim) {
		fd = dim;
		logpiconst = (double) fd * Math.log(2. * Math.PI);
		mue = new double [fd];
	}
	
	/**
	 * Evaluate the density for the given sample vector x. score keeps the
	 * probability (without the prior).
	 * @param x feature vector
	 * @return prior times score
	 */
	public abstract double evaluate(double [] x);
	
	/**
	 * Set the parameters of the density.
	 * @param apr prior probability
	 * @param mue mean vector
	 * @param cov covariance vector
	 */
	public void fill(double apr, double [] mue, double [] cov) {
		this.apr = apr;
		System.arraycopy(mue, 0, this.mue, 0, fd);
		System.arraycopy(cov, 0, this.cov, 0, cov.length);
		update();
	}
	
	/**
	 * Read the parameters of the density from the given ObjectInputStream
	 * @param is
	 * @throws IOException
	 */
	public void fill(InputStream is) throws IOException {
		id = IOUtil.readInt(is, ByteOrder.LITTLE_ENDIAN);
		apr = IOUtil.readDouble(is, ByteOrder.LITTLE_ENDIAN);
	
		if (!IOUtil.readDouble(is, mue, ByteOrder.LITTLE_ENDIAN))
			throw new IOException("Could not read mean vector");
		
		if (!IOUtil.readDouble(is, cov, ByteOrder.LITTLE_ENDIAN))
			throw new IOException("Could not read covariance field");
		
		update();
	}
	
	/**
	 * Dump prior, mean and covariance to the given ObjectOutputStream
	 * @param os
	 * @throws IOException
	 */
	public void write(OutputStream os) throws IOException {
		IOUtil.writeInt(os, id, ByteOrder.LITTLE_ENDIAN);
		IOUtil.writeDouble(os, apr , ByteOrder.LITTLE_ENDIAN);
		IOUtil.writeDouble(os, mue, ByteOrder.LITTLE_ENDIAN);
		IOUtil.writeDouble(os, cov, ByteOrder.LITTLE_ENDIAN);
		
		os.flush();
	}
	
	/**
	 * Update the internally cached variables. Required after modification.
	 */
	public abstract void update();
	
	/**
	 * If there is no current accumulator, initialize one
	 */
	public void init() {
		if (accu != null)
			return;
		accu = new Accumulator();
	}
	
	/**
	 * Absorb the accumulator of the given Density
	 * @param d
	 */
	public void absorb(Density d) {
		if (d.accu != null)
			accu.absorb(d.accu);
	}
	
	/**
	 * Discard the current accumulator
	 */
	public void discard() {
		accu = null;
	}
	
	/**
	 * Reestimate the density from accumulator statistics and set the weight/prior.
	 */
	public void reestimate(double weight) {
		if (accu == null)
			return;
		
		if  (accu.n < fd + 1) {
			logger.info("less observations than feature dimension -- no re-estimation!");
			return;
		}
		
		// normalize statistics
		Arithmetics.sdiv2(accu.mue, accu.apr);
		Arithmetics.sdiv2(accu.cov, accu.apr);
		
		// conclude covariance computation by subtracting new mean
		if (this instanceof DensityDiagonal) {
			for (int i = 0; i < fd; ++i)
				accu.cov[i] -= accu.mue[i] * accu.mue[i];
		} else {
			int l = 0;
			for (int j = 0; j < fd; ++j)
				for (int k = 0; k <= j; ++k)
					accu.cov[l++] -= accu.mue[j] * accu.mue[k];
		}
		
		// update statistics
		fill(weight, accu.mue, accu.cov);
		
		// discard accumulator
		discard();
	}
	
	/**
	 * For the given density posterior, accumulate x
	 * @param gamma
	 * @param x
	 */
	public void accumulate(double gamma, double [] x) {
		accu.n++;
		
		// save your breath
		if (gamma == 0.)
			return;
		
		accu.apr += gamma;

		if (this instanceof DensityDiagonal) {
			for (int k = 0; k < fd; ++k) {
				double t = gamma * x[k];
				accu.mue[k] += t;
				accu.cov[k] += t * x[k];
			}
		} else {
			for (int k = 0; k < fd; ++k)
				accu.mue[k] += gamma * x[k];
			
			int m = 0;
			for (int k = 0; k < fd; ++k)
				for (int l = 0; l <= k; ++l)
					accu.cov[m++] += gamma * x[k] * x[l];
		}
	}
	
	/**
	 * The Density.Accumulator keeps track of the accumulated statistics
	 *  
	 * @author sikoried
	 */
	public final class Accumulator {
		/** number of observations */
		long n = 0;
		
		/** weight accumulator */
		double apr = 0;
		
		/** mean value accumulator */
		double [] mue = new double [fd];
		
		/** covariance accumulator */
		double [] cov = new double [Density.this.cov.length];
		
		/**
		 * Add the referenced accumulator's scores to this one's.
		 * @param source
		 */
		void absorb(Accumulator source) {
			if (source.n == 0)
				return;
			
			// observations
			n += source.n;
			
			// stats
			apr += source.apr;
			Arithmetics.vadd2(mue, source.mue);
			Arithmetics.vadd2(cov, source.cov);
			
			// flush source to prevent further absorbtion
			source.flush();
		}
		
		/**
		 * Flush the accumulator by setting all values to zero
		 */
		void flush() {
			n = 0;
			Arrays.fill(mue, 0.);
			Arrays.fill(cov, 0.);
		}
	}
	
	/**
	 * Reset all the components.
	 */
	public void clear() {
		apr = 0.;
		for (int i = 0; i < fd; ++i)
			mue[i] = 0.;
		for (int i = 0; i < cov.length; ++i)
			cov[i] = 0.;
		lapr = 0.;
		lh = 0.;
	}
	
	/**
	 * Construct a vector containing the specified components
	 * @param prior include the prior
	 * @param mue include the mean vector
	 * @param cov include the covariance
	 * @return
	 */
	public double [] superVector(boolean prior, boolean mue, boolean cov) {
		int size = 0;
		
		// get size
		if (prior)
			size += 1;
		if (mue)
			size += fd;
		if (cov)
			size += fd;
		
		double [] sv = new double [size];
		
		// copy data
		int i = 0;
		if (prior)
			sv[i++] = apr;
		
		if (mue)
			for (double m : this.mue)
				sv[i++] = m;
		
		if (cov) {
			Density d = this;
			if (d instanceof DensityFull)
				d = new DensityDiagonal((DensityFull) d);
			for (double c : d.cov)
				sv[i++] = c;
		}
		
		return sv;
	}
	
	/**
	 * Clone the instance (deep copy)
	 */
	public abstract Density clone();
	
	/**
	 * Compute the quantile (inverse CDF) of a N(0,1) distribution. Based on
	 * some internet junk which was derived using Maple/Matlab.
	 * @param p
	 * @return
	 */
	public static double quantile(double p) {
		final double cutoff = 1e-32;
		
		double f;
		
		if (p < cutoff)
			p = cutoff;
		if (p > 1. - cutoff)
			p = 1. - cutoff;
		
		if (p < 0.0465 || p > 0.9535) {
			// left and right tail
			double r = 
				p < 0.0465 
					? Math.sqrt(Math.log(1. / (p * p)))
					: Math.sqrt(Math.log(1. / ((p - 1.) * (p - 1.))));
					
			final double c3 = -1.000182518760158122;
			final double y0 = 16.682320830719986527;
			final double y1 =  4.120411523939115059;
			final double y2 =  0.029814187308200211;
			final double d0 =  7.173787663925508066;
			final double d1 =  8.759693508958633869;
			
			f = c3 * r + y2 + (y1 * r + y0) / (r * r + d1 * r + d0);
			
			if (p > 0.9535)
				f *= -1;
		} else {
			// center region
			double q = p - .5;
			double r = q * q;

			final double a2 =  1.246899760652504;
			final double z0 =  0.195740115269792;
			final double z1 = -0.652871358365296;
			final double b0 =  0.155331081623468;
			final double b1 = -0.839293158122257;
			
			f = q * (a2 + (z1 * r + z0) / (r * r + b1 * r + b0));
		}
			
		return f;
	}
	
	/**
	 * Compute the cumulative density function (CDF) of a N(0,1) distribution.
	 * Based on Hart, J.F. et al, 'Computer Approximations', Wiley 1968 (FORTRAN 
	 * transcript).
	 * @param z
	 * @return
	 */
	public static double cdf(double z) {
		double zabs;
		double p;
		double expntl, pdf;

		final double p0 = 220.2068679123761;
		final double p1 = 221.2135961699311;
		final double p2 = 112.0792914978709;
		final double p3 = 33.91286607838300;
		final double p4 = 6.373962203531650;
		final double p5 = .7003830644436881;
		final double p6 = .3526249659989109E-01;

		final double q0 = 440.4137358247522;
		final double q1 = 793.8265125199484;
		final double q2 = 637.3336333788311;
		final double q3 = 296.5642487796737;
		final double q4 = 86.78073220294608;
		final double q5 = 16.06417757920695;
		final double q6 = 1.755667163182642;
		final double q7 = .8838834764831844E-1;

		final double cutoff = 7.071;
		final double root2pi = 2.506628274631001;

		zabs = Math.abs(z);
		if (z > 37.0) 
			return 1.;
		if (z < -37.0) 
			return 0.;

		expntl = StrictMath.exp(-.5*zabs*zabs);
		pdf = expntl/root2pi;

		if (zabs < cutoff) {
			p = expntl*((((((p6*zabs + p5)*zabs + p4)*zabs + p3)*zabs +
					p2)*zabs + p1)*zabs + p0)/(((((((q7*zabs + q6)*zabs +
							q5)*zabs + q4)*zabs + q3)*zabs + q2)*zabs + q1)*zabs +
							q0);
		} else {
			p = pdf/(zabs + 1.0/(zabs + 2.0/(zabs + 3.0/(zabs + 4.0/
					(zabs + 0.65)))));
		}

		if (z < 0.)
			return p;
		else
			return 1. - p;
	}
}
