/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Stefan Steidl

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
package de.fau.cs.jstk.vc;

import java.io.*;

import de.fau.cs.jstk.io.FrameSource;

public class FrameFileWriter {

	private BufferedWriter writer;
	private FrameSource source;
	
	public FrameFileWriter(FrameSource source, String filename) throws IOException {
		this.source = source;
		writer = new BufferedWriter(new FileWriter(filename));
	}
	
	public void write() throws IOException {
		if ((source != null) && (writer != null)) {
			int fs = source.getFrameSize();
			double[] frame = new double[fs];
			while (source.read(frame)) {
				StringBuffer str = new StringBuffer();
				str.append(frame[0]);
				for (int i = 1; i < frame.length; i++) {
					str.append(",");
					str.append(frame[i]);					
				}
				str.append('\n');
				writer.write(str.toString());
			}
		}		
	}
	
	public void close() throws IOException {
		writer.close();
	}
	
}
