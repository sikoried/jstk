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

import org.apache.log4j.Logger;

import de.fau.cs.jstk.sampled.filters.BandPassFilter;

public class SplitbandInversion implements AudioSource {
	public static Logger logger = Logger.getLogger(SplitbandInversion.class);
	
	public static int FFT_SIZE = 2048;
	
	private AudioSource s1, s2;

	private RingModulation rm1, rm2;
	private BandPassFilter lp1, hp1, lpout1, lpout2;
	
	public SplitbandInversion(AudioSource s1, AudioSource s2)  {
		this.s1 = s1;
		this.s2 = s2;
	}
	
	public void configure(double splitf, double invf1, double invf2) {
		if (lp1 == null) {
			lp1 = new BandPassFilter(s1, 0., splitf, FFT_SIZE);
			rm1 = new RingModulation(lp1, invf1);
			lpout1 = new BandPassFilter(rm1, 0., invf1, FFT_SIZE);
		} else {
			lp1.setFilterBands(new double [] { 0., splitf });
			rm1.setFrequency(invf1);
			lpout1.setFilterBands(new double [] { 0., invf1 });
		}
		
		if (hp1 == null) {
			hp1 = new BandPassFilter(s2, splitf, s2.getSampleRate(), FFT_SIZE);
			rm2 = new RingModulation(hp1, invf2);
			lpout2 = new BandPassFilter(rm2, 0., invf2, FFT_SIZE);
		} else {
			lp1.setFilterBands(new double [] { splitf, (double) s2.getSampleRate() });
			rm1.setFrequency(invf2);
			lpout2.setFilterBands(new double [] { 0., invf2 });
		}
	}
	
	public int read(double [] buf) throws IOException {
		return read(buf, buf.length);
	}

	private double [] buf = new double [0];
	
	public int read(double [] buf, int length) throws IOException {
		if (this.buf.length != buf.length)
			this.buf = new double [buf.length];
		
		// read from both streams
		int r1 = lpout1.read(this.buf, length);
		int r2 = lpout2.read(buf, length);
		
		if (r1 != r2)
			logger.warn("low and high band have different sample count!");
		
		// combine
		for (int i = 0; i < length; ++i)
			buf[i] += this.buf[i];
		
		return r1;
	}

	public int getSampleRate() {
		return s1.getSampleRate();
	}

	public boolean getPreEmphasis() {
		return s1.getPreEmphasis();
	}

	public void setPreEmphasis(boolean applyPreEmphasis, double a) {
		s1.setPreEmphasis(applyPreEmphasis, a);
		s2.setPreEmphasis(applyPreEmphasis, a);
	}

	public void tearDown() throws IOException {
		s1.tearDown();
		s2.tearDown();
	}
	
	public static void main(String [] args) throws Exception {
		if (args.length != 4) {
			System.err.println("usage: sampled.SplitbandInversion split-f inv-f1 inv-f2 file");
			System.exit(1);
		}
		
		double fsplit = Double.parseDouble(args[0]);
		double fi1 = Double.parseDouble(args[1]);
		double fi2 = Double.parseDouble(args[2]);
		
		// file
		RawAudioFormat raf = RawAudioFormat.getRawAudioFormat("ssg/16");
		AudioFileReader afr1 = new AudioFileReader(args[3], raf, true);
		AudioFileReader afr2 = new AudioFileReader(args[3], raf, true);
		
		// scrambler
		SplitbandInversion sbi = new SplitbandInversion(afr1, afr2);
		sbi.configure(fsplit, fi1, fi2);
		
		double scale = Math.pow(2, raf.getBitRate() - 1) - 1;
		double [] buf = new double [512];
		byte [] bbuf = new byte [1024];
		int r = 0;
		while ((r = sbi.read(buf)) > 0) {
			ByteBuffer bb = ByteBuffer.wrap(bbuf);
			bb.order(ByteOrder.LITTLE_ENDIAN);
	 
			int i;
			for (i = 0; i < r; i++)					 
				bb.putShort((short)(buf[i] * scale));
				
			System.out.write(bbuf, 0, r*2);
		}
	}
}
