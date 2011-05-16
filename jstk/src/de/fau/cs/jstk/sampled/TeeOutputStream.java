package de.fau.cs.jstk.sampled;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream that branches off the data that is being read to another consumer (OutputStream),
 * much like the unix "tee" command.
 * 
 * @author hoenig
 */
public class TeeOutputStream extends OutputStream{
	OutputStream os;
	OutputStream consumer;
	
	public TeeOutputStream(OutputStream os,
			OutputStream consumer){
		this.os = os;
		this.consumer = consumer;		
	}
	
	public void write(byte [] buf, int off, int len) throws IOException{
		os.write(buf, off, len);
		consumer.write(buf, off, len);
		// TODO: ?
		consumer.flush();		
	}
	
	public void write(byte [] buf) throws IOException{
		write(buf, 0, buf.length);
	}
	
	public void flush() throws IOException{
	  consumer.flush();
	}
	
	public void close() throws IOException{
	  flush();

	  os.close();
	  consumer.close();
	}
	
	public void write(int b) throws IOException{
		byte [] buf = new byte[1];
		buf[0] = (byte)b;
		write(buf, 0, 1);		
	}
}
