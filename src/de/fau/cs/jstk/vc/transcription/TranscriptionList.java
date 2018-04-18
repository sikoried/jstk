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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

public class TranscriptionList {

	private Vector<Transcription> list;
	private String filename;
	private int index;

	public TranscriptionList() {
		list = new Vector<Transcription>();
		index = 0;
	}

	public void load(String filename) throws IOException {
		this.filename = filename;
		FileReader file = new FileReader(filename);
		BufferedReader reader = new BufferedReader(file);
		String line;
		while ((line = reader.readLine()) != null) {
			list.addElement(new Transcription(line));
		}
		reader.close();
	}

	public void save() throws IOException {
		save(filename);
	}

	public void save(String filename) throws IOException {
		FileWriter file = new FileWriter(filename);
		BufferedWriter writer = new BufferedWriter(file);

		Iterator<Transcription> iterator = list.listIterator();
		while (iterator.hasNext()) {
			Transcription transcription = iterator.next(); 
			writer.write(transcription.toString() + '\n');
			transcription.markAsSaved();
		}
		writer.close();
		this.filename = filename;
	}

	public int count() {
		if (list == null) {
			return 0;
		}
		return list.size();
	}

	public Transcription goTo(int index) {
		if ((index >= 0) && (index < list.size())) {
			this.index = index;
			return list.get(index);
		}
		return null;
	}
	
	public int getIndex() {
		return index;
	}

	public boolean hasNext() {
		if (index < list.size() - 1) {
			return true;
		}
		return false;
	}

	public Transcription next() {
		if (index < list.size() - 1) {
			index++;
			return list.get(index);
		}
		return null;
	}

	public boolean hasPrevious() {
		if (index > 0) {
			return true;
		}
		return false;
	}

	public Transcription previous() {
		if (index > 0) {
			index--;
			return list.get(index);
		}
		return null;
	}

	public String getTurnName() {
		if ((index >= 0) && (index < list.size())) {
			return list.get(index).filename;
		}
		return null;
	}

	public ArrayList<String> getTurnList() {
		ArrayList<String> turnList = new ArrayList<String>();
		Iterator<Transcription> iterator = list.iterator();
		while (iterator.hasNext()) {
			Transcription transcription = iterator.next();
			turnList.add(transcription.filename);
		}
		return turnList;
	}

	public boolean hasChanged() {
		Iterator<Transcription> iterator = list.iterator();
		
		while (iterator.hasNext()) {
			Transcription transcription = iterator.next();
			if (transcription.hasChanged()) {
				return true;
			}
		}
		
		return false;
	}
	
}
