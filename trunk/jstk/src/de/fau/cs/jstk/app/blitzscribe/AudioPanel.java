package de.fau.cs.jstk.app.blitzscribe;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.fau.cs.jstk.io.BufferedAudioSource;
import de.fau.cs.jstk.sampled.AudioFileReader;
import de.fau.cs.jstk.sampled.AudioSource;
import de.fau.cs.jstk.sampled.ThreadedPlayer;
import de.fau.cs.jstk.sampled.ThreadedPlayer.StateListener;
import de.fau.cs.jstk.vc.VisualizerSpeechSignal;

public class AudioPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private AudioSource as;
	private BufferedAudioSource bas;
	
	private VisualizerSpeechSignal asv = new VisualizerSpeechSignal("blub", null);
	
	private JButton btnPlay = new JButton("play");
	private JButton btnStop = new JButton("stop");
	
	private ThreadedPlayer player = new ThreadedPlayer(); 
	
	public AudioPanel() {
		super();
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		btnPlay.setEnabled(false);
		btnStop.setEnabled(false);
		
		player.addStateListener(new StateListener() {
			public void playerStopped(ThreadedPlayer instance) {
				btnStop.setEnabled(false);
				btnPlay.setEnabled(true);
				btnPlay.setText("play");
				btnStop.setText("stop");
			}
			public void playerStarted(ThreadedPlayer instance) {
				btnPlay.setEnabled(true);
				btnPlay.setText("pause");
				btnStop.setEnabled(true);
				btnStop.setText("stop");
			}
			public void playerPaused(ThreadedPlayer instance) {
				btnPlay.setEnabled(true);
				btnPlay.setText("play");
				btnStop.setText("stop");
			}
		});
		
		// listeners
		btnPlay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (player.isPlaying())
					player.pause();
				else
					play(0);
			}
		});
		
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});
		
		asv.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) { }
			public void mousePressed(MouseEvent e) { }
			public void mouseExited(MouseEvent e) { }
			public void mouseEntered(MouseEvent e) { }

			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() > 1) {
					if (bas == null)
						return;						
					
					double x = e.getX() / asv.getWidth();
					play((int)(x * bas.getBufferSize()));
				}
			}
		});
		
		c.gridx = 0; c.gridy = 0; c.weightx = 0.9; c.weighty = 1.0; add(asv, c);
		c.gridx = 1; c.gridy = 0; c.weightx = 0.1; c.weighty = 0.5; add(btnPlay, c);
		c.gridx = 1; c.gridy = 1; c.weightx = 0.1; c.weighty = 0.5; add(btnStop, c);
	}
	
	public void setAudioFile(File file) throws IOException, UnsupportedAudioFileException {
		try {
			if (player.isPlaying())
				player.stop();

			as = new AudioFileReader(file.getAbsolutePath(), true);
			as.setPreEmphasis(false, 0);
			bas = new BufferedAudioSource(as);
			asv.setBufferedAudioSource(bas);

			btnStop.setEnabled(false);
			btnPlay.setText("play");
			btnPlay.setEnabled(true);

			asv.repaint();
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Thread error",
					JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "IO error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void play(int sample) {
		try {
			if (player.isPlaying())
				player.stop();
			
			player.setup(null, bas.getReader(sample, bas.getBufferSize()), 0.);
			player.start();

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
	
	public void stop() {
		try {
			if (player.isPlaying())
					player.stop();
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Thread error",
					JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "IO error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
}
