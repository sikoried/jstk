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
import java.util.Arrays;
import java.util.LinkedList;

/**
 * The BasicStats object computes min, max, median, mean and standard 
 * deviation of a given double or float array
 * 
 * @author sikoried
 */
public class BasicStats {
	/// number of observations for the statistics
	public int obs;
	
	/// minimum value of the input data array
	public double min;
	
	/// maximum value of the input data array
	public double max;
	
	/// median value of the input data array
	public double med;
	
	/// mean value of the input data array
	public double mean;
	
	/// standard deviation of the input data array
	public double sdev;
	
	public BasicStats() {
		// do nothing
	}
	
	public BasicStats(double [] data) {
		if (data != null && data.length > 0)
			computeStats(data);
	}
	
	public BasicStats(float [] data) {
		if (data != null && data.length > 0)
			computeStats(data);
	}
	
	void computeStats(double [] data) {
		obs = data.length;
		min = data[0];
		max = data[0];
		mean = data[0];
		sdev = data[0] * data[0];
		
		for (int i = 1; i < data.length; ++i) {
			double d = data[i];
			if (min > d) min = d;
			if (max < d) max = d;
			mean += d;
			sdev += d * d;
		}
		
		// mean and standard deviation
		mean /= data.length;
		sdev = Math.sqrt(sdev / data.length - mean * mean);
		
		Arrays.sort(data);
		med = data[data.length / 2];
	}
	
	void computeStats(float [] data) {
		min = data[0];
		max = data[0];
		mean = data[0];
		sdev = data[0] * data[0];
		
		for (int i = 1; i < data.length; ++i) {
			double d = data[i];
			if (min > d) min = d;
			if (max < d) max = d;
			mean += d;
			sdev += d * d;
		}
		
		// mean and standard deviation
		mean /= data.length;
		sdev = Math.sqrt(sdev / data.length - mean * mean);
		
		Arrays.sort(data);
		med = data[data.length / 2];
	}
	
	public static final String SYNOPSIS = 
		"usage: BasicStats data-file-1 <data-file-2 ...>";
	
	public static void main(String [] args) {
		if (args.length < 1) {
			System.out.println(SYNOPSIS);
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

		// for each data row...
		System.err.println("file:col obs min max median mean std-dev");
		for (int i = 0; i < data.size(); ++i) {
			BasicStats bs = new BasicStats(data.get(i));
			System.out.println(names.get(i) + " " + 
				bs.obs + " " +
				bs.min + " " +
				bs.max + " " +
				bs.med + " " +
				bs.mean + " " +
				bs.sdev + " "
				);
		}
	}
}
