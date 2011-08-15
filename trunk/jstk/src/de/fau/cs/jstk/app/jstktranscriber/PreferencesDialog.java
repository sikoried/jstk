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
package de.fau.cs.jstk.app.jstktranscriber;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class PreferencesDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = 212049934775633310L;

	private JFrame mainFrame;
	private Preferences preferences;
	private JTextField wavDirTextField;
	private JTextField[] f0DirTextFields;
	private int pitchDisplays;

	public PreferencesDialog(JFrame frame, Preferences pref) {
		super(frame, "Preferences", true);
		mainFrame = frame;
		preferences = pref;

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setVisible(false);
			}

			public void windowOpened(WindowEvent e) {
				if (mainFrame != null) {
					Dimension parentSize = mainFrame.getSize();
					Point p = mainFrame.getLocation();
					setLocation(p.x + parentSize.width / 2 - 100, p.y
							+ parentSize.height / 2 - 100);
				}
			}
		});

		pitchDisplays = preferences.getInt("f0.displays");
		f0DirTextFields = new JTextField[pitchDisplays + 1];

		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		Box box = Box.createVerticalBox();
		box.add(Box.createVerticalStrut(10));
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Directory for wave files"));
		box.add(panel);
		box.add(Box.createVerticalStrut(10));
		wavDirTextField = new JTextField(30);
		wavDirTextField.setEditable(false);
		wavDirTextField.setBackground(Color.WHITE);
		wavDirTextField.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				selectDir(wavDirTextField.getText(), wavDirTextField);
			}
		});
		box.add(wavDirTextField);
		box.add(Box.createVerticalStrut(15));

		for (int i = 0; i < pitchDisplays; i++) {
			panel = new JPanel();
			panel.setLayout(new FlowLayout(FlowLayout.LEFT));
			panel.add(new JLabel("F0 directory for algorithm " + (i + 1)));
			box.add(panel);
			box.add(Box.createVerticalStrut(10));
			f0DirTextFields[i] = new JTextField(20);
			f0DirTextFields[i].setEditable(false);
			f0DirTextFields[i].setBackground(Color.WHITE);
			f0DirTextFields[i].addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					JTextField textfield = (JTextField) e.getSource();
					selectDir(textfield.getText(), textfield);
				}
			});
			box.add(f0DirTextFields[i]);
			box.add(Box.createVerticalStrut(10));
		}

		panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Directory for corrected f0 files"));
		box.add(panel);
		box.add(Box.createVerticalStrut(10));
		f0DirTextFields[pitchDisplays] = new JTextField(20);
		f0DirTextFields[pitchDisplays].setEditable(false);
		f0DirTextFields[pitchDisplays].setBackground(Color.WHITE);
		f0DirTextFields[pitchDisplays].addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				selectDir(f0DirTextFields[pitchDisplays].getText(),
						f0DirTextFields[pitchDisplays]);
			}
		});
		box.add(f0DirTextFields[pitchDisplays]);
		box.add(Box.createVerticalStrut(15));
		panel = new JPanel();
		JButton okButton = new JButton("OK");
		okButton.setActionCommand("OK");
		okButton.addActionListener(this);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);
		panel.add(okButton);
		panel.add(Box.createHorizontalStrut(15));
		panel.add(cancelButton);
		box.add(panel);
		c.add(box, BorderLayout.CENTER);

		pack();
		setVisible(false);
	}

	public void showDialog() {
		wavDirTextField.setText(preferences.getString("wavdir"));
		for (int i = 0; i < pitchDisplays; i++) {
			f0DirTextFields[i]
					.setText(preferences.getString("f0dir" + i));
		}
		f0DirTextFields[pitchDisplays].setText(preferences
				.getString("f0dir_corrected"));
		setVisible(true);
	}

	private void selectDir(String dir, JTextField textfield) {
		JFileChooser fileChooser = new JFileChooser(textfield.getText());
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogTitle("Choose a directory");
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			textfield.setText(fileChooser.getSelectedFile().getPath());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("OK")) {
			preferences.set("wavdir", wavDirTextField.getText());
			for (int i = 0; i < pitchDisplays; i++) {
				preferences
						.set("f0dir" + i, f0DirTextFields[i].getText());
			}
			preferences.set("f0dir_corrected",
					f0DirTextFields[pitchDisplays].getText());
		}
		setVisible(false);
	}
}
