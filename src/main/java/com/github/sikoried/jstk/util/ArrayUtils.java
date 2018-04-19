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
package com.github.sikoried.jstk.util;


/**
 * 
 * 
 * @author hoenig
 *
 */
public class ArrayUtils {
	/**
	 * an interface for classes that have a *public* clone() method
	 * @author hoenig
	 */
	public interface PubliclyCloneable extends Cloneable{
		/**
		 * @return a deep copy of this object
		 */
		public PubliclyCloneable clone();// throws CloneNotSupportedException;
	}
	
	/**
	 * clone an array of PubliclyCloneable implementers by calling clone() for each element 
	 * @throws CloneNotSupportedException 
	 */
	@SuppressWarnings("unchecked")
	public static <T extends PubliclyCloneable> T[] arrayClone(T[] orig){// throws CloneNotSupportedException{
		// shallow copy:
		T [] ret = orig.clone();

		// deep copies:
		int i;
		for (i = 0; i < orig.length; i++)
			ret[i] = (T) orig[i].clone();
		return ret;
	}
}
