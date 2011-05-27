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
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.vc.interfaces.AudioBufferListener;
import de.fau.cs.jstk.vc.interfaces.SampleSelectedListener;
import de.fau.cs.jstk.vc.interfaces.SignalSectionSelectedListener;
import de.fau.cs.jstk.vc.interfaces.VisualizationListener;


/**
 * Super class for all visual components that visualize information for whole
 * audio files, e.g. the audio signal, the spectrogram, the transliteration,
 * etc.
 * 
 * @author Stefan Steidl
 * 
 */
public abstract class FileVisualizer extends VisualComponent implements
		VisualizationListener, AudioBufferListener {

	/**
	 * Constant defining that the visible section of the signal is not changed
	 * if a section is highlighted.
	 */
	public static final int ALIGN_NONE = 0;

	/**
	 * Constant defining that the visible section of the signal is centered
	 * around a given sample number (if the sample number is > 0) or the
	 * highlighted section is centered in the visible section (it the given
	 * sample number equals zero).
	 */
	public static final int ALIGN_CENTER = 1;

	/**
	 * 
	 */
	public static final int ALIGN_RIGHT = 2;

	/**
	 * 
	 */
	public static final int ALIGN_LEFT = 3;

	/**
	 * constant defining the minimum number of samples shown per pixel
	 */
	public static final double MIN_SAMPLES_PER_PIXEL = 0.1;

	/**
	 * constant defining the maximum number of samples shown per pixel
	 */
	public static final double MAX_SAMPLES_PER_PIXEL = 1000;

	private static final Cursor NORMAL_CURSOR = new Cursor(
			Cursor.DEFAULT_CURSOR);
	private static final Cursor VALUE_CURSOR = new Cursor(
			Cursor.CROSSHAIR_CURSOR);
	private final Cursor ZOOM_CURSOR = getToolkit().createCustomCursor(
			new ImageIcon(this.getClass().getResource("/vc/magnifier.gif")).getImage(), new Point(10, 10), "magnifier");

	private static final long serialVersionUID = 5437098041319843569L;
	protected String name;
	protected BufferedAudioSource audiosource;
	protected JScrollBar scrollbar;
	protected double markedX;

	/**
	 * samplerate of the audio signal
	 */
	public int samplerate = 16000;

	/**
	 * number of the first sample of the highlighted section
	 */
	public int highlightedSectionStartSample = 16000;

	/**
	 * number of the last sample of the highlighted section
	 */
	public int highlightedSectionEndSample = 48000;

	/**
	 * indicates whether an area of the signal is highlighted or not
	 */
	public boolean isHighlighted = false;

	/**
	 * number of the first sample of the selected section
	 */
	public int selectedSectionStartSample = 0;

	/**
	 * number of the last sample of the selected section
	 */
	public int selectedSectionEndSample = 0;

	/**
	 * indicates whether an area of the signal has been selected by mouse
	 * dragging or not
	 */
	protected boolean isSelected = false;

	/**
	 * indicates whether a single sample of the signal has been selected by mouse
	 * click or not
	 */
	protected boolean isMarked = false;

	/**
	 * color of the background of the highlighted section of the signal
	 */
	public Color colorBackgroundHighlightedSection = new Color(231, 221, 197);

	/**
	 * color of the highlighted section of the signal
	 */
	public Color colorHighlightedSignal = new Color(120, 103, 75);

	/**
	 * color of the background of the selected section of the signal
	 */
	public Color colorBackgroundSelectedArea = new Color(230, 230, 230);

	/**
	 * color of the selected section of the signal
	 */
	public Color colorSelectedSignal = new Color(120, 120, 120);

	/**
	 * defines whether the highlighted area is actually shown highlighted or not
	 */
	public boolean showHighlightedSection = true;

	private Vector<VisualizationListener> visualizationListeners;
	private Vector<SampleSelectedListener> sampleSelectedListeners;
	private Vector<SignalSectionSelectedListener> signalSectionSelectedListeners;

	/**
	 * Constructor of the super class
	 * 
	 * @param name
	 *            name of the component
	 * @param source
	 *            BufferedAudioSource where the audio signal is read from
	 */
	protected FileVisualizer(String name, BufferedAudioSource source) {
		this.name = name;
		audiosource = source;

		enabled = false;
		if (source != null) {
			source.addBufferListener(this);
			xMax = source.getBufferSize() - 1;
			enabled = true;
		}

		// create vectors to store various registered listeners
		visualizationListeners = new Vector<VisualizationListener>();
		sampleSelectedListeners = new Vector<SampleSelectedListener>();
		signalSectionSelectedListeners = new Vector<SignalSectionSelectedListener>();

		// start in NORMAL_MODE (default)
		switchMode(NORMAL_MODE);
	}

	public void setBufferedAudioSource(BufferedAudioSource source) {
		audiosource = source;
		if (source != null) {
			enabled = true;
			xMin = 0;
			xMax = source.getBufferSize() - 1;
			isSelected = false;
			isHighlighted = false;
			isMarked = false;
			source.addBufferListener(this);
			if (scrollbar != null) {
				scrollbar.setEnabled(true);
			}
			recalculate();
			draw();
			repaint();
			adjustScrollBar();
		} else {
			enabled = false;
			xMin = 0;
			xMax = 0;
			isSelected = false;
			isHighlighted = false;
			isMarked = false;
			if (scrollbar != null) {
				scrollbar.setEnabled(false);
			}
			draw();
			repaint();
		}
	}

	/**
	 * There are different modes defining the visual component's behavior for
	 * mouse events:
	 * 
	 * @see FileVisualizer#NORMAL_MODE, FileVisualizer#VALUE_MODE,
	 *      FileVisualizer#SELECTION_MODE, FileVisualizer#ZOOM_MODE
	 * @param mode
	 *            desired mode
	 */
	public void switchMode(int mode) {
		this.mode = mode;
		switch (mode) {
		case NORMAL_MODE:
			setCursor(NORMAL_CURSOR);
			break;
		case VALUE_MODE:
			setCursor(VALUE_CURSOR);
			break;
		case SELECTION_MODE:
			setCursor(NORMAL_CURSOR);
			break;
		case ZOOM_MODE:
			setCursor(ZOOM_CURSOR);
			break;
		default:
			setCursor(NORMAL_CURSOR);
		}
	}

	/**
	 * connects a JScrollbar object to the visual component
	 * 
	 * @param bar
	 *            JScrollbar object to be connected
	 */
	public void setScrollbar(JScrollBar bar) {
		scrollbar = bar;
		scrollbar.addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(AdjustmentEvent arg0) {
				xMin = arg0.getValue();
				draw();
				repaint();
				informVisualizationListeners();
			}

		});
	}

	/**
	 * Sets the highlighted area of the visual component
	 * 
	 * @param startSample
	 *            number of the first sample of the selected area
	 * @param endSample
	 *            number of the last sample of the selected area
	 * @param mode
	 *            defines the visible section of the signal
	 * @param sample
	 *            number of sample the visible section of the signal is aligned
	 *            to
	 */
	public void setHighlightedRegion(int startSample, int endSample, int mode,
			int sample) {
		highlightedSectionStartSample = startSample;
		highlightedSectionEndSample = endSample;
		isHighlighted = true;
		double r = (int) convertPXtoX(getWidth() - border_right);
		double l = xMin;
		double ws = r - l;

		switch (mode) {
		case ALIGN_CENTER:
			if (sample > 0) {
				xMin = sample - ws / 2;
			} else {
				xMin = highlightedSectionStartSample
						- (ws - (highlightedSectionEndSample - highlightedSectionStartSample))
						/ 2;
			}
			break;
		case ALIGN_RIGHT:
			if ((sample - ws > xMin)
					&& (sample - ws < highlightedSectionStartSample)) {
				xMin = sample - ws;
			}
			break;
		case ALIGN_LEFT:
			if ((sample < xMin) && (highlightedSectionEndSample < sample + ws)) {
				xMin = sample;
			}
			break;
		}

		if (xMin + ws > xMax) {
			xMin = xMax - ws;
		}
		if (xMin < 0) {
			xMin = 0;
		}

		draw();
		repaint();
		informVisualizationListeners();
	}

	public boolean getSelected() {
		return isSelected;
	}

	public void setSelected(boolean value) {
		isSelected = value;
		draw();
		repaint();
		informVisualizationListeners();
	}
	
	public boolean getMarked() {
		return isMarked;
	}
	
	public void setMarked(boolean value) {
		isMarked = value;
		draw();
		repaint();
		informVisualizationListeners();
	}

	/**
	 * Draws the component: fills the background, draws the background of the
	 * selected area of the signal, draws the complete signal, and draws the
	 * coordinate system
	 */
	protected void draw() {
		if (img == null) {
			return;
		}
		Graphics gr = img.getGraphics();
		clear(gr);
		drawHighlightedArea(gr);
		drawSelectedArea(gr);
		if (audiosource != null) {
			drawSignal(gr);
		}
		drawCoordinateSystem(gr);
		drawSelectedSample(gr);
		imgMouse.getGraphics().drawImage(img, 0, 0, null);
	}

	/**
	 * Fills the background of the highlighted area of the signal with the color
	 * colorBackgroundHighlightedSection
	 * 
	 * @param g
	 *            Graphics object
	 */
	protected void drawHighlightedArea(Graphics g) {
		if (!showHighlightedSection || !isHighlighted) {
			return;
		}
		g.setColor(colorBackgroundHighlightedSection);
		int x1 = convertXtoPX(highlightedSectionStartSample);
		int x2 = convertXtoPX(highlightedSectionEndSample);
		if (x1 < border_left) {
			x1 = border_left;
		}
		if (x2 > getWidth() - border_right) {
			x2 = getWidth() - border_right;
		}
		g.fillRect(x1, border_top, x2 - x1 + 1, getHeight() - border_bottom
				- border_top);
	}

	/**
	 * Fills the background of the highlighted area of the signal with the color
	 * colorBackgroundHighlightedSection
	 * 
	 * @param g
	 *            Graphics object
	 */
	protected void drawSelectedArea(Graphics g) {
		if ((!enabled) || !isSelected) {
			return;
		}
		g.setColor(colorBackgroundSelectedArea);
		int x1 = convertXtoPX(selectedSectionStartSample);
		int x2 = convertXtoPX(selectedSectionEndSample);
		if (x1 < border_left) {
			x1 = border_left;
		}
		if (x2 > getWidth() - border_right) {
			x2 = getWidth() - border_right;
		}
		g.fillRect(x1, border_top, x2 - x1 + 1, getHeight() - border_bottom
				- border_top);
	}

	/**
	 * Draws a vertical line if a sample has been selected
	 * @param g
	 *            Graphics object
	 */
	public void drawSelectedSample(Graphics g) {
		// draw vertical line if one sample has been marked
		if (isMarked) {
			g.setColor(colorSelectedSignal);
			int x = convertXtoPX(markedX);
			g.drawLine(x, border_top, x, getHeight() - border_bottom);			
		}
	}

	/**
	 * Adds the given VisualizationListener to the list of listeners
	 * 
	 * @param listener
	 *            VisualizationListener to be added
	 */
	public void addVisualizationListener(VisualizationListener listener) {
		visualizationListeners.addElement(listener);
	}

	/**
	 * Adds the given SampleSelectedListener to the list of listeners
	 * 
	 * @param listener
	 *            SampleSelectedListener to be added
	 */
	public void addSampleSelectedListener(SampleSelectedListener listener) {
		sampleSelectedListeners.addElement(listener);
	}

	/**
	 * Adds the given SignalSectionSelectedListener to the list of listeners
	 * 
	 * @param listener
	 *            SignalSectionSelectedListener to be added
	 */
	public void addSignalSectionSelectedListener(
			SignalSectionSelectedListener listener) {
		signalSectionSelectedListeners.addElement(listener);
	}

	/**
	 * Sets the VisualizationInformer
	 * 
	 * @param informer
	 *            VisualizationInformer to be set
	 */
	public void setVisualizationInformer(VisualizationInformer informer) {
		visualizationListeners.addElement(informer);
		informer.addVisualizationListener(this);
	}

	/**
	 * Informs all registered VisualizationListeners about a visualization
	 * change
	 */
	public void informVisualizationListeners() {
		ListIterator<VisualizationListener> iterator = visualizationListeners
				.listIterator();
		while (iterator.hasNext()) {
			VisualizationListener listener = iterator.next();
			// System.err.println(this + " informs " + listener +
			// " about a visualization change: " + xMin + " " + xMax);
			listener.VisualizationChanged(this, xPerPixel, xMin,
					highlightedSectionStartSample, highlightedSectionEndSample,
					isHighlighted, selectedSectionStartSample,
					selectedSectionEndSample, isSelected, markedX, isMarked);
		}
	}

	/**
	 * Informs all registered VisualizationListeners about a mouse movement
	 */
	public void informVisualizationListenersAboutMouseMovement(int sample) {
		ListIterator<VisualizationListener> iterator = visualizationListeners
				.listIterator();
		while (iterator.hasNext()) {
			VisualizationListener listener = iterator.next();
			listener.mouseMoved(this, sample);
		}
	}

	/**
	 * Informs all registered SampleSelectedListeners that a sample has been
	 * selected
	 * 
	 * @param sample
	 *            Sample that was selected
	 */
	public void informSampleSelectedListeners(int sample) {
		ListIterator<SampleSelectedListener> iterator = sampleSelectedListeners
				.listIterator();
		while (iterator.hasNext()) {
			SampleSelectedListener listener = iterator.next();
			listener.sampleSelected(sample);
		}
	}

	/**
	 * Informs all registered SignalSectionSelectedListeners that a section of
	 * the signal has been selected or deselected
	 * 
	 * @param sample
	 *            Sample that was selected
	 */
	public void informSignalSectionSelectedListeners() {
		ListIterator<SignalSectionSelectedListener> iterator = signalSectionSelectedListeners
				.listIterator();
		while (iterator.hasNext()) {
			SignalSectionSelectedListener listener = iterator.next();
			listener.sectionSelected(isSelected, selectedSectionStartSample,
					selectedSectionEndSample);
		}
	}

	/**
	 * Reaction to the componentResized event
	 */
	@Override
	public void onResize() {
		adjustSizeOfDoubleBufferingImages();
		adjustScrollBar();
		draw();
		repaint();
	}

	/**
	 * Method where calculations can be done that are needed to draw the signal
	 * but that should not be done for each single repaint
	 */
	abstract protected void recalculate();

	/**
	 * Adjusts the registered scrollbar
	 */
	protected void adjustScrollBar() {
		if (audiosource == null) {
			return;
		}

		// adjust minSample; has to be done even if no scrollbar exists
		int maxSample = audiosource.getBufferSize();
		int visibleSamples = (int) Math
				.round((getWidth() - border_left - border_right) * xPerPixel);
		if ((xMin + visibleSamples) > maxSample) {
			xMin = maxSample - visibleSamples;
		}
		if (xMin < 0) {
			xMin = 0;
		}

		if (scrollbar == null) {
			return;
		}

		// prevent scrollbar from triggering adjustment events
		AdjustmentListener[] listeners = scrollbar.getAdjustmentListeners();
		for (AdjustmentListener l : listeners) {
			scrollbar.removeAdjustmentListener(l);
		}

		// adjust scrollbar
		scrollbar.setMinimum(0);
		scrollbar.setMaximum(audiosource.getBufferSize());
		scrollbar.setUnitIncrement(visibleSamples / 20);
		scrollbar.setValue((int) xMin); // order of setValue and setVisible
		scrollbar.setVisibleAmount(visibleSamples); // important!

		// restore adjustment listeners
		for (AdjustmentListener l : listeners) {
			scrollbar.addAdjustmentListener(l);
		}
	}

	/**
	 * Zooms into or out of the signal by the given factor
	 * 
	 * @param factor
	 *            zoom factor
	 */
	protected void zoom(double factor) {
		xPerPixel *= factor;
		checkSamplesPerPixel();
		adjustScrollBar();
		recalculate();
		draw();
		repaint();
	}

	/**
	 * Zooms into the selected area of the signal
	 * 
	 * @param endDraggingX
	 *            x-coordinate of the mouse after defining the selected area
	 */
	protected void zoom(int endDraggingX) {
		if (Math.abs(startDraggingX - endDraggingX) < 10) {
			// mouse dragging was more like a mouse click than real dragging
			zoom(1.25);
			return;
		}

		int i1 = (int) convertPXtoX(startDraggingX);
		int i2 = (int) convertPXtoX(endDraggingX);
		xPerPixel = (double) Math.abs(i2 - i1)
				/ (getWidth() - border_left - border_right);
		checkSamplesPerPixel();
		xMin = Math.min(i1, i2);
		adjustScrollBar();
		recalculate();
		draw();
		repaint();
	}

	/**
	 * Guarantees that samplesPerPixel is within the interval
	 * [MIN_SAMPLES_PER_PIXEL; MAX_SAMPLES_PER_PIXEL]
	 */
	private void checkSamplesPerPixel() {
		if (xPerPixel < MIN_SAMPLES_PER_PIXEL) {
			xPerPixel = MIN_SAMPLES_PER_PIXEL;
		}
		if (xPerPixel > MAX_SAMPLES_PER_PIXEL) {
			xPerPixel = MAX_SAMPLES_PER_PIXEL;
		}
	}

	private void leftClick(int px) {
		if ((px >= border_left) && (px <= getWidth() - border_right)) {
			double x = convertPXtoX(px);
			if (x <= xMax) {
				informSampleSelectedListeners((int) x);
				markedX = x;
				isMarked = true;
				draw();
				repaint();
				informVisualizationListeners();
			}
		}
	}

	/**
	 * Reaction depends on the mode of the component: In the ZOOM_MODE, the
	 * visual components zoom in (left mouse click) and out (right mouse click).
	 * In the VALUE_MODE, the component triggers SampleSelected events on left
	 * mouse clicks if at least the vertical line cursor (showCursorX) is shown.
	 * In the SELECTION_MODE, an existing selection is deselected and all
	 * registered SignalSectionSelectedListeners are informed.
	 */
	@Override
	public void mouseClicked(MouseEvent arg0) {
		if (arg0.getButton() == MouseEvent.BUTTON3) {
			if (mode == ZOOM_MODE) {
				zoom(1.25);
				informVisualizationListeners();
			}
		}
		if (arg0.getButton() == MouseEvent.BUTTON1) {
			if (mode == ZOOM_MODE) {
				zoom(0.8);
				informVisualizationListeners();
			}
			if (mode == VALUE_MODE) {
				if (showCursorX) {
					leftClick(arg0.getX());
				}
			}
			if (mode == SELECTION_MODE) {
				if (isSelected) {
					isSelected = false;
					draw();
					repaint();
					informVisualizationListeners();
					informSignalSectionSelectedListeners();
				} else if (showCursorX) {
					leftClick(arg0.getX());
				}
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		if (dragged && (arg0.getButton() == MouseEvent.BUTTON1)) {
			if (mode == ZOOM_MODE) {
				zoom(arg0.getX());
				informVisualizationListeners();
			}
			if (mode == SELECTION_MODE) {
				informVisualizationListeners();
				informSignalSectionSelectedListeners();
			}
		}
		dragged = false;
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if (mode == SELECTION_MODE) {
			if (dragged) {
				int x = arg0.getX();
				if (x >= startDraggingX) {
					selectedSectionStartSample = (int) convertPXtoX(startDraggingX);
					selectedSectionEndSample = (int) convertPXtoX(x);
				} else {
					selectedSectionStartSample = (int) convertPXtoX(x);
					selectedSectionEndSample = (int) convertPXtoX(startDraggingX);
				}
				isMarked = false;
				isSelected = true;
				draw();
				repaint();
				informVisualizationListeners();
			}
		}
		dragged = true;
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		if (!enabled) {
			return;
		}
		super.mouseMoved(arg0);

		if ((mode == VALUE_MODE) || (mode == SELECTION_MODE)) {
			if (showCursorX) {
				int px = arg0.getX();
				int py = arg0.getY();
				if ((px >= border_left) && (px <= getWidth() - border_right)
						&& (py >= border_top)
						&& (py <= getHeight() - border_bottom)) {
					int x = (int) convertPXtoX(px);
					informVisualizationListenersAboutMouseMovement(x);
				} else {
					informVisualizationListenersAboutMouseMovement(-1);					
				}
			}
		}
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		super.mouseExited(arg0);

		Graphics g = imgMouse.getGraphics();
		g.drawImage(img, 0, 0, null);
		repaint();

		if ((mode == VALUE_MODE) || (mode == SELECTION_MODE)) {
			if (showCursorX) {
				informVisualizationListenersAboutMouseMovement(-1);				
			}
		}
	}

	public void VisualizationChanged(Object sender, double samplesPerPixel,
			double minSample, int highlightedSectionStartSample,
			int highlightedSectionEndSample, boolean isHighlighted,
			int selectedSectionStartSample, int selectedSectionEndSample,
			boolean isSelected, double markedX, boolean isMarked) {
		boolean recalculate = false;
		if (this.xPerPixel != samplesPerPixel) {
			recalculate = true;
			this.xPerPixel = samplesPerPixel;
		}
		this.xMin = minSample;
		this.highlightedSectionStartSample = highlightedSectionStartSample;
		this.highlightedSectionEndSample = highlightedSectionEndSample;
		this.isHighlighted = isHighlighted;
		this.selectedSectionStartSample = selectedSectionStartSample;
		this.selectedSectionEndSample = selectedSectionEndSample;
		this.isSelected = isSelected;
		this.markedX = markedX;
		this.isMarked = isMarked;
		adjustScrollBar();
		if (recalculate) {
			recalculate();
		}
		draw();
		repaint();
	}
	
	public void newSamplesAvailable(int numSamples) {
		xMax = numSamples - 1;
		if (scrollbar != null) {
		  //scrollbar.setMinimum(0);
		  scrollbar.setMaximum(audiosource.getBufferSize());
		  //scrollbar.setUnitIncrement(visibleSamples / 20);
		  //scrollbar.setValue((int) xMin); // order of setValue and setVisible
		  //scrollbar.setVisibleAmount(visibleSamples); // important!
		}
	}
	
	public void mouseMoved(Object sender, int sample) {
		Graphics g = imgMouse.getGraphics();
		g.drawImage(img, 0, 0, null);
		if (sample >= 0) {
			drawCursor(g, convertXtoPX(sample));
		}
		repaint();
	}

}
