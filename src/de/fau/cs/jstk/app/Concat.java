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

import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.io.FrameOutputStream;

public class Concat {
	public static final String SYNOPSIS = 
		"sikoried, 2/16/2010\n" +
		"Use this program to concatenate feature (frame) files to one single file.\n" +
		"The output is, unless otherwise specified, stdout. Prefer lists  (-l)\n" +
		"before many arguments. The list parameter can be used multiple times.\n" +
		"usage: app.Concat [-o out-file] [-l list-file] [file1 ...]\n";
	
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		String outfile = null;
		LinkedList<String> files = new LinkedList<String>();
		
		// parse arguments
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-o"))
				outfile = args[++i];
			else if (args[i].equals("-l")) {
				BufferedReader lr = new BufferedReader(new FileReader(args[++i]));
				String line = null;
				while ((line = lr.readLine()) != null)
					files.add(line);
			} else
				files.add(args[i]);
		}
		
		if (files.size() == 0) {
			System.err.println("Concat.main(): No input files specified, exitting.");
			System.exit(0);
		}
		
		// read in all the frames, write them to output file.
		FrameInputStream reader = new FrameInputStream(new File(files.remove()));
		FrameOutputStream writer = new FrameOutputStream(reader.getFrameSize(), new File(outfile));
		
		double [] buf = new double [reader.getFrameSize()];
		
		// read first file
		while (reader.read(buf))
			writer.write(buf);
		reader.close();
		
		while (files.size() > 0) {
			String f = files.remove();
			reader = new FrameInputStream(new File(f));
			
			// ensure correct frame size
			if (reader.getFrameSize() != buf.length)
				throw new IOException("Concat.main(): Wrong input frame size of file " + f + " (is: " + reader.getFrameSize() + " req: " + buf.length + ")");
			
			// transfer data
			while (reader.read(buf))
				writer.write(buf);
		
			reader.close();
		}
		
		writer.close();
	}
}
