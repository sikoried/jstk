/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
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

import java.io.IOException;
import java.nio.ByteOrder;

import de.fau.cs.jstk.framed.TriangularWindow;
import de.fau.cs.jstk.framed.Window;
import de.fau.cs.jstk.io.IOUtil;
import de.fau.cs.jstk.sampled.AudioFileReader;
import de.fau.cs.jstk.sampled.AudioSource;
import de.fau.cs.jstk.sampled.RawAudioFormat;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * Use the BandPassFilter to pass frequency bands. For improved accuracy,
 * specify the FFT size. Internally, an overlap-add using triangular windows is
 * applied. <b/>
 * This can also be used to implement a band reject filter by constructing a
 * pass filter around the reject bands or using the invert() function.
 * 
 * @author sikoried
 * @author steidl
 */
public class BandPassFilter implements AudioSource {
	/** default FFT block size */
	public static final int DEFAULT_FFT_COEFFICIENTS = 512;
	
	/** source to read from */
	private AudioSource source;
	
	/** internal window (triangular) */
	private Window window;
	
	/** buffer to read from FFT */
	private double [] buf_in;
	
	/** precomputed filter coefficients: 0 (reject) ... (scale) ... 1 (pass) */
	private double [] filter;
	
	/** overlap container */
	private double [] overlap;
	
	/** number of available filtered samples (also the write index within signal */
	private int available = 0;
	
	/** available filtered signal */
	private double [] signal;
	
	/** fft object to perform the filtering */
	private DoubleFFT_1D fft;
	
	/** end of input stream reached? */
	private boolean eos;

	/**
	 * Generate a new band pass filter for the given frequency range and FFT
	 * block size. The block size should be chosen with respect to the desired
	 * frequency resolution and input sample rate.
	 * @param source AudioSource to read from
	 * @param startf start frequency range
	 * @param endf end frequency range
	 * @param fftsize FFT block size, ideally a power of 2
	 */
	public BandPassFilter(AudioSource source, double startf, double endf, int fftsize) {
		this(source, new double [] { startf, endf }, fftsize);
	}
	
	/**
	 * Generate a new band pass filter for the given frequency ranges and FFT
	 * block size. The block size should be chosen with respect to the desired
	 * frequency resolution and input sample rate.
	 * @param source AudioSource to read from
	 * @param bands list of frequency bands [start][end][start][end]...
	 * @param fftsize FFT block size, ideally a power of 2
	 */
	public BandPassFilter(AudioSource source, double [] bands, int fftsize) {
		this.source = source;
		initialize(fftsize);
		setFilterBands(bands);
	}
	
	/**
	 * Get the internal filter size (i.e. the FFT block size).
	 * @return
	 */
	public int getFilterSize() {
		return filter.length;
	}
	
	/**
	 * Set the filter manually. Array indices are according to DoubleFFT_1D. 
	 * Expert use only...
	 * @param filter
	 */
	public void setFilter(double [] filter) {
		if (filter.length != this.filter.length) 
			throw new IllegalArgumentException("filter array size does not match FFT size");
		System.arraycopy(filter, 0, this.filter, 0, filter.length);
	}
	
	/**
	 * Set the filter bands.
	 * @param bands filter bands [2*i] till [2*i+1] Hz
	 */
	public void setFilterBands(double [] bands) {
		double sr = source.getSampleRate();
		double maxf = 0.;
		for (int i = 0; i < filter.length; ++i)
			filter[i] = 0.;
		
		for (int i = 1; i < filter.length / 2; ++i) {
			double f = (double) i * (sr / filter.length);
			for (int j = 0; j < bands.length; j += 2) {
				if (bands[j+1] > maxf)
					maxf = bands[j+1];
				if (f >= bands[j] && f <= bands[j+1]) {
					filter[2 * i] = 1.;
					filter[2 * i + 1] = 1.;
				}
			}
		}
		
		// keep energy
		filter[0] = 1.;
		
		// special treatment for first coefficient due to FFT indexing
		if (sr / 2. > maxf)
			filter[1] = 0.;
		else
			filter[1] = 1.;
	}
	
	private void initialize(int fftsize) {
		// we need 50% overlap triangular filters
		window = new TriangularWindow(source, fftsize, fftsize / 2, true);
		fft = new DoubleFFT_1D(window.getFrameSize());
		
		// initialize filter banks
		filter = new double [window.getFrameSize()];
	}
	
	/**
	 * Invert the filter, i.e. make the complementary reject to a pass filter.
	 */
	public void invert() {
		for (int i = 2; i < filter.length; ++i)
			filter[i] = 1. - filter[i];
	}
	
	public int read(double [] buf) throws IOException {
		return read(buf, buf.length);
	}
	
	public int read(double [] buf, int length) throws IOException {
		// the general procedure is as follows
		// 1) see if there is enough filtered signal available
		// 2) if not, filter next chunk and store it in cache
		// 3) goto 1)
		
		// as long as we don't have enough signal available, filter some!
		while (length > available) {
			if (buf_in == null) {
				// initial call, so compute the initial overlap
				buf_in = new double [window.getFrameSize()];
				if (!window.read(buf_in))
					return 0;
				
				int l = buf_in.length;
				overlap = new double [l / 2];
				
				// reconstruct the very first frame by putting zeroes followed by
				// the first half of the window just read (and re-do the windowing)
				double [] first = new double [l];
				double [] wt = window.getWeights();
				for (int i = 0; i < l / 2; ++i)
					first[i] = 0.;
				for (int i = l / 2; i < l; ++i)
					first[i] = wt[i] * (buf_in[i-l/2] / wt[i-l/2]);
				
				// apply the filter to compute the overlap
				fft.realForward(first);
				for (int i = 0; i < l; i++)
					first[i] *= filter[i];
				fft.realInverse(first, true);
				
				// save the first overlap
				System.arraycopy(first, l/2, overlap, 0, l/2);
				
				// compute the actual first frame
				fft.realForward(buf_in);
				for (int i = 0; i < l; i++)
					buf_in[i] *= filter[i];
				fft.realInverse(buf_in, true);
				
				// add overlap and save new overlap
				for (int i = 0; i < l/2; ++i)
					buf_in[i] += overlap[i];
				System.arraycopy(buf_in, l/2, overlap, 0, l/2);
				
				// initialize the signal buffer, good heuristic is to increase
				// by l*2
				signal = new double [l * 2];
				
				// copy to signal buffer, increase available counter
				System.arraycopy(buf_in, 0, signal, 0, l/2);
				available = l/2;
			} else if (!eos) {
				int l = buf_in.length;
				if (window.read(buf_in)) {
					// compute the actual first frame
					fft.realForward(buf_in);
					for (int i = 0; i < l; i++)
						buf_in[i] *= filter[i];
					fft.realInverse(buf_in, true);
					
					// add overlap and save new overlap
					for (int i = 0; i < l/2; ++i)
						buf_in[i] += overlap[i];
					System.arraycopy(buf_in, l/2, overlap, 0, l/2);
					
					// verify signal buffer size, increase&copy if necessary
					if (signal.length - available < l/2) {
						double [] newsig = new double [signal.length + 2*l];
						System.arraycopy(signal, 0, newsig, 0, available);
						signal = newsig;
					}
					
					// copy signal and increase pointer
					System.arraycopy(buf_in, 0, signal, available, l/2);
					available += l/2;
				} else {
					// we can't read anything more, just output the overlap
					eos = true;
					
					// verify signal buffer size, increase&copy if necessary
					if (signal.length - available < l/2) {
						double [] newsig = new double [signal.length + l/2];
						System.arraycopy(signal, 0, newsig, 0, available);
						signal = newsig;
					}
					
					// copy signal and increase pointer
					System.arraycopy(overlap, 0, signal, available, l/2);
					available += l/2;					
				}
			} else {
				// we are EOS, so the data won't get more...
				if (available > 0) {
					System.arraycopy(signal, 0, buf, 0, available);
					int help = available;
					available = 0;
					return help;
				} else
					return 0;
			}
		}
		
		// copy signal data and shift signal
		System.arraycopy(signal, 0, buf, 0, length);
		for (int i = length; i < signal.length; ++i)
			signal[i-length] = signal[i];
		
		// mind the counter!
		available -= length;
		
		return length;
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
		"usage: sampled.BandPassFilter fft-blocksize startf,endf [startf2,endf2 ...] < ssg16 > filtered-ssg16";
	
	public static void main(String [] args) throws Exception {
		if (args.length < 2) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		int ffts = Integer.parseInt(args[0]);
		double [] b = new double [(args.length-1) * 2 ];
		for (int i = 0; i < args.length-1; ++i) {
			String [] fs = args[i+1].split(",");
			if (fs.length != 2) {
				System.err.println("not a valid band specification: " + args[i+1]);
				System.exit(1);
			}
			b[2*i] = Double.parseDouble(fs[0]);
			b[2*i+1] = Double.parseDouble(fs[1]);
		}
		
		AudioFileReader afr = new AudioFileReader(System.in, RawAudioFormat.getRawAudioFormat("ssg/16"), true);
		BandPassFilter bpf = new BandPassFilter(afr, b, ffts);
		
		double scale = Math.pow(2, 16 - 1) - 1;
		double [] buf = new double [512];
		short [] out = new short [512];
		int r;
		while ((r = bpf.read(buf)) > 0) {
			for (int i = 0; i < r; ++i)
				out[i] = (short)(buf[i] * scale);
			IOUtil.writeShort(System.out, out, r, ByteOrder.LITTLE_ENDIAN);
		}
	}
}
