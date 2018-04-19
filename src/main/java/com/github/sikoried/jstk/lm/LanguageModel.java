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
package com.github.sikoried.jstk.lm;

import com.github.sikoried.jstk.arch.TreeNode;

/**
 * Up to now, the LanguageModel interface enforces only the generateNetwork 
 * function. This is intended for small sized vocabularies that can explicitly 
 * be modeled as a graph. For larger vocabularies or more complex grammars, 
 * refer to the WFST packages.
 *  
 * @author sikoried
 */
public interface LanguageModel {
	/**
	 * Generate the LST network from the given TokenTrees and build silence
	 * models as requested
	 * @return Root node of the LST network
	 */
	public TreeNode generateNetwork();
}
