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

import com.github.sikoried.jstk.segmented.*;
import com.github.sikoried.jstk.segmented.Word.PHRASE_ACCENT;
import com.github.sikoried.jstk.segmented.Syllable.SYLLABLE_STRESS;
import com.github.sikoried.jstk.segmented.Boundary.BOUNDARIES;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UtteranceTest {
	
	public static Utterance getExample(){
		return new Utterance(
				"I'm not that hungry; besides, I don't like curry.",
				"Prince Hamlet",
				new Word[]{
						new Word("i'm", new Syllable[]{new Syllable()},
								new Phoneme[]{new Phoneme("aɪ"), new Phoneme("m")}, PHRASE_ACCENT.NONE),
						new Word("not", new Syllable[]{new Syllable()}, 
								new Phoneme[]{new Phoneme("n"), new Phoneme("ɑː"), new Phoneme("t")}, PHRASE_ACCENT.NONE),
						new Word("that", new Syllable[]{new Syllable()}, 
								new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE),
						new Word("hungry", new Syllable[]{new Syllable(0, 3, Syllable.SYLLABLE_STRESS.PRIMARY), new Syllable(3, 3,SYLLABLE_STRESS.NONE)},
								new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE),
						new Word("besides", new Syllable[]{new Syllable(), new Syllable()}, 
								new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE),
						new Word("i", new Syllable[]{new Syllable()}, new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE),
						new Word("don't", new Syllable[]{new Syllable(), new Syllable()}, 
								new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE),
						new Word("like", new Syllable[]{new Syllable(), new Syllable()}, 
								new Phoneme[]{new Phoneme("x")}, Word.PHRASE_ACCENT.NONE),
						new Word("curry", new Syllable[]{new Syllable(), new Syllable()}, 
								new Phoneme[]{new Phoneme("x")}, Word.PHRASE_ACCENT.NONE)
				},
				new Boundary[]{new Boundary(Boundary.BOUNDARIES.B2, 4)},
				Utterance.Mood.STATEMENT,
				new Subdivision[]{new Subdivision(0), new Subdivision(4)}, null, null, null, null);
	}
	
	public static Utterance getExamplePart1(){
		return new Utterance(
				"I'm not that hungry; ",
				"Prince Hamlet",
				new Word[]{
						new Word("i'm", new Syllable[]{new Syllable()}, 
								new Phoneme[]{new Phoneme("aɪ"), new Phoneme("m")}, PHRASE_ACCENT.NONE),
						new Word("not", new Syllable[]{new Syllable()}, 
								new Phoneme[]{new Phoneme("n"), new Phoneme("ɑː"), new Phoneme("t")}, PHRASE_ACCENT.NONE),
						new Word("that", new Syllable[]{new Syllable()}, 
								new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE),
						new Word("hungry", new Syllable[]{new Syllable(0, 3, SYLLABLE_STRESS.PRIMARY), new Syllable(3, 3,SYLLABLE_STRESS.NONE)}, 
								new Phoneme[]{new Phoneme("x")}, PHRASE_ACCENT.NONE)						
				},				
				new Boundary[]{new Boundary(Boundary.BOUNDARIES.B2, 4)},
				Utterance.Mood.STATEMENT,
				new Subdivision[]{new Subdivision(0)}, null, null, null, null);
	}
	
	public static Utterance getExamplePart2(){
		return new Utterance(
				"besides, I don't like curry.",
				"Prince Hamlet",
				new Word[]{
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
				new Boundary[]{new Boundary(BOUNDARIES.B2, 0)},
				Utterance.Mood.STATEMENT,
				new Subdivision[]{new Subdivision(0)}, null, null, null, null);
	}
	
	@Test
	public void getOrthographyIndexTest() throws Exception{
		Utterance u = getExample();
		
		// including the trailing space!
		Assertions.assertEquals("I'm not that hungry; ", u.getOrthography(0, 4));

		Assertions.assertEquals("besides, I don't like curry.", u.getOrthography(4, 9));

		// syllables in "hungry"
		Assertions.assertEquals(0, u.getWords()[3].getSyllables()[0].getPosition());
		Assertions.assertEquals(3, u.getWords()[3].getSyllables()[0].getnPhonemes());
		Assertions.assertEquals(3, u.getWords()[3].getSyllables()[1].getPosition());
		Assertions.assertEquals(3, u.getWords()[3].getSyllables()[1].getnPhonemes());
		
		// the o in "not"
		Assertions.assertEquals("ɑː", u.getWords()[1].getPhonemes()[1].getSymbols());
	}
	
	@Test
	public void getSubUtteranceTest() throws Exception{
		Utterance u = getExample();
		Utterance uCopy = getExample();
		Assertions.assertTrue(u != uCopy);
		Assertions.assertTrue(u.equals(uCopy));
		
//		ByteArrayOutputStream os = new ByteArrayOutputStream();
//		XMLEncoder e = new XMLEncoder(os);
//		e.writeObject(u);
//		e.close();
//		System.out.println(new String(os.toByteArray()));
		
		//System.out.println(new String(u.toXML()));
		
		Utterance u1 = getExamplePart1();
		Utterance u2 = getExamplePart2();

		Assertions.assertEquals(
				new String(u.toXML()),
				new String(
				u.getSubUtterance(0, 1).toXML()));

		Assertions.assertEquals(
				new String(u1.toXML()),
				new String(
				u.getSubUtterance(0, 0).toXML()));

		Assertions.assertEquals(
				new String(u1.toXML()),
				new String(
				u.getSubUtterance(0, 0).toXML()));

		Assertions.assertEquals(
				new String(u2.toXML()),
				new String(
				u.getSubUtterance(1, 1).toXML()));
	}
}
