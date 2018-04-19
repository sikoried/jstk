/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY

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
package com.github.sikoried.jstk.io;

import java.io.IOException;

import com.github.sikoried.jstk.sampled.AudioSource;

/**
 * A reader for BufferedAudioSource that does not change the audioSource's read index
 * and can be used separately, e.g. in threads
 * @author sicawolf
 *
 */
public class BufferedAudioSourceReader implements AudioSource {
	
	/**
	 * current read position in the BufferedAudioSource
	 */
	private int currentReadIndex;
	
	/**
	 * the first index behind the read area
	 */
	private int stopIndex;
	
	/**
	 * the first index that will be read
	 */
	private int startIndex;
	
	/**
	 * the audio source to read from
	 */
	private BufferedAudioSource source;
	
	/**
	 * Creates a new BufferedAudioSourceReader that can read the whole audio source.
	 * @param audioSource the audio signal to read from
	 */
	public BufferedAudioSourceReader(BufferedAudioSource audioSource) {
		currentReadIndex = 0;
		stopIndex = -1;
		startIndex = 0;
		source = audioSource;
	}
	
	/**
	 * Creates a new BufferedAudioSourceReader that reads the specified area of the complete audio source.
	 * If the area index or length is outside the buffer range, this will be corrected internally.
	 * @param audioSource the audio signal to read from
	 * @param startIndex the first index that will be read
	 * @param numberOfSamples the number of samples that the window contains
	 */
	public BufferedAudioSourceReader(BufferedAudioSource audioSource, int startIndex, int numberOfSamples) {
		source = audioSource;
		this.startIndex = startIndex;
		
		this.stopIndex = startIndex + numberOfSamples;
		
		// correct if necessary:
		if (startIndex < 0) {
			this.startIndex = 0;
		} else if (startIndex > source.getBufferSize()) {
			this.startIndex = audioSource.getBufferSize(); //no samples will be read 
		}
		if (stopIndex < 0) {
			stopIndex = 0; // no samples will be read
		} else if (stopIndex > audioSource.getBufferSize()) {
			stopIndex = audioSource.getBufferSize();
		}
		currentReadIndex = this.startIndex;
	}
	
	/**
	 * Resets the current read position to the original one.
	 */
	public void resetReadIndex() {
		currentReadIndex = startIndex;
	}
	
	/**
	 * Sets the current read position to the specified one.
	 * @param position the position at which to start the next read
	 */
	public void setReadIndex(int position) {
		if (position < startIndex) {
			position = startIndex;
		} else if ((position > stopIndex) && (stopIndex != -1)) {
			position = stopIndex;
		}
		currentReadIndex = position;
	}

	@Override
	public boolean getPreEmphasis() {
		return source.getPreEmphasis();
	}

	@Override
	public int getSampleRate() {
		return source.getSampleRate();
	}

	@Override
	public int read(double[] buf) {
		return read(buf, buf.length);
	}
	
	public int read(double[] buf, int length) {
		int read = 0;
		int stop = currentReadIndex + length;
		for (; currentReadIndex < stop; currentReadIndex++) {
			if ((currentReadIndex >= stopIndex) && (stopIndex != -1)) {
				break;
			}
			buf[read] = source.get(currentReadIndex);
			read++;
		}
		return read;
	}
	
	public double get(int index) {
		return source.get(index);
	}
	
	public double get(double index, int interpolation) {
		return source.get(index, interpolation);
	}
	

	@Override
	public void setPreEmphasis(boolean applyPreEmphasis, double a) {
		source.setPreEmphasis(applyPreEmphasis, a);
	}

	@Override
	public void tearDown() throws IOException {
		source.tearDown();
	}

}
