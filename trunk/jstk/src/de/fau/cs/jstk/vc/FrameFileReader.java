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
package de.fau.cs.jstk.vc;

import java.io.*;

import de.fau.cs.jstk.io.FrameSource;

public class FrameFileReader implements FrameSource {

	private BufferedReader reader;
	private int frameSize = -1;
	private String[] tokens;

	public FrameFileReader(String filename) throws IOException {
		reader = new BufferedReader(new FileReader(filename));
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				throw new IOException("No data found");
			}

			line = line.trim();

			if ((line.equals("")) || (line.charAt(0) == '@')) {
				continue;
			}
			tokens = line.split("[, \t]+");
			frameSize = tokens.length;
			return;
		}
	}

	@Override
	public boolean read(double[] buf) throws IOException {
		if (tokens == null) {
			return false;
		}

		if (buf.length < frameSize) {
			throw new IOException("buffer too small for frame size: "
					+ buf.length + " < " + frameSize);
		}

		for (int i = 0; i < frameSize; i++) {
			try {
				buf[i] = Double.valueOf(tokens[i]).doubleValue();
			} catch (NumberFormatException e) {
				throw new IOException("invalid double value: " + tokens[i]);
			}
		}

		String line = reader.readLine();

		if (line != null) {
			line = line.trim();
			tokens = line.split("[ \t]+");
			if (tokens.length != frameSize) {
				throw new IOException("Varying number of features per frame");
			}
		} else {
			tokens = null;
			reader.close();
		}

		return true;
	}

	@Override
	public int getFrameSize() {
		return frameSize;
	}

	@Override
	public FrameSource getSource() {
		return null;
	}

}
