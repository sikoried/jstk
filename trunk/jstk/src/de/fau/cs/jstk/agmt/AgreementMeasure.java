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
 * An AgreementMeasure computes a measure of agreement between raters and their
 * ratings. The data needs to be organized by raters and ratings.
 * @author sikoried
 *
 */
public interface AgreementMeasure {
	/**
	 * Compute the agreement with respect to the data.
	 * @param data [rater][subject] = rating
	 * @return
	 */
	public double agreement(double [][] data);
	
	/**
	 * Return the metric used to compute the agreement.
	 * @return
	 */
	public Metric getMetric();
}
