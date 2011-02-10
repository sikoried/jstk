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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import de.fau.cs.jstk.io.*;
import de.fau.cs.jstk.stat.Sample;

public class Convert {
	public static final short LABEL_SIZE = 12;
	
	public static final String SYNOPSIS = 
		"Translate between various file formats.\n\n" +
		"usage: app.Convert in_format out_format < data_in > data_out\n\n" +
		"formats:\n" +
		"  ufv,dim\n" +
		"    Unlabeled feature data, 4 byte (float) per sample dimension\n" +
		"  lfv,dim,label1,label2,...,labeln\n" +
		"    Labeled feature data; 12 byte label, then 4 byte (float) per sample.\n" +
		"    Label ID will be attached according to the sequence of labels in the\n" +
		"    argument: label1 -> 0, label2 -> 1, etc. as the Sample class requires\n" +
		"    numeric labels.\n" +
		"  frame, frame_double\n" +
		"    Unlabeled feature data, 4/8 byte (float/double) per sample dimension\n" +
		"  sample_a, sample_b\n" +
		"    Labeled feature data using the statistics.Sample class, either (a)scii or\n" +
		"    (b)inary.\n" +
		"  csample_a, csample_b\n" +
		"    Labeled and classified feature data using the statistics.Sample class,\n" +
		"    either (a)scii or (b)inary.\n" +
		"  ascii\n" +
		"    Unlabeled ASCII data: TAB separated double values, one sample per line.\n" +
		"  ascii_label\n" +
		"    Labelled ASCII data: TAB separated values, first field is label.\n";
	
	public static enum Format {
		UFV,
		LFV,
		FRAME,
		FRAME_DOUBLE,
		SAMPLE_A,
		SAMPLE_B,
		CSAMPLE_A,
		CSAMPLE_B,
		ASCII,
		ASCII_L
	}
	
	public static int fd = 0;
	
	public static HashMap<String, Integer> lookup1 = new HashMap<String, Integer>();
	public static HashMap<Integer, String> lookup2 = new HashMap<Integer, String>();
	
	private static int lab = 1;
	
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
			
			for (int i = 2; i < list.length; ++i) {
				lookup1.put(list[i], lab);
				lookup2.put(lab, list[i]);
				lab++;
			}
			return Format.LFV;
		} else if (arg.equals("frame"))
			return Format.FRAME;
		else if (arg.equals("frame_double"))
			return Format.FRAME_DOUBLE;
		else if (arg.equals("sample_a"))
			return Format.SAMPLE_A;
		else if (arg.equals("sample_b"))
			return Format.SAMPLE_B;
		else if (arg.equals("csample_a"))
			return Format.CSAMPLE_A;
		else if (arg.equals("csample_b"))
			return Format.CSAMPLE_B;
		else if (arg.equals("ascii"))
			return Format.ASCII;
		else if (arg.equals("ascii_label"))
			return Format.ASCII_L;
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
		BufferedReader br = null;
		FrameReader fr = null;
		SampleReader sr = null;
		
		// possible writers
		FrameWriter fw = null;
		SampleWriter sw = null;
		
		switch (inFormat) {
		case SAMPLE_A: sr = new SampleReader(System.in, true, false); break;
		case SAMPLE_B: sr = new SampleReader(System.in, false, false); break;
		case CSAMPLE_A: sr = new SampleReader(System.in, true, true); break;
		case CSAMPLE_B: sr = new SampleReader(System.in, false, true); break;
		case FRAME: fr = new FrameReader(); fd = fr.getFrameSize(); break;
		case FRAME_DOUBLE: fr = new FrameReader(null, false); fd = fr.getFrameSize(); break;
		case ASCII:
		case ASCII_L:
			br = new BufferedReader(new InputStreamReader(System.in));
		}
		
		double [] buf = new double [fd];
		
		// read until done...
		while (true) {
			Sample s = null;
			byte [] label = null;
			
			// try to read...
			switch (inFormat) {
			case FRAME:
			case FRAME_DOUBLE:
				if (fr.read(buf))
					s = new Sample(0, buf);
				break;
			case SAMPLE_A:
			case SAMPLE_B:
			case CSAMPLE_A:
			case CSAMPLE_B:
				s = sr.read();
				break;
			case LFV:
				label = new byte [LABEL_SIZE];
				if (!IOUtil.readByte(System.in, label))
					break;
			case UFV:
				if (!IOUtil.readFloat(System.in, buf, ByteOrder.LITTLE_ENDIAN)) 
					break;
				
				int length = 0;
				while (label != null && length < label.length && label[length] != 0)
					length++;
				String ls = (label == null ? null : new String(label, 0, length, "ASCII"));
				s = new Sample(label == null ? 0 : lookup1.get(ls), buf);

				break;
			case ASCII:
			case ASCII_L:
				String line = br.readLine();
				if (line == null)
					break;
				String [] cols = line.trim().split("\\s+");
				int i1 = 0;
				ls = null;
				
				if (inFormat == Format.ASCII_L)
					ls = cols[i1++];
				
				if (!lookup1.containsKey(ls)) {
					lookup1.put(ls, lab);
					lookup2.put(lab, ls);
					lab++;
				}
				
				int i2 = 0;
				buf = new double [cols.length - i1];
				for (; i1 < cols.length; ++i1)
					buf[i2++] = Double.parseDouble(cols[i1]);
				
				s = new Sample(ls == null ? 0 : lookup1.get(ls), buf);
			}
			
			// anything read?
			if (s == null)
				break;
			
			// write out...
			switch (outFormat) {
			case SAMPLE_A:
				if (sw == null)
					sw = new SampleWriter(System.out, true, s.x.length, false);
				sw.write(s);
				break;
			case SAMPLE_B:
				if (sw == null)
					sw = new SampleWriter(System.out, false, s.x.length, false);
				sw.write(s);
				break;
			case CSAMPLE_A:
				if (sw == null)
					sw = new SampleWriter(System.out, true, s.x.length, true);
				sw.write(s);
				break;
			case CSAMPLE_B:
				if (sw == null)
					sw = new SampleWriter(System.out, false, s.x.length, true);
				sw.write(s);
				break;
			case FRAME:
				if (fw == null)
					fw = new FrameWriter(s.x.length);
				fw.write(s.x);
				break;
			case FRAME_DOUBLE:
				if (fw == null)
					fw = new FrameWriter(s.x.length, false);
				fw.write(s.x);
				break;
			case LFV:
				byte [] outlabel1 = new byte [LABEL_SIZE];
				byte [] outlabel2 = (s.c == 0 ? "UNSET".getBytes("ASCII") : lookup2.get(s.c).getBytes("ASCII"));
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
			case ASCII_L:
				String lab = lookup2.get(s.c);
				System.out.print((lab == null ? "UNSET" : lab) + "\t");
			case ASCII:
				for (int i = 0; i < s.x.length; ++i) {
					System.out.print(s.x[i]);
					if (i < s.x.length - 1)
						System.out.print("\t");
					else
						System.out.println();
				}
				break;
			}
		}
		
		// be nice, close everything
		if (fw != null)
			fw.close();
	
		System.out.flush();
	}
}
