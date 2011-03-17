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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Correlate two random variables (double arrays). Available 
 * correlation measures: Spearman rho, Pearson r
 * 
 * @author sikoried
 */
public final class Correlator {
	/**
	 * Compute Pearson's correlation coefficient r
	 * 
	 * @param a Random variable, double array
	 * @param b Random variable, double array
	 * @return -1 <= r <= 1
	 * @throws Exception if array sized don't match.
	 */
	public static double pearsonCorrelation(double [] a, double [] b)
			throws Exception {
		if (a.length != b.length)
			throw new Exception("DoubleArrayCorrelator: Array length not equal: "
							+ a.length + " <> " + a.length);

		double mean1 = 0;
		double mean2 = 0;

		for (int i = 0; i < a.length; i++) {
			mean1 += a[i];
			mean2 += b[i];
		}
		mean1 /= a.length;
		mean2 /= b.length;
		
		double sum1 = 0;
		double sum2 = 0;
		double sum3 = 0;
		for (int i = 0; i < b.length; i++) {
			sum1 += (a[i] - mean1) * (b[i] - mean2);
			sum2 += (a[i] - mean1) * (a[i] - mean1);
			sum3 += (b[i] - mean2) * (b[i] - mean2);
		}
		
		return sum1 / (Math.sqrt(sum2) * Math.sqrt(sum3));
	}

	/**
	 * Compute Spearman's rank correlation rho.
	 * 
	 * @param a Random variable, double array
	 * @param b Random variable, double array
	 * @return -1 <= rho <= 1
	 * @throws Exception if array sizes don't match
	 */
	public static double spearmanCorrelation(double[] a, double[] b)
			throws Exception {
		if (a.length != b.length)
			throw new Exception("DoubleArrayCorrelator: Array length not equal: "
							+ a.length + " <> " + b.length);

		a = valsToRank(a);
		b = valsToRank(b);

		double rho = 0;

		for (int i = 0; i < a.length; ++i)
			rho += ((a[i] - b[i]) * (a[i] - b[i]));

		rho = 1. - 6 * rho / (a.length * (a.length * a.length - 1));

		return rho;
	}

	/**
	 * Convert an array of double values to an array of corresponding ranks
	 * @param a
	 * @return
	 */
	private static double[] valsToRank(double [] a) {
		class Pair implements Comparable<Pair> {
			public double val;
			public int index;

			public Pair(int index, double val) {
				this.index = index;
				this.val = val;
			}

			public int compareTo(Pair p) {
				return (int) Math.signum(this.val - p.val);
			}
		}

		ArrayList<Pair> data = new ArrayList<Pair>();
		for (int i = 0; i < a.length; ++i)
			data.add(new Pair(i, a[i]));
		Collections.sort(data);

		int[] map = new int[a.length];
		double[] sorted = new double[a.length];
		double[] ranks = new double[a.length];

		for (int i = 0; i < a.length; ++i) {
			sorted[i] = data.get(i).val;
			map[i] = data.get(i).index;
		}

		for (int i = 0; i < sorted.length; ++i) {
			int cnt = 0;
			double r = i + 1;
			// accumulate for average rank
			while (i + 1 < sorted.length && sorted[i + 1] == sorted[i]) {
				cnt++;
				i++;
				r += (i + 1);
			}
			r /= (cnt + 1);
			// set average rank backwards
			while (cnt >= 0) {
				ranks[i - cnt] = r;
				cnt--;
			}
		}

		for (int i = 0; i < a.length; ++i)
			sorted[map[i]] = ranks[i];

		return sorted;
	}

	public final static String SYNOPSIS = 
		"usage: Correlator datafile1 <datafile2 ...>";

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println(Correlator.SYNOPSIS);
			System.exit(0);
		}

		LinkedList<String> names = new LinkedList<String>();
		LinkedList<double []> data = new LinkedList<double []>();

		// read in data:
		for (int i = 0; i < args.length; ++i) {
			try {
				BufferedReader in = null;
				if (args[i].equals("-")) {
					in = new BufferedReader(new InputStreamReader(System.in));
					args[i] = "STDIN";
				} else
					in = new BufferedReader(new FileReader(args[i]));
				LinkedList<LinkedList<Double>> vals = new LinkedList<LinkedList<Double>>();
				
				String l;
				int ln = 0;
				while ((l = in.readLine()) != null) {
					String [] split = l.split("\\s+");
					if (ln == 0) {
						for (int j = 0; j < split.length; ++j) {
							names.add(args[i] + ":" + j);
							vals.add(new LinkedList<Double>());
						}
					} else if (vals.size() != split.length)
						throw new IOException("Incomplete data at " + args[i] + ":" + ln);
					ln++;
					for (int j = 0; j < split.length; ++j)
						vals.get(j).add(new Double(split[j]));
				}
				in.close();

				for (LinkedList<Double> lld : vals) {
					double [] hlp = new double [lld.size()];
					for (int j = 0; j < lld.size(); ++j)
						hlp[j] = lld.get(j).doubleValue();
					data.add(hlp);
				}
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
			}
		}

		// compute each correlation
		for (int i = 0; i < data.size(); ++i) {
			for (int j = i + 1; j < data.size(); ++j) {
				System.out.println(names.get(i) + "<>" + names.get(j) + ": r   = "
						+ pearsonCorrelation(data.get(i), data.get(j)));
				System.out.println(names.get(i) + "<>" + names.get(j) + ": rho = "
						+ spearmanCorrelation(data.get(i), data.get(j)));
			}
		}
	}
}
