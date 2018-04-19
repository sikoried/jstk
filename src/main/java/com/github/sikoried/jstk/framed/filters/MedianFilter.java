/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer
		Tobias Bocklet
		Florian Hoenig
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

package com.github.sikoried.jstk.framed.filters;

import java.io.IOException;
import java.util.Arrays;

import com.github.sikoried.jstk.io.FrameInputStream;
import com.github.sikoried.jstk.io.FrameOutputStream;
import com.github.sikoried.jstk.io.FrameSource;


/**
 * 
 * 
 * 
 * Implements a median filter, working either on framed or windowed data
 */
public class MedianFilter implements FrameSource {

	/**
	 * The input type of the data 
	 * FRAMED: input frames are treated as vectors which are filtered per dimension 
	 * WINDOWED: input frames are treated as windowed data, filtered like one continuous array
	 */
	public enum InputType {
		WINDOWED, FRAMED
	}

	/** source to filter */
	private FrameSource source = null;
	
	/** frame size */
	private int framesize = 0;
	
	/** complete size (2 * context + 1) */
	private int size = 0;
	
	/** half size (== context) */
	private int half_size = 0;
	
	/** frame context to buffer, depending on input type and context size */
	private int frame_context = 0;
	
	/** input type used */
	private InputType type = InputType.FRAMED;
	
	/** repeat edges or zero out if at beginning/end of input */
	private boolean repeat_edges = true;
	
	/** ringbuffer for the computation */
	private double [][] ringbuffer;
	
	/** helper for ringbuffer: index for next read */
	private int ringbuffer_start = 0;
	
	/** helper for ringbuffer: index for next write */
	private int ringbuffer_end = 0;
	
	/** working buffer for median computation */
	private double [] medianbuf;
	
	/** current position of "central" buffer in ringbuffer */
	private int current_buffer = 0;
	
	/** number of currently cached buffers */
	private int num_cached_buffers = 0;

	/**
	 * Standard constructor treating the input as framed data and repeating the
	 * edges
	 * 
	 * @param source
	 *            The FrameSource to read from
	 * @param size
	 *            The size of the median filter (5 means context of 2 on each
	 *            side). Must be odd.
	 */
	public MedianFilter(FrameSource source, int size) {
		this(source, size, InputType.FRAMED, true);
	}

	/**
	 * "Advanced" constructor, repeating the edges
	 * 
	 * @param source
	 *            The FrameSource to read from
	 * @param size
	 *            The size of the median filter (5 means context of 2 on each
	 *            side). Must be odd.
	 * @param type
	 *            The type of the input data, either WINDOWED or FRAMED
	 */
	public MedianFilter(FrameSource source, int size,
			MedianFilter.InputType type) {
		this(source, size, type, true);
	}

	/**
	 * Full-featured constructor
	 * 
	 * @param source
	 *            The FrameSource to read from
	 * @param size
	 *            The size of the median filter (5 means context of 2 on each
	 *            side). Must be odd.
	 * @param type
	 *            The type of the input data, either WINDOWED or FRAMED
	 * @param repeatEdges
	 *            Wether to repeat the edges (true) or use zeroes instead
	 *            (false)
	 */
	public MedianFilter(FrameSource source, int size,
			MedianFilter.InputType type, boolean repeatEdges) {
		this.type = type;
		this.source = source;
		this.framesize = source.getFrameSize();
		this.repeat_edges = repeatEdges;
		if (size % 2 != 1) {
			throw new IllegalArgumentException(
					"MedianFilter: size must be odd but is " + size);
		}
		this.size = size;
		this.half_size = (size / 2);
		switch (type) {
		case WINDOWED:
			this.frame_context = ((this.half_size) / framesize) + 1;
			break;
		case FRAMED:
			this.frame_context = half_size;
			break;
		}
		ringbuffer = new double [frame_context * 2 + 1] [framesize];
		medianbuf = new double [size];
	}

	public FrameSource getSource() {
		return source;
	}

	public int getFrameSize() {
		return framesize;
	}

	public boolean read(double [] buf) throws IOException {
		// cache as many buffers as we need and are available
		while (num_cached_buffers < ringbuffer.length) {
			if (!source.read(ringbuffer[ringbuffer_end])) {
				break;
			}
			ringbuffer_end = (ringbuffer_end + 1) % ringbuffer.length;
			num_cached_buffers++;
		}

		int ringbuffer_center = (ringbuffer_start + (ringbuffer.length / 2))
				% ringbuffer.length;

		// check if we already treated the last buffer
		if (num_cached_buffers < ringbuffer.length
				&& current_buffer == ringbuffer_end) {
			return false;
		}

		// now do the work
		for (int i = 0; i < framesize; i++) {
			for (int j = -half_size; j <= half_size; j++) {
				int index = i, buf_index = current_buffer;
				boolean isEdge = false;
				switch (type) {
				case WINDOWED:
					index = i + j;
					buf_index = current_buffer;

					if (index < 0) {
						// "fell off" on the left side. check if there is
						// another buffer left of the current one and try to
						// use it, else repeat the margin
						int buf_offset = Math.abs(index) / framesize + 1;
						int max_offset = current_buffer
								- ringbuffer_start
								+ (current_buffer - ringbuffer_start < 0 ? ringbuffer.length
										: 0);

						if (buf_offset > max_offset) {
							index = 0;
							buf_index = ringbuffer_start;
							isEdge = true;
						} else {
							buf_index -= buf_offset;
							buf_index = (buf_index + ringbuffer.length)
									% ringbuffer.length;
							index = (framesize - (Math.abs(index) + framesize)
									% framesize);
						}
					}
					if (index >= framesize) {
						// "fell off" on the right side. check if there is
						// another buffer right of the current one and try
						// to use it, else repeat the margin
						int buf_offset = Math.abs(index) / framesize;
						int max_offset = ((ringbuffer_end - 1 + ringbuffer.length) % ringbuffer.length)
								- current_buffer
								+ (((ringbuffer_end - 1 + ringbuffer.length) % ringbuffer.length)
										- current_buffer < 0 ? ringbuffer.length
										: 0);
						if (buf_offset > max_offset) {
							buf_index = ringbuffer_end - 1;
							index = framesize - 1;
							isEdge = true;
						} else {
							buf_index += buf_offset;
							buf_index %= ringbuffer.length;
							index = index % framesize;
						}
					}
					break;
				case FRAMED:
					index = i;

					buf_index = (current_buffer + j + ringbuffer.length)
							% ringbuffer.length;

					int max_offset = current_buffer
							- ringbuffer_start
							+ (current_buffer - ringbuffer_start < 0 ? ringbuffer.length
									: 0);
					if (j < 0 && Math.abs(j) > max_offset) {
						// "fell off" on the left side, no other buffer
						// there, repeat margin
						buf_index = ringbuffer_start;
						isEdge = true;
					}
					max_offset = ((ringbuffer_end - 1 + ringbuffer.length) % ringbuffer.length)
							- current_buffer
							+ (((ringbuffer_end - 1 + ringbuffer.length) % ringbuffer.length)
									- current_buffer < 0 ? ringbuffer.length
									: 0);
					if (j > 0 && j > max_offset) {
						// "fell off" on the right side, no other buffer
						// there, repeat margin
						buf_index = (ringbuffer_end - 1 + ringbuffer.length)
								% ringbuffer.length;
						isEdge = true;
					}
					break;
				}
				// fill our temporary array to find the median value
				medianbuf[j + half_size] = ((!isEdge || isEdge && repeat_edges) ? ringbuffer[buf_index][index]
						: 0.0);
			}
			// sort the array now that it's filled and copy over the values.
			// IMPORTANT: also override values in buffers as this greatly
			// influences the behaviour
			Arrays.sort(medianbuf);
			buf[i] = medianbuf[half_size];
			ringbuffer[current_buffer][i] = buf[i];
		}

		// get the index of the center of the ringbuffer
		if (current_buffer < ringbuffer_center
				&& (current_buffer >= ringbuffer_start || current_buffer < ringbuffer_end)) {
			// we are somewhere at the beginning and not even in the center yet,
			// just increase current_buffer so it gets there sometime soon
			current_buffer = (current_buffer + 1) % ringbuffer.length;
		} else {
			// we have the beginning behind us, release one buffer from the
			// cache and move on
			num_cached_buffers--;
			current_buffer = (current_buffer + 1) % ringbuffer.length;
			ringbuffer_start = (ringbuffer_start + 1) % ringbuffer.length;
		}
		return true;
	}

	public String toString() {
		return "framed.filters.MedianFilter: length = " + size
				+ ", framesize = " + framesize + ", mode: " + type.toString();
	}

	private static final String nl = System.getProperty("line.separator");
	public static final String SYNOPSIS = "Apply a median filter to a series of values"
			+ nl
			+ "usage: framed.MedianFilter <mode> <context> [--zero-edges]"
			+ nl
			+ "<mode>: either \"framed\" or \"windowed\""
			+ "\tframed: input is treated as one vector per buffer which will be medianed per dimension"
			+ nl
			+ "\twindowed: input is treated as non-overlapping windowed data which will be medianed as one stream of values"
			+ nl
			+ "<context>: context to use for the calculation of the median (median-window size is 2 * <context> + 1)"
			+ nl
			+ "--zero-edges: do not repeat the values at the beginning/end of the data (default) but use zeroes instead to fill the median-window"
			+ nl
			+ nl
			+ "input is read from stdin, output written to stdout, in frame format"
			+ nl + "";

	public static void main(String [] args) throws IOException {
		// parameters
		MedianFilter.InputType type = MedianFilter.InputType.FRAMED;
		int size = 0;
		boolean repeat_edges = true;

		// argument parsing
		if (args.length < 2 || args.length > 3) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		try {
			type = Enum.valueOf(type.getDeclaringClass(), args[0].trim()
					.toUpperCase());
		} catch (IllegalArgumentException e) {
			System.err.println("Unsupported Mode: " + args[0]);
			System.exit(1);
		}
		try {
			Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			System.err.println("Second argument must be an integer!");
			System.exit(1);
		}
		if (args.length == 3) {
			if (args[2].equals("--zero-edges")) {
				repeat_edges = false;
			} else {
				System.err.println("Unknown third argument: " + args[2]);
				System.exit(1);
			}
		}

		FrameInputStream fis = new FrameInputStream();
		MedianFilter filter = new MedianFilter(fis, 2 * size + 1, type,
				repeat_edges);
		double [] buf = new double [filter.getFrameSize()];
		FrameOutputStream fos = new FrameOutputStream(buf.length);
		while (filter.read(buf)) {
			fos.write(buf);
		}
		fos.close();
	}
}
