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
package com.github.sikoried.jstk.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Comparator;

public class Various {
	/**
	 * Analyze a confusion matrix and print the results on the referenced PrintStream
	 * @param result number of events [reference][classified]
	 * @param os Output PrintStream to write results to
	 */
	public static void analyzeConfusionMatrix(int [][] result, PrintStream os) throws IOException {
		os.println("confusion matrix:");

		int max = 0;
		for (int [] dim1 : result)
			for (int dim2 : dim1)
				if (dim2 > max) max = dim2;
		int fieldSize = (int)(Math.floor(Math.log10(max)) + 2.);
		
		for (int i = 0; i < fieldSize; ++i)
			os.print(" ");
		
		for (int i = 0; i < result.length; ++i)
			os.printf("%" + fieldSize + "s ", i);
		os.println();
		
		for (int r1 = 0; r1 < result.length; ++r1) {
			os.printf("%" + fieldSize + "s ", r1);
			for (int r2 = 0; r2 < result.length; ++r2)
				os.printf("%" + fieldSize + "s ", result[r1][r2]);
			os.println();
		}
		
		os.println("class recall precision f-measure");
		double rg = 0, pg = 0;
		for (int i = 0; i < result.length; ++i) {
			int recall = 0;
			int precision = 0;
			for (int j = 0; j < result.length; ++j) {
				recall += result[i][j];
				precision += result[j][i];
			}
			
			double r = (double) result[i][i] / recall;
			double p = (double) result[i][i] / precision;
			double f = 2. * r * p / (p + r);
			
			rg += r;
			pg += p;
			
			os.printf("%" + (int)(Math.floor(Math.log10(result.length)) + 2.) + "s %.2f %.2f %.2f\n", i, r, p, f);
		}
		
		rg /= result.length;
		pg /= result.length;
		
		os.printf("averg %.2f %.2f %.2f\n", rg, pg, 2. * rg * pg / (pg + rg));
		os.flush();
	}
	
	/**
	 * Find and locate the maxima and write them to the pos and val array. The
	 * output is sorted (descending).
	 * 
	 * @param in
	 * @param pos
	 * @param val
	 */
	public static <T> void max(T [] in, int [] pos, T [] val, Comparator<T> comp) {
		int u = 0;
		int n = pos.length;
				
		for (int i = 0; i < in.length; ++i) {
			T v = in[i];
			
			// locate the insert position
			int p = 0;
			while (p < u && comp.compare(v, val[p]) < 0)
				p++;
			
			// v is smaller than all other values
			if (p == n)
				continue;
			
			// shift the old values and indices
			for (int j = u - 2; j >= p; --j) { 
				pos[j+1] = pos[j]; 
				val[j+1] = val[j];
			}
			
			// insert
			pos[p] = i;
			val[p] = v;
			
			// keep track how many slots are used already
			if (u < n)
				u++;
		}
	}
	
	/**
	 * Find and locate the maxima and write them to the pos and val array. The
	 * output is sorted (descending).
	 * 
	 * @param in
	 * @param pos
	 * @param val
	 */
	public static void max(int [] in, int [] pos, int [] val) {
		int u = 0;
		int n = pos.length;
				
		for (int i = 0; i < in.length; ++i) {
			int v = in[i];
			
			// locate the insert position
			int p = 0;
			while (p < u && v < val[p])
				p++;
			
			// v is smaller than all other values
			if (p == n)
				continue;
			
			// shift the old values and indices
			for (int j = u - 2; j >= p; --j) { 
				pos[j+1] = pos[j]; 
				val[j+1] = val[j];
			}
			
			// insert
			pos[p] = i;
			val[p] = v;
			
			// keep track how many slots are used already
			if (u < n)
				u++;
		}
	}
	
	/**
	 * Find and locate the maxima and write them to the pos and val array. The
	 * output is sorted (descending).
	 * 
	 * @param in
	 * @param pos
	 * @param val
	 */
	public static void max(float [] in, int [] pos, float [] val) {
		int u = 0;
		int n = pos.length;
				
		for (int i = 0; i < in.length; ++i) {
			float v = in[i];
			
			// locate the insert position
			int p = 0;
			while (p < u && v < val[p])
				p++;
			
			// v is smaller than all other values
			if (p == n)
				continue;
			
			// shift the old values and indices
			for (int j = u - 2; j >= p; --j) { 
				pos[j+1] = pos[j]; 
				val[j+1] = val[j];
			}
			
			// insert
			pos[p] = i;
			val[p] = v;
			
			// keep track how many slots are used already
			if (u < n)
				u++;
		}
	}
	
	/**
	 * Find and locate the maxima and write them to the pos and val array. The
	 * output is sorted (descending).
	 * 
	 * @param in
	 * @param pos
	 * @param val
	 */
	public static void max(double [] in, int [] pos, double [] val) {
		int u = 0;
		int n = pos.length;
				
		for (int i = 0; i < in.length; ++i) {
			double v = in[i];
			
			// locate the insert position
			int p = 0;
			while (p < u && v < val[p])
				p++;
			
			// v is smaller than all other values
			if (p == n)
				continue;
			
			// shift the old values and indices
			for (int j = u - 2; j >= p; --j) { 
				pos[j+1] = pos[j]; 
				val[j+1] = val[j];
			}
			
			// insert
			pos[p] = i;
			val[p] = v;
			
			// keep track how many slots are used already
			if (u < n)
				u++;
		}
	}
	
	/**
	 * Find and return the maximum values in an descending order.
	 * 
	 * @param values
	 * @param n
	 * @return
	 */
	public static int [] maxv(int [] values, int n) {
		int [] p = new int [n];
		int [] v = new int [n];
		max(values, p, v);
		return v;
	}
	
	/**
	 * Find and return the indices of the maximum values in a descending order
	 * regarding the max values
	 * 
	 * @param values
	 * @param n
	 * @return
	 */
	public static int [] maxp(int [] values, int n) {
		int [] p = new int [n];
		int [] v = new int [n];
		max(values, p, v);
		return p;
	}
	
	/**
	 * Find and return the maximum values in an descending order.
	 * 
	 * @param values
	 * @param n
	 * @return
	 */
	public static float [] maxv(float [] values, int n) {
		int [] p = new int [n];
		float [] v = new float [n];
		max(values, p, v);
		return v;
	}
	
	/**
	 * Find and return the indices of the maximum values in a descending order
	 * regarding the max values
	 * 
	 * @param values
	 * @param n
	 * @return
	 */
	public static int [] maxp(float [] values, int n) {
		int [] p = new int [n];
		float [] v = new float [n];
		max(values, p, v);
		return p;
	}
	
	/**
	 * Find and return the maximum values in an descending order.
	 * 
	 * @param values
	 * @param n
	 * @return
	 */
	public static double [] maxv(double [] values, int n) {
		int [] p = new int [n];
		double [] v = new double [n];
		max(values, p, v);
		return v;
	}
	
	/**
	 * Find and return the indices of the maximum values in a descending order
	 * regarding the max values
	 * 
	 * @param values
	 * @param n
	 * @return
	 */
	public static int [] maxp(double [] values, int n) {
		int [] p = new int [n];
		double [] v = new double [n];
		max(values, p, v);
		return p;
	}
	
	public static final String SYNOPSIS = 
		"sikoried, 7/19/2011\n" +
		"Display the (sorted) max for each input line (floating point numbers).\n" +
		"util.Various num-max < input > output";
	
	public static void main(String [] args) throws IOException {
		if (args.length != 1) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		int n = Integer.parseInt(args[0]);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String l;
		while ((l = br.readLine()) != null) {
			String [] sp = l.split("\\s+");
			
			double [] val = new double [sp.length];
			for (int i = 0; i < sp.length; ++i)
				val[i] = Double.parseDouble(sp[i]);
			
			int [] p = maxp(val, n);
			
			for (int i = 0; i < n-1; ++i) 
				System.out.print(p[i] + " ");
			System.out.println(p[n-1]);
		}
	}
}
