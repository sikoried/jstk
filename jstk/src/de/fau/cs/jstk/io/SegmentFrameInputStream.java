package de.fau.cs.jstk.io;

import java.io.File;
import java.io.IOException;

public class SegmentFrameInputStream extends FrameInputStream {
	private int pos = 0;
	private int start = 0;
	private int end = Integer.MAX_VALUE;
	
	public SegmentFrameInputStream(File file, int start, int end) throws IOException {
		super(file);
		
		this.start = start;
		this.end = end;
		
		seek();
	}
	
	public SegmentFrameInputStream(File file, boolean floats, int start, int end) throws IOException {
		super(file, floats);
		
		this.start = start;
		this.end = end;
		
		seek();
	}
	
	public SegmentFrameInputStream(File file, boolean floats, int fs, int start, int end) throws IOException {
		super(file, floats, fs);
		
		this.start = start;
		this.end = end;
		
		seek();
	}
	
	private void seek() throws IOException {
		if (start > 0) {
			long n = (floats ? start * Float.SIZE/8 : start * Double.SIZE/8);
			is.skip(n);
			pos = start;
		}
	}
	
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
