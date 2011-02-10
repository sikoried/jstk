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
import java.util.ArrayList;

import de.fau.cs.jstk.sampled.AudioFileReader;
import de.fau.cs.jstk.sampled.RawAudioFormat;

public class Duration {

	public static final String SYNOPSIS = 
		"sikoried, 6/17/2009\n\n" +
		"Determines the duration of each file (or files within a list) and\n" +
		"computes an overall duration. Reads all the files to skip incomplete frames.\n\n" + 
		"usage: app.Duration <format-string> <file1> <-l listfile1> [file2 ...]\n" +
		"  <format-string> is\n" +
		"    \"f:path-to-file-with-header\": load audio format from file\n" +
		"    \"t:template-name\": use an existing template (ssg/[8,16], alaw/[8,16], ulaw/[8,16]\n" +
		"    \"r:bit-rate,sample-rate,signed(0,1),little-endian(0,1)\": specify raw format (no-header)\n" +
		"    default: \"" + Mfcc.DEFAULT_AUDIO_FORMAT + "\"\n";
	
	public static void main(String[] args) throws Exception {
		// check arguments
		if (args.length < 2) {
			System.out.println(SYNOPSIS);
			System.exit(1);
		}
		
		RawAudioFormat format = RawAudioFormat.create(args[0]);
		System.err.println(format);
		
		long gFrames = 0;
		int frameRate = 0;
		
		// read in the arguments, check if there's a list, and expand it to filenames
		ArrayList<String> filenames = new ArrayList<String>();
		for (int i = 1; i < args.length; ++i) {
			if (args[i].equals("-l")) {
				BufferedReader br = new BufferedReader(new FileReader(args[++i]));
				String buf;
				while ((buf = br.readLine()) != null)
					filenames.add(buf);
			} else if (args[i].startsWith("-")) {
				System.err.println("unknown parameter \"" + args[i] + "\"");
				System.exit(1);
			} else
				filenames.add(args[i]);
		}
		
		// now do all files
		for (String file : filenames) {
			try {
				AudioFileReader afr = new AudioFileReader(file, format, true);
				int nums = 0;
				long lFrames = 0;
				frameRate = afr.getSampleRate();
				double [] buf = new double [1024];
				
				// read all samples and count
				while ((nums = afr.read(buf)) > 0) { 
					lFrames += nums;
					gFrames += nums;
				}

				// print stats
				System.out.println(framesToTime(lFrames, frameRate) + " " + file);
			} catch (IOException e) {
				System.err.println(file + ": " + e.toString());
				throw e;
			} catch (Exception e) {
				System.err.println(file + ": " + e.toString());
			}
		}
		
		System.out.println(framesToTime(gFrames, frameRate) + " TOTAL PLAYING TIME");
	}
	
	/**
	 * Return a hh:mm:ss time stamp for a number of frames and given sampling rate
	 * @param frames number of frames
	 * @param sampleRate sampling rate
	 * @return time stamp
	 */
	public static String framesToTime(long frames, int sampleRate) {
		long seconds = frames / sampleRate % 60;
		long minutes = (frames / sampleRate / 60) % 60;
		long hours = (frames / sampleRate / 60 / 60);
		
		return
			(hours < 10 ? "0" + hours : hours) + ":" +
			(minutes < 10 ? "0" + minutes : minutes) + ":" +
			(seconds < 10 ? "0" + seconds : seconds);
	}
}
