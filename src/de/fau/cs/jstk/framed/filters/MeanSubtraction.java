package de.fau.cs.jstk.framed.filters;

import java.io.IOException;

import de.fau.cs.jstk.io.FrameSource;

public class MeanSubtraction implements FrameSource {
	private FrameSource source;
	
	public MeanSubtraction(FrameSource source) {
		this.source = source;
	}
	
	public boolean read(double[] buf) throws IOException {
		boolean ret = source.read(buf);
		
		if (!ret)
			return false;
		
		double m = 0;
		for (double d : buf)
			m += d;
		
		for (int i = 0; i < buf.length; ++i)
			buf[i] -= m;
		
		return true;
	}

	public int getFrameSize() {
		return source.getFrameSize();
	}

	public FrameSource getSource() {
		return source;
	}

}
