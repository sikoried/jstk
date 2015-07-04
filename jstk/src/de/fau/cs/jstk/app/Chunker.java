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

import de.fau.cs.jstk.io.ChunkedDataSet;
import de.fau.cs.jstk.io.ChunkedDataSet.Chunk;
import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.io.FrameOutputStream;


public class Chunker {

	public static final String SYNOPSIS =
		"usage: app.Chunker segments [dirname]";
	
	public static void main(String[] args) throws IOException {
		if (!(args.length == 1 || args.length == 2)) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		String file = args[0];
		String dir = (args.length > 1 ? args[1] : null);
		
		ChunkedDataSet cds = new ChunkedDataSet(new File(file), dir, 0);
		
		Chunk c = null;
		while ((c = cds.nextChunk()) != null) {
			FrameInputStream fr = c.getFrameReader();
			float [] buf = new float [fr.getFrameSize()];
		
			File outf = new File((dir != null 
					? dir + System.getProperty("file.separator") + c.getName()
					: c.getName()));
			FrameOutputStream fw = new FrameOutputStream(fr.getFrameSize(), outf);
		
			while (fr.read(buf)) {
				fw.write(buf);
			} 
			
			fw.close();
		}
	}
}
