/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		Tobias Bocklet
		Florian Hoenig
		Stefan Steidl

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
package de.fau.cs.jstk.sampled.filters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import de.fau.cs.jstk.io.IOUtil;
import de.fau.cs.jstk.sampled.AudioFileReader;
import de.fau.cs.jstk.sampled.AudioSource;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * The FIRFilter class convolves an audio signal with a finite impulse response 
 * (FIR) using the overlapp-and-add algorithm. As the resulting signal has
 * (typically) very low gain, it should be forwarded to an ACG.
 *  
 * @author sikoried
 */
public class FIRFilter implements AudioSource {
	
	public static final int MIN_FFT_COEFFICIENTS = 512;
	
	private int min_fft_coefficients;
	
	private AudioSource source;
	
	/** finite impulse response */
	private double [] fir;
	
	private double [] result;
	
	/** resulting overlap (one sample shorter than IR) */
	private double [] overlap;
	
	/** FFT buffer for IR */
	private double [] fft_fir;
	
	/** FFT buffer for signal */
	private double [] fft_sig;
	
	/** buffer for inverse FFT */
	private double [] ifft;
	
	/** FFT object to transform back and forth */
	private DoubleFFT_1D fft;
	
	private int l = 0;
	
	private int fft_size = 0;
	
	private int lastread = -1;
	
	private double scale = 1.;
	
	/**
	 * Generate a new FIR Filter for the given AudioSource and 
	 * @param source
	 * @param fir
	 */
	public FIRFilter(AudioSource source, double [] fir) {
		this(source, fir, MIN_FFT_COEFFICIENTS);
	}
	
	public FIRFilter(AudioSource source, double [] fir, int min_fft_coefficients) {
		this.source = source;
		this.fir = fir;
		this.min_fft_coefficients = min_fft_coefficients;
		this.overlap = new double [fir.length - 1];
	
		scale();
	}
	
	/**
	 * Compute and set the scale factor via the worst case signal (alternating 
	 * +1/-1). This results in a sum of the absolute fir values.
	 */
	private void scale() {
		scale = 0;
		for (int i = 0; i < fir.length; ++i)
			scale += Math.abs(fir[i]);
		scale = 1. / scale;
	}
	
	public int read(double [] buf) throws IOException {
		return read(buf, buf.length);
	}

	public int read(double [] buf, int length) throws IOException {
		// verify buffer size, re-init if necessary
		if (lastread != length) {
			lastread = length;
			
			l = fir.length + length - 1;
			result = new double [l];
			
			// pad to the next power of 2, min 512
			int min = min_fft_coefficients; // MINIMUM_FFT_COEFFICIENTS;
			
			while (min <= l)
				min = min << 1;
			
			fft_size = min;
			
			// allocate buffers
			fft_fir = new double [2*fft_size];
			fft_sig = new double [2*fft_size];
			ifft = new double [2*fft_size];
			
			// prepare filter FFT
			fft = new DoubleFFT_1D(fft_size);
			System.arraycopy(fir, 0, fft_fir, 0, fir.length); // zero padded
			fft.realForwardFull(fft_fir);
		}
		
		// read from the source
		int r = source.read(buf, length);
		
		// do the convolution if there is any data
		if (r > 0) {
			// copy data
			for (int i = 0; i < length; ++i)
				fft_sig[i] = buf[i];
			
			// zero padding
			for (int i = length; i < fft_sig.length; ++i)
				fft_sig[i] = 0.;
			
			// forward FFT
			fft.realForwardFull(fft_sig);
			
			// complex multiplication of signal and filter
			for(int i = 0; i < fft_size; i++){
				ifft[2*i] 	= fft_sig[2*i] * fft_fir[2*i]   - fft_sig[2*i+1] * fft_fir[2*i+1];
				ifft[2*i+1]	= fft_sig[2*i] * fft_fir[2*i+1] + fft_sig[2*i+1] * fft_fir[2*i];
			}
	
			// inverse FFT
			fft.complexInverse(ifft, true);
			
			// real part is convolution result
			for(int i = 1; i < l+1; i++)
				result[i-1] = ifft[2*i];
			
			// step 1: add previous overlap
			for (int i = 0; i < overlap.length; ++i)
				result[i] += overlap[i];
			
			// step 2: transfer to output buffer, clip if neccessary
			for (int i = 0; i < length; ++i)
				buf[i] = scale * result[i];
			
			// step 3: get the new overlap
			System.arraycopy(result, length, overlap, 0, overlap.length);
		} 
		
		return r;
	}

	public int getSampleRate() {
		return source.getSampleRate();
	}

	public double getPreEmphasis() {
		return source.getPreEmphasis();
	}

	public void setPreEmphasis(double a) {
		source.setPreEmphasis(a);
	}

	public boolean getNormalize() {
		return source.getNormalize();
	}
	
	public void setNormalize(boolean n) {
		source.setNormalize(n);
	}
	
	public void tearDown() throws IOException {
		source.tearDown();
	}
	
	/**
	 * Read the impulse response from the given file (separated by whitespaces)
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static double [] loadFIR(File file) throws IOException {
		Scanner sc = new Scanner(new BufferedReader(new FileReader(file)));
		List<Double> imp = new LinkedList<Double>();
		while(sc.hasNext()) {
			try {
				imp.add(sc.nextDouble());
			} catch (InputMismatchException e) {
				throw new IOException("error in reading the " + (imp.size() + 1) + "th impulse");
			}
		}
		
		// build array
		double [] fir = new double [imp.size()];
		int i = 0;
		for (Double d : imp)
			fir[i++] = d;
		
		return fir;
	}
	
	public static final String SYNOPSIS = 
		"sampled.FIRFilter mixer audio-file.wav filter.ascii > ssg16";
	
	public static void main(String [] args) throws Exception {
		if (args.length != 3) {
			System.err.println(SYNOPSIS);
			// System.exit(1);
			
			args = new String [3];
			args[0] = "default_1 [plughw:1,0]";
			args[1] = "/home/sikoried/stud/siniwitt/DA_Daten/RoomResponses/sagi.wav";
			args[2] = "/home/sikoried/stud/siniwitt/DA_Daten/RoomResponses/h423165RIR.txt";
		} 
		
		double [] fir = FIRFilter.loadFIR(new File(args[2]));
	
		AudioFileReader reader2 = new AudioFileReader(args[1], true);			
		FIRFilter firf = new FIRFilter(reader2, fir);
		
		// ssg16
		double scale = Math.pow(2, 16 - 1) - 1;
		
		double [] buf = new double [fir.length];
		int sr;
		while ((sr = firf.read(buf)) > 0) {
			for (int i = 0; i < sr; ++i)
				IOUtil.writeShort(System.out, (short) (buf[i] * scale), ByteOrder.LITTLE_ENDIAN);
		}
		
		// due to bug in thread scheduling in DoubleFFT_1D
		System.exit(0);
	}
}
