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
 * Metrics proposed by Cicchetti in Cicchetti76:AIR. Possible modes: ABSOLUTE and SQUARE
 * 
 * @author sikoried
 */
public class CicchettiMetric implements Metric {
	double c = 1;
	private int mode;

	public static final int ABSOLUTE = 0;
	public static final int SQUARE = 1;

	public CicchettiMetric(int mode) {
		if (mode == ABSOLUTE)
			this.mode = ABSOLUTE;
		else if (mode == SQUARE)
			this.mode = SQUARE;
		else
			this.mode = ABSOLUTE;
	}

	public double weight(double a, double b) {
		if (mode == ABSOLUTE)
			return (a == b ? 1. : (1. - Math.abs((a - b) / (c - 1.))));
		else
			return (a == b ? 1. : (1. - Math.pow((a - b) / (c - 1.), 2)));
	}

	public String toString() {
		if (mode == ABSOLUTE)
			return "(a == b ? 1. : (1. - Math.abs((a-b)/(" + c + "))))";
		else
			return "(a == b ? 1. : (1. - Math.pow((a-b)/(" + c + "), 2)))";
	}
}
