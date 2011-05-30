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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
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

import de.fau.cs.jstk.arch.Codebook;
import de.fau.cs.jstk.arch.Configuration;
import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.stat.hmm.Alignment;
import de.fau.cs.jstk.stat.hmm.MetaAlignment;
import de.fau.cs.jstk.stat.hmm.MetaAlignment.Turn;

public class Trainer {
	private static Logger logger = Logger.getLogger(Trainer.class);
	
	/**
	 * Type of alignment for each job. In case of MANUAL, Turn.outDir is checked
	 * for the manual alignment.
	 * 
	 * @author sikoried
	 */
	enum AlignmentType {
		MANUAL,
		MANUAL_LINEAR,
		FORCED,
		LINEAR
	}
	
	/**
	 * Type of training for each job; see hmm.HMM class.
	 * 
	 * @author sikoried
	 */
	enum TrainingType {
		VITERBI,
		BAUM_WELCH
	}
	
	private static final class Job {
		Turn turn;
		AlignmentType align;
		TrainingType train;
		
		Job(Turn turn, AlignmentType align, TrainingType train) {
			this.turn = turn;
			this.align = align;
			this.train = train;
		}
	}
	
	private static final class Distributor {
		List<Job> turns = null;
		
		/** Turn iterator, handled by next() */
		Iterator<Job> it = null;
		
		/**
		 * Generate a new Job Distributor, managing the given list of jobs.
		 * @param jobs
		 */
		Distributor(List<Job> turns) {
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
		synchronized Job next() {
			if (it.hasNext())
				return it.next();
			else 
				return null;
		}
	}
	
	/**
	 * The Worker class receives Jobs from the referenced Distributor. To be thread
	 * safe, each worker needs an exclusive TokenHierarchy
	 * 
	 * @author sikoried
	 */
	private static final class Worker implements Runnable {
		Distributor dist;
		Configuration conf;
		CountDownLatch latch;
		
		long jobs = 0;
		
		/**
		 * Generate a new Worker thread. Make sure that the TokenHierarchy is
		 * used by this thread only!
		 * @param tok
		 * @param th TokenHierarchy exclusively for this thread and prepped for training!
		 * @param d
		 * @param latch
		 */
		Worker(Configuration config, Distributor d, CountDownLatch latch) {
			this.conf = config;
			this.dist = d;
			this.latch = latch;
		}
		
		public void run() {
			try {
				// work off all jobs
				Job job;
				while ((job = dist.next()) != null) {
					FrameInputStream fs = new FrameInputStream(new File(job.turn.canonicalInputName()));
					
					// generate requested alignment
					MetaAlignment ma = null;
					if (job.align == AlignmentType.MANUAL || job.align == AlignmentType.MANUAL_LINEAR) {
						BufferedReader br = new BufferedReader(new FileReader(job.turn.canonicalOutputName()));
						ma = new MetaAlignment(fs, br, conf.th, job.align == AlignmentType.MANUAL);
						br.close();
					}
					else if (job.align == AlignmentType.FORCED)
						ma = new MetaAlignment(fs, conf.tok.getSentenceTokenization(job.turn.transcription), conf.th, true);
					else if (job.align == AlignmentType.LINEAR)
						ma = new MetaAlignment(fs, conf.tok.getSentenceTokenization(job.turn.transcription), conf.th, false);
					else
						throw new Exception("Trainer.Worker#" + Thread.currentThread().getId() + ".run(): invalid alignment strategy!");

					// save alignment?
					if ((job.align == AlignmentType.LINEAR || job.align == AlignmentType.FORCED) && job.turn.outDir != null) {
						BufferedWriter bw = new BufferedWriter(new FileWriter(job.turn.canonicalOutputName()));
						ma.write(bw);
						bw.close();
					}
					
					// explode the alignment (similar to ISADORA APIS)
					ma.explode();
					
					// do the actual training
					if (job.train == TrainingType.VITERBI) {
						for (Alignment alg : ma.alignments)
							alg.model.incrementVT(alg);
					} else if (job.train == TrainingType.BAUM_WELCH) {
						for (Alignment alg : ma.alignments)
							alg.model.incrementBW(alg.observation);
					} else
						throw new Exception("Trainer.Worker#" + Thread.currentThread().getId() + ".run(): invalid training strategy!");
					
					jobs++;
				}
				
				logger.info("Trainer.Worker#" + Thread.currentThread().getId() + ".run(): finished; computed " + jobs + " jobs"); 
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Trainer.Worker#" + Thread.currentThread().getId() + ".run(): " + e);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Trainer.Worker#" + Thread.currentThread().getId() + ".run(): " + e);
			} finally {
				// notify main thread
				latch.countDown();
			}
		}
	}
	
	public static final String SYNOPSIS = 
		"sikoried, 8/2/2010\n" +
		"Train the models of a TokenHierarchy using the given transcriptions or\n" +
		"alignments. The required alignments can be read or computed on the fly.\n\n" +
		"usage: app.Trainer config codebook codebook-out turn-list feat-dir [options]\n" +
		"-a type\n" +
		"  Use the given alignment strategy. Currently supported:\n" +
		"  manual <directory>        : assume manual alignments in the given directory.\n" +
		"  manual_linear <directory> : same as manual, but use linear alignment in absence of state alignment\n" +
		"  forced [directory]        : compute forced Viterbi alignment; specify a directory to save\n" +
		"                              the alignment result (default strategy).\n" +
		"  linear [directory]        : estimate a linear alignment dependent on the number\n" +
		"                              of states; specify a directory to save the alignment result.\n" +
		"-t type\n" +
		"  Use the given training strategy. Currently supported:\n" +
		"  vt : Viterbi training aka EM* -- fast and efficient; great with linear alignment for\n" +
		"       initialization (default).\n" +
		"  bw : Baum-Welch training based on EM. Computationally more expensive but greater\n" +
		"       accuracy.\n" +
		"-p num\n" +
		"  Number of threads to use for the training. If set to 0, the number of threads\n" +
		"  is set to the number of CPU (default).\n" +
		"--silent\n" +
		"  Mute the DebugOutput.\n";
	
	public static void main(String[] args) throws Exception {
		if (args.length < 5) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		BasicConfigurator.configure();
		
		// silent?
		for (String arg : args)
			if (arg.equals("--silent"))
				logger.setLevel(Level.FATAL);
		
		// default variables
		int p = Runtime.getRuntime().availableProcessors();
		AlignmentType strat_alignment = AlignmentType.FORCED;
		TrainingType strat_training = TrainingType.VITERBI;
		String alignDir = null;
		
		// parse args
		int i = 0;
		String fConfig = args[i++];
		String fCodebook = args[i++];
		String fCodebookOut = args[i++];
		String fTurns = args[i++];
		String inDir = args[i++];
		
		for (; i < args.length; ++i) {
			if (args[i].equals("--silent"))
				logger.setLevel(Level.FATAL);
			else if (args[i].equals("-t")) {
				String s = args[++i];
				
				if (s.equals("bw"))
					strat_training = TrainingType.BAUM_WELCH;
				else if (s.equals("vt"))
					strat_training = TrainingType.VITERBI;
				else
					throw new Exception("Trainer.main(): unknown training strategy");
			}
			else if (args[i].equals("-a")) {
				String s = args[++i];
				if (s.equals("manual")) {
					strat_alignment = AlignmentType.MANUAL;
					alignDir = args[++i];
				} else if (s.equals("manual_linear")) {
					strat_alignment = AlignmentType.MANUAL_LINEAR;
					alignDir = args[++i];
				}
				else if (s.equals("forced"))
					strat_alignment = AlignmentType.FORCED;
				else if (s.equals("linear"))
					strat_alignment = AlignmentType.LINEAR;
				else
					throw new Exception("Trainer.main(): unknown alignment strategy");
				
				// shall we save the alignments?
				if (strat_alignment == AlignmentType.FORCED || strat_alignment == AlignmentType.LINEAR)
					if (i+1 < args.length && !args[i+1].startsWith("-"))
						alignDir = args[++i];
			}
			else if (args[i].equals("-p")) {
				int nc = Integer.parseInt(args[++i]);
				if (nc < 0)
					throw new Exception("Trainer.main(): invalid number of threads");
				else if (nc == 0)
					p = Runtime.getRuntime().availableProcessors();
				else if (nc <= p)
					p = nc;
				else {
					logger.info("Trainer.main(): warning -- using more threads thann CPUs!");
					p = nc;
				}
			} else
				logger.info("Trainer.main(): warning -- ignoring unknown argument \"" + args[i] + "\"");
		}
		
		// read in turn list
		List<Turn> turnList = Turn.readTurnList(fTurns, inDir, alignDir);
		logger.info("Trainer.main(): read " + turnList.size() + " turns");
		
		// build up the job distributor
		List<Job> jobList = new LinkedList<Job>();
		for (Turn t : turnList) 
			jobList.add(new Job(t, strat_alignment, strat_training));
		
		// thread synchronization
		CountDownLatch latch = new CountDownLatch(p);
		Distributor dist = new Distributor(jobList);
		
		// generate the workers
		Worker [] threads = new Worker [p];
		for (int j = 0; j < p; ++j) {
			logger.info("Trainer.main(): preparing thread #" + j);
			Configuration conf = new Configuration(new File(fConfig));
			conf.loadCodebook(new File(fCodebook));
			conf.cb.init();
			
			threads[j] = new Worker(conf, dist, latch);
		}
		
		// start the execution
		logger.info("Trainer.main(): begin training using " + p + " threads");
		ExecutorService e = Executors.newFixedThreadPool(p);		
		
		for (int j = 0; j < p; ++j)
			e.execute(threads[j]);
		
		// wait for all jobs to be done
		latch.await();
		
		// make sure the thread pool is done
		e.shutdownNow();
		
		// combine the accumulators and re-estimate
		logger.info("Trainer.main(): re-estimating...");
		Codebook estimate = threads[0].conf.cb;
		for (int j = 1; j < p; ++j)
			estimate.consume(threads[j].conf.cb);
		estimate.reestimate();
		
		// finally write out the new model
		logger.info("Trainer.main(): writing out " + fCodebookOut);
		estimate.write(new File(fCodebookOut));
	}
}
