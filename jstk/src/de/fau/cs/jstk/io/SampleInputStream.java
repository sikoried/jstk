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
package de.fau.cs.jstk.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Read Samples from a binary InputStream.
 * @author sikoried
 *
 */
public class SampleInputStream {
	/** InputStream to read Sample instances from */
	private InputStream is = null;
	
	/**
	 * Initialize a new SampleInputStream on the given InputStream
	 * @param is
	 */
	public SampleInputStream(InputStream is) {
		this.is = is;
	}
	
	private void close() throws IOException {
		is.close();
	}
}
