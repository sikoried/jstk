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

/**
 * Transform a series of real valued measures into fixed categories.
 * @author sikoried
 */
public class Categorizer {
	/**
	 * Categorize into given classes using lower bounds.
	 * @param data real valued measures
	 * @param lowerBounds lower bounds to categorize
	 * @return new allocated array containing categorized data
	 */
	public static double [] categorize(double [] data, double [] lowerBounds) {
		double [] cat = new double [data.length];
		for (int i = 0; i < data.length; ++i) {
			for (int c = 0; c < lowerBounds.length; c++)
				if (data[i] > lowerBounds[c])
					cat[i] = lowerBounds.length - c + 1;
		}		
		return cat;
	}
	/**
	 * Categorize into nc classes depending on the min and max in the data
	 * @param data real valued measures
	 * @param nc number of classes
	 * @return new allocated array containing categorized data
	 */
	public static double [] categorizeEquidistant(double [] data, int nc) {
		double [] trans = new double[nc-1];
		
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		for (double d : data) {
			if (d < min)
				min = d;
			if (d > max)
				max = d;
		}
		
		double range = max - min;
		
		for (int i = 0; i < trans.length; ++i)
			trans[i] = min + (i+1)*(range/(1.+nc));
		
		return categorize(data, trans);
	}
}
