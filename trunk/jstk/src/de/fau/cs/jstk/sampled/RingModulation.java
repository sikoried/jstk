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

import de.fau.cs.jstk.sampled.filters.BandPassFilter;

/**
 * The (ring) modulation multiplies an input signal (waveform) with a given 
 * carrier frequency. As a result, the input signal spectrum is "lifted" by the 
 * specified frequency and a mirror effect can be observed at the shifting 
 * frequency.<b/>
 * 
 * In combination with a low-pass filter up to the shifting frequency, a simple
 * (voice) scrambling can be achieved that can be reversed using the exact same 
 * modulation scheme.<b/>
 * 
 * The frequency is synthesized as a sinus wave.
 * 
 * @author sikoried
 *
 */
public class RingModulation implements AudioSource {
	private AudioSource source;
	
	/** 2*pi*(freq/samplerate) */
	private double fmod;
	
	private long ind = 0;
	
	/**
	 * Initialize a new SimpleScrambler on the given AudioSource
	 * @param source
	 * @param freq
	 */
	public RingModulation(AudioSource source, double freq) {
		this.source = source;
		setFrequency(freq);
	}
	
	public void setFrequency(double freq) {
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
		 * The (ring) modulation is a simple multiplication of the waveform with
		 * the carrier frequency.
		 */
		for (int i = 0; i < r; ++i) {
			ind++;
			buf[i] *= Math.cos(fmod * ind);
		}
		
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
		"sikoried, 6/8/2011\n" +
		"(De)Scramble an input ssg/16 signal and write it to stdout. This is a\n" +
		"ring modulation of the signal with subsequent low-pass at the given scrambling\n" +
		"frequency.\n\n" +
		"usage: sampled.RingModulation scrambling-freq < in-ssg8 > out-ssg8";
	
	public static void main(String [] args) throws Exception {
		if (args.length != 1) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		// file
		RawAudioFormat raf = RawAudioFormat.getRawAudioFormat("ssg/16");
		AudioFileReader afr = new AudioFileReader(System.in, raf, true);
		
		// scrambler
		double warp = Double.parseDouble(args[0]);
		RingModulation sc = new RingModulation(afr, Double.parseDouble(args[0]));
		
		// low-pass
		// double [] param = Butterworth.ord(new double [] { 300./4000., cutoff/4000. } , new double [] { 250./4000., (cutoff+200.)/4000. }, 3., 60.);
		// Butterworth bpf = new Butterworth(sc, 3, 300., warp, true);
		BandPassFilter bpf = new BandPassFilter(sc, 0., warp, 1024);
		
		double scale = Math.pow(2, raf.getBitRate() - 1) - 1;
		double [] buf = new double [512];
		byte [] bbuf = new byte [1024];
		int r = 0;
		while ((r = bpf.read(buf)) > 0) {
			ByteBuffer bb = ByteBuffer.wrap(bbuf);
			bb.order(ByteOrder.LITTLE_ENDIAN);
	 
			int i;
			for (i = 0; i < r; i++)					 
				bb.putShort((short)(buf[i] * scale));
				
			System.out.write(bbuf, 0, r*2);
		}
	}
}
