package de.fau.cs.jstk.segmented;


import org.junit.Assert;
import org.junit.Test;

import de.fau.cs.jstk.segmented.Boundary.BOUNDARIES;

public class UtteranceTest {

	
	public static Utterance getExample(){
		return new Utterance(
				"I'm not that hungry. Besides, I don't like curry.",
				"Prince Hamlet",
				new Word[]{
						new Word("i'm", new Syllable[]{new Syllable()}),
						new Word("not", new Syllable[]{new Syllable()}),
						new Word("that", new Syllable[]{new Syllable()}),
						new Word("hungry", new Syllable[]{new Syllable(), new Syllable()}),
						new Word("besides", new Syllable[]{new Syllable(), new Syllable()}),
						new Word("i", new Syllable[]{new Syllable()}),
						new Word("don't", new Syllable[]{new Syllable(), new Syllable()}),
						new Word("like", new Syllable[]{new Syllable(), new Syllable()}),
						new Word("curry", new Syllable[]{new Syllable(), new Syllable()})
				},
				new Boundary[]{new Boundary(BOUNDARIES.B3, 4)},
				new Subdivision[]{new Subdivision(4)});
	}
	
	@Test
	public void getOrthographyIndexTest() throws Exception{
		Utterance u = getExample();		
		
		// including the trailing space!
		Assert.assertEquals(u.getOrthography(0, 4), "I'm not that hungry. "); 
		
		Assert.assertEquals(u.getOrthography(4, 9), "Besides, I don't like curry.");		
		
	}

}
