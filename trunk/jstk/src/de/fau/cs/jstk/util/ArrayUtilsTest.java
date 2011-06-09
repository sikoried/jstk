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
