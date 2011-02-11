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
import java.util.Collections;
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

import de.fau.cs.jstk.arch.Configuration;
import de.fau.cs.jstk.arch.Tokenization;
import de.fau.cs.jstk.exceptions.AlignmentException;
import de.fau.cs.jstk.exceptions.OutOfVocabularyException;
import de.fau.cs.jstk.io.FrameReader;
import de.fau.cs.jstk.stat.hmm.MetaAlignment;
import de.fau.cs.jstk.util.Pair;


public class IWRecognizer {
	private static Logger logger = Logger.getLogger(IWRecognizer.class);
	
	static class Distributor {
		List<Pair<String, String>> turns = null;
		String dir = null;
		
		/** Turn iterator, handled by next() */
		Iterator<Pair<String, String>> it = null;
		
		/**
		 * Generate a new Job Distributor, managing the given list of jobs.
		 * @param jobs
		 */
		Distributor(List<Pair<String, String>> turns, String dir) {
			this.turns = turns;
			this.dir = dir;
			it = turns.iterator();
		}
		
		/**
		 * Get the next job in line. This method is synchronized an is used
		 * by the executing Worker threads.
		 * @return next job in line
		 */
		synchronized Pair<String, String> next() {
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
		Configuration conf;
		Distributor distributor;
		CountDownLatch latch;
		String sil;
		List<String> cand = new LinkedList<String>();
		int n;
				
		long jobs = 0;
		
		/**
		 * Initialize a new Worker with the given alphabet, tokenizer, hierarchy
		 * and turn distributor.
		 * @param a
		 * @param tok
		 * @param th TokenHierarchy exclusively for this thread
		 * @param d
		 * @param latch count-down latch for thread synchronization
		 */
		Worker(Configuration config, Distributor d, CountDownLatch latch, String sil, int n) {
			this.conf = config;
			if (!conf.hasTokenTree())
				conf.buildTokenTree();
			
			this.distributor = d;
			this.latch = latch;
			this.sil = sil;
			this.n = n;
			
			for (Tokenization t : conf.tok.tokenizations)
				if (!t.word.equals(sil))
					cand.add(t.word);
		}
		
		public void run() {
			try {
				Pair<String, String> f;
				while ((f = distributor.next()) != null) {
					// generate candidates
					List<Pair<Double, String>> hyp = new LinkedList<Pair<Double, String>>();
					for (String w : cand) {
						FrameReader fs = new FrameReader(new File(distributor.dir + System.getProperty("file.separator") + f.a));
						MetaAlignment ma = new MetaAlignment(fs, conf.tok.getSentenceTokenization(sil + " " + w + " " + sil), conf.tt, true);
						hyp.add(new Pair<Double, String>(ma.score, w));
						fs.close();
					}
					
					// sort by score
					Collections.sort(hyp, new Comparator<Pair<Double, String>>() {
						public int compare(Pair<Double, String> o1,
								Pair<Double, String> o2) {
							return (int) Math.signum(o2.a - o1.a);
						}
					});
					
					// output hypotheses
					StringBuffer sb = new StringBuffer();
					sb.append(hyp.remove(0).b);
					for (int j = 1; j < n; ++j)
						sb.append(" " + hyp.remove(0).b);
					f.b = sb.toString();
					
					jobs++;
				}
				
				logger.info("IWRecognizer.Worker#" + Thread.currentThread().getId() + ".run(): processed " + jobs + " alignments");
			} catch (OutOfVocabularyException e) {
				logger.fatal("IWRecognizer.Worker#" + Thread.currentThread().getId() + ".run(): OutOfVocabularyException " + e.toString());
			} catch (IOException e) {
				logger.fatal("IWRecognizer.Worker#" + Thread.currentThread().getId() + ".run(): " + e.toString());
			} catch (AlignmentException e) {
				logger.fatal("IWRecognizer.Worker#" + Thread.currentThread().getId() + ".run(): AlignmentException " + e.toString());
			} finally {
				// notify the main thread
				latch.countDown();
			}
		}
	}
	
	public static final String SYNOPSIS =
		"sikoried, 11/10/2010\n" +
		"Isolated word recognition. For each word, a \"sil word sil\" sequence is\n" +
		"aligned, and the (n-best) word(s) with the highest actual word alignment score\n" +
		"is/are given.\n\n" +
		"usage: app.IWRecognizer config codebook sil-symbol [options]\n" +
		"-l <turn-list> <in-dir> <out-file>\n" +
		"  Load turns and use the given input directory and write output to out-file.\n" +
		"-p <num-threads>\n" +
		"  Use <num-threads> for parallel execution. Use 0 to automatically determine the\n" +
		"  possible number of threads (default).\n" +
		"-n <num-hyp>\n" +
		"  Produce num-hyp hypotheses as n-best list. (default: 1)\n" +
		"--silent\n" +
		"  Do not produce debug output.\n";
	
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		
		if (args.length < 5) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		// silent?
		for (String arg : args)
			if (arg.equals("--silent"))
				Logger.getLogger("de.fau.cs.jstk").setLevel(Level.FATAL);
		
		// default variables
		int n = 1;
		int p = Runtime.getRuntime().availableProcessors();
		
		// parse args
		int i = 0;
		String fConfig = args[i++];
		String fCodebook = args[i++];
		String sil = args[i++];
		String indir = "";
		String outf = null;
		
		LinkedList<Pair<String, String>> turns = new LinkedList<Pair<String, String>>();
		for (; i < args.length; ++i) {
			if (args[i].equals("-p")) {
				int pc = Integer.parseInt(args[++i]);
				if (pc < 0)
					throw new Exception("IWRecognizer.main(): invalid number of threads!");
				else if (pc == 0)
					p = Runtime.getRuntime().availableProcessors();
				else if (pc <= p)
					p = pc;
				else {
					p = pc;
					logger.info("IWRecognizer.main(): warning -- using more threads than CPUs!");
				}
			} else if (args[i].equals("-l")) {
				// parse turn list
				String lfile = args[++i];
				indir = args[++i];
				outf = args[++i];
				BufferedReader br = new BufferedReader(new FileReader(lfile));
				String line;
				while ((line = br.readLine()) != null)
					turns.add(new Pair<String, String>(line, null));
				
			} else if (args[i].equals("-n"))
				n = Integer.parseInt(args[++i]);
			else if (args[i].equals("--silent"))
				Logger.getLogger("de.fau.cs.jstk").setLevel(Level.FATAL);		
			else
				throw new Exception("IWRecognizer.main(): invalid argument \"" + args[i] + "\"");
		}
		
		// check :)
		if (turns.size() < 1) {
			logger.info("IWRecognizer.main(): nothing to do -- exitting.");
			System.exit(0);
		}
		
		if (outf == null) {
			logger.info("IWRecognizer.main(): no output file specified");
			System.exit(0);
		}
		
		// variables for thread syncronization
		Distributor dist = new Distributor(turns, indir);
		CountDownLatch latch = new CountDownLatch(p);
		
		Worker [] threads = new Worker [p];
		for (int j = 0; j < p; ++j)  {
			logger.info("IWRecognizer.main(): preparing thread #" + j);
			Configuration conf = new Configuration(new File(fConfig));
			conf.loadCodebook(new File(fCodebook));
			threads[j] = new Worker(conf, dist, latch, sil, n);
		}
		
		// start the execution
		logger.info("IWRecognizer.main(): begin alignment using " + p + " threads");
		ExecutorService e = Executors.newFixedThreadPool(p);		
		
		for (int j = 0; j < p; ++j)
			e.execute(threads[j]);
		
		// wait for all jobs to be done
		latch.await();
		
		// make sure the thread pool is done
		e.shutdownNow();
		
		// write out transcriptions
		BufferedWriter bw = new BufferedWriter(new FileWriter(outf));
		for (Pair<String, String> pp : turns)
			bw.append(pp.a + "\t" + pp.b + "\n");
		bw.close();
		
		logger.info("IWRecognizer.main(): finished.");
	}

}
