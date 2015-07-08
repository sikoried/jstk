/*
	Copyright (c) 2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer

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
package de.fau.cs.jstk.sampled.filters;

import java.io.IOException;
import java.util.Arrays;

import de.fau.cs.jstk.sampled.AudioSource;

/**
 * The IIRFilter uses the precomputed a and b arrays (e.g. from Matlab). Make 
 * sure the coefficients match the sampling rate!
 * 
 * The coefficients are indexed in matlab style, starting at 0. The result is
 * y[n] = b[0]*x[n] + b[1]*x[n-1] + ... + b[nb]*x[n-nb] - a[2]*y[n-1] - ... - a[na]*y[n-na]
 * 
 * If a[0] not equal 1, the filter coefficients are normalized by a[0]
 * 
 * @author sikoried
 */
public class IIRFilter implements AudioSource {

	protected AudioSource source;
	
	/** local signal buffer to read from source */
	private double [] buf = new double [0];
	
	/** i/o pointer for input signal ringbuffer */
	private int px;
	
	/** i/o pointer for output signal ringbuffer */ 
	private int py;
	
	/** input signal ringbuffer */
	private double [] xv;
	
	/** output signal ringbuffer */
	private double [] yv;
	
	/** filter coefficients A (applied to prevously filtered signal) */
	private double [] a;
	
	/** filter coefficients B (applied to input signal */
	private double [] b;
	
	/**
	 * Enforce subclasses to call IIRFilter(AudioSource source)
	 */
	@SuppressWarnings("unused")
	private IIRFilter() {
		
	}
	
	/**
	 * Constructor to be called from subclasses
	 * @param source
	 */
	protected IIRFilter(AudioSource source) {
		this.source = source;
	}

	/**
	 * Generate a new IIRFilter for the given AudioSource and coefficients b and
	 * a in matlab style.
	 * 
	 * @param source
	 * @param b
	 * @param a note that a[0] is never read (cf. Matlab)
	 */
	public IIRFilter(AudioSource source, double [] b, double [] a) {
		this(source);
		setCoefficients(b, a);	
	}
	
	/**
	 * Set the filter coefficients stored in Matlab style. If a[0] not equal 1,
	 * the filter coefficients are normalized by a[0].
	 * 
	 * @param b
	 * @param a
	 */
	public void setCoefficients(double [] b, double [] a) {
		this.b = b;
		this.a = a;
		
		xv = new double [b.length + 1];
		yv = new double [a.length + 1];
		
		if (a[0] != 1.) {
			for (int i = 1; i < a.length; ++i)
				a[i] /= a[0];
			for (int i = 0; i < b.length; ++i)
				b[i] /= a[0];
		}
		
		// we need to make sure that the px and py are correct, in case the
		// arrays got smaller!
		if (px >= xv.length)
			px = 0;
		if (py >= yv.length)
			py = 0;
	}
	
	public int read(double [] buf) throws IOException {
		return read(buf, buf.length);
	}

	public int read(double [] buf, int length) throws IOException {
		// validate buffer
		if (this.buf.length != buf.length)
			this.buf = new double [buf.length];
		
		// read the data
		int r = source.read(this.buf, length);
		
		// apply the filter
		for (int i = 0; i < r; ++i) {
			// get the new sample
			xv[px] = this.buf[i];
						
			// compute the output
			buf[i] = b[0] * xv[px];
			for (int j = 1; j < b.length; ++j)
				buf[i] += b[j] * xv[(px - j + xv.length) % xv.length];
			
			for (int j = 1; j < a.length; ++j)
				buf[i] -= a[j] * yv[(py - j + yv.length) % yv.length];
			
			// save the result
			yv[py] = buf[i];
			
			// increment the index of the ring buffer
			px = (px + 1) % xv.length;
			py = (py + 1) % yv.length;
		}
		
		return r;
	}

	public int getSampleRate() {
		return source.getSampleRate();
	}

	public double getPreEmphasis() {
		return source.getPreEmphasis();
	}

	public void setPreEmphasis(double a) {
		source.setPreEmphasis(a);
	}

	public void tearDown() throws IOException {
		source.tearDown();
	}
	
	public String toString() {
		return "b = " + Arrays.toString(b) + " a = " + Arrays.toString(a);
	}
}
