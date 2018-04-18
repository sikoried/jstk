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
package de.fau.cs.jstk.app.transcriberOld;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JOptionPane;

import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.vc.VisualizerAutocorrelation;
import de.fau.cs.jstk.vc.interfaces.MouseMotionVisualizationListener;

public class AutocorrelationWindow extends SpectralWindow {

	private static final long serialVersionUID = -4656635691918338714L;

	private VisualizerAutocorrelation acVisualizer;

	public AutocorrelationWindow(Preferences preferences, WindowClosedListener listener, BufferedAudioSource source) {
		super("Autocorrelation function", "acWindow", preferences, listener);

		Container c = getContentPane();
		acVisualizer = new VisualizerAutocorrelation(source, windowLength, blockSize,
				windowType);
		c.add(acVisualizer, BorderLayout.CENTER);
		
		acVisualizer
				.addMouseMotionVisualizationListener(new MouseMotionVisualizationListener() {
					public void mouseMoved(double x, double y) {
						setPosition(x, y);
					}
				});

	}

	public void show(int sample) {
		this.sample = sample;
		if (sample > 0) {
			acVisualizer.showAutocorrelation(sample);
		}
	}
	
	private void setPosition(double tau, double value) {
		double frequency = 1000.0 / tau;
		frequency = (int) (frequency * 10) / 10.0;
		tau = (int) (tau * 10) / 10.0;
		value = (int) (value * 10) / 10.0;
		positionLabel.setText(tau + " ms (" + frequency + " Hz), " + value);
	}

	public void setBufferedAudioSource(BufferedAudioSource source) {
		super.setBufferedAudioSource(source);
		acVisualizer.setBufferedAudioSource(source);
	}

	@Override
	public void updateProperties() {
		windowType = windowTypeComboBox.getSelectedIndex() + 1;
		try {
			windowLength = Integer.parseInt(windowLengthComboBox
					.getSelectedItem().toString());
			insertIntoComboBox(windowLengthComboBox, windowLength);
			acVisualizer.setParameters(windowLength, blockSize,
					windowType);
			show(sample);
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this,
					"Please enter a positive integer number!", "Error",
					JOptionPane.ERROR_MESSAGE);
			windowLengthComboBox.setSelectedIndex(2);
			windowLength = 32;
		}		
	}
}
