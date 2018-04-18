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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.xiph.speex.spi.Speex2PcmAudioInputStream;
import org.xiph.speex.spi.SpeexAudioFileReader;

import de.fau.cs.jstk.sampled.RawPlayer;

/**
 * provides a static method for reading a speex-file (*.spx, see http://www.speex.org)
 * as an AudioInputStream, using org.xiph.speex.spi.Speex2PcmAudioInputStream
 * @author hoenig
 *
 */
public class SpeexPlayer {
	private static final int OGG_HEADERSIZE = 27;
	private static final int SPEEX_HEADERSIZE = 80;
	private static final int max_headersize = 3 * OGG_HEADERSIZE + SPEEX_HEADERSIZE + 256 + 256 + 2;
	
	public static AudioInputStream getAudioInputStream(InputStream is)
	throws UnsupportedAudioFileException, IOException{
		
		// so markSupported is true
		BufferedInputStream bis = new BufferedInputStream(is);	

		if (!bis.markSupported())
			throw new Error("BufferedInputStream: mark not supported !?!");

		SpeexAudioFileReader reader = new SpeexAudioFileReader();

		bis.mark(max_headersize);

		AudioFileFormat speexFormat = null;

		speexFormat = reader.getAudioFileFormat(bis);

		bis.reset();

		Speex2PcmAudioInputStream pcmStream = 
			new Speex2PcmAudioInputStream(bis, speexFormat.getFormat(), 0);

		AudioInputStream ais = new AudioInputStream(pcmStream,
				// speexFormat.getFormat() reports wrong *FrameRate*, e.g. 50 (Hz).
				// so we invent our own AudioFormat:
				new AudioFormat(speexFormat.getFormat().getSampleRate(), 16, 1, true, false),
				//length: not known: seems ok to pass -1
				-1);
		return ais;
	}
	

	public static void main(String [] args){
		
		Mixer.Info [] availableMixers = AudioSystem.getMixerInfo();
		
		for (Mixer.Info m : availableMixers)
			System.err.println(m.getName());

		System.err.println("usage: < IN.spx [mixer]");	

		String mixer = null;

		if (args.length > 0)
			mixer = args[0];
		
		AudioInputStream ais;
		try {
			ais = SpeexPlayer.getAudioInputStream(System.in);
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		System.err.println("format = " + ais.getFormat());

		final RawPlayer player = new RawPlayer(ais, mixer, 0.1);

		// "Intel [plughw:0,0]" works much better than "Java Sound Audio Engine". 
		// And the latter from time to time refuses to put out anything
		//player.enableStressTest(0.98);

		player.start();
		

		player.join();
		System.err.println("joined");
		
	}

}
