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
