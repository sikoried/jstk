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
package de.fau.cs.jstk.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;

import de.fau.cs.jstk.stat.Sample;

/**
 * The SampleReader reads instances of Samples from the given InputStream (ASCII
 * or binary). You need to specify, if the InputStream provides Sample instances 
 * with or without saved classification result.
 * 
 * @author sikoried
 */
public class SampleReader {
	/** InputStream to read from */
	private InputStream is;
	
	/** not null if to read ASCII input */
	private BufferedReader br = null;
	
	/** indicates whether the input is expected to have classification results */
	private boolean classif;
	
	/** input feature dimension */
	private int fd;
	
	/**
	 * Allocate a new SampleReader to read from the given stream. Indicate if
	 * if this is an ASCII stream (System.in)
	 * @param is
	 * @param ascii
	 * @param classif is the classification result present?
	 * @throws IOException
	 */
	public SampleReader(InputStream is, boolean ascii, boolean classif) throws IOException {
		this.is = is;
		this.classif = classif;
		
		if (ascii)
			br = new BufferedReader(new InputStreamReader(is));
		else
			fd = IOUtil.readInt(is, ByteOrder.LITTLE_ENDIAN);
	}
	
	/**
	 * Read the next Sample from the InputStream.
	 * @return null if no more Sample in the stream
	 * @throws IOException
	 */
	public Sample read() throws IOException {
		if (br != null)
			return readFromAscii(br, classif);
		else
			return readFromBinary(is, fd, classif);
	}
	
	/**
	 * Read in a list of Samples from the given InputStream in ASCII format, 
	 * i.e. line a la "label-no feat1 feat2 ..."
	 * @param is
	 * @param classif indicate if the input stream is expected to contain classification results
	 * @return
	 * @throws IOException
	 */
	public static List<Sample> readFromAscii(InputStream is, boolean classif) throws IOException{
		List<Sample> list = new LinkedList<Sample>();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		Sample s;
		while ((s = readFromAscii(br, classif)) != null) 
			list.add(s);
			
		return list;
	}
	
	/**
	 * Read a single Sample from the prepared BufferedReader
	 * @param br
	 * @param classif indicate if the input stream is expected to contain classification results
	 * @return
	 * @throws IOException
	 */
	public static Sample readFromAscii(BufferedReader br, boolean classif) throws IOException {
		String line = br.readLine();
		if (line == null)
			return null;
		
		String [] split = line.trim().split("\\s+");
				
		int lab = Integer.parseInt(split[0]);
		int y = -1;
		double [] feat = null;
		
		int disp = 1;
		
		// see if we need to read a classification result
		if (classif) {
			y = Integer.parseInt(split[disp]);
			disp = 2;
		}
			
		feat = new double [split.length - disp];
		for (int i = 0; i < feat.length; ++i)
			feat[i] = Double.parseDouble(split[i+disp]);
		
		return new Sample(lab, y, feat);
	}
	
	/**
	 * Read in a list of Samples from the given InputStream in binary format.
	 * The expected format is [frame-size][sample1: class-id data...][...]
	 * @param is
	 * @param classif indicate if the input stream is expected to contain classification results
	 * @return
	 * @throws IOException
	 */
	public static List<Sample> readFromBinary(InputStream is, boolean classif) throws IOException {
		List<Sample> list = new LinkedList<Sample>();

		// read the frame size
		int fd = IOUtil.readInt(is, ByteOrder.LITTLE_ENDIAN);
		
		// now read the samples
		Sample s;
		while ((s = readFromBinary(is, fd, classif)) != null)
			list.add(s);

		return list;
	}
	
	/**
	 * Read a single Sample with the given feature dimension from the stream.
	 * The expected format is [class-id data...]
	 * @param is
	 * @param fd input feature dimension
	 * @param classif indicate if the stream is expected to contain classification results
	 * @return
	 * @throws IOException
	 */
	public static Sample readFromBinary(InputStream is, int fd, boolean classif) throws IOException {
		// read label
		int c = IOUtil.readInt(is, ByteOrder.LITTLE_ENDIAN);
		int y = -1;
		
		if (classif)
			y = IOUtil.readInt(is, ByteOrder.LITTLE_ENDIAN);
		
		double [] x = new double [fd];
		if (!IOUtil.readFloat(is, x, ByteOrder.LITTLE_ENDIAN))
			return null;
		
		return new Sample(c, y, x);
	}
}
