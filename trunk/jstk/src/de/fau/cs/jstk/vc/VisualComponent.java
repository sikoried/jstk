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
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;

import javax.swing.*;

import de.fau.cs.jstk.vc.interfaces.MouseMotionVisualizationListener;

public abstract class VisualComponent extends JComponent implements
		MouseListener, MouseMotionListener {

	private static final long serialVersionUID = -8767301104015473779L;

	/**
	 * constant defining the standard mode of the visual component
	 */
	public static final int NORMAL_MODE = 0;

	/**
	 * constant defining the mode to select a single sample
	 */
	public static final int VALUE_MODE = 1;

	/**
	 * constant defining the mode to select an area of the signal
	 */
	public static final int SELECTION_MODE = 2;

	/**
	 * constant defining the zoom mode
	 */
	public static final int ZOOM_MODE = 3;

	/**
	 * Size of the top border in pixels
	 */
	public int border_top = 10;

	/**
	 * size of the bottom border in pixels
	 */
	public int border_bottom = 10;

	/**
	 * size of the left border in pixels
	 */
	public int border_left = 20;

	/**
	 * number of samples shown per pixel
	 */
	public double xPerPixel = 160;

	/**
	 * first x value that is shown in the component
	 */
	public double xMin = 0;

	/**
	 * maximum x value; this value does not determine the visible region which
	 * is determined only by xMin and xPerPixel
	 */
	public double xMax = 0;

	/**
	 * minimum y value; arbitrary unit
	 */
	public double yMin = -1.05;

	/**
	 * maximum y value; arbitrary unit
	 */
	public double yMax = 1.05;

	/**
	 * difference between xtics in number of samples
	 */
	public int xtics = 16000;

	/**
	 * difference between ytics; arbitrary unit
	 */
	public double ytics = 0.5;

	/**
	 * size of the right border in pixels
	 */
	public int border_right = 10;

	/**
	 * color of the visual component's background, if the component has to valid
	 * audiosource
	 */
	public Color colorBackgroundDisabled = new Color(230, 230, 230);

	/**
	 * color of the visual component's background
	 */
	public Color colorBackground = Color.WHITE;

	/**
	 * color of the signal
	 */
	public Color colorSignal = new Color(64, 64, 64);

	/**
	 * color of the coordinate system
	 */
	public Color colorCoordinateSytem = Color.BLACK;

	/**
	 * color of the mouse cursor (vertical and horizontal lines) in VALUE_MODE
	 */
	public Color colorCursor = new Color(192, 192, 192);

	/**
	 * defines whether the vertical cursor line is shown in VALUE_MODE
	 */
	public boolean showCursorX = true;

	/**
	 * defines whether the horizontal cursor line is shown in VALUE_MODE
	 */
	public boolean showCursorY = true;

	protected int mode = NORMAL_MODE;
	protected boolean enabled = true;
	protected BufferedImage img;
	protected BufferedImage imgMouse;
	protected boolean dragged = false;
	protected int startDraggingX;
	protected int startDraggingY;

	protected Vector<MouseMotionVisualizationListener> mouseMotionListeners;

	/**
	 * Default constructor
	 */
	protected VisualComponent() {

		mouseMotionListeners = new Vector<MouseMotionVisualizationListener>();

		// install listener for componentResized events
		addComponentListener(new ComponentListener() {

			@Override
			public void componentHidden(ComponentEvent arg0) {
			}

			@Override
			public void componentMoved(ComponentEvent arg0) {
			}

			@Override
			public void componentResized(ComponentEvent arg0) {
				onResize();
			}

			@Override
			public void componentShown(ComponentEvent arg0) {
			}

		});

		// component reacts to MouseListener and MouseMotionListener events
		// itself
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	/**
	 * Copies the buffered image onto the Graphics object g
	 */
	@Override
	public void paintComponent(Graphics g) {
		if (imgMouse != null) {
			g.drawImage(imgMouse, 0, 0, null);
		}
	}

	/**
	 * Increases the size of all images used for double buffering if necessary.
	 */
	protected void adjustSizeOfDoubleBufferingImages() {
		final int STEPS = 100;
		if ((img == null) || (img.getWidth() < getWidth())
				|| (img.getHeight() < getHeight())) {
			int w = roundUp(getWidth(), STEPS);
			int h = roundUp(getHeight(), STEPS);
			img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			imgMouse = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		}
	}

	/**
	 * Reaction to the componentResized event
	 */
	public void onResize() {
		adjustSizeOfDoubleBufferingImages();
		draw();
		repaint();
	}

	protected void draw() {
		if (img == null) {
			return;
		}
		Graphics gr = img.getGraphics();
		clear(gr);
		drawSignal(gr);
		drawCoordinateSystem(gr);
		imgMouse.getGraphics().drawImage(img, 0, 0, null);
	}

	/**
	 * Fills the complete Graphics object g with the background color
	 * colorBackground
	 * 
	 * @param g
	 *            Graphics object
	 */
	protected void clear(Graphics g) {
		if (enabled) {
			g.setColor(colorBackground);
			g.fillRect(0, 0, getWidth(), getHeight());
		} else {
			g.setColor(colorBackgroundDisabled);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
	}

	/**
	 * Has to be overwritten by all derived classes in order to draw the signal
	 * on the Graphics object g.
	 * 
	 * @param g
	 *            Graphics object
	 */
	abstract protected void drawSignal(Graphics g);

	/**
	 * Draws a coordinate system on the Graphics object g
	 * 
	 * @param g
	 *            Graphics object
	 */
	protected void drawCoordinateSystem(Graphics g) {
		int y_base = (int) Math
				.round((getHeight() - border_bottom - border_top) * yMax
						/ (yMax - yMin))
				+ border_top;
		g.setColor(colorCoordinateSytem);

		// y-axis
		g.drawLine(border_left, border_top, border_left, getHeight()
				- border_bottom);

		// x-axis
		g.drawLine(border_left, y_base, getWidth() - border_right, y_base);

		// xtics
		if (xtics > 0) {

			double s = xMin;
			if ((int) (s / xtics) * xtics != s) {
				s = (int) (s / xtics + 1) * xtics;
			}
			int p = convertXtoPX(s);
			do {
				g.drawLine(p, y_base, p, y_base + 5);
				s += xtics;
				p = convertXtoPX(s);
			} while (p < getWidth() - border_right);
		}

		// ytics
		if (ytics > 0) {
			double y = 0;
			int p = convertYtoPY(y);
			do {
				g.drawLine(border_left - 5, p, border_left, p);
				y += ytics;
				p = convertYtoPY(y);
			} while (y <= yMax);

			y = -ytics;
			p = convertYtoPY(y);
			while (y >= yMin) {
				g.drawLine(border_left - 5, p, border_left, p);
				y -= ytics;
				p = convertYtoPY(y);
			}
		}
	}

	/**
	 * Draws a horizontal and/or a vertical line through the point of the cursor
	 * 
	 * @param g
	 *            Graphics object
	 * @param x
	 *            x-coordinate of the cursor
	 * @param y
	 *            y-coordinate of the cursor
	 */
	protected void drawCursor(Graphics g, int x, int y) {
		if ((mode == VALUE_MODE) || (mode == SELECTION_MODE)) {
			g.setColor(colorCursor);
			if ((x >= border_left) && (x <= getWidth() - border_right)
					&& (y >= border_top) && (y <= getHeight() - border_bottom)) {

				if (showCursorY) {
					g.drawLine(border_left, y, getWidth() - border_right, y);
				}
				if (showCursorX) {
					g.drawLine(x, border_top, x, getHeight() - border_bottom);
				}
			}
		}
	}

	protected void drawCursor(Graphics g, int x) {
		g.setColor(colorCursor);
		if ((x >= border_left) && (x <= getWidth() - border_right)) {
			g.drawLine(x, border_top, x, getHeight() - border_bottom);
		}
	}

	/**
	 * Rounds the given integer number up to the next position
	 * 
	 * @param number
	 *            number to be rounded up
	 * @param to
	 * @return number rounded up
	 */
	protected int roundUp(int number, int to) {
		if (number / to * to == number) {
			return number;
		}
		return (number / to + 1) * to;
	}

	/**
	 * Converts the given real-world x-coordinate into the x-coordinate of the
	 * pixel on the Graphics object
	 * 
	 * @param x
	 *            real-world x-coordinate
	 * @return x-coordinate of the pixel
	 */
	protected int convertXtoPX(double x) {
		return (int) Math.round(border_left + (x - xMin) / xPerPixel);
	}

	/**
	 * Converts a given x-coordinate of a pixel on the Graphics object into the
	 * real-world x-coordinate
	 * 
	 * @param px
	 *            x-coordinate of the pixel
	 * @return real-world x-coordinate
	 */
	protected double convertPXtoX(int px) {
		return xMin + (px - border_left) * xPerPixel;
	}

	/**
	 * Converts the real-world y value into the y-coordinate of the pixel on the
	 * Graphics object
	 * 
	 * @param y
	 *            real-world y value
	 * @return y-coordinate of the pixel
	 */
	protected int convertYtoPY(double y) {
		return border_top
				+ (int) Math.round((getHeight() - border_top - border_bottom)
						* (yMax - y) / (yMax - yMin));
	}

	/**
	 * Converts the y-coordinate of the pixel on the Graphics object into the
	 * real-world y value
	 * 
	 * @param pz
	 *            y-coordinate of the pixel
	 * @return real-world y value
	 */
	protected double convertPYtoY(int py) {
		return yMax - (yMax - yMin) * (py - border_top)
				/ (getHeight() - border_top - border_bottom);
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		startDraggingX = arg0.getX();
		startDraggingY = arg0.getY();
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		if (!enabled) {
			return;
		}
		if ((mode == VALUE_MODE) || (mode == SELECTION_MODE)) {
			if (showCursorY || showCursorX) {
				int px = arg0.getX();
				int py = arg0.getY();
				Graphics g = imgMouse.getGraphics();
				g.drawImage(img, 0, 0, null);
				drawCursor(g, px, py);
				repaint();
				if ((px >= border_left) && (px <= getWidth() - border_right)
						&& (py >= border_top)
						&& (py <= getHeight() - border_bottom)) {
					double x = convertPXtoX(px);
					if (x < xMax) {
						double y = convertPYtoY(py);
						informMouseMotionVisualizationListeners(x, y);
					}
				}
			}
		}
	}

	/**
	 * Informs all registered MouseMotionVisualizationListeners that that mouse
	 * has moved to the real-world coordinates (x, y)
	 * 
	 * @param x
	 *            x value in real-world coordinates
	 * @param value
	 *            y value in real-world coordinates
	 */
	public void informMouseMotionVisualizationListeners(double x, double y) {
		ListIterator<MouseMotionVisualizationListener> iterator = mouseMotionListeners
				.listIterator();
		while (iterator.hasNext()) {
			MouseMotionVisualizationListener listener = iterator.next();
			listener.mouseMoved(x, y);
		}
	}

	/**
	 * Adds the given MouseMotionVisualizationListener to the list of listeners
	 * 
	 * @param listener
	 *            MouseMotionVisualizationListener to be added
	 */
	public void addMouseMotionVisualizationListener(
			MouseMotionVisualizationListener listener) {
		mouseMotionListeners.addElement(listener);
	}

}
