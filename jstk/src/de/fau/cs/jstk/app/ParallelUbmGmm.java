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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.io.FrameSource;
import de.fau.cs.jstk.stat.Density;
import de.fau.cs.jstk.stat.Mixture;
import de.fau.cs.jstk.trans.NAP;
import de.fau.cs.jstk.util.Pair;

/**
 * The ParallelUbmGmm class reads a trial file and distributes the trials to the
 * available worker threads
 * 
 * @author sikoried
 */
public class ParallelUbmGmm {
	private static Logger logger = Logger.getLogger(ParallelUbmGmm.class);
	
	/**
	 * The Worker class uses Job instances to evaluate data on (cached) models.
	 * Jobs are distributed through a synchronized Distributor. 
	 * @author sikoried
	 */
	static class Worker implements Runnable {
		/** global (synchronized) job distributor */
		Distributor jobDistributor;
		
		/** universal background model to be evaluated by every instance */
		Mixture ubm;
		
		/** fast scoring? */
		int fastScoring;
		
		/** main thread synchronization */
		CountDownLatch latch;
		
		/** per-density nap transformations */
		NAP [] nap = null;
		
		/** reduction rank for NAP transformation */
		int rank;
		
		/** fast scoring buffers */
		int [] ndx = null;
		double [] scr = null;
		
		/** internal statistics */
		long processed_frames = 0;
		long processed_models = 0;
		long processed_files = 0;
		
		/**
		 * Generate a new Worker instance. Make sure the referenced Mixture is
		 * only accessible by this thread/instance as for the internal caching
		 * of scores.
		 * @param ubm needs to be a copy for this thread exclusively!
		 * @param jobDistributor
		 * @param fastScoring number of densities for fast scoring (typically 5)
		 * @param latch count down latch for synchronization with main thread
		 * @param nap component-wise NAP transformation to apply before computation
		 * @param rank rank of projection
		 */
		Worker(Mixture ubm, Distributor jobDistributor, int fastScoring, CountDownLatch latch, NAP [] nap, int rank) {
			this.ubm = ubm;
			this.jobDistributor = jobDistributor;
			this.fastScoring = fastScoring;
			this.latch = latch;
			this.nap = nap;
			this.rank = rank;
		
			if (fastScoring > 0) {
				ndx = new int [fastScoring];
				scr = new double [fastScoring];
			}
		}
		
		/**
		 * Simple ranker class to rank densities by descending scores
		 * @author sikoried
		 */
		class DensityRanker implements Comparator<Pair<Integer, Double>> {
			public int compare(Pair<Integer, Double> o1,
					Pair<Integer, Double> o2) {
				return o2.b.compareTo(o1.b);
			}
		}
		
		/**
		 * The actual scoring procedure. As long as the distributor has jobs, get
		 * the next job and evaluate (fast-score) the feature file. The method
		 * also caches the speaker models on demand.
		 */ 
		public void run() {
			Job current = null;
			try {
				Mixture speaker = new Mixture(ubm);
				while ((current = jobDistributor.next()) != null) {
					
					// Mixture speaker = new Mixture(new FileInputStream(current.getModelFile()));
					FrameSource spmean = new FrameInputStream(current.getModelFile());
					double [] sv = new double [spmean.getFrameSize()];
					spmean.read(sv);
					
					for (int i = 0; i < speaker.nd; ++i)
						System.arraycopy(sv, i*speaker.fd, speaker.components[i].mue, 0, speaker.fd);
					
					FrameSource source = new FrameInputStream(current.getFeatureFile());
					
					double [] buf = new double [source.getFrameSize()];
					
					// score accumulators
					double h1, h2;
					double [] hb = new double [buf.length];
					double score_ubm = 0.;
					double score_spk = 0.;
					
					// number of frames for later normalization
					long frames = 0;
					
					if (fastScoring == 0) {
						while (source.read(buf)) {
							// score all densities -- time consuming!
							if (nap != null) {
								// UBM
								h1 = 0.; h2 = 0.;
								for (int i = 0; i < ubm.nd; ++i) {
									System.arraycopy(buf, 0, hb, 0, buf.length);
									nap[i].project(hb, rank);
									h1 += ubm.components[i].evaluate(hb);
									h2 += speaker.components[i].evaluate(hb);
								}
								score_ubm += Math.log(h1);
								score_spk += Math.log(h2);
							} else {
								score_ubm += Math.log(ubm.evaluate(buf));
								score_spk += Math.log(speaker.evaluate(buf));
							}
							frames++;
						}
					} else {
						while (source.read(buf)) {
							// fast-scoring!
							Arrays.fill(scr, 0.);
							
							// step 1: evaluate UBM
							// ubm.evaluate(buf); obsolete, done in step 2
							
							// step 2: determine best C densities (insert sorted, ascending values)
							Density [] c = ubm.components;
							for (int i = 0; i < ubm.components.length; ++i) {
								if (nap != null) {
									System.arraycopy(buf, 0, hb, 0, buf.length);
									nap[i].project(hb, rank);
									c[i].evaluate(hb);
								} else
									c[i].evaluate(buf);
								
								double s = c[i].ascore;
								
								// do we need to consider this density score at all?
								if (s < scr[0])
									continue;
								
								// locate the insert position
								int p = 0;
								while (p < fastScoring - 1 && s > scr[p + 1])
									p++;
								
								// shift the old values and indices
								for (int j = 1; j <= p; ++j) { 
									ndx[j-1] = ndx[j]; 
									scr[j-1] = scr[j];
								}
								
								// insert
								ndx[p] = i;
								scr[p] = s;
							}
						
							// step 3: sum up those densities for actual UBM score
							double ubm_part = 0.;
							for (int j = 0; j < fastScoring; ++j)
								ubm_part += scr[j];
							score_ubm += Math.log(ubm_part);
							
							// step 4: now evaluate the best densities for each speaker model
							h1 = 0.;
							c = speaker.components;
							
							if (nap != null) {
								for (int j = 0; j < fastScoring; ++j) {
									System.arraycopy(buf, 0, hb, 0, buf.length);
									nap[ndx[j]].project(hb, rank);
									h1 += c[ndx[j]].evaluate(hb); 
								}									
							} else
								for (int j = 0; j < fastScoring; ++j)
									h1 += c[ndx[j]].evaluate(buf);
							
							score_spk += Math.log(h1);
														
							// increase number of frames for later normalization
							frames++;
						}
					}
					
					// finish score computation
					current.score = (score_spk - score_ubm) / frames;

					// private statistics
					processed_frames += frames;
					processed_files++;
					processed_models += 1;
				}
			} catch (IOException e) {
				logger.info(e.toString());
				logger.info("ParallelUbmGmm.Worker#" + Thread.currentThread().getId() + ".run(): Exception working on file " + current.featureFile);
			} finally {
				// notify the main thread
				latch.countDown();
			}
			
			// end of run()
			logger.info("ParallelUbmGmm.Worker#" + Thread.currentThread().getId() + ".run(): processed " + processed_frames + " frames in " + processed_files + " files; " + processed_models + " models");
		}
	}

	/**
	 * A Job consists of a feature and model file name and stores the score.
	 * @author sikoried
	 */
	static class Job {
		private static int idcnt = 0;
		
		String featureFile;
		String featureDir = null;
		
		String modelFile;
		String modelDir = null;
		
		double score;
		
		int id = idcnt++;
		
		Job(String featureFile, String modelFile, String featureDir, String modelDir) {
			this.featureFile = featureFile;
			this.modelFile = modelFile;
			this.featureDir = featureDir;
			this.modelDir = modelDir;
		}
		
		public File getModelFile() {
			return new File(modelDir == null ? modelFile : modelDir + System.getProperty("file.separator") + modelFile);
		}
		
		public File getFeatureFile() {
			return new File(featureDir == null ? featureFile : featureDir + System.getProperty("file.separator") + featureFile);
		}
				
		/**
		 * Produce a ASCII representation of the evaluated trial as 
		 * "speaker-model feature-file score". Scores are not normalized.
		 */
		public String toString() {
			return modelFile + " " + featureFile + " " + score;
		}
	}
	
	/**
	 * The distributor administers a given job list to privide
	 * Workers with jobs.
	 * @author sikoried
	 */
	static class Distributor {
		List<Job> jobs = null;
		
		/** Job iterator, handled by next() */
		Iterator<Job> it = null;
		
		/**
		 * Generate a new Job Distributor, managing the given list of jobs.
		 * @param jobs
		 */
		Distributor(List<Job> jobs) {
			this.jobs = jobs;
			rewind();
		}
		
		void rewind() {
			it = jobs.iterator();
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
	 * Read in a trial file, specify a model and feature file directory if 
	 * required, and generate the Job list.
	 * @param trialFile
	 * @param featureFileDir
	 * @param modelDir
	 */
	public static List<Job> readTrialFile(String trialFile, String featureFileDir, String modelDir) 
		throws IOException {
		logger.info("ParallelUbmGmm.readTrialFile(): reading " + trialFile);
		
		BufferedReader br = new BufferedReader(new FileReader(trialFile));
		List<Job> jl = new LinkedList<Job>();
		
		String line;
		while ((line = br.readLine()) != null) {
			// format is "<speaker-model> <feature-file>"
			String [] trial = line.split("\\s+");
		
			if (!(new File((modelDir != null ? modelDir + System.getProperty("file.separator") : "") + trial[0])).canRead())
				throw new IOException("Could not read speaker model " + trial[0]);
			if (!(new File((featureFileDir != null ? featureFileDir + System.getProperty("file.separator") : "") + trial[1])).canRead())
				throw new IOException("Could not read feature file" + trial[1]);
			
			jl.add(new Job(trial[1], trial[0], featureFileDir, modelDir));
		}
		br.close();
		
		logger.info("ParallelUbmGmm.readTrialFile(): read " + jl.size() + " trials");
		
		return jl;
	}
	
	public static final String SYNOPSIS =
		"sikoried, 12JUL2010\n" +
		"Score trials against a UBM. You may specify fast-scoring and multi-threading\n" +
		"to increase the runtime. Scores are not normalized (this should be done\n" +
		"externally).\n\n" +
		"usage: app.ParallelUbmGmm [options] <ubm> <trial-file> <scored-trial-file>[feature-dir]\n" +
		"  Use the given UBM and a trial file containing lines \"speaker-model feature-file\"\n" +
		"  with an optional feature file directory. Output is a written to scored-trial-file\n" +
		"  containing lines \"speaker-model feature-file unnormalized-score\".\n" +
		"Options:\n" +
		"-f <fast-scoring>\n" +
		"  Specify the number of densities to consider for fast-scoring (typically 5,\n" +
		"  default 0, i.e., no fast-scoring).\n" +
		"-p <num-threads>\n" +
		"  Use <num-threads> CPUs for faster processing. This will increase the memory\n" +
		"  overhead as each thread needs to have it's own model cache. Per default all\n" +
		"  available CPU are used.\n" +
		"-t mnap-file rank\n" +
		"  Specify a MNAP projection and reduction rank. Note that the models need to be\n" +
		"  MNAP transformed as well!\n" +
		"--model-dir <dir>\n" +
		"  Append <dir> before speaker model names in trial file\n" +
		"--silent\n" +
		"  Turn off DebugOutput for silent execution.\n";
	
	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		BasicConfigurator.configure();
		Logger.getLogger("de.fau.cd.jstk").setLevel(Level.INFO);
		
		int fastScoring = 0;
		int threads = Runtime.getRuntime().availableProcessors();
		int rank = 0;

		String napBase = null;
		
		String modelDir = null;
		// parsed arguments are (in order): ubm, trial-file, scored-trial-file, (optional) feature directory
		String [] parsedArgs = { null, null, null, null };
		int j = 0;
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-f"))
				fastScoring = Integer.parseInt(args[++i]);
			else if (args[i].equals("-p")) {
				threads = Integer.parseInt(args[++i]);
//				if (threads > Runtime.getRuntime().availableProcessors()) {
//					threads = Runtime.getRuntime().availableProcessors();
//					logger.info("ParallelUbmGmm.main(): more CPUs requested than available, scaling back to " + threads);
//				}
			} else if (args[i].equals("--silent"))
				Logger.getLogger("de.fau.cs.jstk").setLevel(Level.FATAL);
			else if (args[i].equals("--model-dir")) {
			    modelDir = args[++i];
			} else if (args[i].equals("-t")) {
				napBase = args[++i];
				rank = Integer.parseInt(args[++i]);
			}
			else
				parsedArgs[j++] = args[i];
		}
		
		logger.info("ParallelUbmGmm.main(): execution summary");
		logger.info("ParallelUbmGmm.main(): designated UBM: " + parsedArgs[0]); 
		logger.info("ParallelUbmGmm.main(): trial-file    : " + parsedArgs[1]); 
		logger.info("ParallelUbmGmm.main(): score-output  : " + parsedArgs[2]);
		if (parsedArgs[3] != null)
			logger.info("ParallelUbmGmm.main(): feature-dir   : " + parsedArgs[3]);
		if (modelDir != null)
			logger.info("ParallelUbmGmm.main(): model-dir     : " + modelDir);
		logger.info("ParallelUbmGmm.main(): fast-scoring  : " + fastScoring); 
		logger.info("ParallelUbmGmm.main(): num-threads   : " + threads);
				
		// read in trial file
		List<Job> jobs = readTrialFile(parsedArgs[1], parsedArgs[3], modelDir);
		Distributor d = new Distributor(jobs);
		
		// start jobs
		ExecutorService e = Executors.newFixedThreadPool(threads);
		CountDownLatch latch = new CountDownLatch(threads);
		
		long tstart = System.currentTimeMillis();
		logger.info("ParallelUbmGmm.main(): start time is " + tstart);
		
		for (int i = 0; i < threads; ++i) {
			NAP [] nap = null;
			Mixture ubm = Mixture.readFromFile(new File(parsedArgs[0]));
			if (napBase != null) {
				MNAP mnap = new MNAP(new FileInputStream(napBase));
				nap = mnap.getTransformations();
			}
			e.execute(new Worker(ubm, d, fastScoring, latch, nap, rank));
		}

		// wait for all jobs to be done
		latch.await();
		
		// make sure the thread pool is done
		e.shutdownNow();
		
		// print out the scored trial file
		logger.info("ParallelUbmGmm.main(): writing scored trial file");
		BufferedWriter fw = new BufferedWriter(new FileWriter(parsedArgs[2]));
		Iterator<Job> it = d.jobs.iterator();
		while (it.hasNext()) 
			fw.write(it.next().toString() + "\n");
		fw.close();
		
		long tend = System.currentTimeMillis();
		logger.info("ParallelUbmGmm.main(): end time is " + tend + " (time elapsed: " + (tend - tstart) / 1000. + "s)");
		logger.info("ParallelUbmGmm.main(): finished.");
	}
}
