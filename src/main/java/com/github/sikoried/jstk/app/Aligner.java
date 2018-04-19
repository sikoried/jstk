/*
	Copyright (c) 2009-2011
		Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
		Korbinian Riedhammer, 
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
package com.github.sikoried.jstk.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import com.github.sikoried.jstk.arch.Configuration;
import com.github.sikoried.jstk.arch.TokenHierarchy;
import com.github.sikoried.jstk.arch.Tokenizer;
import com.github.sikoried.jstk.arch.TreeNode;
import com.github.sikoried.jstk.decoder.ViterbiBeamSearch;
import com.github.sikoried.jstk.exceptions.AlignmentException;
import com.github.sikoried.jstk.exceptions.OutOfVocabularyException;
import com.github.sikoried.jstk.io.FrameInputStream;
import com.github.sikoried.jstk.lm.FixedSequences;
import com.github.sikoried.jstk.stat.hmm.MetaAlignment;
import com.github.sikoried.jstk.stat.hmm.MetaAlignment.Turn;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;


/**
 * Use the Aligner to compute forced or linear alignments.
 * @author sikoried
 */
public class Aligner {
	private static Logger logger = LogManager.getLogger(Aligner.class);

	static class Distributor {
		List<Turn> turns = null;
		
		/** Turn iterator, handled by next() */
		Iterator<Turn> it = null;
		
		/**
		 * Generate a new Job Distributor, managing the given list of jobs.
		 * @param turns
		 */
		Distributor(List<Turn> turns) {
			this.turns = turns;
			rewind();
		}
		
		void rewind() {
			it = turns.iterator();
		}
		
		/**
		 * Get the next job in line. This method is synchronized an is used
		 * by the executing Worker threads.
		 * @return next job in line
		 */
		synchronized Turn next() {
			if (it.hasNext())
				return it.next();
			else 
				return null;
		}
	}
	
	/**
	 * The Worker class receives jobs from the distributor and executes them as
	 * a system level thread.
	 * 
	 * @author sikoried
	 */
	private static class Worker implements Runnable {
		Tokenizer tok;
		TokenHierarchy th;
		Distributor distributor;
		CountDownLatch latch;
		
		boolean forced;
		
		long jobs = 0;
		
		/**
		 * Initialize a new Worker with the given alphabet, tokenizer, hierarchy
		 * and turn distributor.
		 * @param tok
		 * @param th TokenHierarchy exclusively for this thread
		 * @param d
		 * @param forcedInsteadOfLinear Use forced alignment instead of linear alignments
		 * @param latch count-down latch for thread synchronization
		 */
		Worker(Tokenizer tok, TokenHierarchy th, Distributor d, boolean forcedInsteadOfLinear, CountDownLatch latch) {
			this.tok = tok;
			this.th = th;
			this.distributor = d;
			this.forced = forcedInsteadOfLinear;
			this.latch = latch;
		}
		
		public void run() {
			try {
				Turn t;
				while ((t = distributor.next()) != null) {
					FrameInputStream fs = new FrameInputStream(new File(t.canonicalInputName()));
					MetaAlignment ma = new MetaAlignment(fs, tok.getSentenceTokenization(t.transcription), th, forced);
					BufferedWriter bw = new BufferedWriter(new FileWriter(t.canonicalOutputName()));
					ma.write(bw);
					
					bw.close();
					fs.close();
					
					jobs++;
				}
				
				logger.info("Aligner.Worker#" + Thread.currentThread().getId() + ".run(): processed " + jobs + " alignments");
			} catch (OutOfVocabularyException e) {
				logger.fatal("Aligner.Worker#" + Thread.currentThread().getId() + ".run(): OutOfVocabularyException " + e.toString());
			} catch (IOException e) {
				logger.fatal("Aligner.Worker#" + Thread.currentThread().getId() + ".run(): IOException " + e.toString());
			} catch (AlignmentException e) {
				logger.fatal("Aligner.Worker#" + Thread.currentThread().getId() + ".run(): AlignmentException " + e.toString());
			} finally {
				// notify the main thread
				latch.countDown();
			}
		}
	}
	
	/**
	 * The BWorker class receives jobs from the distributor and executes them as
	 * a system level thread. This is the beam alignment
	 * 
	 * @author sikoried
	 */
	private static class BWorker implements Runnable {
		Tokenizer tok;
		TokenHierarchy th;
		Distributor distributor;
		CountDownLatch latch;
		
		int bs, bi;
		double bw;
		String silences;
		boolean force_keep_silences;
		
		long jobs = 0;
		
		
		/**
		 * Initialize a new Worker with the given alphabet, tokenizer, hierarchy
		 * and turn distributor.
		 * @param tok
		 * @param th TokenHierarchy exclusively for this thread
		 * @param d
		 * @param bs size of the beam
		 * @param latch count-down latch for thread synchronization
		 */
		BWorker(Tokenizer tok, TokenHierarchy th, Distributor d, int beam_size, int beam_incr, double bw, String silences, boolean force_keep_silences, CountDownLatch latch) {
			this.tok = tok;
			this.th = th;
			this.distributor = d;
			this.bs = beam_size;
			this.bi = beam_incr;
			this.bw = bw;
			this.silences = silences;
			this.force_keep_silences = force_keep_silences;
			this.latch = latch;
		}
		
		public void run() {
			try {
				Turn t;
				while ((t = distributor.next()) != null) {
					logger.info(t.fileName);
					
					// generate decoding tree
					FixedSequences forced = new FixedSequences(tok, th, silences.split("\\s++"),
							force_keep_silences);
					forced.addSequence(t.transcription);
					TreeNode root = forced.generateNetwork();
					
					// prepare decoder
					ViterbiBeamSearch dec = new ViterbiBeamSearch(root, 0., 1.);
					
					// read data and evaluate
					FrameInputStream fs = new FrameInputStream(new File(t.canonicalInputName()));
					List<double []> obs = new LinkedList<double []>();
					double [] buf = new double [fs.getFrameSize()];
					
					while (fs.read(buf))
						obs.add(buf.clone());
					
					int beam = 0;
					ViterbiBeamSearch.Hypothesis h0 = null;
					
					while (beam < bs && h0 == null) {
						// increase beam size
						beam += bi;
						if (beam > bi)
							logger.info("re-trying with beam size = " + beam);
						
						// init the decoder
						Iterator<double []> it = obs.iterator();
						dec.initialize(beam, bw, it.next());
	
						while (it.hasNext())
							dec.step(it.next());
						
						// remove active non-final states
						dec.pruneActiveHypotheses();
						h0 = forced.findBestForcedAlignment(dec.getBestHypotheses(0));
					}
					
					// generate the MetaAlignment
					MetaAlignment ma;
					if (h0 == null) {
						// fall back to real viterbi
						logger.info("Aligner.BWorker.run(): " + t.fileName + " no best hypothesis, falling back to regular Viterbi!");
						fs = new FrameInputStream(new File(t.canonicalInputName()));
						ma = new MetaAlignment(fs, tok.getSentenceTokenization(t.transcription), th, true);
						logger.info("alg-score " + t.fileName + " " + obs.size() + " " + ma.score + " " + (ma.score / obs.size()));
					} else {
						ma = h0.toMetaAlignment(th);
						logger.info("alg-score " + t.fileName + " " + obs.size() + " " + h0.as + " " +  h0.vs + " " + (h0.as / obs.size()) + " " + (h0.vs / obs.size()));
					}
					
					BufferedWriter bw = new BufferedWriter(new FileWriter(t.canonicalOutputName()));
					ma.write(bw);
					
					bw.close();
					fs.close();
					
					jobs++;
				}
				
				logger.info("Aligner.BWorker#" + Thread.currentThread().getId() + ".run(): processed " + jobs + " alignments");
			} catch (OutOfVocabularyException e) {
				logger.fatal("Aligner.BWorker#" + Thread.currentThread().getId() + ".run(): OutOfVocabularyException " + e.toString());
			} catch (IOException e) {
				logger.fatal("Aligner.BWorker#" + Thread.currentThread().getId() + ".run(): IOException " + e.toString());
			} catch (AlignmentException e) {
				logger.fatal("Aligner.BWorker#" + Thread.currentThread().getId() + ".run(): AlignmentException " + e.toString());
			} finally {
				// notify the main thread
				latch.countDown();
			}
		}
	}
	
	public static final String SYNOPSIS = 
		"sikoried, 8/2/2010\n" +
		"Compute forced or linear alignments for the given turn list.\n\n" +
		"usage: app.Aligner config codebook [options]\n" +
		"-l <turn-list> <in-dir> <out-dir>\n" +
		"  Load turns and use the given input/output directories. May be used multiple\n" +
		"  times.\n" +
		"--linear\n" +
		"  Compute linear alignments instead of forced Viterbi (useful for initialization).\n" +
		"-p <num-threads>\n" +
		"  Use <num-threads> for parallel execution. Use 0 to automatically determine the\n" +
		"  possible number of threads (default).\n" +
		"-b \"optional silence symbols\"\n" +
		"  Do a beam alignment instead of a true Viterbi alignment with given optional\n" +
		"  silence symbols (usually \"sil nv\"). This option is incompatible with --linear!\n" +
		"--force-keep-silences\n" +
		"  Tune beam alignment mode (option -b): do not discard silence symbols from\n" +
		"  transcription. Warning: currently, consecutive silences may occur.\n" +
		"--beam-size <num>\n" +
		"  Set the maximum beam size for decoding (default: 1000)\n" +
		"--beam-incr <num>\n" +
		"  Increase the beam size by <num> for each retry (default: 100)\n" + 
		"--silent\n" +
		"  Do not produce debug output.\n";
		
	
	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		// silent?
		for (String arg : args)
			if (arg.equals("--silent"))
				Configurator.setLevel("com.github.sikoried.jstk", Level.FATAL);
		
		// default variables
		boolean forcedInsteadOfLinear = true;
		int p = Runtime.getRuntime().availableProcessors();
		
		boolean beam = false;
		int beam_size = 1000;
		int beam_incr = 100;
		double beam_width = Double.MAX_VALUE;
		String sils = null;
		boolean force_keep_silences = false;
		
		// parse args
		int i = 0;
		String fConfig = args[i++];
		String fCodebook = args[i++];
		
		LinkedList<Turn> turns = new LinkedList<Turn>();
		for (; i < args.length; ++i) {
			if (args[i].equals("--linear"))
				forcedInsteadOfLinear = false;
			else if (args[i].equals("-p")) {
				int pc = Integer.parseInt(args[++i]);
				if (pc < 0)
					throw new Exception("Aligner.main(): invalid number of threads!");
				else if (pc == 0)
					p = Runtime.getRuntime().availableProcessors();
				else if (pc <= p)
					p = pc;
				else {
					p = pc;
					logger.info("Aligner.main(): warning -- using more threads than CPUs!");
				}
			} else if (args[i].equals("-l")) {
				// parse turn list
				List<Turn> part = Turn.readTurnList(args[i+1], args[i+2], args[i+3]);
				logger.info("Aligner.main(): read " + part.size() + " turns from " + args[i+1]);
				turns.addAll(part);
				i += 3;
			} else if (args[i].equals("--silent"))
				Configurator.setLevel("com.github.sikoried.jstk", Level.FATAL);
			else if (args[i].equals("--force-keep-silences"))
				force_keep_silences = true;
			else if (args[i].equals("-b")) {
				beam = true;
				sils = args[++i];
			}
			else if (args[i].equals("--beam-size"))
				beam_size = Integer.parseInt(args[++i]);
			else if (args[i].equals("--beam-incr"))
				beam_incr = Integer.parseInt(args[++i]);
			else
				throw new Exception("Aligner.main(): invalid argument \"" + args[i] + "\"");
		}
		
		// check :)
		if (turns.size() < 1) {
			logger.info("Aligner.main(): nothing to do -- exitting.");
			System.exit(0);
		}
		
		if (!forcedInsteadOfLinear && beam) {
			System.err.println(SYNOPSIS);
			System.exit(0);
		}
		
		
		// variables for thread synchronization
		Distributor dist = new Distributor(turns);
		CountDownLatch latch = new CountDownLatch(p);
		
		Runnable [] threads = new Runnable [p];
		for (int j = 0; j < p; ++j)  {
			logger.info("Aligner.main(): preparing thread #" + j);
			Configuration conf = new Configuration(new File(fConfig));
			conf.loadCodebook(new File(fCodebook));
			
			if (beam)
				threads[j] = new BWorker(conf.tok, conf.th, dist, beam_size, beam_incr, beam_width, sils, force_keep_silences, latch);
			else
				threads[j] = new Worker(conf.tok, conf.th, dist, forcedInsteadOfLinear, latch);
		}
		
		// start the execution
		logger.info("Aligner.main(): begin alignment using " + p + " threads");
		ExecutorService e = Executors.newFixedThreadPool(p);		
		
		for (int j = 0; j < p; ++j)
			e.execute(threads[j]);
		
		// wait for all jobs to be done
		latch.await();
		
		// make sure the thread pool is done
		e.shutdownNow();
		
		logger.info("Aligner.main(): finished.");
	}
}
