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

import java.awt.Color;
import java.awt.Graphics;
import java.io.*;

import de.fau.cs.jstk.sampled.AudioSource;
import de.fau.cs.jstk.framed.*;
import de.fau.cs.jstk.io.BufferedAudioSource;

public class VisualizerSpectrogram extends FileVisualizer {

	private static final long serialVersionUID = 877673756485841354L;

	public int windowFunction = Window.HAMMING_WINDOW;
	public double windowLength = 16;
	public float brightness = 0.4f;
	public boolean colorSpectrogram = false;

	private double[][] spectrogram;
	double shift;
	private double min = 0;
	private double max = 0;
	private int fft_size = 64;
	private double gamma = 1.0;

	public VisualizerSpectrogram(String name, BufferedAudioSource source) {
		super(name, source);
		yMin = 0;
		ytics = 1000;
		border_bottom = 20;
		showCursorY = false;
	}

	@Override
	protected void drawHighlightedArea(Graphics g) {
		// nothing to do since the "background" is part of the spectrogram
	}

	@Override
	protected void drawSelectedArea(Graphics g) {
		// nothing to do since only vertical lines are shown (after drawing the
		// spectrogram)
	}

	@Override
	protected void recalculate() {
		calculate_spectrogram();
	}

	protected void calculate_spectrogram() {
		if (audiosource == null) {
			return;
		}

		Window window;
		shift = (xPerPixel * 1000 / samplerate);
		if (shift < 5.0) {
			shift = 5.0;
		}

		int size = (int) Math.ceil(xMax * 1000 / (shift * samplerate));
		AudioSource as = audiosource.getReader();

		switch (windowFunction) {
		case Window.RECTANGULAR_WINDOW:
			window = new RectangularWindow(as, windowLength, shift);
			break;
		case Window.HANN_WINDOW:
			window = new HannWindow(as, windowLength, shift);
			break;
		default:
			window = new HammingWindow(as, windowLength, shift);
		}
		FFT fft = new FFT(window, fft_size);

		if ((spectrogram == null) || (spectrogram.length < size)
				|| (spectrogram[0].length < fft.getFrameSize())) {
			spectrogram = new double[size][fft.getFrameSize()];
		}

		try {
			int i = 0;
			while ((i < spectrogram.length) && fft.read(spectrogram[i])) {
				for (int j = 0; j < spectrogram[i].length; j++) {
					spectrogram[i][j] = Math.pow(spectrogram[i][j], gamma);
					if (spectrogram[i][j] < min) {
						min = spectrogram[i][j];
					}
					if (spectrogram[i][j] > max) {
						max = spectrogram[i][j];
					}
				}
				i++;
			}
		} catch (IOException e) {
			System.err.println(e);
		}
	}

	@Override
	protected void drawSignal(Graphics g) {
		if (spectrogram == null) {
			calculate_spectrogram();
		}

		draw(g);
	}

	protected void draw(Graphics g) {
		double nshift = shift * samplerate / 1000;
		int y0 = getHeight() - border_bottom;
		int x1 = convertXtoPX(highlightedSectionStartSample);
		int x2 = convertXtoPX(highlightedSectionEndSample);
		int c1_r = colorBackgroundHighlightedSection.getRed();
		int c1_g = colorBackgroundHighlightedSection.getGreen();
		int c1_b = colorBackgroundHighlightedSection.getBlue();
		int c2_r = colorHighlightedSignal.getRed();
		int c2_g = colorHighlightedSignal.getGreen();
		int c2_b = colorHighlightedSignal.getBlue();
		int num_samples = audiosource.getBufferSize();
		int size = (int) Math.ceil(num_samples / nshift);

		double samples = xMin;
		for (int x = border_left; x < getWidth() - border_right; x++) {
			samples += xPerPixel;
			int idx = (int) (samples / nshift);
			if (idx >= size) {
				break;
			}
			double spectrum[] = spectrogram[idx];
			boolean selected = false;
			if (showHighlightedSection && isHighlighted && (x >= x1)
					&& (x <= x2)) {
				selected = true;
			}
			int s = fft_size / 2;
			if (s > y0 - border_top) {
				s = y0 - border_top;
			}
			for (int i = 0; i < s; i++) {
				int f = (int) ((spectrum[i] - min) * 255.0 / (max - min));
				if (f > 255) {
					f = 255;
				}
				if (f < 0) {
					f = 0;
				}

				if (colorSpectrogram) {
					Color c = new Color(Color.HSBtoRGB((255-f)/360.0f, 1.0f, brightness+(1.0f-brightness)*f/255.0f));
					if (selected) {
						int f1 = (c.getRed() * c1_r + (255 - c.getRed()) * c2_r) / 255;
						int f2 = (c.getGreen() * c1_g + (255 - c.getGreen()) * c2_g) / 255;
						int f3 = (c.getBlue() * c1_b + (255 - c.getBlue()) * c2_b) / 255;
						g.setColor(new Color(f1, f2, f3));
					} else {
						g.setColor(c);
					}					
				} else {
					f = 255 - f;
					if (selected) {
						int f1 = (f * c1_r + (255 - f) * c2_r) / 255;
						int f2 = (f * c1_g + (255 - f) * c2_g) / 255;
						int f3 = (f * c1_b + (255 - f) * c2_b) / 255;
						g.setColor(new Color(f1, f2, f3));
					} else {
						g.setColor(new Color(f, f, f));
					}
				}
				g.drawLine(x, y0 - i, x, y0 - i);
			}
		}

		// draw vertical lines to show selected area
		if (isSelected) {
			g.setColor(colorSelectedSignal);
			x1 = convertXtoPX(selectedSectionStartSample);
			x2 = convertXtoPX(selectedSectionEndSample);
			g.drawLine(x1, border_top, x1, y0);
			g.drawLine(x2, border_top, x2, y0);
		}
	}

	@Override
	public void onResize() {
		int h = getHeight() - border_bottom - border_top;
		int new_fft_size = (int) (windowLength * samplerate / 1000);
		while (new_fft_size < h) {
			new_fft_size = new_fft_size * 2;
		}
		yMax = h * samplerate / new_fft_size;
		if (new_fft_size != fft_size) {

			fft_size = new_fft_size;
			calculate_spectrogram();
		}
		super.onResize();
	}

	@Override
	public String toString() {
		return "VisualizerSpectrogram '" + name + "'";
	}

	public void setGamma(double gamma) {
		if ((gamma > 0.09) && (gamma < 2.01)) {
			this.gamma = gamma;
			recalculate();
			draw();
			repaint();
		}
	}

	public double getGamma() {
		return this.gamma;
	}

}
