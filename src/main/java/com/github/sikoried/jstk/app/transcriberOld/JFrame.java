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
package com.github.sikoried.jstk.app.transcriberOld;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class JFrame extends javax.swing.JFrame {

	private static final long serialVersionUID = -6144740476260197233L;
	
	protected Preferences preferences;
	protected String name;
	
	public JFrame(String title, String name, Preferences pref) {
		super(title);
		this.name = name;
		setPreferences(pref);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				saveWindowProperties();
			}
		});

	}
	
	public JFrame(String title, String name) {
		this(title, name, null);	
	}

	public void setPreferences(Preferences preferences) {
		this.preferences = preferences;
		if (preferences != null) {
			int x = preferences.getInt(name + ".x");
			int y = preferences.getInt(name + ".y");
			int w = preferences.getInt(name + ".width");
			int h = preferences.getInt(name + ".height");
			setLocation(x, y);
			setSize(w, h);
		}
	}
	
	public void saveWindowProperties() {
		Point p = getLocation();
		preferences.set(name + ".x", Integer.toString(p.x));
		preferences.set(name + ".y", Integer.toString(p.y));
		Dimension d = getSize();
		preferences.set(name + ".width", Integer.toString(d.width));
		preferences.set(name + ".height", Integer.toString(d.height));
	}
	
}
