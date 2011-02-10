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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteOrder;
import java.util.List;

import de.fau.cs.jstk.stat.Sample;

/**
 * Write out Samples to a given OutputStream, either in ASCII or binary format.
 * 
 * @author sikoried
 */
public class SampleWriter {
	/** OutputStream to write to */
	private OutputStream os;
	
	/** if not null: write ASCII to BufferedWriter */
	private BufferedWriter bw = null;
	
	/** indicates if the classification results needs to be written as well */
	private boolean classif;
	
	/**
	 * Allocate a new SampleWriter to write to the given OutputStream either
	 * binary or ASCII data. You may also request to save the classification 
	 * result.
	 * @param os
	 * @param ascii write ASCII?
	 * @param fd feature dimension
	 * @param classif indicate to write classification result
	 * @throws IOException
	 */
	public SampleWriter(OutputStream os, boolean ascii, int fd, boolean classif) throws IOException {
		this.os = os;
		this.classif = classif;
		if (ascii) 
			bw = new BufferedWriter(new OutputStreamWriter(os));
		else
			IOUtil.writeInt(os, fd, ByteOrder.LITTLE_ENDIAN);
	}
	
	/**
	 * Write the given Sample to the stream.
	 * @param s
	 * @throws IOException
	 */
	public void write(Sample s) throws IOException {
		if (bw != null)
			writeToAscii(bw, s, classif);
		else
			writeToBinary(os, s, classif);
	}
	
	/**
	 * Flush and close the stream.
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (bw != null)
			bw.flush();

		os.flush();
		os.close();
	}
	
	/**
	 * In the end, close the data file to prevent data loss!
	 */
	public void finalize() throws Throwable{
		try {
			if (bw != null)
				bw.flush();
			os.flush();
			os.close();
		} finally {
			super.finalize();
		}
	}
	
	/**
	 * Write a list of Samples to the given OutputStream in ASCII format. Note
	 * that the original class info is saved, but not the assigned (y).
	 * @param os
	 * @param list List of Samples to write
	 * @param classif indicate if to write the classification result
	 * @throws IOException
	 */
	public static void writeToAscii(BufferedWriter bw, List<Sample> list, boolean classif) throws IOException {
		for (Sample s : list) 
			writeToAscii(bw, s, classif);
	}
	
	/**
	 * Write a single Sample to the given ASCII output stream.
	 * @param bw initialized BufferedWriter
	 * @param s 
	 * @param classif indicate if to write the classification result
	 * @throws IOException
	 */
	public static void writeToAscii(BufferedWriter bw, Sample s, boolean classif) throws IOException {
		if (classif)
			bw.append(s.toClassifiedString() + "\n");
		else
			bw.append(s.toString() + "\n");
		
		bw.flush();
	}
	
	/**
	 * Write a list of Samples to the given OutputStream in binary format. Note
	 * that the assigned class (y) is not written. The resulting format is 
	 * [frame-size][sample1: class-id data...][...]
	 * @param os
	 * @param list
	 * @param classif indicate if to write the classification result
	 * @throws IOException
	 */
	public static void writeToBinary(OutputStream os, List<Sample> list, boolean classif) throws IOException {
		if (list.size() < 1)
			throw new IOException("empty list");
		
		// write frame size as header
		IOUtil.writeInt(os, list.get(0).x.length, ByteOrder.LITTLE_ENDIAN);
			
		// write all samples
		for (Sample s : list)
			writeToBinary(os, s, classif);
	}
	
	/**
	 * Write a single Sample to the output stream. Format is [class-id data...]
	 * @param os
	 * @param s
	 * @param classif indicate if to write the classification result
	 * @throws IOException
	 */
	public static void writeToBinary(OutputStream os, Sample s, boolean classif) throws IOException {
		IOUtil.writeInt(os, s.c, ByteOrder.LITTLE_ENDIAN);
		
		if (classif)
			IOUtil.writeInt(os, s.y, ByteOrder.LITTLE_ENDIAN);
		
		IOUtil.writeFloat(os, s.x, ByteOrder.LITTLE_ENDIAN);
	}
}
