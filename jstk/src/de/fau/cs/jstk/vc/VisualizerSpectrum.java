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

import de.fau.cs.jstk.framed.Window;
import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.io.BufferedAudioSourceReader;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class VisualizerSpectrum extends VisualComponent {

	private static final long serialVersionUID = -4210642749764396885L;

	private int windowLength; // in [ms]
	private int blockSize; // minimum block size (power of 2)
	private int windowType;

	private int samplerate;
	private BufferedAudioSourceReader audiosource;
	private Window window;
	private int sample;
	private double spectrum[];

	public VisualizerSpectrum(BufferedAudioSource source, int windowLength,
			int minBlockSize, int windowType) {
		this.windowLength = windowLength;
		this.blockSize = minBlockSize;
		this.windowType = windowType;

		xMin = 0; // 0 kHz
		xMax = 8050;
		xtics = 1000; // 1 kHz
		yMin = -110;
		yMax = 0;
		ytics = 10;
		mode = VALUE_MODE;
		enabled = false;

		if (source != null) {
			samplerate = source.getSampleRate();
			xMax = samplerate / 2;
			audiosource = source.getReader();
			window = Window.create(source, windowType, windowLength, 10);
			enabled = true;
		}
	}

	public void setBufferedAudioSource(BufferedAudioSource source) {
		audiosource = null;
		enabled = false;
		sample = -1;
		if (source != null) {
			samplerate = source.getSampleRate();
			audiosource = source.getReader();
			window = Window.create(source, windowType, windowLength, 10);
			enabled = true;
		}
		draw();
		repaint();
	}

	public void setParameters(int windowLength, int minBlockSize, int windowType) {
		boolean changed = false;
		if ((this.windowLength != windowLength)
				|| (this.windowType != windowType)) {
			this.windowLength = windowLength;
			this.windowType = windowType;
			if (audiosource != null) {
				window = Window.create(audiosource, windowType, windowLength,
						10);
				changed = true;
			}
		}
		if (this.blockSize != minBlockSize) {
			this.blockSize = minBlockSize;
			if (audiosource != null) {
				changed = true;
			}
		}
		if (changed) {
			showSpectrum(sample);
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

	public static double[] calculateSpectrum(
			BufferedAudioSourceReader audiosource, int sample, int samplerate,
			double windowLength, int blockSize, Window window, boolean log) {
		int windowSize = (int) (windowLength * samplerate / 1000);

		while (windowSize > blockSize) {
			blockSize *= 2;
		}
		double buf[] = new double[blockSize];
		DoubleFFT_1D fft = new DoubleFFT_1D(blockSize);
		double[] spectrum = new double[blockSize / 2 + 1];

		audiosource.setReadIndex(sample);
		int count = audiosource.read(buf, windowSize);
		// TODO: if (count < 0)... hoenig
		for (int i = count; i < blockSize; i++) {
			buf[i] = 0;
		}

		window.applyWindowToFrame(buf);

		fft.realForward(buf);

		// refer to the documentation of DoubleFFT_1D.realForward for indexing!
		spectrum[0] = Math.abs(buf[0]);
		double spectralEnergy = spectrum[0];

		for (int i = 1; i < (blockSize - (blockSize % 2)) / 2; i++) {
			spectrum[i] = Math.sqrt(buf[2 * i] * buf[2 * i] + buf[2 * i + 1]
					* buf[2 * i + 1]);
			spectralEnergy += spectrum[i];
		}

		if (blockSize % 2 == 0) {
			spectrum[blockSize / 2] = Math.abs(buf[1]);
		} else {
			spectrum[blockSize / 2] = Math.sqrt(buf[blockSize - 1] * buf[blockSize - 1]
					+ buf[1] * buf[1]);
		}

		spectralEnergy += spectrum[blockSize / 2];

		// normalize the spectral energy to 1
		if (spectralEnergy > 0.) {
			for (int i = 0; i < spectrum.length; i++) {
				spectrum[i] /= spectralEnergy;
				if (log) {spectrum[i] = 20 * Math.log10(spectrum[i]); }
			}
		}

		return spectrum;
	}

	@Override
	protected void drawSignal(Graphics g) {
		if ((spectrum == null) || !enabled || (sample == -1)) {
			return;
		}

		int px[] = new int[spectrum.length];
		int py[] = new int[spectrum.length];

		for (int i = 0; i < spectrum.length; i++) {
			px[i] = convertXtoPX(i * samplerate / 2.0 / (spectrum.length - 1));
			py[i] = convertYtoPY(spectrum[i]);
		}

		g.setColor(colorSignal);
		g.drawPolyline(px, py, px.length);
	}

	public void showSpectrum(int sample) {
		this.sample = sample;
		spectrum = calculateSpectrum(audiosource, sample, samplerate,
				windowLength, blockSize, window, true);
		draw();
		repaint();
	}
}
