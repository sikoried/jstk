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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.fau.cs.jstk.io.FrameDestination;
import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.io.FrameOutputStream;
import de.fau.cs.jstk.io.FrameReader;
import de.fau.cs.jstk.io.FrameSource;
import de.fau.cs.jstk.io.FrameWriter;
import de.fau.cs.jstk.io.IOUtil;
import de.fau.cs.jstk.io.SampleDestination;
import de.fau.cs.jstk.io.SampleInputStream;
import de.fau.cs.jstk.io.SampleOutputStream;
import de.fau.cs.jstk.io.SampleReader;
import de.fau.cs.jstk.io.SampleSource;
import de.fau.cs.jstk.io.SampleWriter;
import de.fau.cs.jstk.stat.Sample;

public class Convert {
	public static final short LABEL_SIZE = 12;
	
	public static final String SYNOPSIS = 
		"Translate between various file formats.\n\n" +
		"usage: app.Convert in_format out_format < data_in > data_out\n\n" +
		"formats:\n" +
		"  ufv,dim\n" +
		"    Unlabeled feature data, 4 byte (float) per sample dimension\n" +
		"  lfv,dim\n" +
		"    Labeled feature data; 12 byte label, then 4 byte (float) per sample.\n" +
		"    Labels must be numeric.\n" +
		"  frame, frame_double\n" +
		"    Unlabeled feature data, 4/8 byte (float/double) per sample dimension\n" +
		"  sample_a, sample_b\n" +
		"    Labeled feature data using the stat.Sample class, either (a)scii or\n" +
		"    (b)inary. Format is <short:label> <short:classif-result> <float: feature data>\n" +
		"  ascii\n" +
		"    Unlabeled ASCII data: TAB separated double values, one sample per line.\n";
	
	public static enum Format {
		UFV,
		LFV,
		FRAME,
		FRAME_DOUBLE,
		SAMPLE_A,
		SAMPLE_B,
		ASCII
	}
	
	public static int fd = 0;
	
	/**
	 * Analyze the format string
	 */
	public static Format determineFormat(String arg) {
		if (arg.startsWith("ufv,")) {
			fd = Integer.parseInt(arg.substring(4));
			return Format.UFV;
		} else if (arg.startsWith("lfv,")) {
			String [] list = arg.split(",");
			fd = Integer.parseInt(list[1]);
			return Format.LFV;
		} else if (arg.equals("frame"))
			return Format.FRAME;
		else if (arg.equals("frame_double"))
			return Format.FRAME_DOUBLE;
		else if (arg.equals("sample_a"))
			return Format.SAMPLE_A;
		else if (arg.equals("sample_b"))
			return Format.SAMPLE_B;
		else if (arg.equals("ascii"))
			return Format.ASCII;
		else
			throw new RuntimeException("invalid format \"" + arg + "\"");
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		// get the formats
		Format inFormat = determineFormat(args[0]);
		Format outFormat = determineFormat(args[1]);
		
		// possible readers
		FrameSource fsource = null;
		SampleSource ssource = null;
		
		
		// possible writers
		FrameDestination fdest = null;
		SampleDestination sdest = null;
		
		switch (inFormat) {
		case SAMPLE_A: ssource = new SampleReader(new InputStreamReader(System.in)); break;
		case SAMPLE_B: ssource = new SampleInputStream(System.in); break;
		case FRAME: fsource = new FrameInputStream(null); fd = fsource.getFrameSize(); break;
		case FRAME_DOUBLE: fsource = new FrameInputStream(null, false); fd = fsource.getFrameSize(); break;
		case ASCII: fsource = new FrameReader(new InputStreamReader(System.in)); fd = fsource.getFrameSize(); break;
		}
		
		double [] buf = new double [fd];
		
		// read until done...
		while (true) {
			Sample s = null;
			short label = 0;
			
			// try to read...
			switch (inFormat) {
			case FRAME:
			case FRAME_DOUBLE:
			case ASCII:
				if (fsource.read(buf))
					s = new Sample((short) 0, buf);
				break;
			case SAMPLE_A:
			case SAMPLE_B:
				s = ssource.read();
				break;
			case LFV:
				byte [] bl = new byte [LABEL_SIZE];
				if (!IOUtil.readByte(System.in, bl))
					break;
				String textual = new String(bl);
				try {
					label = Short.parseShort(textual);
				} catch (NumberFormatException e) {
					throw new IOException("Invalid label '" + textual + "' -- only numeric labels allowed!");
				}
			case UFV:
				if (!IOUtil.readFloat(System.in, buf, ByteOrder.LITTLE_ENDIAN)) 
					break;
				
				s = new Sample(label, buf);
				break;
			}
			
			// anything read?
			if (s == null)
				break;
						
			// write out...
			switch (outFormat) {
			case SAMPLE_A:
				if (sdest == null)
					sdest = new SampleWriter(new OutputStreamWriter(System.out));
				sdest.write(s);
				break;
			case SAMPLE_B:
				if (sdest == null)
					sdest = new SampleOutputStream(System.out, s.x.length);
				sdest.write(s);
				break;
			case FRAME:
				if (fdest == null)
					fdest = new FrameOutputStream(s.x.length);
				fdest.write(s.x);
				break;
			case FRAME_DOUBLE:
				if (fdest == null)
					fdest = new FrameOutputStream(s.x.length, false);
				fdest.write(s.x);
				break;
			case LFV:
				byte [] outlabel1 = new byte [LABEL_SIZE];
				byte [] outlabel2 = Integer.toString(s.c).getBytes();
				for (int i = 0; i < LABEL_SIZE; ++i) {
					if (i < outlabel2.length)
						outlabel1[i] = outlabel2[i];
					else
						outlabel1[i] = 0;
				}
				System.out.write(outlabel1);
			case UFV:
				ByteBuffer bb = ByteBuffer.allocate(buf.length * Float.SIZE/8);
				
				// UFVs are little endian!
				bb.order(ByteOrder.LITTLE_ENDIAN);
				
				for (double d : s.x) 
					bb.putFloat((float) d);
				
				System.out.write(bb.array());
				break;
			case ASCII:
				if (fdest == null)
					fdest = new FrameWriter(new OutputStreamWriter(System.out));
				fdest.write(s.x);
				break;
			}
		}
		
		// be nice, close everything
		if (fdest != null)
			fdest.close();
		if (sdest != null)
			sdest.close();
		
		System.out.flush();
	}
}
