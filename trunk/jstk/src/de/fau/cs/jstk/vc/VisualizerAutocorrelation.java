/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
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
package de.fau.cs.jstk.vc;

import java.awt.Graphics;


import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import de.fau.cs.jstk.sampled.AudioSource;
import de.fau.cs.jstk.framed.*;
import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.io.BufferedAudioSourceReader;

public class VisualizerAutocorrelation extends VisualComponent {

	private static final long serialVersionUID = -3581036388109199792L;

	private int windowLength; // in [ms]
	private int blockSize = 256; // minimum block size (power of 2)
	private int windowType;

	private int samplerate = 16000;
	private BufferedAudioSourceReader audiosource;
	private Window window;
	private int sample;
	private double autocorrelation[];

	public VisualizerAutocorrelation(BufferedAudioSource source,
			int windowLength, int minBlockSize, int windowType) {
		this.windowLength = windowLength;
		this.blockSize = minBlockSize;
		this.windowType = windowType;

		xMin = 0; // 0 ms
		xtics = 1; // 1 ms
		yMin = -1;
		yMax = 1;
		ytics = 0.5;
		mode = VALUE_MODE;
		enabled = false;
		if (source != null) {
			samplerate = source.getSampleRate();
			audiosource = source.getReader();
			window = createWindow(source, windowType, windowLength, 10);
			enabled = true;
		}

		while (blockSize * 1000.0 / samplerate < windowLength) {
			blockSize *= 2;
		}
		xMax = blockSize * 1000.0 / samplerate;
	}

	public void setBufferedAudioSource(BufferedAudioSource source) {
		audiosource = null;
		enabled = false;
		sample = -1;
		if (source != null) {
			samplerate = source.getSampleRate();
			audiosource = source.getReader();
			window = createWindow(source, windowType, windowLength, 10);
			enabled = true;
		}
		draw();
		repaint();
	}

	private Window createWindow(AudioSource source, int windowType,
			int windowLength, int shift) {
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

	public void setParameters(int windowLength, int minBlockSize, int windowType) {
		boolean changed = false;
		if ((this.windowLength != windowLength)
				|| (this.windowType != windowType)) {
			this.windowLength = windowLength;
			this.windowType = windowType;
			if (audiosource != null) {
				window = createWindow(audiosource, windowType, windowLength, 10);
			}
			changed = true;
		}
		int bsize = minBlockSize;
		while (bsize * 1000.0 / samplerate < windowLength) {
			bsize *= 2;
		}
		if (this.blockSize != bsize) {
			this.blockSize = bsize;
			xMax = blockSize * 1000.0 / samplerate;
			xPerPixel = (double) (xMax - xMin)
					/ (getWidth() - border_left - border_right);
			changed = true;
		}
		if (changed) {
			showAutocorrelation(sample);
		}
	}

	/**
	 * Reaction to the componentResized event
	 */
	public void onResize() {
		adjustSizeOfDoubleBufferingImages();
		xPerPixel = (double) (xMax - xMin)
				/ (getWidth() - border_left - border_right);
		draw();
		repaint();
	}

	/*
	 * private void printArray(String s, double[] array) { System.out.print(s);
	 * for (double value : array) { System.out.print(" " + value); }
	 * System.out.println(); }
	 */

	private void calculateAutocorrelation(int sample) {
		int windowSize = windowLength * samplerate / 1000;

		while (windowSize > blockSize) {
			blockSize *= 2;
		}
		this.sample = sample;

		autocorrelation = new double[blockSize];
		DoubleFFT_1D fft = new DoubleFFT_1D(blockSize);
		DoubleFFT_1D ifft = new DoubleFFT_1D(blockSize / 2);

		audiosource.setReadIndex(sample);
		int count = audiosource.read(autocorrelation, windowSize);
		for (int i = count; i < blockSize; i++) {
			autocorrelation[i] = 0;
		}

		window.applyWindowToFrame(autocorrelation);

		fft.realForward(autocorrelation);

		// refer to the documentation of DoubleFFT_1D.realForward for indexing!
		autocorrelation[0] = (autocorrelation[0] * autocorrelation[0]);

		for (int i = 1; i < (blockSize - (blockSize % 2)) / 2; i++) {
			autocorrelation[2 * i] = autocorrelation[2 * i]
					* autocorrelation[2 * i] + autocorrelation[2 * i + 1]
					* autocorrelation[2 * i + 1];
			autocorrelation[2 * i + 1] = 0.0;
		}

		if (blockSize % 2 == 0) {
			autocorrelation[1] = (autocorrelation[1] * autocorrelation[1]);
		} else {
			autocorrelation[blockSize / 2] = (autocorrelation[blockSize - 1]
					* autocorrelation[blockSize - 1] + autocorrelation[1]
					* autocorrelation[1]);
		}

		ifft.realInverse(autocorrelation, false);
		for (int i = 1; i < autocorrelation.length; i++) {
			autocorrelation[i] /= autocorrelation[0];
		}
		autocorrelation[0] = 1.0;
	}

	@Override
	protected void drawSignal(Graphics g) {
		if ((autocorrelation == null) || !enabled || (sample == -1)) {
			return;
		}

		int px[] = new int[autocorrelation.length / 2];
		int py[] = new int[autocorrelation.length / 2];

		double wLength = blockSize * 1000.0 / samplerate;
		for (int i = 0; i < autocorrelation.length / 2; i++) {
			px[i] = convertXtoPX((double) (i * wLength)
					/ (autocorrelation.length / 2 - 1));
			py[i] = convertYtoPY(autocorrelation[i]);
		}

		g.setColor(colorSignal);
		g.drawPolyline(px, py, px.length);
	}

	public void showAutocorrelation(int sample) {
		if (audiosource != null) {
			calculateAutocorrelation(sample);
		}
		draw();
		repaint();
	}

}
