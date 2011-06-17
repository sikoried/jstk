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
package de.fau.cs.jstk.app.pitchcorrector;

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

import de.fau.cs.jstk.app.transcriber.AutocorrelationWindow;
import de.fau.cs.jstk.app.transcriber.JFrame;
import de.fau.cs.jstk.app.transcriber.PitchEstimatorWindow;
import de.fau.cs.jstk.app.transcriber.Preferences;
import de.fau.cs.jstk.app.transcriber.SpectrogramControlWindow;
import de.fau.cs.jstk.app.transcriber.SpectrumWindow;
import de.fau.cs.jstk.app.transcriber.TurnListDialog;
import de.fau.cs.jstk.app.transcriber.TurnSelectedListener;
import de.fau.cs.jstk.app.transcriber.WindowClosedListener;
import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.io.BufferedFrameSource;
import de.fau.cs.jstk.sampled.AudioFileReader;
import de.fau.cs.jstk.sampled.ThreadedPlayer;
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

public class PitchCorrector extends JFrame implements KeyListener,
		ActionListener, WindowClosedListener, TurnSelectedListener,
		PitchDefinedListener, SampleSelectedListener {

	class PitchContextMenu {
		JMenuItem f0SetToOriginalItem;
		JMenuItem f0SetToZeroItem;
		JMenuItem f0SetToDoubleItem;
		JMenuItem f0SetTohalfItem;
		JMenuItem f0SplineInterpolationItem;
	}

	private static final long serialVersionUID = -9038493231330330945L;

	private AudioFileReader reader;
	private BufferedAudioSource source;
	private BufferedFrameSource pitchsource;
	private VisualizerSpeechSignal audioSignalVisualizer;
	private VisualizerPower powerVisualizer;
	private VisualizerSpectrogram spectrogramVisualizer;
	private VisualizerTranscription transcriptionVisualizer;
	private VisualizerPitch[] pitchVisualizers;
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
	private JCheckBoxMenuItem viewSpectrogramControlItem;
	private JCheckBoxMenuItem viewSpectrumItem;
	private JCheckBoxMenuItem viewAutocorrelationItem;
	private JCheckBoxMenuItem viewPitchEstimatorItem;
	private JCheckBoxMenuItem zoomItem;
	private JMenuItem aboutItem;
	private PitchContextMenu[] pitchContextMenus;
	private JPanel[] pitchPanels;
	private JCheckBoxMenuItem[] viewPitchPanelItems;

	private Preferences preferences;
	private int pitchDisplays;
	private ThreadedPlayer audioPlay;

	public PitchCorrector(String title) throws Exception {
		super(title, "mainWindow");

		preferences = new PitchCorrectorPreferences("pitchcorrector.ini");
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
		// Note: need to have jstk/res on java build path!
		playButton = new JButton("", new ImageIcon(this.getClass().getResource(
				"/app/transcriber/play1.png")));
		playButton.setRolloverIcon(new ImageIcon(this.getClass().getResource(
				"/app/transcriber/play2.png")));
		playButton.setDisabledIcon(new ImageIcon(this.getClass().getResource(
				"/app/transcriber/play3.png")));
		playButton.setRolloverEnabled(true);
		playButton.setContentAreaFilled(false);
		playButton.setBorderPainted(false);
		playButton.setMargin(new Insets(0, 0, 0, 0));
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
		audioSignalVisualizer.setPreferredSize(new Dimension(640, 70));
		box.add(audioSignalVisualizer);
		box.add(Box.createVerticalStrut(10));

		powerVisualizer = new VisualizerPower("powerVis", source);
		powerVisualizer.setPreferredSize(new Dimension(640, 50));
		box.add(powerVisualizer);
		box.add(Box.createVerticalStrut(10));

		spectrogramVisualizer = new VisualizerSpectrogram("spectrogramVis",
				source);
		spectrogramVisualizer.setPreferredSize(new Dimension(640, 158));
		box.add(spectrogramVisualizer);
		box.add(Box.createVerticalStrut(10));

		int shift = preferences.getInt("f0.shift");
		pitchDisplays = preferences.getInt("f0.displays");
		pitchVisualizers = new VisualizerPitch[pitchDisplays];
		pitchContextMenus = new PitchContextMenu[pitchDisplays];
		pitchPanels = new JPanel[pitchDisplays];
		for (int i = 0; i < pitchDisplays; i++) {
			pitchContextMenus[i] = new PitchContextMenu();

			pitchVisualizers[i] = new VisualizerPitch("pitchVis" + i, source, null,
					null, shift);
			pitchVisualizers[i].setPreferredSize(new Dimension(640, 70));
			pitchVisualizers[i].setComponentPopupMenu(createPopupMenu(i));
			pitchVisualizers[i].f0min = preferences.getInt("f0.minimum");
			pitchVisualizers[i].f0max = preferences.getInt("f0.maximum");
			pitchPanels[i] = new JPanel();
			pitchPanels[i].setLayout(new BorderLayout());
			pitchPanels[i].add(pitchVisualizers[i], BorderLayout.CENTER);
			pitchPanels[i].add(Box.createVerticalStrut(10), BorderLayout.SOUTH);
			box.add(pitchPanels[i]);			
		}

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
		for (int i = 0; i < pitchDisplays; i++) {
			pitchVisualizers[i].setVisualizationInformer(informer);
		}
		transcriptionVisualizer.setVisualizationInformer(informer);

		// set the desired mode: NORMAL_MODE, VALUE_MODE, SELECTION_MODE, or
		// ZOOM_MODE
		audioSignalVisualizer.switchMode(FileVisualizer.SELECTION_MODE);
		audioSignalVisualizer.showCursorY = false;
		powerVisualizer.switchMode(FileVisualizer.SELECTION_MODE);
		powerVisualizer.showCursorY = false;
		spectrogramVisualizer.switchMode(FileVisualizer.SELECTION_MODE);
		c.add(scrollbar, BorderLayout.SOUTH);

		spectrogramControlWindow = new SpectrogramControlWindow(preferences,
				this, spectrogramVisualizer);
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
		for (int i = 0; i < pitchDisplays; i++) {
			pitchVisualizers[i]
					.addSampleSelectedListener(new SampleSelectedListener() {
						public void sampleSelected(int sample) {
							spectrumWindow.show(sample);
							acWindow.show(sample);
							pitchEstimatorWindow.show(sample);
						}
					});
		}

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

		for (int i = 0; i < pitchDisplays; i++) {
			pitchVisualizers[i]
					.addF0PointsSelectedListener(new F0PointsSelectedListener() {
						public void f0PointsSelected(VisualizerPitch sender,
								F0Point[] points) {
							int n = sender.getNumberOfSelectedFrames();
							for (int j = 0; j < pitchDisplays; j++) {
								if (pitchVisualizers[j] != sender) {
									pitchVisualizers[j].removeSelection();
									modifyPopUpMenu(j, 0);
								} else {
									modifyPopUpMenu(j, n);									
								}
							}
						}
					});
		}

		addKeyListener(this);
		this.setJMenuBar(createMenuBar());
		enableMenuItems(false);

		setFocusable(true);
		setVisible(true);
		
		for (int i = 0; i < pitchDisplays; i++) {
			if (!preferences.getBoolean("f0display" + i + ".visible")) {
				viewPitchPanelItems[i].setSelected(false);
			} 
			showPitchPanel(i);
		}
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
		viewPitchPanelItems = new JCheckBoxMenuItem[pitchDisplays];
		for (int i = 0; i < pitchDisplays; i++) {
			viewPitchPanelItems[i] = newCheckBoxMenuItem(viewMenu,
					"Show pitch display " + (i + 1));
			viewPitchPanelItems[i].setSelected(true);
		}
		viewMenu.addSeparator();
		viewSpectrogramControlItem = newCheckBoxMenuItem(viewMenu,
				"View spectrogram control window");
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

	public JPopupMenu createPopupMenu(int number) {
		JPopupMenu popup = new JPopupMenu();
		pitchContextMenus[number].f0SetToOriginalItem = newMenuItem(popup,
				"Set to original value", "F0_setToOriginal" + number);
		pitchContextMenus[number].f0SetToZeroItem = newMenuItem(popup,
				"Set to zero", "F0_setToZero" + number);
		pitchContextMenus[number].f0SetToDoubleItem = newMenuItem(popup,
				"Double value", "F0_setToDouble" + number);
		pitchContextMenus[number].f0SetTohalfItem = newMenuItem(popup,
				"Half value", "F0_setToHalf" + number);
		pitchContextMenus[number].f0SplineInterpolationItem = newMenuItem(
				popup, "Spline interpolation", "F0_splineInterpolation"
						+ number);
		modifyPopUpMenu(number, 0);
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
			if ((audioPlay != null) && (audioPlay.isPlaying())) {
				try {
					audioPlay.stop();
				} catch (Exception e) {
					System.err.println(e.toString());
				}
			} else if (zoomItem.isSelected()) {
				audioSignalVisualizer
						.switchMode(VisualComponent.SELECTION_MODE);
				powerVisualizer.switchMode(VisualComponent.SELECTION_MODE);
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
		 * case KeyEvent.VK_PLUS: case KeyEvent.VK_ADD: if (mod ==
		 * InputEvent.SHIFT_DOWN_MASK) {
		 * spectrogramVisualizer.setProperties(spectrogramVisualizer.getGamma()
		 * + 0.1, spectrogramVisualizer.getBrightness()); } else {
		 * spectrogramVisualizer.setProperties(spectrogramVisualizer.getGamma(),
		 * spectrogramVisualizer.getBrightness() + 0.05f); } break; case
		 * KeyEvent.VK_MINUS: case KeyEvent.VK_SUBTRACT: if (mod ==
		 * InputEvent.SHIFT_DOWN_MASK) {
		 * spectrogramVisualizer.setProperties(spectrogramVisualizer.getGamma()
		 * - 0.1, spectrogramVisualizer.getBrightness()); } else {
		 * spectrogramVisualizer.setProperties(spectrogramVisualizer.getGamma(),
		 * spectrogramVisualizer.getBrightness() - 0.05f); } break;
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

	private void updatePitchDisplays(int display) {
		for (int i = 0; i < pitchDisplays; i++) {
			if (i != display) {
				pitchVisualizers[i].update();
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		String cmd = arg0.getActionCommand();
		int pitchDisplay = 0;
		try {
			pitchDisplay = Integer.parseInt(cmd.substring(cmd.length() - 1,
					cmd.length()));
		} catch (Exception e) {
		}

		if (source == loadTranscriptionItem) {
			loadTranscription();
			return;
		}
		if (source == saveTranscriptionItem) {
			saveTranscription();
			return;
		}
		if (source == saveAsTranscriptionItem) {
			saveAsTranscription();
			return;
		}
		if (source == closeItem) {
			close();
			return;
		}
		if (source == preferencesItem) {
			preferencesDialog.showDialog();
			return;
		}
		if (source == exitItem) {
			exit();
			return;
		}
		if (source == goToTurnItem) {
			turnListDialog.show(list.getIndex());
			return;
		}
		if (source == goToFirstTurnItem) {
			firstTurn();
			return;
		}
		if (source == goToNextTurnItem) {
			nextTurn();
			return;
		}
		if (source == goToPrevTurnItem) {
			prevTurn();
			return;
		}
		if (source == goToNextWordItem) {
			transcriptionVisualizer.nextWord();
			return;
		}
		if (source == goToPrevWordItem) {
			transcriptionVisualizer.previousWord();
			return;
		}
		if (source == playWordItem) {
			playWord();
			return;
		}
		if (source == playSelectedRegionItem) {
			playSelectedRegion();
			return;
		}
		if (source == playTurnItem) {
			playTurn();
			return;
		}
		if (source == editWordItem) {
			wordEdit.requestFocusInWindow();
			return;
		}
		if (source == deleteWordItem) {
			transcriptionVisualizer.deleteWord();
			return;
		}
		if (source == insertWordItem) {
			insertWord();
			return;
		}
		if (source == moveRightWordBoundaryRightItem) {
			transcriptionVisualizer.moveRightBorder(160);
			return;
		}
		if (source == moveRightWordBoundaryLeftItem) {
			transcriptionVisualizer.moveRightBorder(-160);
			return;
		}
		if (source == moveLeftWordBoundaryRightItem) {
			transcriptionVisualizer.moveLeftBorder(160);
			return;
		}
		if (source == moveLeftWordBoundaryLeftItem) {
			transcriptionVisualizer.moveLeftBorder(-160);
			return;
		}
		if (source == connectToNextWordItem) {
			transcriptionVisualizer.connect(+1);
			return;
		}
		if (source == connectToPrevWordItem) {
			transcriptionVisualizer.connect(-1);
			return;
		}
		if (source == disconnectFromNextWordItem) {
			transcriptionVisualizer.disconnect(+1);
			return;
		}
		if (source == disconnectFromPrevWordItem) {
			transcriptionVisualizer.disconnect(-1);
			return;
		}
		for (int i = 0; i < pitchDisplays; i++) {
			if (source == viewPitchPanelItems[i]) {
				showPitchPanel(i);
				return;
			}
		}
		if (source == viewSpectrogramControlItem) {
			showSpectrogramControl();
			return;
		}
		if (source == viewSpectrumItem) {
			showSpectrum();
			return;
		}
		if (source == viewAutocorrelationItem) {
			showAutocorrelation();
			return;
		}
		if (source == viewPitchEstimatorItem) {
			showPitchEstimator();
			return;
		}
		if (source == zoomItem) {
			zoomMode();
			return;
		}
		if (source == wordEdit) {
			transcriptionVisualizer.setWord(wordEdit.getText());
			requestFocusInWindow();
			return;
		}
		if (source == aboutItem) {
			aboutDialog.setVisible(true);
			return;
		}
		if (cmd.contains("F0_setToOriginal")) {
			pitchVisualizers[pitchDisplay].setToOriginal();
			updatePitchDisplays(pitchDisplay);
			return;
		}
		if (cmd.contains("F0_setToZero")) {
			pitchVisualizers[pitchDisplay].setToZero();
			updatePitchDisplays(pitchDisplay);
			return;
		}
		if (arg0.getActionCommand().contains("F0_setToHalf")) {
			pitchVisualizers[pitchDisplay].setToHalf();
			updatePitchDisplays(pitchDisplay);
			return;
		}
		if (arg0.getActionCommand().contains("F0_setToDouble")) {
			pitchVisualizers[pitchDisplay].setToDouble();
			updatePitchDisplays(pitchDisplay);
			return;
		}
		if (arg0.getActionCommand().contains("F0_splineInterpolation")) {
			pitchVisualizers[pitchDisplay].splineInterpolation();
			updatePitchDisplays(pitchDisplay);
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

	private void modifyPopUpMenu(int number, int n) {
		switch (n) {
		case 0:
			pitchContextMenus[number].f0SetToOriginalItem.setEnabled(false);
			pitchContextMenus[number].f0SetToZeroItem.setEnabled(false);
			pitchContextMenus[number].f0SetToDoubleItem.setEnabled(false);
			pitchContextMenus[number].f0SetTohalfItem.setEnabled(false);
			pitchContextMenus[number].f0SplineInterpolationItem
					.setEnabled(false);
			break;
		case 1:
			pitchContextMenus[number].f0SetToOriginalItem.setEnabled(true);
			pitchContextMenus[number].f0SetToZeroItem.setEnabled(true);
			pitchContextMenus[number].f0SetToDoubleItem.setEnabled(true);
			pitchContextMenus[number].f0SetTohalfItem.setEnabled(true);
			pitchContextMenus[number].f0SplineInterpolationItem
					.setEnabled(false);
			break;
		default:
			pitchContextMenus[number].f0SetToOriginalItem.setEnabled(true);
			pitchContextMenus[number].f0SetToZeroItem.setEnabled(true);
			pitchContextMenus[number].f0SetToDoubleItem.setEnabled(true);
			pitchContextMenus[number].f0SetTohalfItem.setEnabled(true);
			pitchContextMenus[number].f0SplineInterpolationItem
					.setEnabled(true);
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
		for (int i = 0; i < pitchDisplays; i++) {
			viewPitchPanelItems[i].setEnabled(enable);
		}
		viewSpectrogramControlItem.setEnabled(enable);
		viewSpectrumItem.setEnabled(enable);
		viewAutocorrelationItem.setEnabled(enable);
		viewPitchEstimatorItem.setEnabled(enable);
		zoomItem.setEnabled(enable);
	}

	private void newTranscription(Transcription transcription) {
		try {
			reader = new AudioFileReader(
					preferences.getString("wavdir")
							+ System.getProperty("file.separator")
							+ list.getTurnName(), false);
			reader.setPreEmphasis(false, 0);
			//BandPassFilter filteredSource = new BandPassFilter(reader, 0, 8000, 64);
			source = new BufferedAudioSource(reader);
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
			for (int i = 0; i < pitchDisplays; i++) {
				pitchVisualizers[i].setBufferedAudioSource(source);
			}
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
		BufferedFrameSource sources[] = new BufferedFrameSource[pitchDisplays]; 
		pitchsource = null;

		String filename = list.getTurnName();
		int index = filename.lastIndexOf('.');
		if (index > 0) {
			filename = filename.substring(0, index);
		}

		for (int i = 0; i < pitchDisplays; i++) {
			try {
				sources[i] = new BufferedFrameSource(new FrameFileReader(
					preferences.getString("f0dir" + i)
							+ System.getProperty("file.separator") + filename
							+ preferences.getString("f0suffix_original")));
			} catch (IOException e) {
				sources[i] = null;
			}
		}

		try {
			pitchsource = new BufferedFrameSource(new FrameFileReader(
					preferences.getString("f0dir_corrected")
							+ System.getProperty("file.separator") + filename
							+ preferences.getString("f0suffix_corrected")));
		} catch (IOException e) {
			try {
				pitchsource = new BufferedFrameSource(sources[0]);
			} catch (IOException e1) {
				pitchsource = null;
			}
		}

		for (int i = 0; i < pitchDisplays; i++) {
			if (sources[i] != null) {
				pitchVisualizers[i].setBufferedAudioSource(audiosource);
				pitchVisualizers[i].setBufferedPitchSources(sources[i], pitchsource);
			} else {
				pitchVisualizers[i].setBufferedAudioSource(null);
				pitchVisualizers[i].setBufferedPitchSources(null, null);
			}
		}
	}

	public boolean savePitch() {
		if (pitchVisualizers[0].hasChanged()) {
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
		for (int i = 0; i < pitchDisplays; i++) {
			pitchVisualizers[i].setBufferedAudioSource(null);
		}
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
			if (audioPlay == null)
				audioPlay = new ThreadedPlayer();
			
			if (audioPlay.isPlaying())
				audioPlay.stop();
			audioPlay.setup(null, source.getReader(start, end - start + 1), 0.);
			audioPlay.start();

		} catch (LineUnavailableException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Audio error",
					JOptionPane.ERROR_MESSAGE);
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Thread error",
					JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "File error",
					JOptionPane.ERROR_MESSAGE);
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

	public void showPitchPanel(int panel) {
		JPanel p = pitchPanels[panel];
		JCheckBoxMenuItem item = viewPitchPanelItems[panel];
		if (item.isSelected()) {
			p.setVisible(true);
		} else {
			p.setVisible(false);
		}
		setMinimumSize(new Dimension(getSize().width, 100));
		preferences.set("f0display" + panel + ".visible", String.valueOf(item.isSelected()));
		pack();
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
		int activePitchDisplay = 0;
		int n = pitchVisualizers[activePitchDisplay].getNumberOfSelectedFrames();
		
		while ((n == 0) && (activePitchDisplay < pitchDisplays-1)) {
			activePitchDisplay++;
			n = pitchVisualizers[activePitchDisplay].getNumberOfSelectedFrames();
		}
		
		if (n == 1) {
			F0Point[] points = pitchVisualizers[activePitchDisplay].getSelectedPoints();
			pitchVisualizers[activePitchDisplay].setToValue(points[0].frame, frequency);
			updatePitchDisplays(activePitchDisplay);
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
		for (int i = 0; i < pitchDisplays; i++) {
			if (pitchVisualizers[i].getNumberOfSelectedFrames() == 1) {
				pitchVisualizers[i].removeSelection();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		// System.err.println(AudioSystem.getMixer(null).getMixerInfo().getName());
		// Mixer.Info [] availableMixers = AudioSystem.getMixerInfo();
		// for (Mixer.Info m : availableMixers)
		// System.out.println(m.getName());

		new PitchCorrector("JSTK Pitch Corrector");
	}

}
