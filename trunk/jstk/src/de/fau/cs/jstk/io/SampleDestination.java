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

import de.fau.cs.jstk.stat.Sample;

/**
 * Destination to write Samples to
 * 
 * @author sikoried
 */
public interface SampleDestination {
	/**
	 * Write the given Sample
	 * @param s
	 * @throws IOException
	 */
	void write(Sample s) throws IOException;
	
	/**
	 * Flush the destination.
	 * @throws IOException
	 */
	void flush() throws IOException;
	
	/**
	 * Flush and close the destination.
	 * @throws IOException
	 */
	void close() throws IOException;
}
