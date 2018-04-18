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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ListIterator;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.vc.VisualizerPitchEstimator;
import de.fau.cs.jstk.vc.interfaces.PitchDefinedListener;

public class PitchEstimatorWindow extends JFrame implements
		PitchDefinedListener {

	private static final long serialVersionUID = -8655061993833640560L;

	private VisualizerPitchEstimator pitchEstimator;
	private WindowClosedListener mainWindow;
	private JList f0List;
	private JButton f0Button;
	private DefaultListModel model;
	private Vector<Double> f0Values;
	private JComboBox zoomComboBox;
	private double frequency;
	protected Vector<PitchDefinedListener> pitchDefinedListeners;

	public PitchEstimatorWindow(Preferences pref, WindowClosedListener listener,
			BufferedAudioSource audiosource) {
		super("Pitch estimator", "pitchEstimatorWindow", pref);
		mainWindow = listener;

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				hideWindow();
			}
		});

		Container c = getContentPane();
		int size = preferences.getInt("f0.windowLength");
		int f0Min = preferences.getInt("f0.minimum");
		int f0Max = preferences.getInt("f0.maximum");
		pitchEstimator = new VisualizerPitchEstimator(audiosource, size, f0Min,
				f0Max);
		pitchEstimator.addPitchDefinedListener(this);

		c.setLayout(new BorderLayout());
		c.add(pitchEstimator, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		model = new DefaultListModel();
		f0List = new JList(model);

		JScrollPane scrollpane = new JScrollPane(f0List);
		scrollpane.setPreferredSize(new Dimension(80, 50));
		panel.add(scrollpane, BorderLayout.CENTER);
		f0Button = new JButton("Assign");
		f0Button.setEnabled(false);
		panel.add(f0Button, BorderLayout.SOUTH);
		c.add(panel, BorderLayout.EAST);
		
		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Zoom factor"));
		String[] zoomFactors = {"1", "2", "3", "4", "5"};
		zoomComboBox = new JComboBox(zoomFactors);
		int factor = preferences.getInt("pitchEstimatorWindow.zoom");
		zoomComboBox.setSelectedIndex(factor - 1);
		pitchEstimator.setZoom(factor);
		zoomComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setZoomFactor();
			}
		});
		panel.add(zoomComboBox);
		c.add(panel, BorderLayout.NORTH);

		f0List.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_DELETE) {
					removeF0Values();
				}
			}

			public void keyReleased(KeyEvent arg0) {
			}

			public void keyTyped(KeyEvent arg0) {
			}
		});

		f0Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				informPitchDefinedListeners();
			}

		});

		pitchDefinedListeners = new Vector<PitchDefinedListener>();
		f0Values = new Vector<Double>();

		setVisible(false);
	}

	protected void hideWindow() {
		setVisible(false);
		mainWindow.windowClosed(this);
	}

	public void setBufferedAudioSource(BufferedAudioSource source) {
		pitchEstimator.setBufferedAudioSource(source);
	}

	public void show(int sample) {
		if (sample > 0) {
			model.clear();
			f0Values.clear();
			calculateMean();
			pitchEstimator.showSignal(sample);
		}
	}

	@Override
	public void pitchDefined(double frequency) {
		frequency = (int) (10 * frequency) / 10.0;
		model.addElement(frequency);
		f0Values.add(new Double(frequency));
		f0List.ensureIndexIsVisible(model.getSize() - 1);
		calculateMean();
	}

	private void calculateMean() {
		double sum = 0;
		for (int i = 0; i < f0Values.size(); i++) {
			sum += f0Values.get(i).doubleValue();
		}
		if (sum > 0) {
			sum /= f0Values.size();
			frequency = (int) (sum * 10) / 10.0;
			f0Button.setText("Assign " + frequency + " Hz");
			f0Button.setEnabled(true);
		} else {
			f0Button.setText("Assign");
			f0Button.setEnabled(false);

		}
	}

	private void removeF0Values() {
		for (int i = model.getSize() - 1; i >= 0; i--) {
			if (f0List.isSelectedIndex(i)) {
				model.remove(i);
				f0Values.remove(i);
			}
		}
		calculateMean();
	}

	public void addPitchDefinedListener(PitchDefinedListener listener) {
		pitchDefinedListeners.add(listener);
	}

	private void informPitchDefinedListeners() {
		ListIterator<PitchDefinedListener> iterator = pitchDefinedListeners
				.listIterator();
		while (iterator.hasNext()) {
			PitchDefinedListener listener = iterator.next();
			listener.pitchDefined(frequency);
		}
	}
	
	private void setZoomFactor() {
		int factor = Integer.parseInt(zoomComboBox.getSelectedItem().toString());
		pitchEstimator.setZoom(factor);
		preferences.set("pitchEstimatorWindow.zoom", zoomComboBox.getSelectedItem().toString());
	}

}
