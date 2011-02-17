/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
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
package de.fau.cs.jstk.vc.transcription;

import java.io.*;
import java.util.Iterator;
import java.util.Vector;

public class Transcription {

	public int minWordSize = 800; // minimal size of one word given in samples
	public int maxSamples;
	public String filename;
	public String original_transcription;

	private Vector<TranscriptionEntry> transcription;

	public Transcription(String t) throws IOException {
		transcription = new Vector<TranscriptionEntry>();
		original_transcription = t;

		String[] tokens = t.split("[ \t]+");
		if (tokens.length % 3 != 1) {
			throw new IOException("Error in line '" + t + "'");
		}
		filename = tokens[0];
		int i = 1;
		while (i < tokens.length) {
			String word = tokens[i++];
			if (word.equals("[empty]")) {
				word = "";
			}
			int start = Integer.parseInt(tokens[i++]);
			int end = Integer.parseInt(tokens[i++]);
			transcription.addElement(new TranscriptionEntry(word, start, end));
		}
	}

	public Iterator<TranscriptionEntry> getIterator() {
		return transcription.listIterator();
	}

	public TranscriptionEntry get(int index) {
		if ((index >= 0) && (index < transcription.size())) {
			return transcription.get(index);
		}
		return null;
	}

	public int countWords() {
		return transcription.size();
	}

	public int insert(String word, int from, int to)
			throws TranscriptionOverlappingEntriesException {
		// find index position for new element
		int idx = 0;

		if (transcription.size() > 0) {
			TranscriptionEntry prev = null;

			Iterator<TranscriptionEntry> iterator = transcription.iterator();
			TranscriptionEntry e = iterator.next();

			while (iterator.hasNext() && (e.startSample < from)) {
				prev = e;
				e = iterator.next();
				idx++;
			}
			if (e.startSample < from) {
				idx++;
				prev = e;
				e = null;
			}

			if ((e != null) && (e.startSample < to)) {
				throw new TranscriptionOverlappingEntriesException();
			}

			if ((prev != null) && (prev.endSample > from)) {
				throw new TranscriptionOverlappingEntriesException();
			}
		}

		// create new Element and insert it
		TranscriptionEntry entry = new TranscriptionEntry(word, from, to);
		transcription.insertElementAt(entry, idx);
		return idx;
	}

	public boolean delete(int index) {
		if ((index < 0) || (index >= transcription.size())) {
			return false;
		}
		transcription.remove(index);
		return true;
	}

	public String toString() {
		StringBuffer s = new StringBuffer(filename);
		Iterator<TranscriptionEntry> iterator = transcription.listIterator();
		while (iterator.hasNext()) {
			s.append(" ");
			s.append(iterator.next());

		}
		return s.toString();
	}
	
	public boolean hasChanged() {
		return !toString().equals(original_transcription);
	}

	public int moveRightBorder(int currentEntry, int samples) {
		TranscriptionEntry entry = get(currentEntry);
		if (entry == null) {
			return -1;
		}
		TranscriptionEntry next = get(currentEntry + 1);
		int oldEndSample = entry.endSample;

		entry.endSample += samples;
		if (entry.endSample - entry.startSample < minWordSize) {
			entry.endSample = entry.startSample + minWordSize;
		}
		if (entry.endSample > maxSamples) {
			entry.endSample = maxSamples;
		}
		if (next != null) {
			if ((oldEndSample == next.startSample - 1) && (samples < 0)) {
				next.startSample = entry.endSample + 1;
			}
			if (entry.endSample > next.startSample - 1) {
				if (next.endSample - next.startSample - samples > minWordSize) {
					next.startSample = entry.endSample + 1;
				} else {
					entry.endSample -= samples;
				}
			}
		}

		return entry.endSample;

	}

	public int moveLeftBorder(int currentEntry, int samples) {
		TranscriptionEntry entry = get(currentEntry);
		if (entry == null) {
			return -1;
		}
		TranscriptionEntry prev = get(currentEntry - 1);
		int oldStartSample = entry.startSample;

		entry.startSample += samples;
		if (entry.endSample - entry.startSample < minWordSize) {
			entry.startSample = entry.endSample - minWordSize;
		}
		if (entry.startSample < 0) {
			entry.startSample = 0;
		}
		if (prev != null) {
			if ((oldStartSample == prev.endSample + 1) && (samples > 0)) {
				prev.endSample = entry.startSample - 1;
			}
			if (entry.startSample < prev.endSample + 1) {
				if (prev.endSample - prev.startSample - samples > minWordSize) {
					prev.endSample = entry.startSample - 1;
				} else {
					entry.startSample -= samples;
				}
			}
		}

		return entry.startSample;
	}

	public void connect(int entry1, int entry2) {
		boolean next = true;
		if (entry1 > entry2) {
			int h = entry1;
			entry1 = entry2;
			entry2 = h;
			next = false;
		}
		if (entry2 - entry1 != 1) {
			return;
		}
		TranscriptionEntry e1 = get(entry1);
		TranscriptionEntry e2 = get(entry2);

		if ((e1 == null) || (e2 == null)) {
			return;
		}

		if (next) {
			e1.endSample = e2.startSample - 1;
		} else {
			e2.startSample = e1.endSample + 1;
		}
	}

	public void disconnect(int entry1, int entry2) {
		if (entry1 > entry2) {
			int h = entry1;
			entry1 = entry2;
			entry2 = h;
		}
		if (entry2 - entry1 != 1) {
			return;
		}
		TranscriptionEntry e1 = get(entry1);
		TranscriptionEntry e2 = get(entry2);

		if ((e1 == null) || (e2 == null)) {
			return;
		}

		if (e1.endSample == e2.startSample - 1) {
			e1.endSample--;
		}
	}

}
