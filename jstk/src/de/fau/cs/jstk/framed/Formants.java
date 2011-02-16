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
package de.fau.cs.jstk.framed;

import java.io.IOException;

import de.fau.cs.jstk.io.FrameSource;
import de.fau.cs.jstk.sampled.AudioFileReader;
import de.fau.cs.jstk.sampled.AudioSource;

/**
 * Extract the first n Formants from the given LPC spectrum, 
 * in Hz.
 * 
 * @author sikoried
 */
public class Formants implements FrameSource {

	/** LPC object to read from */
	private LPCSpectrum lpc;
	
	/** number of LPC coefficients */
	private int fs;
	
	/** internal read buffer */
	private double [] buf;
	
	/** number of formants to extract */
	private int n;

	/** 1 / sample rate */
	private double toFreq;
	
	/**
	 * Construct a new Formant extractor, extracting the first 3
	 * formants from the given FrameSource
	 * 
	 * @param source
	 * @param sampleRate sample rate of the underlying signal
	 */
	public Formants(LPCSpectrum lpc, int sampleRate) {
		this(lpc, sampleRate, 3);
	}
	
	/**
	 * Construct a new Formant extractor, extracting the first n
	 * formants from the given FrameSource
	 * 
	 * @param source
	 * @param sampleRate sample rate of the underlying signal
	 * @param n number of Formants to extract
	 */
	public Formants(LPCSpectrum lpc, int sampleRate, int n) {
		this.lpc = lpc;
		this.fs = lpc.getFrameSize();
		this.n = n;
		this.toFreq = .5 * sampleRate / fs;
		this.buf = new double [fs];
	}

	public FrameSource getSource() {
		return lpc;
	}
	
	public int getFrameSize() {
		return n;
	}
	
	/**
	 * Read the next LPC frame and extract the n maxima
	 */
	public boolean read(double [] buf) throws IOException {
		if (!lpc.read(this.buf))
			return false;
		
		int j = 0;
		for (int i = 1; i < fs - 1 && j < n; ++i) {
			if (this.buf[i-1] <= this.buf[i] && this.buf[i+1] <= this.buf[i])
				buf[j++] = i * toFreq;
		}
		
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("usage: framed.Formants file num-formants");
			System.exit(1);
		}
		
		AudioSource as = new AudioFileReader(args[0], true);
		Formants fs =  new Formants(new LPCSpectrum(new AutoCorrelation(new HammingWindow(as, 25, 10))), as.getSampleRate(), Integer.parseInt(args[1]));
		
		double [] buf = new double [fs.getFrameSize()];
		while (fs.read(buf)) {
			for (double d : buf)
				System.out.print(d + " ");
			System.out.println();
		}
	}

}
