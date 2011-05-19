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
import java.io.FileInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;

import org.xiph.speex.spi.Speex2PcmAudioInputStream;
import org.xiph.speex.spi.SpeexAudioFileReader;

/**
 * currently unused. see SpeexPlayer.
 * @author hoenig
 *
 */
public class SpeexDecoder {
	public static void main(String []args) throws Exception{
		SpeexAudioFileReader reader = new SpeexAudioFileReader();
		
		InputStream is = new BufferedInputStream(new FileInputStream(args[0]));

		if (!is.markSupported())
			throw new Error("mark unsupported");
		
		is.mark(100000);
		AudioFileFormat af = reader.getAudioFileFormat(is);
		
		System.err.println(af.toString());
		
		is.reset();		
					
		Speex2PcmAudioInputStream pcmStream = new Speex2PcmAudioInputStream(is, af.getFormat(), 0);
		
		//byte [] buffer = new byte[1000]; 
		while(true){
			int readByte = pcmStream.read();
			if (readByte == -1)
				break;
			System.out.write(readByte);			
		}		
	}
}
