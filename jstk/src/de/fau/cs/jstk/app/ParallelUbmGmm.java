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
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.fau.cs.jstk.framed.SimulatedFrameSource;
import de.fau.cs.jstk.io.FrameInputStream;
import de.fau.cs.jstk.io.FrameSource;
import de.fau.cs.jstk.stat.Density;
import de.fau.cs.jstk.stat.Mixture;
import de.fau.cs.jstk.trans.NAP;
import de.fau.cs.jstk.util.Pair;


public class ParallelUbmGmm {
	private static Logger logger = Logger.getLogger(ParallelUbmGmm.class);
	
	/** use UFV instead of Frame format */
	public static int ufv = 0;
	
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
		
		/** lookup table for string->speakermodel */
		HashMap<String, Mixture> models;
		int fastScoring;
		
		/** main thread synchronization */
		CountDownLatch latch;
		
		/** per-density nap transformations */
		NAP [] nap = null;
		
		int rank;
		
		/** fast scoring buffers */
		int [] ndx = null;
		double [] scr = null;
		
		/** internal statistics */
		long processed_frames = 0;
		long processed_models = 0;
		long processed_files = 0;
		
		/**
		 * Generate a new Worker instance. Make sure the referenced models are
		 * only accessible by this thread/instance as for the internal caching
		 * of scores.
		 * @param ubm needs to be a copy for this thread exclusively!
		 * @param jobDistributor
		 * @param fastScoring number of densities for fast scoring (typically 5)
		 */
		Worker(Mixture ubm, Distributor jobDistributor, int fastScoring, CountDownLatch latch, NAP [] nap, int rank) {
			this.ubm = ubm;
			this.jobDistributor = jobDistributor;
			this.fastScoring = fastScoring;
			this.latch = latch;
			this.nap = nap;
			this.rank = rank;
			models = new HashMap<String, Mixture>();
			
			
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
				while ((current = jobDistributor.next()) != null) {
					logger.info("ParallelUbmGmm.Worker#" + Thread.currentThread().getId() + ".run(): starting job " + current.summary());
					
					// cache unloaded models
					for (String spk : current.models) {
						if (!models.containsKey(spk)) {
							models.put(spk, Mixture.readFromFile(new File((current.modelDir != null ? current.modelDir + System.getProperty("file.separator") : "") + spk)));
							logger.info("ParallelUbmGmm.Worker#" + Thread.currentThread().getId() + ".run(): caching speaker model " + spk);
						}
					}
					
					// open the feature file, prepare buffers
					FrameSource source = new SimulatedFrameSource(current.getFrameReader());
					
					double [] buf = new double [source.getFrameSize()];
					
					// score accumulators
					double h1;
					double [] h2 = new double [buf.length];
					double score_ubm = 0.;
					double [] score_spk = new double [current.models.size()];
					
					// cache the densities for faster access
					Mixture [] speakers = new Mixture [current.models.size()];
					int m = 0;
					for (String key : current.models)
						speakers[m++] = models.get(key);
					
					// number of frames for later normalization
					long frames = 0;
					
					if (fastScoring == 0) {
						while (source.read(buf)) {
							// score all densities -- time consuming!

							// background model
							if (nap != null) {
								h1 = 0.;
								for (int i = 0; i < ubm.nd; ++i) {
									System.arraycopy(buf, 0, h2, 0, buf.length);
									nap[i].project(h2, rank);
									h1 += ubm.components[i].evaluate(h2);
								}
								score_ubm += Math.log(h1);
							} else
								score_ubm += Math.log(ubm.evaluate(buf));						
							
							// speaker models
							if (nap != null) {
								for (int i = 0; i < score_spk.length; ++i) {
									h1 = 0.;
									for (int j = 0; j < speakers[i].nd; ++j) {
										System.arraycopy(buf, 0, h2, 0, buf.length);
										nap[j].project(h2, rank);
										h1 += speakers[i].components[j].evaluate(h2);
									}
									score_spk[i] += Math.log(h1);
								}
							} else
								for (int i = 0; i < score_spk.length; ++i)
									score_spk[i] += Math.log(speakers[i].evaluate(buf));
							
							// increase number of frames for later normalization
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
									System.arraycopy(buf, 0, h2, 0, buf.length);
									nap[i].project(h2, rank);
									c[i].evaluate(h2);
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
							for (int i = 0; i < score_spk.length; ++i) {
								double spk_part = 0.;
								c = speakers[i].components;
								
								if (nap != null) {
									for (int j = 0; j < fastScoring; ++j) {
										System.arraycopy(buf, 0, h2, 0, buf.length);
										nap[ndx[j]].project(h2, rank);
										spk_part += c[ndx[j]].evaluate(h2); 
									}									
								} else
									for (int j = 0; j < fastScoring; ++j)
										spk_part += c[ndx[j]].evaluate(buf);
								score_spk[i] += Math.log(spk_part);
							}
							
							// increase number of frames for later normalization
							frames++;
						}
					}
					
					// finish score computation
					for (int i = 0; i < score_spk.length; ++i)
						current.scores.add((score_spk[i] - score_ubm) / frames);
					
					logger.info("ParallelUbmGmm.Worker#" + Thread.currentThread().getId() + ".run(): processed " + frames + " frames");
					
					// private statistics
					processed_frames += frames;
					processed_files++;
					processed_models += score_spk.length;
				}
			} catch (IOException e) {
				logger.info(e.toString());
				logger.info("ParallelUbmGmm.Worker#" + Thread.currentThread().getId() + ".run(): Exception working on file " + current.fileName);
			} catch (ClassNotFoundException e) {
				logger.info(e.toString());
				logger.info("ParallelUbmGmm.Worker#" + Thread.currentThread().getId() + ".run(): Exception loading models");// for " + current.fileName);
			} finally {
				// notify the main thread
				latch.countDown();
			}
			
			// end of run()
			logger.info("ParallelUbmGmm.Worker#" + Thread.currentThread().getId() + ".run(): processed " + processed_frames + " frames in " + processed_files + " files; " + processed_models + " models");
		}
	}

	/**
	 * A Job consists of a feature file and a list of speaker models
	 * to evaluate. The worker provides the actual models and the UBM
	 * and will place the scores in the job and mark it as processed.
	 * @author sikoried
	 */
	static class Job {
		private static int idcnt = 0;
		String fileName;
		String featureFileDir = null;
		String modelDir = null;
		List<String> models;
		List<Double> scores = new LinkedList<Double>();
		
		int id = idcnt++;
		
		Job(String fileName, List<String> modelFiles, String featureFileDir, String modelDir) {
			this.fileName = fileName;
			this.models = modelFiles;
			this.featureFileDir = featureFileDir;
			this.modelDir = modelDir;
		}
		
		public FrameInputStream getFrameReader() throws IOException {
			return new FrameInputStream(new File((featureFileDir != null ? featureFileDir + System.getProperty("file.separator") : "") + fileName));
		}
		
		public String summary() {
			StringBuffer sb = new StringBuffer();
			sb.append(id + ":" + fileName + ":" + models.size() + ":");
			for (String m : models)
				sb.append(m + ",");
			return sb.toString();
		}
		
		/**
		 * Produce a ASCII representation of the evaluated
		 * trials in lines of "speaker file score". Scores 
		 * are not normalized.
		 */
		public String toString() {
			StringBuffer sb = new StringBuffer();
			
			Iterator<String> i1 = models.iterator();
			Iterator<Double> i2 = scores.iterator();
			
			for (int i = 0; i < scores.size(); ++i) {
				sb.append(i1.next());
				sb.append(" " + fileName + " ");
				sb.append(i2.next());
				
				if (i < scores.size() - 1)
					sb.append("\n");
			}
			
			return sb.toString();
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
	 * Read in a trial file, specify a feature file directory if required, and 
	 * generate the Job list.
	 * @param trialFile
	 * @param featureFileDir
	 * @param modelDir
	 */
	public static List<Job> readTrialFile(String trialFile, String featureFileDir, String modelDir) 
		throws IOException {
		logger.info("ParallelUbmGmm.readTrialFile(): reading " + trialFile);
		
		BufferedReader br = new BufferedReader(new FileReader(trialFile));
		HashMap<String, List<String>> fileToModels = new HashMap<String, List<String>>();
		String line;
		int numTrials = 0;
		while ((line = br.readLine()) != null) {
			// format is "<speaker-model> <feature-file>"
			String [] trial = line.split("\\s+");
		
			if (!(new File((modelDir != null ? modelDir + System.getProperty("file.separator") : "") + trial[0])).canRead())
				throw new IOException("Could not read speaker model " + trial[0]);
			if (!(new File((featureFileDir != null ? featureFileDir + System.getProperty("file.separator") : "") + trial[1])).canRead())
				throw new IOException("Could not read feature file" + trial[1]);
			
			if (!fileToModels.containsKey(trial[1]))
				fileToModels.put(trial[1], new LinkedList<String>());
			
			fileToModels.get(trial[1]).add(trial[0]);
			numTrials++;
		}
		br.close();
		
		logger.info("ParallelUbmGmm.readTrialFile(): read " + numTrials + " trials; organizing...");
		List<Job> jl = new LinkedList<Job>();
		
		for (Entry<String, List<String>> e : fileToModels.entrySet())
			jl.add(new Job(e.getKey(), e.getValue(), featureFileDir, modelDir));
		
		logger.info("ParallelUbmGmm.readTrialFile(): prepared " + jl.size() + " jobs for execution");
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
		"-t proj-base rank\n" +
		"  Specify a MNAP projection base and reduction rank. Expects \"proj-base\".DENS files\n" +
		"  where DENS starts at 1.\n" +
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
		if (ufv > 0)
			logger.info("ParallelUbmGmm.main(): using UFV(" + ufv + ")");
		
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
				nap = new NAP [ubm.nd];
				for (int k = 0; k < nap.length; ++k) {
					ObjectInputStream ois = new ObjectInputStream(new FileInputStream(napBase + "." + (k+1)));
					nap[k] = new NAP(ois);
					ois.close();
				}
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
