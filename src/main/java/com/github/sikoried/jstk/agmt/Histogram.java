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
package com.github.sikoried.jstk.agmt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Use the Histogram class to compute histograms (binnings) of real valued data
 * @author sikoried
 *
 */
public class Histogram {
	/**
	 * Compute the histogram to a given list of values. The output array
	 * contains the values in ascending order
	 * @param data real valued data
	 * @param precision set the precision (number of considered digits; 0 for all digits)
	 * @return double['id'][0] = value, double['id'][1] = count where id = 0, ..., count(disjoint(data))
	 */
	public static double [][] histogram(double [] data, double precision) {
		HashMap<Double, Integer> bin = new HashMap<Double, Integer>();
		for (double d : data) {
			// enforce max precision
			if (precision != 0.)
				d = (double)((long)(d / precision)) * precision;
			
			if (!bin.containsKey(d))
				bin.put(d, 0);
			
			bin.put(d, bin.get(d) + 1);
		}
		
		double [][] hist = new double [bin.keySet().size()][2];
		ArrayList<Double> skeys = new ArrayList<Double>(bin.keySet());
		Collections.sort(skeys);
		for (int i = 0; i < hist.length; ++i) {
			hist[i][0] = skeys.get(i);
			hist[i][1] = bin.get(hist[i][0]);
		}
		
		return hist;
	}

	public static final String SYNOPSIS =
		"usage: agmt.Histogram [precision] < data > histo ";
	
	public static void main(String[] args) throws Exception {
		// allow a maximum precision 
		double mp = 0.;
		
		if (args.length == 1)
			mp = Double.parseDouble(args[0]);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		ArrayList<Double> vals = new ArrayList<Double>();
		String l;
		while ((l = in.readLine()) != null)
			vals.add(Double.parseDouble(l.trim()));
		
		double [] data = new double [vals.size()];
		for (int i = 0; i < data.length; ++i)
			data[i] = vals.get(i);
		
		double [][] hist = Histogram.histogram(data,mp);
		for (double [] d : hist)
			System.out.println(d[0] + "\t" + d[1]);
	}
}
