package de.fau.cs.jstk.sampled;

import java.io.IOException;

import javax.sound.sampled.AudioInputStream;

/**
 * AudioInputStream that branches off the data that is being read to another consumer,
 * e.g. to visualize loudness. A bit like the unix "tee" command.
 * @author hoenig
 *
 */
public class AudioInputStreamListener extends AudioInputStream{
	
	private Consumer consumer;
	
	public interface Consumer{
		void consume(byte [] buf, int off, int len);		
	}
	
	//AudioInputStream ais;	
	public AudioInputStreamListener(AudioInputStream ais,
			Consumer consumer){
		super(ais, ais.getFormat(), ais.getFrameLength());
		this.consumer = consumer;
				
	}
	
	public int read(byte [] buf, int off, int len)throws IOException{
		int readBytes = super.read(buf, off, len);
		
		//System.err.println("readBytes = " + readBytes);
		consumer.consume(buf, off, len);
		
		return readBytes;	
	}
	
	public int read(byte [] buf) throws IOException{
		return read(buf, 0, buf.length);
	}
}
