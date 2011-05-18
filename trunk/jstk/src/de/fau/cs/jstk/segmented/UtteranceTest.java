package de.fau.cs.jstk.segmented;

import org.junit.Assert;
import org.junit.Test;

import de.fau.cs.jstk.segmented.Boundary.BOUNDARIES;
import de.fau.cs.jstk.segmented.Syllable.SYLLABLE_STRESS;
import de.fau.cs.jstk.segmented.Word.PHRASE_ACCENT;

public class UtteranceTest {
	
	public static Utterance getExample(){
		return new Utterance(
				"I'm not that hungry. Besides, I don't like curry.",
				"Prince Hamlet",
				new Word[]{
						new Word("i'm", new Syllable[]{new Syllable()}, 
								new Phoneme[]{new Phoneme("aɪ"), new Phoneme("m")}, PHRASE_ACCENT.NONE),
						new Word("not", new Syllable[]{new Syllable()}, 
								new Phoneme[]{new Phoneme("n"), new Phoneme("ɑː"), new Phoneme("t")}, PHRASE_ACCENT.NONE),
						new Word("that", new Syllable[]{new Syllable()}, 
								new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE),
						new Word("hungry", new Syllable[]{new Syllable(0, 3, SYLLABLE_STRESS.PRIMARY), new Syllable(3, 3,SYLLABLE_STRESS.NONE)}, 
								new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE),
						new Word("besides", new Syllable[]{new Syllable(), new Syllable()}, 
								new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE),
						new Word("i", new Syllable[]{new Syllable()}, new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE),
						new Word("don't", new Syllable[]{new Syllable(), new Syllable()}, 
								new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE),
						new Word("like", new Syllable[]{new Syllable(), new Syllable()}, 
								new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE),
						new Word("curry", new Syllable[]{new Syllable(), new Syllable()}, 
								new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE)
				},
				new Boundary[]{new Boundary(BOUNDARIES.B3, 4)},
				new Subdivision[]{new Subdivision(4)});
	}
	
	@Test
	public void getOrthographyIndexTest() throws Exception{
		Utterance u = getExample();		
		
		// including the trailing space!
		Assert.assertEquals("I'm not that hungry. ", u.getOrthography(0, 4)); 
		
		Assert.assertEquals("Besides, I don't like curry.", u.getOrthography(4, 9));		

		// syllables in "hungry"
		Assert.assertEquals(0, u.getWords()[3].getSyllables()[0].getPosition());
		Assert.assertEquals(3, u.getWords()[3].getSyllables()[0].getnPhonemes());
		Assert.assertEquals(3, u.getWords()[3].getSyllables()[1].getPosition());
		Assert.assertEquals(3, u.getWords()[3].getSyllables()[1].getnPhonemes());		
		
		// the o in "not"
		Assert.assertEquals("ɑː", u.getWords()[1].getPhonemes()[1].getSymbols());		
	}
}
