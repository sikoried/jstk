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
package de.fau.cs.jstk.segmented;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Node;

import de.fau.cs.jstk.segmented.Boundary.BOUNDARIES;

public class Utterance implements Serializable{
	
	private static final long serialVersionUID = 3535642214459508273L;

	/**
	 * The utterance as displayed to the user/learner: with capitalization, punctuation etc.
	 */
	private String orthography = null;

	/**
	 * role which is speaking this, e.g. "Prince Hamlet" as in Prince Hamlet:
	 * "To be or not to be"
	 * *not* the name of the participant. only used for dod (Dialogue of the Day) 
	 */
	private String role = null;
	
	private Word [] words = null;
	
	/** 
	 * phrase Boundaries (B2, B3 boundaries)
	 */
	private Boundary [] boundaries = null;
	
	
	
	/**
	 * Possible subdivisions of this utterance, e.g. for a novice second language learner
	 * when training this utterance. Often, but not always coincides with B3 boundaries;
	 * sometimes coincides with B2 boundaries.
	 * First subdivision starts at the first word, i.e. there is always at least one subdivision.
	 */
	private Subdivision [] subdivisions;
	
	private String segmentId = null;
	private String segmentTrack = null;
	private String segmentRev = null;
	private String segmentFilename = null;

	public Utterance(){
	}
	
	public Utterance(String orthography, String speaker,
			Word [] words,
			Boundary [] boundaries, Subdivision [] subdivisions) {
		this.setOrthography(orthography);
		this.words = words;
		this.setSpeaker(speaker);
		this.boundaries = boundaries;
		this.setSubdivisions(subdivisions);
	}

	static Utterance read(Node node, String speaker) throws Exception{
		String nodeName = node.getNodeName();
		
		if (!nodeName.equals("utterance"))
			throw new Exception("Expecting node name utterance, got " + nodeName);			
		
		String orthography = null;
		
		List<Boundary> boundaries = new LinkedList<Boundary>();
		List<Subdivision> subdivisions = new LinkedList<Subdivision>();
		List<Word> words = new LinkedList<Word>();
		node = node.getFirstChild();
		
		
		while (node != null) {
			nodeName = node.getNodeName();
			if (nodeName.equals("#text")){
				node = node.getNextSibling();				
				continue;
			}
			else if (nodeName.equals("orthography")) {
				orthography = node.getTextContent();
			}

			else if (nodeName.equals("boundary")){				
				boundaries.add(Boundary.read(node));
			}

			else if (nodeName.equals("subdivision")){				
				subdivisions.add(Subdivision.read(node));
			}
			else if (nodeName.equals("word")){
				words.add(Word.read(node));
			}
			else{
				throw new Exception("unexpected node name in utterance: " + nodeName);
			}

			node = node.getNextSibling();
		}
		//System.out.println("orthography = " + orthography + ", speaker = " + speaker);
		
		Boundary [] boundaryDummy = new Boundary[0];
		Subdivision[] subdivisionDummy = new Subdivision[0];
		Word [] wordDummy = new Word[0];
				
		return new Utterance(orthography, speaker,
				words.toArray(wordDummy),
				boundaries.toArray(boundaryDummy),
				subdivisions.toArray(subdivisionDummy));
	}
	

	
//	/**
//	 * 
//	 * @param i
//	 * @return the part of orthography that belongs to main phrase number i
//	 * (according to getB3Boundaries()) 
//	 */
//	public String getMainPhraseOrthographyy(int i){
//		int start, end;
//		
//		if (boundaries.length == 0)
//			return getOrthography();
//		
//		// find (B3) boundaries surrounding main phrase i
//		int nB3 = 0;
//		int boundaryBefore = Integer.MIN_VALUE, boundaryAfter = Integer.MIN_VALUE;
//		
//		int boundary;
//		for (boundary = 0; boundary < boundaries.length; boundary++) {
//			if (boundaries[boundary].getType() == BOUNDARIES.B3)
//				nB3++;
//			if (nB3 == i && 
//					// don't overwrite!
//					boundaryBefore == Integer.MIN_VALUE){
//				boundaryBefore = boundary;
//			}
//			if (nB3 == i + 1){
//				boundaryAfter = boundary;
//				break;
//			}			
//		}		
//				
//		if (i == 0)
//			boundaryBefore = -1;
//		
//		if (i == subdivisions.length - 1)
//			boundaryAfter = boundaries.length;
//		
//		if (boundaryBefore == Integer.MIN_VALUE || boundaryAfter == Integer.MIN_VALUE)
//			throw new Error("getMainPhraseOrthography: Implementation Error?");		
//		
//		
//		/*
//		System.out.println(orthography);
//		System.out.println("i = "  + i + ", boundaryBefore = " + boundaryBefore + ", boundaryAfter = " + boundaryAfter);
//		*/
//		
//
//		if (boundaryBefore == -1)		
//			start = 0;
//		else
//			start = boundaries[boundaryBefore].getBeginsInOrthography();
//		
//		if (boundaryAfter == boundaries.length)
//			end = getOrthography().length();
//		else {	
//			end = boundaries[boundaryAfter].getBeginsInOrthography();
//		}
//		
//		//System.out.println("-> " + orthography.substring(start, end));
//		
//		return getOrthography().substring(start, end);		
//		
//	}

	public void setOrthography(String orthography) {
		this.orthography = orthography;
	}

	public String getOrthography() {
		return orthography;
	}
	
	/**
	 * 
	 * @param beginIndex
	 * @param endIndex
	 * @return the part of the orthographic transcription that belongs to the words 
	 * beginIndex through endIndex - 1.
	 * @throws Exception 
	 */
	public String getOrthography(int beginIndex, int endIndex) throws Exception{
		return orthography.substring(getOrthographyIndex(beginIndex),
				getOrthographyIndex(endIndex));		
	}
	
	/**
	 * estimate where a word begins in the orthographic transcription. 
	 * Some heuristic assumptions are used, e.g. the words must be written as in their
	 * graphemic transcription (except case). Quotation-marks and punctuation might not
	 * be handled well.
	 * @param i
	 * @return the character index of the start of word i in the orthographic transcription. Special case: For i == getWords().length, getOrthography.length() is returned.
	 * @throws Exception if graphemic transcriptions of words aren't found in orthography (ignoring case) 
	 */
	public int getOrthographyIndex(int word)throws Exception{
		int position = 0, i;
		String lcOrtho = orthography.toLowerCase();
		
		System.err.println("getOrthographyIndex for word " + word);
		
		// include any starting quotations marks
		if (word == 0)
			return 0;
		// special case
		else if (word == words.length)
			return orthography.length();
		// search for all words 0 to word - 1
		else{
			int thisPos;
			for (i = 0; i <= word; i++){
				String lcWord = words[i].getGraphemes().toLowerCase();
				thisPos = lcOrtho.indexOf(lcWord, position);
				if (thisPos == -1){
					String msg = "Could not find \"" + lcWord + "\" in \"" + lcOrtho + "\"!";
					System.err.println(msg);
					throw new Exception(msg);
				}
				position = thisPos;
				if (i < word)
					position += lcWord.length() + 1;
			}			
		}		
		System.err.println("position = " + position);
		return position;
	}

	public void setSpeaker(String speaker) {
		this.role = speaker;
	}

	public String getSpeaker() {
		return role;
	}
	
	// FIXME
	@Override
	public String toString(){
		String ret = "subdivisions = ";
		for (Subdivision s : getSubdivisions()){
			ret += "" + s.getIndex() + "; ";
			
		}
		ret += "\n";
		return ret;		
		
	}

	public String getSubdivisionOrthography(int i) throws Exception {
		int startIndex, endIndex;

		System.err.println("getSubdivisionOrthography: subdivision " + i);
		
		startIndex = getSubdivisions()[i].getIndex();
		if (i == getSubdivisions().length - 1)
			endIndex = words.length;
		else
			endIndex = getSubdivisions()[i + 1].getIndex();
		
		System.err.println("startIndex = " + startIndex);
		System.err.println("endIndex = " + endIndex);
		System.err.println("strlen = " + getOrthography().length());				
		
		//return getOrthography().substring(start, end);
		return getOrthography(startIndex, endIndex);
	}

	public void setSubdivisions(Subdivision [] subdivisions) {
		this.subdivisions = subdivisions;
	}

	public Subdivision [] getSubdivisions() {
		return subdivisions;
	}

	public void setWords(Word [] words) {
		this.words = words;
	}

	public Word [] getWords() {
		return words;
	}

	public Boundary[] getBoundaries() {
		return boundaries;
	}

	public void setBoundaries(Boundary[] boundaries) {
		this.boundaries = boundaries;
	}
	
	/**
	 * B3 boundaries divide the utterance into main phrases.
	 * @return B3 boundaries
	 */
	public Boundary [] getB3Boundaries() {
		List<Boundary> list = new LinkedList<Boundary>();
		
		for (Boundary b : boundaries)			
			if (b.getType().equals(BOUNDARIES.B3))
				list.add(b);
		
		return list.toArray(new Boundary[]{});		
	}

	/**
	 * @return the number of main phrases, according to B3 boundaries,
	 * (i.e. the number of B3 boundaries + 1)
	 */
	public int getNMainPhrases(){
//		int ret = 1;
//		for (Boundary b : boundaries){
//			if (b.getType() == BOUNDARIES.B3)
//				ret++;			
//		}
//		return ret;		
		return getB3Boundaries().length + 1;
	}

	public void setSegmentId(String segmentId) {
		this.segmentId = segmentId;
	}

	public String getSegmentId() {
		return segmentId;
	}

	public void setSegmentTrack(String segmentTrack) {
		this.segmentTrack = segmentTrack;
	}

	public String getSegmentTrack() {
		return segmentTrack;
	}

	public void setSegmentRev(String segmentRev) {
		this.segmentRev = segmentRev;
	}

	public String getSegmentRev() {
		return segmentRev;
	}

	public void setSegmentFilename(String segmentFilename) {
		this.segmentFilename = segmentFilename;
	}

	public String getSegmentFilename() {
		return segmentFilename;
	}
	
	/**
	 * The identifier that's used in the label files.
	 */
	public String getMoniker() {
		return segmentId + (segmentTrack == null ? "" : "_" + segmentTrack);
	}
	
//	public void setPhraseAccents(PhraseAccent [] phraseAccents) {
//		this.phraseAccents = phraseAccents;
//	}
//
//	public PhraseAccent [] getPhraseAccents() {
//		return phraseAccents;
//	}

}

