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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.io.FrameOutputStream;
import de.fau.cs.jstk.util.Pair;


public class Split {

	public static final String SYNOPSIS =
		"sikoried, 11/9/2010\n" +
		"Split a feature file in parts as defined by the given list. You may specify\n" +
		"an output directory. The list contains lines of \"out-file num-frames\".\n\n" +
		"usage: app.Split infile outlist [out-dir]";
	
	public static void main(String[] args) throws IOException {
		if (!(args.length == 2 || args.length == 3)) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		String file = args[0];
		String list = args[1];
		String outd = (args.length == 3 ? args[2] : null);
		
		List<Pair<String, Integer>> segs = new LinkedList<Pair<String, Integer>>();
		BufferedReader br = new BufferedReader(new FileReader(list));
		String line;
		while ((line = br.readLine()) != null) {
			String [] sp = line.split("\\s+");
			segs.add(new Pair<String, Integer>(sp[0], Integer.parseInt(sp[1])));
		}
		
		if (segs.size() == 0)
			throw new IOException("Split.main(): no output files listed!");
		
		FrameInputStream fr = new FrameInputStream(new File(file));
		float [] buf = new float [fr.getFrameSize()];
		
		int i = 0;
		Pair<String, Integer> p = segs.remove(0);
		FrameOutputStream fw = new FrameOutputStream(fr.getFrameSize(), new File(outd == null ? p.a : outd + System.getProperty("file.separator") + p.a));
		
		while (fr.read(buf)) {
			fw.write(buf);
			
			// done with segment?
			if (++i == p.b) {
				if (segs.size() == 0) {
					fw.close();
					fw = null;
					break;
				}
				
				p = segs.remove(0);
				fw.close();
				fw = new FrameOutputStream(fr.getFrameSize(), new File(outd == null ? p.a : outd + System.getProperty("file.separator") + p.a));
				i = 0;
			}
		}
		
		if (fw != null) {
			System.err.println("Split.main(): WARNING -- input file ended before requested segment!");
			fw.close();
		}
	}

}
