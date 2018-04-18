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
package de.fau.cs.jstk.agmt;

public abstract class Utils {
	/**
	 * Add a rater to a existing group of raters.
	 * @param data existing rater set
	 * @param rater new rater to add
	 * @return copy of data arrsy plus new rater
	 */
	public static double [][] addRater(double [][] data, double [] rater) {
		double [][] dnew = new double [data.length+1][rater.length];
		for (int i = 0; i < data.length; ++i)
			for (int j = 0; j < data[i].length; ++j)
				dnew[i][j] = data[i][j];
		for (int i = 0; i < data[0].length; ++i)
			dnew[data.length][i] = rater[i];
		return dnew;
	}
}
