/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		Tobias Bocklet

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
package de.fau.cs.jstk.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import javax.sound.sampled.LineUnavailableException;

import de.fau.cs.jstk.sampled.AudioFileReader;
import de.fau.cs.jstk.sampled.AudioPlay;
import de.fau.cs.jstk.sampled.AudioSource;
import de.fau.cs.jstk.sampled.RawAudioFormat;

public class ThreadedPlayer implements Runnable {

	/**
	 * A StateListener can be used to get notified by the ThreadedPlayer
	 * to react on its state changes without actively polling the player's 
	 * state.
	 * 
	 * @author sikoried
	 */
	public interface StateListener {
		public void playerStarted(ThreadedPlayer instance);
		public void playerStopped(ThreadedPlayer instance);
		public void playerPaused(ThreadedPlayer instance);
	}
	
	private List<StateListener> dependents = new LinkedList<StateListener>();
	
	private Thread thread = null;
	
	private AudioPlay player;

	/** is the player paused? */
	private volatile boolean paused = false;

	/** is a stop requested? */
	private volatile boolean stopRequested = false;

	/** is the playback finished? indicator to the player thread */
	private volatile boolean finished = true;
	
	/**
	 * Create a threaded player using the given audio player.
	 * @param player
	 */
	public ThreadedPlayer(AudioPlay player) {
		this.player = player;		
	}

	/**
	 * Setup the ThreadedPlayer to play the given audio file.
	 * @param mixer null for default mixer (not advised)
	 * @param format
	 * @param file
	 * @throws IOException
	 */
	public void setup(String mixer, RawAudioFormat format, File file) throws IOException {
		setup(mixer, new AudioFileReader(file.getCanonicalPath(), format, true), 0.0);
		/*
		try {
			if (isPlaying())
				stop();
			
			player = new AudioPlay(mixer, new AudioFileReader(file.getCanonicalPath(), format, true));
		} catch (LineUnavailableException e) {
			throw new IOException("LineUnavailableException: " + e.toString());
		}
		*/
	}
	
	public double setup(String mixer, RawAudioFormat format, InputStream inputStream, double desiredBufDur) throws IOException {
		return setup(mixer, new AudioFileReader(inputStream, format, true), desiredBufDur);
		/*
		try {
			if (isPlaying())
				stop();			
			player = new AudioPlay(mixer, new AudioFileReader(inputStream, format, true), desiredBufDur);
		} catch (LineUnavailableException e) {
			throw new IOException("LineUnavailableException: " + e.toString());
		}
		*/		
		
	}
	
	/**
	 * Setup the ThreadedPlayer to play the given audio data.
	 * @param mixer null for default mixer (not advised)
	 * @param format
	 * @param data
	 * @throws IOException
	 */
	public double setup(String mixer, RawAudioFormat format, byte [] data) throws IOException {
		return setup(mixer, new AudioFileReader(format, data), 0.0);
		/*
		try {
			if (isPlaying())
				stop();
			
			player = new AudioPlay(mixer, new AudioFileReader(format, data));
		} catch (LineUnavailableException e) {
			throw new IOException("LineUnavailableException: " + e.toString());
		}
		*/
	}
	
	/**
	 * Setup the ThreadedPlayer to play from the given AudioSource.
	 * @param mixer null for default mixer (not advised)
	 * @param format
	 * @param data
	 * @throws IOException
	 */
	public double setup(String mixer, AudioSource source, double desiredBufDur) throws IOException {
		try {
			if (isPlaying())
				stop();
			
			player = new AudioPlay(mixer, source, desiredBufDur);
		} catch (LineUnavailableException e) {
			throw new IOException("LineUnavailableException: " + e.toString());
		}
		return player.getActualBufDur();
	}
	
	
	/** 
	 * (un)pause the recording; on pause, the recording continues, but is not
	 * saved to the file.
	 */
	public void pause() {
		if (!paused)	
			notifyPause();
		else
			notifyStart();
	
		paused = !paused;
	}

	/**
	 * Stop the playback. This method blocks until the playback thread died.
	 */
	public void stop() {
		stopRequested = true;
		try {
			if (thread != null)
				thread.join();
			else if (player != null)
				player.tearDown();
			thread = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Start the playback.
	 * @throws IOException
	 */
	public void start() throws IOException {
		if (thread != null)
			throw new IOException("Thread is still playing!");

		thread = new Thread(this);
		thread.start();
	}

	public void run() {
		try {
			finished = false;
			notifyStart();
			
			while (!stopRequested) {
				if (paused) {
					// FIXME: calling yield() in a loop doesn't seem to be a good idea
					// see http://forums.sun.com/thread.jspa?threadID=167194
					// -> sleep one buffer length?
					// or rather wait() here and notify() in pause()?
					Thread.yield();
					continue;
				}
				if (player.write() <= 0)
					break;
			}
			
			// free the device
			player.tearDown();
		} catch (IOException e) {
			System.err.println("ThreadedPlayer.run(): I/O error: " + e.toString());
		} catch (Exception e) {
			System.err.println("ThreadedPlayer.run(): " + e.toString());
		} finally {
			// note to main thread
			thread = null;
			finished = true;
			stopRequested = false;
			paused = false;
			notifyStop();
		}
		
	}
	
	public boolean isPlaying() {
		return thread != null && !finished;
	}
	
	public boolean isPaused() {
		return paused;
	}

	/**
	 * Register a StateListener with the ThreadedRecorder to get notified if the
	 * state changed.
	 * @param client
	 */
	public void addStateListener(StateListener client) {
		dependents.add(client);
	}
	
	/**
	 * De-register a StateListener to allow the garbage collection to work properly
	 * @param client
	 */
	public void removeStateListener(StateListener client) {
		for (int i = 0; i < dependents.size(); ++i)
			if (dependents.get(i) == client) {
				dependents.remove(i);
				break;
			}
	}
	
	private void notifyStart() {
		for (StateListener s : dependents)
			s.playerStarted(this);
	}
	
	private void notifyPause() {
		for (StateListener s : dependents)
			s.playerPaused(this);
	}
	
	private void notifyStop() {
		for (StateListener s : dependents)
			s.playerStopped(this);
	}
	
	public static void main(String [] args) throws Exception {
		if (args.length < 2) {
			System.err.println("usage: bin.ThreadedPlayer mixer-name <file1 ...>");
			System.exit(1);
		}
		
		for (int i = 1; i < args.length; ++i) {
			String file = args[i];
			System.err.println("Hit [ENTER] to start playing " + file);
			System.in.read();
			
			ThreadedPlayer play = new ThreadedPlayer(new AudioPlay(args[0], new AudioFileReader(file, RawAudioFormat.getAudioFormat("ssg/16"), true)));
			play.start();
		}
	}
}