package de.fau.cs.jstk.app.blitzscribe;

public class Turn {
	public String file;
	public String text;
	
	public Turn(String line) {
		line = line.trim();
		int p = line.indexOf(' ');
		if (p < 0) {
			file = line;
			text = "";
		} else {
			file = line.substring(0, p);
			text = line.substring(p);
		}
	}
	
	public Turn(String file, String text) {
		this.file = file;
		this.text = (text == null ? "" :  text);
	}
	
	public String toString() {
		return file + (text.length() > 0 ? " " + text : "");
	}
}
