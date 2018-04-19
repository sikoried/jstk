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
package com.github.sikoried.jstk.vc;

import java.util.ListIterator;
import java.util.Vector;

import com.github.sikoried.jstk.vc.interfaces.VisualizationListener;

/**
 * Each visual component can change the visualization, but is also able to react
 * to visualization changes caused by other visual components. Each visual
 * component can be registered as VisualizationListener at every other visual
 * component, resulting in n * (n-1) connections. However, a central
 * VisualizationListener can be created that has to be set at each visual
 * component (n times) and reacts as a VisualizationListener that forwards all
 * changes to the other visual components.
 * 
 * @author Stefan Steidl
 * 
 */
public class VisualizationInformer implements VisualizationListener {

	private Vector<VisualizationListener> visualizationListeners;
	private String name;

	/**
	 * Creates a VisualizationInformer object with the standard name 'informer'
	 */
	public VisualizationInformer() {
		this("informer");
	}

	/**
	 * Creates a VisualizationInformer object with the given name
	 * 
	 * @param name
	 *            name of the VisualizationInformer object
	 */
	public VisualizationInformer(String name) {
		this.name = name;
		visualizationListeners = new Vector<VisualizationListener>();
	}

	/**
	 * Forwards the visualization changes to all the registered
	 * VisualizationListeners
	 */
	@Override
	public void VisualizationChanged(Object sender, double samplesPerPixel,
			double minSample, int highlightedSectionStartSample,
			int highlightedSectionEndSample, boolean isHighlighted,
			int selectedSectionStartSample, int selectedSectionEndSample,
			boolean isSelected, double markedX, boolean isMarked) {
		ListIterator<VisualizationListener> iterator = visualizationListeners
				.listIterator();
		while (iterator.hasNext()) {
			VisualizationListener listener = iterator.next();
			if (sender != listener) {
				// System.err.println(this + " informs " + listener +
				// " about a visualization change: " + samplesPerPixel + " " +
				// minSample);
				listener.VisualizationChanged(sender, samplesPerPixel,
						minSample, highlightedSectionStartSample,
						highlightedSectionEndSample, isHighlighted,
						selectedSectionStartSample, selectedSectionEndSample,
						isSelected, markedX, isMarked);
			}
		}

	}

	@Override
	public void mouseMoved(Object sender, int sample) {
		ListIterator<VisualizationListener> iterator = visualizationListeners
				.listIterator();
		while (iterator.hasNext()) {
			VisualizationListener listener = iterator.next();
			if (sender != listener) {
				listener.mouseMoved(sender, sample);
			}
		}

	}

	@Override
	public String toString() {
		return "VisualizationInformer '" + name + "'";
	}

	/**
	 * Adds the given VisualizationListener to the list of registered listeners
	 * 
	 * @param listener
	 *            VisualizationListener to be added
	 */
	public void addVisualizationListener(VisualizationListener listener) {
		visualizationListeners.addElement(listener);
	}

}
