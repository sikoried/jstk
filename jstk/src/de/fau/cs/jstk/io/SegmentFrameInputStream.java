package de.fau.cs.jstk.io;

import java.io.File;
import java.io.IOException;

/**
 * Read a segment from a FrameInputStream, as for example provided by a 
 * kaldi-style segments file.  Note that start and end are frame numbers.
 * 
 * @author sikoried
 *
 */
public class SegmentFrameInputStream extends FrameInputStream {
	/** Current frame position */
	private int pos = 0;
	
	/** Frame starting position */
	private int start = 0;
	
	/** end frame (exclusive) */
	private int end = Integer.MAX_VALUE;
	
	/**
	 * Create a SegmentFrameInputStream that outputs a segment defined by start
	 * and end (exclusive) frame.
	 * @throws IOException
	 */
	public SegmentFrameInputStream(File file, int start, int end) 
			throws IOException {
		super(file);
		
		this.start = start;
		this.end = end;
		
		seek();
	}
	
	/**
	 * Create a SegmentFrameInputStream that outputs a segment defined by start
	 * and end (exclusive) frame.
	 * @throws IOException
	 */
	public SegmentFrameInputStream(File file, boolean floats, int start, 
			int end) throws IOException {
		super(file, floats);
		
		this.start = start;
		this.end = end;
		
		seek();
	}
	
	/**
	 * Create a SegmentFrameInputStream that outputs a segment defined by start
	 * and end (exclusive) frame.
	 * @throws IOException
	 */
	public SegmentFrameInputStream(File file, boolean floats, int fs, int start,
			int end) throws IOException {
		super(file, floats, fs);
		
		this.start = start;
		this.end = end;
		
		seek();
	}
	
	/** Seek the stream to the start position */
	private void seek() throws IOException {
		if (start > 0) {
			long n = (floats ? start * Float.SIZE/8 : start * Double.SIZE/8);
			is.skip(n);
			pos = start;
		}
	}
	
	/**
	 * Read double frame, return false if EOS
	 */
	@Override
	public boolean read(double [] buf) throws IOException {
		if (pos < end) {
			pos++;
			return super.read(buf);
		} else {
			is.close();
			return false;
		}
	}
	
	/**
	 * Read float frame, return false if EOS
	 */
	@Override
	public boolean read(float [] buf) throws IOException {
		if (pos < end) {
			pos++;
			return super.read(buf);
		} else {
			is.close();
			return false;
		}
	}
}
