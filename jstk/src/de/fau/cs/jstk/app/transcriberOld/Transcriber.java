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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JTextField;

import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.io.BufferedFrameSource;
import de.fau.cs.jstk.sampled.AudioFileReader;
import de.fau.cs.jstk.sampled.AudioPlay;
import de.fau.cs.jstk.sampled.filters.BandPassFilter;
import de.fau.cs.jstk.vc.F0Point;
import de.fau.cs.jstk.vc.FileVisualizer;
import de.fau.cs.jstk.vc.FrameFileReader;
import de.fau.cs.jstk.vc.FrameFileWriter;
import de.fau.cs.jstk.vc.VisualComponent;
import de.fau.cs.jstk.vc.VisualizationInformer;
import de.fau.cs.jstk.vc.VisualizerPitch;
import de.fau.cs.jstk.vc.VisualizerPower;
import de.fau.cs.jstk.vc.VisualizerSpectrogram;
import de.fau.cs.jstk.vc.VisualizerSpeechSignal;
import de.fau.cs.jstk.vc.interfaces.F0PointsSelectedListener;
import de.fau.cs.jstk.vc.interfaces.PitchDefinedListener;
import de.fau.cs.jstk.vc.interfaces.SampleSelectedListener;
import de.fau.cs.jstk.vc.interfaces.WordHighlightedListener;
import de.fau.cs.jstk.vc.transcription.Transcription;
import de.fau.cs.jstk.vc.transcription.TranscriptionList;
import de.fau.cs.jstk.vc.transcription.VisualizerTranscription;


public class Transcriber extends JFrame implements KeyListener, ActionListener,
		WindowClosedListener, TurnSelectedListener, PitchDefinedListener,
		SampleSelectedListener {

	private static final long serialVersionUID = -9038493231330330945L;

	private AudioFileReader reader;
	private BufferedAudioSource source;
	private BufferedFrameSource pitchsource;
	private VisualizerSpeechSignal audioSignalVisualizer;
	private VisualizerPower powerVisualizer;
	private VisualizerSpectrogram spectrogramVisualizer;
	private VisualizerTranscription transcriptionVisualizer;
	private VisualizerPitch pitchVisualizer;
	private TranscriptionList list;
	private String transcriptionFile;
	private JLabel fileLabel;
	private JButton playButton;
	private JTextField wordEdit = new JTextField(20);
	private SpectrogramControlWindow spectrogramControlWindow;
	private SpectrumWindow spectrumWindow;
	private AutocorrelationWindow acWindow;
	private PitchEstimatorWindow pitchEstimatorWindow;
	private PreferencesDialog preferencesDialog;
	private TurnListDialog turnListDialog;
	private AboutDialog aboutDialog;

	private JMenuItem loadTranscriptionItem;
	private JMenuItem saveAsTranscriptionItem;
	private JMenuItem saveTranscriptionItem;
	private JMenuItem closeItem;
	private JMenuItem preferencesItem;
	private JMenuItem exitItem;
	private JMenuItem goToTurnItem;
	private JMenuItem goToFirstTurnItem;
	private JMenuItem goToNextTurnItem;
	private JMenuItem goToPrevTurnItem;
	private JMenuItem goToNextWordItem;
	private JMenuItem goToPrevWordItem;
	private JMenuItem playWordItem;
	private JMenuItem playSelectedRegionItem;
	private JMenuItem playTurnItem;
	private JMenuItem editWordItem;
	private JMenuItem deleteWordItem;
	private JMenuItem insertWordItem;
	private JMenuItem moveRightWordBoundaryRightItem;
	private JMenuItem moveRightWordBoundaryLeftItem;
	private JMenuItem moveLeftWordBoundaryRightItem;
	private JMenuItem moveLeftWordBoundaryLeftItem;
	private JMenuItem connectToNextWordItem;
	private JMenuItem connectToPrevWordItem;
	private JMenuItem disconnectFromNextWordItem;
	private JMenuItem disconnectFromPrevWordItem;
	private JMenuItem f0SetToOriginalItem;
	private JMenuItem f0SetToZeroItem;
	private JMenuItem f0SetToDoubleItem; 
	private JMenuItem f0SetTohalfItem; 
	private JMenuItem f0SplineInterpolationItem; 
	private JCheckBoxMenuItem viewSpectrogramControlItem;
	private JCheckBoxMenuItem viewSpectrumItem;
	private JCheckBoxMenuItem viewAutocorrelationItem;
	private JCheckBoxMenuItem viewPitchEstimatorItem;
	private JCheckBoxMenuItem zoomItem;
	private JMenuItem aboutItem;

	private Preferences preferences;

	public Transcriber(String title) throws Exception {
		super(title, "mainWindow");

		preferences = new TranscriberPreferences("transcriber.ini");
		setPreferences(preferences);
		transcriptionFile = preferences.getString("transcriptionFile");

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});

		Container c = getContentPane();
		c.setLayout(new BorderLayout());

		Box box = Box.createVerticalBox();

		JPanel panel = new JPanel();
		playButton = new JButton("", new ImageIcon(this.getClass().getResource("/app/transcriber/play1.png")));
		playButton.setRolloverIcon(new ImageIcon(this.getClass().getResource("/app/transcriber/play2.png")));
		playButton.setDisabledIcon(new ImageIcon(this.getClass().getResource("/app/transcriber/play3.png")));
		playButton.setRolloverEnabled(true);
		playButton.setContentAreaFilled(false);
		playButton.setBorderPainted(false);
		playButton.setMargin(new Insets(0, 0, 0, 0));;
		playButton.setEnabled(false);
		playButton.setMnemonic('p');
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				play();
			}
		});
		playButton.setFocusable(false);
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(playButton);
		box.add(panel);
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT));
		fileLabel = new JLabel(" ");
		p.add(fileLabel);
		box.add(p);
		box.add(Box.createVerticalStrut(10));

		// reader = new AudioFileReader("radiorecord_news.wav", false);
		// source = new BufferedAudioSource(reader);

		audioSignalVisualizer = new VisualizerSpeechSignal("audioSignalVis",
				source);
		audioSignalVisualizer.setPreferredSize(new Dimension(640, 100));
		box.add(audioSignalVisualizer);
		box.add(Box.createVerticalStrut(10));
		
		powerVisualizer = new VisualizerPower("powerVis", source);
		powerVisualizer.setPreferredSize(new Dimension(640, 100));
		box.add(powerVisualizer);
		box.add(Box.createVerticalStrut(10));

		spectrogramVisualizer = new VisualizerSpectrogram("spectrogramVis",
				source);
		spectrogramVisualizer.setPreferredSize(new Dimension(640, 158));
		box.add(spectrogramVisualizer);
		box.add(Box.createVerticalStrut(10));

		int shift = preferences.getInt("f0.shift");
		pitchVisualizer = new VisualizerPitch("pitchVis", source, null, null,
				shift);
		pitchVisualizer.setPreferredSize(new Dimension(640, 150));
		pitchVisualizer.setComponentPopupMenu(createPopupMenu());
		pitchVisualizer.f0min = preferences.getInt("f0.minimum");
		pitchVisualizer.f0max = preferences.getInt("f0.maximum");
		box.add(pitchVisualizer);
		box.add(Box.createVerticalStrut(10));

		transcriptionVisualizer = new VisualizerTranscription(
				"transcriptionVis", source);
		transcriptionVisualizer.setPreferredSize(new Dimension(640, 50));
		box.add(transcriptionVisualizer);
		JPanel panel2 = new JPanel();
		panel2.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel2.add(wordEdit);
		box.add(panel2);
		box.add(Box.createVerticalStrut(10));
		wordEdit.setEnabled(false);
		wordEdit.setBackground(audioSignalVisualizer.colorBackgroundDisabled);
		wordEdit.addActionListener(this);
		wordEdit.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {
			}

			public void keyReleased(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE) {
					wordEdit.setText(transcriptionVisualizer.getWord());
					requestFocusInWindow();
				}
			}

			public void keyTyped(KeyEvent arg0) {
				if (arg0.getKeyChar() == ' ') {
					arg0.consume();
				}
			}
		});

		c.add(box, BorderLayout.NORTH);

		// Create scrollbar and connect it to one of the visual components
		JScrollBar scrollbar = new JScrollBar(JScrollBar.HORIZONTAL);
		scrollbar.setEnabled(false);
		audioSignalVisualizer.setScrollbar(scrollbar);

		// Add each visual component as VisualizationListener to every other
		// visual component:
		// n * (n-1) connections
		// audioSignalVisualizer.addVisualization
		// Listener(spectrogramVisualizer);
		// audioSignalVisualizer.addVisualizationListener(transcriptionVisualizer);
		// spectrogramVisualizer.addVisualizationListener(audioSignalVisualizer);
		// spectrogramVisualizer.addVisualizationListener(transcriptionVisualizer);
		// transcriptionVisualizer.addVisualizationListener(audioSignalVisualizer);
		// transcriptionVisualizer.addVisualizationListener(spectrogramVisualizer);

		// Or create one VisualizationInformer and only set the informer for all
		// visual components:
		VisualizationInformer informer = new VisualizationInformer();
		audioSignalVisualizer.setVisualizationInformer(informer);
		powerVisualizer.setVisualizationInformer(informer);
		spectrogramVisualizer.setVisualizationInformer(informer);
		pitchVisualizer.setVisualizationInformer(informer);
		transcriptionVisualizer.setVisualizationInformer(informer);
		// myVisualizer.setVisualizationInformer(informer);

		// set the desired mode: NORMAL_MODE, VALUE_MODE, SELECTION_MODE, or
		// ZOOM_MODE
		audioSignalVisualizer.switchMode(FileVisualizer.SELECTION_MODE);
		audioSignalVisualizer.showCursorY = false;
		powerVisualizer.switchMode(FileVisualizer.SELECTION_MODE);
		powerVisualizer.showCursorY = false;
		spectrogramVisualizer.switchMode(FileVisualizer.SELECTION_MODE);
		c.add(scrollbar, BorderLayout.SOUTH);

		spectrogramControlWindow = new SpectrogramControlWindow(preferences, this, spectrogramVisualizer);
		spectrumWindow = new SpectrumWindow(preferences, this, source);
		acWindow = new AutocorrelationWindow(preferences, this, source);
		pitchEstimatorWindow = new PitchEstimatorWindow(preferences, this,
				source);
		pitchEstimatorWindow.addPitchDefinedListener(this);
		preferencesDialog = new PreferencesDialog(this, preferences);
		turnListDialog = new TurnListDialog(this, this);
		aboutDialog = new AboutDialog(this);

		audioSignalVisualizer.addSampleSelectedListener(this);
		powerVisualizer.addSampleSelectedListener(this);
		spectrogramVisualizer.addSampleSelectedListener(this);
		pitchVisualizer.addSampleSelectedListener(new SampleSelectedListener() {
			public void sampleSelected(int sample) {
				spectrumWindow.show(sample);
				acWindow.show(sample);
				pitchEstimatorWindow.show(sample);
			}
		});

		transcriptionVisualizer
				.addWordHighlightedWordListener(new WordHighlightedListener() {
					public void wordHighlighted() {
						if (transcriptionVisualizer.isHighlighted) {
							wordEdit.setText(transcriptionVisualizer.getWord());
						} else {
							wordEdit.setText("");
						}
					}
				});
		
		pitchVisualizer.addF0PointsSelectedListener(new F0PointsSelectedListener() {
			public void f0PointsSelected(VisualizerPitch sender, F0Point[] points) {
				int n = pitchVisualizer.getNumberOfSelectedFrames();
				modifyPopUpMenu(n);
			}
		});

		addKeyListener(this);
		this.setJMenuBar(createMenuBar());
		enableMenuItems(false);

		setFocusable(true);
		// pack();
		setVisible(true);
	}

	private JMenuItem newMenuItem(JMenu menu, String item) {
		JMenuItem menuItem = new JMenuItem(item);
		menuItem.addActionListener(this);
		menu.add(menuItem);
		return menuItem;
	}

	private JMenuItem newMenuItem(JPopupMenu popup, String item,
			String actionCommand) {
		JMenuItem menuItem = new JMenuItem(item);
		menuItem.addActionListener(this);
		menuItem.setActionCommand(actionCommand);
		popup.add(menuItem);
		return menuItem;
	}

	private JCheckBoxMenuItem newCheckBoxMenuItem(JMenu menu, String item) {
		JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(item);
		menuItem.addActionListener(this);
		menu.add(menuItem);
		return menuItem;
	}

	private JMenuBar createMenuBar() {
		JMenuBar menubar = new JMenuBar();

		// File menu
		JMenu fileMenu = new JMenu("File");

		loadTranscriptionItem = newMenuItem(fileMenu, "Load transcription...");
		saveTranscriptionItem = newMenuItem(fileMenu, "Save transcription");
		saveAsTranscriptionItem = newMenuItem(fileMenu,
				"Save transcription as...");
		fileMenu.addSeparator();
		closeItem = newMenuItem(fileMenu, "Close");
		preferencesItem = newMenuItem(fileMenu, "Preferences...");
		fileMenu.addSeparator();
		exitItem = newMenuItem(fileMenu, "Exit");
		menubar.add(fileMenu);

		// Navigation menu
		JMenu naviMenu = new JMenu("Navigation");
		goToTurnItem = newMenuItem(naviMenu, "Go to turn...");
		goToFirstTurnItem = newMenuItem(naviMenu, "Go to first turn");
		goToNextTurnItem = newMenuItem(naviMenu, "Go to next turn");
		goToPrevTurnItem = newMenuItem(naviMenu, "Go to previous turn");
		naviMenu.addSeparator();
		goToNextWordItem = newMenuItem(naviMenu, "Go to next word");
		goToPrevWordItem = newMenuItem(naviMenu, "Go to prev word");
		menubar.add(naviMenu);

		// Sound menu
		JMenu soundMenu = new JMenu("Sound");
		playWordItem = newMenuItem(soundMenu, "Play word");
		playSelectedRegionItem = newMenuItem(soundMenu, "Play selected region");
		playTurnItem = newMenuItem(soundMenu, "Play turn");
		menubar.add(soundMenu);

		// Transcription menu
		JMenu transcriptionMenu = new JMenu("Transcription");
		editWordItem = newMenuItem(transcriptionMenu, "Edit word");
		deleteWordItem = newMenuItem(transcriptionMenu, "Delete word");
		insertWordItem = newMenuItem(transcriptionMenu, "Insert word");
		transcriptionMenu.addSeparator();
		moveRightWordBoundaryRightItem = newMenuItem(transcriptionMenu,
				"Move right word boundary to the right");
		moveRightWordBoundaryLeftItem = newMenuItem(transcriptionMenu,
				"Move right word boundary to the left");
		moveLeftWordBoundaryRightItem = newMenuItem(transcriptionMenu,
				"Move left word boundary to the right");
		moveLeftWordBoundaryLeftItem = newMenuItem(transcriptionMenu,
				"Move left word boundary to the left");
		transcriptionMenu.addSeparator();
		connectToNextWordItem = newMenuItem(transcriptionMenu,
				"Connect to next word");
		connectToPrevWordItem = newMenuItem(transcriptionMenu,
				"Connect to prev word");
		disconnectFromNextWordItem = newMenuItem(transcriptionMenu,
				"Disconnect from next word");
		disconnectFromPrevWordItem = newMenuItem(transcriptionMenu,
				"Disconnect from prev word");
		menubar.add(transcriptionMenu);

		// View menu
		JMenu viewMenu = new JMenu("View");
		viewSpectrogramControlItem = newCheckBoxMenuItem(viewMenu, "View spectrogram control window");
		viewMenu.addSeparator();		
		viewSpectrumItem = newCheckBoxMenuItem(viewMenu, "View spectrum");
		viewAutocorrelationItem = newCheckBoxMenuItem(viewMenu,
				"View autocorrelation");
		viewPitchEstimatorItem = newCheckBoxMenuItem(viewMenu,
				"Show pitch estimator window");
		viewMenu.addSeparator();
		zoomItem = newCheckBoxMenuItem(viewMenu, "Zoom mode");
		menubar.add(viewMenu);

		// Help menu
		JMenu helpMenu = new JMenu("Help");
		aboutItem = newMenuItem(helpMenu, "About...");
		menubar.add(helpMenu);

		return menubar;
	}

	public JPopupMenu createPopupMenu() {
		JPopupMenu popup = new JPopupMenu();
		f0SetToOriginalItem = newMenuItem(popup, "Set to original value", "F0_setToOriginal");
		f0SetToZeroItem = newMenuItem(popup, "Set to zero", "F0_setToZero");
		f0SetToDoubleItem = newMenuItem(popup, "Double value", "F0_setToDouble");
		f0SetTohalfItem = newMenuItem(popup, "Half value", "F0_setToHalf");
		f0SplineInterpolationItem = newMenuItem(popup, "Spline interpolation", "F0_splineInterpolation");
		modifyPopUpMenu(0);
		return popup;
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		int code = arg0.getKeyCode();
		int mod = arg0.getModifiersEx();
		switch (code) {
		case KeyEvent.VK_RIGHT:
			if (mod == InputEvent.SHIFT_DOWN_MASK) {
				transcriptionVisualizer.moveRightBorder(160);
			} else if (mod == InputEvent.CTRL_DOWN_MASK) {
				transcriptionVisualizer.moveLeftBorder(160);
			} else if (mod == InputEvent.ALT_DOWN_MASK) {
				nextTurn();
			} else {
				transcriptionVisualizer.nextWord();
			}
			break;
		case KeyEvent.VK_LEFT:
			if (mod == InputEvent.SHIFT_DOWN_MASK) {
				transcriptionVisualizer.moveRightBorder(-160);
			} else if (mod == InputEvent.CTRL_DOWN_MASK) {
				transcriptionVisualizer.moveLeftBorder(-160);
			} else if (mod == InputEvent.ALT_DOWN_MASK) {
				prevTurn();
			} else {
				transcriptionVisualizer.previousWord();
			}
			break;
		case KeyEvent.VK_DELETE:
			transcriptionVisualizer.deleteWord();
			break;
		case KeyEvent.VK_ENTER:
			wordEdit.requestFocusInWindow();
			break;
		case KeyEvent.VK_F3:
			audioSignalVisualizer.switchMode(VisualComponent.ZOOM_MODE);
			powerVisualizer.switchMode(VisualComponent.ZOOM_MODE);
			spectrogramVisualizer.switchMode(VisualComponent.ZOOM_MODE);
			zoomItem.setSelected(true);
			break;
		case KeyEvent.VK_F5:
			showSpectrum();
			break;
		case KeyEvent.VK_F6:
			showAutocorrelation();
			break;
		case KeyEvent.VK_F7:
			showPitchEstimator();
			break;
		case KeyEvent.VK_ESCAPE:
			if (zoomItem.isSelected()) {
				audioSignalVisualizer
						.switchMode(VisualComponent.SELECTION_MODE);
				powerVisualizer
				.switchMode(VisualComponent.SELECTION_MODE);
				spectrogramVisualizer
						.switchMode(VisualComponent.SELECTION_MODE);
				zoomItem.setSelected(false);
			} else if (audioSignalVisualizer.getSelected()) {
				audioSignalVisualizer.setSelected(false);
			} else if (audioSignalVisualizer.getMarked()) {
				audioSignalVisualizer.setMarked(false);
			}
			break;
		/*
		case KeyEvent.VK_PLUS:
		case KeyEvent.VK_ADD:
			if (mod == InputEvent.SHIFT_DOWN_MASK) {
				spectrogramVisualizer.setProperties(spectrogramVisualizer.getGamma() + 0.1, spectrogramVisualizer.getBrightness());
			} else {
				spectrogramVisualizer.setProperties(spectrogramVisualizer.getGamma(), spectrogramVisualizer.getBrightness() + 0.05f);				
			}
			break;
		case KeyEvent.VK_MINUS:
		case KeyEvent.VK_SUBTRACT:
			if (mod == InputEvent.SHIFT_DOWN_MASK) {
				spectrogramVisualizer.setProperties(spectrogramVisualizer.getGamma() - 0.1, spectrogramVisualizer.getBrightness());
			} else {
				spectrogramVisualizer.setProperties(spectrogramVisualizer.getGamma(), spectrogramVisualizer.getBrightness() - 0.05f);				
			}
			break;
		*/			
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		char c = arg0.getKeyChar();

		switch (c) {
		case 'D':
			transcriptionVisualizer.disconnect(+1);
			break;
		case 'd':
			transcriptionVisualizer.disconnect(-1);
			break;
		case 'i':
			insertWord();
			break;
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == loadTranscriptionItem) {
			loadTranscription();
			return;
		}
		if (arg0.getSource() == saveTranscriptionItem) {
			saveTranscription();
			return;
		}
		if (arg0.getSource() == saveAsTranscriptionItem) {
			saveAsTranscription();
			return;
		}
		if (arg0.getSource() == closeItem) {
			close();
			return;
		}
		if (arg0.getSource() == preferencesItem) {
			preferencesDialog.showDialog();
			return;
		}
		if (arg0.getSource() == exitItem) {
			exit();
			return;
		}
		if (arg0.getSource() == goToTurnItem) {
			turnListDialog.show(list.getIndex());
			return;
		}
		if (arg0.getSource() == goToFirstTurnItem) {
			firstTurn();
			return;
		}
		if (arg0.getSource() == goToNextTurnItem) {
			nextTurn();
			return;
		}
		if (arg0.getSource() == goToPrevTurnItem) {
			prevTurn();
			return;
		}
		if (arg0.getSource() == goToNextWordItem) {
			transcriptionVisualizer.nextWord();
			return;
		}
		if (arg0.getSource() == goToPrevWordItem) {
			transcriptionVisualizer.previousWord();
			return;
		}
		if (arg0.getSource() == playWordItem) {
			playWord();
			return;
		}
		if (arg0.getSource() == playSelectedRegionItem) {
			playSelectedRegion();
			return;
		}
		if (arg0.getSource() == playTurnItem) {
			playTurn();
			return;
		}
		if (arg0.getSource() == editWordItem) {
			wordEdit.requestFocusInWindow();
			return;
		}
		if (arg0.getSource() == deleteWordItem) {
			transcriptionVisualizer.deleteWord();
			return;
		}
		if (arg0.getSource() == insertWordItem) {
			insertWord();
			return;
		}
		if (arg0.getSource() == moveRightWordBoundaryRightItem) {
			transcriptionVisualizer.moveRightBorder(160);
			return;
		}
		if (arg0.getSource() == moveRightWordBoundaryLeftItem) {
			transcriptionVisualizer.moveRightBorder(-160);
			return;
		}
		if (arg0.getSource() == moveLeftWordBoundaryRightItem) {
			transcriptionVisualizer.moveLeftBorder(160);
			return;
		}
		if (arg0.getSource() == moveLeftWordBoundaryLeftItem) {
			transcriptionVisualizer.moveLeftBorder(-160);
			return;
		}
		if (arg0.getSource() == connectToNextWordItem) {
			transcriptionVisualizer.connect(+1);
			return;
		}
		if (arg0.getSource() == connectToPrevWordItem) {
			transcriptionVisualizer.connect(-1);
			return;
		}
		if (arg0.getSource() == disconnectFromNextWordItem) {
			transcriptionVisualizer.disconnect(+1);
			return;
		}
		if (arg0.getSource() == disconnectFromPrevWordItem) {
			transcriptionVisualizer.disconnect(-1);
			return;
		}
		if (arg0.getSource() == viewSpectrogramControlItem) {
			showSpectrogramControl();
			return;
		}
		if (arg0.getSource() == viewSpectrumItem) {
			showSpectrum();
			return;
		}
		if (arg0.getSource() == viewAutocorrelationItem) {
			showAutocorrelation();
			return;
		}
		if (arg0.getSource() == viewPitchEstimatorItem) {
			showPitchEstimator();
			return;
		}
		if (arg0.getSource() == zoomItem) {
			zoomMode();
			return;
		}
		if (arg0.getSource() == wordEdit) {
			transcriptionVisualizer.setWord(wordEdit.getText());
			requestFocusInWindow();
			return;
		}
		if (arg0.getSource() == aboutItem) {
			aboutDialog.setVisible(true);
			return;
		}
		if (arg0.getActionCommand().equals("F0_setToOriginal")) {
			pitchVisualizer.setToOriginal();
			return;
		}
		if (arg0.getActionCommand().equals("F0_setToZero")) {
			pitchVisualizer.setToZero();
			return;
		}
		if (arg0.getActionCommand().equals("F0_setToHalf")) {
			pitchVisualizer.setToHalf();
			return;
		}
		if (arg0.getActionCommand().equals("F0_setToDouble")) {
			pitchVisualizer.setToDouble();
			return;
		}
		if (arg0.getActionCommand().equals("F0_splineInterpolation")) {
			pitchVisualizer.splineInterpolation();
			return;
		}
	}

	public void windowClosed(Object sender) {
		if (sender == spectrogramControlWindow) {
			viewSpectrogramControlItem.setSelected(false);
		}
		if (sender == spectrumWindow) {
			viewSpectrumItem.setSelected(false);
		}
		if (sender == acWindow) {
			viewAutocorrelationItem.setSelected(false);
		}
		if (sender == pitchEstimatorWindow) {
			viewPitchEstimatorItem.setSelected(false);
		}
	}

	private void modifyPopUpMenu(int n) {
		switch (n) {
		case 0: 
			f0SetToOriginalItem.setEnabled(false);
			f0SetToZeroItem.setEnabled(false);
			f0SetToDoubleItem.setEnabled(false); 
			f0SetTohalfItem.setEnabled(false); 
			f0SplineInterpolationItem.setEnabled(false); 					
			break;
		case 1: 
			f0SetToOriginalItem.setEnabled(true);
			f0SetToZeroItem.setEnabled(true);
			f0SetToDoubleItem.setEnabled(true); 
			f0SetTohalfItem.setEnabled(true); 
			f0SplineInterpolationItem.setEnabled(false); 					
			break;
		default:
			f0SetToOriginalItem.setEnabled(true);
			f0SetToZeroItem.setEnabled(true);
			f0SetToDoubleItem.setEnabled(true); 
			f0SetTohalfItem.setEnabled(true); 
			f0SplineInterpolationItem.setEnabled(true); 					
		}
	}
	
	public void turnSelected(int index) {
		try {
			if (!savePitch()) {
				return;
			}
			Transcription transcription = list.goTo(index);
			newTranscription(transcription);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	private void enableMenuItems(boolean enable) {
		loadTranscriptionItem.setEnabled(!enable);
		saveAsTranscriptionItem.setEnabled(enable);
		saveTranscriptionItem.setEnabled(enable);
		closeItem.setEnabled(enable);
		preferencesItem.setEnabled(true);
		exitItem.setEnabled(true);
		goToTurnItem.setEnabled(enable);
		goToFirstTurnItem.setEnabled(enable);
		goToNextTurnItem.setEnabled(enable);
		goToPrevTurnItem.setEnabled(enable);
		goToNextWordItem.setEnabled(enable);
		goToPrevWordItem.setEnabled(enable);
		playWordItem.setEnabled(enable);
		playSelectedRegionItem.setEnabled(enable);
		playTurnItem.setEnabled(enable);
		editWordItem.setEnabled(enable);
		deleteWordItem.setEnabled(enable);
		insertWordItem.setEnabled(enable);
		moveRightWordBoundaryRightItem.setEnabled(enable);
		moveRightWordBoundaryLeftItem.setEnabled(enable);
		moveLeftWordBoundaryRightItem.setEnabled(enable);
		moveLeftWordBoundaryLeftItem.setEnabled(enable);
		connectToNextWordItem.setEnabled(enable);
		connectToPrevWordItem.setEnabled(enable);
		disconnectFromNextWordItem.setEnabled(enable);
		disconnectFromPrevWordItem.setEnabled(enable);
		viewSpectrogramControlItem.setEnabled(enable);
		viewSpectrumItem.setEnabled(enable);
		viewAutocorrelationItem.setEnabled(enable);
		viewPitchEstimatorItem.setEnabled(enable);
		zoomItem.setEnabled(enable);
	}

	private void newTranscription(Transcription transcription)
			throws IOException, UnsupportedAudioFileException {
		try {
			reader = new AudioFileReader(
					preferences.getString("wavdir")
							+ System.getProperty("file.separator")
							+ list.getTurnName(), false);
			reader.setPreEmphasis(0);
			BandPassFilter filteredSource = new BandPassFilter(reader, 0, 8000, 64);
			source = new BufferedAudioSource(filteredSource);
			audioSignalVisualizer.setBufferedAudioSource(source);
			powerVisualizer.setBufferedAudioSource(source);
			spectrogramVisualizer.setBufferedAudioSource(source);
			transcriptionVisualizer.setBufferedAudioSource(source);
			spectrumWindow.setBufferedAudioSource(source);
			acWindow.setBufferedAudioSource(source);
			pitchEstimatorWindow.setBufferedAudioSource(source);
			newPitch(source);
			transcriptionVisualizer.setTranscription(transcription, source);
			playButton.setEnabled(true);
			wordEdit.setEnabled(true);
			wordEdit.setBackground(audioSignalVisualizer.colorBackground);
			fileLabel.setText(transcription.filename);
		} catch (Exception e) {
			source = null;
			audioSignalVisualizer.setBufferedAudioSource(source);
			powerVisualizer.setBufferedAudioSource(source);
			spectrogramVisualizer.setBufferedAudioSource(source);
			pitchVisualizer.setBufferedAudioSource(source);
			transcriptionVisualizer.setBufferedAudioSource(source);
			spectrumWindow.setBufferedAudioSource(source);
			acWindow.setBufferedAudioSource(source);
			pitchEstimatorWindow.setBufferedAudioSource(source);
			transcriptionVisualizer.setTranscription(null, source);
			playButton.setEnabled(false);
			wordEdit.setEnabled(false);
			wordEdit.setBackground(audioSignalVisualizer.colorBackgroundDisabled);
			fileLabel.setText(" ");
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
		repaint();
	}

	public void loadTranscription() {
		try {			
			JFileChooser fc = new JFileChooser(transcriptionFile);
			fc.setSelectedFile(new File(transcriptionFile));
			fc.setDialogTitle("Load transcription");
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				list = new TranscriptionList();
				transcriptionFile = fc.getSelectedFile().getPath();
				list.load(transcriptionFile);
				turnListDialog.fillList(list.getTurnList());
				newTranscription(list.goTo(0));
				enableMenuItems(true);
			}
		} catch (Exception e) {
			enableMenuItems(false);
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void saveTranscription() {
		if (list != null) {
			try {
				list.save();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public void saveAsTranscription() {
		if (list != null) {
			JFileChooser fc = new JFileChooser(transcriptionFile);
			fc.setSelectedFile(new File(transcriptionFile));
			fc.setDialogTitle("Save transcription");
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				try {
					list.save(fc.getSelectedFile().getName());
				} catch (IOException e) {
					JOptionPane.showMessageDialog(this, e.getMessage(),
							"Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	public void newPitch(BufferedAudioSource audiosource) {
		BufferedFrameSource source1 = null;
		pitchsource = null;

		String filename = list.getTurnName();
		int index = filename.lastIndexOf('.');
		if (index > 0) {
			filename = filename.substring(0, index);
		}

		try {
			source1 = new BufferedFrameSource(new FrameFileReader(
					preferences.getString("f0dir_original")
							+ System.getProperty("file.separator") + filename
							+ preferences.getString("f0suffix_original")));
			try {
				pitchsource = new BufferedFrameSource(new FrameFileReader(
						preferences.getString("f0dir_corrected")
								+ System.getProperty("file.separator")
								+ filename
								+ preferences.getString("f0suffix_corrected")));
			} catch (IOException e) {
				pitchsource = new BufferedFrameSource(source1);
			}
			pitchVisualizer.setBufferedAudioSource(audiosource);
			pitchVisualizer.setBufferedPitchSources(source1, pitchsource);
		} catch (IOException e) {
			pitchVisualizer.setBufferedAudioSource(null);
			pitchVisualizer.setBufferedPitchSources(null, null);
		}

	}

	public boolean savePitch() {
		if (pitchVisualizer.hasChanged()) {
			try {
				String filename = list.getTurnName();
				int index = filename.lastIndexOf('.');
				if (index > 0) {
					filename = filename.substring(0, index);
				}
				filename = preferences.getString("f0dir_corrected")
						+ System.getProperty("file.separator") + filename
						+ preferences.getString("f0suffix_corrected");
				FrameFileWriter writer = new FrameFileWriter(
						pitchsource.getReader(), filename);
				writer.write();
				writer.close();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}

	public void close() {
		if (!savePitch()) {
			return;
		}

		if ((list != null) && list.hasChanged()) {
			int answer = JOptionPane
					.showConfirmDialog(
							this,
							"The transcription has been changed.\n Do you want to save it?",
							"Question", JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE);
			if (answer == JOptionPane.OK_OPTION) {
				try {
					list.save();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(this, e.getMessage(),
							"Error", JOptionPane.ERROR_MESSAGE);
				}
			} else if (answer == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		source = null;
		audioSignalVisualizer.setBufferedAudioSource(null);
		powerVisualizer.setBufferedAudioSource(null);
		spectrogramVisualizer.setBufferedAudioSource(null);
		pitchVisualizer.setBufferedAudioSource(null);
		transcriptionVisualizer.setBufferedAudioSource(null);
		transcriptionVisualizer.setTranscription(null, null);
		spectrumWindow.setBufferedAudioSource(null);
		acWindow.setBufferedAudioSource(null);
		pitchEstimatorWindow.setBufferedAudioSource(null);
		wordEdit.setEnabled(false);
		wordEdit.setBackground(audioSignalVisualizer.colorBackgroundDisabled);
		fileLabel.setText(" ");
		playButton.setEnabled(false);
		list = null;
		enableMenuItems(false);
	}

	public void exit() {
		close();
		spectrumWindow.saveWindowProperties();
		acWindow.saveWindowProperties();
		pitchEstimatorWindow.saveWindowProperties();
		preferences.set("transcriptionFile", transcriptionFile);
		try {
			preferences.save();
		} catch (IOException e) {
			System.err.println(e);
		}
		System.exit(0);
	}

	public void nextTurn() {
		if (list != null) {
			if (list.hasNext()) {
				if (!savePitch()) {
					return;
				}
				try {
					Transcription transcription = list.next();
					newTranscription(transcription);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(this, e.getMessage(),
							"Error", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			}
		}
	}

	public void prevTurn() {
		if (list != null) {
			if (list.hasPrevious()) {
				if (!savePitch()) {
					return;
				}
				try {
					Transcription transcription = list.previous();
					newTranscription(transcription);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(this, e.getMessage(),
							"Error", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			}
		}
	}

	public void firstTurn() {
		if (list != null) {
			if (!savePitch()) {
				return;
			}
			try {
				Transcription transcription = list.goTo(0);
				newTranscription(transcription);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		}
	}

	/**
	 * Demonstration of how to play selected or highlighted regions of the audio
	 * signal
	 */
	public void play() {
		if (source == null) {
			return;
		}
		if (audioSignalVisualizer.getSelected()) {
			playSelectedRegion();
		} else {
			playWord();
		}
	}

	public void playWord() {
		if (source == null) {
			return;
		}
		if (audioSignalVisualizer.isHighlighted) {
			int s = audioSignalVisualizer.highlightedSectionStartSample;
			int e = audioSignalVisualizer.highlightedSectionEndSample;
			play(s, e);
		}
	}

	public void playSelectedRegion() {
		if (source == null) {
			return;
		}
		if (audioSignalVisualizer.getSelected()) {
			int s = audioSignalVisualizer.selectedSectionStartSample;
			int e = audioSignalVisualizer.selectedSectionEndSample;
			play(s, e);
		}
	}

	public void playTurn() {
		if (source == null) {
			return;
		}
		play(0, source.getBufferSize());
	}

	private void play(int start, int end) {
		try {
			AudioPlay audioPlay = new AudioPlay(source.getReader(start, end
					- start + 1));

			// play whole section
			while (audioPlay.write() > 0) {
			}

		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "File error", JOptionPane.ERROR_MESSAGE);
		} catch (LineUnavailableException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Audio error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void insertWord() {
		if (audioSignalVisualizer.getSelected()) {
			transcriptionVisualizer.insertWord("",
					audioSignalVisualizer.selectedSectionStartSample,
					audioSignalVisualizer.selectedSectionEndSample);
			audioSignalVisualizer.setSelected(false);
		} else {
			JOptionPane.showMessageDialog(this, "Please select a region!",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void showSpectrogramControl() {
		if (spectrogramControlWindow.isVisible()) {
			spectrogramControlWindow.setVisible(false);
			viewSpectrogramControlItem.setSelected(false);
		} else {
			spectrogramControlWindow.setVisible(true);
			viewSpectrogramControlItem.setSelected(true);			
		}
	}
	
	public void showSpectrum() {
		if (spectrumWindow.isVisible()) {
			spectrumWindow.setVisible(false);
			viewSpectrumItem.setSelected(false);
		} else {
			spectrumWindow.setVisible(true);
			viewSpectrumItem.setSelected(true);
		}
	}

	public void showAutocorrelation() {
		if (acWindow.isVisible()) {
			acWindow.setVisible(false);
			viewAutocorrelationItem.setSelected(false);
		} else {
			acWindow.setVisible(true);
			viewAutocorrelationItem.setSelected(true);
		}
	}

	public void showPitchEstimator() {
		if (pitchEstimatorWindow.isVisible()) {
			pitchEstimatorWindow.setVisible(false);
			viewPitchEstimatorItem.setSelected(false);
		} else {
			pitchEstimatorWindow.setVisible(true);
			viewPitchEstimatorItem.setSelected(true);
		}
	}

	public void zoomMode() {
		if (zoomItem.isSelected()) {
			audioSignalVisualizer.switchMode(VisualComponent.ZOOM_MODE);
			powerVisualizer.switchMode(VisualComponent.ZOOM_MODE);
			spectrogramVisualizer.switchMode(VisualComponent.ZOOM_MODE);
		} else {
			audioSignalVisualizer.switchMode(VisualComponent.SELECTION_MODE);
			powerVisualizer.switchMode(VisualComponent.SELECTION_MODE);
			spectrogramVisualizer.switchMode(VisualComponent.SELECTION_MODE);
		}
	}

	public void pitchDefined(double frequency) {
		int n = pitchVisualizer.getNumberOfSelectedFrames();
		if (n == 1) {
			F0Point[] points = pitchVisualizer.getSelectedPoints();
			pitchVisualizer.setToValue(points[0].frame, frequency);
		} else if (n == 0) {
			JOptionPane.showMessageDialog(this, "No F0 points selected.",
					"Error", JOptionPane.ERROR_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(this,
					"More than 1 F0 point selected.", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void sampleSelected(int sample) {
		spectrumWindow.show(sample);
		acWindow.show(sample);
		pitchEstimatorWindow.show(sample);
		if (pitchVisualizer.getNumberOfSelectedFrames() == 1) {
			pitchVisualizer.removeSelection();
		}
	}

	public static void main(String[] args) throws Exception {
		// System.err.println(AudioSystem.getMixer(null).getMixerInfo().getName());
		// Mixer.Info [] availableMixers = AudioSystem.getMixerInfo();
		// for (Mixer.Info m : availableMixers)
		//  	System.out.println(m.getName());
		
		new Transcriber("JSTK Transcriber");
	}

}
