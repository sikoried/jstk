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
package de.fau.cs.jstk.util;

import java.io.File;

import de.fau.cs.jstk.stat.Density;
import de.fau.cs.jstk.stat.DensityDiagonal;
import de.fau.cs.jstk.stat.Mixture;

public class Gnuplot {
	/** Generate the gnuplot command (parametric) for this density, accounting 
	 * for the first 2 dimensions 
	 */
	public static String covarianceAsGnuplot(Density d) {
		double [][] cov = new double [2][2];
		
		// build covariance matrix
		if (d instanceof DensityDiagonal) {
			cov[0][1] = cov[1][0] = 0.;
			cov[0][0] = d.cov[0];
			cov[1][1] = d.cov[1];
		} else {
			cov[0][0] = d.cov[0];
			cov[0][1] = cov[1][0] = d.cov[1];
			cov[1][1] = d.cov[2];
		}
		
		// get the eigen vectors and values
		Jama.EigenvalueDecomposition eigt = new Jama.Matrix(cov).eig();
		
		// width and height of the cov ellipse: sqrt of the eigenvalues
		double [] wh = eigt.getRealEigenvalues();
		
		wh[0] = Math.sqrt(wh[0]);
		wh[1] = Math.sqrt(wh[1]);
		
		double [][] eig = eigt.getV().getArray();
		
		// the eigen decomposition sorts the eigen values and vectors by
		// ascencending eigen value, thus we need to swap if the covariance
		// are in a descending order
		if (cov[0][0] > cov[1][1] && wh[0] < wh[1]) {
			double h = wh[0];
			wh[0] = wh[1];
			wh[1] = h;
			
			h = eig[0][0];
			eig[0][0] = eig[0][1];
			eig[0][1] = h;
			
			h = eig[1][0];
			eig[1][0] = eig[1][1];
			eig[1][1] = h;
		}
		
		// rotation angle from the eigenvector belonging to the first dimension
		double angle = Math.atan2(eig[0][1], eig[0][0]); 
		
		final String template1 = "X0 + DIM1 * cos(A) * cos(t) - DIM2 * sin(A) * sin(t)";
		final String template2 = "X1 + DIM1 * sin(A) * cos(t) + DIM2 * cos(A) * sin(t)";
		
		String rep1 = template1.replace("A", "" + angle);
		rep1 = rep1.replace("DIM1", "" + wh[0]);
		rep1 = rep1.replace("DIM2", "" + wh[1]);
		rep1 = rep1.replace("X0", "" + d.mue[0]);
		rep1 = rep1.replace("X1", "" + d.mue[1]);
		
		String rep2 = template2.replace("A", "" + angle);
		rep2 = rep2.replace("DIM1", "" + wh[0]);
		rep2 = rep2.replace("DIM2", "" + wh[1]);
		rep2 = rep2.replace("X0", "" + d.mue[0]);
		rep2 = rep2.replace("X1", "" + d.mue[1]);
		
		return rep1 + ", " + rep2;
	}
	
	public static final String SYNOPSIS = 
		"usage: statistics.MixtureDensity data-file <details:none|id|details> mixture1 [mixture2 ...]";
	
	public static void main(String [] args) throws Exception {
		if (args.length < 3) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		int desc = 0;
		if (args[1].equals("none"))
			desc = 0;
		else if (args[1].equals("id"))
			desc = 1;
		else if (args[1].equals("details"))
			desc = 2;
		else
			throw new Exception("illegal parameter " + args[1]);
		
		System.out.println("set term png");
		System.out.println("set parametric");
		System.out.println("plot '" + args[0] + "' w d notitle, \\");
		
		for (int i = 2; i < args.length; ++i) {
			Mixture md = Mixture.readFromFile(new File(args[i]));
			for (int j = 0; j < md.nd; ++j) {
				int oldid = md.components[j].id;
				md.components[j].id = j;
				System.out.print(covarianceAsGnuplot(md.components[j]));
				if (desc == 0)
					System.out.print(" notitle");
				else if (desc == 1)
					System.out.print(" t '" + j + "'");
				else if (desc == 2)
					System.out.print(String.format("t '%.2f,%.2f : %.2f'", md.components[j].mue[0], md.components[j].mue[1], md.components[j].apr));
				else
					throw new Exception("illegal desc type " + desc);
				
				if (j < md.nd - 1) 
					System.out.println(", \\");
				md.components[j].id = oldid;
			}
		}
	
		System.out.println(";");
		
		System.out.println("quit");
	}
}
