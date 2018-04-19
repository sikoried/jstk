/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		Tobias Bocklet
		Florian Hoenig
		Stefan Steidl

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

package com.github.sikoried.jstk.segmented;


	/**
	 * Structure for Syllable Nucleus. 
	 * 
	 *
	 */
    public class SyllableNucleus {
	/** index of maximum in the data */
	public int i_max = -1;
	/** index of left minimum in the data */
	public int i_min_l = -1;
	/** index of right minimum in the data */
	public int i_min_r = -1;
	/** index of left syllable nucleus limit in the data */
	public int i_snl_l = -1;
	/** index of right syllable nucleus limit in the data */
	public int i_snl_r = -1;

	/** absolute value of the maximum */
	public double max = -1.;
	/** absolute value of the left minimum */
	public double min_l = -1.;
	/** absolute value of the right minimum */
	public double min_r = -1.;
	/** absolute value of the left syllable nucleus limit */
	public double snl_l = -1.;
	/** absolute value of the right syllable nucleus limit */
	public double snl_r = -1.;

	/** maximum energy value in this nucleus */
	public double max_e = -1.;
	/** integrated energy of this nucleus */
	public double sum_e = -1.;

	public double frequency = -1.;
	public double duration_feature = -1.;
	public double loudness_feature = -1.;

	/**
	 * the band in which the nucleus appeared
	 * 0: 100Hz - 300Hz
	 * 1: 300Hz - 2300Hz
	 * 2: 2300Hz - 5000Hz
	 */
	public int band = -1;


	
	/**
	 * calculates the distance, in frames, between this nucleus and one
	 * left of it
	 * @param left The syllable nucleus to the left of this one
	 * @return distance to the left nucleus
	 */
	public int getDistance(SyllableNucleus left) {
	    return this.i_snl_l - left.i_snl_r;
	}

	/**
	 * calculates the duration, in frames, of this nucleus
	 * @return this nucleus' duration in frames
	 */
	public int getFramesDuration() {
	    return i_snl_r - i_snl_l;
	}

	@Override
	public String toString() {
	    return
		"i_min_l = " + i_min_l + ", min_l = " + min_l + "\n" +
		"i_snl_l = " + i_snl_l + ", snl_l = " + snl_l + "\n" +
		"i_max   = " + i_max   + ", max   = " + max + "\n" +
		"i_snl_r = " + i_snl_r + ", snl_r = " + snl_r + "\n" +
		"i_min_r = " + i_min_r + ", min_r = " + min_r + "\n" +
		"max_e   = " + max_e + "\n" +
		"sum_e   = " + sum_e;
	}
    
	
}
