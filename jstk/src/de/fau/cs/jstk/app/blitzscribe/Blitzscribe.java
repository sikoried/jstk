/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		
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

package de.fau.cs.jstk.app.blitzscribe;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * A simplistic tool for very fast (utterance level) transcription of large 
 * speech data; inspired by Blitzscribe [1].
 * 
 * The .trl to read/write contains lines as "turn-file.wav [transcription"; the
 * file names must not contain whitespace as these are used to split the string.
 * 
 * [1] B. Roy and D. Roy (2009). Fast transcription of unstructured audio 
 *     recordings. Proc. Annual Conference of the Int'l Speech Communication 
 *     Association (INTERSPEECH)
 *  
 * @author sikoried
 *
 */
public class Blitzscribe extends JFrame implements WindowListener {
	private static final long serialVersionUID = 1L;

	private JButton btnOpen = new JButton("Open");
	private JButton btnSave = new JButton("Save");
	private JButton btnSaveAs = new JButton("Save as...");
	
	private JTextField tfTranscription = new JTextField();
	
	private JList liFileList = new JList();
	private DefaultListModel listModel = new DefaultListModel();
	
	private BufferedWriter logw = null;
	
	private AudioPanel ap = new AudioPanel();
	
	private JFileChooser fc = new JFileChooser();
	
	public Blitzscribe() {
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		fc.setFileFilter(new FileNameExtensionFilter("transcription files", "trl"));
		
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println("Could not set look and feel: " + e.toString());
			setDefaultLookAndFeelDecorated(true);
		}
		
		// add the listeners
		addWindowListener(this);
		
		// layout
		initUI();
		
		liFileList.setModel(listModel);
		liFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		loadTrl(new File("/net/speechdata/LMELectures/segmented/20090427-Hornegger-PA01/20090427-Hornegger-PA01.trl"));
	}
	
	private void initUI() {
		setTitle("Blitzscribe");
		JPanel root = new JPanel();
		GridBagConstraints c = new GridBagConstraints();
		root.setLayout(new GridBagLayout());
		
		// set up text field
		tfTranscription.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) { }
			public void keyReleased(KeyEvent e) { }
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					next();
					if (!e.isShiftDown())
						play();
				} else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && e.isShiftDown()) {
					prev();
					e.consume();
				} else if (e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown()) {
					play();
					e.consume();
				}
			}
		});
		
		// set up list box action
		liFileList.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) { }
			public void mousePressed(MouseEvent e) { }
			public void mouseExited(MouseEvent e) { }
			public void mouseEntered(MouseEvent e) { }
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() > 1) {
					int pos = liFileList.getSelectedIndex();
					if (pos >= 0) {
						save();
						display(pos);
					}
				} else
					tfTranscription.requestFocus();
			}
		});
		
		// button actions
		btnOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// we only need this once
				int r = fc.showOpenDialog(Blitzscribe.this);
				if (r == JFileChooser.APPROVE_OPTION)
					loadTrl(fc.getSelectedFile());
				tfTranscription.requestFocus();
			}
		});
		
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (curFile != null)
					saveTrl(Blitzscribe.this.curFile);
				tfTranscription.requestFocus();
			}
		});
		
		btnSaveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int r = fc.showOpenDialog(Blitzscribe.this);
				if (r == JFileChooser.APPROVE_OPTION)
					saveTrl(fc.getSelectedFile());
				tfTranscription.requestFocus();
			}
		});
		
		// button short cuts
		btnOpen.setMnemonic(KeyEvent.VK_O);
		btnSave.setMnemonic(KeyEvent.VK_S);
		
		// buttons
		c.gridx = 0; c.gridy = 0; root.add(btnOpen, c);
		c.gridx = 1; c.gridy = 0; root.add(btnSave, c);
		c.gridx = 2; c.gridy = 0; root.add(btnSaveAs, c);
		double old = c.weightx;
		c.gridx = 3; c.gridy = 0; c.weightx = 1.0; c.fill = GridBagConstraints.HORIZONTAL; root.add(new JPanel(), c);
		c.weightx = old;
		
		
		// audio panel
		c.gridx = 0; c.gridy = 1; c.gridwidth = 4; root.add(ap, c);
		
		// transcription panel
		c.weightx = 1.0; 
		c.gridx = 0; c.gridy = 2; c.gridwidth = 4; root.add(tfTranscription, c);
		
		// the available list
		JScrollPane sp = new JScrollPane(liFileList);
		c.weightx = 1.0; c.weighty = 1.0;
		c.gridx = 0; c.gridy = 3; c.gridwidth = 4; c.fill = GridBagConstraints.BOTH; root.add(sp, c);
		
		// display
		setSize(640, 480);
		pack();
		setContentPane(root);
	}

	private int curIndex = -1;
	private Turn curData = null;
	
	private void play() {
		ap.toggle();
	}
	
	/**
	 * Go to the next turn, if possible
	 */
	private void next() {
		save();
		if (! (curIndex < listModel.getSize() - 1)) {
			System.err.println("Already at end of list!");
			return;
		}
		display(++curIndex);
	}
	
	/**
	 * Go to the previous turn, if possible
	 */
	private void prev() {
		save();
		if (curIndex < 1) {
			System.err.println("Already at beginning of list!");
			return;
		}
		display(--curIndex);
	}
	
	/**
	 * If there was a change to the transcription, update the variables and
	 * append the change to the protocol.
	 */
	private void save() {
		if (curIndex < 0 || curData == null)
			return;
		
		String text = tfTranscription.getText().trim();
			
		if (!text.equals(curData.text)) {
			curData.text = text;
			if (logw != null) {
				try {
					logw.append(System.currentTimeMillis() + " " + curData.toString() + "\n");
					logw.flush();
				} catch (IOException e) {
					System.err.println("Error writing protocol: " + e.toString());
				}
			}
		}
	}
	
	/**
	 * Display the selected turn (list index)
	 * @param pos
	 */
	private void display(int pos) {
		// make sure the player stops
		ap.stop();
		
		System.err.println("Loading item " + pos);
		
		curIndex = pos;
		curData = (Turn) listModel.getElementAt(pos);
		
		liFileList.setSelectedIndex(curIndex);
		liFileList.ensureIndexIsVisible(curIndex);
		tfTranscription.requestFocus();
		tfTranscription.setText(curData.text);
		tfTranscription.setCaretPosition(curData.text.length());
		
		try {
			// update audio panel
			ap.setAudioFile(new File(curDir + curData.file));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "IO exception",
					JOptionPane.ERROR_MESSAGE);
		} catch (UnsupportedAudioFileException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Unsupported audio format",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private String curDir = "";
	private File curFile = null;
	/**
	 * Load a transcription file and select the first non-empty entry.
	 * @param file
	 */
	public void loadTrl(File file) {
		try {
			// clear list model
			listModel.clear();
			
			// read in new data
			BufferedReader br = new BufferedReader(new FileReader(file));
			int i = 0, j = 0;
			String l;
			while ((l = br.readLine()) != null) {
				Turn t = new Turn (l);
				listModel.addElement(t);
				i++;
				if (t.text.length() > 0)
					j++;
			}
			
			if (i == 0)
				throw new IOException("Could not import any turns, check file!");
			
			System.err.println("Imported " + i + " turns (" + j + " prefilled).");
			
			// update dir, if necessary
			curDir = (file.getParent() == null ? "" : file.getParent() + System.getProperty("file.separator"));
			curFile = file;
			
			setTitle("Blitzscribe: " + file.getName());
			
			// go forward to the first empty transcription
			int p = 0;
			while (((Turn) listModel.get(p)).text.length() > 0)
				p++;
			
			display(p);
			
			// set up protocol file (append for existing files
			if (logw != null)
				logw.close();
			logw = new BufferedWriter(new FileWriter(new File(file.getAbsoluteFile() + "~"), true));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.toString(), "An exception ocurred while loading turn file!", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Write the current state of the transcription file.
	 * @param file
	 */
	public void saveTrl(File file) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			curFile = file;
			for (int i = 0; i < listModel.getSize(); ++i)
				bw.append(((Turn) listModel.get(i)).toFinalString() + "\n");
			bw.close();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.toString(), "An exception ocurred while writing turn file!", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void windowClosing(WindowEvent arg0) {
		// prevent an accidental close of the program
		if (JOptionPane.showConfirmDialog(this, "Are you sure? Did you save your transcription?", "Alert", JOptionPane.YES_NO_OPTION)
				== JOptionPane.YES_OPTION)
			System.exit(0);
	}
	
	// un-used event listeners
	public void windowClosed(WindowEvent arg0) { }
	public void windowActivated(WindowEvent arg0) { }
	public void windowDeactivated(WindowEvent arg0) { }
	public void windowDeiconified(WindowEvent arg0) { }
	public void windowIconified(WindowEvent arg0) { }
	public void windowOpened(WindowEvent arg0) { }

	public static void main(String [] args) {
		Blitzscribe bs = new Blitzscribe();
		bs.setVisible(true);
	}
}
