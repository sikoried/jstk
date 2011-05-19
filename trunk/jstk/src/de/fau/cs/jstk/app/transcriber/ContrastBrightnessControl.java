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
package de.fau.cs.jstk.app.transcriber;

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JComponent;

public class ContrastBrightnessControl extends JComponent implements
		MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 3434379326516261245L;

	private double x = 0.5;
	private double xMin = 0.0;
	private double xMax = 1.0;
	private double y = 1.0;
	private double yMin = 0.0;
	private double yMax = 2.0;
	private int dragging = 0;
	private Vector<ContrastBrightnessListener> contrastBrightnessListeners;

	public ContrastBrightnessControl() {
		addMouseListener(this);
		addMouseMotionListener(this);

		contrastBrightnessListeners = new Vector<ContrastBrightnessListener>();

		setPreferredSize(new Dimension(200, 200));
	}

	public int convertXtoPX(double x) {
		return (int) ((x - xMin) / (xMax - xMin) * getWidth());
	}

	public double convertPXtoX(int px) {
		double x = xMin + px * (xMax - xMin) / getWidth();
		if (x < xMin) {
			x = xMin;
		}
		if (x > xMax) {
			x = xMax;
		}
		return x;
	}

	public int convertYtoPY(double y) {
		return (int) (getHeight() - (y - yMin) / (yMax - yMin) * getHeight());
	}

	public double convertPYtoY(int py) {
		double y = yMin + (getHeight() - py) * (yMax - yMin) / getWidth();
		if (y < yMin) {
			y = yMin;
		}
		if (y > yMax) {
			y = yMax;
		}
		return y;
	}

	@Override
	public void paintComponent(Graphics g) {
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());

		int px = convertXtoPX(x);
		int py = convertYtoPY(y);
		g.setColor(Color.BLACK);
		g.fillRect(px - 4, py - 4, 8, 8);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		x = convertPXtoX(e.getX());
		y = convertPYtoY(e.getY());
		repaint();
		informListeners();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		int px = convertXtoPX(x);
		int py = convertYtoPY(y);
		int mx = e.getX();
		int my = e.getY();
		if ((Math.abs(px - mx) < 5) && (Math.abs(py - my) < 5)) {
			dragging = 1;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (dragging == 2) {
			x = convertPXtoX(e.getX());
			y = convertPYtoY(e.getY());
			repaint();
			informListeners();
		}
		dragging = 0;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (dragging >= 1) {
			dragging = 2;
			x = convertPXtoX(e.getX());
			y = convertPYtoY(e.getY());
			repaint();
			informListeners();			
		}
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
	}

	public void addContrastBrightnessListener(
			ContrastBrightnessListener listener) {
		contrastBrightnessListeners.add(listener);
	}

	public void informListeners() {
		Iterator<ContrastBrightnessListener> iterator = contrastBrightnessListeners
				.iterator();
		while (iterator.hasNext()) {
			ContrastBrightnessListener listener = iterator.next();
			listener.contrastBrightnessChanged(y, x);
		}
	}

}
