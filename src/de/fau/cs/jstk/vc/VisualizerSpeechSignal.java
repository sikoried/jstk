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

import de.fau.cs.jstk.io.BufferedAudioSource;


public class VisualizerSpeechSignal extends FileVisualizer {

	private static final long serialVersionUID = 7150449147394338325L;

	public VisualizerSpeechSignal(String name, BufferedAudioSource source) {
		super(name, source);
	}

	@Override
	protected void recalculate() {
		// nothing to calculate
	}

	@Override
	protected void drawSignal(Graphics g) {
		if (xPerPixel > 1) {
			draw(g);
		} else {
			draw_zoom(g);
		}
	}

	protected void draw_zoom(Graphics g) {
		double s = xMin;
		double v = audiosource.get((int) xMin);
		int last_py = convertYtoPY(v);

		for (int px = border_left + 1; px <= getWidth() - border_right; px++) {
			s += xPerPixel;
			v = audiosource.get(s, BufferedAudioSource.LINEAR_INTERPOLATION);
			int py = convertYtoPY(v);

			if (showHighlightedSection && isHighlighted
					&& (s >= highlightedSectionStartSample)
					&& (s <= highlightedSectionEndSample)) {
				g.setColor(colorHighlightedSignal);
			} else if (isSelected && (s >= selectedSectionStartSample)
					&& (s <= selectedSectionEndSample)) {
				g.setColor(colorSelectedSignal);
			} else {
				g.setColor(colorSignal);
			}
			g.drawLine(px - 1, last_py, px, py);
			last_py = py;
		}
	}

	protected void draw(Graphics g) {
		int last_py = 0;
		int last_py2 = 0;
		int py0 = convertYtoPY(0);
		int s = (int) xMin;
		int mode = 0;

		for (int px = border_left + 1; px <= getWidth() - border_right; px++) {
			double v_alt = audiosource.get(s++);
			double min = v_alt;
			double max = v_alt;
			double mean = v_alt;
			int zero_crossings = 0;

			for (int i = 1; i < xPerPixel; i++) {
				double v = audiosource.get(s++);
				mean += v;
				if (v < min) {
					min = v;
				}
				if (v > max) {
					max = v;
				}
				if (((v_alt < 0) && (v > 0)) || ((v_alt > 0) && (v < 0))) {
					zero_crossings++;
				}
				v_alt = v;
			}
			mean /= xPerPixel;
			if (showHighlightedSection && isHighlighted
					&& (s >= highlightedSectionStartSample)
					&& (s <= highlightedSectionEndSample)) {
				g.setColor(colorHighlightedSignal);
			} else if (isSelected && (s >= selectedSectionStartSample)
					&& (s <= selectedSectionEndSample)) {
				g.setColor(colorSelectedSignal);
			} else {
				g.setColor(colorSignal);
			}
			if (zero_crossings < 2) {
				int py = convertYtoPY(mean);
				if (mode > 0) {
					if (mode == 1) {
						if (mean > 0) {
							last_py = last_py2;
						}
					}
					g.drawLine(px - 1, last_py, px, py);
				}
				last_py = py;
				mode = 2;
			} else {
				int py1 = convertYtoPY(min);
				int py2 = convertYtoPY(max);
				g.drawLine(px, py0, px, py1);
				g.drawLine(px, py0, px, py2);
				if (mode == 2) {
					if (mean < 0) {
						g.drawLine(px - 1, last_py, px, py1);
					} else {
						g.drawLine(px - 1, last_py, px, py2);
					}
				}
				last_py = py1;
				last_py2 = py2;
				mode = 1;
			}
		}
	}

	@Override
	public String toString() {
		return "VisualizerSpeechSignal '" + name + "'";
	}

}
