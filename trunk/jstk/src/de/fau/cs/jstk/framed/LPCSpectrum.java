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
import de.fau.cs.jstk.sampled.AudioSource;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * Compute the LPC model spectrum which is basically a smoothed spectrum; can be
 * used to determine the formant structure.
 * 
 * @author sikoried
 * 
 */
public class LPCSpectrum implements FrameSource {
	/** default order of LPC computation */
	public static final int DEFAULT_ORDER = 14;

	/** pad the FFT input coefficients to the next power of 2 */
	public static final boolean DEFAULT_PAD = true;

	/** pad the FFT input coefficients to the next power of 2? */
	private boolean pad;

	/** order of the LPC computation */
	private int order;

	/** FrameSource to read from */
	private FrameSource source;

	/** autocorrelation values */
	private double[] ac;

	/** alphas for Durbin iteration */
	private double[] alpha;

	/** input frame size */
	private int fs_in;

	/** output frame size */
	private int fs_out;

	/** FFT frame size */
	private int fs_fft;

	/** internal buffer for FFT */
	private double[] buf_fft;

	/** FFT instance */
	private DoubleFFT_1D fft;

	/**
	 * Create a LPC spectrum with DEFAULT_ORDER and DEFAULT_PAD
	 * 
	 * @param source
	 */
	public LPCSpectrum(FrameSource source) {
		this(source, DEFAULT_ORDER, DEFAULT_PAD);
	}

	public FrameSource getSource() {
		return source;
	}
	
	/**
	 * Create a LPC spectrum of given order and DEFAULT_PAD
	 * 
	 * @param source
	 * @param order
	 */
	public LPCSpectrum(FrameSource source, int order) {
		this(source, DEFAULT_ORDER, DEFAULT_PAD);
	}

	/**
	 * Create a LPC spectrum with given order and padding
	 * 
	 * @param source
	 * @param order
	 * @param pad
	 */
	public LPCSpectrum(FrameSource source, int order, boolean pad) {
		this.source = source;
		this.order = order;
		this.pad = pad;

		initialize();
	}

	/**
	 * Initialize internal buffers
	 */
	private void initialize() {
		// init internal buffers
		fs_in = source.getFrameSize();
		ac = new double[fs_in];

		alpha = new double[order + 1];

		fs_fft = fs_in;

		if (pad) {
			// pad to the next power of 2, min 512
			int min = 512;

			while (min < fs_fft)
				min = min << 1;

			fs_fft = min;
		} else {
			// maybe the frame is larger than the default fft frame?
			if (fs_fft < fs_in)
				fs_fft = fs_in;
		}

		fft = new DoubleFFT_1D(fs_fft);
		buf_fft = new double[fs_fft];
		fs_out = fs_fft / 2 + 1;
	}

	public int getFrameSize() {
		return fs_out;
	}

	/**
	 * Read next frame from the AutoCorrelation and compute the LPC spectrum
	 */
	public boolean read(double[] buf) throws IOException {
		if (!(source.read(ac))) {
			return false;
		}

		// Durbin iteration
		double sum;
		double e = ac[0];
		alpha[0] = buf_fft[0] = 1.;

		for (int n = 1; n <= order; ++n) {
			sum = 0.0;
			for (int j = 1; j < n; ++j)
				sum += buf_fft[j] * ac[n - j];

			double k = -(ac[n] + sum) / e;

			alpha[n] = k;

			for (int j = 1; j < n; ++j) {
				alpha[j] = buf_fft[j] + k * buf_fft[n - j];
			}

			e *= (1. - k * k);

			for (int j = 0; j <= n; ++j)
				buf_fft[j] = alpha[j];
		}

		// set un-computed coefficients to zero
		for (int j = order + 1; j < fs_fft; j++)
			buf_fft[j] = 0.;

		// forward fft
		fft.realForward(buf_fft);

		// refer to the documentation of DoubleFFT_1D.realForward for indexing!
		// integrate the LPC spectrum computation!
		buf[0] = Math.abs(buf_fft[0]);

		for (int i = 1; i < (fs_fft - (fs_fft % 2)) / 2; ++i)
			buf[i] = e
					/ (buf_fft[2 * i] * buf_fft[2 * i] + buf_fft[2 * i + 1]
							* buf_fft[2 * i + 1]);

		if (fs_fft % 2 == 0)
			buf[fs_fft / 2] = e / (buf_fft[1] * buf_fft[1]);
		else
			buf[fs_fft / 2] = e
					/ (buf_fft[fs_fft - 1] * buf_fft[fs_fft - 1] + buf_fft[1]
							* buf_fft[1]);

		return true;
	}

	public String toString() {
		return "lpc: fs_in=" + fs_in + " fs_fft=" + fs_fft + " fs_out="
				+ fs_out + " order=" + order;
	}

	public static void main(String[] args) throws Exception {
		AudioSource as = new de.fau.cs.jstk.sampled.AudioFileReader(args[0], true);
		System.err.println(as);

		Window w = new HammingWindow(as, 25, 10);
		System.err.println(w);

		FrameSource lpc = new LPCSpectrum(new AutoCorrelation(w));
		System.err.println(lpc);

		double[] buf = new double[lpc.getFrameSize()];

		while (lpc.read(buf)) {
			int i = 0;
			for (; i < buf.length - 1; ++i)
				System.out.print(buf[i] + " ");
			System.out.println(buf[i]);
		}
	}
}
