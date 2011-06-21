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

import de.fau.cs.jstk.framed.Window;
import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.io.BufferedAudioSourceReader;

public class VisualizerSpectrogram extends FileVisualizer implements Runnable {

	private static final long serialVersionUID = 877673756485841354L;

	private double[][] spectrogram;
	double shift;
	private double min = 0;
	private double max = 0;
	private int fft_size = 64;
	private int windowFunction = Window.HAMMING_WINDOW;
	private int windowLength = 16;
	private boolean colorSpectrogram = false;
	private double brightness = 0.5;
	private double gamma = 1.0;

	private Thread computeThread;
	private boolean requestStop = false;

	public VisualizerSpectrogram(String name, BufferedAudioSource source) {
		super(name, source);
		yMin = 0;
		yMax = samplerate / 2.0;
		ytics = 1000;
		border_bottom = 20;
		showCursorY = false;
	}

	@Override
	public void setBufferedAudioSource(BufferedAudioSource source) {
		super.setBufferedAudioSource(source);
		yMax = samplerate / 2.0;
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
		stop();
		if (spectrogram != null) {
			for (int i = 0; i < spectrogram.length; i++) {
				spectrogram[i] = null;
			}
		}
		if (audiosource != null) {
			computeThread = new Thread(this);
			computeThread.start();
		}
	}

	@Override
	public void run() {
		calculate_spectrogram();
	}

	private void stop() {
		if ((computeThread != null) && computeThread.isAlive()) {
			//System.err.println("Asking spectrogram calculation to stop... ");
			requestStop = true;
			while (computeThread.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			requestStop = false;
		}
	}

	protected void calculate_spectrogram() {
		if (audiosource == null) {
			return;
		}
		
		BufferedAudioSourceReader as = audiosource.getReader();

		// System.err.println("Calculating spectrogram... ");

		Window window = Window.create(as, windowFunction, windowLength, 10, false);

		min = 0.0;
		max = 0.0;
		int i = 0;
		boolean finished = false;
		int size = 0;
		if (spectrogram != null) {
			size = spectrogram.length;
		}
		
		while (!finished) {
			
			if (i >= size) {
				xMax = audiosource.getBufferSize() - 1;
				size = (int) Math.ceil(xMax * 100.0 / samplerate);
				if (spectrogram == null) {
					if (size > 0) {
						spectrogram = new double[size][];
					} else {
						try {
							System.err.println("sleeping " + xMax);
							Thread.sleep(100);
						} catch (InterruptedException e) {
						}
						continue;						
					}
				} else if (size > spectrogram.length) {
					double[][] old = spectrogram;
					spectrogram = new double[size][];
					System.arraycopy(old, 0, spectrogram, 0, old.length);					
				} else {
					if (audiosource.stillReading) {
						try {
							System.err.println("sleeping again " + audiosource.getBufferSize());
							Thread.sleep(100);
						} catch (InterruptedException e) {
						}
						continue;
					} else {
						break;
					}
				}
			}
			
			if (i % 100 == 0) {
				Thread.yield();
			}
			
			// int sample = (int) (i * xPerPixel);
			int sample = (int) (i * (samplerate / 100));
			double[] spectrum = VisualizerSpectrum.calculateSpectrum(as,
					sample, samplerate, windowLength, fft_size, window, false);

			if (i == -1) {
				System.err.println();
				System.err.println("sample: " + sample);
				System.err.println("samplerate: " + samplerate);
				System.err.println("windowLength: " + windowLength);
				System.err.println("fft_size: " + fft_size);
				System.err.println("window: " + window);
				for (int j = 0; j < spectrum.length; j++) {
					System.err.print(" " + spectrum[j]);
				}
				System.err.println();
			}

			for (int j = 0; j < spectrum.length; j++) {
				if (spectrum[j] < min) {
					min = spectrum[j];
				}
				if (spectrum[j] > max) {
					max = spectrum[j];
				}
			}			

			spectrogram[i] = spectrum;
			i++;
						
			if (requestStop) {
				break;
			}
		}
		System.err.println("Spectrogram computation finished: " + i);
		
		javax.swing.SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				draw();
				repaint();
			}
		});
	}

	@Override
	protected void drawSignal(Graphics g) {
		if (spectrogram == null) {
			recalculate();
		}

		draw(g);
	}

	public double getValue(double[] spectrum, int fft_size, double frequency) {
		double index = frequency / (0.5 * samplerate / (fft_size - 1));
		int i = (int) index;
		double f = index - i;
		// System.err.println(frequency + " " + index);
		if (i + 1 < spectrum.length) {
			return (1 - f) * spectrum[i] + f * spectrum[i + 1];
		}
		return spectrum[i];
	}

	protected void draw(Graphics g) {
		int y0 = getHeight() - border_bottom;
		int h = y0 - border_top;
		int x1 = convertXtoPX(highlightedSectionStartSample);
		int x2 = convertXtoPX(highlightedSectionEndSample);
		xMax = audiosource.getBufferSize() - 1;
		//int size = (int) Math.ceil(xMax * 100.0 / samplerate);
		
		if (spectrogram == null) {
			return;
		}
		

		int i = (int) (xMin / xPerPixel);
		double hMin = Math.pow(min, gamma);
		double hMax = Math.pow(max, gamma);

		for (int x = border_left; (x < getWidth() - border_right); x++, i++) {
			int idx = (int) (convertPXtoX(x) / samplerate * 100);
			if (idx >= spectrogram.length) {
				break;
			}
			
			double spectrum[] = spectrogram[idx];
		    
			if (spectrum == null) {
				// System.err.println("no spectrum for " + i);
		    	break;
		    }
		    
			boolean selected = false;
			if (showHighlightedSection && isHighlighted && (x >= x1)
					&& (x <= x2)) {
				selected = true;
			}

			for (int j = 0; j <= h; j++) {
				double v = getValue(spectrum, fft_size / 2 + 1, j * yMax / h);
				v = Math.pow(v, gamma);
				int f = (int) ((v - hMin) * 255.0 / (hMax - hMin));

				g.setColor(determineColor(f, selected));
				g.drawLine(x, y0 - j, x, y0 - j);
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

	private Color determineColor(int f, boolean selected) {
		int c1_r = colorBackgroundHighlightedSection.getRed();
		int c1_g = colorBackgroundHighlightedSection.getGreen();
		int c1_b = colorBackgroundHighlightedSection.getBlue();
		int c2_r = colorHighlightedSignal.getRed();
		int c2_g = colorHighlightedSignal.getGreen();
		int c2_b = colorHighlightedSignal.getBlue();

		if (colorSpectrogram) {
			Color c = new Color(Color.HSBtoRGB((255 - f) / 360.0f, 1.0f,
					(float) (brightness + (1.0 - brightness) * f / 255.0)));

			if (selected) {
				int f1 = (c.getRed() * c1_r + (255 - c.getRed()) * c2_r) / 255;
				int f2 = (c.getGreen() * c1_g + (255 - c.getGreen()) * c2_g) / 255;
				int f3 = (c.getBlue() * c1_b + (255 - c.getBlue()) * c2_b) / 255;
				return new Color(f1, f2, f3);
			}
			return c;
		} else {
			f *= ((1 - brightness) * 2);
			if (f > 255) {
				f = 255;
			}

			if (selected) {
				int f1 = ((255 - f) * c1_r + f * c2_r) / 255;
				int f2 = ((255 - f) * c1_g + f * c2_g) / 255;
				int f3 = ((255 - f) * c1_b + f * c2_b) / 255;
				return new Color(f1, f2, f3);
			}
			return new Color(255 - f, 255 - f, 255 - f);
		}

	}

	private int determineFFTSize() {
		int h = getHeight() - border_bottom - border_top;
		int new_fft_size = (int) (windowLength * samplerate / 1000);
		while (new_fft_size < h) {
			new_fft_size = new_fft_size * 2;
		}
		return new_fft_size;
	}

	@Override
	public void onResize() {
		int new_fft_size = determineFFTSize();
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

	private void refresh() {
		recalculate();
		draw();
		repaint();
	}

	public void setColorSpectrogram(boolean color) {
		colorSpectrogram = color;
		draw();
		repaint();
	}

	public void setWindowFunction(int function) {
		windowFunction = function;
		refresh();
	}

	public void setWindowLength(int length) {
		windowLength = length;
		fft_size = determineFFTSize();
		refresh();
	}

	public void setProperties(double gamma, double brightness) {
		if ((gamma > 0.09) && (gamma < 2.01)) {
			this.gamma = gamma;
		}
		if ((brightness >= 0.0f) && (brightness <= 1.0f)) {
			this.brightness = brightness;
		}
		draw();
		repaint();
	}

	public boolean getColor() {
		return this.colorSpectrogram;
	}

	public double getGamma() {
		return this.gamma;
	}

	public double getBrightness() {
		return brightness;
	}

	public double getWindowLength() {
		return windowLength;
	}

	public int getWindowFunction() {
		return windowFunction;
	}

}
