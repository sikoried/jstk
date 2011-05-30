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
package de.fau.cs.jstk.stat.hmm;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.fau.cs.jstk.arch.Token;
import de.fau.cs.jstk.arch.TokenHierarchy;
import de.fau.cs.jstk.arch.Tokenization;
import de.fau.cs.jstk.exceptions.AlignmentException;
import de.fau.cs.jstk.exceptions.OutOfVocabularyException;
import de.fau.cs.jstk.exceptions.TrainingException;
import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.io.FrameSource;


/**
 * A MetaAlignment contains the sub-alignments of a whole utterance/sentence.
 * @author sikoried
 *
 */
public class MetaAlignment {
	private static Logger logger = Logger.getLogger(MetaAlignment.class);
	
	public double score = 0.;
	
	/**
	 * The Turn class holds all information about a turn: file name and input/output
	 * directory.
	 * @author sikoried
	 */
	public static class Turn {
		public String fileName;
		public String transcription;
		public String inDir;
		public String outDir;
		
		/**
		 * Create a new Turn from the given turn line.
		 * @param line Line containing "turn-id and its transcription"
		 * @param inDir
		 * @param outDir
		 */
		public Turn(String line, String inDir, String outDir) {
			String trim = line.trim().replaceAll("\\s+", " ");
			int pos = trim.indexOf(" ");
			if (pos == -1)
				throw new IllegalArgumentException("missing transcription in line " + line);
			
			fileName = trim.substring(0, pos);
			transcription = trim.substring(pos + 1);
			
			this.inDir = inDir;
			this.outDir = outDir;
		}
		
		/**
		 * String summary of the turn.
		 */
		public String toString() {
			return 
				inDir + System.getProperty("file.separator") + fileName + " " + 
				outDir + System.getProperty("file.separator") + fileName + " " + 
				transcription;
		}
		
		public String canonicalInputName() { 
			return inDir + System.getProperty("file.separator") + fileName;
		}
		
		public String canonicalOutputName() {
			return outDir + System.getProperty("file.separator") + fileName;
		}
		
		/**
		 * Read in a turn list.
		 * @param listFile
		 * @param inDir
		 * @param outDir
		 * @return
		 * @throws IOException
		 */
		public static List<Turn> readTurnList(String listFile, String inDir, String outDir) throws IOException {
			LinkedList<Turn> tl = new LinkedList<Turn>();
			BufferedReader br = new BufferedReader(new FileReader(listFile));
			String line;
			
			while ((line = br.readLine()) != null)
				tl.add(new Turn(line, inDir, outDir));
			
			br.close();
			
			return tl;
		}
	}
	
	/** The actual alignments, with references to the models */
	public List<Alignment> alignments = new LinkedList<Alignment>();
	
	/** TokenTree to resolve tokens and HMM ids */
	public TokenHierarchy th = null;
	
	/**
	 * Create a new (empty) MetaAlignment which uses the given TokenTree
	 * @param tt 
	 */
	public MetaAlignment(TokenHierarchy th) {
		this.th = th;
	}
	
	/**
	 * Create a new MetaAlignment containing the given alignments referencing
	 * the given TokenTree
	 * @param tt
	 * @param alignments
	 */
	public MetaAlignment(TokenHierarchy th, List<Alignment> alignments) {
		this.th = th;
		this.alignments = alignments;
	}
	
	/**
	 * Load a MetaAlignment from a given file, cache the feature data and
	 * map the models to the given TokenTree (for thread safety). The alignment
	 * may specify a state sequence. If the state sequence is missing, a forced
	 * alignment is computed.
	 * @param source
	 * @param in expecting lines "model num-frames [state-seq]"
	 * @param tt
	 * @param forcedInsteadOfLinear
	 * @throws IOException
	 * @throws OutOfVocabularyException
	 */
	public MetaAlignment(FrameSource source, BufferedReader in, TokenHierarchy th, boolean forcedInsteadOfLinear)
		throws AlignmentException, OutOfVocabularyException, IOException {
		this(th);
		read(source, in, forcedInsteadOfLinear);		
	}
	
	/**
	 * Create a new MetaAlignment by aligning a transcription to a 
	 * feature sequence.
	 * @param source
	 * @param transcription
	 * @param tt
	 * @param forcedInsteadOfLinear Use forced alignment instead of linear
	 * @throws OutOfVocabularyException
	 * @throws IOException
	 */
	public MetaAlignment(FrameSource source, Iterable<Tokenization> transcription, TokenHierarchy th, boolean forcedInsteadOfLinear)
		throws AlignmentException, OutOfVocabularyException, IOException {
		this(th);
		score = align(source, transcription, forcedInsteadOfLinear);
	}
	
	/**
	 * For each alignment, follow the token hierarchy and duplicate the
	 * alignment but attach the more general token. This corresponds to the 
	 * ISADORA APIS training, yet in a more straight-forward way.
	 * This should not be called before saving the MetaAlignment. Also, there
	 * is no check whether or not a duplicated Alignment already exists.
	 */
	public void explode() throws AlignmentException {
		// keep the duplicates separately for now
		List<Alignment> duplicates = new LinkedList<Alignment>();
		for (Alignment a : alignments) {
			// follow the hierarchy up to the top
			Token tok = th.getPolyphone(a.model.id);
			while ((tok = tok.lessContext) != null) {
				if (a.q != null)
					duplicates.add(new Alignment(tok.hmm, a.observation, a.q));
				else
					duplicates.add(new Alignment(tok.hmm, a.observation));
			}
		}
		
		// add the duplicates
		alignments.addAll(duplicates);
	}
	
	/**
	 * Generate a String representation. For debug only, for I/O use read/write.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (Alignment a : alignments)
			sb.append(th.getPolyphone(a.model.id) + (a.q == null ? " " : " " + Arrays.toString(a.q) + " "));
		return sb.toString();
	}
	
	/**
	 * Compute the meta-alignment for the given feature file and transcription.
	 * You may request a forced alignment (instead of the linear time alignment).
	 * @param source
	 * @param transcription
	 * @param forcedInsteadOfLinear
	 * @return overall alignment score
	 * @throws OutOfVocabularyException
	 * @throws IOException
	 */
	public double align(FrameSource source, Iterable<Tokenization> transcription, boolean forcedInsteadOfLinear) 
		throws AlignmentException, OutOfVocabularyException, IOException {
		// get overall transcription (models are required for later MetaAlignment construction)
		List<Token> tseq = new LinkedList<Token>();
		for (Tokenization t : transcription) {
			for (Token tt : th.tokenizeWord(t.sequence))
				tseq.add(tt);
		}
		
		// build up model sequence
		Hmm [] seq = new Hmm [tseq.size()];
		for (int i = 0; i < tseq.size(); ++i)
			seq[i] = tseq.get(i).hmm;
		
		// build up meta HMM
		Hmm meta = new Hmm(seq);
		
		// cache data
		LinkedList<double []> cache = new LinkedList<double []>();
		double [] buf = new double [source.getFrameSize()];
		while (source.read(buf))
			cache.add(buf.clone());
		
		// check if the observation sequence is long enough
		if (cache.size() < meta.ns) {
			logger.fatal("MetaAlignment.align(): observation shorter than meta HMM");
			if (source instanceof FrameInputStream)
				logger.fatal("possibly broken file or transcription: " + ((FrameInputStream) source).getFileName(true));
			
			throw new AlignmentException("MetaAlignment.align(): observation shorter than meta HMM");
		}
		
		// compute the overall alignment
		Alignment a = new Alignment(meta, cache);
		
		if (forcedInsteadOfLinear)
			a.forcedAlignment();
		else
			a.forceLinearAlignment();
		
		// split the alignment in the subsequences
		alignments = new LinkedList<Alignment>();
		int firstState = 0;
		int lastState;
		int t = 0;
		Iterator<double []> dx = cache.iterator();
		for (int i = 0; i < seq.length; ++i) {
			// model state ranges
			lastState = firstState + seq[i].ns - 1;
			
			// copy the alignment info
			LinkedList<Integer> q = new LinkedList<Integer>();
			LinkedList<double []> o = new LinkedList<double []>();
			while (t < cache.size() && a.q[t] <= lastState) {
				o.add(dx.next());
				q.add(a.q[t] - firstState);
				t++;
			}
			
			// update range for next iteration
			firstState = lastState + 1;
			
			if (o.size() < seq[i].ns) {
				logger.fatal("MetaAlignment.align(): observation subsequence shorter than number of states at submodel " + i);
				logger.fatal(meta.toString());
				logger.fatal(a.toString());
				if (source instanceof FrameInputStream)
					logger.fatal("possible file or transcription: " + ((FrameInputStream) source).getFileName(true));
				
				throw new AlignmentException("MetaAlignment.align(): observation subsequence shorter than token HMM");
			}
			
			// allocate the sub alignment
			alignments.add(new Alignment(seq[i], o, q));
		}
		
		return a.score;
	}

	/**
	 * Read the alignment from an BufferedReader.
	 * @param source associated feature data
	 * @param in initialized stream to read the alignments from
	 * @throws IOException
	 */
	public void read(FrameSource source, BufferedReader in, boolean forcedInsteadOfLinear) 
		throws AlignmentException, OutOfVocabularyException, IOException {
		// clear old alignments
		alignments = new LinkedList<Alignment>();
		
		// cache feature data
		double [] buf = new double [source.getFrameSize()];
		LinkedList<double []> data = new LinkedList<double []>();
		while (source.read(buf))
			data.add(buf.clone());
		
		// read alignments
		String line;
		while ((line = in.readLine()) != null) {
			String [] split = line.split("\\s+");
			
			// get the Token and respective model
			Token tok = th.getPolyphone(split[0]);
			if (tok == null)
				throw new OutOfVocabularyException("MetaAlignment.read(): model '" + split[0] + "' not in TokenTree.");
			if (tok.hmm == null)
				throw new AlignmentException("MetaAlignment.read(): token '" + split[0] + "' has no attached HMM.");
			
			Hmm model = tok.hmm;
			
			// validate length
			int length = Integer.parseInt(split[1]);
			if (data.size() < length) {
				// throw new IOException("MetaAlignment.read(): Alignment is too long for feature file, aborting.");
				logger.info("MetaAlignment.read(): alignment is too long, shortening to feature sequence! Are you using manual alignments?");
				if (source instanceof FrameInputStream)
					logger.info("possibly broken file or transcription: " + ((FrameInputStream) source).getFileName(true));
				length = data.size();
			}
			
			// is there a frame alignment?
			int [] qstar = null;
			if (split.length > 2) {
				qstar = new int [split.length - 2];
				for (int i = 0; i < qstar.length; ++i)
					qstar[i] = Integer.parseInt(split[2 + i]);
			}
			
			// build up the alignment
			LinkedList<double []> seq = new LinkedList<double []>();
			for (int i = 0; i < length; ++i) {
				seq.add(data.remove());
			}
			
			// validate alignment
			if (seq.size() < model.ns)
				throw new TrainingException("Alignment for " + split[0] + "too short! Check your alignment file.");
			
			// initialize the new alignment
			if (qstar != null)
				alignments.add(new Alignment(model, seq, qstar));
			else {
				Alignment alg = new Alignment(model, seq);
				if (forcedInsteadOfLinear)
					alg.forcedAlignment();
				else
					alg.forceLinearAlignment();
				
				alignments.add(alg);
			}
		}
	}
	
	/**
	 * Write the alignment to an BufferedWriter. Format is "model no-frames [state-seq]".
	 * @param out initialized stream to write the alignment to
	 * @return
	 */
	public void write(BufferedWriter out) throws IOException {
		for (Alignment a : alignments)
			out.write(a.pack() + "\n");
		out.flush();
	}
}
