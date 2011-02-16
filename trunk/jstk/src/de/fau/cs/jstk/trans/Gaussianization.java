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
package de.fau.cs.jstk.trans;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import de.fau.cs.jstk.arch.Configuration;
import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.io.FrameOutputStream;
import de.fau.cs.jstk.stat.Density;
import de.fau.cs.jstk.util.Pair;

/**
 * The Gaussianization enforces a Gaussian distribution for each feature
 * dimension.
 * 
 * @author sikoried
 */
public class Gaussianization {
	private static Logger logger = Logger.getLogger(Configuration.class);
	
	/**
	 * Gaussianize each feature dimension of the data list individually.
	 * @param data
	 */
	public static void gaussianize(List<double []> data) {
		if (data.size() == 0)
			return;
		
		int ns = data.size();
		int fd = data.get(0).length;
		
		// for each feature dimension
		for (int i = 0; i < fd; ++i) {
			// rank all the samples using the index within the list
			LinkedList<Pair<Integer, Double>> sorted = new LinkedList<Pair<Integer, Double>>();
			int k = 0;
			for (double [] x : data)
				sorted.add(new Pair<Integer, Double>(k++, x[i]));
			
			Collections.sort(sorted, new Comparator<Pair<Integer, Double>> () {
				public int compare(Pair<Integer, Double> o1,
						Pair<Integer, Double> o2) {
					return o1.b.compareTo(o2.b);
				}
			});
			
			k = 1;
			for (Pair<Integer, Double> p : sorted) {
				data.get(p.a)[i] = Density.quantile((double)(k) / ns);
				if (k < ns - 1)
					k++;
			}
		}
	}
	
	public static final String SYNOPSIS = 
		"sikoried, 5/12/2010\n" +
		"Gaussianize an input feature stream (Frame format; will cache all features).\n\n" +
		"usage: transformations.Gaussianization list outdir [indir]";
	
	public static void main(String[] args) throws Exception {
		if (args.length < 2 || args.length < 3) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		BasicConfigurator.configure();
		
		String listFile = args[0];
		String outDir = args[1] + System.getProperty("file.separator");
		String inDir = (args.length == 3 ? args[2] + System.getProperty("file.separator") : "");
		
		LinkedList<Pair<String, String>> iolist = new LinkedList<Pair<String, String>>();
		BufferedReader br = new BufferedReader(new FileReader(listFile));
		String lb;
		while ((lb = br.readLine()) != null) {
			String in = inDir + lb;
			String out = outDir + lb;
			
			iolist.add(new Pair<String, String>(in, out));
		}
		
		if (iolist.size() < 1) {
			logger.info("Gaussianization.main(): no input files.");
			System.exit(0);
		}
		
		while (iolist.size() > 0) {
			Pair<String, String> p = iolist.remove();
			FrameInputStream fr = new FrameInputStream(new File(p.a));
			FrameOutputStream fw = new FrameOutputStream(fr.getFrameSize(), new File(p.b));
			
			LinkedList<double []> data = new LinkedList<double []>();
			
			double [] buf = new double [fr.getFrameSize()];
			while (fr.read(buf))
				data.add(buf.clone());
			
			gaussianize(data);
			
			while(data.size() > 0)
				fw.write(data.remove());
			
			fw.close();
		}
	}
}
