package de.fau.cs.jstk.sampled;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * InputStream that branches off the data that is being read to another consumer (OutputStream),
 * much like the unix "tee" command.
 * 
 * @author hoenig
 */
public class TeeInputStream extends InputStream{
	InputStream is;
	OutputStream consumer;
	
	public TeeInputStream(InputStream is,
			OutputStream consumer){
		this.is = is;
		this.consumer = consumer;		
	}
	
	public int read(byte [] buf, int off, int len) throws IOException{
		int readBytes = is.read(buf, off, len);
		consumer.write(buf, off, readBytes);
		// TODO: ?
		consumer.flush();
		return readBytes;
	}
	
	public int read(byte [] buf) throws IOException{
		return read(buf, 0, buf.length);
	}
	
	public void flush() throws IOException{
	  consumer.flush();
	}
	
	public void close() throws IOException{
	  flush();

	  is.close();
	  consumer.close();
	}
	
	public int read() throws IOException{
		byte [] buf = new byte[1];
		read(buf, 0, 1);
		return buf[0];
	}
}
