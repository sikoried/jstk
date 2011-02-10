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

import de.fau.cs.jstk.exceptions.*;

import java.io.*;
import java.util.*;

public class DataSet {
	// miscellaneous
	private String name;
	
	public DataSet(String name) {
		this.name = name;
	}
	
	public int getNumberOfClasses() {
		return ht1.keySet().size();
	}
	
	public int getNumberOfSamples() {
		return samples.size();
	}
	
	public String getDataSetName() {
		return name;
	}
	
	public void setDataSetName(String name) {
		this.name = name;
	}
	
	// ID management
	private static int nextId = 0;
	private HashMap<Integer, String> ht1 = new HashMap<Integer, String>();
	private HashMap<String, Integer> ht2 = new HashMap<String, Integer>();
	
	/**
	 * Resolve a class ID to its original class string
	 * @param id
	 * @return original class name
	 * @throws DataSetException
	 */
	public String idToName(int id) throws DataSetException {
		if (!ht1.containsKey(id))
			throw new DataSetException("invalid key");
		return ht1.get(id);
	}
	
	/**
	 * Resolve a class string to its internal class ID. If this is the first request
	 * for this class name, a new ID is generated.
	 * @param id
	 * @return unique numeric class ID
	 */
	public int nameToId(String id) {
		// if not resolvable, a new ID needs to be created!
		if (!ht2.containsKey(id)) {
			ht1.put(nextId, id);
			ht2.put(id, nextId);
			nextId++;
		}
		return ht2.get(id);
	}
	
	/**
	 * Determine the highest registered class ID
	 * @return class ID
	 */
	public int getMaximumId() {
		return nextId-1;
	}
	
	// sample management
	public ArrayList<Sample> samples = new ArrayList<Sample>();
	public HashMap<Integer, ArrayList<Sample>> samplesByClass = new HashMap<Integer, ArrayList<Sample>>();
	
	/**
	 * Load a data set from an ASCII file. The expected format is one sample per line, features
	 * separated by (arbitrary) whitespace. The position of the label starts at 0.
	 * @param fileName path to data file, use "-" to read from STDIN
	 * @param labelPos column of the label string, starting with 0 (set -1 for no label)
	 * @throws DataSetException
	 */
	public void fromAsciiFile(String fileName, int labelPos) throws DataSetException {
		// try to figure out the name of the data set by analyzing the filename
		if (fileName.equals("-"))
			name = "STDIN";
		else {
			String [] path = fileName.split("[\\/\\\\]");
			name = path[path.length-1];
		}
		
		try {
			samples = new ArrayList<Sample>();
			
			BufferedReader br;
			if (fileName.equals("-"))
				br = new BufferedReader(new InputStreamReader(System.in));
			else
				br = new BufferedReader(new FileReader(new File(fileName)));
		
			String buf;
			while ((buf = br.readLine()) != null) {
				String [] tok = buf.split("\\s+");
				
				// valid line?
				if (tok.length < 1)
					break;
				
				// read label first
				int c = labelPos >= 0 ? nameToId(tok[labelPos]) : -1;
				
				// read remaining fields as numerics
				double [] x = new double [labelPos >= 0 ? tok.length-1 : tok.length];
				for (int i = 0, j = 0; i < x.length; ++i, ++j) {
					if (j == labelPos) {
						i--;
						continue;
					}
					x[i] = Double.parseDouble(tok[j]);
				}
				addSample(new Sample(c, x));
			}
		} catch (IOException e) {
			throw new DataSetException("Error reading data file: " + e);
		}
	}
	
	public void addSample(Sample s) {
		samples.add(s);
		if (!samplesByClass.containsKey(s.c))
			samplesByClass.put(s.c, new ArrayList<Sample>());
		samplesByClass.get(s.c).add(s);
	}
	
	/**
	 * Randomly add Gaussian distributed samples to the data set
	 * according to the mean and covariance given by the density
	 * @param d Location to generate samples
	 * @param c class ID for the generated samples
	 * @param n number of samples to generate
	 */
	public void addRandomSamples(DensityDiagonal d, int c, int n) {
		Random [] generators = new Random [d.fd];
		
		for (int i = 0; i < d.fd; ++i)
			generators[i] = new Random();
		
		for (int i = 0; i < n; ++i) {
			Sample s = new Sample(c, d.fd);
			for (int j = 0; j < d.fd; ++j)
				s.x[j] = (generators[j].nextGaussian() + d.mue[j]) * d.cov[j];
			samples.add(s);
		}
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		try {
			for (Sample s : samples) {
				sb.append(idToName(s.c));
				for (int i = 0; i < s.x.length; ++i)
					sb.append(" " + s.x[i]);
				sb.append("\n");
			}
			
		} catch (Exception e) {
			System.err.println("some exception: " + e.toString());
		}
		
		return sb.toString();
	}
}
