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
package de.fau.cs.jstk.agmt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Compute the multi-rater Kappa according to Fleiss. In general, it puts the
 * observed agreement in relation to the expected agreement.
 * 
 * @author sikoried
 */
public final class Kappa implements AgreementMeasure {
	private Metric metric;

	public Kappa(Metric metric) {
		this.metric = metric;
	}

	public Metric getMetric() {
		return metric;
	}

	/**
	 * Calculate kappa value for given group of raters and records. Most common
	 * metric is CicchettiMetric.ABSOLUTE (also used by Tino).
	 * 
	 * @param data data[rater][subject] = rating
	 * @param metric Weighting metric to use.
	 * @return kappa
	 */
	public double agreement(double[][] data) {
		int cr = data.length; // cout raters
		int cs = data[0].length; // count subjects
		int mx = 0;
		int mn = Integer.MAX_VALUE;

		// determine max category used
		for (double[] rater : data) {
			for (double rating : rater) {
				if (rating > mx)
					mx = (int) rating;
				if (mn > rating)
					mn = (int) rating;
			}
		}
		
		if (metric instanceof CicchettiMetric)
			((CicchettiMetric) metric).c = mx - mn;

		double[][] emat = new double[cr][cr];
		double[][] omat = new double[cr][cr];
		double[][] kmat = new double[cr][cr];
		double[][][][] pmat = new double[cr][cr][mx + 1][mx + 1];
		double[][] prel = new double[cr][mx + 1];

		// calculate the prel matrix:
		// how likely gives a rater "a" the mark "x"
		for (int i = 0; i < cr; ++i) {
			for (double score : data[i])
				prel[i][(int) score] += 1;

			for (int j = mn; j <= mx; ++j)
				prel[i][j] /= cs;
		}

		// calc p matrix
		for (int i = 0; i < cr; ++i) {
			for (int j = i + 1; j < cr; ++j) {
				// System.out.println("calcing pmat for " + i + " and " + j);
				// we're working on these 2 raters now
				double[] ratera = data[i];
				double[] raterb = data[j];

				// now check agreement by incrementing the accordant bins
				// do this for all recordings
				for (int z = 0; z < cs; ++z) {
					pmat[i][j][(int) ratera[z]][(int) raterb[z]] += 1;
					pmat[j][i][(int) raterb[z]][(int) ratera[z]] += 1;
				}
			}
		}
		// normalisieren
		for (int i = 0; i < cr; ++i)
			for (int j = i; j < cr; ++j)
				for (int a = mn; a <= mx; ++a)
					for (int b = mn; b <= mx; ++b) {
						pmat[i][j][a][b] /= cs;
						pmat[j][i][a][b] /= cs;
					}

		// calc o matrix
		for (int a = 0; a < cr; ++a) {
			for (int b = a + 1; b < cr; ++b) {
				double o = 0;
				for (int x = mn; x <= mx; ++x)
					for (int y = mn; y <= mx; ++y)
						o += (pmat[a][b][x][y] * metric.weight(x, y));

				omat[a][b] = o;
				omat[b][a] = o;
			}
		}

		// calc e matrix
		for (int a = 0; a < cr; ++a) {
			for (int b = a + 1; b < cr; ++b) {
				double e = 0;
				for (int x = mn; x <= mx; ++x)
					for (int y = mn; y <= mx; ++y)
						e += (prel[a][x] * prel[b][y] * metric.weight(x, y));
				emat[a][b] = e;
				emat[b][a] = e;
			}
		}

		for (int a = 0; a < cr; ++a) {
			for (int b = a + 1; b < cr; ++b) {
				double k = ((omat[a][b] - emat[a][b]) / (1. - emat[a][b]));
				kmat[a][b] = k;
				kmat[b][a] = k;
			}
		}

		// all pair-wise kappas calculated, get the muti-rater agreement
		double kdf = 0;
		double _kdf = 0;
		for (int i = 0; i < cr; ++i) {
			for (int j = 0; j < cr; ++j) {
				if (i == j)
					continue;
				kdf += ((1. - emat[i][j]) * kmat[i][j]);
				_kdf += (1. - emat[i][j]);
			}
		}
		kdf /= _kdf;

		// print summary
		System.err.println("multi-rater kappa summary:");
		System.err.println(cr + " raters");
		System.err.println(cs + " patients");
		System.err.println("likert scale: [" + mn + "; " + mx + "]");
		System.err.println("metric: " + metric);
		for (int i = 0; i < cr; ++i)
			for (int j = i + 1; j < cr; ++j)
				System.err.println("  kappa(" + i + ", " + j + ") = "
						+ kmat[i][j]);
		System.err.println("multi-rater kappa = " + kdf);

		return kdf;
	}

	public static final String SYNOPSIS = "Usage: Kappa rater1 rater2 <rater3 ...>";

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println(Kappa.SYNOPSIS);
			System.exit(0);
		}

		double [][] data = new double[args.length][];

		// read in data:
		for (int i = 0; i < args.length; ++i) {
			try {
				BufferedReader in = new BufferedReader(args[i].equals("-") ? new InputStreamReader(System.in) : new FileReader(args[i]));
				ArrayList<Double> vals = new ArrayList<Double>();
				String l;
				while ((l = in.readLine()) != null)
					vals.add(new Double(l));
				in.close();

				data[i] = new double[vals.size()];
				for (int j = 0; j < vals.size(); ++j)
					data[i][j] = vals.get(j).doubleValue();
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
			}
		}
		
		Kappa kappa1 = new Kappa(new NominalMetric());
		Kappa kappa2 = new Kappa(new CicchettiMetric(CicchettiMetric.ABSOLUTE));
		Kappa kappa3 = new Kappa(new CicchettiMetric(CicchettiMetric.SQUARE));

		kappa1.agreement(data);
		kappa2.agreement(data);
		kappa3.agreement(data);
	}
}
