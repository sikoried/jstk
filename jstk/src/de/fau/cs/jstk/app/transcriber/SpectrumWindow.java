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
package de.fau.cs.jstk.app.transcriber;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JOptionPane;

import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.vc.VisualizerSpectrum;
import de.fau.cs.jstk.vc.interfaces.MouseMotionVisualizationListener;

public class SpectrumWindow extends SpectralWindow {

	private static final long serialVersionUID = -4088428146926500523L;

	private VisualizerSpectrum spectrumVisualizer;

	public SpectrumWindow(Preferences preferences, WindowClosedListener listener,
			BufferedAudioSource source) {
		super("Spectrum", "spectrumWindow", preferences, listener);

		Container c = getContentPane();
		spectrumVisualizer = new VisualizerSpectrum(source, windowLength,
				blockSize, windowType);
		c.add(spectrumVisualizer, BorderLayout.CENTER);


		spectrumVisualizer
				.addMouseMotionVisualizationListener(new MouseMotionVisualizationListener() {
					public void mouseMoved(double x, double y) {
						setPosition(x, y);
					}
				});
	}

	public void show(int sample) {
		this.sample = sample;
		if (sample > 0) {
			spectrumVisualizer.showSpectrum(sample);
		}
	}

	private void setPosition(double frequency, double value) {
		frequency = (int) (frequency * 10)/ 10.0;
		value = (int) (value * 10) / 10.0;
		positionLabel.setText(frequency + " Hz, " + value + " dB");
	}

	public void setBufferedAudioSource(BufferedAudioSource source) {
		super.setBufferedAudioSource(source);
		spectrumVisualizer.setBufferedAudioSource(source);
	}

	@Override
	public void updateProperties() {
		windowType = windowTypeComboBox.getSelectedIndex() + 1;
		try {
			windowLength = Integer.parseInt(windowLengthComboBox
					.getSelectedItem().toString());
			insertIntoComboBox(windowLengthComboBox, windowLength);
			spectrumVisualizer.setParameters(windowLength, blockSize,
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
