/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer

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
package de.fau.cs.jstk.sampled;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The SimpleScrambler applies a frequency warping (multiplication in the 
 * time domain and linear shift in the frequency domain respectively).
 * The scrambling frequency may be set on demand. Use the negative scrambling
 * frequency to de-scramble.
 * For speech it is advised to apply a bandpass filter of 300-3400 after (de-)
 * scrambling. 
 * 
 * @author sikoried
 *
 */
public class SimpleScrambler implements AudioSource {
	private AudioSource source;
	
	/** 2*pi*(freq/samplerate) */
	private double fmod;
	
	/**
	 * Initialize a new SimpleScrambler on the given AudioSource
	 * @param source
	 * @param freq
	 */
	public SimpleScrambler(AudioSource source, double freq) {
		this.source = source;
		this.fmod = 2. * Math.PI * freq / (double) source.getSampleRate();
	}
	
	public int read(double [] buf) throws IOException {
		return read(buf, buf.length);
	}
	
	public int read(double [] buf, int length) throws IOException {
		int r = source.read(buf, length);
		
		if (r < 1)
			return r;
		
		/** 
		 * The actual (de)scrambling is a complex multiplication of the signal
		 * with the target frequency as 
		 *   buf[k] = buf[k] * exp(2*pi*i*(freq/rate)*k)
		 * but can be simplified to the real part as 
		 *  buf[i] = buf[i] * cos(2*pi*(freq/rate)*k)
		 */
		for (int i = 0; i < r; ++i)
			buf[i] *= Math.cos(fmod * i);
		
		return r;
	}
	
	public int getSampleRate() {
		return source.getSampleRate();
	}
	
	public boolean getPreEmphasis() {
		return source.getPreEmphasis();
	}
	
	public void setPreEmphasis(boolean applyPreEmphasis, double a) {
		source.setPreEmphasis(applyPreEmphasis, a);
	}
	
	public void tearDown() throws IOException {
		source.tearDown();
	}
	
	public static final String SYNOPSIS = 
		"usage: sampled.SimpleScrambler scrambling-freq [ band-start,band-end ] < in-ssg16 > out-ssg16";
	
	public static void main(String [] args) throws Exception {
		if (args.length < 1 || args.length > 2) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		// file
		RawAudioFormat raf = RawAudioFormat.getRawAudioFormat("ssg/16");
		AudioFileReader afr = new AudioFileReader(System.in, raf, false);
		
		// scrambler
		SimpleScrambler sc = new SimpleScrambler(afr, Double.parseDouble(args[0]));
		
		AudioSource out = sc;
		
		if (args.length == 2) {
			String [] spl = args[1].split(",");
			if (spl.length != 2) {
				System.err.println("wronf band pass filter format; needs to be start,end");
				System.exit(1);
			}
			
			double startf = Double.parseDouble(spl[0]);
			double endf = Double.parseDouble(spl[1]);
			out = new BandPassFilter(sc, startf, endf, 128);
		}
		
		double scale = Math.pow(2, raf.getBitRate() - 1) - 1;
		double [] buf = new double [512];
		byte [] bbuf = new byte [1024];
		int r = 0;
		while ((r = out.read(buf)) > 0) {
			ByteBuffer bb = ByteBuffer.wrap(bbuf);
			bb.order(ByteOrder.LITTLE_ENDIAN);
	 
			int i;
			for (i = 0; i < r; i++)					 
				bb.putShort((short)(buf[i] * scale));
				
			System.out.write(bbuf, 0, r*2);
		}
	}
}
