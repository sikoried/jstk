package de.fau.cs.jstk.sampled;

import java.io.IOException;
import de.fau.cs.jstk.framed.*;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class BandPassFilter implements AudioSource {

	private AudioSource source;
	private Window window;
	private double[] buffer;
	private double[] input1;
	private double[] input2;
	private double[] frame;
	private int[] filter;
	private DoubleFFT_1D fft;
	private int index;
	private int eof;
	private boolean finishedReading = false;
	private int total;

	public BandPassFilter(AudioSource source, double f1, double f2,
			int windowLength) throws IOException {
		this.source = source;
		window = new TriangularWindow(source, windowLength, 0);
		initializeFilter(f1, f2);
		initializeBuffer();
	}

	private void initializeFilter(double f1, double f2) {
		int size = window.getFrameSize();
		int samplerate = source.getSampleRate();
		filter = new int[size];

		for (int i = 1; i < filter.length / 2; i++) {
			double f = (double) (i * samplerate) / filter.length;
			if ((f < f1) || (f > f2)) {
				filter[2 * i] = 0;
				filter[2 * i + 1] = 0;
			} else {
				filter[2 * i] = 1;
				filter[2 * i + 1] = 1;
			}
		}
		filter[0] = 1;
		filter[1] = 1;
		if (samplerate / 2 > f2) {
			filter[1] = 0;
		}
	}

	private void initializeBuffer() throws IOException {
		int size = window.getFrameSize();
		input1 = new double[size / 2];
		input2 = new double[size / 2];
		buffer = new double[size];
		frame = new double[size];
		fft = new DoubleFFT_1D(size);
		eof = -1;

		int count = source.read(input1);
		System.arraycopy(input1, 0, frame, size / 2, size / 2);
		applyFilter(frame);
		System.arraycopy(frame, size / 2, buffer, 0, size / 2);
		if (count < size / 2) {
			eof = count;
		} else {
			count = source.read(input2);

			System.arraycopy(input1, 0, frame, 0, size / 2);
			System.arraycopy(input2, 0, frame, size / 2, size / 2);
			double[] h = input1;
			input1 = input2;
			input2 = h;

			applyFilter(frame);
			for (int i = 0; i < size / 2; i++) {
				buffer[i] += frame[i];
			}
			System.arraycopy(frame, size / 2, buffer, size / 2, size / 2);
			if (count < size / 2) {
				eof = count + size / 2;
			}
		}

		index = 0;
	}

	private void print(double[] buf) {
		System.out.print(buf[0]);
		for (int i = 1; i < buf.length; i++) {
			double h = (int) (buf[i] * 100) / 100.0;
			System.out.print(" " + h);
		}
		System.out.println();
	}

	private void applyFilter(double[] buf) {
		window.applyWindowToFrame(buf);

		fft.realForward(buf);

		for (int i = 0; i < buf.length; i++) {
			buf[i] *= filter[i];
		}

		fft.realInverse(buf, true);
	}

	@Override
	public int read(double[] buf) throws IOException {
		return read(buf, buf.length);
	}

	@Override
	public int read(double[] buf, int length) throws IOException {

		int size = frame.length;
		int numCopied = 0;

		if (finishedReading) {
			return 0;
		}

		while (numCopied < length) {

			int numAvailable = size / 2 - index;

			if (numAvailable > length - numCopied) {
				numAvailable = length - numCopied;
			}

			if ((eof != -1) && (numAvailable > eof)) {
				numAvailable = eof;
				finishedReading = true;
			}

			System.arraycopy(buffer, index, buf, numCopied, numAvailable);
			total += numAvailable;
			numCopied += numAvailable;
			index += numAvailable;

			if (finishedReading) {
				return numCopied;
			}

			if (index == size / 2) {
				int count = source.read(input2);
				if (count < 0) {
					count = 0;
				}
				for (int i = count; i < input2.length; i++) {
					input2[i] = 0;
				}
				System.arraycopy(input1, 0, frame, 0, size / 2);
				System.arraycopy(input2, 0, frame, size / 2, size / 2);
				double[] h = input1;
				input1 = input2;
				input2 = h;
				applyFilter(frame);
				for (int i = 0; i < size / 2; i++) {
					buffer[i] = frame[i] + buffer[i + size / 2];
				}
				System.arraycopy(frame, size / 2, buffer, size / 2, size / 2);
				index = 0;

				if (eof != -1) {
					eof -= size / 2;
				} else if (count < size / 2) {
					eof = size / 2 + count;
				}
			}
		}

		return numCopied;
	}

	@Override
	public int getSampleRate() {
		return source.getSampleRate();
	}

	@Override
	public boolean getPreEmphasis() {
		return source.getPreEmphasis();
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
