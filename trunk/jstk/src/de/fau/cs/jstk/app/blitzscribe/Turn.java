/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		
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

package de.fau.cs.jstk.app.blitzscribe;

import java.text.DecimalFormat;
import java.util.Arrays;

public class Turn {
	public final int id;
	
	public String file;
	public String text;
	
	public Turn(String line, int id) {
		this.id = id;
		line = line.trim();
		int p = line.indexOf(' ');
		if (p < 0) {
			file = line;
			text = "";
		} else {
			file = line.substring(0, p);
			text = line.substring(p).trim();
		}
	}
	
	public Turn(String file, String text, int id) {
		this.id = id;
		this.file = file;
		this.text = (text == null ? "" :  text.trim());
	}
	
	public String getShortName() {
		try {
			String f = file;
			int p = f.lastIndexOf(".");
			
			if (p > 0) {
				String ext = f.substring(p);
				final String [] valid = new String [] { ".wav" };
				if (Arrays.asList(valid).contains(ext))
					f = f.substring(0, p);
				else
					throw new Exception("invalid file exception " + ext);
			}
			
			String [] split = f.split("_");
			
			String ss = split[split.length-2];
			String se = split[split.length-1];
			
			ss = ss.replaceFirst("^0+", "");
			se = se.replaceFirst("^0+", "");
			
			int ms1 = Integer.parseInt(ss);
			int ms2 = Integer.parseInt(se);
			
			DecimalFormat twoDForm = new DecimalFormat("000.00");
			DecimalFormat intFormat = new DecimalFormat("0000");
	        return intFormat.format(id) + " " + twoDForm.format((double) (ms2 - ms1) / 1000 );
		} catch (Exception e) {
			System.err.println(e.toString());
			return file;
		}
	}
	
	public String toString() {
		return getShortName() + (text.length() > 0 ? " " + text : "");
	}
	
	public String toFinalString() {
		return file + (text.length() > 0 ? " " + text : "");
	}
}
