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
public class AudioPlay {
	private SourceDataLine line;
	
	private int bs;
	private double [] buf = null;
	
	private String mixerName = null;
	private AudioSource source = null;
	
	/// output bit rate
	public static final int BITRATE = 16;
	
	/// buffer length in msec
	//public static final int BUFLENGTH = 200;
	
	
	private double desiredBufDur = 0;
	private double actualBufDur = 0;

	private double scale = 1.;
			
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
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				System.err.println("calling tearDown...");
				try {
					tearDown();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	
	/** 
	 * Initialize the play back by setting up the outgoing lines. 
	 * @throws IOException
	 * @throws LineUnavailableException
	 */
	private void initialize() throws IOException, LineUnavailableException {
		// standard linear PCM at 16 bit and the available sample rate
		AudioFormat af = new AudioFormat(source.getSampleRate(), BITRATE, 1, true, false);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
		
		// No mixer specified, use default mixer
		if (mixerName == null) {
			line = (SourceDataLine) AudioSystem.getLine(info);
		} else {
			// mixerName specified, use this Mixer to write to 
			Mixer.Info [] availableMixers = AudioSystem.getMixerInfo();
			Mixer.Info target = null;
			for (Mixer.Info m : availableMixers)
				if (m.getName().trim().equals(mixerName))
					target = m;
			
			// If no target, fall back to default line
			if (target != null)
				line = (SourceDataLine) AudioSystem.getMixer(target).getLine(info);
		}
		
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
		bs = line.getBufferSize();
		buf = new double [bs];
		
		actualBufDur = bs / af.getFrameSize() / af.getFrameRate();
		
		scale = Math.pow(2, BITRATE - 1);
	}
	
	public double getActualBufDur(){
		return actualBufDur;
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
	 */
	public int write() throws IOException {
		int count = source.read(buf);
		
		if (count <= 0) {
			tearDown();
			return 0;
		}
		
		// set rest to zero
		if (count < bs)
			for (int i = count; i < bs; ++i)
				buf[i] = 0;
		
		// double -> short conversion
		ByteBuffer bb = ByteBuffer.allocate(bs * Short.SIZE/8);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		// FIXME: -1 will be mapped to -32768 which is not a short! 
		for (double d : buf) 
			bb.putShort((short)(d * scale));
		
		byte [] outgoing = bb.array();
		count = line.write(outgoing, 0, outgoing.length);
		
		return count;
	}

	protected void finalize() throws Throwable {
		try {
			tearDown();
		} finally {
			super.finalize();
		}
	}
	
	public static final String SYNOPSIS = 
		"usage: sampled.AudioPlay [-m mixer-name] [-f format-string] [file1 ...]\n" +
		"Play back the listed audio files. If required, use the specified mixer\n" +
		"device. Specify a format string if referring to raw data (e.g. t:ssg/16)";
	
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
		for (int i = (mixer == null ? 0 : 2) + (format == null ? 0 : 2); i < args.length; ++i) {
			System.err.println("Now playing " + args[i]);
			AudioPlay play = (format == null ? 
				new AudioPlay(mixer, new AudioFileReader(args[i], true)) :
				new AudioPlay(mixer, new AudioFileReader(args[i], RawAudioFormat.create(format), true)));
			
			// play whole file
			while (play.write() > 0)
				;
		}
	}
}