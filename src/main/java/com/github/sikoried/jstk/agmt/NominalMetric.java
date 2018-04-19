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
package com.github.sikoried.jstk.agmt;

/**
 * Nominal metric. Used for binary decisions: Either match or not. Can
 * be inverted to result in 0 when a and b match.
 * 
 * @author sikoried
 */
public class NominalMetric implements Metric {
	private boolean invert = false;
	
	/**
	 * Construct a standard nominal metric. a == b <=> w = 1
	 */
	public NominalMetric() {
		
	}
	
	/**
	 * Construct a nominal metric with optional invertation.
	 */
	public NominalMetric(boolean invert) {
		this.invert = invert;
	}
	
	public double weight(double a, double b) {
		double w = 0;
		if (a == b)
			w = 1;
		else
			w = 0;
		
		if (invert)
			return 1 - w;
		else
			return w;
	}
	
	public String toString() {
		return "w(a,b) = 1 <=> a != b; w(a,b) = 0 <=> a == b";
	}
}
