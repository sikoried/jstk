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
package de.fau.cs.jstk.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import de.fau.cs.jstk.stat.Sample;


/**
 * Use the ChunkedDataSet if you have a list of files containing framed data and
 * want to read that in sequencially.
 * @author sikoried
 *
 */
public class ChunkedDataSet {
	/** 10 ms shift */
	public static double FRAME_SHIFT = 0.01;
	
	private LinkedList<Chunk> chunks = new LinkedList<Chunk> ();
	private ListIterator<Chunk> iter = null;

	private int fs = 0;  // frame size for all chunks
	
	
	/**
	 * A chunk consists of its name and a (ready-to-read) FrameInputStream
	 */
	public class Chunk {
		/** chunk name */
		private String name;
		
		/** file to work on */
		private File file;
		
		/** start frame */
		private int start = 0;
		
		/** endframe (exclusive) */
		private int end = Integer.MAX_VALUE;
		
		/**
		 * Create a new Chunk and prepare the FrameInputStream to read from the given
		 * file.
		 * 
		 * @param fileName
		 * @throws IOException
		 */
		public Chunk(File file) {
			this.file = file;
			this.name = file.getName();
			this.start = 0;
			this.end = Integer.MAX_VALUE;
		}
		
		/**
		 * Create a new Chunk and prepare the FrameInputStream to read from the given
		 * file.
		 * 
		 * @param fileName
		 * @throws IOException
		 */
		public Chunk(String name, File file, int start, int end) {
			this.name = name;
			this.file = file;
			this.start = start;
			this.end = end;
		}
		
		/** FrameInputStream allocated on demand */
		private FrameInputStream reader = null;
		
		/**
		 * Get the initialized FrameInputStream
		 */
		public FrameInputStream getFrameReader() throws IOException {
			if (reader == null) {
				if (start > 0 || end > 0)
					reader = new SegmentFrameInputStream(file, true, fs, start, end);
				else
					reader = new FrameInputStream(file, true, fs);
			}
			
			return reader;
		}
		
		public void reset() {
			reader =  null;
		}
		
		public String getName() {
			return name;
		}
	}
	
	/**
	 * Get the next Chunk from the list.
	 * @return Chunk instance on success, null if there's no more chunks
	 * @throws IOException
	 */
	public synchronized Chunk nextChunk() throws IOException {
		if (chunks.size() == 0)
			return null;
		
		if (iter == null)
			iter = chunks.listIterator();
		
		if (iter.hasNext())
			return iter.next();
		else
			iter = null;
		
		return null;
	}
	
	/**
	 * Rewind the chunk list and start again from the first.
	 */
	public synchronized void rewind() {
		for (Chunk c : chunks)
			c.reset();
	}
	
	/** 
	 * Create a ChunkDataSet using the given list file.
	 * @param listFile path to the list file
	 * @throws IOException
	 */
	public ChunkedDataSet(File listFile) throws IOException {
		this(listFile, null, 0);
	}
	
	/**
	 * Create a ChunkedDataSet using the given list file and consider them UFV
	 * @param listFile
	 * @param fs frame size to expect (no-header), 0 for Frame format
	 * @throws IOException
	 */
	public ChunkedDataSet(File listFile, int fs) throws IOException {
		this(listFile, null, fs);
	}
	
	/**
	 * Create a ChankDataSet using a given list file and the directory where the
	 * feature files are located.
	 * @param listFile
	 * @param dir input directory
	 * @param fs frame size (set > 0 for UFV)
	 * @throws IOException
	 */
	public ChunkedDataSet(File listFile, String dir, int fs) throws IOException {
		this.fs = fs;
		setChunkList(listFile, dir);
	}
	
	/**
	 * Create a ChunkedDataSet using the given list of files and a UFV frame
	 * size if necessary.
	 * @param fileNames
	 * @param fs 0 for Frame format, other for frame size (no-header)
	 */
	public ChunkedDataSet(List<File> files, int fs) throws IOException {
		this.fs = fs;
		
		// validate and add files
		for (File file : files) {
			if (file.canRead())
				chunks.add(new Chunk(file));
			else
				throw new IOException("Could not read file " + file);
		}
	}
	
	/**
	 * Get the number of Chunks in this data set
	 * @return
	 */
	public int numberOfChunks() {
		return chunks.size();
	}
	
	/**
	 * Load the given chunk list. If the parameter <dir> contains a String, this String is appended to the filename
	 * @param fileName
	 * @param dir
	 * @throws IOException
	 */
	public void setChunkList(File listFile, String dir) throws IOException {
		chunks.clear();
		BufferedReader br = new BufferedReader(new FileReader(listFile));
		int n = 0;
		String line;
		while ((line = br.readLine()) != null) {
			String [] s = line.split("\\s");
			if (s.length == 1) {
				// single file entry
				String name = line;
				if(dir != null)
					name = dir + System.getProperty("file.separator") + name;
				
				File f = new File(name);
				if (f.canRead())
					chunks.add(new Chunk(f));
			} else {
				String cname = s[0];
				String fname = s[1];
				double dstart = Double.parseDouble(s[2]);
				double dend = Double.parseDouble(s[3]);
				
				int start = (int) (dstart / FRAME_SHIFT);
				int end = (dend > 0.0 ? (int) (dend / FRAME_SHIFT) : Integer.MAX_VALUE);
				System.err.println(start);
				System.err.println(end);
				if(dir != null)
					fname = dir + System.getProperty("file.separator") + fname;
				
				File f = new File(fname);
				if (f.canRead() && start >= 0 && end > start)
					chunks.add(new Chunk(cname, f, start, end));
				else
					throw new IOException("Chunk invalid in line " + n); 
			}
			
			n++;
		}
		
		br.close();
	}
	
	/**
	 * Cache all chunks into a List<Sample> for easier (single-core) access
	 * @return
	 * @throws IOException
	 */
	public synchronized List<Sample> cachedData() throws IOException {
		// remember old index
		LinkedList<Sample> data = new LinkedList<Sample>();
		for (Chunk chunk : chunks) {
			double [] buf = new double [chunk.getFrameReader().getFrameSize()];
			while (chunk.reader.read(buf))
				data.add(new Sample((short) 0, buf));
		}
		
		return data;
	}
}
