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

import de.fau.cs.jstk.exceptions.MalformedParameterStringException;
import de.fau.cs.jstk.io.FrameOutputStream;
import de.fau.cs.jstk.io.FrameSource;
import de.fau.cs.jstk.sampled.*;

public abstract class Window implements FrameSource {
	AudioSource source;

	public static final int HAMMING_WINDOW = 1;
	public static final int HANN_WINDOW = 2;
	public static final int RECTANGULAR_WINDOW = 3;

	/** frame length in ms */
	private double wl = 16;

	/** frame shift in ms */
	private double ws = 10;

	/** number of samples in window */
	protected int nsw;

	/** number of samples for shift */
	private int nss;

	/** weights of the window */
	private double[] w = null;

	/**
	 * Create a default Hamming windos (16ms size, 10ms shift)
	 * 
	 * @param source
	 *            AudioSource to read from
	 */
	public Window(AudioSource source) {
		this.source = source;
		updateNumerOfSamples();
	}

	public FrameSource getSource() {
		return null;
	}

	/**
	 * Create a Hamming window using given frame shift length
	 * 
	 * @param source
	 *            AudioSource to read from
	 * @param windowLength
	 *            Frame length in milli-seconds
	 * @param shiftLength
	 *            Shift length in milli-seconds
	 */
	public Window(AudioSource source, double windowLength, double shiftLength) {
		this.source = source;
		setWindowSpecs(windowLength, shiftLength);
	}

	/**
	 * Get the number of samples within one frame (i.e. the dimension of the
	 * feature vector)
	 * 
	 * @return number of samples within one frame
	 */
	public int getFrameSize() {
		return nsw;
	}

	public double getShift() {
		return ws;
	}

	private void setWindowSpecs(double windowLength, double shiftLength) {
		wl = windowLength;
		ws = shiftLength;

		// can't be longer than frame length
		if (ws > wl)
			ws = wl;

		updateNumerOfSamples();
	}

	private void updateNumerOfSamples() {
		int sr = source.getSampleRate();
		nsw = (int) (sr * wl / 1000.);
		nss = (int) (sr * ws / 1000.);

		// re-allocate ring buffer to correct size, reset current index
		rb = new double[nsw];
		rb_helper = new double[nss];
		cind = -1;

		// initialize the weights
		w = initWeights();
	}

	public int getNumberOfFramesPerSecond() {
		return (int) (1000. / ws);
	}

	/** ring buffer for internal storage of the signal */
	private double[] rb = null;

	/** array to cache the newly read data (nss samples) */
	private double[] rb_helper = null;

	/** current index in the ring buffer */
	private int cind = -1;

	/** number of padded samples */
	private int ps = 0;

	/**
	 * Extract the next frame from the audio stream using a window function
	 * 
	 * @param buf
	 *            buffer to save the signal frame
	 * @return true on success, false if the audio stream terminated before the
	 *         window was filled
	 */
	public boolean read(double[] buf) throws IOException {
		// end of stream?
		if (cind == nsw)
			return false;

		int n = 0;
		if (cind < 0) {
			// initialize the buffer, apply window, return
			n = source.read(rb);

			// anythig read?
			if (n <= 0)
				return false;

			// apply window function to signal
			cind = 0;
			for (int i = 0; i < nsw; ++i)
				buf[i] = rb[i] * w[i];

			// done for now
			return true;
		} else if (ps == 0) {
			// default: read from the source...
			n = source.read(rb_helper);
		}

		// anything read at all? if not, we also need no padding!
		if (n == 0)
			return false;

		if (n == nss) {
			// default: enough frames read
			for (int i = 0; i < nss; ++i)
				rb[(cind + i) % nsw] = rb_helper[i];
		} else {
			// stream comes to an end, take what's there...
			int i;
			for (i = 0; i < n; ++i)
				rb[(cind + i) % nsw] = rb_helper[i];

			// ...and pad with zeros until end; increment the padding counter!
			for (; i < nss; ++i, ++ps)
				rb[(cind + i) % nsw] = 0.;

			// if there's more padded values as the window is large, we have no
			// genuine signal anymore
			if (ps >= nsw) {
				cind = nsw;
				return false;
			}
		}

		// advance ring buffer index
		cind = (cind + nss) % nsw;

		// apply window function to signal
		for (int i = 0; i < nsw; ++i)
			buf[i] = rb[(cind + i) % nsw] * w[i];

		return true;
	}

	/**
	 * Applies the window function to a given single frame
	 * 
	 * @param buf
	 *            buffer containing a single frame
	 */
	public void applyWindowToFrame(double buf[]) throws IllegalArgumentException {
		if (buf.length < nsw) {
			throw new IllegalArgumentException("Given Frame has the wrong size");
		}
		
		// apply window function to signal
		for (int i = 0; i < nsw; i++) {
			buf[i] = buf[i] * w[i];
		}
	}

	/**
	 * Return a copy of the window weights
	 * @return
	 */
	public double [] getWeights() {
		return w.clone();
	}
	
	public String toString() {
		return "length=" + wl + "ms (" + nsw + " samples) shift=" + ws + "ms ("
				+ nss + " samples)";
	}

	/**
	 * Actual window function to be implemented by the subclasses.
	 * 
	 * @return the weights according to the window function
	 */
	protected abstract double[] initWeights();

	/**
	 * Compute the window energy (current implementation is a sum over absolute
	 * values).
	 * 
	 * @param buf
	 * @return
	 */
	public static double energy(double[] buf) {
		double e = 0.;
		for (double d : buf)
			e += Math.abs(d);
		return e;
	}

	/**
	 * Generate a new Window object using the parameter string and AudioSource
	 * 
	 * @param source
	 * @param parameterString
	 *            "hamm|hann|rect,length-ms,shift-ms"
	 * @return
	 * @throws MalformedParameterStringException
	 */
	public static Window create(AudioSource source, String parameterString)
			throws MalformedParameterStringException {
		if (parameterString == null)
			return new HammingWindow(source);
		else {
			try {
				String[] help = parameterString.split(",");
				int length = Integer.parseInt(help[1]);
				int shift = Integer.parseInt(help[2]);
				if (help[0].equals("hamm"))
					return new HammingWindow(source, length, shift);
				else if (help[0].equals("hann"))
					return new HannWindow(source, length, shift);
				else if (help[0].equals("rect"))
					return new RectangularWindow(source, length, shift);
				else
					throw new MalformedParameterStringException(
							"unknown window");
			} catch (Exception e) {
				throw new MalformedParameterStringException(e.toString());
			}
		}
	}
	
	public static Window create(AudioSource source, int windowType,
			double windowLength, double shift) {
		switch (windowType) {
		case Window.RECTANGULAR_WINDOW:
			return new RectangularWindow(source, windowLength, shift);
		case Window.HANN_WINDOW:
			return new HannWindow(source, windowLength, shift);
		case Window.HAMMING_WINDOW:
		default:
			return new HammingWindow(source, windowLength, shift);
		}
	}

	public static final String SYNOPSIS = 
		"framed.AutoCorrelation [format-string] file > frame-output";

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		AudioSource as = new de.fau.cs.jstk.sampled.AudioFileReader(args[0],
				RawAudioFormat.create(args.length > 1 ? args[1] : "f:"
						+ args[0]), true);
		Window window = new HammingWindow(as, 25, 10);

		System.err.println(as);
		System.err.println(window);

		double [] buf = new double[window.getFrameSize()];

		FrameOutputStream fos = new FrameOutputStream(buf.length);
		
		while (window.read(buf))
			fos.write(buf);
		
		fos.close();
	}
}
