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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.fau.cs.jstk.framed.Window;
import de.fau.cs.jstk.io.BufferedAudioSource;

public abstract class SpectralWindow extends JFrame {

	private static final long serialVersionUID = 464650558977376646L;

	protected JComboBox windowTypeComboBox;
	protected JComboBox windowLengthComboBox;
	protected JLabel positionLabel;
	protected WindowClosedListener mainWindow;

	protected int windowType = Window.HAMMING_WINDOW;
	protected int windowLength = 32; // [ms]
	protected int blockSize = 128; // samples
	protected int sample = -1;

	protected SpectralWindow(String caption, String name,
			Preferences preferences, WindowClosedListener listener) {
		super(caption, name, preferences);
		this.mainWindow = listener;

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				hideWindow();
			}
		});

		Container c = getContentPane();
		c.setLayout(new BorderLayout());

		JPanel panelTop = new JPanel();
		panelTop.setLayout(new FlowLayout(FlowLayout.LEFT));
		String[] windowTypes = { "Hamming", "Hann", "Rectangle" };
		windowTypeComboBox = new JComboBox(windowTypes);
		windowTypeComboBox.setSelectedIndex(0);
		windowTypeComboBox.setEnabled(false);
		windowTypeComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateProperties();
			}
		});
		panelTop.add(windowTypeComboBox);
		String[] windowLengths = { "8", "16", "32", "64" };
		windowLengthComboBox = new JComboBox(windowLengths);
		windowLengthComboBox.setSelectedIndex(2);
		windowLengthComboBox.setEditable(true);
		windowLengthComboBox.setEnabled(false);
		windowLengthComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateProperties();
			}
		});
		panelTop.add(windowLengthComboBox);
		c.add(panelTop, BorderLayout.NORTH);

		JPanel panelBottom = new JPanel();
		panelBottom.setLayout(new FlowLayout(FlowLayout.LEFT));
		positionLabel = new JLabel(" ");
		panelBottom.add(positionLabel);
		c.add(panelBottom, BorderLayout.SOUTH);

		setVisible(false);
	}

	protected void hideWindow() {
		setVisible(false);
		mainWindow.windowClosed(this);
	}

	public abstract void updateProperties();

	protected void insertIntoComboBox(JComboBox box, int value) {
		int i = 0;
		for (; i < box.getItemCount(); i++) {
			int v = Integer.parseInt(box.getItemAt(i).toString());
			if (v == value) {
				return;
			}
			if (v > value) {
				break;
			}
		}
		box.insertItemAt(String.valueOf(value), i);
	}

	public void setBufferedAudioSource(BufferedAudioSource source) {
		boolean enabled = (source == null) ? false : true;
		windowTypeComboBox.setEnabled(enabled);
		windowLengthComboBox.setEnabled(enabled);
	}

}
