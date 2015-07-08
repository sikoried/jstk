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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.sampled.AudioFileReader;
import de.fau.cs.jstk.sampled.AudioSource;
import de.fau.cs.jstk.sampled.ThreadedPlayer;
import de.fau.cs.jstk.sampled.ThreadedPlayer.ProgressListener;
import de.fau.cs.jstk.vc.VisualizerSpeechSignal;

public class AudioPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private AudioSource as;
	private BufferedAudioSource bas;	
	private ThreadedPlayer player = new ThreadedPlayer();
	private JProgressBar progressBar = new JProgressBar();
	private ProgressListener progressListener;
	private String mixerName = null;
	
	private VisualizerSpeechSignal vss = new VisualizerSpeechSignal("", bas);
	
	public AudioPanel() {
		super();
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
	
		// set up visualizer
		vss.border_left = 0;
		vss.border_right = 0;
		vss.border_bottom = 0;
		vss.border_top = 0;
				
		vss.showCursorX = true;
		vss.yMax =  0.25;
		vss.yMin = -0.25;
		
		
		progressListener = new ProgressListener() {
			private int count = 0;
			public void resetCount() {
				count = 0;
				progressBar.setValue(0);
			}
			public void bytesPlayed(int b) {
				count += b;	
				
				if (bas != null) {
					// we have 16 bit...
					double x = 100. * count / (bas.getBufferSize() * 2);
					progressBar.setValue((int) x);
				}
			}
		};
		
		player.addProgressListener(progressListener);
		
		progressBar.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) { }
			public void mousePressed(MouseEvent e) { }
			public void mouseExited(MouseEvent e) { }
			public void mouseEntered(MouseEvent e) { }

			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() > 1) {
					if (bas == null)
						return;
					
					int sample = (int)((double) bas.getBufferSize() * e.getX() / progressBar.getWidth());
					
					progressListener.resetCount();
										
					play(sample);
					progressListener.bytesPlayed(sample*2);
				}
			}
		});
		
		vss.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) { }
			public void mousePressed(MouseEvent e) { }
			public void mouseExited(MouseEvent e) { }
			public void mouseEntered(MouseEvent e) { }

			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() > 1) {
					if (bas == null)
						return;
					
					int sample = (int)((double) bas.getBufferSize() * e.getX() / vss.getWidth());
					
					progressListener.resetCount();
					
					play(sample);
					progressListener.bytesPlayed(sample*2);
					e.consume();
				}
			}
		});
	
		vss.setPreferredSize(new Dimension(100, 150));
		vss.setMinimumSize(new Dimension(100, 150));
		
		progressBar.setPreferredSize(new Dimension(100, 30));
		progressBar.setMinimumSize(new Dimension(100, 30));
		
		c.fill = GridBagConstraints.BOTH; c.weightx = 1.0;
		c.weighty = 0.8; c.gridx = 0; c.gridy = 0; add(vss, c);
		c.weighty = 0.2; c.gridx = 0; c.gridy = 1; add(progressBar, c);
		
	}
	
	public AudioPanel(String mixerName) {
		this();
		
		this.mixerName = mixerName;
	}
	
	public void setAudioFile(File file) throws IOException, UnsupportedAudioFileException {
		try {
			stop();
			
			// set up audio
			as = new AudioFileReader(file.getAbsolutePath(), true);
			as.setPreEmphasis(0);
			bas = new BufferedAudioSource(as);
			
			// active wait till buffer is full
			while(bas.stillReading)
				Thread.sleep(10);
			
			if (bas.getBufferSize() == 0)
				throw new IOException("Empty recording!");
			
			// update the visualizer
			vss.xPerPixel = (double) bas.getBufferSize() / vss.getWidth();
			vss.setBufferedAudioSource(bas);
			vss.repaint();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "IO error",
					JOptionPane.ERROR_MESSAGE);
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Threading error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void play(int sample) {
		try {
			stop();
			
			if (bas.getBufferSize() == 0)
				return;
			
			System.err.println("starting playback from 0");
			
			player.setup(mixerName, bas.getReader(sample, bas.getBufferSize()), 0.);
			player.start();

		} catch (LineUnavailableException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Audio error",
					JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "File error",
					JOptionPane.ERROR_MESSAGE);
		} 
	}
	
	public void stop() {
		try {
			if (player.isPlaying()) {
				System.err.println("stopping playback");
				player.stop();
			}
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Thread error",
					JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "IO error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void toggle() {
		if (player.isPlaying()) {
			if (player.isPaused())
				System.err.println("resuming playback");
			else
				System.err.println("pausing playback");
			
			player.pause();
		} else
			play(0);
	}
}
