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
package de.fau.cs.jstk.stat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import de.fau.cs.jstk.io.ChunkedDataSet;
import de.fau.cs.jstk.io.FrameReader;
import de.fau.cs.jstk.util.Arithmetics;


/**
 * A parallel implementation of the EM algorithm. Uses an initial mixture and
 * a ChunkedDataSet to update the parameters.
 * 
 * @author sikoried
 *
 */
public final class ParallelEM {
	private static Logger logger = Logger.getLogger(ParallelEM.class);
	
	/** number of threads (= CPUs) to use */
	private int numThreads = 0;
	
	/** data set to use; do not forget to rewind if required! */
	private ChunkedDataSet data = null;
	
	/** previous estimate */
	public Mixture previous = null;
	
	/** current estimate */
	public Mixture current = null;
	
	/** number of components */
	private int nd;
	
	/** feature dimension */
	private int fd;
	
	/** diagonal covariances? */
	private boolean dc;
	
	/** number of iterations performed by this instance */
	public int ni = 0;
	
	/**
	 * Generate a new Estimator for parallel EM iterations.
	 * 
	 * @param initial Initial mixture to start from (DATA IS MODIFIED)
	 * @param data data set to use
	 * @param numThreads number of threads (= CPUs)
	 * @throws IOException
	 */
	public ParallelEM(Mixture initial, ChunkedDataSet data, int numThreads) 
		throws IOException {
		this.data = data;
		this.numThreads = numThreads;
		this.current = initial;
		this.fd = initial.fd;
		this.nd = initial.nd;
		this.dc = initial.diagonal();
	}
	
	/**
	 * Set the data set to work on
	 */
	public void setChunkedDataSet(ChunkedDataSet data) {
		this.data = data;
	}
	
	/**
	 * Set the number of threads for the next iteration
	 */
	public void setNumberOfThreads(int num) {
		numThreads = num;
	}
	
	/**
	 * Perform a number of EM iterations
	 * @param iterations
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void iterate(int iterations) throws IOException, InterruptedException {
		while (iterations-- > 0)
			iterate();
	}
	
	/**
	 * Perform one EM iteration
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void iterate() throws IOException, InterruptedException {
		logger.info("ParallelEM.iterate(): BEGIN iteration " + (++ni));
		
		// each thread gets a partial estimate and a working copy of the current
		Mixture [] partialEstimates = new Mixture[numThreads];
		Mixture [] workingCopies = new Mixture[numThreads];
		
		// save the old mixture, put zeros in the current
		previous = current.clone();
		
		ExecutorService e = Executors.newFixedThreadPool(numThreads);
		
		// BEGIN EM PART1: accumulate the statistics
		CountDownLatch latch = new CountDownLatch(numThreads);
		
		for (int i = 0; i < numThreads; ++i)
			e.execute(new Worker(workingCopies[i] = current.clone(), partialEstimates[i] = new Mixture(fd, nd, dc), latch));
		
		// wait for all jobs to be done
		latch.await();
		
		// make sure the thread pool is done
		e.shutdownNow();
		
		// rewind the list 
		data.rewind();
		
		// BEGIN EM PART2: combine the partial estimates
		current.clear();
		
		// sum of all posteriors
		double ps = 0.;
		
		for (Mixture est : partialEstimates) {
			for (int i = 0; i < nd; ++i) {
				Density source = est.components[i];
				Density target = current.components[i];
				
				target.apr += source.apr;
				ps += source.apr;				
				
				for (int j = 0; j < fd; ++j)
					target.mue[j] += source.mue[j];
				
				for (int j = 0; j < target.cov.length; ++j)
					target.cov[j] += source.cov[j];
			}
		}
		
		// normalize means and covariances
		for (int i = 0; i < nd; ++i) {
			Density d = current.components[i];
			
			double [] mue = d.mue;
			double [] cov = d.cov;
			
			Arithmetics.sdiv2(mue, d.apr);
			Arithmetics.sdiv2(cov, d.apr);
			
			d.apr /= ps;
			
			// conclude covariance computation
			if (dc) {
				for (int j = 0; j < fd; ++j)
					cov[j] -= mue[j] * mue[j];
			} else {
				int l = 0;
				for (int j = 0; j < fd; ++j)
					for (int k = 0; k <= j; ++k)
						cov[l++] -= mue[j] * mue[k];
			}
			
			// update the internals of the new estimate
			d.update();
		}
		
		// END EM Part2

		logger.info("ParallelEM.iterate(): END");
	}
	
	/**
	 * First part of the EM: Accumulate posteriors, prepare priors and mean
	 */
	private class Worker implements Runnable {
		Mixture work, est;
		CountDownLatch latch;
		
		/** feature buffer */
		double [] f;
		
		/** posterior buffer */
		double [] p;
		
		/** number of chunks processed by this thread */
		int cnt_chunk = 0;
		
		/** number of frames processed by this thread */
		int cnt_frame = 0;
		
		Worker(Mixture workingCopy, Mixture partialEstimate, CountDownLatch latch) {
			this.latch = latch;
			this.work = workingCopy;
			this.est = partialEstimate;
			
			// init the buffers
			f = new double [fd];
			p = new double [nd];
			
			// make sure the estimate is cleared up!
			est.clear();
		}
		
		/**
		 * Main thread routine: read as there are chunks, compute posteriors,
		 * update the accus
		 */
		public void run() {
			try {
				ChunkedDataSet.Chunk chunk;
				
				// as long as we have chunks to do... NB: data is (synchronized) from ParallelEM!
				while ((chunk = data.nextChunk()) != null) {
					FrameReader source = chunk.getFrameReader();
						
					while (source.read(f)) {
						work.evaluate(f);
						work.posteriors(p);
												
						for (int i = 0; i < nd; ++i) {
							// prior
							est.components[i].apr += p[i];
							
							double [] mue = est.components[i].mue;
							double [] cov = est.components[i].cov;
							
							double pf;
							
							// diagonal ? cov and mue in one run
							if (dc) {
								for (int j = 0; j < fd; ++j) {
									pf = p[i] * f[j];
									mue[j] += pf;
									cov[j] += pf * f[j];
								}
							} else {
								int l = 0;
								for (int j = 0; j < fd; ++j) {
									pf = p[i] * f[j];
									mue[j] += pf;
									for (int k = 0; k <= j; ++k)
										cov[l++] += pf * f[k];
								}
							}
						}
						
						cnt_frame++;
					}
					
					cnt_chunk++;
				}
				
				logger.info("ParallelEM.Worker#" + Thread.currentThread().getId() + ".run(): processed " + cnt_frame + " in " + cnt_chunk + " chunks");
			
			} catch (IOException e) {
				logger.info("ParallelEM.Worker#" + Thread.currentThread().getId() + ".run(): IOException: " + e.toString());
			} finally {
				// notify the main thread
				latch.countDown();
			}
		}
	}
}
