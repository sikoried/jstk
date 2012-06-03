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

import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import de.fau.cs.jstk.segmented.Boundary.BOUNDARIES;
import de.fau.cs.jstk.util.ArrayUtils;
import de.fau.cs.jstk.util.ArrayUtils.PubliclyCloneable;

/**
 * represents a turn, sentence etc: orthography with punctuation as displayed to the speaker,
 * words, boundaries, possible subdivisions etc.
 * @author hoenig
 *
 */
public class Utterance implements Serializable, PubliclyCloneable{
	
	private static final long serialVersionUID = 3535642214459508273L;

	private static enum SegmentAttributes {
		NR, ID, TRACK, REV, FILENAME, SPEAKER
	}
	
	
	/**
	 * The utterance as displayed to the user/learner: with capitalization, punctuation etc.
	 */
	private String orthography = null;
	
	private double melody = 0.0;

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
	
	public enum Mood{
		STATEMENT{  public String toString(){return "."; }},
		QUESTION{   public String toString(){return "?"; }},
		COMMAND{    public String toString(){return "!"; }},
		EXCLAMATION{public String toString(){return "!!";}}
	};
	
	private Mood mood = null; 
	
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
	
	public Utterance(String orthography, String role,
			Word [] words,
			Boundary [] boundaries,
			Mood mood,
			Subdivision [] subdivisions,
			String segmentId, String segmentTrack, String segmentRev, String segmentFilename) {
		this.setOrthography(orthography);

		setWords(words);
		this.setRole(role);
		setBoundaries(boundaries);
		this.mood = mood;
		setSubdivisions(subdivisions);
		this.segmentId = segmentId;
		this.segmentTrack = segmentTrack;
		this.segmentRev = segmentRev;
		this.segmentFilename = segmentFilename;
	}
	
	@Override
	public Utterance clone(){// throws CloneNotSupportedException{
		//return new Utterance(orthography, role,
				//words, boundaries, subdivisions, segmentId, segmentTrack, segmentRev, segmentFilename);
		
		Utterance newUtterance;
		try {
			newUtterance = (Utterance) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
		
		// need deep array-copies:
		newUtterance.setWords(words);
		newUtterance.setBoundaries(boundaries);
		newUtterance.setSubdivisions(subdivisions);
		
		// stuff that's not (yet?) in constructor
		newUtterance.setMelody(melody);
		
		return newUtterance;
	}

	static Utterance read(Node textsegmentNode) throws Exception{
		String nodeName = textsegmentNode.getNodeName();
		String melody = null;
		
		if (nodeName.equals("#text")) {
			textsegmentNode = textsegmentNode.getNextSibling();
			nodeName = textsegmentNode.getNodeName();
		}		
		if (!nodeName.equals("textsegment"))
			throw new Exception("expecting node textsegment, got "
					+ nodeName);

		NamedNodeMap attributes = textsegmentNode.getAttributes();
		String segmentId = null;
		String segmentTrack = null;
		String segmentRev = null;
		String segmentFilename = null;
		String speaker = null;
		for (int i = 0; i < attributes.getLength(); i++) {
			Node item = attributes.item(i);
			switch (SegmentAttributes.valueOf(item.getLocalName().toUpperCase())) {
			case NR:
				break;
			case ID:
				segmentId = item.getNodeValue();
				break;
			case TRACK:
				segmentTrack = item.getNodeValue();
				break;
			case REV:
				segmentRev = item.getNodeValue();
				break;
			case FILENAME:
				segmentFilename = item.getNodeValue();
				break;
			case SPEAKER:
				speaker = item.getNodeValue();
				break;
			default:
				throw new Exception("Unknown textsegment attribute: " + item.getNodeName());
			}
		}

		Node utteranceNode = textsegmentNode.getFirstChild();
		nodeName = utteranceNode.getNodeName();
		if (nodeName.equals("#text")) {
			utteranceNode = utteranceNode.getNextSibling();
			nodeName = utteranceNode.getNodeName();
		}
		nodeName = utteranceNode.getNodeName();
		if (!nodeName.equals("utterance")) {
			throw new Exception("expecting node utterance, got " + nodeName);
		}		
		
		if (!nodeName.equals("utterance"))
			throw new Exception("Expecting node name utterance, got " + nodeName);			
		
		String orthography = null;
		
		List<Boundary> boundaries = new LinkedList<Boundary>();
		List<Subdivision> subdivisions = new LinkedList<Subdivision>();
		List<Word> words = new LinkedList<Word>();
		utteranceNode = utteranceNode.getFirstChild();
		
		while (utteranceNode != null) {
			nodeName = utteranceNode.getNodeName();
			if (nodeName.equals("#text")){
				utteranceNode = utteranceNode.getNextSibling();				
				continue;
			}
			else if (nodeName.equals("orthography")) {
				orthography = utteranceNode.getTextContent();
			}
			
			else if (nodeName.equals("melody")) {
				melody = utteranceNode.getTextContent();
			}

			else if (nodeName.equals("boundary")){				
				boundaries.add(Boundary.read(utteranceNode));
			}

			else if (nodeName.equals("subdivision")){				
				subdivisions.add(Subdivision.read(utteranceNode));
			}
			else if (nodeName.equals("word")){
				words.add(Word.read(utteranceNode));
			}
			else{
				throw new Exception("unexpected node name in utterance: " + nodeName);
			}

			utteranceNode = utteranceNode.getNextSibling();
		}
		//System.out.println("orthography = " + orthography + ", speaker = " + speaker);
		
		Boundary [] boundaryDummy = new Boundary[0];
		Subdivision[] subdivisionDummy = new Subdivision[0];
		Word [] wordDummy = new Word[0];
				
		Utterance utterance = new Utterance(orthography, speaker,
				words.toArray(wordDummy),
				boundaries.toArray(boundaryDummy),
				Utterance.guessMood(orthography),
				subdivisions.toArray(subdivisionDummy),				
				segmentId, segmentTrack, segmentRev, segmentFilename);
		
		if (melody != null){
			utterance.setMelody(Double.parseDouble(melody));
			System.err.println("melody = " + utterance.getMelody());
		}
		
		return utterance;
	}
	
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
		
//		System.err.println("getOrthographyIndex for word " + word);
		
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
//		System.err.println("position = " + position);
		return position;
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

	/**
	 * 
	 * @param i
	 * @return (an estimate of) part of the orthographic representation that belongs to word i,
	 *   including spaces/punctiation/etc. limitations see getOrthographyIndex().
	 * @throws Exception see that of getOrthographyIndex()
	 */
	public String getWordOrthography(int i) throws Exception {
		int startIndex, endIndex;

//		System.err.println("getSubdivisionOrthography: subdivision " + i);
		
		startIndex = i;
		endIndex = i + 1;		
		
		return getOrthography(startIndex, endIndex);
	}
	
	/**
	 * 
	 * @param i
	 * @return (an estimate of) part of the orthographic representation that belongs to subdivision i
	 * @throws Exception see that of getOrthographyIndex()
	 */
	public String getSubdivisionOrthography(int i) throws Exception {
		int startIndex, endIndex;

//		System.err.println("getSubdivisionOrthography: subdivision " + i);
		
		startIndex = getSubdivisions()[i].getIndex();
		if (i == getSubdivisions().length - 1)
			endIndex = words.length;
		else
			endIndex = getSubdivisions()[i + 1].getIndex();
		
//		System.err.println("startIndex = " + startIndex);
//		System.err.println("endIndex = " + endIndex);
//		System.err.println("strlen = " + getOrthography().length());				
		
		return getOrthography(startIndex, endIndex);
	}

	public void setSubdivisions(Subdivision [] subdivisions) {
		this.subdivisions = ArrayUtils.arrayClone(subdivisions);
	}

	public Subdivision [] getSubdivisions() {
		return subdivisions;
	}

	public void setWords(Word [] words) {
		this.words = ArrayUtils.arrayClone(words);
	}

	public Word [] getWords() {
		return words;
	}

	public Boundary[] getBoundaries() {
		return boundaries;
	}

	public void setBoundaries(Boundary[] boundaries) {
		this.boundaries = ArrayUtils.arrayClone(boundaries);
	}
	
	/**
	 * @param word
	 * @return boundary before word or null if there's no boundary 
	 */
	public Boundary getBoundaryBeforeWord(int word){
		if (word > words.length){
			throw new IndexOutOfBoundsException("index = " + word + " but number of words is only " + words.length);
		}
		for (Boundary b: boundaries){
			if (b.getIndex() == word)
				return b;			
		}
		return null;
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

	public void setRole(String role) {
		this.role = role;
	}

	public String getRole() {
		return role;
	}
	
	public byte [] toXML(){
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		XMLEncoder e = new XMLEncoder(os);
		e.writeObject(this);
		e.close();
		return os.toByteArray();
	}
	
	@Override
	public boolean equals(Object o){
		if (!(o instanceof Utterance))
			return false;
		Utterance u = (Utterance) o;
		
		byte [] me = toXML();
		byte [] other = u.toXML();
		return Arrays.equals(me, other);						
	}
	
	
	/** convenience shortcut to get a single subdivision */
	public Utterance getSubUtterance(int subdivision) throws Exception {
		return getSubUtterance(subdivision, subdivision);		
	}
	
	/** create a new Utterance that comprises subdivisions 'first' through 'last' (inclusive) */ 
	public Utterance getSubUtterance(int first, int last) throws Exception {
		int i;		
		
		String orthography = "";
		for (i = first; i <= last; i++){
			orthography += getSubdivisionOrthography(i);
			
//			System.out.printf("this o [%d] = %s, total = %s\n",
//					i, getSubdivisionOrthography(i), orthography);
		}		
		
		int firstWord;
		if (subdivisions.length == 0)
			firstWord = 0;
		else 
			firstWord = subdivisions[first].getIndex();
		
		int lastWord;
		if (subdivisions.length == 0 ||
			last == subdivisions.length - 1)
			lastWord = words.length - 1;
		else
			lastWord = subdivisions[last + 1].getIndex() - 1; 
		
		System.err.printf("firstWord=%d, lastWord=%d\n", firstWord, lastWord);
		
		// search for start of new boundaries
		for (i = 0; i < boundaries.length; i++)
			if (boundaries[i].getIndex() > firstWord)
				break;			
		int firstBoundary = i;
		
		// search for end of new boundaries
		for (i = boundaries.length - 1; i >= 0; i--)
			if (boundaries[i].getIndex() <= lastWord)
				break;
		int lastBoundary = i;
		
		int B2before = 0;
		if (getBoundaryBeforeWord(firstWord) != null &&
				getBoundaryBeforeWord(firstWord).getType() == BOUNDARIES.B2)
			B2before = 1;
				
		int B2after = 0;
		if (getBoundaryBeforeWord(lastWord + 1) != null &&
				getBoundaryBeforeWord(lastWord + 1).getType() == BOUNDARIES.B2)
			B2after = 1;
		System.err.printf("B2before=%d, B2after=%d\n", B2before, B2after);
		
		Boundary [] newBoundaries = new Boundary[lastBoundary - firstBoundary + 1 + B2before + B2after];
		
		if (B2before == 1)
			newBoundaries[0] = new Boundary(BOUNDARIES.B2, 0);
		if (B2after == 1)
			newBoundaries[lastBoundary - firstBoundary + 1 + B2before] = new Boundary(BOUNDARIES.B2,
					lastWord - firstWord + 1);
					
		for (i = firstBoundary; i <= lastBoundary; i++)
			newBoundaries[i - firstBoundary + B2before] = new Boundary(boundaries[i].getType(), 
					boundaries[i].getIndex() - firstWord);		 
		
		Subdivision [] newSubdivisions = new Subdivision[last - first + 1];
		for (i = first; i <= last; i++)
			newSubdivisions[i - first] = new Subdivision(subdivisions[i].getIndex() - firstWord);
		
		System.err.println("boundaries = " + Arrays.deepToString(newBoundaries));
		
		// FIXME: adapt when we have syllable-level scores 
		Utterance utterance =
				new Utterance(orthography, getRole(),
						Arrays.copyOfRange(words,
								firstWord, lastWord + 1),		
								newBoundaries,
								mood,
								newSubdivisions,
								segmentId, segmentTrack,
								// TODO
								null,//segmentRev, 
								null//segmentFilename
						);
		utterance.setMelody(melody);
		return utterance;
	}
	
	/**
	 * 
	 * @return mood as indicated by punctuation ("?", "!", "!!", or "." at end of orthography)
	 */
	public static Mood guessMood(String orthography){
		// remove trailing white spaces
		String tmp = orthography.replaceAll("\\s+$", "");
		String candidate = tmp.substring(tmp.length() - 1, tmp.length());
		if (candidate.endsWith("!!"))
			return Mood.EXCLAMATION;
		if (candidate.endsWith("!"))
			return Mood.COMMAND;
		if (candidate.endsWith("?"))
			return Mood.QUESTION;
		return Mood.STATEMENT;
	}	

	/**
	 * Generates a textual representation of this utterance 
	 * (e.g. "What should we do tonight?").
	 * @param includePhraseAnnotation
	 *        if true, include phrase boundaries and accents, e.g.
	 *        "What SA should we do PA tonight SA?"
	 * @return the textual representation 
	 */
	public String toTextString(boolean includePhraseAnnotation){
		String ret = "";
		
		// B2 before first word? (i.e. from a subdivided utterance)
		Boundary b = getBoundaryBeforeWord(0);
		if (b != null && b.getIndex() == 0 &&
				b.getType() == BOUNDARIES.B2)
			ret += "B2 ";
		
		for (int i = 0; i < words.length; i++){
			Word w = words[i];

			ret += w.getGraphemes() + " ";
			if (includePhraseAnnotation){
				switch(w.getPhraseAccent()){
				case NONE:
					break;
				case PRIMARY:
					ret += "PA ";
					break;
				case SECONDARY:
					ret += "SA ";
					break;
				case EXTRA:
					ret += "EA ";
					break;
				default: 
					throw new Error("unknown phrase accent value " + w.getPhraseAccent());
				}
				
				b = getBoundaryBeforeWord(i + 1);
				
				if (b != null)
					switch (b.getType()){
					case B2:
						ret += "B2 ";
						break;
					case B3:
						ret += "B3 ";
						break;
					default:
						throw new Error("unknown phrase boundary value " + b.getType());
					}
			}
		}
		
		ret += mood.toString();
		return ret;
	
	}

	public Mood getMood() {
		return mood;
	}

	public void setMood(Mood mood) {
		this.mood = mood;
	}

	public double getMelody() {
		return melody;
	}

	public void setMelody(double melody) {
		this.melody = melody;
	}
	
//	public void setPhraseAccents(PhraseAccent [] phraseAccents) {
//		this.phraseAccents = phraseAccents;
//	}
//
//	public PhraseAccent [] getPhraseAccents() {
//		return phraseAccents;
//	}

}

