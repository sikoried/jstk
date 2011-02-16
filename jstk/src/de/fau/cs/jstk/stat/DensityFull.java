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
import java.util.Scanner;

import org.apache.log4j.Logger;

/**
 * The DensityFull is a Gaussian density with full covariance matrix. The
 * computation is sped up using Cholesky decomposition. This is also numerically 
 * stable.
 * 
 * K = L L^T => K^-1 = L^T^-1 L^-1
 * z = x - mue
 * 
 * z^T K^-1 z = || L^-1 x ||^2 = || y ||^2
 * 
 * y = L^-1 x => x = L y; solve using forward substitution
 * 
 * @author sikoried
 *
 */
public final class DensityFull extends Density {
	private static Logger logger = Logger.getLogger(DensityFull.class);
			
	/** regularization constant */
	public transient static final double GAMMA = 1e-3;
	
	/** cov = L L^T where L is a lower triangular, packed matrix; Cholesky decomposition! */
	transient public double [] L;
	
	/** cache for forward substitution */
	transient private double [] y;
	
	/**
	 * Allocate a new full covariance density
	 * @param dim
	 */
	public DensityFull(int dim) {
		super(dim);
		cov = new double [fd * (fd + 1) / 2];
		L = new double [fd * (fd + 1) / 2];
		y = new double [fd];
	}
	
	/**
	 * Allocate a new full covariance density and fill it with the referenced
	 * copy.
	 * @param copy
	 */
	public DensityFull(DensityFull copy) {
		this(copy.apr, copy.mue, copy.cov);
		this.id = copy.id;
	}

	/**
	 * Create a new Density with diagonal covariance
	 * @param apr prior probability
	 * @param mue mean vector
	 * @param cov covariance vector
	 */
	public DensityFull(double apr, double [] mue, double [] cov) {
		this(mue.length);
		fill(apr, mue, cov);
	}

	/**
	 * Allocate a new full covariance density and fill it from the Scanner.
	 * Expects lower triangular matrix!
	 * @param fd
	 * @param scanner
	 * @throws IOException
	 */
	public DensityFull(int fd, Scanner scanner) throws IOException {
		this(fd);
		
		while (!scanner.hasNextDouble())
			scanner.next();
		double a = scanner.nextDouble();
		
		double [] mue = new double [fd];
		while (!scanner.hasNextDouble())
			scanner.next();
		for (int i = 0; i < fd; ++i)
			mue[i] = scanner.nextDouble();
		
		double [] cov = new double [fd * (fd + 1) / 2];
		while (!scanner.hasNextDouble())
			scanner.next();
		for (int i = 0; i < cov.length; ++i)
			cov[i] = scanner.nextDouble();
		
		fill(a, mue, cov);
	}
	
	/** Update the internal variables. Required after modification. */
	public void update() {
		// check for NaN!
		long nans = 0;
		long minc = 0;
		
		for (int i = 0; i < fd; ++i) {
			if (Double.isNaN(mue[i])) {
				mue[i] = 0;
				nans++;
			}
			if (Double.isNaN(cov[i])) {
				cov[i] = MIN_COV;
				nans++;
			} else if (Math.abs(cov[i]) < MIN_COV) {
				double sig = (cov[i] >= 0. ? 1. : -1.);
				cov[i] = sig * MIN_COV;
				minc++;
			}
		}
		
		// some more values to check for full covariance...
		for (int i = fd; i < cov.length; ++i) {
			if (Double.isNaN(cov[i])) {
				cov[i] = MIN_COV;
				nans++;
			} else if (Math.abs(cov[i]) < MIN_COV) {
				double sig = (cov[i] >= 0. ? 1. : -1.); 
				cov[i] = sig * MIN_COV;
				minc++;
			}
		}
		
		lapr = Math.log(apr);
		
		if (Double.isNaN(apr) || Double.isNaN(lapr)) {
			apr = 1e-10;
			lapr = Math.log(1e-10);
			nans++;
		}
		
		if (nans > 0)
			logger.fatal("Density#" + id + ".update(): fixed " + nans + " NaN values (this should NEVER be!)");
		if (minc > 0)
			logger.info("Density#" + id + ".update(): enforced " + minc + " minimum covariances");
		
		
		// construct the matrix
		double [][] help = new double [fd][fd];
		int k = 0;
		for (int i = 0; i < fd; ++i) {
			for (int j = 0; j <= i; ++j)
				help[i][j] = help[j][i] = cov[k++];
		}
		
		// regularize the matrix on demand
		Jama.Matrix cm = new Jama.Matrix(help);
		Jama.CholeskyDecomposition chol = null;
		double gamma = GAMMA;
		
		// we might need to regularize the covariance matrix
		while (gamma < 1.) {
			chol = cm.chol();
			
			// yay, works!
			if (chol.isSPD())
				break;
						
			// regularize...
			double trace = cm.trace();
			for (int i = 0; i < fd; ++i) {
				double regularized = (1. - gamma) * cm.get(i,i) + gamma * trace / fd;
				cm.set(i, i, regularized);
			}
			
			gamma += gamma;
		}
		
		// regularization not possible, fall back to diagonal matrix
		if (!chol.isSPD()) {
			logger.fatal("DensityFull.update(): regularization of covariance matrix impossible, falling back to diagonal");
			k = 0;
			for (int i = 0; i < fd; ++i) {
				for (int j = 0; j <= i; ++j) {
					if (i == j)
						cm.set(i, i, cov[k]);
					else {
						// set other values to zero
						cov[k] = 0.;
						cm.set(i, j, 0.);
						cm.set(j, i, 0.);
					}
					k++;
				}
			}
			
			// redo the decomposition
			chol = cm.chol();
		}
		
		Jama.Matrix mat = chol.getL();
		
		
		// log |K| = log | L L^T | = log (det |L|)^2 = 2 * sum_i log L(i,i) 
		logdet = 0.;
		for (int i = 0; i < fd; ++i)
			logdet += Math.log(mat.get(i,i));
		logdet *= 2.;
		
		// save the lower triangular matrix in a packed form
		k = 0;
		for (int i = 0; i < fd; ++i) {
			for (int j = 0; j <= i; ++j)
				L[k++] = mat.get(i, j);
		}
	}
	
	/**
	 * Create a deep copy of this instance.
	 */
	public Density clone() {
		return new DensityFull(this);
	}
	
	/**
	 * Evaluate the density for the given sample vector x. score keeps the
	 * probability (without the prior).
	 * @param x feature vector
	 * @return prior times score
	 */
	public double evaluate(double [] x) {
		// score = exp(-.5 * ( log(det) + fd*log(2*pi) + (x-mue)^T cov^-1 (x-mue)
		
		score = logdet + logpiconst;
		
		// forward substition Ly = (x-mue)
		// y1 = x1 / L[1,1]
		// y2 = (x2 - y1 L[2,1])/L[2,2]
		// ...
		int k = 0;
		for (int i = 0; i < fd; ++i) {
			y[i] = x[i] - mue[i];
			for (int j = 0; j < i; ++j)
				y[i] -= y[j] * L[k++];
			y[i] /= L[k++];
		}
		
		// scalar product
		for (int i = 0; i < fd; ++i)
			score += y[i]*y[i];
		
		score *= -.5;
		
		lh = lapr + score;
		
		score = Math.exp(score);
		
		if (Double.isNaN(score) || score < MIN_PROB)
			score = MIN_PROB;
		
		ascore = apr * score;
		
		return ascore;
	}
	
	/**
	 * Obtain a string representation of the density.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer(); 
		sb.append("apr = " + apr + "\nmue =");
		for (double m : mue)
			sb.append(" " + m);
		sb.append("\ncov =\n");
		int k = 0;
		for (int i = 0; i < fd; ++i) {
			sb.append("\t");
			for (int j = 0; j <= i; ++j)
				sb.append(cov[k++] + "\t");
			sb.append("\n");
		}
		// sb.append("logdet = " + logdet + "\nlogpiconst = " + logpiconst);
		return sb.toString();
	}
}
