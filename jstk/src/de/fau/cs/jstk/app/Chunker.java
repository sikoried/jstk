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
package de.fau.cs.jstk.app;


import java.io.File;
import java.io.IOException;

import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.io.FrameOutputStream;


public class Chunker {

	public static final String SYNOPSIS =
		"sikoried, 11/9/2010\n" +
		"Split a feature file in parts as defined by the given list. You may specify\n" +
		"an output directory. The list contains lines of \"out-file num-frames\".\n\n" +
		"usage: app.Chunker infile startframe endframe > frames.ft";
	
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		String file = args[0];
		int start = Integer.parseInt(args[1]);
		int end = Integer.parseInt(args[2]);
		if (end <= 0) {
			end = Integer.MAX_VALUE;
		}
		
		FrameInputStream fr = new FrameInputStream(new File(file));
		float [] buf = new float [fr.getFrameSize()];
		
		int i = 0;
		FrameOutputStream fw = new FrameOutputStream(fr.getFrameSize(), null);
		
		while (i < end && fr.read(buf)) {
			if (i < start) {
				i++;
			} else if (i >= start && i < end) {
				fw.write(buf);
				i++;
			} 
		}
		
		fw.close();
	}

}
