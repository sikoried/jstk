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
import java.util.ArrayList;
import java.util.List;

import de.fau.cs.jstk.framed.FrameSource;
import de.fau.cs.jstk.framed.Selection;
import de.fau.cs.jstk.framed.SimulatedFrameSource;
import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.io.FrameOutputStream;


public final class Merge {
	public static final String SYNOPSIS =
		"sikoried, 8/19/2010\n" +
		"Merge files of same length using either all dimensions or only parts. This\n" +
		"is the vertical complement to bin.Concat which pastes files of same dimension\n" +
		"in a row.\n\n" +
		"usage: app.Merge out-file file1[:selection-string] [file2 ...] [options]\n" +
		"-c\n" +
		"  Cache data (useful for large nummer of files to merge to avoid the 'too many open files'\n" +
		"  error. NEEDS TO BE PLACED BEFORE INPUT FILES (only affects files after parameter)";

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}

		boolean cache = false;
		
		String outf = null;
		List<FrameSource> sources = new ArrayList<FrameSource>();
		
		// parse arguments
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-c"))
				cache = true;
			else {
				if (outf == null)
					outf = args[i];
				else {
					String [] tok = args[i].split(":");
					if (tok.length == 1)
						sources.add(cache ? 
								new SimulatedFrameSource(new FrameInputStream(new File(tok[0]))) 
								: new FrameInputStream(new File(tok[0])));
					else if (tok.length == 2)
						sources.add(cache ? 
								new SimulatedFrameSource(Selection.create(new FrameInputStream(new File(tok[0])), tok[1])) 
								: Selection.create(new FrameInputStream(new File(tok[0])), tok[1]));
					else
						throw new IOException("Merge.main(): malformed parameter string at argument " + i);
				}
			}
		}
		
		int outdim = 0;
		List<double []> sourceBuffers = new ArrayList<double []>();
		for (FrameSource fs : sources) {
			outdim += fs.getFrameSize();
			sourceBuffers.add(new double [fs.getFrameSize()]);
		}
		
		System.err.println("writing " + outf + " fs=" + outdim);
		FrameOutputStream fw = new FrameOutputStream(outdim, new File(outf));
		
		double [] buf = new double [outdim];
		boolean done = false;
		while (!done) {
			// read from all sources
			for (int i = 0; i < sources.size() && !done; ++i)
				if (!sources.get(i).read(sourceBuffers.get(i)))
					done = true;
			
			if (done)
				break;
			
			// compose new frame
			int i = 0;
			for (double [] f : sourceBuffers) {
				System.arraycopy(f, 0, buf, i, f.length);
				i += f.length;
			}
			
			// write out
			fw.write(buf);
		}
		
		fw.close();
	}
}
