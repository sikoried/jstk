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
import java.awt.event.MouseEvent;
import java.util.ListIterator;
import java.util.Vector;

import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.io.BufferedAudioSourceReader;
import de.fau.cs.jstk.vc.interfaces.PitchDefinedListener;


public class VisualizerPitchEstimator extends VisualComponent {

	private static final long serialVersionUID = 8935523963242322579L;

	public Color colorFrame = new Color(231, 221, 197);

	private BufferedAudioSourceReader audiosource;
	private int samplerate;
	private int windowLength;
	private int f0Min;
	private int f0Max;
	private double[] values;
	private int sample = -1;
	private double pitchStartSample;
	private double pitchEndSample;
	protected Vector<PitchDefinedListener> pitchDefinedListeners;

	public VisualizerPitchEstimator(BufferedAudioSource source, int windowLength, int f0Min, int f0Max) {
		yMin = -1;
		yMax = +1;
		ytics = 0.25;
		xPerPixel = 0.25;

		audiosource = null;
		enabled = false;
		this.windowLength = windowLength;
		this.f0Min = f0Min;
		this.f0Max = f0Max;
		if (source != null) {
			samplerate = source.getSampleRate();
			audiosource = source.getReader();
			enabled = true;
		}

		pitchDefinedListeners = new Vector<PitchDefinedListener>();
	}

	public void setBufferedAudioSource(BufferedAudioSource source) {
		audiosource = null;
		sample = -1;
		if (source != null) {
			samplerate = source.getSampleRate();
			audiosource = source.getReader();
			enabled = true;
		} else {
			enabled = false;
		}
		draw();
		repaint();
	}
	
	public void setZoom(int zoom) {
		if (zoom > 0) {
			xPerPixel = 1.0 / zoom;
			getValues(sample);
			draw();
			repaint();
		}
	}

	public void onResize() {
		int w = getWidth();
		if ((values == null) || (values.length < w)) {
			values = new double[w];
			showSignal(sample);
		}
		super.onResize();
	}

	public void showSignal(int sample) {
		if (sample > 0) {
			this.sample = sample;
			getValues(sample);
			draw();
			repaint();
		}
	}

	@Override
	protected void drawSignal(Graphics g) {
		if ((audiosource != null) && enabled && (sample != -1)) {
			int s = convertXtoPX(sample);
			int e = convertXtoPX(sample + windowLength * samplerate / 1000 - 1);
			g.setColor(colorFrame);
			g.fillRect(s, border_top, e - s, getHeight() - border_top
					- border_bottom);

			int w = getWidth() - border_left - border_right;
			int x[] = new int[w];
			int y[] = new int[w];

			for (int i = 0; i < w; i++) {
				x[i] = border_left + i;
				y[i] = convertYtoPY(values[i]);
			}

			g.setColor(colorSignal);
			g.drawPolyline(x, y, w);

			int y0 = getHeight() - border_bottom;
			int px = convertXtoPX(pitchStartSample);
			g.drawLine(px, border_top, px, y0);
			px = convertXtoPX(pitchEndSample);
			g.drawLine(px, border_top, px, y0);
		}
	}

	private void getValues(int sample) {
		if ((audiosource != null) && (sample != -1)) {
			int w = getWidth() - border_left - border_right;
			int s = (int) (w * xPerPixel) - windowLength * samplerate / 1000;

			xMin = (int) (sample - s / 2);
			double pos = xMin;

			double min = 0;
			double max = 0;
			for (int i = 0; i < w; i++) {
				values[i] = audiosource.get(pos,
						BufferedAudioSource.SINC_INTERPOLATION);
				if (values[i] > max) {
					max = values[i];
				}
				if (values[i] < min) {
					min = values[i];
				}
				pos += xPerPixel;
			}
			min = Math.abs(min);
			max = Math.abs(max);
			double range = max > min ? max : min;
			range *= 1.1;
			yMin = -range;
			yMax = range;
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		if (dragged && (arg0.getButton() == MouseEvent.BUTTON1)) {
			if ((pitchEndSample - pitchStartSample) > 0) {
				double frequency = samplerate
						/ (pitchEndSample - pitchStartSample);
				if ((frequency >= f0Min) && (frequency <= f0Max)) {
					informPitchSelectedListeners(frequency);
				}
			}
		}
		dragged = false;
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if (dragged) {
			int x = arg0.getX();
			if (x >= startDraggingX) {
				pitchStartSample = getZeroCrossing(startDraggingX);
				pitchEndSample = getZeroCrossing(x);
			} else {
				pitchStartSample = getZeroCrossing(x);
				pitchEndSample = getZeroCrossing(startDraggingX);
			}
			draw();
			repaint();
		}
		dragged = true;
	}
	
	private double getZeroCrossing(int px) {
		int idx = (int) ((convertPXtoX(px) - xMin) / xPerPixel + 0.5);
		idx = px - border_left;
		int l = idx - 3;
		int r = idx + 3;
		if (l < 0) {
			l = 0;
		}
		if (r >= values.length) {
			r = values.length - 1;
		}
		for (int i = l; i < r; i++) {
			if (((values[i] < 0) && (values[i+1] > 0)) ||
				((values[i] > 0) && (values[i+1] < 0))) {
				if (Math.abs(values[i]) < Math.abs(values[i+1])) {
					return convertPXtoX(border_left + i);
				} else {
					return convertPXtoX(border_left + i + 1);
				}
			}
		}
		return convertPXtoX(px);
	}

	public void addPitchDefinedListener(PitchDefinedListener listener) {
		pitchDefinedListeners.add(listener);
	}

	public void informPitchSelectedListeners(double frequency) {
		ListIterator<PitchDefinedListener> iterator = pitchDefinedListeners
				.listIterator();
		while (iterator.hasNext()) {
			PitchDefinedListener listener = iterator.next();
			listener.pitchDefined(frequency);
		}
	}

}
