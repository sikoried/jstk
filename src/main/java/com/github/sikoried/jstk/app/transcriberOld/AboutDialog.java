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
package com.github.sikoried.jstk.app.transcriberOld;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class AboutDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = -9169231100633702785L;
	private static SimpleAttributeSet ITALIC_GRAY = new SimpleAttributeSet();
	private static SimpleAttributeSet BOLD_BLACK = new SimpleAttributeSet();
	private static SimpleAttributeSet BLACK = new SimpleAttributeSet();
	private static String newLine = System.getProperty("line.separator");

	private JFrame mainFrame;
	private JTextPane textPane;

	static {
		StyleConstants.setForeground(ITALIC_GRAY, Color.gray);
		StyleConstants.setItalic(ITALIC_GRAY, true);
		StyleConstants.setFontFamily(ITALIC_GRAY, "Helvetica");
		StyleConstants.setFontSize(ITALIC_GRAY, 14);
		StyleConstants.setAlignment(ITALIC_GRAY, StyleConstants.ALIGN_CENTER);

		StyleConstants.setForeground(BOLD_BLACK, Color.black);
		StyleConstants.setBold(BOLD_BLACK, true);
		StyleConstants.setFontFamily(BOLD_BLACK, "Helvetica");
		StyleConstants.setFontSize(BOLD_BLACK, 14);
		StyleConstants.setAlignment(BOLD_BLACK, StyleConstants.ALIGN_CENTER);

		StyleConstants.setForeground(BLACK, Color.black);
		StyleConstants.setFontFamily(BLACK, "Helvetica");
		StyleConstants.setFontSize(BLACK, 14);
	}

	public AboutDialog(JFrame parent) {
		super(parent, "About...", true);
		mainFrame = parent;

		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				if (mainFrame != null) {
					Dimension parentSize = mainFrame.getSize();
					Point p = mainFrame.getLocation();
					setLocation(p.x + parentSize.width / 2 - 100, p.y
							+ parentSize.height / 2 - 100);
				}
			}
		});

		Container c = getContentPane();
		textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setBackground(Color.WHITE);

		insertText(newLine + "JSTK Transcriber, version 1.0 beta" + newLine, BOLD_BLACK);
		insertText("Copyright 2011" + newLine, BOLD_BLACK);
		insertText("Stefan Steidl", ITALIC_GRAY);
		insertText("Korbinian Riedhammer", ITALIC_GRAY);
		insertText("Tobias Bocklet", ITALIC_GRAY);
		insertText("Florian Hönig" + newLine, ITALIC_GRAY);
		insertText(
				"This software is part of the Java Speech Toolkit (JSTK), "
						+ "which is developed and maintained by the Speech Group at the University of Erlangen-Nuremberg."
						+ "The JSTK is available at" + newLine, BLACK);
		insertText("http://code.google.com/p/jstk/" + newLine, ITALIC_GRAY);
		insertText(
				"The JSTK is licensed under the GNU General Public License, version 3. "
						+ "Please see the file LICENSE for more details, or visit"
						+ newLine, BLACK);
		insertText("http://www.gnu.org/" + newLine, ITALIC_GRAY);

		JScrollPane scrollpane = new JScrollPane(textPane);
		c.add(scrollpane);

		JPanel panel2 = new JPanel();
		JButton button = new JButton("OK");
		panel2.add(button);
		button.addActionListener(this);
		c.add(panel2, BorderLayout.SOUTH);

		setSize(400, 400);
		setVisible(false);
	}

	public void actionPerformed(ActionEvent e) {
		setVisible(false);
	}

	protected void insertText(String text, AttributeSet set) {
		try {
			Document doc = textPane.getDocument();
			int oldLength = doc.getLength();
			doc.insertString(oldLength, text + newLine, set);
			((StyledDocument) doc).setParagraphAttributes(oldLength,
					doc.getLength() - oldLength, set, false);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
}
