package com.github.sikoried.jstk.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import com.github.sikoried.jstk.io.FrameInputStream;
import com.github.sikoried.jstk.io.FrameSource;


public class Posteriors {

	public static final String SYNOPSIS = 
		"usage: app.Posteriors file-list dir1 dir2 [ dir3 ... ]\n" +
		"Compute segment posteriors from individual logscores.";
	
	public static void main(String [] args) throws IOException {
		if (args.length < 3) {
			System.err.println(SYNOPSIS);
			System.err.println(Arrays.toString(args));
			System.exit(1);
		}
				
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		String l;
		while ((l = br.readLine()) != null) {
			double [] logpsum = new double [args.length - 1];
			int [] numf = new int [args.length - 1];
			for (int i = 1; i < args.length; ++i) {
				FrameSource fr = new FrameInputStream(new File(args[i] + System.getProperty("file.separator") + l));
				double [] lp = new double [fr.getFrameSize()];
				
				if (lp.length != 1)
					throw new IOException(args[i] + " : not a lopg file! (dim > 1)");
				
				while (fr.read(lp)) {
					logpsum[i-1] += lp[0];
					numf[i-1]++;
				}
			}
			
			// compute logp
			double sum = 0.0;
			for (int i = 0; i < logpsum.length; ++i)
				sum += (logpsum[i] = Math.exp(logpsum[i] / (double) numf[i]));
			
			double mv = -Double.MAX_VALUE;
			int mp = 0;
			for (int i = 0; i < logpsum.length; ++i) {
				logpsum[i] /= sum;
				
				if (logpsum[i] > mv) {
					mv = logpsum[i];
					mp = i;
				}
			}
			
			System.out.printf("%s %d %.5f", l, mp, mv);
			for (double d : logpsum)
				System.out.printf(" %.5f", d);
			System.out.println();			
		}
		
	}

}
