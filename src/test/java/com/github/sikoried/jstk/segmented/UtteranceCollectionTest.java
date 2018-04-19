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
package com.github.sikoried.jstk.segmented;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.github.sikoried.jstk.segmented.Syllable.SYLLABLE_STRESS;
import com.github.sikoried.jstk.segmented.Boundary.BOUNDARIES;
import com.github.sikoried.jstk.segmented.Word.PHRASE_ACCENT;

public class UtteranceCollectionTest {
	public UtteranceCollectionTest(){
	}
	
	@Test
	public void readTest() throws IOException, Exception{
		// Note: need to have jstk/res on java build path!
		URL url = getClass().getResource("/segmented/dialog.xml");

		InputStream is = url.openStream();
		Assertions.assertTrue(is != null);
		
		UtteranceCollection collection =
			UtteranceCollection.read(new BufferedInputStream(is));
		
		Assertions.assertEquals("go", collection.getTurns()[1].getWords()[2].getGraphemes());
		
		Assertions.assertEquals("tonight", collection.getTurns()[0].getWords()[4].getGraphemes());
		Assertions.assertEquals(2, collection.getTurns()[0].getWords()[4].getSyllables()[1].getPosition());
		Assertions.assertEquals(3, collection.getTurns()[0].getWords()[4].getSyllables()[1].getnPhonemes());
		Assertions.assertEquals(SYLLABLE_STRESS.NONE, collection.getTurns()[0].getWords()[4].getSyllables()[0].getStress());
		Assertions.assertEquals(SYLLABLE_STRESS.PRIMARY, collection.getTurns()[0].getWords()[4].getSyllables()[1].getStress());
		
		Assertions.assertEquals(4, collection.getTurns()[2].getSubdivisions()[1].getIndex());
		
		Assertions.assertEquals(7, collection.getTurns()[3].getBoundaries()[1].getIndex());
		Assertions.assertEquals(Boundary.BOUNDARIES.B2, collection.getTurns()[3].getBoundaries()[1].getType());
		Assertions.assertEquals(10, collection.getTurns()[3].getBoundaries()[2].getIndex());
		Assertions.assertEquals(BOUNDARIES.B3, collection.getTurns()[3].getBoundaries()[2].getType());

		Assertions.assertEquals(PHRASE_ACCENT.SECONDARY, collection.getTurns()[0].getWords()[0].getPhraseAccent());
		Assertions.assertEquals(PHRASE_ACCENT.NONE, collection.getTurns()[0].getWords()[1].getPhraseAccent());
		Assertions.assertEquals(PHRASE_ACCENT.PRIMARY, collection.getTurns()[0].getWords()[3].getPhraseAccent());

		//this assumes UTF-8 encoding of source files! 
		Assertions.assertEquals("ɑː", collection.getTurns()[2].getWords()[1].getPhonemes()[1].getSymbols());
		
		Assertions.assertEquals("Speaker 1", collection.getTurns()[0].getRole());
		Assertions.assertEquals("Speaker 2", collection.getTurns()[1].getRole());
		Assertions.assertEquals("sentence1", collection.getTurns()[0].getSegmentTrack());
		Assertions.assertEquals("sentence2", collection.getTurns()[1].getSegmentTrack());
		
		Assertions.assertEquals("123", collection.getTurns()[0].getSegmentId());
		Assertions.assertEquals("456", collection.getTurns()[0].getSegmentRev());
		
		Assertions.assertEquals("789.wav", collection.getTurns()[0].getSegmentFilename());
	}

//	@Test
//	public void blaTest() throws Exception{
//
//
//		URL url = new URL("file:///home/hoenig/disk/Stichproben/C-AuDiT/de/Annotation/Master/xml/panel-sorted.xml");
//
//		InputStream is = url.openStream();
//		Assertions.assertTrue(is != null);
//
//		UtteranceCollection collection = 
//			UtteranceCollection.read(new BufferedInputStream(is));
//	}

}
