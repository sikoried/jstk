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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.fau.cs.jstk.vc.VisualizerSpectrogram;

public class SpectrogramControlWindow extends JFrame implements
		ContrastBrightnessListener {

	private static final long serialVersionUID = 7203314678810998437L;

	private JComboBox windowTypeComboBox;
	private JComboBox windowLengthComboBox;
	private JComboBox colorSpectrogramComboBox;
	private ContrastBrightnessControl contrastBrightnessControl;
	private WindowClosedListener mainWindow;
	private VisualizerSpectrogram spectrogramVisualizer;

	public SpectrogramControlWindow(Preferences preferences,
			WindowClosedListener listener,
			VisualizerSpectrogram spectrogramVisualizer) {
		super("Spectrogram control window", "spectrogramControlWindow",
				preferences);
		mainWindow = listener;
		this.spectrogramVisualizer = spectrogramVisualizer;

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				hideWindow();
			}
		});

		Container c = getContentPane();
		c.setLayout(new BorderLayout());

		Box box = Box.createVerticalBox();
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Window type"));
		String[] windowTypes = { "Hamming", "Hann", "Rectangle" };
		windowTypeComboBox = new JComboBox(windowTypes);
		int window = preferences.getInt("spectrogram.windowType");
		windowTypeComboBox.setSelectedIndex(window - 1);
		spectrogramVisualizer.setWindowFunction(window);
		windowTypeComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setWindowType();
			}
		});
		panel.add(windowTypeComboBox);
		box.add(panel);

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Window length"));
		String[] windowLengths = { "2", "4", "8", "16", "32", "64" };
		double length = preferences.getDouble("spectrogram.windowLength");
		windowLengthComboBox = new JComboBox(windowLengths);
		windowLengthComboBox.setSelectedItem(String.valueOf((int) length));
		spectrogramVisualizer.setWindowLength(length);
		windowLengthComboBox.setEditable(true);
		windowLengthComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setWindowLength();
			}
		});
		panel.add(windowLengthComboBox);
		box.add(panel);

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Color spectrogram"));
		String[] colors = { "color", "gray", };
		colorSpectrogramComboBox = new JComboBox(colors);
		boolean color = preferences.getBoolean("spectrogram.color");
		if (color) {
			colorSpectrogramComboBox.setSelectedIndex(0);
			spectrogramVisualizer.setColorSpectrogram(true);
		} else {
			colorSpectrogramComboBox.setSelectedIndex(1);			
			spectrogramVisualizer.setColorSpectrogram(false);
		}
		colorSpectrogramComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setColor();
			}
		});
		panel.add(colorSpectrogramComboBox);
		box.add(panel);

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Brightness/contrast"));
		box.add(panel);

		panel = new JPanel();
		double gamma = preferences.getDouble("spectrogram.gamma");
		double brightness = preferences.getDouble("spectrogram.brightness");
		spectrogramVisualizer.setProperties(gamma, brightness);
		contrastBrightnessControl = new ContrastBrightnessControl(brightness, gamma);
		contrastBrightnessControl.addContrastBrightnessListener(this);
		panel.add(contrastBrightnessControl);
		box.add(panel);

		c.add(box, BorderLayout.NORTH);

		setAlwaysOnTop(true);
		
	}

	private void hideWindow() {
		setVisible(false);
		mainWindow.windowClosed(this);
	}

	private void setWindowType() {
		int window = windowTypeComboBox.getSelectedIndex() + 1;
		spectrogramVisualizer.setWindowFunction(window);
		preferences.set("spectrogram.windowType", String.valueOf(window));
	}

	private void setWindowLength() {
		double length = Double.parseDouble(windowLengthComboBox.getSelectedItem().toString());
		spectrogramVisualizer.setWindowLength(length);
		preferences.set("spectrogram.windowLength", String.valueOf(length));
	}

	private void setColor() {
		if (colorSpectrogramComboBox.getSelectedIndex() == 0) {
			spectrogramVisualizer.setColorSpectrogram(true);
			preferences.set("spectrogram.color", "true");
		} else {
			spectrogramVisualizer.setColorSpectrogram(false);
			preferences.set("spectrogram.color", "false");
		}
	}

	@Override
	public void contrastBrightnessChanged(double contrast, double brightness) {
		spectrogramVisualizer.setProperties(contrast, brightness);
		preferences.set("spectrogram.gamma", String.valueOf(contrast));
		preferences.set("spectrogram.brightness", String.valueOf(brightness));
	}

}
