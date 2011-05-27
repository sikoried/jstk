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
package de.fau.cs.jstk.io;

import de.fau.cs.jstk.sampled.*;
import de.fau.cs.jstk.vc.interfaces.AudioBufferListener;
import de.fau.cs.jstk.vc.interfaces.SignalSectionSelectedListener;
import de.fau.cs.jstk.vc.interfaces.VisualizationListener;

import java.io.IOException;
import java.util.ListIterator;
import java.util.Vector;

/**
 * A Buffer for the complete audio data.
 * 
 */
public class BufferedAudioSource implements AudioSource, Runnable {

	public static final int NO_INTERPOLATION = 0;
	public static final int LINEAR_INTERPOLATION = 1;
	public static final int SINC_INTERPOLATION = 2;

	private static final int BUFFER_SIZE_EXP = 18;
	private static final int BUFFER_SIZE = 1 << BUFFER_SIZE_EXP; // ca. 16 s bei
										 						// 16kHz

	private Vector<AudioBufferListener> audioBufferListeners;

	/**
	 * the source file to read from
	 */
	private AudioSource audioSource;

	/**
	 * the last position (buffer[bufferIndex][bufferPosition]) that has not been
	 * read yet;
	 */
	private int bufferIndex = 0;
	private int bufferPosition = 0;

	/**
	 * Number of samples that have already been read
	 */
	private int samplesRead = 0;

	/**
	 * total number of samples stored in this buffered audio source
	 */
	private int numSamples = 0;

	/**
	 * the values stored in a 2-dimensional array to avoid copying data while
	 * reading long files
	 */
	private double[][] buffer = null;

	/**
	 * the maximum value that occurs in buffer (will be computed once only)
	 */
	private double maximum = 0;

	/**
	 * the minimum value that occurs in buffer (will be computed only once)
	 */
	private double minimum = 0;

	private boolean stopRequest = false;

	public boolean stillReading = false;

	/**
	 * Gets a new Buffer for the AudioSource file that provides all its values.
	 * Note that if the AudioSource is an AudioFileListReader, then the values
	 * of all the files will be kept in the buffer, and getSampleRate() will
	 * return the last file's sample rate.
	 * 
	 * @param source
	 *            the AudioFileReader or AudioFileListReader whose values will
	 *            be buffered
	 * @throws IllegalArgumentException
	 *             - if the AudioSource is an AudioCapture (because streams
	 *             cannot be read to end and buffered).
	 */
	public BufferedAudioSource(AudioSource source) {
		if (source instanceof AudioCapture) {
			throw new IllegalArgumentException(
					"The AudioSourceBuffer will not work for streams.");
		}
		audioSource = source;

		audioBufferListeners = new Vector<AudioBufferListener>();

		Thread t = new Thread(this);
		long start = System.currentTimeMillis();
		t.start();

		/*
		 * try { while (t.isAlive()) { Thread.sleep(100); } } catch
		 * (InterruptedException e) { e.printStackTrace(); } long end =
		 * System.currentTimeMillis(); long duration = (end - start) / 1000;
		 * System.err.println(duration + " seconds");
		 */
	}

	/**
	 * Reads all the sample values of the audio source into the internal buffer
	 * and closes the file reader.
	 */
	private void fillBuffer() {
		double[] newValues = new double[BUFFER_SIZE];
		buffer = new double[128][];
		int more = 1;
		int index = 0;
		stillReading = true;

		while (more > 0) {

			if (stopRequest) {
				stillReading = false;
				System.err.println("Reading stopped");
				break;
			}

			try {
				more = audioSource.read(newValues);
			} catch (IOException e) {
				break;
			}
			if (more > 0) {
				if (buffer[index] == null) {
					buffer[index] = newValues;
					index++;
					newValues = new double[BUFFER_SIZE];
				} else {
					System.err
							.println("this case is not handled properly and should not have happened");
				}
				numSamples += more;
				//informAudioBufferListeners();
			}

			if (index == buffer.length) {
				double[][] save = buffer;
				buffer = new double[buffer.length + 128][];
				System.arraycopy(save, 0, buffer, 0, save.length);
			}
			
			System.err.println(numSamples + " samples available; " + more + " new samples");
		}

		stillReading = false;
		informAudioBufferListeners();
		System.err.println("Reading finished: " + numSamples + " samples");

		try {
			audioSource.tearDown();
		} catch (IOException e) {
		}
	}

	/**
	 * Returns the number of samples that are available in the buffer.
	 * 
	 * @return the number of samples, or 0 if it is not yet filled.
	 */
	public int getBufferSize() {
		return numSamples;
	}

	@Override
	public boolean getPreEmphasis() {
		return audioSource.getPreEmphasis();
	}

	@Override
	public int getSampleRate() {
		return audioSource.getSampleRate();
	}

	public int read(double[] buf) throws IOException {
		return read(buf, buf.length);
	}

	@Override
	public int read(double[] buf, int length) throws IOException {
		if (stillReading) {
			while ((samplesRead + length >= numSamples) && stillReading) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			}
		}

		if ((numSamples == 0) || (samplesRead >= numSamples)) {
			return -1;
		}

		// Do not read more samples than are available
		if (samplesRead + length > numSamples) {
			length = numSamples - samplesRead;
		}

		if (bufferPosition + length <= BUFFER_SIZE) {
			System.arraycopy(buffer[bufferIndex], bufferPosition, buf, 0,
					length);
			bufferPosition += length;

			if (bufferPosition == BUFFER_SIZE) {
				bufferIndex++;
				bufferPosition = 0;
			}
		} else {
			int missing = length;
			int pos = 0;
			while (missing > 0) {
				int copy = BUFFER_SIZE - bufferPosition;
				if (copy > missing) {
					copy = missing;
				}
				System.arraycopy(buffer[bufferIndex], bufferPosition, buf, pos,
						copy);
				bufferPosition += copy;

				if (bufferPosition == BUFFER_SIZE) {
					bufferIndex++;
					bufferPosition = 0;
				}

				pos += copy;
				missing -= copy;
			}
		}

		samplesRead += length;
		return length;
	}

	/**
	 * Creates a new BufferedAudioSourceReader that can be used as a new
	 * AudioSource, starting read from the beginning.
	 * 
	 * @return the BufferedAudioSourceReader that does not change this source's
	 *         read index and can be used separately (e.g. in threads)
	 */
	public BufferedAudioSourceReader getReader() {
		return new BufferedAudioSourceReader(this);
	}

	/**
	 * Creates a new BufferedAudioSourceReader that reads up to numberOfSamples
	 * values from the buffer beginning at the specified startIndex.
	 * 
	 * @param startIndex
	 *            where to start reading
	 * @param numberOfSamples
	 *            the maximum number of values to read (e.g. frame size)
	 * @return the BufferedAudioSourceReader that will provide the frame
	 */
	public BufferedAudioSourceReader getReader(int startIndex,
			int numberOfSamples) {
		return new BufferedAudioSourceReader(this, startIndex, numberOfSamples);
	}

	@Override
	public void setPreEmphasis(boolean applyPreEmphasis, double a) {
		audioSource.setPreEmphasis(applyPreEmphasis, a);
	}

	@Override
	public void tearDown() throws IOException {
		buffer = null; // delete buffer values!
		// audioSource.tearDown() has already been called in fillBuffer() from
		// the constructor
	}

	/**
	 * Gets the sample value at the specified index.
	 * 
	 * @param index
	 *            the sample index in the sample buffer
	 * @return the sample value at index
	 */
	public double get(int index) {
		if (stillReading) {
			while ((index >= numSamples) && stillReading) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			}
		}

		if ((index < 0) || (index >= numSamples)) {
			return 0;
		}

		int bufIdx = index >> BUFFER_SIZE_EXP;
		int bufPos = index & (BUFFER_SIZE - 1);

		return buffer[bufIdx][bufPos];
	}

	/**
	 * Gets the interpolated sample value at the specified position.
	 * 
	 * @param index
	 *            position at which the interpolated value has to be calculated
	 * @param interpolation
	 *            type of interpolation: LINEAR_INTERPOLATION,
	 *            SINC_INTERPOLATION, default: value at the rounded down index
	 *            in the buffer
	 * @return interpolated value
	 */
	public double get(double index, int interpolation) {
		if (stillReading) {
			while ((index >= numSamples) && stillReading) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			}
		}

		if ((index < 0) || (index >= numSamples)) {
			return 0;
		}

		switch (interpolation) {
		case LINEAR_INTERPOLATION:
			int i1 = (int) index;
			int i2 = i1 + 1;
			if (i1 < 0) {
				i1 = 0;
			}

			int bufIdx1 = i1 >> BUFFER_SIZE_EXP;
			int bufPos1 = i1 & (BUFFER_SIZE - 1);
			int bufIdx2 = bufIdx1;
			int bufPos2 = bufPos1 + 1;
			if (bufPos2 >= BUFFER_SIZE) {
				bufPos2 = 0;
				bufIdx2++;
			}

			if (i2 >= numSamples) {
				return buffer[bufIdx1][bufPos1];
			}

			double v = (index - i1) * buffer[bufIdx1][bufPos1] + (i2 - index)
					* buffer[bufIdx2][bufPos2];
			return v;
		case SINC_INTERPOLATION:
			i1 = (int) index - 500;
			i2 = (int) index + 500;
			if (i1 < 0) {
				i1 = 0;
			}
			if (i2 >= buffer.length) {
				i2 = buffer.length - 1;
			}
			v = 0;
			for (int i = i1; i <= i2; i++) {
				double h = Math.PI * (index - i);
				int bufIdx = i >> BUFFER_SIZE_EXP;
				int bufPos = i & (BUFFER_SIZE - 1);
				if (h == 0) {
					v += buffer[bufIdx][bufPos];
				} else {
					v += buffer[bufIdx][bufPos] * Math.sin(h) / h;
				}
			}
			return v;
		default:
			int bufIdx = ((int) index) >> BUFFER_SIZE_EXP;
			int bufPos = ((int) index) & (BUFFER_SIZE - 1);
			return buffer[bufIdx][bufPos];
		}
	}

	/**
	 * Gets the maximum value of the buffer. It will be computed only the first
	 * time this method is called.
	 * 
	 * @return the buffer's maximum
	 */
	public double getMax() {
		if (maximum == 0) {
			if (buffer == null || numSamples < 1) {
				return 0;
			}
			double max = buffer[0][0];
			int c = 0;
			loop: for (int i = 0; i < buffer.length; i++) {
				for (int j = 0; j < buffer[i].length; j++) {
					c++;
					if (c > numSamples) {
						break loop;
					}
					if (buffer[i][j] > max) {
						max = buffer[i][j];
					}
				}
			}
			maximum = max;
			return max;
		}
		return maximum;
	}

	/**
	 * Gets the minimum value of the buffer. It will be computed only if this
	 * method is called for the first time.
	 * 
	 * @return the buffer's minimum.
	 */
	public double getMin() {
		if (minimum == 0) {
			if (buffer == null || numSamples < 1) {
				return 0;
			}
			double min = buffer[0][0];
			int c = 0;
			loop: for (int i = 0; i < buffer.length; i++) {
				for (int j = 0; j < buffer[i].length; j++) {
					c++;
					if (c > numSamples) {
						break loop;
					}
					if (buffer[i][j] > min) {
						min = buffer[i][j];
					}
				}
			}
			minimum = min;
			return min;
		}
		return minimum;
	}

	/**
	 * Gets the corresponding millisecond value for the specified index.
	 * 
	 * @param index
	 *            the sample value index
	 * @return the time in milliseconds when the sample occurs
	 */
	public double getBufferIndexAsMilliseconds(int index) {
		return (double) ((index) / (double) audioSource.getSampleRate()) * 1000.0;
	}

	/**
	 * Gets the corresponding sample index for the specified milliseconds.
	 * 
	 * @param milliseconds
	 *            the milliseconds that should be converted to a sample buffer
	 *            index
	 * @return the sample value index
	 */
	public int getMillisecondsAsBufferIndex(double milliseconds) {
		double rate = milliseconds
				/ (double) getBufferIndexAsMilliseconds(buffer.length);
		return (int) (rate * (double) buffer.length);
	}

	@Override
	public void run() {
		System.err.println("Thread started: reading file");
		fillBuffer();
	}

	public void addBufferListener(AudioBufferListener listener) {
		synchronized(audioBufferListeners) {
			audioBufferListeners.add(listener);
		}
	}

	public void informAudioBufferListeners() {
		synchronized (audioBufferListeners) {
			ListIterator<AudioBufferListener> iterator = audioBufferListeners
					.listIterator();
			// System.err.println("Informing audio buffer listeners: " +
			// numSamples);
			while (iterator.hasNext()) {
				AudioBufferListener listener = iterator.next();
				listener.newSamplesAvailable(numSamples);
			}
		}
	}

	/*
	 * /** tries to read as many doubles into buf as possible, starting from
	 * index
	 * 
	 * @param buf the buffer to write to
	 * 
	 * @param startIndex the first index of the values in the file
	 * 
	 * @return the number of values that have been read /
	 * 
	 * @Override public int read(double[] buf, int startIndex) { int read = 0;
	 * if (startIndex < 0 || startIndex >= buffer.length) { return read; }
	 * 
	 * int endIndex = startIndex + buf.length; if (endIndex >= buffer.length) {
	 * endIndex = buffer.length; } for (int i = startIndex; i < endIndex; i++) {
	 * buf[i - startIndex] = buffer[i]; read++; }
	 * 
	 * return read; }
	 */

	/*
	 * /** copies the values from file[startIndex] until file[startIndex +
	 * howMany - 1] to a new double[]
	 * 
	 * @param startIndex the first index to copy
	 * 
	 * @param howMany the number of values to be copied
	 * 
	 * @return the values copied from the buffer /
	 * 
	 * @Override public double[] read(int startIndex, int howMany) { int
	 * endIndex = startIndex + howMany; if (buffer == null || startIndex < 0 ||
	 * howMany < 1 || howMany > buffer.length || endIndex > buffer.length) {
	 * return null; } double [] values = new double[howMany]; for (int i =
	 * startIndex; i < endIndex; i++) { values[i - startIndex] = buffer[i]; }
	 * return values; }
	 */

	/*
	 * /** Gets the buffer that contains the file's values.
	 * 
	 * @return the buffer that contains all values (note that if you change
	 * them, then the originals are no longer available) / public double[]
	 * bufferValues () { return buffer; }
	 */

}
