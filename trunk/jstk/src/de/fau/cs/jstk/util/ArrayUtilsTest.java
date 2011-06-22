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
package de.fau.cs.jstk.util;

import org.junit.Assert;
import org.junit.Test;

import de.fau.cs.jstk.util.ArrayUtils.PubliclyCloneable;

public class ArrayUtilsTest {
	public ArrayUtilsTest(){};
	
	private class Bla implements PubliclyCloneable{
		private int i;
		
		public Bla(int i){
			this.setI(i);
		}
		
		@Override
		public Bla clone(){
			return new Bla(getI());			
		}

		public void setI(int i) {
			this.i = i;
		}

		public int getI() {
			return i;
		}
		
	}
	
	@Test
	public void arrayCloneTest(){
		Bla [] orig = new Bla [] {new Bla(1)};
				
		// deep copy
		Bla [] copy = ArrayUtils.arrayClone(orig);
		
		// orig is unchanged
		copy[0].setI(2);
		
		Assert.assertEquals(1, orig[0].getI());
		
		//shallow copy
		Bla [] shallowCopy = orig.clone();
		
		// orig is changed
		shallowCopy[0].setI(2);
		Assert.assertEquals(2, orig[0].getI());

		
	}
	
	@Test
	public void arrayCloneStringTest(){
		String [] orig = new String[]{"asdf"};
		
		// for Strings, this suffices:
		String [] copy = orig.clone();
		copy[0] = "aaa";
		
		Assert.assertEquals("asdf", orig[0]);
		
		
	}

}
