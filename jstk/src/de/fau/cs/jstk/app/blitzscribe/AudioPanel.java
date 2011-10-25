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

public class AudioPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private AudioSource as;
	private BufferedAudioSource bas;	
	private ThreadedPlayer player = new ThreadedPlayer();
	private JProgressBar progressBar = new JProgressBar();
	private ProgressListener progressListener;
	
	public AudioPanel() {
		super();
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
	
		
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
					progressListener.bytesPlayed(sample*2);
					
					play(sample);
				}
			}
		});
	
		progressBar.setPreferredSize(new Dimension(100, 30));
		c.fill = GridBagConstraints.BOTH; c.weightx = 1.0; c.weighty = 1.0; 
		c.gridx = 0; c.gridy = 2; add(progressBar, c);
	}
	
	public void setAudioFile(File file) throws IOException, UnsupportedAudioFileException {
		try {
			stop();

			// set up audio
			System.err.println("setting up buffered audio source");
			as = new AudioFileReader(file.getAbsolutePath(), true);
			as.setPreEmphasis(false, 0);
			bas = new BufferedAudioSource(as);
			
			// active wait till buffer is full
			while(bas.stillReading)
				Thread.sleep(100);
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
			
			System.err.println("starting playback from 0");
			
			player.setup(null, bas.getReader(sample, bas.getBufferSize()), 0.);
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
