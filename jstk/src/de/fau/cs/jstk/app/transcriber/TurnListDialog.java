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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class TurnListDialog extends JDialog implements DocumentListener, KeyListener {

	private static final long serialVersionUID = 590345296049579660L;

	private JTextField textField;
	private JList turnList;
	private DefaultListModel model;
	private TurnSelectedListener mainWindow;

	public TurnListDialog(TurnSelectedListener listener, JFrame frame) {
		super(frame, "Turn list", true);
		mainWindow = listener;

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setVisible(false);
			}
		});

		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		Box box = Box.createVerticalBox();
		box.add(Box.createVerticalStrut(10));
		textField = new JTextField(20);
		textField.addKeyListener(this);
		textField.getDocument().addDocumentListener(this);
		box.add(textField);
		box.add(Box.createVerticalStrut(10));
		c.add(box, BorderLayout.NORTH);
		model = new DefaultListModel();
		turnList = new JList(model);
		turnList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		turnList.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					close();
				}
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}

		});
		turnList.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
					close();
				}
			}
			public void keyReleased(KeyEvent arg0) {
			}
			public void keyTyped(KeyEvent arg0) {
			}
		});
		turnList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
			}

		});
		JScrollPane scrollpane = new JScrollPane(turnList);
		c.add(scrollpane, BorderLayout.CENTER);

		setSize(200, 300);
		setLocation(650, 100);
		setVisible(false);
	}

	public void fillList(ArrayList<String> list) {
		model.clear();
		Iterator<String> iterator = list.iterator();
		while (iterator.hasNext()) {
			String s = iterator.next();
			model.addElement(s);
		}
	}
	
	public void show(int index) {
		turnList.setSelectedIndex(index);
		turnList.ensureIndexIsVisible(index);
		textField.requestFocusInWindow();
		setVisible(true);
	}

	public void close() {
		setVisible(false);
		mainWindow.turnSelected(turnList.getSelectedIndex());
	}
	
	private void searchForTurn() {
		String search = textField.getText();
		
		int count = model.size();
		int i = 0;
		String turn = (String) model.elementAt(i);
		while ((i < count-1) && (search.compareTo(turn) > 0)) {
			i++;
			turn = (String) model.elementAt(i);
		}
		turnList.setSelectedIndex(i);
		turnList.ensureIndexIsVisible(i);		
	}
	
	@Override
	public void changedUpdate(DocumentEvent arg0) {
	}

	@Override
	public void insertUpdate(DocumentEvent arg0) {
		searchForTurn();	
	}

	@Override
	public void removeUpdate(DocumentEvent arg0) {
		searchForTurn();	
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		switch (arg0.getKeyCode()) {
		case KeyEvent.VK_ENTER:
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_UP:
		case KeyEvent.VK_PAGE_DOWN:
		case KeyEvent.VK_PAGE_UP:
		case KeyEvent.VK_KP_DOWN:
		case KeyEvent.VK_KP_UP:
		case KeyEvent.VK_HOME:
		case KeyEvent.VK_END:
			java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(turnList, arg0);
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
	}

}
