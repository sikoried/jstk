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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ListIterator;
import java.util.Vector;

import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.io.BufferedFrameSource;
import de.fau.cs.jstk.util.SplineInterpolation;
import de.fau.cs.jstk.vc.interfaces.F0PointsSelectedListener;


public class VisualizerPitch extends FileVisualizer {

	private static final long serialVersionUID = -2301943139572446782L;

	public int f0max = 600;
	public int f0min = 50;
	public int shift;
	public Color colorSignal2 = Color.RED;

	private BufferedFrameSource pitchSource1;
	private BufferedFrameSource pitchSource2;
	private boolean[] selected;
	private int endDraggingX;
	private int endDraggingY;
	private boolean changed = false;
	private Vector<F0PointsSelectedListener> f0PointsSelectedListeners;

	public VisualizerPitch(String name, BufferedAudioSource audiosource,
			BufferedFrameSource pitchSource1, BufferedFrameSource pitchSource2,
			int shift) {
		super(name, audiosource);
		this.pitchSource1 = pitchSource1;
		this.pitchSource2 = pitchSource2;
		this.shift = shift;

		yMin = 0;
		yMax = f0max;
		ytics = 100;

		if (audiosource != null) {
			selected = new boolean[(int) (xMax / shift)];
		}

		f0PointsSelectedListeners = new Vector<F0PointsSelectedListener>();
	}

	@Override
	public void setBufferedAudioSource(BufferedAudioSource source) {
		this.pitchSource1 = null;
		this.pitchSource2 = null;
		super.setBufferedAudioSource(source);

		selected = null;
		if (source != null) {
			selected = new boolean[(int) (xMax / shift)];
		}
		changed = false;
	}

	public void setBufferedPitchSources(BufferedFrameSource pitchSource1,
			BufferedFrameSource pitchSource2) {
		this.pitchSource1 = pitchSource1;
		this.pitchSource2 = pitchSource2;
		changed = false;
		draw();
		repaint();
	}

	@Override
	protected void recalculate() {
	}

	@Override
	protected void drawSignal(Graphics g) {
		double nshift = shift * samplerate / 1000;

		int firstFrame = (int) (xMin / nshift);
		double lastSample = convertPXtoX(getWidth() - border_right);
		if (lastSample > xMax) {
			lastSample = xMax;
		}
		int lastFrame = (int) (lastSample / nshift - 1);
		if ((pitchSource1 != null)
				&& (lastFrame >= pitchSource1.getBufferSize())) {
			lastFrame = pitchSource1.getBufferSize() - 1;
		}

		if (pitchSource1 != null) {
			for (int i = firstFrame; i <= lastFrame; i++) {
				int f0_1 = (int) pitchSource1.get(i)[0];
				int f0_2 = (pitchSource2 == null) ? f0_1 : (int) pitchSource2
						.get(i)[0];
				int py1 = convertYtoPY(f0_1);
				int py2 = convertYtoPY(f0_2);
				double sample = (i + 0.5) * nshift;
				int px = convertXtoPX(sample);

				g.setColor(colorSignal);
				g.drawOval(px - 1, py1 - 1, 3, 3);
				if (py1 == py2) {
					if (selected[i]) {
						g.fillOval(px - 1, py1 - 1, 3, 3);
					}
				} else {
					g.setColor(colorSignal2);
					g.drawOval(px - 1, py2 - 1, 3, 3);
					if (selected[i]) {
						g.fillOval(px - 1, py2 - 1, 3, 3);
					}
				}
			}
		}

		if (dragged) {
			int x1 = (startDraggingX < endDraggingX) ? startDraggingX
					: endDraggingX;
			int y1 = (startDraggingY < endDraggingY) ? startDraggingY
					: endDraggingY;
			int w = Math.abs(endDraggingX - startDraggingX);
			int h = Math.abs(endDraggingY - startDraggingY);
			g.setColor(colorBackgroundSelectedArea);
			g.drawRect(x1, y1, w, h);
		}

	}

	private int pitchAt(int px, int py) {
		if ((px < border_left) || (px > getWidth() - border_right)) {
			return -1;
		}
		double nshift = shift * samplerate / 1000;
		int sample = (int) convertPXtoX(px);

		if (sample > xMax) {
			return -1;
		}

		int index = (int) (sample / nshift);
		int f0 = -1;

		if (Math.abs(convertXtoPX((index + 0.5) * nshift) - px) > 3) {
			return -1;
		}

		if ((pitchSource2 != null) && (index < pitchSource2.getBufferSize())) {
			f0 = (int) pitchSource2.get(index)[0];
		} else if ((pitchSource1 != null)
				&& (index < pitchSource1.getBufferSize())) {
			f0 = (int) pitchSource1.get(index)[0];
		} else {
			return -1;
		}

		if (Math.abs(f0 - convertPYtoY(py)) < 20) {
			return index;
		}

		return -1;
	}
	
	public void removeSelection() {
		for (int i = 0; i < selected.length; i++) {
			selected[i] = false;
		}
		draw();
		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		if (!enabled) {
			return;
		}
		if (arg0.getButton() == MouseEvent.BUTTON1) {
			int idx = pitchAt(arg0.getX(), arg0.getY());
			if (idx != -1) {
				if (arg0.isShiftDown()) {
					if (!selected[idx]) {
						int l = idx;
						while ((l >= 0) && (!selected[l])) {
							l--;
						}
						if (l < 0) {
							l = idx;
						}
						int r = idx;
						while ((r < selected.length) && (!selected[r])) {
							r++;
						}
						if (r >= selected.length) {
							r = idx;
						}
						for (int i = l; i <= r; i++) {
							selected[i] = true;
						}
					}
				} else if (arg0.isControlDown()) {
					selected[idx] = !selected[idx];
				} else {
					boolean state = selected[idx];
					for (int i = 0; i < selected.length; i++) {
						selected[i] = false;
					}
					selected[idx] = !state;
				}
				if (getNumberOfSelectedFrames() == 1) {
					markSelectedFrame(idx);
				}
				informF0PointsSelectedListeners();
			} else {
				for (int i = 0; i < selected.length; i++) {
					selected[i] = false;
				}
			}
			draw();
			repaint();
		}
		super.mouseClicked(arg0);
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		super.mouseDragged(arg0);

		if (mode == NORMAL_MODE) {
			endDraggingX = arg0.getX();
			endDraggingY = arg0.getY();
			draw();
			repaint();
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		if (!enabled) {
			return;
		}
		if (dragged && (arg0.getButton() == MouseEvent.BUTTON1)) {
			if (mode == NORMAL_MODE) {
				int x1 = (startDraggingX < endDraggingX) ? startDraggingX
						: endDraggingX;
				int y1 = (startDraggingY < endDraggingY) ? startDraggingY
						: endDraggingY;
				int x2 = x1 + Math.abs(endDraggingX - startDraggingX);
				int y2 = y1 + Math.abs(endDraggingY - startDraggingY);
				double f0max = convertPYtoY(y1);
				double f0min = convertPYtoY(y2);
				double nshift = shift * samplerate / 1000.0;
				int idx1 = (int) (convertPXtoX(x1) / nshift);
				int idx2 = (int) (convertPXtoX(x2) / nshift);

				if (convertXtoPX((idx1 + 0.5) * nshift) < x1) {
					idx1++;
				}
				if (convertXtoPX((idx2 + 0.5) * nshift) > x2) {
					idx2--;
				}

				if (!arg0.isControlDown()) {
					for (int i = 0; i < selected.length; i++) {
						selected[i] = false;
					}
				}
				for (int i = idx1; i <= idx2; i++) {
					if ((pitchSource2 != null)
							&& (i < pitchSource2.getBufferSize())) {
						if ((pitchSource2.get(i)[0] >= f0min)
								&& (pitchSource2.get(i)[0] <= f0max)) {
							selected[i] = true;
						}
					} else if ((pitchSource1 != null)
							&& (i < pitchSource1.getBufferSize())) {
						if ((pitchSource1.get(i)[0] >= f0min)
								&& (pitchSource1.get(i)[0] <= f0max)) {
							selected[i] = true;
						}
					}
				}
				informF0PointsSelectedListeners();
				dragged = false;
				draw();
				repaint();
			}
		}
		super.mouseReleased(arg0);
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		super.mouseMoved(arg0);
		int index = pitchAt(arg0.getX(), arg0.getY());
		if (index == -1) {
			setToolTipText(null);
		} else {
			double frequency = (int) (pitchSource2.get(index)[0] * 10) / 10.0;
			setToolTipText(frequency + " Hz");			
		}
	}
	
	private void markSelectedFrame(int index) {
		double nshift = shift * samplerate / 1000;
		markedX = index * nshift;
		isMarked = true;
		informVisualizationListeners();
		informSampleSelectedListeners((int) markedX);
	}

	public void setToOriginal() {
		if (pitchSource2 != null) {
			for (int i = 0; i < selected.length; i++) {
				if (selected[i]) {
					pitchSource2.get(i)[0] = pitchSource1.get(i)[0];
				}
			}
			changed = true;
			draw();
			repaint();
		}
	}

	public void setToZero() {
		if (pitchSource2 != null) {
			for (int i = 0; i < selected.length; i++) {
				if (selected[i]) {
					pitchSource2.get(i)[0] = 0;
				}
			}
			changed = true;
			draw();
			repaint();
		}
	}

	public void setToHalf() {
		if (pitchSource2 != null) {
			for (int i = 0; i < selected.length; i++) {
				if ((selected[i]) && (pitchSource2.get(i)[0] >= (2 * f0min))) {
					pitchSource2.get(i)[0] /= 2;
				}
			}
			changed = true;
			draw();
			repaint();
		}
	}

	public void setToDouble() {
		if (pitchSource2 != null) {
			for (int i = 0; i < selected.length; i++) {
				if ((selected[i]) && (pitchSource2.get(i)[0] <= (f0max / 2))) {
					pitchSource2.get(i)[0] *= 2;
				}
			}
			changed = true;
			draw();
			repaint();
		}
	}

	public void setToValue(int frame, double frequency) {
		if (pitchSource2 != null) {
			pitchSource2.get(frame)[0] = frequency;
			changed = true;
			draw();
			repaint();
		}
	}

	public boolean hasChanged() {
		if ((audiosource == null) || (pitchSource2 == null)) {
			return false;
		}
		return changed;
	}

	public int getNumberOfSelectedFrames() {
		if (pitchSource2 == null) {
			return 0;
		}
		int n = 0;
		for (int i = 0; i < selected.length; i++) {
			if (selected[i]) {
				n++;
			}
		}
		return n;
	}

	public F0Point[] getSelectedPoints() {
		int n = getNumberOfSelectedFrames();
		if (n < 1) {
			return null;
		}
		F0Point[] points = new F0Point[n];

		n = 0;
		for (int i = 0; i < selected.length; i++) {
			if (selected[i]) {
				points[n++] = new F0Point(i, pitchSource2.get(i)[0]);
			}
		}

		return points;
	}

	private int getLeftNeighbor(int idx) {
		for (int i = idx - 1; i >= 0; i--) {
			if (selected[i]) {
				return i;
			}
		}
		return -1;
	}

	private int getRightNeighbor(int idx) {
		for (int i = idx + 1; i < selected.length; i++) {
			if (selected[i]) {
				return i;
			}
		}
		return -1;
	}

	public void splineInterpolation() {
		if (getNumberOfSelectedFrames() > 1) {
			F0Point[] points = getSelectedPoints();
			SplineInterpolation spline = new SplineInterpolation(points);

			for (int i = 0; i < selected.length; i++) {
				if (!selected[i]) {
					int l = getLeftNeighbor(i);
					int r = getRightNeighbor(i);
					if ((l != -1) && (r != -1)) {
						pitchSource2.get(i)[0] = spline.getValueAt(i);
					}
				}
			}

			draw();
			repaint();
		}
	}

	public void addF0PointsSelectedListener(F0PointsSelectedListener listener) {
		f0PointsSelectedListeners.add(listener);
	}

	public void informF0PointsSelectedListeners() {
		F0Point[] points = getSelectedPoints();
		ListIterator<F0PointsSelectedListener> iterator = f0PointsSelectedListeners
				.listIterator();
		while (iterator.hasNext()) {
			F0PointsSelectedListener listener = iterator.next();
			listener.f0PointsSelected(points);
		}

	}

}
