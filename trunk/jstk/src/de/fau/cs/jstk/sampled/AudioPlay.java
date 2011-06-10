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
package de.fau.cs.jstk.sampled;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * Use the AudioPlay class to play back audio files on a certain device. Is used
 * by bin.ThreadedPlayer
 * @author sikoried
 * @see de.fau.cs.jstk.app.ThreadedPlayer
 */
public class AudioPlay implements Runnable {
	private SourceDataLine line;
	
	//private int bs;
	private double [] doubleBuf = null;
	private byte[] byteBuf = null;
	private int fs;
	
	private String mixerName = null;
	private AudioSource source = null;
	
	/// output bit rate
	public static final int BIT_DEPTH = 16;
	
	/// buffer length in msec
	//public static final int BUFLENGTH = 200;
	
	
	/// in seconds
	private double desiredBufDur = 0;
	/// in seconds
	//private double actualBufDur = 0;
	
	private AudioFormat af;
	private boolean lineOpened = false;

	private double scale = 1.;
	
	private Thread playThread = null;
	private boolean requestStop = false;
			
	/**
	 * Creates a new AudioPlay object using the given AudioSource.
	 * @param source
	 * @throws IOException
	 * @throws LineUnavailableException
	 */
	public AudioPlay(AudioSource source) 
		throws IOException, LineUnavailableException {
		this(null, source, 0.0);
	}
	
	public AudioPlay(String mixerName, AudioSource source) throws IOException,
			LineUnavailableException {
		this(mixerName, source, 0.0);
	}
	
	/**
	 * Creates a new AudioPlay object and initializes it. If the desired mixer
	 * is null or can't provide the DataLine, the default mixer is used.
	 * 
	 * @param mixerName name of mixer to use for play back (or null for default)
	 * @param source where to read the data from 
	 * @param desiredBufDur desired buffer length in seconds (= latency). 
	 *        If 0.0, let SourceDataLine.open() decide. 
	 * @param 
	 * @throws IOException
	 * @throws LineUnavailableException
	 */
	public AudioPlay(String mixerName, AudioSource source, double desiredBufDur)
		throws IOException, LineUnavailableException {
		
		this.mixerName = mixerName;
		this.source = source;
		this.desiredBufDur = desiredBufDur;

		initialize();
//		 isn't called when applet window or browser is closed
//		Runtime.getRuntime().addShutdownHook(new Thread(){
//			public void run(){
//				System.err.println("calling tearDown...");
//				try {
//					tearDown();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		});
//		
	}

	
	public void setAudioSource(AudioSource source) throws IOException, LineUnavailableException {
		if (this.source.getSampleRate() != source.getSampleRate()) {
			line.stop();
			line.close();
			this.source = source;
			initialize();
		} else {
			this.source = source;
		}
	}
	
	/** 
	 * Initialize the play back by setting up the outgoing lines. 
	 * @throws IOException
	 * @throws LineUnavailableException
	 */
	private void initialize() throws IOException, LineUnavailableException {
		// standard linear PCM at 16 bit and the available sample rate
		af = new AudioFormat(source.getSampleRate(), BIT_DEPTH, 1, true, false);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
		
		// No mixer specified, use default mixer
		if (mixerName == null) {
			line = (SourceDataLine) AudioSystem.getLine(info);
		} else {
			// mixerName specified, use this Mixer to write to			
			
			Mixer.Info target = null;
			try {
				target = MixerUtil.getMixerInfoFromName(mixerName, false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			
			// If no target, fall back to default line
			if (target != null)
				line = (SourceDataLine) AudioSystem.getMixer(target).getLine(info);
			else{
				System.err.println("mixer not found: " + mixerName + ". Available mixers:");
								
				for (Mixer.Info m : getMixerList(null))
					System.err.println(m.getName());
				line = (SourceDataLine) AudioSystem.getLine(info);
			}
		}
		
		// open line later (see openLine) 
		/*
		if (desiredBufDur != 0){
			int desiredBufSize = (int)Math.round(desiredBufDur * af.getFrameRate())
				* af.getFrameSize();
			line.open(af, desiredBufSize);
			if (line.getBufferSize() != desiredBufSize){
				System.out.println("could not set desiredBufDur = " + desiredBufDur + 
						" which corresponds to a buffer size of " + desiredBufSize + ". Got bufSize = " + 
						line.getBufferSize());
			}
		}
		else
			line.open(af);
		//System.out.println("line.getBufferSize = " + line.getBufferSize());
		line.start();
		
		// init the buffer		
		//bs = (int) (BUFLENGTH * af.getSampleRate() / 1000);
		int bytes = line.getBufferSize();
		byteBuf = new byte [bytes];
		doubleBuf = new double [bytes / af.getFrameSize()];
		fs = af.getFrameSize();
		
		actualBufDur = bytes / af.getFrameSize() / af.getFrameRate();
		
		*/
		
		// System.err.println(String.format("bytes = %d, samples = %d\n", byteBuf.length, doubleBuf.length));
		
		/* -1 because of the assymetry of two's complement.
		 * Example: for 16 bit depth, we need a scale of 32767, not 32768, because otherwise
		 * we cannot represent the double value -1.0.
		 */
		scale = Math.pow(2, BIT_DEPTH - 1) - 1;
		// System.err.println("scale = " + scale);
	}
	
	private void openLine() throws LineUnavailableException{
		lineOpened = true;
		

		try{
			if (desiredBufDur != 0){
				int desiredBufSize = (int)Math.round(desiredBufDur * af.getFrameRate())
				* af.getFrameSize();


				line.open(af, desiredBufSize);

				if (line.getBufferSize() != desiredBufSize){
					System.out.println("could not set desiredBufDur = " + desiredBufDur + 
							" which corresponds to a buffer size of " + desiredBufSize + ". Got bufSize = " + 
							line.getBufferSize());
				}
			}
			else
				line.open(af);
		}		
		// important to catch IllegalArgumentException also!!
		catch (IllegalArgumentException e){
			throw new LineUnavailableException(e.getMessage());
		}

		//System.out.println("line.getBufferSize = " + line.getBufferSize());
		line.start();
		
		// init the buffer		
		//bs = (int) (BUFLENGTH * af.getSampleRate() / 1000);
		int bytes = line.getBufferSize();
		byteBuf = new byte [bytes];
		doubleBuf = new double [bytes / af.getFrameSize()];
		fs = af.getFrameSize();
		
		//actualBufDur = bytes / af.getFrameSize() / af.getFrameRate();
		
		
		
	}
	
	/* we do not know actualBufDur until we actually opened the line 
	public double getActualBufDur(){
		return actualBufDur;
	}
	*/
	
	private static Mixer.Info [] getMixerList(AudioFormat af) {
		return MixerUtil.getMixerList(af, false);
	}
	
	/**
	 * 
	 * @return String of mixerName
	 */
	public String getMixerName(){
		if(mixerName != null)
			return mixerName;
		else
			return "default mixer";
	}

	/**
	 * Close everyting
	 * @throws IOException
	 */
	public void tearDown() throws IOException {
		//No, don't! line.flush();
		// drain is likely to cause problems, too: line.drain();
		line.stop();
		line.close();
		//source.tearDown(); DO NOT tear down the audiosource after play back!!! steidl
	}

	/**
	 * write one frame from data array to audioSource (playback)
	 * @return number of bytes played(written to audioSource) or -1 if audiobuffer is empty
	 * @throws IOException
	 * @throws LineUnavailableException 
	 */
	public int write() throws IOException, LineUnavailableException {		
		int bytes;
		
		// open only here, otherwise we block audio device all the time
		if (!lineOpened)
			openLine();
		
		//bytes = line.available();
		//if (bytes > byteBuf.length)
		//	throw new Error("argh!");
		bytes = byteBuf.length;		
		
		int frames = bytes / fs / 2;
		
		int readFrames = source.read(doubleBuf, frames);
		
		int readBytes = readFrames * fs;
		
		if (readFrames < 0) {
			tearDown();
			return -1;
		}
		
		// set rest to zero
		if (readFrames < frames)
			for (int i = readFrames; i < frames; ++i)
				doubleBuf[i] = 0;
		
		// double -> short conversion
		ByteBuffer bb = ByteBuffer.wrap(byteBuf);
		bb.order(ByteOrder.LITTLE_ENDIAN);
 
		int i;
		for (i = 0; i < frames; i++)					 
			bb.putShort((short)(doubleBuf[i] * scale));		
		
		// System.out.println("that would be available, now that we have fetched the data: " + line.available());
		readFrames = line.write(byteBuf, 0, readBytes);		
		
		return readFrames;
	}

	protected void finalize() throws Throwable {
		try {
			tearDown();
		} finally {
			super.finalize();
		}
	}
	
	@Override
	public void run() {
		// play whole section
		try {
			while (write() > 0) {
				if (requestStop) {
					// System.err.println("AudioPlay forced to stop");
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void play() {
		stop();
		playThread = new Thread(this);
		playThread.start();
	}
	
	public void stop() {
		if ((playThread != null) && playThread.isAlive()) {			
			requestStop = true;
			while (playThread.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			requestStop = false;
		}
	}
	
	public boolean isPlaying() {
		if ((playThread != null) && playThread.isAlive()) {			
			return true;
		}
		return false;
	}

	
	public static final String SYNOPSIS = 
		"usage: sampled.AudioPlay [-m mixer-name] [-f format-string] [file1 ...]\n" +
		"Play back the listed audio files. If required, use the specified mixer\n" +
		"device. Specify a format string if referring to raw data (e.g. t:ssg/16).\n" +
		"WAV-Format should be automatically detected from the file header.\n" +
		"Speex (*.spx) files must end with .spx.";
	
	public static void main(String [] args) throws Exception {
		if (args.length < 1) {
			System.err.println(SYNOPSIS) ;
			System.exit(1);
		}
		
		String mixer = null;
		String format = null;
		
		// scan for arguments
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-m"))
				mixer = args[++i];
			else if (args[i].equals("-f"))
				format = args[++i];
		}
		
		// process files
		for (int i = 0 + (mixer == null ? 0 : 2) + (format == null ? 0 : 2); i < args.length; ++i) {
			String file = args[i];
			System.err.println("Now playing " + file);
			AudioSource reader;
			if (format == null){
				if (file.endsWith(".spx"))
					reader = new SpeexFileReader(new FileInputStream(file));
				else
					reader = new AudioFileReader(file, true);
			}
			else
				reader = new AudioFileReader(file, RawAudioFormat.create(format), true);
			
			reader.setPreEmphasis(false, 1);
			
			AudioPlay play = new AudioPlay(mixer, reader);
			
			// play whole file
			while (play.write() > 0)
				;
		}
	}

}