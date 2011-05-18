package de.fau.cs.jstk.segmented;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import de.fau.cs.jstk.segmented.Boundary.BOUNDARIES;
import de.fau.cs.jstk.segmented.Syllable.SYLLABLE_STRESS;
import de.fau.cs.jstk.segmented.Word.PHRASE_ACCENT;

public class UtteranceCollectionTest {
	public UtteranceCollectionTest(){		
	}
	
	@Test
	public void readTest() throws IOException, Exception{
		// Note: need to have jstk/res on java build path!
		URL url = getClass().getResource("/segmented/dialog.xml");

		InputStream is = url.openStream();
		Assert.assertTrue(is != null);
		
		UtteranceCollection collection = 
			UtteranceCollection.read(new BufferedInputStream(is));
		
		Assert.assertEquals("go", collection.getTurns()[1].getWords()[2].getGraphemes());
		
		Assert.assertEquals("tonight", collection.getTurns()[0].getWords()[4].getGraphemes());
		Assert.assertEquals(2, collection.getTurns()[0].getWords()[4].getSyllables()[1].getPosition());
		Assert.assertEquals(3, collection.getTurns()[0].getWords()[4].getSyllables()[1].getnPhonemes());
		Assert.assertEquals(SYLLABLE_STRESS.NONE, collection.getTurns()[0].getWords()[4].getSyllables()[0].getStress());
		Assert.assertEquals(SYLLABLE_STRESS.PRIMARY, collection.getTurns()[0].getWords()[4].getSyllables()[1].getStress());
		
		Assert.assertEquals(4, collection.getTurns()[2].getSubdivisions()[1].getIndex());
		
		Assert.assertEquals(7, collection.getTurns()[3].getBoundaries()[1].getIndex());
		Assert.assertEquals(BOUNDARIES.B2, collection.getTurns()[3].getBoundaries()[1].getType());
		Assert.assertEquals(10, collection.getTurns()[3].getBoundaries()[2].getIndex());
		Assert.assertEquals(BOUNDARIES.B3, collection.getTurns()[3].getBoundaries()[2].getType());

		Assert.assertEquals(PHRASE_ACCENT.SECONDARY, collection.getTurns()[0].getWords()[0].getPhraseAccent());
		Assert.assertEquals(PHRASE_ACCENT.NONE, collection.getTurns()[0].getWords()[1].getPhraseAccent());
		Assert.assertEquals(PHRASE_ACCENT.PRIMARY, collection.getTurns()[0].getWords()[3].getPhraseAccent());

		Assert.assertEquals("ɑː", collection.getTurns()[2].getWords()[1].getPhonemes()[1].getSymbols());	
	}

//	@Test
//	public void blaTest() throws Exception{
//
//
//		URL url = new URL("file:///home/hoenig/disk/Stichproben/C-AuDiT/de/Annotation/Master/xml/panel-sorted.xml");
//
//		InputStream is = url.openStream();
//		Assert.assertTrue(is != null);
//
//		UtteranceCollection collection = 
//			UtteranceCollection.read(new BufferedInputStream(is));
//	}

}
