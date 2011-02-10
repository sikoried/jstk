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
package de.fau.cs.jstk.framed;

import java.io.IOException;

/**
 * Compute the autocorrelation coefficients. Make sure to use a proper window!
 * 
 * @author sikoried
 */
public class AutoCorrelation implements FrameSource {

	/** FrameSource to read from */
	private FrameSource source;

	/** internal read buffer */
	private double[] buf;

	/** frame size */
	private int fs;

	/**
	 * Construct an AutoCorrelation object using the given source to read from.
	 * 
	 * @param source
	 */
	public AutoCorrelation(FrameSource source) {
		this.source = source;
		this.fs = source.getFrameSize();
		this.buf = new double[fs];
	}

	public FrameSource getSource() {
		return source;
	}
	
	public int getFrameSize() {
		return fs;
	}

	/**
	 * Reads from the window and computes the autocorrelation (the lazy way...)
	 */
	public boolean read(double[] buf) throws IOException {
		if (!source.read(this.buf))
			return false;
		
		// compute autocorrelation
		for (int j = 0; j < fs; ++j) {
			buf[j] = 0.;
			for (int i = 0; i < fs - j; ++i)
				buf[j] += this.buf[i + j] * this.buf[i];
		}
		
		return true;
	}
}
