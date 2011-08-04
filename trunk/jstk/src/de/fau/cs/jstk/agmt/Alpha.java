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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import de.fau.cs.jstk.util.Pair;

/**
 * Krippendorffs Alpha, e.g. http://en.wikipedia.org/wiki/Krippendorff%27s_Alpha
 * 
 * @author sikoried
 */
public final class Alpha implements AgreementMeasure {
	/** Metric to compare ratings */
	private Metric metric;

	/**
	 * Allocate a new Alpha instance with the given Metric
	 * @param metric
	 */
	public Alpha(Metric metric) {
		this.metric = metric;
	}

	public Metric getMetric() {
		return metric;
	}

	/**
	 * Calculate Krippendorff's Alpha. Missing ratings have to be
	 * Double.MAX_VALUE
	 * 
	 * @param data data[rater][subject] = rating
	 * @return alpha
	 */
	public double agreement(double[][] data) {
		int ns = data[0].length; // number of subjects
		int nc = 0; // number of classes
		@SuppressWarnings("unused")
		int nr = 0; // number of ratings
		int ne = data.length; // number of experts

		int[] m = new int[ns]; // ratings per subject

		// ensure that class id are consistent with value in case of nonnominal
		// metrics
		ArrayList<Double> flat = new ArrayList<Double>();
		for (int i = 0; i < data.length; ++i)
			for (int j = 0; j < data[i].length; ++j)
				flat.add(data[i][j]);
		Collections.sort(flat);

		HashMap<Double, Integer> classMap = new HashMap<Double, Integer>();
		for (double d : flat) {
			if (!classMap.containsKey(d))
				classMap.put(d, new Integer(nc++));
		}

		// initialize nc, nr, ne, m
		for (int i = 0; i < ne; ++i) {
			for (int j = 0; j < ns; ++j) {
				if (data[i][j] == Double.MAX_VALUE)
					continue;
				m[j]++;
				nr++;
			}
		}

		// construct coincidence matrix
		double[][] coincidence = new double[nc][nc];

		// for every subject...
		for (int s = 0; s < ns; ++s) {
			// ...check all pairs
			HashMap<Pair<Integer, Integer>, Integer> lc = new HashMap<Pair<Integer, Integer>, Integer>(); // local
																		// coincidences
			for (int i = 0; i < ne; ++i) {
				if (data[i][s] == Double.MAX_VALUE)
					continue;

				int cid1 = classMap.get(data[i][s]);

				for (int j = 0; j < ne; ++j) {
					if (i == j)
						continue;

					if (data[j][s] == Double.MAX_VALUE)
						continue;

					int cid2 = classMap.get(data[j][s]);

					Pair<Integer, Integer> key = new Pair<Integer, Integer>(cid1, cid2);

					if (lc.containsKey(key))
						lc.put(key, 1 + lc.get(key));
					else
						lc.put(key, 1);
				}
			}

			// transfer local coincidences to coincidence matrix
			Iterator<Map.Entry<Pair<Integer, Integer>, Integer>> it = lc.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Pair<Integer, Integer>, Integer> e = it.next();
				int cid1 = e.getKey().a;
				int cid2 = e.getKey().b;
				coincidence[cid1][cid2] += ((double) e.getValue() / (m[s] - 1));
			}

		}

		// count occurrences
		double na = 0;
		double[] occurrence = new double[nc];

		for (int i = 0; i < nc; i++) {
			for (int j = 0; j < nc; j++) {
				double ci = coincidence[i][j];
				if (ci == Double.MAX_VALUE)
					continue;
				occurrence[i] += ci;
				na += ci;
			}
		}

		double d_o = 0;
		for (int i = 0; i < nc; ++i) {
			for (int j = i + 1; j < nc; ++j) {
				d_o += metric.weight(i + 1, j + 1) * coincidence[i][j];
			}
		}
		d_o *= (na - 1);

		double d_e = 0;
		for (int i = 0; i < nc; ++i) {
			for (int j = i + 1; j < nc; ++j) {
				d_e += metric.weight(i + 1, j + 1) * occurrence[i]
						* occurrence[j];
			}
		}

		return 1. - d_o / d_e;
	}

	public static final String SYNOPSIS = 
		"sikoried, 6/29/2011\n" +
		"Compute Krippendorff's Alpha on the given rater files. Each file may contain\n" +
		"several raters, use - to read from stdin. Set missing observations to 'nan'\n\n" +
		"usage: agmt.Alpha file1 [file2 ...]";

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println(Alpha.SYNOPSIS);
			System.exit(1);
		}
		
		LinkedList<String> names = new LinkedList<String>();
		double [][] data = new double [0][];
		
		// read in data files
		for (int i = 0; i < args.length; ++i) {
			try {
				BufferedReader in = null;
				if (args[i].equals("-")) {
					in = new BufferedReader(new InputStreamReader(System.in));
					args[i] = "stdin";
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
					
					for (int j = 0; j < split.length; ++j) {
						if (split[j].equals("nan"))
							vals.get(j).add(Double.MAX_VALUE);
						else
							vals.get(j).add(new Double(split[j]));
					}
				}
				in.close();

				// new data array will need more columns!
				double [][] ndata = new double [data.length + vals.size()][];
				
				// transfer old stuff
				for (int j = 0; j < data.length; ++j)
					ndata[j] = data[j];
				
				int k = data.length;
				for (LinkedList<Double> lld : vals) {
					double [] hlp = new double [lld.size()];
					for (int j = 0; j < lld.size(); ++j)
						hlp[j] = lld.get(j).doubleValue();
					ndata[k++] = hlp;
				}
				
				data = ndata;
			} catch (IOException e) {
				System.err.println(e);
				e.printStackTrace();
			}
		}
		
		if (data.length < 2) {
			System.err.println("You need to specify at least two raters!");
			System.exit(1);
		} else
			System.err.println("Read " + data.length + " raters with " + data[0].length + " ratings each");

		Alpha alpha1 = new Alpha(new NominalMetric(true));
		Alpha alpha2 = new Alpha(new IntervalMetric());
		Alpha alpha3 = new Alpha(new RatioMetric());

		System.out.println("alpha nominal = " + alpha1.agreement(data));
		System.out.println("alpha interval = " + alpha2.agreement(data));
		System.out.println("alpha ratio = " + alpha3.agreement(data));
	}
}
