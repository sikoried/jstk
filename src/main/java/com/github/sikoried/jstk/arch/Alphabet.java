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
package com.github.sikoried.jstk.arch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * The alphabet contains the valid tokens with the respective number of HMM states.
 * 
 * @author sikoried
 */
public final class Alphabet {
	/** Lookup table: token->num-hmm-states */
	public HashMap<String, Short> lookup = new HashMap<String, Short>();
	
	/**
	 * Generate a new empty Alphabet
	 */
	public Alphabet() {
		
	}
	
	/**
	 * Generate a new Alphabet and load the entries from the given file.
	 * @param file
	 * @throws IOException
	 */
	public Alphabet(File file) throws IOException {
		String buf;
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		while ((buf = br.readLine()) != null) {
			if (buf.startsWith(";"))
				continue;
			String [] split = buf.trim().split("\\s+");
			lookup.put(split[0], Short.parseShort(split[1]));
		}
	}
	
	/**
	 * Check if a given String represents a valid token.
	 * @param val
	 * @return
	 */
	public boolean isValidToken(String val) {
		return lookup.containsKey(val);
	}
	
	/**
	 * Generate a String representation of the Alphabet.
	 */
	public String toString() {
		return "Alphabet with " + lookup.size() + " tokens.";
	}
	
	/**
	 * Dump an ASCII representation to the given stream
	 * @param ps
	 */
	public void dump(PrintStream ps) {
		for (Entry<String, Short> e : lookup.entrySet())
			ps.println(e.getKey() + "\t" + e.getValue());
	}
	
	public static final String SYNOPSIS = 
		"sikoried, 7/26/2010\n" +
		"The Alphabet can  be used to verify a alphabet from the given file.\n" +
		"usage: Alphabet alphabet-file \n";
	
	public static void main(String [] args) throws IOException {
		if (args.length != 1) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}

		Alphabet a = new Alphabet(new File(args[0]));
		
		a.dump(System.out);
	}
}
