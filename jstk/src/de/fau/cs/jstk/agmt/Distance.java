package de.fau.cs.jstk.agmt;

import java.io.File;
import java.io.IOException;

import de.fau.cs.jstk.io.FrameInputStream;

public class Distance {

	public static String SYNOPSIS = 
		"Compute (per dimension) mean values and average distance between two frame files.\n" +
		"usage: agmt.Distance file1 file2";
	public static void main(String [] args) throws IOException {
		if (args.length != 2) {
			System.err.println(SYNOPSIS);
			System.exit(1);
		}
		
		FrameInputStream fis1 = new FrameInputStream(new File(args[0]));
		FrameInputStream fis2 = new FrameInputStream(new File(args[1]));
		
		if (fis1.getFrameSize() != fis2.getFrameSize())
			throw new IOException("Frame file dimensions do not match!");
		
		double [] buf1 = new double [fis1.getFrameSize()];
		double [] buf2 = new double [fis2.getFrameSize()];
		
		double [] m1 = new double [buf1.length];
		double [] m2 = new double [buf1.length];
		double [] dist = new double [buf1.length];
	
		long frames = 0;
		while (fis1.read(buf1) && fis2.read(buf2)) {
			for (int i = 0; i < buf1.length; ++i) {
				m1[i] += buf1[i];
				m2[i] += buf2[i];
				dist[i] += Math.abs(buf1[i] - buf2[i]);
			}
			frames++;
		}
		
		for (int i = 0; i < buf1.length; ++i) {
			m1[i] /= frames;
			m2[i] /= frames;
		}
		
		fis1.close();
		fis2.close();
		
		for (int i = 0; i < dist.length; ++i)
			System.out.println(i + "\t" + m1[i] + "\t" + m2[i] + "\t" + (dist[i] / frames));
	}

}
