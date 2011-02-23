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
package de.fau.cs.jstk.app.transcriber;

import java.io.*;
import java.util.*;

public class Preferences {

	private LinkedHashMap<String, String> properties;
	private LinkedHashMap<String, String> defaults;
	private String filename;

	public Preferences(String filename) {
		this.filename = filename;
		properties = new LinkedHashMap<String, String>();
		properties.put("transcriptionFile", "");
		properties.put("wavdir", "./WAV/");
		properties.put("f0dir_original", "./F0_AUTO/");
		properties.put("f0dir_corrected", "./F0_CORRECTED/");
		properties.put("f0suffix_original", ".f0");
		properties.put("f0suffix_corrected", ".f0.new");
		properties.put("f0.shift", "10");
		properties.put("f0.windowLength", "16");
		properties.put("f0.minimum", "50");
		properties.put("f0.maximum", "600");
		properties.put("mainWindow.x", "0");
		properties.put("mainWindow.y", "0");
		properties.put("mainWindow.width", "650");
		properties.put("mainWindow.height", "400");
		properties.put("spectrumWindow.x", "650");
		properties.put("spectrumWindow.y", "0");
		properties.put("spectrumWindow.width", "400");
		properties.put("spectrumWindow.height", "400");
		properties.put("acWindow.x", "650");
		properties.put("acWindow.y", "450");
		properties.put("acWindow.width", "400");
		properties.put("acWindow.height", "400");
		properties.put("pitchEstimatorWindow.x", "0");
		properties.put("pitchEstimatorWindow.y", "0");
		properties.put("pitchEstimatorWindow.width", "1050");
		properties.put("pitchEstimatorWindow.height", "150");
		properties.put("pitchEstimatorWindow.zoom", "3");

		defaults = new LinkedHashMap<String, String>(properties);
		try {
			load(filename);
		} catch (IOException e) {
			System.err.println("No configuration file found ('" + filename + "')");
		}
	}

	public void load(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = reader.readLine()) != null) {
			// System.out.println(line);
			String[] tokens = line.split("=");
			if (tokens.length == 2) {
				String key = tokens[0].trim();
				String value = tokens[1].trim();
				properties.put(key, value);
			} else {
				System.err.println("Cannot interpret line '" + line + "'");
			}
		}
	}
	
	public void save(String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		Set<Map.Entry<String, String>> set = properties.entrySet();
		Iterator<Map.Entry<String, String>> iterator = set.iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, String> entry = iterator.next();
			writer.write(entry.getKey() + "=" + entry.getValue());
			writer.newLine();
		}
		writer.close();
	}
	
	public void save() throws IOException {
		save(filename);
	}
	
	public int getInt(String key) {
		try {
			return Integer.parseInt(properties.get(key));
		} catch (Exception e) {
			return Integer.parseInt(defaults.get(key));
		}
	}
	
	public double getDouble(String key) {
		try {
			return Double.parseDouble(properties.get(key));
		} catch (Exception e) {
			return Double.parseDouble(defaults.get(key));
		}
	}

	public String getString(String key) {
		return properties.get(key);
	}
	
	public void set(String key, String value) {
		properties.put(key, value);
	}
}
