package de.fau.cs.jstk.app.transcriber;

import java.io.IOException;

public interface Preferences {
	public void load(String filename) throws IOException;

	public void save(String filename) throws IOException;

	public void save() throws IOException;

	public int getInt(String key);

	public double getDouble(String key);

	public String getString(String key);

	public void set(String key, String value);

}
