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
package de.fau.cs.jstk.app;

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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.fau.cs.jstk.arch.Configuration;
import de.fau.cs.jstk.arch.TokenTree;
import de.fau.cs.jstk.arch.TokenTree.TreeNode;
import de.fau.cs.jstk.arch.Tokenizer;
import de.fau.cs.jstk.decoder.ViterbiBeamSearch;
import de.fau.cs.jstk.decoder.ViterbiBeamSearch.Hypothesis;
import de.fau.cs.jstk.exceptions.AlignmentException;
import de.fau.cs.jstk.exceptions.OutOfVocabularyException;
import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.lm.FixedSequences;
import de.fau.cs.jstk.stat.hmm.MetaAlignment;
import de.fau.cs.jstk.stat.hmm.MetaAlignment.Turn;


/**
 * Use the Aligner to compute forced or linear alignments.
 * @author sikoried
 */
public class Aligner {
	private static Logger logger = Logger.getLogger(Aligner.class);

	static class Distributor {
		List<Turn> turns = null;
		
		/** Turn iterator, handled by next() */
		Iterator<Turn> it = null;
		
		/**
		 * Generate a new Job Distributor, managing the given list of jobs.
		 * @param jobs
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
		TokenTree tt;
		Distributor distributor;
		CountDownLatch latch;
		
		boolean forced;
		
		long jobs = 0;
		
		/**
		 * Initialize a new Worker with the given alphabet, tokenizer, hierarchy
		 * and turn distributor.
		 * @param a
		 * @param tok
		 * @param th TokenHierarchy exclusively for this thread
		 * @param d
		 * @param forcedInsteadOfLinear Use forced alignment instead of linear alignments
		 * @param latch count-down latch for thread synchronization
		 */
		Worker(Configuration conf, Distributor d, boolean forcedInsteadOfLinear, CountDownLatch latch) {
			this.tok = conf.tok;
			
			if (!conf.hasTokenTree())
				conf.buildTokenTree();
			
			this.tt = conf.tt;
			this.distributor = d;
			this.forced = forcedInsteadOfLinear;
			this.latch = latch;
		}
		
		public void run() {
			try {
				Turn t;
				while ((t = distributor.next()) != null) {
					FrameInputStream fs = new FrameInputStream(new File(t.canonicalInputName()));
					MetaAlignment ma = new MetaAlignment(fs, tok.getSentenceTokenization(t.transcription), tt, forced);
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
		TokenTree tt;
		Distributor distributor;
		CountDownLatch latch;
		
		int bs;
		double bw;
		String silences;
		
		long jobs = 0;
		
		/**
		 * Initialize a new Worker with the given alphabet, tokenizer, hierarchy
		 * and turn distributor.
		 * @param a
		 * @param tok
		 * @param th TokenHierarchy exclusively for this thread
		 * @param d
		 * @param bs size of the beam
		 * @param latch count-down latch for thread synchronization
		 */
		BWorker(Configuration conf, Distributor d, int bs, double bw, String silences, CountDownLatch latch) {
			this.tok = conf.tok;
			
			if (!conf.hasTokenTree())
				conf.buildTokenTree();
			
			this.tt = conf.tt;
			this.distributor = d;
			this.bs = bs;
			this.bw = bw;
			this.silences = silences;
			this.latch = latch;
		}
		
		public void run() {
			try {
				Turn t;
				while ((t = distributor.next()) != null) {
					
					// generate decoding tree
					FixedSequences forced = new FixedSequences(tt);
					forced.addSequence(t.transcription, silences);
					TreeNode root = forced.generateNetwork(null, null, 0.);
					
					// prepare decoder
					ViterbiBeamSearch dec = new ViterbiBeamSearch(root, 0., 1.);
					
					// read data and evaluate
					FrameInputStream fs = new FrameInputStream(new File(t.canonicalInputName()));
					List<double []> obs = new LinkedList<double []>();
					double [] buf = new double [fs.getFrameSize()];
					
					while (fs.read(buf))
						obs.add(buf.clone());
					
					// init the decoder
					Iterator<double []> it = obs.iterator();
					dec.initialize(bs, bw, it.next());

					while (it.hasNext())
						dec.step(it.next());
					
					// remove active non-final states
					dec.pruneActiveHypotheses();
					
					MetaAlignment ma;
					Hypothesis h0 = forced.findBestForcedAlignment(dec.getBestHypotheses(0));
					
					// generate the MetaAlignment
					if (h0 == null) {
						// fall back to real viterbi
						logger.info("Aligner.BWorker.run(): " + t.fileName + " no best hypothesis, falling back to regular Viterbi!");
						fs = new FrameInputStream(new File(t.canonicalInputName()));
						ma = new MetaAlignment(fs, tok.getSentenceTokenization(t.transcription), tt, true);
					} else
						ma = h0.toMetaAlignment(tt);
					 
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
		"--beam-size <num>\n" +
		"  Set the beam size to the given number of active hypotheses (default: 100)\n" +
		"--silent\n" +
		"  Do not produce debug output.\n";
	
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		
		if (args.length < 3) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		// silent?
		for (String arg : args)
			if (arg.equals("--silent"))
				Logger.getLogger("de.fau.cs.jstk").setLevel(Level.FATAL);
		
		// default variables
		boolean forcedInsteadOfLinear = true;
		int p = Runtime.getRuntime().availableProcessors();
		
		boolean beam = false;
		int bs = 10000;
		double bw = Double.MAX_VALUE;
		String sils = null;
		
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
				Logger.getLogger("de.fau.cs.jstk").setLevel(Level.FATAL);
			else if (args[i].equals("-b")) {
				beam = true;
				sils = args[++i];
			}
			else if (args[i].equals("--beam-size"))
				bs = Integer.parseInt(args[++i]);
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
		
		
		// variables for thread syncronization
		Distributor dist = new Distributor(turns);
		CountDownLatch latch = new CountDownLatch(p);
		
		Runnable [] threads = new Runnable [p];
		for (int j = 0; j < p; ++j)  {
			logger.info("Aligner.main(): preparing thread #" + j);
			Configuration conf = new Configuration(new File(fConfig));
			conf.loadCodebook(new File(fCodebook));
			
			if (beam)
				threads[j] = new BWorker(conf, dist, bs, bw, sils, latch);
			else
				threads[j] = new Worker(conf, dist, forcedInsteadOfLinear, latch);
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